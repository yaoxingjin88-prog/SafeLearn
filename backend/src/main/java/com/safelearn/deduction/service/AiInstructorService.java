package com.safelearn.deduction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.deduction.entity.SimulationDecision;
import com.safelearn.deduction.entity.SimulationSession;
import com.safelearn.deduction.repository.SimulationDecisionRepository;
import com.safelearn.repository.KnowledgeBaseRepository;
import com.safelearn.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiInstructorService {

    private final SimulationDecisionRepository decisionRepo;
    private final KnowledgeBaseRepository knowledgeRepo;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfig;

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${app.ai.api-url:}")
    private String apiUrl;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.model:gpt-4o-mini}")
    private String model;

    private boolean aiEnabled() {
        return systemConfig.getBoolean("ai.enabled", aiEnabled);
    }

    private String apiKey() {
        return systemConfig.getString("ai.apiKey", apiKey);
    }

    private String aiModel() {
        return systemConfig.getString("ai.model", model);
    }

    public Map<String, Object> evaluate(SimulationSession session, int ruleScore) {
        List<SimulationDecision> decisions = decisionRepo.findBySessionIdOrderByElapsedMsAsc(session.getId());
        String decisionsJson = serializeDecisions(decisions);

        if (aiEnabled() && apiUrl != null && !apiUrl.isBlank() && apiKey() != null && !apiKey().isBlank()) {
            try {
                Map<String, Object> llm = evaluateWithLlm(session, ruleScore, decisionsJson);
                if (llm != null) return llm;
            } catch (Exception ignored) {
                // 回退到知识库教官
            }
        }
        return evaluateWithKnowledge(session, ruleScore, decisions);
    }

    private Map<String, Object> evaluateWithLlm(SimulationSession session, int ruleScore, String decisionsJson) {
        String prompt = buildPrompt(session, ruleScore, decisionsJson);
        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey());

        Map<String, Object> body = Map.of(
                "model", aiModel(),
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.3
        );

        ResponseEntity<Map> resp = rest.exchange(
                apiUrl,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        if (resp.getBody() == null) return null;
        List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.getBody().get("choices");
        if (choices == null || choices.isEmpty()) return null;
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");
        try {
            return parseJsonResponse(content);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> parseJsonResponse(String content) throws Exception {
        if (content == null) return null;
        String json = content.trim();
        if (json.contains("```")) {
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) json = json.substring(start, end + 1);
        }
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    private Map<String, Object> evaluateWithKnowledge(SimulationSession session, int ruleScore, List<SimulationDecision> decisions) {
        boolean success = "success".equals(session.getOutcome());
        long firstDecisionMs = decisions.isEmpty() ? Long.MAX_VALUE : decisions.get(0).getElapsedMs();
        boolean fastResponse = firstDecisionMs <= 90_000;
        boolean hasBadChoice = decisions.stream().anyMatch(d -> "opt-observe".equals(d.getOptionId()) || "opt-open-door".equals(d.getOptionId()) || "opt-skip".equals(d.getOptionId()));
        boolean goodBranch = "isolation".equals(session.getBranch()) || "venting".equals(session.getBranch()) || "fire".equals(session.getBranch());

        int timeliness = fastResponse ? 22 : (firstDecisionMs <= 120_000 ? 15 : 8);
        if (!success) timeliness = Math.min(timeliness, 12);

        int procedure = goodBranch ? 23 : 12;
        if ("isolation".equals(session.getBranch())) procedure = 24;

        int decision = Math.min(25, Math.max(5, ruleScore / 4));
        if (hasBadChoice) decision = Math.max(3, decision - 10);

        int outcome = success ? 24 : 6;
        if (success && session.getMaxTemperature() != null && session.getMaxTemperature().doubleValue() < 150) {
            outcome = 25;
        }

        int total = timeliness + procedure + decision + outcome;
        String kbTip = knowledgeRepo.searchByKeyword("热失控").stream()
                .findFirst()
                .map(k -> k.getContent())
                .orElse("请参考储能电站热失控应急处置 SOP。");

        List<String> highlights = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        if (success) highlights.add("成功控制事态，推演达到受控终态");
        if (goodBranch) highlights.add("处置分支符合隔离/通风/灭火基本流程");
        if (fastResponse) highlights.add("首次决策响应较为及时");
        if (!success) improvements.add("未能有效控制事故扩大，需加强早期预警响应");
        if (hasBadChoice) improvements.add("存在高风险决策（如继续观察、盲目开门），请复习 SOP");
        if (!fastResponse) improvements.add("首次决策响应偏慢，建议在 60°C 后 30 秒内完成处置");
        if (improvements.isEmpty()) improvements.add("可进一步压缩响应时间，提升处置熟练度");

        Map<String, Object> dimensions = Map.of(
                "timeliness", dim(timeliness, fastResponse ? "响应时效良好" : "响应时效有待提升"),
                "procedure", dim(procedure, goodBranch ? "处置顺序基本符合规范" : "处置顺序需按 SOP 优化"),
                "decision", dim(decision, hasBadChoice ? "部分决策存在安全风险" : "决策选项整体合理"),
                "outcome", dim(outcome, success ? "事故后果受控" : "事故扩大，需复盘")
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalScore", total);
        result.put("rating", rating(total));
        result.put("dimensions", dimensions);
        result.put("highlights", highlights);
        result.put("instructorSummary", String.format(
                "本次推演%s。%s 建议结合《%s》继续巩固应急处置要点。",
                success ? "达成受控终态" : "未能控制事故扩大",
                kbTip.length() > 40 ? kbTip.substring(0, 40) + "…" : kbTip,
                knowledgeRepo.searchByKeyword("热失控").stream().findFirst().map(k -> k.getTitle()).orElse("热失控处置规范")
        ));
        result.put("improvements", improvements);
        return result;
    }

    private Map<String, Object> dim(int score, String comment) {
        return Map.of("score", score, "comment", comment);
    }

    private String rating(int score) {
        if (score >= 90) return "excellent";
        if (score >= 75) return "good";
        if (score >= 60) return "average";
        return "poor";
    }

    private String serializeDecisions(List<SimulationDecision> decisions) {
        try {
            return objectMapper.writeValueAsString(decisions.stream().map(d -> Map.of(
                    "decisionPointId", d.getDecisionPointId(),
                    "optionId", d.getOptionId(),
                    "elapsedMs", d.getElapsedMs(),
                    "scoreDelta", d.getScoreDelta() != null ? d.getScoreDelta() : 0,
                    "optimal", Boolean.TRUE.equals(d.getOptimal())
            )).toList());
        } catch (Exception e) {
            return "[]";
        }
    }

    public String buildPrompt(SimulationSession session, int ruleScore, String decisionsJson) {
        return """
                你是一名储能电站安全培训 AI 教官，负责评估学员在「单电池热失控处置」推演中的表现。

                ## 输入
                - 场景 ID：%s
                - 推演时长：%d ms
                - 最终状态：%s
                - 分支：%s
                - 最高温度：%s °C
                - 规则基础分：%d/100
                - 决策时间线（JSON）：
                %s

                ## 评分维度（每项 0-25 分）
                1. 响应时效：是否在 60°C 后 30s 内做出首次有效处置
                2. 处置顺序：是否先隔离/通风再灭火，符合 GB/T 42288
                3. 决策合理性：禁止盲目开门、禁止用水直接喷淋 Li-ion
                4. 结果控制：是否避免热扩散

                ## 输出格式（严格 JSON，不要 markdown）
                {
                  "totalScore": 0-100,
                  "rating": "excellent|good|average|poor",
                  "dimensions": {
                    "timeliness": { "score": 0-25, "comment": "..." },
                    "procedure": { "score": 0-25, "comment": "..." },
                    "decision": { "score": 0-25, "comment": "..." },
                    "outcome": { "score": 0-25, "comment": "..." }
                  },
                  "highlights": ["..."],
                  "improvements": ["..."],
                  "instructorSummary": "200字以内总评"
                }
                """.formatted(
                session.getScenarioId(),
                session.getElapsedMs() != null ? session.getElapsedMs() : 0,
                session.getOutcome() != null ? session.getOutcome() : "pending",
                session.getBranch() != null ? session.getBranch() : "none",
                session.getMaxTemperature() != null ? session.getMaxTemperature() : "0",
                ruleScore,
                decisionsJson
        );
    }
}

package com.safelearn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.dto.AiAskRequest;
import com.safelearn.dto.FeedbackRequest;
import com.safelearn.entity.KnowledgeBase;
import com.safelearn.entity.QaRecord;
import com.safelearn.entity.User;
import com.safelearn.repository.KnowledgeBaseRepository;
import com.safelearn.repository.QaRecordRepository;
import com.safelearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final QaRecordRepository qaRepo;
    private final UserRepository userRepo;
    private final KnowledgeBaseRepository knowledgeRepo;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfig;

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${app.ai.api-url:}")
    private String apiUrl;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.model:qwen-plus}")
    private String model;

    /** 流式问答专用线程池，避免阻塞 Web 请求线程。 */
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是「储能安全 AI 助手」，专注于储能电站安全、锂电池热失控、消防安全、BMS、应急处置等领域。
            请遵循以下要求回答：
            1. 优先依据提供的【知识库参考资料】作答，资料不足时可结合储能安全行业通识补充，但需保证专业、准确。
            2. 回答使用简体中文，条理清晰，可使用分点说明，避免冗长空话。
            3. 涉及应急处置时，给出具体、可执行的步骤与安全注意事项。
            4. 若问题与储能/电池安全无关，礼貌引导用户回到相关主题。
            """;

    private static final List<String> DEFAULT_RELATED_QUESTIONS = List.of(
            "热失控前兆有哪些？",
            "储能柜冒烟如何处理？",
            "锂电池消防系统如何选择？"
    );

    // ---------- 从配置中心读取 AI 配置，@Value / 常量作为兜底默认值 ----------

    private boolean aiEnabled() {
        return systemConfig.getBoolean("ai.enabled", aiEnabled);
    }

    private String apiKey() {
        return systemConfig.getString("ai.apiKey", apiKey);
    }

    private String aiModel() {
        return systemConfig.getString("ai.model", model);
    }

    private String systemPrompt() {
        return systemConfig.getString("ai.systemPrompt", DEFAULT_SYSTEM_PROMPT);
    }

    /**
     * 流式问答：通过 SSE 逐块返回千问生成的 token，结束后保存问答记录。
     * 事件格式：
     *   event: sources  data: [{id,title,relevance}]   （首先发送，便于前端展示来源）
     *   event: delta     data: {"text":"..."}            （逐块文本）
     *   event: done      data: {"relatedQuestions":[...]}
     *   event: error     data: {"message":"..."}
     */
    public SseEmitter askStream(String userId, AiAskRequest req) {
        String question = req.getQuestion().trim();
        SseEmitter emitter = new SseEmitter(120_000L);

        List<ScoredKnowledge> matches = searchKnowledge(question);
        List<Map<String, Object>> sources = matches.stream()
                .limit(3)
                .map(m -> {
                    Map<String, Object> source = new LinkedHashMap<>();
                    source.put("id", m.entry.getId());
                    source.put("title", m.entry.getTitle());
                    source.put("relevance", Math.round(m.score * 100.0) / 100.0);
                    return source;
                })
                .toList();

        streamExecutor.execute(() -> {
            StringBuilder full = new StringBuilder();
            try {
                emitter.send(SseEmitter.event().name("sources").data(sources));

                boolean streamed = false;
                if (aiEnabled() && apiUrl != null && !apiUrl.isBlank() && apiKey() != null && !apiKey().isBlank()) {
                    streamed = streamWithQwen(question, matches, emitter, full);
                }
                if (!streamed) {
                    // 千问不可用：回退本地答案，仍按 delta 一次性发出
                    String fallback = buildAnswer(question, matches);
                    full.append(fallback);
                    emitter.send(SseEmitter.event().name("delta").data(Map.of("text", fallback)));
                }

                persistQa(userId, question, full.toString(), sources);

                emitter.send(SseEmitter.event().name("done")
                        .data(Map.of("relatedQuestions", pickRelatedQuestions(question))));
                emitter.complete();
            } catch (Exception e) {
                log.warn("流式问答失败: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("message", "AI 服务暂时不可用，请稍后重试。")));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /** 调用千问流式接口，逐块通过 SSE 转发；成功流式返回 true。 */
    private boolean streamWithQwen(String question, List<ScoredKnowledge> matches,
                                   SseEmitter emitter, StringBuilder full) {
        String userContent = buildUserContent(question, matches);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content", userContent)
        ));
        body.put("temperature", 0.4);
        body.put("stream", true);

        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey());
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(120_000);
            conn.getOutputStream().write(objectMapper.writeValueAsBytes(body));

            if (conn.getResponseCode() != 200) {
                return false;
            }

            boolean gotAny = false;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String payload = line.substring(5).trim();
                    if (payload.isEmpty()) continue;
                    if ("[DONE]".equals(payload)) break;
                    String delta = extractDelta(payload);
                    if (delta != null && !delta.isEmpty()) {
                        full.append(delta);
                        emitter.send(SseEmitter.event().name("delta").data(Map.of("text", delta)));
                        gotAny = true;
                    }
                }
            }
            return gotAny;
        } catch (Exception e) {
            log.warn("千问流式调用失败: {}", e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractDelta(String jsonChunk) {
        try {
            Map<String, Object> obj = objectMapper.readValue(jsonChunk, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) obj.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
            return delta != null ? (String) delta.get("content") : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void persistQa(String userId, String question, String answer, List<Map<String, Object>> sources) {
        try {
            User user = userRepo.findById(userId).orElse(null);
            if (user == null) return;
            QaRecord record = new QaRecord();
            record.setUser(user);
            record.setQuestion(question);
            record.setAnswer(answer);
            record.setSources(serializeSources(sources));
            qaRepo.save(record);
        } catch (Exception e) {
            log.warn("保存问答记录失败: {}", e.getMessage());
        }
    }

    public Map<String, Object> ask(String userId, AiAskRequest req) {
        String question = req.getQuestion().trim();
        List<ScoredKnowledge> matches = searchKnowledge(question);
        String answer = generateAnswer(question, matches);
        List<Map<String, Object>> sources = matches.stream()
                .limit(3)
                .map(m -> {
                    Map<String, Object> source = new LinkedHashMap<>();
                    source.put("id", m.entry.getId());
                    source.put("title", m.entry.getTitle());
                    source.put("relevance", Math.round(m.score * 100.0) / 100.0);
                    return source;
                })
                .toList();

        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        QaRecord record = new QaRecord();
        record.setUser(user);
        record.setQuestion(question);
        record.setAnswer(answer);
        record.setSources(serializeSources(sources));
        qaRepo.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("sources", sources);
        result.put("relatedQuestions", pickRelatedQuestions(question));
        return result;
    }

    public Map<String, Object> getHistory(String userId, int page, int pageSize) {
        Page<QaRecord> records = qaRepo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page - 1, pageSize));
        Map<String, Object> result = new HashMap<>();
        result.put("items", records.getContent().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("question", r.getQuestion());
            m.put("answer", r.getAnswer());
            m.put("rating", r.getRating());
            m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
            return m;
        }).toList());
        result.put("total", records.getTotalElements());
        return result;
    }

    public Map<String, Object> feedback(FeedbackRequest req) {
        QaRecord record = qaRepo.findById(req.getRecordId())
                .orElseThrow(() -> new RuntimeException("记录不存在"));
        record.setRating(req.getRating());
        qaRepo.save(record);
        return Map.of("success", true);
    }

    public List<Map<String, Object>> getKnowledgeList() {
        return knowledgeRepo.findAll().stream().map(this::toKnowledgeInfo).toList();
    }

    private List<ScoredKnowledge> searchKnowledge(String question) {
        Map<String, ScoredKnowledge> scored = new LinkedHashMap<>();
        List<String> keywords = extractKeywords(question);

        for (String keyword : keywords) {
            for (KnowledgeBase entry : knowledgeRepo.searchByKeyword(keyword)) {
                scored.compute(entry.getId(), (id, existing) -> {
                    double addScore = scoreEntry(entry, question, keyword);
                    if (existing == null) {
                        return new ScoredKnowledge(entry, addScore);
                    }
                    existing.score += addScore;
                    return existing;
                });
            }
        }

        if (scored.isEmpty()) {
            for (KnowledgeBase entry : knowledgeRepo.findAll()) {
                double score = scoreEntry(entry, question, question);
                if (score > 0) {
                    scored.put(entry.getId(), new ScoredKnowledge(entry, score));
                }
            }
        }

        return scored.values().stream()
                .sorted(Comparator.comparingDouble((ScoredKnowledge s) -> s.score).reversed())
                .limit(5)
                .toList();
    }

    private double scoreEntry(KnowledgeBase entry, String question, String keyword) {
        double score = 0;
        String lowerQuestion = question.toLowerCase(Locale.ROOT);
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);

        if (entry.getTitle() != null && entry.getTitle().toLowerCase(Locale.ROOT).contains(lowerKeyword)) {
            score += 2.5;
        }
        if (entry.getContent() != null && entry.getContent().toLowerCase(Locale.ROOT).contains(lowerKeyword)) {
            score += 1.5;
        }
        if (entry.getTags() != null && entry.getTags().toLowerCase(Locale.ROOT).contains(lowerKeyword)) {
            score += 2.0;
        }
        if (entry.getTitle() != null && lowerQuestion.contains(entry.getTitle().toLowerCase(Locale.ROOT))) {
            score += 3.0;
        }
        return score;
    }

    private List<String> extractKeywords(String question) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        keywords.add(question);
        Arrays.stream(question.split("[\\s,，。！？；;、]+"))
                .map(String::trim)
                .filter(s -> s.length() >= 2)
                .forEach(keywords::add);

        List<String> presets = List.of("热失控", "冒烟", "消防", "BMS", "储能", "锂电池", "灭火", "安全检查", "应急");
        for (String preset : presets) {
            if (question.contains(preset)) {
                keywords.add(preset);
            }
        }
        return new ArrayList<>(keywords);
    }

    /** 优先调用通义千问生成回答，失败时回退到知识库拼接答案。 */
    private String generateAnswer(String question, List<ScoredKnowledge> matches) {
        if (aiEnabled() && apiUrl != null && !apiUrl.isBlank() && apiKey() != null && !apiKey().isBlank()) {
            try {
                String llmAnswer = answerWithQwen(question, matches);
                if (llmAnswer != null && !llmAnswer.isBlank()) {
                    return llmAnswer.trim();
                }
            } catch (Exception e) {
                log.warn("通义千问调用失败，回退知识库答案: {}", e.getMessage());
            }
        }
        return buildAnswer(question, matches);
    }

    /** 拼接发给千问的用户消息：知识库资料 + 用户问题。 */
    private String buildUserContent(String question, List<ScoredKnowledge> matches) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < Math.min(matches.size(), 4); i++) {
            KnowledgeBase entry = matches.get(i).entry;
            context.append("【资料").append(i + 1).append("】")
                    .append(entry.getTitle()).append("\n")
                    .append(entry.getContent()).append("\n\n");
        }
        return context.length() > 0
                ? "知识库参考资料：\n" + context + "\n用户问题：" + question
                : "用户问题：" + question;
    }

    @SuppressWarnings("unchecked")
    private String answerWithQwen(String question, List<ScoredKnowledge> matches) {
        String userContent = buildUserContent(question, matches);

        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey());

        Map<String, Object> body = Map.of(
                "model", aiModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt()),
                        Map.of("role", "user", "content", userContent)
                ),
                "temperature", 0.4
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
        return message != null ? (String) message.get("content") : null;
    }

    private String buildAnswer(String question, List<ScoredKnowledge> matches) {
        if (matches.isEmpty()) {
            return "关于\"" + question + "\"：暂未在知识库中找到完全匹配的内容。"
                    + "建议参考《储能电站安全管理规定》和《锂离子电池储能系统技术规范》，"
                    + "或补充更具体的问题描述（如设备类型、告警现象、处置阶段）。";
        }

        if (matches.size() == 1) {
            ScoredKnowledge best = matches.get(0);
            return best.entry.getTitle() + "：\n" + best.entry.getContent();
        }

        StringBuilder answer = new StringBuilder("根据知识库检索，为您整理如下：\n\n");
        for (int i = 0; i < Math.min(matches.size(), 3); i++) {
            ScoredKnowledge match = matches.get(i);
            answer.append(i + 1).append(". ").append(match.entry.getTitle()).append("\n")
                    .append(match.entry.getContent()).append("\n\n");
        }
        return answer.toString().trim();
    }

    private List<String> pickRelatedQuestions(String question) {
        return systemConfig.getStringList("ai.relatedQuestions", DEFAULT_RELATED_QUESTIONS).stream()
                .filter(q -> !q.equals(question))
                .limit(3)
                .collect(Collectors.toList());
    }

    private String serializeSources(List<Map<String, Object>> sources) {
        try {
            return objectMapper.writeValueAsString(sources.stream().map(s -> s.get("title")).toList());
        } catch (Exception e) {
            return "[]";
        }
    }

    private Map<String, Object> toKnowledgeInfo(KnowledgeBase entry) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", entry.getId());
        m.put("category", entry.getCategory());
        m.put("title", entry.getTitle());
        m.put("content", entry.getContent());
        m.put("tags", entry.getTags());
        m.put("source", entry.getSource());
        return m;
    }

    private static class ScoredKnowledge {
        private final KnowledgeBase entry;
        private double score;

        private ScoredKnowledge(KnowledgeBase entry, double score) {
            this.entry = entry;
            this.score = score;
        }
    }
}

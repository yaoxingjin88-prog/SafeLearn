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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final QaRecordRepository qaRepo;
    private final UserRepository userRepo;
    private final KnowledgeBaseRepository knowledgeRepo;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${app.ai.api-url:}")
    private String apiUrl;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.model:qwen-plus}")
    private String model;

    private static final String SYSTEM_PROMPT = """
            你是「储能安全 AI 助手」，专注于储能电站安全、锂电池热失控、消防安全、BMS、应急处置等领域。
            请遵循以下要求回答：
            1. 优先依据提供的【知识库参考资料】作答，资料不足时可结合储能安全行业通识补充，但需保证专业、准确。
            2. 回答使用简体中文，条理清晰，可使用分点说明，避免冗长空话。
            3. 涉及应急处置时，给出具体、可执行的步骤与安全注意事项。
            4. 若问题与储能/电池安全无关，礼貌引导用户回到相关主题。
            """;

    private static final List<String> RELATED_QUESTIONS = List.of(
            "热失控前兆有哪些？",
            "储能柜冒烟如何处理？",
            "锂电池消防系统如何选择？"
    );

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
        if (aiEnabled && apiUrl != null && !apiUrl.isBlank() && apiKey != null && !apiKey.isBlank()) {
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

    @SuppressWarnings("unchecked")
    private String answerWithQwen(String question, List<ScoredKnowledge> matches) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < Math.min(matches.size(), 4); i++) {
            KnowledgeBase entry = matches.get(i).entry;
            context.append("【资料").append(i + 1).append("】")
                    .append(entry.getTitle()).append("\n")
                    .append(entry.getContent()).append("\n\n");
        }
        String userContent = (context.length() > 0
                ? "知识库参考资料：\n" + context + "\n用户问题：" + question
                : "用户问题：" + question);

        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
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
        return RELATED_QUESTIONS.stream()
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

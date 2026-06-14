package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.common.DifficultyLevel;
import com.safelearn.dto.TrainingDecisionRequest;
import com.safelearn.entity.Scenario;
import com.safelearn.entity.TrainingRecord;
import com.safelearn.entity.User;
import com.safelearn.repository.ScenarioRepository;
import com.safelearn.repository.TrainingRecordRepository;
import com.safelearn.repository.UserProgressRepository;
import com.safelearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final ScenarioRepository scenarioRepo;
    private final TrainingRecordRepository recordRepo;
    private final UserRepository userRepo;
    private final UserProgressRepository progressRepo;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfig;

    private static final int MASTERY_THRESHOLD = 60;

    /** 掌握度解锁阈值，可由管理端配置覆盖。 */
    private int masteryThreshold() {
        return systemConfig.getInt("learning.masteryThreshold", MASTERY_THRESHOLD);
    }

    public List<Map<String, Object>> getScenarios(String userId) {
        final Set<String> completedIds = userId != null
                ? new HashSet<>(progressRepo.findCompletedChapterIdsByUserId(userId))
                : new HashSet<>();
        final Set<String> qualifiedIds = userId != null
                ? new HashSet<>(progressRepo.findQualifiedChapterIdsByUserId(userId, masteryThreshold()))
                : new HashSet<>();
        return scenarioRepo.findAll().stream()
                .filter(s -> !isTestScenario(s))
                .filter(this::hasDecisionTraining)
                .map(s -> {
            Map<String, Object> m = toScenarioInfo(s);
            m.put("unlocked", isScenarioUnlocked(s, userId, completedIds, qualifiedIds));
            return m;
        }).toList();
    }

    private boolean isTestScenario(Scenario s) {
        if (s.getName() == null || s.getName().isBlank()) return true;
        String name = s.getName().trim().toLowerCase(Locale.ROOT);
        return "test".equals(name) || name.startsWith("test_") || name.startsWith("test ");
    }

    private boolean hasDecisionTraining(Scenario s) {
        List<Map<String, Object>> dps = getDecisionPoints(s);
        return !dps.isEmpty();
    }

    public Map<String, Object> getScenarioById(String id) {
        Scenario s = scenarioRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("场景不存在"));
        return toScenarioInfo(s);
    }

    @Transactional
    public Map<String, Object> startTraining(String userId, String scenarioId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        Scenario scenario = scenarioRepo.findById(scenarioId).orElseThrow(() -> new RuntimeException("场景不存在"));

        List<String> prereqIds = parsePrereqIds(scenario.getPrerequisiteIds());
        if (!prereqIds.isEmpty()) {
            for (String prereqId : prereqIds) {
                if (scenario.getDifficulty() != null && scenario.getDifficulty() > DifficultyLevel.BASIC) {
                    int threshold = masteryThreshold();
                    boolean met = progressRepo.existsByUserIdAndChapterIdAndCompletedWithMastery(
                            userId, prereqId, threshold);
                    if (!met) {
                        throw new RuntimeException("请先完成前置章节并达到掌握度" + threshold + "%");
                    }
                } else {
                    boolean met = progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, prereqId);
                    if (!met) {
                        throw new RuntimeException("请先完成前置章节学习");
                    }
                }
            }
        }

        TrainingRecord record = new TrainingRecord();
        record.setUser(user);
        record.setScenario(scenario);
        record.setStartTime(LocalDateTime.now());
        record.setTotalScore(0);
        record = recordRepo.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", record.getId());
        result.put("firstDecisionPoint", Map.of(
                "id", "dp1",
                "description", "发现电池温度异常升高，应采取什么措施？",
                "options", List.of(
                        Map.of("id", "opt1", "text", "立即切断电源", "score", 30),
                        Map.of("id", "opt2", "text", "继续观察", "score", 0),
                        Map.of("id", "opt3", "text", "启动冷却系统", "score", 20)
                )
        ));
        return result;
    }

    @Transactional
    public Map<String, Object> submitDecision(String userId, TrainingDecisionRequest req) {
        TrainingRecord record = recordRepo.findByIdWithDetails(req.getRecordId())
                .orElseThrow(() -> new RuntimeException("训练记录不存在"));
        if (!record.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权操作该训练记录");
        }
        if (record.getEndTime() != null) {
            throw new RuntimeException("训练已结束");
        }

        Scenario scenario = record.getScenario();
        if (scenario == null) {
            throw new RuntimeException("训练场景不存在");
        }

        List<Map<String, Object>> decisionPoints = getDecisionPoints(scenario);
        Map<String, Object> decisionPoint = decisionPoints.stream()
                .filter(dp -> req.getDecisionPointId().equals(dp.get("id")))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("决策点不存在"));

        Map<String, Object> option = findOption(decisionPoint, req.getOptionId());
        int score = toInt(option.get("score"));
        boolean correct = Boolean.TRUE.equals(option.get("correct"));
        String consequence = correct
                ? "操作正确，成功控制险情"
                : "操作不当，险情有所扩大";

        List<Map<String, Object>> decisions = getDecisionsList(record);
        boolean alreadySubmitted = decisions.stream()
                .anyMatch(d -> req.getDecisionPointId().equals(d.get("decisionPointId")));
        if (!alreadySubmitted) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("decisionPointId", req.getDecisionPointId());
            entry.put("optionId", req.getOptionId());
            entry.put("responseTime", req.getResponseTime() != null ? req.getResponseTime() : 0);
            entry.put("score", score);
            entry.put("correct", correct);
            entry.put("question", decisionPoint.get("question"));
            entry.put("selectedAnswer", option.get("text"));
            entry.put("correctAnswer", findCorrectAnswer(decisionPoint));
            entry.put("timelinePhase", decisionPoint.get("timelinePhase"));
            entry.put("explanation", decisionPoint.get("explanation"));
            entry.put("regulationRef", decisionPoint.get("regulationRef"));
            if (!correct) {
                Object optionConsequence = option.get("consequence");
                if (optionConsequence != null) {
                    entry.put("consequence", optionConsequence);
                }
            }
            decisions.add(entry);
            saveDecisions(record, decisions);
        }

        int totalScore = decisions.stream().mapToInt(d -> toInt(d.get("score"))).sum();
        record.setTotalScore(totalScore);

        Map<String, Object> nextDecisionPoint = null;
        if (decisions.size() < decisionPoints.size()) {
            nextDecisionPoint = decisionPoints.get(decisions.size());
        } else {
            finalizeRecord(record, scenario, decisions, decisionPoints);
        }
        recordRepo.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("consequence", consequence);
        result.put("totalScore", totalScore);
        result.put("nextDecisionPoint", nextDecisionPoint);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRecords(String userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        Page<TrainingRecord> records = recordRepo.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(safePage - 1, safeSize));
        Map<String, Object> result = new HashMap<>();
        result.put("items", records.getContent().stream().map(this::toRecordInfo).toList());
        result.put("total", records.getTotalElements());
        result.put("page", safePage);
        result.put("pageSize", safeSize);
        result.put("totalPages", records.getTotalPages());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRecordById(String id) {
        TrainingRecord record = recordRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("记录不存在"));
        return toRecordDetail(record);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRecordById(String userId, String id) {
        TrainingRecord record = recordRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("记录不存在"));
        if (record.getUser() == null || !record.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权查看该训练记录");
        }
        return toRecordDetail(record);
    }

    private boolean isScenarioUnlocked(Scenario s, String userId,
                                       Set<String> completedIds, Set<String> qualifiedIds) {
        if (userId == null) return true;
        List<String> prereqIds = parsePrereqIds(s.getPrerequisiteIds());
        if (prereqIds.isEmpty()) return true;
        for (String prereqId : prereqIds) {
            if (s.getDifficulty() != null && s.getDifficulty() > DifficultyLevel.BASIC) {
                if (!qualifiedIds.contains(prereqId)) return false;
            } else {
                if (!completedIds.contains(prereqId)) return false;
            }
        }
        return true;
    }

    private Map<String, Object> toScenarioInfo(Scenario s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("description", s.getDescription());
        m.put("duration", s.getDuration());
        m.put("timeLimit", s.getDuration());
        m.put("difficulty", s.getDifficulty());
        m.put("difficultyLabel", DifficultyLevel.label(s.getDifficulty() != null ? s.getDifficulty() : 1));
        m.put("prerequisiteIds", parsePrereqIds(s.getPrerequisiteIds()));
        m.put("initialConditions", parseJson(s.getInitialConditions()));
        m.put("events", parseJson(s.getEvents()));
        m.put("decisionPoints", getDecisionPoints(s));
        return m;
    }

    private List<Map<String, Object>> parseDecisionPoints(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getDecisionPoints(Scenario scenario) {
        return parseDecisionPoints(scenario.getDecisionPoints());
    }

    private Map<String, Object> toRecordInfo(TrainingRecord r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("scenarioId", r.getScenario() != null ? r.getScenario().getId() : null);
        m.put("scenarioName", r.getScenario() != null ? r.getScenario().getName() : null);
        m.put("totalScore", r.getTotalScore());
        m.put("rating", r.getRating());
        m.put("startTime", r.getStartTime() != null ? r.getStartTime().toString() : null);
        m.put("endTime", r.getEndTime() != null ? r.getEndTime().toString() : null);
        m.put("completedAt", r.getEndTime() != null ? r.getEndTime().toString() : (r.getStartTime() != null ? r.getStartTime().toString() : null));
        return m;
    }

    private Map<String, Object> toRecordDetail(TrainingRecord r) {
        Map<String, Object> m = toRecordInfo(r);
        m.put("decisions", parseJsonList(r.getDecisions()));
        applyFeedbackFields(m, r.getFeedback());
        return m;
    }

    @SuppressWarnings("unchecked")
    private void applyFeedbackFields(Map<String, Object> m, String feedbackJson) {
        if (feedbackJson == null || feedbackJson.isBlank()) {
            m.put("feedback", "");
            m.put("debrief", List.of());
            m.put("highlights", List.of());
            m.put("improvements", List.of());
            return;
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(feedbackJson, new TypeReference<Map<String, Object>>() {});
            m.put("feedback", parsed.getOrDefault("summary", parsed.get("instructorSummary")));
            m.put("instructorSummary", parsed.get("instructorSummary"));
            m.put("debrief", parsed.getOrDefault("debrief", List.of()));
            m.put("highlights", parsed.getOrDefault("highlights", List.of()));
            m.put("improvements", parsed.getOrDefault("improvements", List.of()));
            m.put("keyNodeReview", parsed.getOrDefault("keyNodeReview", List.of()));
            m.put("typicalMistakes", parsed.getOrDefault("typicalMistakes", List.of()));
            m.put("correctFlow", parsed.getOrDefault("correctFlow", List.of()));
            m.put("wrongFlow", parsed.getOrDefault("wrongFlow", List.of()));
            m.put("knowledgePoints", parsed.getOrDefault("knowledgePoints", List.of()));
            if (parsed.get("maxScore") != null) m.put("maxScore", parsed.get("maxScore"));
            return;
        } catch (Exception ignored) {
            /* legacy plain text */
        }
        m.put("feedback", feedbackJson);
        m.put("instructorSummary", feedbackJson);
        m.put("debrief", List.of());
        m.put("highlights", List.of());
        m.put("improvements", List.of());
    }

    private List<String> parsePrereqIds(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Object parseJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            try {
                return objectMapper.readValue(json, List.class);
            } catch (Exception e2) {
                return json;
            }
        }
    }

    private Object parseJsonList(String json) {
        if (json == null) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            try {
                return objectMapper.readValue(json, Map.class);
            } catch (Exception e2) {
                return new ArrayList<>();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findOption(Map<String, Object> decisionPoint, String optionId) {
        Object optionsObj = decisionPoint.get("options");
        if (!(optionsObj instanceof List<?> options)) {
            throw new RuntimeException("决策选项不存在");
        }
        for (Object item : options) {
            if (item instanceof Map<?, ?> option && optionId.equals(option.get("id"))) {
                return (Map<String, Object>) option;
            }
        }
        throw new RuntimeException("决策选项不存在");
    }

    @SuppressWarnings("unchecked")
    private String findCorrectAnswer(Map<String, Object> decisionPoint) {
        Object optionsObj = decisionPoint.get("options");
        if (!(optionsObj instanceof List<?> options)) {
            return "";
        }
        for (Object item : options) {
            if (item instanceof Map<?, ?> option && Boolean.TRUE.equals(option.get("correct"))) {
                return String.valueOf(option.get("text"));
            }
        }
        return "";
    }

    private List<Map<String, Object>> getDecisionsList(TrainingRecord record) {
        if (record.getDecisions() == null || record.getDecisions().isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(record.getDecisions(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveDecisions(TrainingRecord record, List<Map<String, Object>> decisions) {
        try {
            record.setDecisions(objectMapper.writeValueAsString(decisions));
        } catch (Exception e) {
            throw new RuntimeException("保存决策记录失败");
        }
    }

    private void finalizeRecord(TrainingRecord record, Scenario scenario,
                                List<Map<String, Object>> decisions,
                                List<Map<String, Object>> decisionPoints) {
        record.setEndTime(LocalDateTime.now());
        int totalScore = record.getTotalScore() != null ? record.getTotalScore() : 0;
        int maxScore = decisionPoints.stream()
                .mapToInt(dp -> maxOptionScore(dp))
                .sum();
        record.setRating(calculateRating(totalScore, maxScore));
        try {
            record.setFeedback(objectMapper.writeValueAsString(
                    buildReportPayload(scenario, decisions, decisionPoints, totalScore, maxScore)));
        } catch (Exception e) {
            record.setFeedback(buildFeedback(decisions, totalScore, maxScore));
        }
    }

    private Map<String, Object> buildReportPayload(Scenario scenario,
                                                   List<Map<String, Object>> decisions,
                                                   List<Map<String, Object>> decisionPoints,
                                                   int totalScore, int maxScore) {
        String rating = calculateRating(totalScore, maxScore);
        long wrongCount = decisions.stream().filter(d -> !Boolean.TRUE.equals(d.get("correct"))).count();
        String level = switch (rating) {
            case "excellent" -> "表现优秀";
            case "good" -> "表现良好";
            case "average" -> "基本合格";
            default -> "需要加强";
        };

        List<Map<String, Object>> debrief = new ArrayList<>();
        for (int i = 0; i < decisionPoints.size(); i++) {
            Map<String, Object> dp = decisionPoints.get(i);
            Map<String, Object> answered = i < decisions.size() ? decisions.get(i) : null;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("step", i + 1);
            item.put("timelinePhase", dp.get("timelinePhase"));
            item.put("question", dp.get("question"));
            item.put("correct", answered != null && Boolean.TRUE.equals(answered.get("correct")));
            item.put("selectedAnswer", answered != null ? answered.get("selectedAnswer") : null);
            item.put("correctAnswer", findCorrectAnswer(dp));
            item.put("explanation", dp.get("explanation"));
            item.put("regulationRef", dp.get("regulationRef"));
            item.put("score", answered != null ? answered.get("score") : 0);
            debrief.add(item);
        }

        List<String> highlights = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        for (int i = 0; i < decisions.size(); i++) {
            Map<String, Object> d = decisions.get(i);
            String phase = d.get("timelinePhase") != null ? String.valueOf(d.get("timelinePhase")) : ("第" + (i + 1) + "步");
            if (Boolean.TRUE.equals(d.get("correct"))) {
                highlights.add(phase + "：决策正确，处置符合规范要求。");
            } else {
                String correctAns = d.get("correctAnswer") != null ? String.valueOf(d.get("correctAnswer")) : "见解析";
                improvements.add(phase + "：应优先选择「" + truncate(correctAns, 48) + "」。");
                if (d.get("explanation") != null) {
                    improvements.add(String.valueOf(d.get("explanation")));
                }
            }
        }
        if (highlights.isEmpty() && wrongCount == 0) {
            highlights.add("全部决策正确，应急处置流程掌握扎实。");
        }
        if (improvements.isEmpty() && wrongCount > 0) {
            improvements.add("共有 " + wrongCount + " 项决策需要改进，建议结合法规依据复盘并重新训练。");
        }

        String summary = level + "。本次得分 " + totalScore + "/" + maxScore + "。"
                + (wrongCount == 0 ? " 全部决策正确，处置链条完整。" : " 共有 " + wrongCount + " 项决策需要改进。");

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("summary", summary);
        report.put("instructorSummary", summary);
        report.put("totalScore", totalScore);
        report.put("maxScore", maxScore);
        report.put("rating", rating);
        report.put("debrief", debrief);
        report.put("highlights", highlights);
        report.put("improvements", improvements);
        applyScenarioReportMeta(report, scenario);
        return report;
    }

    @SuppressWarnings("unchecked")
    private void applyScenarioReportMeta(Map<String, Object> report, Scenario scenario) {
        Object parsed = parseJson(scenario.getInitialConditions());
        if (!(parsed instanceof Map<?, ?> ic)) return;
        Object metaObj = ic.get("trainingReportMeta");
        if (!(metaObj instanceof Map<?, ?> meta)) return;
        copyIfPresent(report, meta, "keyNodeReview");
        copyIfPresent(report, meta, "typicalMistakes");
        copyIfPresent(report, meta, "correctFlow");
        copyIfPresent(report, meta, "wrongFlow");
        copyIfPresent(report, meta, "knowledgePoints");
    }

    private void copyIfPresent(Map<String, Object> target, Map<?, ?> source, String key) {
        if (source.get(key) != null) {
            target.put(key, source.get(key));
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }

    @SuppressWarnings("unchecked")
    private int maxOptionScore(Map<String, Object> decisionPoint) {
        Object optionsObj = decisionPoint.get("options");
        if (!(optionsObj instanceof List<?> options)) {
            return 0;
        }
        return options.stream()
                .filter(item -> item instanceof Map<?, ?>)
                .mapToInt(item -> toInt(((Map<String, Object>) item).get("score")))
                .max()
                .orElse(0);
    }

    private String calculateRating(int totalScore, int maxScore) {
        if (maxScore <= 0) {
            return totalScore >= 90 ? "excellent" : totalScore >= 70 ? "good" : totalScore >= 60 ? "average" : "poor";
        }
        int percentage = totalScore * 100 / maxScore;
        if (percentage >= 90) return "excellent";
        if (percentage >= 70) return "good";
        if (percentage >= 60) return "average";
        return "poor";
    }

    private String buildFeedback(List<Map<String, Object>> decisions, int totalScore, int maxScore) {
        long wrongCount = decisions.stream().filter(d -> !Boolean.TRUE.equals(d.get("correct"))).count();
        String rating = calculateRating(totalScore, maxScore);
        String level = switch (rating) {
            case "excellent" -> "表现优秀";
            case "good" -> "表现良好";
            case "average" -> "基本合格";
            default -> "需要加强";
        };
        StringBuilder feedback = new StringBuilder(level).append("。");
        if (wrongCount == 0) {
            feedback.append("全部决策正确，应急处置流程掌握扎实。");
        } else {
            feedback.append("共有 ").append(wrongCount).append(" 项决策需要改进，建议复习相关章节并重新训练。");
        }
        if (maxScore > 0) {
            feedback.append(" 本次得分 ").append(totalScore).append("/").append(maxScore).append("。");
        }
        return feedback.toString();
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

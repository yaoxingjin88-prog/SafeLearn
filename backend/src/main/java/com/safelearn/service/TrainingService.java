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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final ScenarioRepository scenarioRepo;
    private final TrainingRecordRepository recordRepo;
    private final UserRepository userRepo;
    private final UserProgressRepository progressRepo;
    private final ObjectMapper objectMapper;

    private static final int MASTERY_THRESHOLD = 60;

    public List<Map<String, Object>> getScenarios(String userId) {
        final Set<String> completedIds = userId != null
                ? new HashSet<>(progressRepo.findCompletedChapterIdsByUserId(userId))
                : new HashSet<>();
        final Set<String> qualifiedIds = userId != null
                ? new HashSet<>(progressRepo.findQualifiedChapterIdsByUserId(userId, MASTERY_THRESHOLD))
                : new HashSet<>();
        return scenarioRepo.findAll().stream().map(s -> {
            Map<String, Object> m = toScenarioInfo(s);
            m.put("unlocked", isScenarioUnlocked(s, userId, completedIds, qualifiedIds));
            return m;
        }).toList();
    }

    public Map<String, Object> getScenarioById(String id) {
        Scenario s = scenarioRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("场景不存在"));
        return toScenarioInfo(s);
    }

    public Map<String, Object> startTraining(String userId, String scenarioId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        Scenario scenario = scenarioRepo.findById(scenarioId).orElseThrow(() -> new RuntimeException("场景不存在"));

        List<String> prereqIds = parsePrereqIds(scenario.getPrerequisiteIds());
        if (!prereqIds.isEmpty()) {
            for (String prereqId : prereqIds) {
                if (scenario.getDifficulty() != null && scenario.getDifficulty() > DifficultyLevel.BASIC) {
                    boolean met = progressRepo.existsByUserIdAndChapterIdAndCompletedWithMastery(
                            userId, prereqId, MASTERY_THRESHOLD);
                    if (!met) {
                        throw new RuntimeException("请先完成前置章节并达到掌握度" + MASTERY_THRESHOLD + "%");
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

    public Map<String, Object> submitDecision(String userId, TrainingDecisionRequest req) {
        TrainingRecord record = recordRepo.findById(req.getRecordId())
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
            decisions.add(entry);
            saveDecisions(record, decisions);
        }

        int totalScore = decisions.stream().mapToInt(d -> toInt(d.get("score"))).sum();
        record.setTotalScore(totalScore);

        Map<String, Object> nextDecisionPoint = null;
        if (decisions.size() < decisionPoints.size()) {
            nextDecisionPoint = decisionPoints.get(decisions.size());
        } else {
            finalizeRecord(record, decisions, decisionPoints);
        }
        recordRepo.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("consequence", consequence);
        result.put("totalScore", totalScore);
        result.put("nextDecisionPoint", nextDecisionPoint);
        return result;
    }

    public List<Map<String, Object>> getRecords(String userId) {
        return recordRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toRecordInfo).toList();
    }

    public Map<String, Object> getRecordById(String id) {
        TrainingRecord record = recordRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("记录不存在"));
        return toRecordDetail(record);
    }

    public Map<String, Object> getRecordById(String userId, String id) {
        TrainingRecord record = recordRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("记录不存在"));
        if (!record.getUser().getId().equals(userId)) {
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
        m.put("decisionPoints", parseJson(s.getDecisionPoints()));
        return m;
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
        m.put("feedback", r.getFeedback());
        return m;
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
    private List<Map<String, Object>> getDecisionPoints(Scenario scenario) {
        Object parsed = parseJson(scenario.getDecisionPoints());
        if (parsed instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
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

    private void finalizeRecord(TrainingRecord record, List<Map<String, Object>> decisions,
                                List<Map<String, Object>> decisionPoints) {
        record.setEndTime(LocalDateTime.now());
        int totalScore = record.getTotalScore() != null ? record.getTotalScore() : 0;
        int maxScore = decisionPoints.stream()
                .mapToInt(dp -> maxOptionScore(dp))
                .sum();
        record.setRating(calculateRating(totalScore, maxScore));
        record.setFeedback(buildFeedback(decisions, totalScore, maxScore));
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

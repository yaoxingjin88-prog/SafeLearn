package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.ExamPaper;
import com.safelearn.entity.Question;
import com.safelearn.entity.QuestionCategory;
import com.safelearn.repository.ExamPaperRepository;
import com.safelearn.repository.QuestionCategoryRepository;
import com.safelearn.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPaperAssemblyService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<String> QUESTION_TYPES = List.of("single", "multiple", "truefalse", "short", "case");

    private final ExamPaperRepository paperRepo;
    private final QuestionRepository questionRepo;
    private final QuestionCategoryRepository categoryRepo;
    private final ObjectMapper objectMapper;
    private final AdminNotificationService notificationService;

    public List<Map<String, Object>> getCategoryOptions() {
        return categoryRepo.findByParentIdIsNullOrderBySortOrderAscNameAsc().stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    return m;
                })
                .toList();
    }

    public Map<String, Object> getDefaultConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("categoryIds", categoryRepo.findByParentIdIsNullOrderBySortOrderAscNameAsc().stream()
                .map(QuestionCategory::getId).limit(4).toList());
        config.put("excludeRecent", true);
        config.put("excludeMonths", 3);
        config.put("typeRules", defaultTypeRules());
        config.put("examSettings", defaultExamSettings());
        config.put("antiCheat", defaultAntiCheat());
        config.put("publish", defaultPublish());
        return config;
    }

    public Map<String, Object> generatePaper(Map<String, Object> body) {
        Map<String, Object> config = extractConfig(body);
        List<Map<String, Object>> typeRules = castList(config.get("typeRules"));

        Set<String> categoryIds = resolveCategoryIds(castStringList(config.get("categoryIds")));
        Set<String> excludedIds = Boolean.TRUE.equals(config.get("excludeRecent"))
                ? collectRecentQuestionIds(toInt(config.get("excludeMonths"), 3))
                : Set.of();

        List<Question> pool = questionRepo.findAll().stream()
                .filter(q -> "published".equals(q.getStatus()))
                .filter(q -> categoryIds.isEmpty() || categoryIds.contains(q.getCategoryId()))
                .filter(q -> !excludedIds.contains(q.getId()))
                .toList();

        List<String> selectedIds = new ArrayList<>();
        List<Map<String, Object>> snapshots = new ArrayList<>();
        Map<String, Integer> typeDistribution = new LinkedHashMap<>();

        for (Map<String, Object> rule : typeRules) {
            String type = String.valueOf(rule.get("type"));
            int count = toInt(rule.get("count"), 0);
            if (count <= 0) continue;

            Map<String, Integer> ratio = parseDifficultyRatio(rule.get("difficultyRatio"));
            List<Question> typePool = pool.stream()
                    .filter(q -> type.equals(q.getType()))
                    .filter(q -> !selectedIds.contains(q.getId()))
                    .toList();

            List<Question> picked = pickByDifficulty(typePool, count, ratio);
            for (Question q : picked) {
                selectedIds.add(q.getId());
                snapshots.add(toQuestionSnapshot(q, toInt(rule.get("scorePerQuestion"), 1)));
            }
            typeDistribution.put(type, picked.size());
        }

        Map<String, Object> preview = buildPreview(typeRules, selectedIds, typeDistribution);
        preview.put("questionIds", selectedIds);
        preview.put("questions", snapshots);
        return preview;
    }

    public Map<String, Object> getPaperById(String id) {
        return toDetail(paperRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("试卷不存在")));
    }

    @Transactional
    public Map<String, Object> savePaper(Map<String, Object> body) {
        String id = body.get("id") != null ? String.valueOf(body.get("id")) : null;
        ExamPaper paper = id != null
                ? paperRepo.findById(id).orElseThrow(() -> new RuntimeException("试卷不存在"))
                : new ExamPaper();

        applyPaperFields(paper, body);
        if (paper.getStatus() == null || paper.getStatus().isBlank()) {
            paper.setStatus("draft");
        }
        return toDetail(paperRepo.save(paper));
    }

    @Transactional
    public Map<String, Object> publishPaper(String id) {
        ExamPaper paper = paperRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("试卷不存在"));
        List<String> questionIds = parseQuestionIds(paper.getQuestionIds());
        if (questionIds.isEmpty()) {
            throw new RuntimeException("请先组卷后再发布");
        }
        boolean wasPublished = "published".equals(paper.getStatus());
        paper.setStatus("published");
        paper.setPublishedAt(LocalDateTime.now());
        if (paper.getQuestionsSnapshot() == null || paper.getQuestionsSnapshot().isBlank()) {
            paper.setQuestionsSnapshot(buildSnapshotJson(questionIds, paper.getConfig()));
        }
        ExamPaper saved = paperRepo.save(paper);
        if (!wasPublished) {
            String title = saved.getTitle() != null ? saved.getTitle() : "未命名考试";
            String body = "《" + title + "》已发布，共 " + questionIds.size() + " 题，请及时通知相关人员参加。";
            notificationService.createMessage("paper-publish:" + saved.getId(), "exam",
                    "考试《" + title + "》已发布", body, "/admin/learning/exams", false, null);
            notificationService.createNotification("paper-publish-notice:" + saved.getId(), "exam", "info",
                    "考试发布提醒", body, "/admin/learning/exams", null);
        }
        return toDetail(saved);
    }

    @Transactional
    public Map<String, Object> publishDirect(Map<String, Object> body) {
        Map<String, Object> saved = savePaper(body);
        return publishPaper(String.valueOf(saved.get("id")));
    }

    private void applyPaperFields(ExamPaper paper, Map<String, Object> body) {
        if (body.containsKey("title")) paper.setTitle(String.valueOf(body.get("title")));
        if (body.containsKey("mode")) paper.setMode(String.valueOf(body.get("mode")));
        if (body.containsKey("examType")) paper.setExamType(String.valueOf(body.get("examType")));
        if (body.containsKey("status")) paper.setStatus(String.valueOf(body.get("status")));
        if (body.containsKey("timeLimit")) paper.setTimeLimit(toInt(body.get("timeLimit"), 60));
        if (body.containsKey("totalScore")) paper.setTotalScore(toInt(body.get("totalScore"), 100));
        if (body.containsKey("passScore")) paper.setPassScore(toInt(body.get("passScore"), 60));
        if (body.containsKey("department")) paper.setDepartment(String.valueOf(body.get("department")));
        if (body.containsKey("config")) paper.setConfig(normalizeJson(body.get("config")));
        if (body.containsKey("questionIds")) paper.setQuestionIds(normalizeJson(body.get("questionIds")));
        if (body.containsKey("questionsSnapshot")) paper.setQuestionsSnapshot(normalizeJson(body.get("questionsSnapshot")));

        Map<String, Object> config = extractConfig(body);
        if (body.containsKey("config") || paper.getConfig() == null) {
            paper.setConfig(normalizeJson(config));
        }
        Map<String, Object> examSettings = castMap(config.get("examSettings"));
        if (examSettings != null) {
            if (examSettings.get("title") != null) paper.setTitle(String.valueOf(examSettings.get("title")));
            if (examSettings.get("examType") != null) paper.setExamType(String.valueOf(examSettings.get("examType")));
            if (examSettings.get("timeLimit") != null) paper.setTimeLimit(toInt(examSettings.get("timeLimit"), 60));
            if (examSettings.get("totalScore") != null) paper.setTotalScore(toInt(examSettings.get("totalScore"), 100));
            if (examSettings.get("passScore") != null) paper.setPassScore(toInt(examSettings.get("passScore"), 60));
        }
        Map<String, Object> publish = castMap(config.get("publish"));
        if (publish != null && "department".equals(String.valueOf(publish.get("scope")))) {
            paper.setDepartment("指定部门/人员");
        } else if (publish != null) {
            paper.setDepartment("全员");
        }
    }

    private Map<String, Object> toDetail(ExamPaper paper) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", paper.getId());
        m.put("title", paper.getTitle());
        m.put("mode", paper.getMode());
        m.put("examType", paper.getExamType());
        m.put("status", paper.getStatus());
        m.put("timeLimit", paper.getTimeLimit());
        m.put("totalScore", paper.getTotalScore());
        m.put("passScore", paper.getPassScore());
        m.put("department", paper.getDepartment());
        m.put("config", parseJsonMap(paper.getConfig()));
        m.put("questionIds", parseQuestionIds(paper.getQuestionIds()));
        m.put("questions", parseJsonList(paper.getQuestionsSnapshot()));
        m.put("preview", buildPreviewFromPaper(paper));
        m.put("publishedAt", paper.getPublishedAt() != null ? paper.getPublishedAt().format(DT) : null);
        m.put("createdAt", paper.getCreatedAt() != null ? paper.getCreatedAt().format(DT) : null);
        m.put("updatedAt", paper.getUpdatedAt() != null ? paper.getUpdatedAt().format(DT) : null);
        return m;
    }

    private Map<String, Object> buildPreviewFromPaper(ExamPaper paper) {
        Map<String, Object> config = parseJsonMap(paper.getConfig());
        List<Map<String, Object>> typeRules = castList(config.get("typeRules"));
        List<String> questionIds = parseQuestionIds(paper.getQuestionIds());
        Map<String, Integer> typeDistribution = new LinkedHashMap<>();
        if (!questionIds.isEmpty()) {
            Map<String, Question> questionMap = questionRepo.findAllById(questionIds).stream()
                    .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
            for (String qid : questionIds) {
                Question q = questionMap.get(qid);
                if (q == null) continue;
                typeDistribution.merge(q.getType(), 1, Integer::sum);
            }
        }
        return buildPreview(typeRules, questionIds, typeDistribution);
    }

    private Map<String, Object> buildPreview(List<Map<String, Object>> typeRules,
                                               List<String> questionIds,
                                               Map<String, Integer> typeDistribution) {
        int totalQuestions = questionIds.size();
        int totalScore = 0;
        List<Map<String, Object>> distribution = new ArrayList<>();

        for (Map<String, Object> rule : typeRules) {
            String type = String.valueOf(rule.get("type"));
            int count = typeDistribution.getOrDefault(type, toInt(rule.get("count"), 0));
            int scorePer = toInt(rule.get("scorePerQuestion"), 1);
            int subtotal = count * scorePer;
            totalScore += subtotal;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", type);
            item.put("typeLabel", typeLabel(type));
            item.put("count", count);
            item.put("scorePerQuestion", scorePer);
            item.put("subtotal", subtotal);
            item.put("difficultyRatio", rule.get("difficultyRatio"));
            item.put("percent", totalQuestions == 0 ? 0 : round(count * 100.0 / totalQuestions, 1));
            distribution.add(item);
        }

        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("totalQuestions", totalQuestions);
        preview.put("totalScore", totalScore);
        preview.put("distribution", distribution);
        return preview;
    }

    private List<Question> pickByDifficulty(List<Question> pool, int count, Map<String, Integer> ratio) {
        if (pool.isEmpty() || count <= 0) return List.of();

        int easyCount = count * ratio.getOrDefault("easy", 0) / 100;
        int mediumCount = count * ratio.getOrDefault("medium", 0) / 100;
        int hardCount = count - easyCount - mediumCount;

        Map<String, List<Question>> buckets = new LinkedHashMap<>();
        buckets.put("easy", shuffle(pool.stream().filter(q -> "easy".equals(q.getDifficulty())).toList()));
        buckets.put("medium", shuffle(pool.stream().filter(q -> "medium".equals(q.getDifficulty())).toList()));
        buckets.put("hard", shuffle(pool.stream().filter(q -> "hard".equals(q.getDifficulty())).toList()));

        List<Question> picked = new ArrayList<>();
        picked.addAll(take(buckets.get("easy"), easyCount));
        picked.addAll(take(buckets.get("medium"), mediumCount));
        picked.addAll(take(buckets.get("hard"), hardCount));

        if (picked.size() < count) {
            Set<String> used = picked.stream().map(Question::getId).collect(Collectors.toSet());
            List<Question> remain = shuffle(pool.stream().filter(q -> !used.contains(q.getId())).toList());
            picked.addAll(take(remain, count - picked.size()));
        }
        return picked.size() > count ? picked.subList(0, count) : picked;
    }

    private List<Question> take(List<Question> list, int count) {
        return list.size() <= count ? new ArrayList<>(list) : new ArrayList<>(list.subList(0, count));
    }

    private List<Question> shuffle(List<Question> list) {
        List<Question> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        return copy;
    }

    private Set<String> collectRecentQuestionIds(int months) {
        LocalDateTime since = LocalDateTime.now().minusMonths(Math.max(1, months));
        Set<String> ids = new HashSet<>();
        for (ExamPaper paper : paperRepo.findAll()) {
            if (!"published".equals(paper.getStatus())) continue;
            LocalDateTime ref = paper.getPublishedAt() != null ? paper.getPublishedAt() : paper.getUpdatedAt();
            if (ref == null || ref.isBefore(since)) continue;
            ids.addAll(parseQuestionIds(paper.getQuestionIds()));
        }
        return ids;
    }

    private Set<String> resolveCategoryIds(List<String> rootIds) {
        if (rootIds == null || rootIds.isEmpty()) return Set.of();
        List<QuestionCategory> all = categoryRepo.findAll();
        Set<String> ids = new HashSet<>();
        for (String rootId : rootIds) {
            ids.add(rootId);
            collectDescendants(rootId, all, ids);
        }
        return ids;
    }

    private void collectDescendants(String parentId, List<QuestionCategory> all, Set<String> ids) {
        for (QuestionCategory c : all) {
            if (parentId.equals(c.getParentId())) {
                ids.add(c.getId());
                collectDescendants(c.getId(), all, ids);
            }
        }
    }

    private Map<String, Object> toQuestionSnapshot(Question q, int score) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.getId());
        m.put("type", q.getType());
        m.put("question", q.getContent());
        m.put("options", parseJsonList(q.getOptions()));
        m.put("explanation", q.getExplanation());
        m.put("score", score);
        return m;
    }

    private String buildSnapshotJson(List<String> questionIds, String configJson) {
        Map<String, Object> config = parseJsonMap(configJson);
        List<Map<String, Object>> typeRules = castList(config.get("typeRules"));
        Map<String, Integer> scoreMap = new HashMap<>();
        for (Map<String, Object> rule : typeRules) {
            scoreMap.put(String.valueOf(rule.get("type")), toInt(rule.get("scorePerQuestion"), 1));
        }
        List<Map<String, Object>> snapshots = new ArrayList<>();
        for (String qid : questionIds) {
            questionRepo.findById(qid).ifPresent(q -> {
                snapshots.add(toQuestionSnapshot(q, scoreMap.getOrDefault(q.getType(), 1)));
            });
        }
        try {
            return objectMapper.writeValueAsString(snapshots);
        } catch (Exception e) {
            throw new RuntimeException("生成试卷快照失败");
        }
    }

    private List<Map<String, Object>> defaultTypeRules() {
        List<Map<String, Object>> rules = new ArrayList<>();
        rules.add(typeRule("single", 10, 2, 30, 50, 20));
        rules.add(typeRule("multiple", 5, 3, 20, 50, 30));
        rules.add(typeRule("truefalse", 5, 1, 30, 50, 20));
        rules.add(typeRule("short", 2, 6, 20, 60, 20));
        rules.add(typeRule("case", 1, 10, 10, 60, 30));
        return rules;
    }

    private Map<String, Object> typeRule(String type, int count, int score, int easy, int medium, int hard) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("type", type);
        rule.put("count", count);
        rule.put("scorePerQuestion", score);
        Map<String, Integer> ratio = new LinkedHashMap<>();
        ratio.put("easy", easy);
        ratio.put("medium", medium);
        ratio.put("hard", hard);
        rule.put("difficultyRatio", ratio);
        return rule;
    }

    private Map<String, Object> defaultExamSettings() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", "储能电站消防安全考试");
        m.put("examType", "formal");
        m.put("timeLimit", 60);
        m.put("totalScore", 0);
        m.put("passScore", 0);
        m.put("passPercent", 60);
        m.put("resultView", "score_only");
        m.put("description", "请认真作答，考试结束后可查看错题及解析。");
        return m;
    }

    private Map<String, Object> defaultAntiCheat() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("shuffleQuestions", true);
        m.put("shuffleOptions", true);
        m.put("preventSwitchScreen", true);
        m.put("fullscreen", true);
        m.put("switchLimit", 3);
        m.put("attemptLimit", 2);
        m.put("autoSubmitOnTimeout", true);
        return m;
    }

    private Map<String, Object> defaultPublish() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mode", "immediate");
        m.put("scheduledAt", null);
        m.put("scope", "all");
        return m;
    }

    private Map<String, Integer> parseDifficultyRatio(Object raw) {
        Map<String, Integer> ratio = new LinkedHashMap<>();
        ratio.put("easy", 30);
        ratio.put("medium", 50);
        ratio.put("hard", 20);
        Map<String, Object> map = castMap(raw);
        if (map == null) return ratio;
        for (String key : List.of("easy", "medium", "hard")) {
            if (map.containsKey(key)) ratio.put(key, toInt(map.get(key), ratio.get(key)));
        }
        return ratio;
    }

    private String typeLabel(String type) {
        return switch (type) {
            case "single" -> "单选题";
            case "multiple" -> "多选题";
            case "truefalse" -> "判断题";
            case "short" -> "简答题";
            case "case" -> "案例分析题";
            default -> type;
        };
    }

    private Map<String, Object> extractConfig(Map<String, Object> body) {
        Object config = body.get("config");
        if (config instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        Map<String, Object> merged = getDefaultConfig();
        if (body.containsKey("categoryIds")) merged.put("categoryIds", body.get("categoryIds"));
        if (body.containsKey("typeRules")) merged.put("typeRules", body.get("typeRules"));
        return merged;
    }

    private String normalizeJson(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("JSON 格式无效");
        }
    }

    private List<String> parseQuestionIds(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private List<Map<String, Object>> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().filter(Map.class::isInstance).map(v -> (Map<String, Object>) v).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        return null;
    }

    private List<String> castStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}

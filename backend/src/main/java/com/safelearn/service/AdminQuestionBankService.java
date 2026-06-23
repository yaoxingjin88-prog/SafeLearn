package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.Question;
import com.safelearn.entity.QuestionCategory;
import com.safelearn.repository.QuestionCategoryRepository;
import com.safelearn.repository.QuestionRepository;
import com.safelearn.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminQuestionBankService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Set<String> VALID_TYPES = Set.of("single", "multiple", "truefalse", "short", "case");
    private static final Set<String> VALID_DIFFICULTIES = Set.of("easy", "medium", "hard");
    private static final Set<String> VALID_STATUSES = Set.of("draft", "published", "disabled");

    private final QuestionRepository questionRepo;
    private final QuestionCategoryRepository categoryRepo;
    private final QuizAttemptRepository attemptRepo;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> getCategoryTree(String keyword) {
        List<QuestionCategory> all = categoryRepo.findAllByOrderBySortOrderAscNameAsc();
        Map<String, Long> directCounts = all.stream()
                .collect(Collectors.toMap(QuestionCategory::getId, c -> questionRepo.countByCategoryId(c.getId())));

        Map<String, List<QuestionCategory>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(QuestionCategory::getParentId));

        List<QuestionCategory> roots = all.stream()
                .filter(c -> c.getParentId() == null)
                .sorted(Comparator.comparing(QuestionCategory::getSortOrder).thenComparing(QuestionCategory::getName))
                .toList();

        long totalCount = questionRepo.count();
        List<Map<String, Object>> tree = new ArrayList<>();
        tree.add(Map.of(
                "id", "all",
                "name", "全部题库",
                "parentId", "",
                "questionCount", totalCount,
                "children", List.of()
        ));

        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        for (QuestionCategory root : roots) {
            Map<String, Object> node = buildCategoryNode(root, childrenMap, directCounts, kw);
            if (node != null) tree.add(node);
        }
        return tree;
    }

    public Map<String, Object> searchQuestions(String categoryId, String type, String difficulty,
                                                String status, String tags, String keyword,
                                                int page, int pageSize) {
        Set<String> categoryIds = resolveCategoryIds(categoryId);
        List<Question> all = questionRepo.findAll().stream()
                .filter(q -> categoryIds == null || categoryIds.contains(q.getCategoryId()))
                .filter(q -> matchType(q, type))
                .filter(q -> matchDifficulty(q, difficulty))
                .filter(q -> matchStatus(q, status))
                .filter(q -> matchTags(q, tags))
                .filter(q -> matchKeyword(q, keyword))
                .sorted(Comparator.comparing(Question::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int total = all.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        List<Map<String, Object>> items = (from >= total ? List.<Question>of() : all.subList(from, to)).stream()
                .map(this::toListItem)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", pageSize == 0 ? 0 : (int) Math.ceil(total * 1.0 / pageSize));
        return result;
    }

    public Map<String, Long> getTypeCounts(String categoryId, String difficulty, String status, String tags, String keyword) {
        Set<String> categoryIds = resolveCategoryIds(categoryId);
        List<Question> filtered = questionRepo.findAll().stream()
                .filter(q -> categoryIds == null || categoryIds.contains(q.getCategoryId()))
                .filter(q -> matchDifficulty(q, difficulty))
                .filter(q -> matchStatus(q, status))
                .filter(q -> matchTags(q, tags))
                .filter(q -> matchKeyword(q, keyword))
                .toList();

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("all", (long) filtered.size());
        for (String t : List.of("single", "multiple", "truefalse", "short", "case")) {
            counts.put(t, filtered.stream().filter(q -> t.equals(q.getType())).count());
        }
        return counts;
    }

    public List<String> getAllTags() {
        return questionRepo.findAll().stream()
                .flatMap(q -> parseTags(q.getTags()).stream())
                .distinct()
                .sorted()
                .toList();
    }

    public Map<String, Object> getQuestionById(String id) {
        return toDetail(questionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在")));
    }

    @Transactional
    public Map<String, Object> createQuestion(Map<String, Object> body) {
        Question q = new Question();
        applyFields(q, body, true);
        return toDetail(questionRepo.save(q));
    }

    @Transactional
    public Map<String, Object> updateQuestion(String id, Map<String, Object> body) {
        Question q = questionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在"));
        applyFields(q, body, false);
        return toDetail(questionRepo.save(q));
    }

    @Transactional
    public Map<String, Object> deleteQuestion(String id) {
        if (!questionRepo.existsById(id)) {
            throw new RuntimeException("题目不存在");
        }
        questionRepo.deleteById(id);
        return Map.of("success", true);
    }

    @Transactional
    public Map<String, Object> batchOperate(List<String> ids, String action) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("请选择题目");
        }
        List<Question> questions = questionRepo.findAllById(ids);
        if (questions.isEmpty()) {
            throw new RuntimeException("未找到所选题目");
        }
        switch (action) {
            case "delete" -> questionRepo.deleteAll(questions);
            case "publish" -> questions.forEach(q -> q.setStatus("published"));
            case "disable" -> questions.forEach(q -> q.setStatus("disabled"));
            default -> throw new RuntimeException("不支持的操作: " + action);
        }
        if (!"delete".equals(action)) {
            questionRepo.saveAll(questions);
        }
        return Map.of("success", true, "count", questions.size());
    }

    private Map<String, Object> buildCategoryNode(QuestionCategory cat,
                                                   Map<String, List<QuestionCategory>> childrenMap,
                                                   Map<String, Long> directCounts,
                                                   String keyword) {
        List<QuestionCategory> children = childrenMap.getOrDefault(cat.getId(), List.of()).stream()
                .sorted(Comparator.comparing(QuestionCategory::getSortOrder).thenComparing(QuestionCategory::getName))
                .toList();

        List<Map<String, Object>> childNodes = new ArrayList<>();
        long childTotal = 0;
        for (QuestionCategory child : children) {
            Map<String, Object> childNode = buildCategoryNode(child, childrenMap, directCounts, keyword);
            if (childNode != null) {
                childNodes.add(childNode);
                childTotal += ((Number) childNode.get("questionCount")).longValue();
            }
        }

        long direct = directCounts.getOrDefault(cat.getId(), 0L);
        long total = direct + childTotal;

        boolean nameMatch = keyword.isEmpty() || cat.getName().toLowerCase().contains(keyword);
        boolean childMatch = !childNodes.isEmpty();
        if (!nameMatch && !childMatch) return null;

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", cat.getId());
        node.put("name", cat.getName());
        node.put("parentId", cat.getParentId() != null ? cat.getParentId() : "");
        node.put("questionCount", total);
        node.put("children", childNodes);
        return node;
    }

    private Set<String> resolveCategoryIds(String categoryId) {
        if (categoryId == null || categoryId.isBlank() || "all".equals(categoryId)) {
            return null;
        }
        List<QuestionCategory> all = categoryRepo.findAll();
        Set<String> ids = new HashSet<>();
        collectDescendantIds(categoryId, all, ids);
        ids.add(categoryId);
        return ids;
    }

    private void collectDescendantIds(String parentId, List<QuestionCategory> all, Set<String> ids) {
        for (QuestionCategory c : all) {
            if (parentId.equals(c.getParentId())) {
                ids.add(c.getId());
                collectDescendantIds(c.getId(), all, ids);
            }
        }
    }

    private boolean matchType(Question q, String type) {
        return type == null || type.isBlank() || "all".equals(type) || type.equals(q.getType());
    }

    private boolean matchDifficulty(Question q, String difficulty) {
        return difficulty == null || difficulty.isBlank() || "all".equals(difficulty) || difficulty.equals(q.getDifficulty());
    }

    private boolean matchStatus(Question q, String status) {
        return status == null || status.isBlank() || "all".equals(status) || status.equals(q.getStatus());
    }

    private boolean matchTags(Question q, String tags) {
        if (tags == null || tags.isBlank()) return true;
        List<String> filterTags = Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (filterTags.isEmpty()) return true;
        List<String> questionTags = parseTags(q.getTags());
        return filterTags.stream().anyMatch(questionTags::contains);
    }

    private boolean matchKeyword(Question q, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String kw = keyword.trim().toLowerCase();
        if (q.getContent() != null && q.getContent().toLowerCase().contains(kw)) return true;
        return parseTags(q.getTags()).stream().anyMatch(t -> t.toLowerCase().contains(kw));
    }

    private void applyFields(Question q, Map<String, Object> body, boolean creating) {
        if (body.containsKey("categoryId") || creating) {
            String categoryId = String.valueOf(body.get("categoryId"));
            if (categoryId.isBlank() || "null".equals(categoryId)) {
                throw new RuntimeException("请选择分类");
            }
            if (!categoryRepo.existsById(categoryId)) {
                throw new RuntimeException("分类不存在");
            }
            q.setCategoryId(categoryId);
        }
        if (body.containsKey("type") || creating) {
            String type = String.valueOf(body.get("type"));
            if (!VALID_TYPES.contains(type)) throw new RuntimeException("题目类型无效");
            q.setType(type);
        }
        if (body.containsKey("difficulty")) {
            String difficulty = String.valueOf(body.get("difficulty"));
            if (!VALID_DIFFICULTIES.contains(difficulty)) throw new RuntimeException("难度无效");
            q.setDifficulty(difficulty);
        } else if (creating) {
            q.setDifficulty("medium");
        }
        if (body.containsKey("status")) {
            String status = String.valueOf(body.get("status"));
            if (!VALID_STATUSES.contains(status)) throw new RuntimeException("状态无效");
            q.setStatus(status);
        } else if (creating) {
            q.setStatus("published");
        }
        if (body.containsKey("content") || creating) {
            String content = String.valueOf(body.get("content")).trim();
            if (content.isBlank()) throw new RuntimeException("题干不能为空");
            q.setContent(content);
        }
        if (body.containsKey("options")) {
            q.setOptions(normalizeJson(body.get("options")));
        }
        if (body.containsKey("explanation")) {
            q.setExplanation(body.get("explanation") == null ? null : String.valueOf(body.get("explanation")));
        }
        if (body.containsKey("tags")) {
            q.setTags(normalizeJson(body.get("tags")));
        }
        if (body.containsKey("attachments")) {
            q.setAttachments(normalizeJson(body.get("attachments")));
        }
        if (body.containsKey("settings")) {
            q.setSettings(normalizeJson(body.get("settings")));
        }
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

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> toListItem(Question q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.getId());
        m.put("content", q.getContent());
        m.put("type", q.getType());
        m.put("difficulty", q.getDifficulty());
        m.put("status", q.getStatus());
        m.put("tags", parseTags(q.getTags()));
        m.put("usageCount", resolveUsageCount(q));
        m.put("categoryId", q.getCategoryId());
        m.put("categoryName", categoryRepo.findById(q.getCategoryId()).map(QuestionCategory::getName).orElse(""));
        m.put("updatedAt", q.getUpdatedAt() != null ? q.getUpdatedAt().format(DATE) : "");
        return m;
    }

    private Map<String, Object> toDetail(Question q) {
        Map<String, Object> m = toListItem(q);
        m.put("options", parseOptions(q.getOptions()));
        m.put("explanation", q.getExplanation());
        m.put("attachments", parseAttachments(q.getAttachments()));
        m.put("settings", parseSettings(q.getSettings()));
        m.put("categoryPath", buildCategoryPath(q.getCategoryId()));
        m.put("createdAt", q.getCreatedAt() != null ? q.getCreatedAt().format(DATE) : "");
        return m;
    }

    private List<Map<String, Object>> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> parseAttachments(String attachmentsJson) {
        if (attachmentsJson == null || attachmentsJson.isBlank()) {
            return defaultAttachments();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(attachmentsJson, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> result = defaultAttachments();
            result.putAll(parsed);
            return result;
        } catch (Exception e) {
            return defaultAttachments();
        }
    }

    private Map<String, Object> defaultAttachments() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("files", List.of());
        m.put("images", List.of());
        m.put("norms", List.of());
        return m;
    }

    private Map<String, Object> parseSettings(String settingsJson) {
        if (settingsJson == null || settingsJson.isBlank()) {
            return defaultSettings();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(settingsJson, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> result = defaultSettings();
            result.putAll(parsed);
            return result;
        } catch (Exception e) {
            return defaultSettings();
        }
    }

    private Map<String, Object> defaultSettings() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("allowComments", false);
        m.put("allowReport", true);
        m.put("showAnalysisInExam", true);
        return m;
    }

    private String buildCategoryPath(String categoryId) {
        List<String> names = new ArrayList<>();
        String current = categoryId;
        int guard = 0;
        while (current != null && guard++ < 10) {
            Optional<QuestionCategory> cat = categoryRepo.findById(current);
            if (cat.isEmpty()) break;
            names.add(0, cat.get().getName());
            current = cat.get().getParentId();
        }
        return String.join(" / ", names);
    }

    private int resolveUsageCount(Question q) {
        if (q.getUsageCount() != null && q.getUsageCount() > 0) {
            return q.getUsageCount();
        }
        if (q.getSourceQuizId() != null) {
            return (int) attemptRepo.countByQuizId(q.getSourceQuizId());
        }
        return 0;
    }
}

package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.Chapter;
import com.safelearn.entity.Course;
import com.safelearn.entity.QuizAttempt;
import com.safelearn.repository.ChapterRepository;
import com.safelearn.repository.CourseCategoryRepository;
import com.safelearn.repository.CourseRepository;
import com.safelearn.repository.QuizAttemptRepository;
import com.safelearn.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCourseService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final CourseCategoryRepository categoryRepo;
    private final UserProgressRepository progressRepo;
    private final QuizAttemptRepository quizAttemptRepo;
    private final ObjectMapper objectMapper;

    public Map<String, Object> searchCourses(String category, String status, String keyword,
                                              String department, String createdFrom, String createdTo,
                                              int page, int pageSize) {
        List<Course> all = courseRepo.findAll().stream()
                .sorted(Comparator.comparing(Course::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Course> filtered = all.stream()
                .filter(c -> category == null || category.isBlank() || "all".equals(category) || category.equals(c.getCategory()))
                .filter(c -> status == null || status.isBlank() || "all".equals(status) || status.equals(c.getStatus()))
                .filter(c -> keyword == null || keyword.isBlank()
                        || (c.getTitle() != null && c.getTitle().contains(keyword))
                        || (c.getDescription() != null && c.getDescription().contains(keyword)))
                .filter(c -> department == null || department.isBlank() || "all".equals(department)
                        || matchDepartment(c, department))
                .filter(c -> matchDateRange(c, createdFrom, createdTo))
                .toList();

        int total = filtered.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        List<Map<String, Object>> items = filtered.subList(from, to).stream()
                .map(this::toCourseListItem)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", pageSize == 0 ? 0 : (int) Math.ceil(total * 1.0 / pageSize));
        return result;
    }

    public List<Map<String, Object>> getCourses() {
        return courseRepo.findAll().stream()
                .sorted(Comparator.comparing(Course::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toCourseListItem)
                .toList();
    }

    public Map<String, Object> getCourseById(String id) {
        Course course = courseRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        Map<String, Object> info = toCourseDetail(course);
        info.put("chapters", chapterRepo.findByCourseIdOrderByOrderNumAsc(id).stream()
                .map(this::toChapterInfo).toList());
        info.put("stats", buildCourseStats(course));
        return info;
    }

    @Transactional
    public Map<String, Object> createCourse(Map<String, Object> data) {
        Course course = new Course();
        applyCourseFields(course, data);
        if (course.getStatus() == null || course.getStatus().isBlank()) {
            course.setStatus("draft");
        }
        course = courseRepo.save(course);
        return toCourseDetail(course);
    }

    @Transactional
    public Map<String, Object> updateCourse(String id, Map<String, Object> data) {
        Course course = courseRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        applyCourseFields(course, data);
        courseRepo.save(course);
        return toCourseDetail(course);
    }

    @Transactional
    public Map<String, Object> deleteCourse(String id) {
        if (!courseRepo.existsById(id)) {
            throw new RuntimeException("课程不存在");
        }
        courseRepo.deleteById(id);
        return Map.of("success", true);
    }

    public List<Map<String, Object>> getChapters(String courseId) {
        courseRepo.findById(courseId).orElseThrow(() -> new RuntimeException("课程不存在"));
        return chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId).stream()
                .map(this::toChapterInfo)
                .toList();
    }

    @Transactional
    public Map<String, Object> createChapter(String courseId, Map<String, Object> data) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        Chapter chapter = new Chapter();
        chapter.setCourse(course);
        applyChapterFields(chapter, data);
        if (chapter.getOrderNum() == null) {
            int nextOrder = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId).size() + 1;
            chapter.setOrderNum(nextOrder);
        }
        if (chapter.getDifficultyLevel() == null) {
            chapter.setDifficultyLevel(1);
        }
        if (chapter.getContentType() == null || chapter.getContentType().isBlank()) {
            chapter.setContentType(chapter.getVideoUrl() != null && !chapter.getVideoUrl().isBlank() ? "video" : "html");
        }
        chapter = chapterRepo.save(chapter);
        refreshCourseDuration(course);
        return toChapterInfo(chapter);
    }

    @Transactional
    public Map<String, Object> updateChapter(String courseId, String chapterId, Map<String, Object> data) {
        Chapter chapter = chapterRepo.findByIdAndCourseId(chapterId, courseId)
                .orElseThrow(() -> new RuntimeException("章节不存在"));
        applyChapterFields(chapter, data);
        chapterRepo.save(chapter);
        refreshCourseDuration(chapter.getCourse());
        return toChapterInfo(chapter);
    }

    @Transactional
    public Map<String, Object> deleteChapter(String courseId, String chapterId) {
        Chapter chapter = chapterRepo.findByIdAndCourseId(chapterId, courseId)
                .orElseThrow(() -> new RuntimeException("章节不存在"));
        Course course = chapter.getCourse();
        chapterRepo.delete(chapter);
        refreshCourseDuration(course);
        return Map.of("success", true);
    }

    private boolean matchDepartment(Course course, String department) {
        Map<String, Object> meta = parseMetadata(course.getExtraMetadata());
        Object depts = meta.get("targetDepartments");
        if (!(depts instanceof List<?> list) || list.isEmpty()) {
            return true;
        }
        return list.stream().anyMatch(item -> department.equals(String.valueOf(item)));
    }

    private boolean matchDateRange(Course course, String from, String to) {
        if (course.getCreatedAt() == null) return true;
        LocalDate created = course.getCreatedAt().toLocalDate();
        if (from != null && !from.isBlank()) {
            LocalDate start = LocalDate.parse(from);
            if (created.isBefore(start)) return false;
        }
        if (to != null && !to.isBlank()) {
            LocalDate end = LocalDate.parse(to);
            if (created.isAfter(end)) return false;
        }
        return true;
    }

    private Map<String, Object> buildCourseStats(Course course) {
        String courseId = course.getId();
        int chapterCount = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId).size();
        long learnerCount = progressRepo.countDistinctUsersByCourseId(courseId);
        long completedCount = countFullyCompletedUsers(courseId, chapterCount);
        double completionRate = learnerCount == 0 ? 0 : round(completedCount * 100.0 / learnerCount, 1);

        Set<String> chapterIds = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId).stream()
                .map(Chapter::getId).collect(Collectors.toSet());
        List<QuizAttempt> attempts = quizAttemptRepo.findAll().stream()
                .filter(a -> a.getQuiz() != null && chapterIds.contains(a.getQuiz().getChapterId()))
                .toList();
        double avgScore = attempts.isEmpty() ? 0 : round(attempts.stream()
                .mapToInt(a -> a.getScore() != null ? a.getScore() : 0).average().orElse(0), 1);
        long passed = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();
        double positiveRate = attempts.isEmpty() ? 0 : round(passed * 100.0 / attempts.size(), 1);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("learnerCount", learnerCount);
        stats.put("completedCount", completedCount);
        stats.put("completionRate", completionRate);
        stats.put("avgScore", avgScore);
        stats.put("positiveRate", positiveRate);
        return stats;
    }

    private long countFullyCompletedUsers(String courseId, int chapterCount) {
        if (chapterCount == 0) return 0;
        return progressRepo.findAll().stream()
                .filter(p -> p.getCourse() != null && courseId.equals(p.getCourse().getId()))
                .filter(p -> Boolean.TRUE.equals(p.getCompleted()))
                .collect(Collectors.groupingBy(p -> p.getUser().getId(), Collectors.counting()))
                .values().stream()
                .filter(count -> count >= chapterCount)
                .count();
    }

    private void applyCourseFields(Course course, Map<String, Object> data) {
        if (data.containsKey("title")) course.setTitle((String) data.get("title"));
        if (data.containsKey("description")) course.setDescription((String) data.get("description"));
        if (data.containsKey("coverImage")) course.setCoverImage((String) data.get("coverImage"));
        if (data.containsKey("category")) course.setCategory((String) data.get("category"));
        if (data.containsKey("status")) course.setStatus((String) data.get("status"));
        if (data.containsKey("totalDuration")) course.setTotalDuration(toInteger(data.get("totalDuration")));

        Map<String, Object> meta = parseMetadata(course.getExtraMetadata());
        mergeMetadata(meta, data);
        course.setExtraMetadata(serializeMetadata(meta));
    }

    @SuppressWarnings("unchecked")
    private void mergeMetadata(Map<String, Object> meta, Map<String, Object> data) {
        if (data.containsKey("subtitle")) meta.put("subtitle", data.get("subtitle"));
        if (data.containsKey("instructor")) meta.put("instructor", data.get("instructor"));
        if (data.containsKey("instructorTitle")) meta.put("instructorTitle", data.get("instructorTitle"));
        if (data.containsKey("targetAudience")) meta.put("targetAudience", data.get("targetAudience"));
        if (data.containsKey("objectives")) meta.put("objectives", data.get("objectives"));
        if (data.containsKey("tags")) meta.put("tags", data.get("tags"));
        if (data.containsKey("knowledgePoints")) meta.put("knowledgePoints", data.get("knowledgePoints"));
        if (data.containsKey("targetDepartments")) meta.put("targetDepartments", data.get("targetDepartments"));
        if (data.containsKey("targetRoles")) meta.put("targetRoles", data.get("targetRoles"));
        if (data.containsKey("extraMetadata") && data.get("extraMetadata") instanceof Map<?, ?> nested) {
            meta.putAll((Map<String, Object>) nested);
        }
    }

    private void applyChapterFields(Chapter chapter, Map<String, Object> data) {
        if (data.containsKey("title")) chapter.setTitle((String) data.get("title"));
        if (data.containsKey("content")) chapter.setContent((String) data.get("content"));
        if (data.containsKey("videoUrl")) chapter.setVideoUrl((String) data.get("videoUrl"));
        if (data.containsKey("summary")) chapter.setSummary((String) data.get("summary"));
        if (data.containsKey("contentType")) chapter.setContentType((String) data.get("contentType"));
        if (data.containsKey("required")) chapter.setRequired(toBoolean(data.get("required")));
        if (data.containsKey("allowDownload")) chapter.setAllowDownload(toBoolean(data.get("allowDownload")));
        if (data.containsKey("duration")) chapter.setDuration(toInteger(data.get("duration")));
        if (data.containsKey("orderNum")) chapter.setOrderNum(toInteger(data.get("orderNum")));
        if (data.containsKey("order")) chapter.setOrderNum(toInteger(data.get("order")));
        if (data.containsKey("difficultyLevel")) chapter.setDifficultyLevel(toInteger(data.get("difficultyLevel")));
        if (data.containsKey("scenarioId")) chapter.setScenarioId((String) data.get("scenarioId"));
        if (data.containsKey("prerequisiteIds")) {
            chapter.setPrerequisiteIds(serializePrerequisiteIds(data.get("prerequisiteIds")));
        }
    }

    private void refreshCourseDuration(Course course) {
        if (course == null) return;
        int total = chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId()).stream()
                .map(Chapter::getDuration)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        course.setTotalDuration(total);
        courseRepo.save(course);
    }

    private Map<String, Object> toCourseListItem(Course course) {
        Map<String, Object> info = toCourseDetail(course);
        int chapterCount = chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId()).size();
        Map<String, Object> stats = buildCourseStats(course);
        info.put("chapterCount", chapterCount);
        info.put("learnerCount", stats.get("learnerCount"));
        info.put("completedCount", stats.get("completedCount"));
        info.put("completionRate", stats.get("completionRate"));
        info.put("categoryName", resolveCategoryName(course.getCategory()));
        return info;
    }

    private Map<String, Object> toCourseDetail(Course course) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", course.getId());
        m.put("title", course.getTitle());
        m.put("description", course.getDescription());
        m.put("coverImage", course.getCoverImage());
        m.put("category", course.getCategory());
        m.put("categoryName", resolveCategoryName(course.getCategory()));
        m.put("totalDuration", course.getTotalDuration());
        m.put("status", course.getStatus());
        m.put("chapterCount", course.getChapters() != null ? course.getChapters().size() : 0);
        m.put("createdAt", course.getCreatedAt() != null ? course.getCreatedAt().format(DT) : null);
        m.put("updatedAt", course.getUpdatedAt() != null ? course.getUpdatedAt().format(DT) : null);
        m.putAll(parseMetadata(course.getExtraMetadata()));
        return m;
    }

    private Map<String, Object> toChapterInfo(Chapter chapter) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", chapter.getId());
        m.put("courseId", chapter.getCourse() != null ? chapter.getCourse().getId() : null);
        m.put("title", chapter.getTitle());
        m.put("content", chapter.getContent());
        m.put("videoUrl", chapter.getVideoUrl());
        m.put("summary", chapter.getSummary());
        m.put("contentType", chapter.getContentType() != null ? chapter.getContentType() : "html");
        m.put("required", chapter.getRequired() != null ? chapter.getRequired() : true);
        m.put("allowDownload", chapter.getAllowDownload() != null ? chapter.getAllowDownload() : false);
        m.put("duration", chapter.getDuration());
        m.put("orderNum", chapter.getOrderNum());
        m.put("order", chapter.getOrderNum());
        m.put("difficultyLevel", chapter.getDifficultyLevel());
        m.put("prerequisiteIds", parsePrerequisiteIds(chapter.getPrerequisiteIds()));
        m.put("scenarioId", chapter.getScenarioId());
        return m;
    }

    private String resolveCategoryName(String code) {
        if (code == null) return "";
        return categoryRepo.findAllByOrderBySortOrderAscNameAsc().stream()
                .filter(c -> code.equals(c.getCode()))
                .map(c -> c.getName())
                .findFirst()
                .orElse(code);
    }

    private Map<String, Object> parseMetadata(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return new LinkedHashMap<>(objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {}));
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String serializeMetadata(Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            throw new RuntimeException("课程扩展信息格式错误");
        }
    }

    private String serializePrerequisiteIds(Object value) {
        if (value == null) return null;
        if (value instanceof String str) return str.isBlank() ? null : str;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("前置章节格式错误");
        }
    }

    private List<String> parsePrerequisiteIds(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Boolean toBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(value.toString());
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}

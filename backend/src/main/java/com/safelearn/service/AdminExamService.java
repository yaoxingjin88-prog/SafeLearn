package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.Chapter;
import com.safelearn.entity.ChapterQuiz;
import com.safelearn.entity.Course;
import com.safelearn.entity.ExamPaper;
import com.safelearn.repository.*;
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
public class AdminExamService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int COMPREHENSIVE_DRAW_COUNT = 20;
    private static final int COMPREHENSIVE_PASS_SCORE = 70;
    private static final int COMPREHENSIVE_TIME_LIMIT = 45;
    private static final int COMPREHENSIVE_MIN_POOL = 5;

    private final ChapterQuizRepository quizRepo;
    private final ChapterRepository chapterRepo;
    private final CourseRepository courseRepo;
    private final QuizAttemptRepository attemptRepo;
    private final ComprehensiveExamAttemptRepository comprehensiveAttemptRepo;
    private final ExamPaperRepository paperRepo;
    private final ObjectMapper objectMapper;

    public Map<String, Object> searchExams(String keyword, String examType, String department,
                                            String status, String createdFrom, String createdTo,
                                            int page, int pageSize) {
        List<Map<String, Object>> all = buildAllExamRows().stream()
                .filter(row -> matchKeyword(row, keyword))
                .filter(row -> matchExamType(row, examType))
                .filter(row -> matchDepartment(row, department))
                .filter(row -> matchStatus(row, status))
                .filter(row -> matchDateRange(row, createdFrom, createdTo))
                .sorted(Comparator.comparing(row -> String.valueOf(row.get("createdAt")), Comparator.reverseOrder()))
                .toList();

        int total = all.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(total, from + pageSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", from >= total ? List.of() : all.subList(from, to));
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", pageSize == 0 ? 0 : (int) Math.ceil(total * 1.0 / pageSize));
        return result;
    }

    public Map<String, Object> getExamById(String id) {
        return buildAllExamRows().stream()
                .filter(row -> id.equals(String.valueOf(row.get("id"))))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("考试不存在"));
    }

    public Map<String, Object> getExamStats(String id) {
        Map<String, Object> exam = getExamById(id);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("examId", id);
        stats.put("title", exam.get("title"));

        if ("comprehensive".equals(exam.get("sourceType"))) {
            String courseId = String.valueOf(exam.get("courseId"));
            long attemptCount = comprehensiveAttemptRepo.findAll().stream()
                    .filter(a -> courseId.equals(a.getCourseId()))
                    .count();
            long passedCount = comprehensiveAttemptRepo.findAll().stream()
                    .filter(a -> courseId.equals(a.getCourseId()))
                    .filter(a -> Boolean.TRUE.equals(a.getPassed()))
                    .count();
            stats.put("attemptCount", attemptCount);
            stats.put("passedCount", passedCount);
            stats.put("passRate", attemptCount == 0 ? 0 : round(passedCount * 100.0 / attemptCount, 1));
        } else {
            long attemptCount = attemptRepo.countByQuizId(id);
            stats.put("attemptCount", attemptCount);
            stats.put("passedCount", attemptRepo.findAll().stream()
                    .filter(a -> a.getQuiz() != null && id.equals(a.getQuiz().getId()))
                    .filter(a -> Boolean.TRUE.equals(a.getPassed()))
                    .count());
            stats.put("passRate", attemptCount == 0 ? 0 : round(
                    attemptRepo.findAll().stream()
                            .filter(a -> a.getQuiz() != null && id.equals(a.getQuiz().getId()))
                            .filter(a -> Boolean.TRUE.equals(a.getPassed()))
                            .count() * 100.0 / attemptCount, 1));
        }
        return stats;
    }

    @Transactional
    public Map<String, Object> updateExam(String id, Map<String, Object> body) {
        if (id.startsWith("comprehensive:")) {
            throw new RuntimeException("综合考试暂不支持编辑，请在课程配置中管理");
        }
        ChapterQuiz quiz = quizRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("考试不存在"));

        if (body.containsKey("title")) quiz.setTitle(String.valueOf(body.get("title")));
        if (body.containsKey("examType")) quiz.setExamType(String.valueOf(body.get("examType")));
        if (body.containsKey("status")) quiz.setStatus(String.valueOf(body.get("status")));
        if (body.containsKey("passScore")) quiz.setPassScore(toInteger(body.get("passScore")));
        if (body.containsKey("timeLimit")) quiz.setTimeLimit(toInteger(body.get("timeLimit")));
        if (body.containsKey("totalScore")) quiz.setTotalScore(toInteger(body.get("totalScore")));
        if (body.containsKey("questions")) quiz.setQuestions(String.valueOf(body.get("questions")));

        quizRepo.save(quiz);
        return getExamById(id);
    }

    @Transactional
    public Map<String, Object> deleteExam(String id) {
        if (id.startsWith("comprehensive:")) {
            throw new RuntimeException("综合考试不可删除");
        }
        if (paperRepo.existsById(id)) {
            paperRepo.deleteById(id);
            return Map.of("success", true);
        }
        if (!quizRepo.existsById(id)) {
            throw new RuntimeException("考试不存在");
        }
        quizRepo.deleteById(id);
        return Map.of("success", true);
    }

    private List<Map<String, Object>> buildAllExamRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Chapter> chapterMap = chapterRepo.findAll().stream()
                .collect(Collectors.toMap(Chapter::getId, c -> c, (a, b) -> a));
        Map<String, Course> courseMap = courseRepo.findAll().stream()
                .collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));
        Map<String, String> chapterToCourse = chapterRepo.findAll().stream()
                .filter(ch -> ch.getCourse() != null)
                .collect(Collectors.toMap(Chapter::getId, ch -> ch.getCourse().getId(), (a, b) -> a));

        for (ChapterQuiz quiz : quizRepo.findAll()) {
            String courseId = chapterToCourse.get(quiz.getChapterId());
            Chapter chapter = chapterMap.get(quiz.getChapterId());
            Course course = courseId != null ? courseMap.get(courseId) : null;
            if (chapter == null || course == null) continue;
            rows.add(toChapterExamRow(quiz, chapter, course));
        }

        for (Course course : courseRepo.findAll()) {
            int poolSize = countQuestionPool(course.getId());
            if (poolSize < COMPREHENSIVE_MIN_POOL) continue;
            rows.add(toComprehensiveExamRow(course, poolSize));
        }

        for (ExamPaper paper : paperRepo.findAll()) {
            rows.add(toPaperExamRow(paper));
        }
        return rows;
    }

    private Map<String, Object> toPaperExamRow(ExamPaper paper) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", paper.getId());
        row.put("sourceType", "paper");
        row.put("title", paper.getTitle());
        row.put("examType", paper.getExamType() != null ? paper.getExamType() : "formal");
        row.put("department", paper.getDepartment() != null ? paper.getDepartment() : "全员");
        row.put("questionCount", parseQuestionIdCount(paper.getQuestionIds()));
        row.put("timeLimit", paper.getTimeLimit());
        row.put("totalScore", paper.getTotalScore() != null ? paper.getTotalScore() : 100);
        row.put("passScore", paper.getPassScore() != null ? paper.getPassScore() : 60);
        row.put("status", paper.getStatus() != null ? paper.getStatus() : "draft");
        row.put("attemptCount", 0);
        row.put("createdAt", paper.getCreatedAt() != null ? paper.getCreatedAt().format(DT) : null);
        return row;
    }

    private int parseQuestionIdCount(String questionIdsJson) {
        if (questionIdsJson == null || questionIdsJson.isBlank()) return 0;
        try {
            List<?> list = objectMapper.readValue(questionIdsJson, List.class);
            return list.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Object> toChapterExamRow(ChapterQuiz quiz, Chapter chapter, Course course) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", quiz.getId());
        row.put("sourceType", "chapter");
        row.put("chapterId", quiz.getChapterId());
        row.put("courseId", course.getId());
        row.put("courseTitle", course.getTitle());
        row.put("chapterTitle", chapter.getTitle());
        row.put("title", quiz.getTitle());
        row.put("examType", quiz.getExamType() != null ? quiz.getExamType() : "mock");
        row.put("department", resolveDepartment(course));
        row.put("questionCount", countQuestions(quiz.getQuestions()));
        row.put("timeLimit", quiz.getTimeLimit());
        row.put("totalScore", quiz.getTotalScore() != null ? quiz.getTotalScore() : 100);
        row.put("passScore", quiz.getPassScore() != null ? quiz.getPassScore() : 60);
        row.put("status", quiz.getStatus() != null ? quiz.getStatus() : mapCourseToExamStatus(course.getStatus()));
        row.put("attemptCount", attemptRepo.countByQuizId(quiz.getId()));
        row.put("createdAt", quiz.getCreatedAt() != null ? quiz.getCreatedAt().format(DT) : null);
        return row;
    }

    private Map<String, Object> toComprehensiveExamRow(Course course, int poolSize) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", "comprehensive:" + course.getId());
        row.put("sourceType", "comprehensive");
        row.put("courseId", course.getId());
        row.put("courseTitle", course.getTitle());
        row.put("title", course.getTitle() + "综合考试");
        row.put("examType", "formal");
        row.put("department", resolveDepartment(course));
        row.put("questionCount", Math.min(COMPREHENSIVE_DRAW_COUNT, poolSize));
        row.put("timeLimit", COMPREHENSIVE_TIME_LIMIT);
        row.put("totalScore", 100);
        row.put("passScore", COMPREHENSIVE_PASS_SCORE);
        row.put("status", mapCourseToExamStatus(course.getStatus()));
        row.put("attemptCount", comprehensiveAttemptRepo.findAll().stream()
                .filter(a -> course.getId().equals(a.getCourseId())).count());
        row.put("createdAt", course.getCreatedAt() != null ? course.getCreatedAt().format(DT) : null);
        return row;
    }

    private String resolveDepartment(Course course) {
        Map<String, Object> meta = parseMetadata(course.getExtraMetadata());
        Object depts = meta.get("targetDepartments");
        if (depts instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(String::valueOf).collect(Collectors.joining("、"));
        }
        Object audience = meta.get("targetAudience");
        if (audience != null && !String.valueOf(audience).isBlank()) {
            return String.valueOf(audience);
        }
        return "全员";
    }

    private String mapCourseToExamStatus(String courseStatus) {
        if ("published".equals(courseStatus)) return "published";
        if ("draft".equals(courseStatus)) return "draft";
        return "pending";
    }

    private int countQuestionPool(String courseId) {
        List<String> chapterIds = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId).stream()
                .map(Chapter::getId).toList();
        if (chapterIds.isEmpty()) return 0;
        int total = 0;
        for (ChapterQuiz quiz : quizRepo.findByChapterIdIn(chapterIds)) {
            total += countQuestions(quiz.getQuestions());
        }
        return total;
    }

    private int countQuestions(String questionsJson) {
        if (questionsJson == null || questionsJson.isBlank()) return 0;
        try {
            List<?> list = objectMapper.readValue(questionsJson, List.class);
            return list.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean matchKeyword(Map<String, Object> row, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String q = keyword.trim().toLowerCase();
        return contains(String.valueOf(row.get("title")), q)
                || contains(String.valueOf(row.get("courseTitle")), q);
    }

    private boolean matchExamType(Map<String, Object> row, String examType) {
        if (examType == null || examType.isBlank() || "all".equals(examType)) return true;
        return examType.equals(String.valueOf(row.get("examType")));
    }

    private boolean matchDepartment(Map<String, Object> row, String department) {
        if (department == null || department.isBlank() || "all".equals(department) || "全部部门".equals(department)) {
            return true;
        }
        return String.valueOf(row.get("department")).contains(department);
    }

    private boolean matchStatus(Map<String, Object> row, String status) {
        if (status == null || status.isBlank() || "all".equals(status)) return true;
        return status.equals(String.valueOf(row.get("status")));
    }

    private boolean matchDateRange(Map<String, Object> row, String from, String to) {
        String createdAt = String.valueOf(row.get("createdAt"));
        if (createdAt == null || "null".equals(createdAt)) return true;
        try {
            LocalDate created = LocalDateTime.parse(createdAt, DT).toLocalDate();
            if (from != null && !from.isBlank()) {
                if (created.isBefore(LocalDate.parse(from))) return false;
            }
            if (to != null && !to.isBlank()) {
                if (created.isAfter(LocalDate.parse(to))) return false;
            }
        } catch (Exception ignored) {
            return true;
        }
        return true;
    }

    private boolean contains(String source, String query) {
        return source != null && source.toLowerCase().contains(query);
    }

    private Map<String, Object> parseMetadata(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return new LinkedHashMap<>(objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {}));
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
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

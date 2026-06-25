package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.*;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCourseMonitoringService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final UserRepository userRepo;
    private final UserProgressRepository progressRepo;
    private final QuizAttemptRepository quizAttemptRepo;
    private final UserCertificateRepository certificateRepo;
    private final CourseCategoryRepository categoryRepo;
    private final ObjectMapper objectMapper;
    private final AdminInboxScheduler inboxScheduler;

    public Map<String, Object> getMonitoringSummary(String courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
        int chapterCount = chapters.size();
        Set<String> chapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toSet());

        List<User> targetUsers = resolveTargetUsers(course);
        Map<String, List<UserProgress>> progressByUser = groupProgressByUser(courseId);

        long completedCount = targetUsers.stream()
                .filter(u -> isFullyCompleted(u.getId(), chapterCount, progressByUser))
                .count();
        long expectedCount = targetUsers.size();
        double completionRate = expectedCount == 0 ? 0 : round(completedCount * 100.0 / expectedCount, 1);
        long incompleteCount = Math.max(0, expectedCount - completedCount);

        long warningCount = targetUsers.stream()
                .map(u -> buildLearnerRow(u, courseId, chapterCount, chapterIds, progressByUser))
                .filter(row -> !"none".equals(row.get("warningStatus")))
                .count();

        Map<String, Object> courseInfo = buildCourseInfo(course);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("expectedCount", expectedCount);
        stats.put("completedCount", completedCount);
        stats.put("completionRate", completionRate);
        stats.put("avgScore", computeCourseAvgScore(chapterIds));
        stats.put("incompleteCount", incompleteCount);
        stats.put("warningCount", warningCount);
        stats.put("chapterCount", chapterCount);

        inboxScheduler.notifyCourseLowProgress(course, warningCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("course", courseInfo);
        result.put("stats", stats);
        result.put("departments", listDepartments(targetUsers));
        return result;
    }

    public Map<String, Object> searchLearners(String courseId, String keyword, String department,
                                               String learningStatus, String warningStatus,
                                               int page, int pageSize) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
        int chapterCount = chapters.size();
        Set<String> chapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toSet());

        List<User> targetUsers = resolveTargetUsers(course);
        Map<String, List<UserProgress>> progressByUser = groupProgressByUser(courseId);

        List<Map<String, Object>> allRows = targetUsers.stream()
                .map(u -> buildLearnerRow(u, courseId, chapterCount, chapterIds, progressByUser))
                .filter(row -> matchKeyword(row, keyword))
                .filter(row -> matchDepartment(row, department))
                .filter(row -> matchLearningStatus(row, learningStatus))
                .filter(row -> matchWarningStatus(row, warningStatus))
                .sorted(Comparator.comparing(row -> String.valueOf(row.get("username"))))
                .toList();

        int total = allRows.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(total, from + pageSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", from >= total ? List.of() : allRows.subList(from, to));
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", pageSize == 0 ? 0 : (int) Math.ceil(total * 1.0 / pageSize));
        return result;
    }

    public Map<String, Object> getLearnerDetail(String courseId, String userId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("学员不存在"));

        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
        List<UserProgress> progressList = progressRepo.findByUserIdAndCourseId(userId, courseId);
        Map<String, UserProgress> progressByChapter = progressList.stream()
                .collect(Collectors.toMap(p -> p.getChapter().getId(), p -> p, (a, b) -> a));

        List<Map<String, Object>> chapterRows = chapters.stream().map(ch -> {
            UserProgress progress = progressByChapter.get(ch.getId());
            List<QuizAttempt> attempts = quizAttemptRepo.findByUserIdAndChapterId(userId, ch.getId());
            Integer bestScore = attempts.stream()
                    .map(QuizAttempt::getScore)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(null);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("chapterId", ch.getId());
            row.put("title", ch.getTitle());
            row.put("order", ch.getOrderNum());
            row.put("duration", ch.getDuration());
            row.put("progress", progress != null ? safeInt(progress.getProgress()) : 0);
            row.put("completed", progress != null && Boolean.TRUE.equals(progress.getCompleted()));
            row.put("studySeconds", progress != null ? safeInt(progress.getStudySeconds()) : 0);
            row.put("lastAccessAt", progress != null && progress.getLastAccessAt() != null
                    ? progress.getLastAccessAt().format(DT) : null);
            row.put("quizScore", bestScore);
            row.put("quizPassed", attempts.stream().anyMatch(a -> Boolean.TRUE.equals(a.getPassed())));
            return row;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("department", user.getDepartment());
        result.put("employeeNo", formatEmployeeNo(user));
        result.put("courseTitle", course.getTitle());
        result.put("chapters", chapterRows);
        return result;
    }

    private Map<String, List<UserProgress>> groupProgressByUser(String courseId) {
        return progressRepo.findByCourseIdWithUser(courseId).stream()
                .collect(Collectors.groupingBy(p -> p.getUser().getId()));
    }

    private Map<String, Object> buildLearnerRow(User user, String courseId, int chapterCount,
                                                 Set<String> chapterIds,
                                                 Map<String, List<UserProgress>> progressByUser) {
        List<UserProgress> rows = progressByUser.getOrDefault(user.getId(), List.of());
        long completedChapters = rows.stream().filter(p -> Boolean.TRUE.equals(p.getCompleted())).count();
        boolean hasStarted = rows.stream().anyMatch(p ->
                Boolean.TRUE.equals(p.getCompleted()) || (p.getProgress() != null && p.getProgress() > 0));

        int progressPercent;
        if (chapterCount == 0) {
            progressPercent = 0;
        } else if (completedChapters >= chapterCount) {
            progressPercent = 100;
        } else if (hasStarted) {
            progressPercent = (int) Math.round(completedChapters * 100.0 / chapterCount);
            if (progressPercent == 0) {
                progressPercent = (int) Math.round(rows.stream()
                        .mapToInt(p -> safeInt(p.getProgress()))
                        .average().orElse(0));
            }
        } else {
            progressPercent = 0;
        }

        int studySeconds = rows.stream()
                .mapToInt(p -> safeInt(p.getStudySeconds()))
                .sum();
        LocalDateTime lastAccess = rows.stream()
                .map(UserProgress::getLastAccessAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        String learningStatus = resolveLearningStatus(completedChapters, chapterCount, hasStarted);
        String warningStatus = resolveWarningStatus(learningStatus, progressPercent);
        String certificateStatus = resolveCertificateStatus(user.getId(), learningStatus, courseId);
        Integer examScore = computeUserBestScore(user.getId(), chapterIds);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("userId", user.getId());
        row.put("username", user.getUsername());
        row.put("email", user.getEmail());
        row.put("department", user.getDepartment() != null ? user.getDepartment() : "未分配");
        row.put("employeeNo", formatEmployeeNo(user));
        row.put("avatarUrl", user.getAvatarUrl());
        row.put("progress", progressPercent);
        row.put("completedChapters", completedChapters);
        row.put("totalChapters", chapterCount);
        row.put("learningStatus", learningStatus);
        row.put("studySeconds", studySeconds);
        row.put("studyDuration", formatDuration(studySeconds));
        row.put("examScore", examScore != null ? examScore : 0);
        row.put("certificateStatus", certificateStatus);
        row.put("warningStatus", warningStatus);
        row.put("lastAccessAt", lastAccess != null ? lastAccess.format(DT) : null);
        return row;
    }

    private Map<String, Object> buildCourseInfo(Course course) {
        Map<String, Object> courseInfo = new LinkedHashMap<>();
        courseInfo.put("id", course.getId());
        courseInfo.put("title", course.getTitle());
        courseInfo.put("coverImage", course.getCoverImage());
        courseInfo.put("category", course.getCategory());
        courseInfo.put("categoryName", resolveCategoryName(course.getCategory()));
        courseInfo.put("totalDuration", course.getTotalDuration());
        courseInfo.put("status", course.getStatus());
        courseInfo.put("publishedAt", "published".equals(course.getStatus()) && course.getUpdatedAt() != null
                ? course.getUpdatedAt().format(DT) : null);
        courseInfo.putAll(parseMetadata(course.getExtraMetadata()));
        return courseInfo;
    }

    private List<User> resolveTargetUsers(Course course) {
        List<User> trainees = userRepo.findAll().stream()
                .filter(u -> "trainee".equalsIgnoreCase(u.getRole()))
                .sorted(Comparator.comparing(User::getUsername, Comparator.nullsLast(String::compareTo)))
                .toList();

        Map<String, Object> meta = parseMetadata(course.getExtraMetadata());
        Object deptsObj = meta.get("targetDepartments");
        if (!(deptsObj instanceof List<?> deptList) || deptList.isEmpty()) {
            return trainees;
        }
        Set<String> depts = deptList.stream().map(String::valueOf).collect(Collectors.toSet());
        if (depts.contains("全体员工")) {
            return trainees;
        }
        return trainees.stream()
                .filter(u -> u.getDepartment() != null && depts.contains(u.getDepartment()))
                .toList();
    }

    private List<String> listDepartments(List<User> users) {
        LinkedHashSet<String> depts = new LinkedHashSet<>();
        depts.add("全部");
        users.stream()
                .map(User::getDepartment)
                .filter(Objects::nonNull)
                .filter(d -> !d.isBlank())
                .sorted()
                .forEach(depts::add);
        return new ArrayList<>(depts);
    }

    private boolean isFullyCompleted(String userId, int chapterCount,
                                       Map<String, List<UserProgress>> progressByUser) {
        if (chapterCount == 0) return false;
        long completed = progressByUser.getOrDefault(userId, List.of()).stream()
                .filter(p -> Boolean.TRUE.equals(p.getCompleted()))
                .count();
        return completed >= chapterCount;
    }

    private String resolveLearningStatus(long completedChapters, int chapterCount, boolean hasStarted) {
        if (chapterCount > 0 && completedChapters >= chapterCount) {
            return "completed";
        }
        if (hasStarted) {
            return "in_progress";
        }
        return "not_started";
    }

    private String resolveWarningStatus(String learningStatus, int progressPercent) {
        if ("not_started".equals(learningStatus)) {
            return "not_started";
        }
        if ("in_progress".equals(learningStatus) && progressPercent < 50) {
            return "low_progress";
        }
        return "none";
    }

    private String resolveCertificateStatus(String userId, String learningStatus, String courseId) {
        if ("not_started".equals(learningStatus)) {
            return "not_started";
        }
        return certificateRepo.findByUserIdAndCourseId(userId, courseId).isPresent()
                ? "obtained" : "not_obtained";
    }

    private Integer computeUserBestScore(String userId, Set<String> chapterIds) {
        if (chapterIds.isEmpty()) return null;
        return quizAttemptRepo.findAll().stream()
                .filter(a -> a.getUser() != null && userId.equals(a.getUser().getId()))
                .filter(a -> a.getQuiz() != null && chapterIds.contains(a.getQuiz().getChapterId()))
                .map(QuizAttempt::getScore)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private double computeCourseAvgScore(Set<String> chapterIds) {
        if (chapterIds.isEmpty()) return 0;
        return round(quizAttemptRepo.findAll().stream()
                .filter(a -> a.getQuiz() != null && chapterIds.contains(a.getQuiz().getChapterId()))
                .mapToInt(a -> a.getScore() != null ? a.getScore() : 0)
                .average().orElse(0), 1);
    }

    private String formatEmployeeNo(User user) {
        if (user.getUsername() != null && user.getUsername().matches("(?i)[A-Z]{0,2}\\d+")) {
            return user.getUsername().toUpperCase();
        }
        String id = user.getId() != null ? user.getId().replace("-", "") : "000000";
        return "GZ" + id.substring(0, Math.min(8, id.length())).toUpperCase();
    }

    private String formatDuration(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private boolean matchKeyword(Map<String, Object> row, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String q = keyword.trim().toLowerCase();
        return containsIgnoreCase(String.valueOf(row.get("username")), q)
                || containsIgnoreCase(String.valueOf(row.get("employeeNo")), q)
                || containsIgnoreCase(String.valueOf(row.get("email")), q)
                || containsIgnoreCase(String.valueOf(row.get("department")), q);
    }

    private boolean matchDepartment(Map<String, Object> row, String department) {
        if (department == null || department.isBlank() || "all".equals(department) || "全部".equals(department)) {
            return true;
        }
        return department.equals(String.valueOf(row.get("department")));
    }

    private boolean matchLearningStatus(Map<String, Object> row, String learningStatus) {
        if (learningStatus == null || learningStatus.isBlank() || "all".equals(learningStatus)) return true;
        return learningStatus.equals(String.valueOf(row.get("learningStatus")));
    }

    private boolean matchWarningStatus(Map<String, Object> row, String warningStatus) {
        if (warningStatus == null || warningStatus.isBlank() || "all".equals(warningStatus)) return true;
        return warningStatus.equals(String.valueOf(row.get("warningStatus")));
    }

    private boolean containsIgnoreCase(String source, String query) {
        return source != null && source.toLowerCase().contains(query);
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

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}

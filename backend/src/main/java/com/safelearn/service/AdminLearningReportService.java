package com.safelearn.service;

import com.safelearn.entity.*;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminLearningReportService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");
    private static final List<String> CHART_COLORS = List.of(
            "#3b82f6", "#10b981", "#f59e0b", "#8b5cf6", "#ef4444", "#06b6d4", "#64748b"
    );

    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final CourseCategoryRepository categoryRepo;
    private final ChapterRepository chapterRepo;
    private final UserProgressRepository progressRepo;
    private final QuizAttemptRepository quizAttemptRepo;

    @Transactional(readOnly = true)
    public Map<String, Object> getReport(String keyword, String department, String category,
                                         String learningStatus, LocalDate from, LocalDate to,
                                         int trendDays, int page, int pageSize) {
        AnalyticsScope scope = AnalyticsScope.create(userRepo, department, from, to);
        List<User> learners = scope.usersById.values().stream()
                .sorted(Comparator.comparing(User::getUsername, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<Course> courses = courseRepo.findByStatusOrderByCreatedAtDesc("published").stream()
                .filter(course -> matchCategory(course, category))
                .toList();
        List<UserProgress> allProgress = progressRepo.findAll().stream()
                .filter(item -> scope.containsUser(item.getUser()))
                .filter(item -> scope.inRange(item.getLastAccessAt()))
                .toList();
        List<QuizAttempt> attempts = quizAttemptRepo.findAll().stream()
                .filter(item -> scope.containsUser(item.getUser()))
                .filter(item -> scope.inRange(item.getCompletedAt() != null
                        ? item.getCompletedAt() : item.getStartedAt()))
                .toList();

        Map<String, Integer> chapterCounts = courses.stream().collect(Collectors.toMap(
                Course::getId,
                course -> chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId()).size(),
                (left, right) -> left,
                LinkedHashMap::new
        ));

        List<Map<String, Object>> learnerRows = buildLearnerRows(learners, courses, chapterCounts, allProgress, attempts);
        List<Map<String, Object>> filteredRows = learnerRows.stream()
                .filter(row -> matchKeyword(row, keyword))
                .filter(row -> matchLearningStatus(row, learningStatus))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", buildSummary(learners, filteredRows, allProgress, attempts, scope));
        result.put("charts", buildCharts(learners, courses, chapterCounts, allProgress, attempts, scope, trendDays));
        result.put("followUp", paginate(filteredRows.stream()
                .filter(row -> !"none".equals(row.get("warningStatus")))
                .toList(), page, pageSize));
        result.put("departments", scope.departments);
        result.put("categories", listCategories());
        result.put("generatedAt", LocalDateTime.now().format(DATE_TIME));
        return result;
    }

    private Map<String, Object> buildSummary(List<User> learners, List<Map<String, Object>> rows,
                                              List<UserProgress> progress, List<QuizAttempt> attempts,
                                              AnalyticsScope scope) {
        long completedUsers = rows.stream()
                .filter(row -> "completed".equals(row.get("learningStatus")))
                .map(row -> String.valueOf(row.get("userId")))
                .distinct()
                .count();
        long passed = attempts.stream().filter(item -> Boolean.TRUE.equals(item.getPassed())).count();
        double passRate = attempts.isEmpty() ? 0 : round(passed * 100.0 / attempts.size(), 1);

        int totalStudySeconds = progress.stream()
                .mapToInt(item -> item.getStudySeconds() != null ? item.getStudySeconds() : 0)
                .sum();
        double avgHours = learners.isEmpty() ? 0
                : round(totalStudySeconds / 3600.0 / learners.size(), 1);

        YearMonth current = YearMonth.from(scope.to);
        YearMonth previous = current.minusMonths(1);
        long traineeDelta = learners.stream()
                .filter(user -> user.getCreatedAt() != null && YearMonth.from(user.getCreatedAt()).equals(current))
                .count();
        long prevMonthTrainees = learners.stream()
                .filter(user -> user.getCreatedAt() != null && YearMonth.from(user.getCreatedAt()).equals(previous))
                .count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("traineeCount", learners.size());
        summary.put("traineeDelta", traineeDelta - prevMonthTrainees);
        summary.put("completionRate", learners.isEmpty() ? 0 : round(completedUsers * 100.0 / learners.size(), 1));
        summary.put("completedCount", completedUsers);
        summary.put("avgStudyHours", avgHours);
        summary.put("examPassRate", passRate);
        summary.put("passedCount", passed);
        return summary;
    }

    private Map<String, Object> buildCharts(List<User> learners, List<Course> courses,
                                             Map<String, Integer> chapterCounts,
                                             List<UserProgress> progress, List<QuizAttempt> attempts,
                                             AnalyticsScope scope, int trendDays) {
        Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("learningTrend", buildLearningTrend(progress, scope, trendDays));
        charts.put("courseCompletion", buildCourseCompletion(learners.size(), courses, chapterCounts, progress));
        charts.put("departmentCompletion", buildDepartmentCompletion(learners, progress));
        charts.put("examScoreDistribution", buildExamScoreDistribution(attempts));
        return charts;
    }

    private Map<String, Object> buildLearningTrend(List<UserProgress> progress, AnalyticsScope scope, int trendDays) {
        int days = Math.max(7, Math.min(trendDays, 30));
        LocalDate end = scope.to;
        LocalDate start = end.minusDays(days - 1L);
        if (start.isBefore(scope.from)) {
            start = scope.from;
        }

        List<String> labels = new ArrayList<>();
        List<Long> learnerCounts = new ArrayList<>();
        List<Long> completedCounts = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            labels.add(day.format(DAY_LABEL));
            LocalDate finalDay = day;
            Set<String> activeUsers = progress.stream()
                    .filter(item -> item.getLastAccessAt() != null
                            && item.getLastAccessAt().toLocalDate().equals(finalDay))
                    .map(item -> item.getUser().getId())
                    .collect(Collectors.toSet());
            long completed = progress.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getCompleted())
                            && item.getLastAccessAt() != null
                            && item.getLastAccessAt().toLocalDate().equals(finalDay))
                    .count();
            learnerCounts.add((long) activeUsers.size());
            completedCounts.add(completed);
        }

        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("labels", labels);
        trend.put("learners", learnerCounts);
        trend.put("completedCourses", completedCounts);
        return trend;
    }

    private Map<String, Object> buildCourseCompletion(int learnerCount, List<Course> courses,
                                                     Map<String, Integer> chapterCounts,
                                                     List<UserProgress> progress) {
        List<Map<String, Object>> items = new ArrayList<>();
        double totalRate = 0;
        int counted = 0;
        int colorIndex = 0;
        for (Course course : courses.stream().limit(6).toList()) {
            int chapterCount = chapterCounts.getOrDefault(course.getId(), 0);
            if (chapterCount == 0 || learnerCount == 0) continue;
            Map<String, Long> completedByUser = progress.stream()
                    .filter(item -> item.getCourse() != null && course.getId().equals(item.getCourse().getId()))
                    .filter(item -> Boolean.TRUE.equals(item.getCompleted()))
                    .collect(Collectors.groupingBy(item -> item.getUser().getId(), Collectors.counting()));
            long fullyCompleted = completedByUser.values().stream()
                    .filter(count -> count >= chapterCount)
                    .count();
            double rate = round(fullyCompleted * 100.0 / learnerCount, 1);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", shorten(course.getTitle(), 10));
            item.put("fullName", course.getTitle());
            item.put("rate", rate);
            item.put("color", CHART_COLORS.get(colorIndex++ % CHART_COLORS.size()));
            items.add(item);
            totalRate += rate;
            counted++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("overallRate", counted == 0 ? 0 : round(totalRate / counted, 1));
        return result;
    }

    private List<Map<String, Object>> buildDepartmentCompletion(List<User> learners, List<UserProgress> progress) {
        Map<String, List<User>> grouped = learners.stream()
                .collect(Collectors.groupingBy(user -> AnalyticsScope.deptName(user), LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> items = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            Set<String> userIds = entry.getValue().stream().map(User::getId).collect(Collectors.toSet());
            Set<String> completedUsers = progress.stream()
                    .filter(item -> userIds.contains(item.getUser().getId()) && Boolean.TRUE.equals(item.getCompleted()))
                    .map(item -> item.getUser().getId())
                    .collect(Collectors.toSet());
            double rate = entry.getValue().isEmpty() ? 0
                    : round(completedUsers.size() * 100.0 / entry.getValue().size(), 1);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", entry.getKey());
            item.put("rate", rate);
            items.add(item);
        }
        items.sort((a, b) -> Double.compare(((Number) b.get("rate")).doubleValue(), ((Number) a.get("rate")).doubleValue()));
        return items;
    }

    private List<Map<String, Object>> buildExamScoreDistribution(List<QuizAttempt> attempts) {
        long[] buckets = new long[4];
        String[] labels = {"90分以上", "80-89分", "60-79分", "60分以下"};
        for (QuizAttempt attempt : attempts) {
            if (attempt.getScore() == null) continue;
            int score = attempt.getScore();
            if (score >= 90) buckets[0]++;
            else if (score >= 80) buckets[1]++;
            else if (score >= 60) buckets[2]++;
            else buckets[3]++;
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", labels[i]);
            item.put("value", buckets[i]);
            item.put("color", CHART_COLORS.get(i % CHART_COLORS.size()));
            items.add(item);
        }
        return items;
    }

    private List<Map<String, Object>> buildLearnerRows(List<User> learners, List<Course> courses,
                                                        Map<String, Integer> chapterCounts,
                                                        List<UserProgress> progress,
                                                        List<QuizAttempt> attempts) {
        if (courses.isEmpty()) return List.of();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (User user : learners) {
            Course primaryCourse = pickPrimaryCourse(user.getId(), courses, progress);
            if (primaryCourse == null) continue;
            String courseId = primaryCourse.getId();
            int chapterCount = chapterCounts.getOrDefault(courseId, 0);
            Set<String> chapterIds = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId).stream()
                    .map(Chapter::getId)
                    .collect(Collectors.toSet());
            List<UserProgress> userProgress = progress.stream()
                    .filter(item -> item.getUser() != null && user.getId().equals(item.getUser().getId()))
                    .filter(item -> item.getCourse() != null && courseId.equals(item.getCourse().getId()))
                    .toList();
            long completedChapters = userProgress.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getCompleted()))
                    .count();
            boolean hasStarted = userProgress.stream().anyMatch(item ->
                    Boolean.TRUE.equals(item.getCompleted())
                            || (item.getProgress() != null && item.getProgress() > 0));
            int progressPercent = chapterCount == 0 ? 0
                    : (int) Math.round(completedChapters * 100.0 / chapterCount);
            if (progressPercent == 0 && hasStarted) {
                progressPercent = (int) Math.round(userProgress.stream()
                        .mapToInt(item -> item.getProgress() != null ? item.getProgress() : 0)
                        .average().orElse(0));
            }
            int studySeconds = userProgress.stream()
                    .mapToInt(item -> item.getStudySeconds() != null ? item.getStudySeconds() : 0)
                    .sum();
            String learningStatus = resolveLearningStatus(completedChapters, chapterCount, hasStarted);
            Integer examScore = attempts.stream()
                    .filter(item -> item.getUser() != null && user.getId().equals(item.getUser().getId()))
                    .filter(item -> item.getQuiz() != null && chapterIds.contains(item.getQuiz().getChapterId()))
                    .map(QuizAttempt::getScore)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(null);
            String warningStatus = resolveWarningStatus(learningStatus, progressPercent, examScore);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", user.getId());
            row.put("username", user.getUsername());
            row.put("department", AnalyticsScope.deptName(user));
            row.put("courseId", courseId);
            row.put("courseTitle", primaryCourse.getTitle());
            row.put("progress", progressPercent);
            row.put("studyHours", round(studySeconds / 3600.0, 1));
            row.put("studyDuration", formatStudyHours(studySeconds));
            row.put("examScore", examScore);
            row.put("examLabel", examScore == null ? "未考试" : examScore + "分");
            row.put("learningStatus", learningStatus);
            row.put("warningStatus", warningStatus);
            rows.add(row);
        }
        return rows;
    }

    private Course pickPrimaryCourse(String userId, List<Course> courses, List<UserProgress> progress) {
        Map<String, LocalDateTime> lastAccessByCourse = progress.stream()
                .filter(item -> item.getUser() != null && userId.equals(item.getUser().getId()))
                .filter(item -> item.getCourse() != null)
                .collect(Collectors.toMap(
                        item -> item.getCourse().getId(),
                        item -> item.getLastAccessAt() != null ? item.getLastAccessAt() : LocalDateTime.MIN,
                        (left, right) -> left.isAfter(right) ? left : right
                ));
        return courses.stream()
                .sorted(Comparator
                        .comparing((Course course) -> lastAccessByCourse.getOrDefault(course.getId(), LocalDateTime.MIN))
                        .reversed()
                        .thenComparing(Course::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(courses.get(0));
    }

    private String resolveLearningStatus(long completedChapters, int chapterCount, boolean hasStarted) {
        if (chapterCount > 0 && completedChapters >= chapterCount) return "completed";
        if (hasStarted) return "in_progress";
        return "not_started";
    }

    private String resolveWarningStatus(String learningStatus, int progressPercent, Integer examScore) {
        if (examScore != null && examScore > 0 && examScore < 60) return "exam_fail";
        if (examScore != null && examScore >= 60 && examScore < 70) return "retake";
        if ("not_started".equals(learningStatus)) return "not_started";
        if (!"completed".equals(learningStatus)) return "incomplete";
        if (progressPercent < 50) return "low_progress";
        return "none";
    }

    private boolean matchCategory(Course course, String category) {
        if (category == null || category.isBlank() || "all".equals(category)) return true;
        return category.equals(course.getCategory());
    }

    private boolean matchKeyword(Map<String, Object> row, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String q = keyword.trim().toLowerCase();
        return contains(row.get("username"), q)
                || contains(row.get("department"), q)
                || contains(row.get("courseTitle"), q);
    }

    private boolean matchLearningStatus(Map<String, Object> row, String learningStatus) {
        if (learningStatus == null || learningStatus.isBlank() || "all".equals(learningStatus)) return true;
        return learningStatus.equals(String.valueOf(row.get("learningStatus")));
    }

    private boolean contains(Object value, String query) {
        return value != null && String.valueOf(value).toLowerCase().contains(query);
    }

    private List<Map<String, Object>> listCategories() {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of("code", "all", "name", "全部分类"));
        categoryRepo.findAllByOrderBySortOrderAscNameAsc().forEach(category -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", category.getCode());
            item.put("name", category.getName());
            items.add(item);
        });
        return items;
    }

    private Map<String, Object> paginate(List<Map<String, Object>> all, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(pageSize, 100));
        int total = all.size();
        int from = (safePage - 1) * safeSize;
        int to = Math.min(total, from + safeSize);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", from < total ? all.subList(from, to) : List.of());
        result.put("total", total);
        result.put("page", safePage);
        result.put("pageSize", safeSize);
        result.put("totalPages", safeSize == 0 ? 0 : (int) Math.ceil(total * 1.0 / safeSize));
        return result;
    }

    private String formatStudyHours(int seconds) {
        return round(seconds / 3600.0, 1) + "h";
    }

    private String shorten(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}

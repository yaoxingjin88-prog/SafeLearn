package com.safelearn.service;

import com.safelearn.entity.*;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 管理端首页聚合查询，避免前端为一张驾驶舱并发拼接多个接口。 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<String> COLORS = List.of("#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#06b6d4");

    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final CourseCategoryRepository categoryRepo;
    private final ChapterRepository chapterRepo;
    private final UserProgressRepository progressRepo;
    private final QuizAttemptRepository quizAttemptRepo;
    private final TrainingRecordRepository trainingRecordRepo;
    private final UserCertificateRepository certificateRepo;
    private final SystemConfigService systemConfig;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        List<User> learners = userRepo.findAll().stream()
                .filter(user -> !"admin".equalsIgnoreCase(user.getRole()))
                .toList();
        List<Course> courses = courseRepo.findByStatusOrderByCreatedAtDesc("published");
        List<UserProgress> progress = progressRepo.findAll();
        List<QuizAttempt> attempts = quizAttemptRepo.findAll();
        List<TrainingRecord> records = trainingRecordRepo.findAll();
        List<UserCertificate> certificates = certificateRepo.findAll();

        Map<String, Integer> chapterCounts = courses.stream().collect(Collectors.toMap(
                Course::getId,
                course -> chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId()).size(),
                (left, right) -> left,
                LinkedHashMap::new
        ));
        Set<String> completedLearners = progress.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCompleted()))
                .map(item -> item.getUser().getId())
                .collect(Collectors.toSet());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stats", buildStats(learners, completedLearners, attempts, progress, certificates, now));
        result.put("completionTrend", buildCompletionTrend(learners.size(), progress));
        result.put("categoryCompletion", buildCategoryCompletion(learners.size(), courses, chapterCounts, progress));
        result.put("departments", buildDepartments(learners));
        result.put("categoryCompletionByDepartment", buildCategoryCompletionByDepartment(learners, courses, chapterCounts, progress));
        result.put("departmentOverview", buildDepartmentOverview(learners, progress));
        result.put("trainingPlans", buildTrainingPlans(learners.size(), courses, chapterCounts, progress));
        result.put("alerts", buildAlerts(learners, progress, attempts, records, certificates, now, 4));
        result.put("announcements", buildAnnouncements(courses, now).stream().limit(5).toList());
        result.put("calendarEvents", buildCalendarEvents(courses, progress, records, now.toLocalDate()));
        result.put("generatedAt", now.format(DATE_TIME));
        return result;
    }

    /** 预警中心：返回完整预警列表与汇总统计。 */
    @Transactional(readOnly = true)
    public Map<String, Object> getAlertCenter() {
        LocalDateTime now = LocalDateTime.now();
        List<User> learners = userRepo.findAll().stream()
                .filter(user -> !"admin".equalsIgnoreCase(user.getRole()))
                .toList();
        List<UserProgress> progress = progressRepo.findAll();
        List<QuizAttempt> attempts = quizAttemptRepo.findAll();
        List<TrainingRecord> records = trainingRecordRepo.findAll();
        List<UserCertificate> certificates = certificateRepo.findAll();
        List<Map<String, Object>> items = buildAlerts(learners, progress, attempts, records, certificates, now, null);

        long dangerCount = items.stream().filter(i -> "danger".equals(i.get("level"))).count();
        long warningCount = items.stream().filter(i -> "warning".equals(i.get("level"))).count();
        long infoCount = items.stream().filter(i -> "info".equals(i.get("level"))).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", items.size());
        result.put("dangerCount", dangerCount);
        result.put("warningCount", warningCount);
        result.put("infoCount", infoCount);
        result.put("generatedAt", now.format(DATE_TIME));
        return result;
    }

    private Map<String, Object> buildStats(List<User> learners, Set<String> completedLearners,
                                           List<QuizAttempt> attempts, List<UserProgress> progress,
                                           List<UserCertificate> certificates, LocalDateTime now) {
        long passed = attempts.stream().filter(item -> Boolean.TRUE.equals(item.getPassed())).count();
        double passRate = attempts.isEmpty() ? 0 : round(passed * 100.0 / attempts.size(), 2);
        long lowProgress = averageProgressByUser(progress).values().stream().filter(value -> value < 40).count();
        long expiring = certificates.stream()
                .filter(item -> item.getExpiresAt() != null && !item.getExpiresAt().isBefore(now)
                        && item.getExpiresAt().isBefore(now.plusDays(30)))
                .count();

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        long completedToday = progress.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCompleted()) && item.getLastAccessAt() != null
                        && item.getLastAccessAt().toLocalDate().equals(today))
                .map(item -> item.getUser().getId()).distinct().count();
        long completedYesterday = progress.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCompleted()) && item.getLastAccessAt() != null
                        && item.getLastAccessAt().toLocalDate().equals(yesterday))
                .map(item -> item.getUser().getId()).distinct().count();
        long passedToday = attempts.stream()
                .filter(item -> Boolean.TRUE.equals(item.getPassed()) && item.getCompletedAt() != null
                        && item.getCompletedAt().toLocalDate().equals(today)).count();
        long passedYesterday = attempts.stream()
                .filter(item -> Boolean.TRUE.equals(item.getPassed()) && item.getCompletedAt() != null
                        && item.getCompletedAt().toLocalDate().equals(yesterday)).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("traineeCount", learners.size());
        stats.put("completedTraining", completedLearners.size());
        stats.put("incompleteTraining", Math.max(0, learners.size() - completedLearners.size()));
        stats.put("completionRate", learners.isEmpty() ? 0 : round(completedLearners.size() * 100.0 / learners.size(), 2));
        stats.put("examPassRate", passRate);
        stats.put("safetyAlerts", lowProgress + expiring);
        stats.put("traineeDelta", 0);
        stats.put("completedDelta", completedToday - completedYesterday);
        stats.put("incompleteDelta", completedYesterday - completedToday);
        stats.put("passRateDelta", round(passedToday - passedYesterday, 1));
        stats.put("alertsDelta", 0);
        return stats;
    }

    private Map<String, Object> buildCompletionTrend(int learnerCount, List<UserProgress> progress) {
        List<String> labels = new ArrayList<>();
        List<Long> completed = new ArrayList<>();
        List<Double> rates = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int offset = 6; offset >= 0; offset--) {
            LocalDate day = today.minusDays(offset);
            Set<String> users = progress.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getCompleted()) && item.getLastAccessAt() != null
                            && item.getLastAccessAt().toLocalDate().equals(day))
                    .map(item -> item.getUser().getId())
                    .collect(Collectors.toSet());
            labels.add(day.format(DAY));
            completed.add((long) users.size());
            rates.add(learnerCount == 0 ? 0 : round(users.size() * 100.0 / learnerCount, 1));
        }
        return Map.of("labels", labels, "completedUsers", completed, "completionRates", rates);
    }

    private List<Map<String, Object>> buildCategoryCompletion(int learnerCount, List<Course> courses,
                                                               Map<String, Integer> chapterCounts,
                                                               List<UserProgress> progress) {
        Map<String, String> categoryNames = categoryRepo.findAllByOrderBySortOrderAscNameAsc().stream()
                .collect(Collectors.toMap(CourseCategory::getCode, CourseCategory::getName, (a, b) -> a, LinkedHashMap::new));
        Map<String, List<Course>> grouped = courses.stream().collect(Collectors.groupingBy(
                Course::getCategory, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> items = new ArrayList<>();
        int index = 0;
        for (var entry : grouped.entrySet()) {
            Set<String> courseIds = entry.getValue().stream().map(Course::getId).collect(Collectors.toSet());
            long expected = (long) entry.getValue().stream().mapToInt(course -> chapterCounts.getOrDefault(course.getId(), 0)).sum()
                    * Math.max(learnerCount, 1);
            long finished = progress.stream()
                    .filter(item -> courseIds.contains(item.getCourse().getId()) && Boolean.TRUE.equals(item.getCompleted()))
                    .map(item -> item.getUser().getId() + ":" + item.getChapter().getId())
                    .distinct().count();
            double rate = expected == 0 ? 0 : round(finished * 100.0 / expected, 1);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", entry.getKey());
            item.put("name", categoryNames.getOrDefault(entry.getKey(), readableCategory(entry.getKey())));
            item.put("rate", rate);
            item.put("color", COLORS.get(index++ % COLORS.size()));
            items.add(item);
        }
        items.sort((a, b) -> Double.compare(((Number) b.get("rate")).doubleValue(), ((Number) a.get("rate")).doubleValue()));
        return items.stream().limit(6).toList();
    }

    private List<String> buildDepartments(List<User> learners) {
        return learners.stream()
                .map(this::resolveDepartment)
                .distinct()
                .sorted()
                .toList();
    }

    private Map<String, List<Map<String, Object>>> buildCategoryCompletionByDepartment(List<User> learners,
                                                                                       List<Course> courses,
                                                                                       Map<String, Integer> chapterCounts,
                                                                                       List<UserProgress> progress) {
        Map<String, List<User>> grouped = learners.stream()
                .collect(Collectors.groupingBy(this::resolveDepartment, LinkedHashMap::new, Collectors.toList()));
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (var entry : grouped.entrySet()) {
            Set<String> userIds = entry.getValue().stream().map(User::getId).collect(Collectors.toSet());
            List<UserProgress> scoped = progress.stream()
                    .filter(item -> userIds.contains(item.getUser().getId()))
                    .toList();
            result.put(entry.getKey(), buildCategoryCompletion(entry.getValue().size(), courses, chapterCounts, scoped));
        }
        return result;
    }

    private Map<String, Map<String, Object>> buildDepartmentOverview(List<User> learners, List<UserProgress> progress) {
        Map<String, List<User>> grouped = learners.stream()
                .collect(Collectors.groupingBy(this::resolveDepartment, LinkedHashMap::new, Collectors.toList()));
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (var entry : grouped.entrySet()) {
            Set<String> userIds = entry.getValue().stream().map(User::getId).collect(Collectors.toSet());
            Set<String> completed = progress.stream()
                    .filter(item -> userIds.contains(item.getUser().getId()) && Boolean.TRUE.equals(item.getCompleted()))
                    .map(item -> item.getUser().getId())
                    .collect(Collectors.toSet());
            int count = entry.getValue().size();
            Map<String, Object> overview = new LinkedHashMap<>();
            overview.put("traineeCount", count);
            overview.put("completedTraining", completed.size());
            overview.put("completionRate", count == 0 ? 0 : round(completed.size() * 100.0 / count, 2));
            result.put(entry.getKey(), overview);
        }
        return result;
    }

    private String resolveDepartment(User user) {
        if (user.getDepartment() == null || user.getDepartment().isBlank()) {
            return "未分配部门";
        }
        return user.getDepartment().trim();
    }

    private List<Map<String, Object>> buildTrainingPlans(int learnerCount, List<Course> courses,
                                                          Map<String, Integer> chapterCounts,
                                                          List<UserProgress> progress) {
        List<Map<String, Object>> plans = new ArrayList<>();
        for (Course course : courses.stream().limit(5).toList()) {
            List<UserProgress> courseProgress = progress.stream()
                    .filter(item -> item.getCourse().getId().equals(course.getId())).toList();
            Set<String> learnedUsers = courseProgress.stream().map(item -> item.getUser().getId()).collect(Collectors.toSet());
            int chapterCount = chapterCounts.getOrDefault(course.getId(), 0);
            Map<String, Long> completedByUser = courseProgress.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getCompleted()))
                    .collect(Collectors.groupingBy(item -> item.getUser().getId(), Collectors.counting()));
            long completedUsers = chapterCount == 0 ? 0 : completedByUser.values().stream()
                    .filter(count -> count >= chapterCount).count();
            double rate = learnerCount == 0 ? 0 : round(completedUsers * 100.0 / learnerCount, 1);

            Map<String, Object> plan = new LinkedHashMap<>();
            plan.put("id", course.getId());
            plan.put("name", course.getTitle());
            plan.put("department", "全部部门");
            plan.put("learned", learnedUsers.size());
            plan.put("target", learnerCount);
            plan.put("rate", rate);
            plan.put("status", rate >= 100 ? "completed" : learnedUsers.isEmpty() ? "not_started" : "in_progress");
            plans.add(plan);
        }
        return plans;
    }

    private List<Map<String, Object>> buildAlerts(List<User> learners, List<UserProgress> progress,
                                                   List<QuizAttempt> attempts, List<TrainingRecord> records,
                                                   List<UserCertificate> certificates, LocalDateTime now,
                                                   Integer limit) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        attempts.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getPassed()))
                .max(Comparator.comparing(item -> Optional.ofNullable(item.getCompletedAt()).orElse(item.getStartedAt())))
                .ifPresent(item -> alerts.add(alert("exam", "考试异常预警",
                        item.getUser().getUsername() + " 最近一次考试未通过",
                        "danger", Optional.ofNullable(item.getCompletedAt()).orElse(item.getStartedAt()),
                        "/admin/learning/exams")));

        Map<String, Double> averages = averageProgressByUser(progress);
        long lowProgress = learners.stream().filter(user -> averages.getOrDefault(user.getId(), 0.0) < 40).count();
        if (lowProgress > 0) {
            alerts.add(alert("progress", "培训进度预警",
                    lowProgress + " 名学员培训进度低于 40%", "warning", now,
                    "/admin/learning/monitoring?warningStatus=low_progress"));
        }

        records.stream().filter(item -> item.getTotalScore() != null && item.getTotalScore() < 60)
                .max(Comparator.comparing(TrainingRecord::getStartTime))
                .ifPresent(item -> alerts.add(alert("training", "低分预警",
                        item.getUser().getUsername() + " 应急训练得分 " + item.getTotalScore(),
                        "warning", Optional.ofNullable(item.getEndTime()).orElse(item.getStartTime()),
                        "/admin/learning/monitoring")));

        long expiring = certificates.stream().filter(item -> item.getExpiresAt() != null
                        && !item.getExpiresAt().isBefore(now) && item.getExpiresAt().isBefore(now.plusDays(30))).count();
        if (expiring > 0) {
            alerts.add(alert("certificate", "证书即将到期",
                    expiring + " 份安全证书将在 30 天内到期", "info", now,
                    "/admin/org"));
        }
        if (alerts.isEmpty()) {
            alerts.add(alert("normal", "运行状态正常",
                    "当前没有需要立即处理的安全预警", "info", now, "/dashboard"));
        }
        if (limit == null || limit <= 0) {
            return alerts;
        }
        return alerts.stream().limit(limit).toList();
    }

    private List<Map<String, Object>> buildAnnouncements(List<Course> courses, LocalDateTime now) {
        List<Map<String, Object>> notices = new ArrayList<>();
        String configured = systemConfig.getString("dashboard.announcement", "");
        if (!configured.isBlank()) {
            notices.add(notice("notice:system", configured, now.toLocalDate(), true, "announcement", "/dashboard"));
        }
        courses.stream().limit(8).forEach(course -> notices.add(notice(
                "notice:course-" + course.getId(),
                "课程《" + course.getTitle() + "》已上线",
                course.getCreatedAt() != null ? course.getCreatedAt().toLocalDate() : now.toLocalDate(),
                false,
                "course",
                "/admin/learning/courses/" + course.getId()
        )));
        if (notices.isEmpty()) {
            notices.add(notice("notice:default", "储能安全培训管理平台运行正常",
                    now.toLocalDate(), false, "system", "/dashboard"));
        }
        return notices;
    }

    /** 管理端消息汇总：公告与课程上线通知。 */
    @Transactional(readOnly = true)
    public Map<String, Object> getMessageSummary() {
        LocalDateTime now = LocalDateTime.now();
        List<Course> courses = courseRepo.findByStatusOrderByCreatedAtDesc("published");
        List<Map<String, Object>> items = buildAnnouncements(courses, now);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", items.size());
        result.put("generatedAt", now.format(DATE_TIME));
        return result;
    }

    private List<Map<String, Object>> buildCalendarEvents(List<Course> courses, List<UserProgress> progress,
                                                           List<TrainingRecord> records, LocalDate today) {
        LocalDate start = today.withDayOfMonth(1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        Map<LocalDate, String> events = new TreeMap<>();
        courses.stream().filter(course -> course.getCreatedAt() != null)
                .map(course -> course.getCreatedAt().toLocalDate())
                .filter(date -> !date.isBefore(start) && !date.isAfter(end))
                .forEach(date -> events.putIfAbsent(date, "planned"));
        progress.stream().filter(item -> item.getLastAccessAt() != null)
                .map(item -> item.getLastAccessAt().toLocalDate())
                .filter(date -> !date.isBefore(start) && !date.isAfter(end))
                .forEach(date -> events.put(date, "in_progress"));
        records.stream().map(item -> Optional.ofNullable(item.getEndTime()).orElse(item.getStartTime()))
                .filter(Objects::nonNull).map(LocalDateTime::toLocalDate)
                .filter(date -> !date.isBefore(start) && !date.isAfter(end))
                .forEach(date -> events.put(date, "completed"));
        return events.entrySet().stream()
                .map(entry -> Map.<String, Object>of("date", entry.getKey().toString(), "type", entry.getValue()))
                .toList();
    }

    private Map<String, Double> averageProgressByUser(List<UserProgress> progress) {
        return progress.stream().collect(Collectors.groupingBy(
                item -> item.getUser().getId(),
                Collectors.averagingInt(item -> Optional.ofNullable(item.getProgress()).orElse(0))
        ));
    }

    private Map<String, Object> alert(String type, String title, String description, String level,
                                       LocalDateTime time, String actionPath) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", type + "-" + (time != null ? time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")) : "now"));
        item.put("type", type);
        item.put("title", title);
        item.put("description", description);
        item.put("level", level);
        item.put("time", time != null ? time.format(DATE_TIME) : null);
        item.put("actionPath", actionPath);
        return item;
    }

    private Map<String, Object> notice(String id, String title, LocalDate date, boolean pinned,
                                        String type, String actionPath) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("title", title);
        item.put("date", date.toString());
        item.put("pinned", pinned);
        item.put("type", type);
        item.put("actionPath", actionPath);
        return item;
    }

    private String readableCategory(String category) {
        return switch (category == null ? "" : category) {
            case "basic" -> "储能基础知识";
            case "thermal" -> "热失控防控";
            case "fire" -> "消防安全";
            case "operation" -> "设备安全管理";
            case "emergency" -> "应急处置";
            default -> category == null || category.isBlank() ? "其他" : category;
        };
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}

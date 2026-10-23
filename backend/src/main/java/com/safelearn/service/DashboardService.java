package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.deduction.entity.SimulationScoreReport;
import com.safelearn.deduction.entity.SimulationSession;
import com.safelearn.deduction.repository.SimulationScoreReportRepository;
import com.safelearn.deduction.repository.SimulationSessionRepository;
import com.safelearn.entity.*;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final UserProgressRepository progressRepo;
    private final TrainingRecordRepository recordRepo;
    private final SimulationSessionRepository simulationSessionRepo;
    private final SimulationScoreReportRepository scoreReportRepo;
    private final ScenarioRepository scenarioRepo;
    private final UserRepository userRepo;
    private final LearnerAssignmentRepository assignmentRepo;
    private final AdminTrainingAlertRepository trainingAlertRepo;
    private final UserCaseProgressRepository caseProgressRepo;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfig;
    private final AssignmentCompletionService assignmentCompletionService;

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    public Map<String, Object> getStats(String userId) {
        long courseCount = courseRepo.findByStatusOrderByCreatedAtDesc("published").size();
        long completedCount = progressRepo.countByUserIdAndCompletedTrue(userId);
        long simulationCount = simulationSessionRepo.countByUserIdAndStatus(userId, "completed");
        long trainingCount = recordRepo.countByUserId(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("courseCount", courseCount);
        stats.put("completedCount", completedCount);
        stats.put("simulationCount", simulationCount);
        stats.put("trainingCount", trainingCount);
        Double dedAvg = simulationSessionRepo.avgTotalScoreByUserId(userId);
        Double trainAvg = recordRepo.avgCompletedScoreByUserId(userId);
        int avgScore = dedAvg != null ? (int) Math.round(dedAvg)
                : (trainAvg != null ? (int) Math.round(trainAvg) : 0);
        stats.put("avgScore", avgScore);
        return stats;
    }

    /**
     * 管理端工作台总览：在线/总学习人数、各部门学习进度。
     * 说明：系统未维护实时会话心跳，"在线"以最近 15 分钟内有学习活动的用户近似，
     * 同时给出"今日活跃"作为更稳定的口径。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminOverview() {
        List<User> learners = userRepo.findAll().stream()
                .filter(u -> !"admin".equalsIgnoreCase(u.getRole()))
                .toList();
        long totalLearners = learners.size();

        LocalDateTime now = LocalDateTime.now();
        long onlineLearners = progressRepo.countDistinctActiveUsersSince(now.minusMinutes(15));
        long activeToday = progressRepo.countDistinctActiveUsersSince(now.toLocalDate().atStartOfDay());

        // 按用户聚合进度，便于按部门汇总
        Map<String, double[]> progressByUser = new HashMap<>(); // userId -> [完成章节数, 平均进度]
        for (Object[] row : progressRepo.aggregateProgressByUser()) {
            String uid = (String) row[0];
            double completed = row[1] != null ? ((Number) row[1]).doubleValue() : 0;
            double avgProgress = row[2] != null ? ((Number) row[2]).doubleValue() : 0;
            progressByUser.put(uid, new double[]{completed, avgProgress});
        }

        // 按部门分组
        Map<String, List<User>> byDept = new LinkedHashMap<>();
        for (User u : learners) {
            String dept = (u.getDepartment() == null || u.getDepartment().isBlank())
                    ? "未分配部门" : u.getDepartment().trim();
            byDept.computeIfAbsent(dept, k -> new ArrayList<>()).add(u);
        }

        List<Map<String, Object>> departments = new ArrayList<>();
        for (var entry : byDept.entrySet()) {
            List<User> members = entry.getValue();
            double progressSum = 0;
            double completedSum = 0;
            int learnedMembers = 0;
            for (User u : members) {
                double[] agg = progressByUser.get(u.getId());
                if (agg != null) {
                    progressSum += agg[1];
                    completedSum += agg[0];
                    if (agg[1] > 0 || agg[0] > 0) learnedMembers++;
                }
            }
            int memberCount = members.size();
            int avgProgress = memberCount == 0 ? 0 : (int) Math.round(progressSum / memberCount);

            Map<String, Object> dept = new LinkedHashMap<>();
            dept.put("name", entry.getKey());
            dept.put("memberCount", memberCount);
            dept.put("learnedMembers", learnedMembers);
            dept.put("completedChapters", (int) completedSum);
            dept.put("avgProgress", Math.min(100, avgProgress));
            departments.add(dept);
        }
        // 进度高的部门排在前面
        departments.sort((a, b) -> Integer.compare(
                ((Number) b.get("avgProgress")).intValue(),
                ((Number) a.get("avgProgress")).intValue()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalLearners", totalLearners);
        result.put("onlineLearners", onlineLearners);
        result.put("activeToday", activeToday);
        result.put("departments", departments);
        return result;
    }

    @Transactional
    public Map<String, Object> getOverview(String userId) {
        assignmentCompletionService.syncForUser(userId);
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        List<UserProgress> allProgress = progressRepo.findByUserIdWithDetails(userId);
        List<Course> publishedCourses = courseRepo.findByStatusOrderByCreatedAtDesc("published");

        long completedChapters = progressRepo.countByUserIdAndCompletedTrue(userId);
        int studyMinutes = calcCourseStudyMinutes(allProgress)
                + calcTrainingStudyMinutes(userId)
                + calcSimulationStudyMinutes(userId);
        Double dedAvg = simulationSessionRepo.avgTotalScoreByUserId(userId);
        Double trainAvg = recordRepo.avgCompletedScoreByUserId(userId);
        int avgScore = dedAvg != null ? (int) Math.round(dedAvg)
                : (trainAvg != null ? (int) Math.round(trainAvg) : 0);
        long deductionCount = simulationSessionRepo.countByUserIdAndStatus(userId, "completed");
        long deductionSuccess = simulationSessionRepo.findByUserIdOrderByStartedAtDesc(userId).stream()
                .filter(s -> "success".equals(s.getOutcome())).count();

        long pendingMandatory = publishedCourses.stream()
                .mapToLong(course -> countIncompleteChapters(userId, course.getId()))
                .sum();

        Map<String, Object> overview = new HashMap<>();
        overview.put("displayName", resolveDisplayName(user));
        overview.put("joinDays", calcJoinDays(user.getCreatedAt()));
        overview.put("streakDays", calcStreakDays(allProgress));
        overview.put("studyHours", Math.round(studyMinutes / 60.0 * 10) / 10.0);
        overview.put("completedCourses", countCompletedCourses(userId, publishedCourses));
        overview.put("completedChapters", completedChapters);
        overview.put("avgScore", avgScore);
        overview.put("pendingMandatory", pendingMandatory);
        overview.put("deductionCount", deductionCount);
        overview.put("deductionSuccessRate", deductionCount == 0 ? 0
                : Math.round(deductionSuccess * 1000.0 / deductionCount) / 10.0);
        overview.put("deductionAvgScore", dedAvg != null ? Math.round(dedAvg * 10) / 10.0 : 0);
        overview.put("continueLearning", buildContinueLearning(userId, allProgress));
        List<Map<String, Object>> assignedTasks = buildAssignedTasks(userId);
        List<Map<String, Object>> suggestedTasks = buildSuggestedTasks(userId);
        overview.put("assignedTasks", assignedTasks);
        overview.put("suggestedTasks", suggestedTasks);
        overview.put("mandatoryTasks", mergeTaskLists(assignedTasks, suggestedTasks));
        overview.put("learningPath", buildLearningPath(userId));
        boolean showRecommended = systemConfig.getBoolean("dashboard.showRecommendedCourses", true);
        overview.put("recommendedCourses", showRecommended
                ? buildRecommendedCourses(userId, publishedCourses) : List.of());
        overview.put("showRecommendedCourses", showRecommended);
        overview.put("recentDeductions", buildRecentDeductions(userId));
        // 管理端可配置的首页展示内容
        overview.put("announcement", systemConfig.getString("dashboard.announcement", ""));
        overview.put("recommendBanner", systemConfig.getJson("dashboard.recommendBanner", Map.of()));
        return overview;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudyTrend(String userId, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        Map<LocalDate, Double> dailyMinutes = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            dailyMinutes.put(d, 0.0);
        }

        for (UserProgress p : progressRepo.findByUserIdWithDetails(userId)) {
            if (p.getLastAccessAt() == null) continue;
            LocalDate day = p.getLastAccessAt().toLocalDate();
            if (day.isBefore(start) || day.isAfter(end)) continue;
            dailyMinutes.merge(day, (double) chapterStudiedMinutes(p), Double::sum);
        }

        for (TrainingRecord r : recordRepo.findByUserIdOrderByCreatedAtDesc(userId)) {
            if (r.getEndTime() == null) continue;
            LocalDate day = r.getEndTime().toLocalDate();
            if (day.isBefore(start) || day.isAfter(end)) continue;
            dailyMinutes.merge(day, (double) trainingStudiedMinutes(r), Double::sum);
        }

        for (SimulationSession s : simulationSessionRepo.findByUserIdOrderByStartedAtDesc(userId)) {
            if (!"completed".equals(s.getStatus())) continue;
            LocalDate day = s.getFinishedAt() != null
                    ? s.getFinishedAt().toLocalDate()
                    : (s.getStartedAt() != null ? s.getStartedAt().toLocalDate() : null);
            if (day == null || day.isBefore(start) || day.isAfter(end)) continue;
            double mins = s.getElapsedMs() != null && s.getElapsedMs() > 0
                    ? s.getElapsedMs() / 60000.0
                    : 5;
            dailyMinutes.merge(day, mins, Double::sum);
        }

        List<String> labels = new ArrayList<>();
        List<Double> hours = new ArrayList<>();
        double totalHours = 0;
        for (var entry : dailyMinutes.entrySet()) {
            labels.add(entry.getKey().format(DAY_LABEL));
            double h = Math.round(entry.getValue() / 60.0 * 10) / 10.0;
            hours.add(h);
            totalHours += h;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", days);
        result.put("labels", labels);
        result.put("hours", hours);
        result.put("totalHours", Math.round(totalHours * 10) / 10.0);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAbilityRadar(String userId) {
        List<UserProgress> progress = progressRepo.findByUserIdWithDetails(userId);

        double theory = avgMastery(progress);

        Double trainAvg = recordRepo.avgCompletedScoreByUserId(userId);
        double emergency = trainAvg != null ? trainAvg : 0;

        List<String> sessionIds = simulationSessionRepo.findByUserIdOrderByStartedAtDesc(userId).stream()
                .filter(s -> "completed".equals(s.getStatus()))
                .map(SimulationSession::getId)
                .toList();
        List<SimulationScoreReport> reports = sessionIds.isEmpty()
                ? List.of()
                : scoreReportRepo.findBySessionIdIn(sessionIds);

        double timeliness = avgDimensionScore(reports, "timeliness");
        double procedure = avgDimensionScore(reports, "procedure");
        double decision = avgDimensionScore(reports, "decision");
        double outcome = avgDimensionScore(reports, "outcome");

        List<Map<String, Object>> dimensions = new ArrayList<>();
        dimensions.add(dim("理论知识", fallback(theory, 0)));
        dimensions.add(dim("应急处置", fallback(emergency, theory * 0.85)));
        dimensions.add(dim("响应时效", fallback(timeliness, emergency > 0 ? emergency * 0.9 : theory * 0.8)));
        dimensions.add(dim("操作规范", fallback(procedure, theory * 0.88)));
        dimensions.add(dim("决策判断", fallback(decision, emergency > 0 ? emergency * 0.92 : theory * 0.82)));
        dimensions.add(dim("事故控制", fallback(outcome, emergency > 0 ? emergency * 0.88 : theory * 0.85)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dimensions", dimensions);
        result.put("hasDeductionData", !reports.isEmpty());
        result.put("overallScore", Math.round(dimensions.stream()
                .mapToInt(d -> ((Number) d.get("value")).intValue())
                .average().orElse(0)));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLearningCalendar(String userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        Map<String, Map<String, Object>> dayMap = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", d.toString());
            day.put("studyMinutes", 0);
            day.put("events", new ArrayList<Map<String, Object>>());
            dayMap.put(d.toString(), day);
        }

        for (UserProgress p : progressRepo.findByUserIdWithDetails(userId)) {
            if (p.getLastAccessAt() == null) continue;
            LocalDate day = p.getLastAccessAt().toLocalDate();
            if (day.isBefore(start) || day.isAfter(end)) continue;
            addCalendarEvent(dayMap, day, chapterStudiedMinutes(p), "course",
                    p.getChapter() != null ? "学习章节：" + p.getChapter().getTitle() : "课程学习",
                    p.getCourse() != null ? "/user/courses/" + p.getCourse().getId()
                            + "/chapters/" + (p.getChapter() != null ? p.getChapter().getId() : "") : null);
        }

        for (TrainingRecord r : recordRepo.findByUserIdOrderByCreatedAtDesc(userId)) {
            if (r.getEndTime() == null) continue;
            LocalDate day = r.getEndTime().toLocalDate();
            if (day.isBefore(start) || day.isAfter(end)) continue;
            String title = r.getScenario() != null ? "完成训练：" + r.getScenario().getName() : "应急训练";
            addCalendarEvent(dayMap, day, trainingStudiedMinutes(r), "training", title,
                    r.getScenario() != null ? "/user/training/" + r.getScenario().getId() : null);
        }

        for (SimulationSession s : simulationSessionRepo.findByUserIdOrderByStartedAtDesc(userId)) {
            if (!"completed".equals(s.getStatus())) continue;
            LocalDate day = s.getFinishedAt() != null ? s.getFinishedAt().toLocalDate()
                    : (s.getStartedAt() != null ? s.getStartedAt().toLocalDate() : null);
            if (day == null || day.isBefore(start) || day.isAfter(end)) continue;
            int mins = s.getElapsedMs() != null && s.getElapsedMs() > 0
                    ? (int) Math.ceil(s.getElapsedMs() / 60000.0) : 5;
            addCalendarEvent(dayMap, day, mins, "simulation", "完成事故推演",
                    "/user/simulation/records");
        }

        List<Map<String, Object>> days = new ArrayList<>(dayMap.values());
        int activeDays = (int) days.stream()
                .filter(d -> ((Number) d.get("studyMinutes")).intValue() > 0).count();
        int totalMinutes = days.stream().mapToInt(d -> ((Number) d.get("studyMinutes")).intValue()).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("activeDays", activeDays);
        result.put("totalMinutes", totalMinutes);
        result.put("days", days);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLearningHistory(String userId) {
        List<Map<String, Object>> history = new ArrayList<>();

        for (UserProgress p : progressRepo.findByUserIdWithDetails(userId)) {
            if (p.getLastAccessAt() == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "course");
            item.put("time", p.getLastAccessAt().toString());
            item.put("title", p.getChapter() != null ? p.getChapter().getTitle() : "章节学习");
            item.put("subtitle", p.getCourse() != null ? p.getCourse().getTitle() : "");
            item.put("progress", p.getProgress());
            item.put("completed", p.getCompleted());
            if (p.getCourse() != null && p.getChapter() != null) {
                item.put("link", "/user/courses/" + p.getCourse().getId() + "/chapters/" + p.getChapter().getId());
            }
            history.add(item);
        }

        for (TrainingRecord r : recordRepo.findByUserIdOrderByCreatedAtDesc(userId)) {
            if (r.getEndTime() == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "training");
            item.put("time", r.getEndTime().toString());
            item.put("title", r.getScenario() != null ? r.getScenario().getName() : "应急训练");
            item.put("subtitle", "得分 " + (r.getTotalScore() != null ? r.getTotalScore() : "-"));
            item.put("link", r.getScenario() != null ? "/user/training/" + r.getScenario().getId() : null);
            history.add(item);
        }

        history.sort((a, b) -> String.valueOf(b.get("time")).compareTo(String.valueOf(a.get("time"))));
        return history.stream().limit(30).toList();
    }

    @SuppressWarnings("unchecked")
    private void addCalendarEvent(Map<String, Map<String, Object>> dayMap, LocalDate day,
                                  int minutes, String type, String title, String link) {
        Map<String, Object> cell = dayMap.get(day.toString());
        if (cell == null) return;
        cell.put("studyMinutes", ((Number) cell.get("studyMinutes")).intValue() + minutes);
        List<Map<String, Object>> events = (List<Map<String, Object>>) cell.get("events");
        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("type", type);
        ev.put("title", title);
        ev.put("minutes", minutes);
        if (link != null) ev.put("link", link);
        events.add(ev);
    }

    public List<Map<String, Object>> getRecentCourses(String userId) {
        List<UserProgress> progressList = progressRepo.findRecentByUserId(userId);

        Map<String, Integer> courseProgress = new LinkedHashMap<>();
        for (UserProgress p : progressList) {
            String courseId = p.getCourse().getId();
            courseProgress.merge(courseId, p.getProgress(), Math::max);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (var entry : courseProgress.entrySet()) {
            courseRepo.findById(entry.getKey()).ifPresent(course -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", course.getId());
                m.put("title", course.getTitle());
                m.put("category", course.getCategory());
                m.put("progress", entry.getValue());
                result.add(m);
            });
        }

        if (result.isEmpty()) {
            courseRepo.findByStatusOrderByCreatedAtDesc("published").stream().limit(5).forEach(course -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", course.getId());
                m.put("title", course.getTitle());
                m.put("category", course.getCategory());
                m.put("progress", 0);
                result.add(m);
            });
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTrainingAlertsForUser(String userId) {
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return List.of();
        String dept = user.getDepartment() != null ? user.getDepartment().trim() : "";
        String username = user.getUsername();

        return trainingAlertRepo.findAll().stream()
                .filter(a -> !"resolved".equalsIgnoreCase(a.getStatus()))
                .filter(a -> matchesAlertAudience(a, dept, username))
                .sorted(Comparator.comparing(AdminTrainingAlert::getOccurredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(20)
                .map(this::toTrainingAlertInfo)
                .toList();
    }

    private boolean matchesAlertAudience(AdminTrainingAlert alert, String dept, String username) {
        String alertDept = alert.getDepartment();
        if (alertDept != null && !alertDept.isBlank() && !"全员".equals(alertDept.trim())) {
            if (dept.isBlank() || !alertDept.trim().equals(dept)) return false;
        }
        String trainee = alert.getTraineeName();
        if (trainee != null && !trainee.isBlank()) {
            return trainee.trim().equalsIgnoreCase(username)
                    || trainee.trim().equalsIgnoreCase(resolveDisplayName(userRepo.findByUsername(username).orElse(null)));
        }
        return true;
    }

    private Map<String, Object> toTrainingAlertInfo(AdminTrainingAlert alert) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", alert.getId());
        m.put("alertNo", alert.getAlertNo());
        m.put("type", alert.getType());
        m.put("level", alert.getLevel());
        m.put("title", alert.getTitle());
        m.put("description", alert.getDescription());
        m.put("status", alert.getStatus());
        m.put("actionPath", alert.getActionPath());
        m.put("occurredAt", alert.getOccurredAt() != null ? alert.getOccurredAt().toString() : null);
        return m;
    }

    private String resolveDisplayName(User user) {
        if (user == null) return "";
        return switch (user.getUsername()) {
            case "zhanggong" -> "张工";
            case "lisi" -> "李工";
            case "wangwu" -> "王工";
            default -> user.getUsername();
        };
    }

    private long calcJoinDays(LocalDateTime createdAt) {
        if (createdAt == null) return 1;
        return Math.max(1, ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDate.now()) + 1);
    }

    private int calcStreakDays(List<UserProgress> progressList) {
        if (progressList.isEmpty()) return 0;
        Set<LocalDate> activeDays = new HashSet<>();
        for (UserProgress p : progressList) {
            if (p.getLastAccessAt() != null) {
                activeDays.add(p.getLastAccessAt().toLocalDate());
            }
        }
        int streak = 0;
        LocalDate day = LocalDate.now();
        while (activeDays.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return Math.max(streak, activeDays.isEmpty() ? 0 : 1);
    }

    private long countIncompleteChapters(String userId, String courseId) {
        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
        long incomplete = 0;
        for (Chapter ch : chapters) {
            boolean done = progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId());
            if (!done) incomplete++;
        }
        return incomplete;
    }

    private long countCompletedCourses(String userId, List<Course> courses) {
        return courses.stream().filter(course -> {
            List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId());
            if (chapters.isEmpty()) return false;
            return chapters.stream().allMatch(ch ->
                    progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId()));
        }).count();
    }

    private Map<String, Object> buildContinueLearning(String userId, List<UserProgress> progressList) {
        List<UserProgress> sorted = progressList.stream()
                .sorted(Comparator.comparing(
                        UserProgress::getLastAccessAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        if (!sorted.isEmpty()) {
            UserProgress mostRecent = sorted.get(0);
            if (!Boolean.TRUE.equals(mostRecent.getCompleted())) {
                return toContinueLearningMap(mostRecent);
            }
            Course course = mostRecent.getCourse();
            Chapter lastChapter = mostRecent.getChapter();
            if (course != null && lastChapter != null) {
                Optional<Chapter> next = findNextIncompleteChapter(userId, course.getId(), lastChapter.getId());
                if (next.isPresent()) {
                    int progress = progressRepo.findByUserIdAndChapterId(userId, next.get().getId())
                            .map(p -> p.getProgress() != null ? p.getProgress() : 0)
                            .orElse(0);
                    return toContinueLearningMap(course, next.get(), progress);
                }
            }
            Optional<UserProgress> inProgress = sorted.stream()
                    .filter(p -> !Boolean.TRUE.equals(p.getCompleted()))
                    .findFirst();
            if (inProgress.isPresent()) {
                return toContinueLearningMap(inProgress.get());
            }
            return findFirstIncompleteChapter(userId).orElse(null);
        }

        return findFirstIncompleteChapter(userId).orElse(null);
    }

    private Optional<Map<String, Object>> findFirstIncompleteChapter(String userId) {
        for (Course course : courseRepo.findByStatusOrderByCreatedAtDesc("published")) {
            for (Chapter ch : chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId())) {
                if (!progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId())) {
                    int progress = progressRepo.findByUserIdAndChapterId(userId, ch.getId())
                            .map(p -> p.getProgress() != null ? p.getProgress() : 0)
                            .orElse(0);
                    return Optional.of(toContinueLearningMap(course, ch, progress));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Chapter> findNextIncompleteChapter(String userId, String courseId, String afterChapterId) {
        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
        boolean passed = false;
        for (Chapter ch : chapters) {
            if (!passed) {
                if (ch.getId().equals(afterChapterId)) passed = true;
                continue;
            }
            if (!progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId())) {
                return Optional.of(ch);
            }
        }
        for (Chapter ch : chapters) {
            if (!progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId())) {
                return Optional.of(ch);
            }
        }
        return Optional.empty();
    }

    private Map<String, Object> toContinueLearningMap(UserProgress p) {
        return toContinueLearningMap(p.getCourse(), p.getChapter(),
                p.getProgress() != null ? p.getProgress() : 0);
    }

    private Map<String, Object> toContinueLearningMap(Course course, Chapter chapter, int progress) {
        int duration = chapter != null && chapter.getDuration() != null ? chapter.getDuration() : 30;
        int pct = Math.min(100, Math.max(0, progress));
        int remaining = pct >= 100 ? 0 : Math.max(1, duration * (100 - pct) / 100);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("courseId", course != null ? course.getId() : null);
        m.put("chapterId", chapter != null ? chapter.getId() : null);
        m.put("courseTitle", course != null ? course.getTitle() : "");
        m.put("chapterTitle", chapter != null ? chapter.getTitle() : "开始学习");
        m.put("progress", pct);
        m.put("remainingMinutes", remaining);
        return m;
    }

    private List<Map<String, Object>> buildAssignedTasks(String userId) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (LearnerAssignment assignment : assignmentRepo.findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, "completed")) {
            tasks.add(toTaskMap(assignment));
            if (tasks.size() >= 5) break;
        }
        return tasks;
    }

    private Map<String, Object> toTaskMap(LearnerAssignment assignment) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", assignment.getId());
        task.put("title", assignment.getTitle());
        task.put("type", assignment.getType());
        task.put("deadline", assignment.getDeadline() != null ? assignment.getDeadline() : "建议尽快完成");
        task.put("status", mapAssignmentStatus(assignment.getStatus()));
        task.put("actionText", assignment.getActionText() != null ? assignment.getActionText() : "去完成");
        task.put("link", assignment.getLink());
        task.put("assigned", true);
        return task;
    }

    private List<Map<String, Object>> buildSuggestedTasks(String userId) {
        List<Map<String, Object>> tasks = new ArrayList<>();

        List<Scenario> scenarios = scenarioRepo.findAll();
        if (!scenarios.isEmpty()) {
            Scenario next = scenarios.stream()
                    .filter(s -> recordRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                            .noneMatch(r -> r.getScenario() != null && s.getId().equals(r.getScenario().getId()) && r.getEndTime() != null))
                    .findFirst().orElse(null);
            if (next != null) {
                Map<String, Object> task = new HashMap<>();
                task.put("id", "suggest-training-" + next.getId());
                task.put("title", next.getName());
                task.put("type", "training");
                task.put("deadline", "建议尽快完成");
                task.put("status", "待考核");
                task.put("actionText", "去训练");
                task.put("link", "/training/" + next.getId());
                task.put("assigned", false);
                tasks.add(task);
            }
        }

        courseRepo.findByStatusOrderByCreatedAtDesc("published").stream()
                .filter(course -> countIncompleteChapters(userId, course.getId()) > 0)
                .limit(2)
                .forEach(course -> {
                    List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId());
                    long done = chapters.stream()
                            .filter(ch -> progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId()))
                            .count();
                    Map<String, Object> task = new HashMap<>();
                    task.put("id", "suggest-course-" + course.getId());
                    task.put("title", course.getTitle());
                    task.put("type", "course");
                    task.put("deadline", "未开始 (" + done + "/" + chapters.size() + " 课时)");
                    task.put("status", done == 0 ? "未开始" : "进行中");
                    task.put("actionText", "去学习");
                    task.put("link", "/courses/" + course.getId());
                    task.put("assigned", false);
                    tasks.add(task);
                });

        return tasks.stream().limit(4).toList();
    }

    private List<Map<String, Object>> mergeTaskLists(List<Map<String, Object>> assigned, List<Map<String, Object>> suggested) {
        List<Map<String, Object>> merged = new ArrayList<>(assigned);
        for (Map<String, Object> task : suggested) {
            if (merged.size() >= 5) break;
            merged.add(task);
        }
        return merged;
    }

    private List<Map<String, Object>> buildLearningPath(String userId) {
        long completedChapters = progressRepo.countByUserIdAndCompletedTrue(userId);
        boolean theoryStarted = completedChapters > 0;
        boolean theoryDone = completedChapters >= 5;
        boolean caseDone = caseProgressRepo.findByUserId(userId).stream()
                .anyMatch(p -> Boolean.TRUE.equals(p.getCompleted()));
        long simCount = simulationSessionRepo.countByUserIdAndStatus(userId, "completed");
        boolean simDone = simCount > 0;
        boolean trainDone = recordRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .anyMatch(r -> r.getEndTime() != null);

        List<Map<String, Object>> path = new ArrayList<>();
        path.add(learningPathStep("theory", "1. 安全理论", "学习储能基础知识与运维规范", "/courses/list",
                theoryDone ? "done" : (theoryStarted ? "current" : "current")));
        path.add(learningPathStep("case", "2. 案例复盘", "研读典型事故案例与教训", "/cases/list",
                caseDone ? "done" : (theoryStarted ? "current" : "locked")));
        path.add(learningPathStep("simulation", "3. 事故推演", "三维场景观察热失控演化", "/simulation/scenarios",
                simDone ? "done" : (caseDone || theoryDone ? "current" : "locked")));
        path.add(learningPathStep("training", "4. 应急训练", "分支决策练习处置流程", "/training/scenarios",
                trainDone ? "done" : (simDone ? "current" : "locked")));
        return path;
    }

    private Map<String, Object> learningPathStep(String key, String title, String description, String link, String status) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("key", key);
        step.put("title", title);
        step.put("description", description);
        step.put("link", link);
        step.put("status", status);
        return step;
    }

    /** @deprecated 兼容旧调用，请使用 buildAssignedTasks + buildSuggestedTasks */
    private List<Map<String, Object>> buildMandatoryTasks(String userId) {
        return mergeTaskLists(buildAssignedTasks(userId), buildSuggestedTasks(userId));
    }

    private String mapAssignmentStatus(String status) {
        if (status == null) return "待完成";
        return switch (status) {
            case "in_progress" -> "进行中";
            case "completed" -> "已完成";
            default -> "待完成";
        };
    }

    private List<Map<String, Object>> buildRecommendedCourses(String userId, List<Course> courses) {
        return courses.stream()
                .filter(course -> !isCourseFullyCompleted(userId, course.getId()))
                .limit(4)
                .map(course -> {
                    List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId());
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", course.getId());
                    m.put("title", course.getTitle());
                    m.put("description", course.getDescription());
                    m.put("coverImage", course.getCoverImage());
                    m.put("totalDuration", course.getTotalDuration());
                    m.put("chapterCount", chapters.size());
                    m.put("tag", course.getCategory().equals("thermal") ? "3D 互动" : "视频课程");
                    m.put("meta", (course.getTotalDuration() != null ? course.getTotalDuration() : 0) + "分钟 · "
                            + chapters.size() + "章节");
                    return m;
                }).toList();
    }

    private List<Map<String, Object>> buildRecentDeductions(String userId) {
        return simulationSessionRepo.findByUserIdOrderByStartedAtDesc(userId).stream()
                .filter(s -> "completed".equals(s.getStatus()))
                .limit(5)
                .map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("sessionId", s.getId());
                    m.put("scenarioId", s.getScenarioId());
                    m.put("outcome", s.getOutcome());
                    m.put("totalScore", s.getTotalScore());
                    m.put("rating", s.getRating());
                    m.put("finishedAt", s.getFinishedAt() != null ? s.getFinishedAt().toString() : null);
                    scenarioRepo.findById(s.getScenarioId()).ifPresent(sc -> m.put("scenarioName", sc.getName()));
                    return m;
                }).toList();
    }

    private boolean isCourseFullyCompleted(String userId, String courseId) {
        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
        if (chapters.isEmpty()) return false;
        return chapters.stream().allMatch(ch ->
                progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId()));
    }

    private int calcCourseStudyMinutes(List<UserProgress> progressList) {
        return progressList.stream().mapToInt(this::chapterStudiedMinutes).sum();
    }

    private int chapterStudiedMinutes(UserProgress p) {
        if (p.getStudySeconds() != null && p.getStudySeconds() > 0) {
            return (int) Math.ceil(p.getStudySeconds() / 60.0);
        }
        int duration = p.getChapter() != null && p.getChapter().getDuration() != null
                ? p.getChapter().getDuration() : 30;
        if (Boolean.TRUE.equals(p.getCompleted())) return duration;
        int pct = p.getProgress() != null ? p.getProgress() : 0;
        return duration * pct / 100;
    }

    private int calcTrainingStudyMinutes(String userId) {
        return recordRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(r -> r.getEndTime() != null)
                .mapToInt(this::trainingStudiedMinutes)
                .sum();
    }

    private int trainingStudiedMinutes(TrainingRecord r) {
        if (r.getScenario() != null && r.getScenario().getDuration() != null) {
            return r.getScenario().getDuration();
        }
        if (r.getStartTime() != null && r.getEndTime() != null) {
            long mins = ChronoUnit.MINUTES.between(r.getStartTime(), r.getEndTime());
            return (int) Math.max(1, mins);
        }
        return 30;
    }

    private int calcSimulationStudyMinutes(String userId) {
        return simulationSessionRepo.findByUserIdOrderByStartedAtDesc(userId).stream()
                .filter(s -> "completed".equals(s.getStatus()))
                .mapToInt(s -> s.getElapsedMs() != null && s.getElapsedMs() > 0
                        ? (int) Math.ceil(s.getElapsedMs() / 60000.0)
                        : 5)
                .sum();
    }

    private double avgMastery(List<UserProgress> progress) {
        OptionalDouble fromMastery = progress.stream()
                .filter(p -> p.getMasteryLevel() != null && p.getMasteryLevel() > 0)
                .mapToInt(UserProgress::getMasteryLevel)
                .average();
        if (fromMastery.isPresent()) return fromMastery.getAsDouble();
        return progress.stream()
                .mapToInt(p -> p.getProgress() != null ? p.getProgress() : 0)
                .average()
                .orElse(0);
    }

    private double avgCategoryMastery(List<UserProgress> progress, String category) {
        return progress.stream()
                .filter(p -> p.getCourse() != null && category.equals(p.getCourse().getCategory()))
                .mapToInt(p -> {
                    if (p.getMasteryLevel() != null && p.getMasteryLevel() > 0) return p.getMasteryLevel();
                    return p.getProgress() != null ? p.getProgress() : 0;
                })
                .average()
                .orElse(0);
    }

    private double avgDimensionScore(List<SimulationScoreReport> reports, String key) {
        if (reports.isEmpty()) return 0;
        double sum = 0;
        int count = 0;
        for (SimulationScoreReport r : reports) {
            Integer score = extractDimensionScore(r.getDimensions(), key);
            if (score != null) {
                sum += score * 4;
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    private Integer extractDimensionScore(String dimensionsJson, String key) {
        if (dimensionsJson == null || dimensionsJson.isBlank()) return null;
        try {
            Map<String, Object> dims = objectMapper.readValue(dimensionsJson, new TypeReference<>() {});
            Object val = dims.get(key);
            if (val instanceof Map<?, ?> m) {
                Object score = m.get("score");
                if (score instanceof Number n) return n.intValue();
            }
        } catch (Exception ignored) {
            // ignore malformed JSON
        }
        return null;
    }

    private Map<String, Object> dim(String name, double value) {
        return Map.of("name", name, "value", (int) Math.round(Math.min(100, Math.max(0, value))));
    }

    private double fallback(double primary, double secondary) {
        return primary > 0 ? primary : secondary;
    }
}

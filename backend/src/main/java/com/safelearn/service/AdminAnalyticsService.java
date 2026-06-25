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
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private static final List<String> CHART_COLORS = List.of(
            "#3b82f6", "#10b981", "#f59e0b", "#8b5cf6", "#ef4444", "#06b6d4", "#64748b"
    );

    private final UserProgressRepository progressRepo;
    private final QuizAttemptRepository quizAttemptRepo;
    private final SimulationSessionRepository simulationSessionRepo;
    private final SimulationScoreReportRepository scoreReportRepo;
    private final TrainingRecordRepository trainingRecordRepo;
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final ScenarioRepository scenarioRepo;
    private final QaRecordRepository qaRecordRepo;
    private final UserCertificateRepository certificateRepo;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics() {
        return getAnalytics(null, null, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics(String department, LocalDate from, LocalDate to) {
        AnalyticsScope scope = AnalyticsScope.create(userRepo, department, from, to);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", buildSummary(scope));
        result.put("filters", Map.of(
                "department", scope.department == null ? "all" : scope.department,
                "from", scope.from.toString(),
                "to", scope.to.toString()
        ));
        result.put("departments", scope.departments);
        result.put("learningEffect", buildLearningEffect(scope));
        result.put("simulationEffect", buildSimulationEffect(scope));
        result.put("userActivity", buildUserActivity(scope));
        result.put("departmentCompare", buildDepartmentCompare(scope));
        result.put("aiUsage", buildAiUsage(scope));
        result.put("certificates", buildCertificates(scope));
        result.put("exportRows", buildExportRows(scope));
        result.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        return result;
    }

    private Map<String, Object> buildSummary(AnalyticsScope scope) {
        List<UserProgress> progress = progressRepo.findAll().stream()
                .filter(item -> scope.containsUser(item.getUser()))
                .filter(item -> scope.inRange(item.getLastAccessAt()))
                .toList();
        Set<String> completedLearners = progress.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCompleted()))
                .map(item -> item.getUser().getId())
                .collect(Collectors.toSet());

        List<QuizAttempt> attempts = quizAttemptRepo.findAll().stream()
                .filter(item -> scope.containsUser(item.getUser()))
                .filter(item -> scope.inRange(item.getCompletedAt() != null
                        ? item.getCompletedAt() : item.getStartedAt()))
                .toList();
        long passed = attempts.stream().filter(item -> Boolean.TRUE.equals(item.getPassed())).count();
        int traineeCount = scope.userIds.size();
        double completionRate = traineeCount == 0 ? 0
                : round(completedLearners.size() * 100.0 / traineeCount, 1);
        double examPassRate = attempts.isEmpty() ? 0 : round(passed * 100.0 / attempts.size(), 1);
        int incomplete = Math.max(0, traineeCount - completedLearners.size());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("traineeCount", traineeCount);
        summary.put("completionRate", completionRate);
        summary.put("examPassRate", examPassRate);
        summary.put("incompleteTraining", incomplete);
        return summary;
    }

    private List<Map<String, Object>> buildExportRows(AnalyticsScope scope) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> item : buildCourseCompletionRank(scope, null)) {
            if ("暂无课程数据".equals(item.get("name"))) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("section", "课程完成率");
            row.put("name", item.get("name"));
            row.put("value", item.get("value") + "%");
            row.put("learners", item.get("learners"));
            row.put("completed", item.get("completed"));
            rows.add(row);
        }
        for (Map<String, Object> item : buildDeptScoreRank(scope)) {
            if ("暂无部门得分".equals(item.get("name"))) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("section", "部门均分");
            row.put("name", item.get("name"));
            row.put("value", item.get("value"));
            row.put("samples", item.get("samples"));
            rows.add(row);
        }
        return rows;
    }

    // ── 1. 学习效果分析 ──

    private Map<String, Object> buildLearningEffect(AnalyticsScope scope) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("courseCompletionRank", buildCourseCompletionRank(scope, 5));
        section.put("quizPassRateTrend", buildQuizPassRateTrend(scope));
        section.put("masteryDistribution", buildMasteryDistribution(scope));
        return section;
    }

    private List<Map<String, Object>> buildCourseCompletionRank(AnalyticsScope scope, Integer limit) {
        List<Course> courses = courseRepo.findByStatusOrderByCreatedAtDesc("published");
        Map<String, Integer> chapterCounts = new HashMap<>();
        for (Course c : courses) {
            chapterCounts.put(c.getId(), chapterRepo.findByCourseIdOrderByOrderNumAsc(c.getId()).size());
        }

        Map<String, Set<String>> usersByCourse = new HashMap<>();
        Map<String, Map<String, Set<String>>> completedChaptersByUserCourse = new HashMap<>();

        for (UserProgress up : progressRepo.findAll()) {
            if (up.getCourse() == null || up.getUser() == null || !scope.containsUser(up.getUser())) continue;
            String courseId = up.getCourse().getId();
            String userId = up.getUser().getId();
            usersByCourse.computeIfAbsent(courseId, k -> new HashSet<>()).add(userId);
            if (Boolean.TRUE.equals(up.getCompleted()) && up.getChapter() != null) {
                completedChaptersByUserCourse
                        .computeIfAbsent(courseId, k -> new HashMap<>())
                        .computeIfAbsent(userId, k -> new HashSet<>())
                        .add(up.getChapter().getId());
            }
        }

        List<Map<String, Object>> ranked = new ArrayList<>();
        for (Course course : courses) {
            int totalChapters = chapterCounts.getOrDefault(course.getId(), 0);
            if (totalChapters == 0) continue;
            Set<String> users = usersByCourse.getOrDefault(course.getId(), Set.of());
            if (users.isEmpty()) continue;

            long fullyCompleted = users.stream().filter(uid -> {
                Set<String> done = completedChaptersByUserCourse
                        .getOrDefault(course.getId(), Map.of())
                        .getOrDefault(uid, Set.of());
                return done.size() >= totalChapters;
            }).count();

            int rate = (int) Math.round(fullyCompleted * 100.0 / users.size());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", course.getTitle());
            item.put("value", rate);
            item.put("learners", users.size());
            item.put("completed", fullyCompleted);
            ranked.add(item);
        }

        ranked.sort((a, b) -> Integer.compare((Integer) b.get("value"), (Integer) a.get("value")));
        List<Map<String, Object>> result = limit == null
                ? new ArrayList<>(ranked)
                : new ArrayList<>(ranked.stream().limit(limit).toList());
        for (int i = 0; i < result.size(); i++) {
            result.get(i).put("color", CHART_COLORS.get(i % CHART_COLORS.size()));
        }
        return result.isEmpty() ? List.of(chartItem("暂无课程数据", 0, "#d1d5db")) : result;
    }

    private Map<String, Object> buildQuizPassRateTrend(AnalyticsScope scope) {
        List<String> months = new ArrayList<>();
        List<Integer> rates = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月");

        for (YearMonth month : scope.monthsInRange(12)) {
            months.add(month.format(fmt));
            List<QuizAttempt> attempts = quizAttemptRepo.findAll().stream()
                    .filter(a -> scope.containsUser(a.getUser()))
                    .filter(a -> a.getCompletedAt() != null || a.getStartedAt() != null)
                    .filter(a -> {
                        LocalDateTime t = a.getCompletedAt() != null ? a.getCompletedAt() : a.getStartedAt();
                        return YearMonth.from(t).equals(month);
                    })
                    .toList();

            if (attempts.isEmpty()) {
                rates.add(0);
            } else {
                long passed = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();
                rates.add((int) Math.round(passed * 100.0 / attempts.size()));
            }
        }

        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("months", months);
        trend.put("rates", rates);
        return trend;
    }

    private List<Map<String, Object>> buildMasteryDistribution(AnalyticsScope scope) {
        Map<String, Long> buckets = new LinkedHashMap<>();
        buckets.put("未掌握", 0L);
        buckets.put("初步了解", 0L);
        buckets.put("基本掌握", 0L);
        buckets.put("熟练掌握", 0L);
        buckets.put("精通", 0L);

        for (UserProgress up : progressRepo.findAll()) {
            if (!scope.containsUser(up.getUser()) || !Boolean.TRUE.equals(up.getCompleted())) continue;
            int level = up.getMasteryLevel() != null ? up.getMasteryLevel() : 0;
            if (level <= 0) {
                int progress = up.getProgress() != null ? up.getProgress() : 0;
                level = progress >= 90 ? 4 : progress >= 70 ? 3 : progress >= 50 ? 2 : progress >= 20 ? 1 : 0;
            }
            String key = switch (level) {
                case 1 -> "初步了解";
                case 2 -> "基本掌握";
                case 3 -> "熟练掌握";
                case 4, 5 -> "精通";
                default -> "未掌握";
            };
            buckets.merge(key, 1L, Long::sum);
        }

        if (buckets.values().stream().allMatch(v -> v == 0)) {
            return List.of(chartItem("暂无完成记录", 0, "#d1d5db"));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        int i = 0;
        for (var e : buckets.entrySet()) {
            if (e.getValue() > 0) {
                result.add(chartItem(e.getKey(), e.getValue(), CHART_COLORS.get(i++ % CHART_COLORS.size())));
            }
        }
        return result;
    }

    // ── 2. 训练推演效果 ──

    private Map<String, Object> buildSimulationEffect(AnalyticsScope scope) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("scoreDistribution", buildScoreDistribution(scope));
        section.put("scenarioSuccessRate", buildScenarioSuccessRate(scope));
        section.put("decisionRadar", buildDecisionRadar(scope));
        return section;
    }

    private List<Map<String, Object>> buildScoreDistribution(AnalyticsScope scope) {
        String[] labels = {"0-59分", "60-69分", "70-79分", "80-89分", "90-100分"};
        long[] counts = new long[5];

        for (SimulationSession s : simulationSessionRepo.findAll()) {
            if (!scope.containsUserId(s.getUserId()) || s.getTotalScore() == null) continue;
            int score = s.getTotalScore();
            if (score < 60) counts[0]++;
            else if (score < 70) counts[1]++;
            else if (score < 80) counts[2]++;
            else if (score < 90) counts[3]++;
            else counts[4]++;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", labels[i]);
            item.put("value", counts[i]);
            item.put("color", CHART_COLORS.get(i % CHART_COLORS.size()));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildScenarioSuccessRate(AnalyticsScope scope) {
        Map<String, long[]> stats = new LinkedHashMap<>();
        for (SimulationSession s : simulationSessionRepo.findAll()) {
            if (!scope.containsUserId(s.getUserId())) continue;
            if (!"completed".equals(s.getStatus()) && s.getFinishedAt() == null) continue;
            stats.computeIfAbsent(s.getScenarioId(), k -> new long[2]);
            stats.get(s.getScenarioId())[0]++;
            if ("success".equalsIgnoreCase(s.getOutcome())) {
                stats.get(s.getScenarioId())[1]++;
            }
        }

        Map<String, String> scenarioNames = scenarioRepo.findAll().stream()
                .collect(Collectors.toMap(Scenario::getId, Scenario::getName, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (var e : stats.entrySet()) {
            long total = e.getValue()[0];
            if (total == 0) continue;
            int rate = (int) Math.round(e.getValue()[1] * 100.0 / total);
            Map<String, Object> item = new LinkedHashMap<>();
            String name = scenarioNames.getOrDefault(e.getKey(), "场景-" + e.getKey().substring(0, 8));
            item.put("name", name.length() > 12 ? name.substring(0, 12) + "…" : name);
            item.put("fullName", name);
            item.put("value", rate);
            item.put("total", total);
            result.add(item);
        }

        result.sort((a, b) -> Integer.compare((Integer) b.get("value"), (Integer) a.get("value")));
        if (result.isEmpty()) {
            return List.of(Map.of("name", "暂无推演", "value", 0, "total", 0));
        }
        return result.stream().limit(8).toList();
    }

    private Map<String, Object> buildDecisionRadar(AnalyticsScope scope) {
        Set<String> scopedSessionIds = simulationSessionRepo.findAll().stream()
                .filter(session -> scope.containsUserId(session.getUserId()))
                .map(SimulationSession::getId)
                .collect(Collectors.toSet());
        List<SimulationScoreReport> reports = scoreReportRepo.findAll().stream()
                .filter(report -> scopedSessionIds.contains(report.getSessionId()))
                .filter(report -> scope.inRange(report.getCreatedAt()))
                .toList();
        Map<String, String> dimLabels = Map.of(
                "timeliness", "时效性",
                "procedure", "规范性",
                "decision", "决策判断",
                "outcome", "事故控制"
        );

        List<Map<String, Object>> indicators = new ArrayList<>();
        List<Number> values = new ArrayList<>();

        for (var entry : dimLabels.entrySet()) {
            double avg = avgDimensionScore(reports, entry.getKey());
            indicators.add(Map.of("name", entry.getValue(), "max", 100));
            values.add((int) Math.round(avg));
        }

        if (reports.isEmpty()) {
            values = List.of(0, 0, 0, 0);
        }

        Map<String, Object> radar = new LinkedHashMap<>();
        radar.put("indicators", indicators);
        radar.put("values", values);
        return radar;
    }

    // ── 3. 用户活跃度 ──

    private Map<String, Object> buildUserActivity(AnalyticsScope scope) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("weeklyActiveTrend", buildWeeklyActiveTrend(scope));
        section.put("studyDurationDistribution", buildStudyDurationDistribution(scope));
        section.put("newUserTrend", buildNewUserTrend(scope));
        return section;
    }

    private Map<String, Object> buildWeeklyActiveTrend(AnalyticsScope scope) {
        WeekFields wf = WeekFields.ISO;
        List<String> weeks = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        LocalDate cursor = scope.to.minusWeeks(11).with(wf.dayOfWeek(), 1);
        if (cursor.isBefore(scope.from)) {
            cursor = scope.from.with(wf.dayOfWeek(), 1);
        }

        while (!cursor.isAfter(scope.to)) {
            weeks.add(cursor.getMonthValue() + "/" + cursor.getDayOfMonth());
            LocalDateTime start = cursor.atStartOfDay();
            LocalDateTime end = cursor.plusDays(7).atStartOfDay();
            long active = progressRepo.findAll().stream()
                    .filter(p -> p.getLastAccessAt() != null && scope.containsUser(p.getUser()))
                    .filter(p -> !p.getLastAccessAt().isBefore(start) && p.getLastAccessAt().isBefore(end))
                    .map(p -> p.getUser().getId())
                    .distinct()
                    .count();
            counts.add(active);
            cursor = cursor.plusWeeks(1);
            if (weeks.size() >= 12) break;
        }

        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("weeks", weeks);
        trend.put("counts", counts);
        return trend;
    }

    private List<Map<String, Object>> buildStudyDurationDistribution(AnalyticsScope scope) {
        Map<String, Integer> userMinutes = new HashMap<>();

        for (UserProgress p : progressRepo.findAll()) {
            if (!scope.containsUser(p.getUser())) continue;
            userMinutes.merge(p.getUser().getId(), chapterStudiedMinutes(p), Integer::sum);
        }
        for (TrainingRecord r : trainingRecordRepo.findAll()) {
            if (!scope.containsUser(r.getUser()) || r.getEndTime() == null) continue;
            if (!scope.inRange(r.getEndTime())) continue;
            userMinutes.merge(r.getUser().getId(), trainingStudiedMinutes(r), Integer::sum);
        }
        for (SimulationSession s : simulationSessionRepo.findAll()) {
            if (!scope.containsUserId(s.getUserId())) continue;
            if (!"completed".equals(s.getStatus()) && s.getFinishedAt() == null) continue;
            int mins = s.getElapsedMs() != null && s.getElapsedMs() > 0
                    ? (int) Math.ceil(s.getElapsedMs() / 60000.0) : 5;
            userMinutes.merge(s.getUserId(), mins, Integer::sum);
        }

        long[] buckets = new long[5];
        String[] labels = {"0-1小时", "1-3小时", "3-5小时", "5-10小时", "10小时以上"};
        for (String userId : scope.userIds) {
            int mins = userMinutes.getOrDefault(userId, 0);
            double hours = mins / 60.0;
            if (hours < 1) buckets[0]++;
            else if (hours < 3) buckets[1]++;
            else if (hours < 5) buckets[2]++;
            else if (hours < 10) buckets[3]++;
            else buckets[4]++;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            result.add(chartItem(labels[i], buckets[i], CHART_COLORS.get(i % CHART_COLORS.size())));
        }
        return result;
    }

    private Map<String, Object> buildNewUserTrend(AnalyticsScope scope) {
        List<String> months = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月");

        for (YearMonth month : scope.monthsInRange(12)) {
            months.add(month.format(fmt));
            long count = scope.usersById.values().stream()
                    .filter(user -> user.getCreatedAt() != null)
                    .filter(user -> YearMonth.from(user.getCreatedAt()).equals(month))
                    .count();
            counts.add(count);
        }

        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("months", months);
        trend.put("counts", counts);
        return trend;
    }

    // ── 4. 部门对比 ──

    private Map<String, Object> buildDepartmentCompare(AnalyticsScope scope) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("deptScoreRank", buildDeptScoreRank(scope));
        section.put("deptActivityHeatmap", buildDeptActivityHeatmap(scope));
        return section;
    }

    private List<Map<String, Object>> buildDeptScoreRank(AnalyticsScope scope) {
        Map<String, List<Integer>> scoresByDept = new HashMap<>();

        for (TrainingRecord r : trainingRecordRepo.findAll()) {
            if (!scope.containsUser(r.getUser()) || r.getTotalScore() == null) continue;
            if (!scope.inRange(r.getEndTime())) continue;
            String dept = deptName(r.getUser());
            scoresByDept.computeIfAbsent(dept, key -> new ArrayList<>()).add(r.getTotalScore());
        }
        for (SimulationSession s : simulationSessionRepo.findAll()) {
            if (!scope.containsUserId(s.getUserId()) || s.getTotalScore() == null) continue;
            User user = scope.usersById.get(s.getUserId());
            if (user == null) continue;
            String dept = deptName(user);
            scoresByDept.computeIfAbsent(dept, key -> new ArrayList<>()).add(s.getTotalScore());
        }

        List<Map<String, Object>> ranked = new ArrayList<>();
        for (var entry : scoresByDept.entrySet()) {
            double avg = entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", entry.getKey());
            item.put("value", (int) Math.round(avg));
            item.put("samples", entry.getValue().size());
            ranked.add(item);
        }
        ranked.sort((a, b) -> Integer.compare((Integer) b.get("value"), (Integer) a.get("value")));

        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).put("color", CHART_COLORS.get(i % CHART_COLORS.size()));
        }
        return ranked.isEmpty() ? List.of(chartItem("暂无部门得分", 0, "#d1d5db")) : ranked;
    }

    private Map<String, Object> buildDeptActivityHeatmap(AnalyticsScope scope) {
        List<String> departments = scope.department != null
                ? List.of(scope.department)
                : new ArrayList<>(scope.departments);
        if (departments.isEmpty()) departments = List.of("未分配部门");

        WeekFields wf = WeekFields.ISO;
        List<String> weeks = new ArrayList<>();
        List<LocalDate> weekStarts = new ArrayList<>();
        LocalDate cursor = scope.to.minusWeeks(7).with(wf.dayOfWeek(), 1);
        if (cursor.isBefore(scope.from)) {
            cursor = scope.from.with(wf.dayOfWeek(), 1);
        }
        while (!cursor.isAfter(scope.to) && weeks.size() < 8) {
            weeks.add(cursor.getMonthValue() + "/" + cursor.getDayOfMonth());
            weekStarts.add(cursor);
            cursor = cursor.plusWeeks(1);
        }

        List<List<Object>> data = new ArrayList<>();
        for (int w = 0; w < weekStarts.size(); w++) {
            LocalDateTime start = weekStarts.get(w).atStartOfDay();
            LocalDateTime end = weekStarts.get(w).plusDays(7).atStartOfDay();
            for (int d = 0; d < departments.size(); d++) {
                String dept = departments.get(d);
                long active = progressRepo.findAll().stream()
                        .filter(p -> p.getLastAccessAt() != null && scope.containsUser(p.getUser()))
                        .filter(p -> !p.getLastAccessAt().isBefore(start) && p.getLastAccessAt().isBefore(end))
                        .filter(p -> dept.equals(deptName(p.getUser())))
                        .map(p -> p.getUser().getId())
                        .distinct()
                        .count();
                data.add(List.of(w, d, active));
            }
        }

        Map<String, Object> heatmap = new LinkedHashMap<>();
        heatmap.put("departments", departments);
        heatmap.put("weeks", weeks);
        heatmap.put("data", data);
        return heatmap;
    }

    // ── 5. AI 助手 ──

    private Map<String, Object> buildAiUsage(AnalyticsScope scope) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("qaTrend", buildQaTrend(scope));
        section.put("qaCategoryDistribution", buildQaCategoryDistribution(scope));
        return section;
    }

    private Map<String, Object> buildQaTrend(AnalyticsScope scope) {
        List<String> months = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月");

        for (YearMonth month : scope.monthsInRange(12)) {
            months.add(month.format(fmt));
            long count = qaRecordRepo.findAll().stream()
                    .filter(q -> q.getCreatedAt() != null)
                    .filter(q -> scope.containsUser(q.getUser()))
                    .filter(q -> YearMonth.from(q.getCreatedAt()).equals(month))
                    .count();
            counts.add(count);
        }

        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("months", months);
        trend.put("counts", counts);
        return trend;
    }

    private List<Map<String, Object>> buildQaCategoryDistribution(AnalyticsScope scope) {
        Map<String, Long> categories = new LinkedHashMap<>();
        categories.put("消防安全", 0L);
        categories.put("隐患排查", 0L);
        categories.put("应急处置", 0L);
        categories.put("设备操作", 0L);
        categories.put("其他", 0L);

        for (QaRecord q : qaRecordRepo.findAll()) {
            if (!scope.containsUser(q.getUser()) || !scope.inRange(q.getCreatedAt())) continue;
            String cat = classifyQuestion(q.getQuestion());
            categories.merge(cat, 1L, Long::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        int i = 0;
        for (var entry : categories.entrySet()) {
            if (entry.getValue() > 0) {
                result.add(chartItem(entry.getKey(), entry.getValue(), CHART_COLORS.get(i++ % CHART_COLORS.size())));
            }
        }
        return result.isEmpty() ? List.of(chartItem("暂无问答", 0, "#d1d5db")) : result;
    }

    // ── 6. 证书成就 ──

    private Map<String, Object> buildCertificates(AnalyticsScope scope) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("issueTrend", buildCertIssueTrend(scope));
        section.put("typeDistribution", buildCertTypeDistribution(scope));
        return section;
    }

    private Map<String, Object> buildCertIssueTrend(AnalyticsScope scope) {
        List<String> months = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月");

        for (YearMonth month : scope.monthsInRange(12)) {
            months.add(month.format(fmt));
            long count = certificateRepo.findAll().stream()
                    .filter(cert -> cert.getIssuedAt() != null)
                    .filter(cert -> scope.containsUserId(cert.getUserId()))
                    .filter(cert -> YearMonth.from(cert.getIssuedAt()).equals(month))
                    .count();
            counts.add(count);
        }

        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("months", months);
        trend.put("counts", counts);
        return trend;
    }

    private List<Map<String, Object>> buildCertTypeDistribution(AnalyticsScope scope) {
        Map<String, Long> grouped = certificateRepo.findAll().stream()
                .filter(cert -> scope.containsUserId(cert.getUserId()))
                .filter(cert -> scope.inRange(cert.getIssuedAt()))
                .collect(Collectors.groupingBy(cert -> certLevelLabel(cert.getCertLevel()), Collectors.counting()));

        if (grouped.isEmpty()) {
            return List.of(chartItem("暂无证书", 0, "#d1d5db"));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        int i = 0;
        for (var entry : grouped.entrySet()) {
            result.add(chartItem(entry.getKey(), entry.getValue(), CHART_COLORS.get(i++ % CHART_COLORS.size())));
        }
        return result;
    }

    // ── helpers ──

    private String classifyQuestion(String question) {
        if (question == null) return "其他";
        String q = question.toLowerCase();
        if (q.contains("消防") || q.contains("灭火") || q.contains("火灾")) return "消防安全";
        if (q.contains("隐患") || q.contains("排查") || q.contains("检查")) return "隐患排查";
        if (q.contains("应急") || q.contains("处置") || q.contains("演练")) return "应急处置";
        if (q.contains("设备") || q.contains("操作") || q.contains("bms") || q.contains("pcs")) return "设备操作";
        return "其他";
    }

    private String certLevelLabel(String level) {
        if (level == null) return "通用证书";
        return switch (level) {
            case "basic" -> "基础证书";
            case "intermediate" -> "中级证书";
            case "advanced" -> "高级证书";
            default -> level;
        };
    }

    private String deptName(User u) {
        if (u.getDepartment() == null || u.getDepartment().isBlank()) return "未分配部门";
        return u.getDepartment().trim();
    }

    private int chapterStudiedMinutes(UserProgress p) {
        if (p.getStudySeconds() != null && p.getStudySeconds() > 0) {
            return (int) Math.ceil(p.getStudySeconds() / 60.0);
        }
        int duration = 30;
        if (p.getChapter() != null && p.getChapter().getDuration() != null) {
            duration = p.getChapter().getDuration();
        }
        if (Boolean.TRUE.equals(p.getCompleted())) return duration;
        int pct = p.getProgress() != null ? p.getProgress() : 0;
        return duration * pct / 100;
    }

    private int trainingStudiedMinutes(TrainingRecord r) {
        if (r.getScenario() != null && r.getScenario().getDuration() != null) {
            return r.getScenario().getDuration();
        }
        if (r.getStartTime() != null && r.getEndTime() != null) {
            return (int) Math.max(1, ChronoUnit.MINUTES.between(r.getStartTime(), r.getEndTime()));
        }
        return 30;
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

    private Map<String, Object> chartItem(String name, Number value, String color) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("value", value);
        item.put("color", color);
        return item;
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}

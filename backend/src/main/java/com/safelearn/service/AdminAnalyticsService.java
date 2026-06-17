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
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("learningEffect", buildLearningEffect());
        result.put("simulationEffect", buildSimulationEffect());
        result.put("userActivity", buildUserActivity());
        result.put("departmentCompare", buildDepartmentCompare());
        result.put("aiUsage", buildAiUsage());
        result.put("certificates", buildCertificates());
        return result;
    }

    // ── 1. 学习效果分析 ──

    private Map<String, Object> buildLearningEffect() {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("courseCompletionRank", buildCourseCompletionRank());
        section.put("quizPassRateTrend", buildQuizPassRateTrend());
        section.put("masteryDistribution", buildMasteryDistribution());
        return section;
    }

    private List<Map<String, Object>> buildCourseCompletionRank() {
        List<Course> courses = courseRepo.findByStatusOrderByCreatedAtDesc("published");
        Map<String, Integer> chapterCounts = new HashMap<>();
        for (Course c : courses) {
            chapterCounts.put(c.getId(), chapterRepo.findByCourseIdOrderByOrderNumAsc(c.getId()).size());
        }

        Map<String, Set<String>> usersByCourse = new HashMap<>();
        Map<String, Map<String, Set<String>>> completedChaptersByUserCourse = new HashMap<>();

        for (UserProgress up : progressRepo.findAll()) {
            if (up.getCourse() == null || up.getUser() == null) continue;
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
        List<Map<String, Object>> top5 = new ArrayList<>(ranked.stream().limit(5).toList());
        for (int i = 0; i < top5.size(); i++) {
            top5.get(i).put("color", CHART_COLORS.get(i % CHART_COLORS.size()));
        }
        return top5.isEmpty() ? List.of(chartItem("暂无课程数据", 0, "#d1d5db")) : top5;
    }

    private Map<String, Object> buildQuizPassRateTrend() {
        LocalDate now = LocalDate.now();
        List<String> months = new ArrayList<>();
        List<Integer> rates = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月");

        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            months.add(month.format(fmt));
            int year = month.getYear();
            int monthValue = month.getMonthValue();

            List<QuizAttempt> attempts = quizAttemptRepo.findAll().stream()
                    .filter(a -> a.getCompletedAt() != null || a.getStartedAt() != null)
                    .filter(a -> {
                        LocalDateTime t = a.getCompletedAt() != null ? a.getCompletedAt() : a.getStartedAt();
                        return t.getYear() == year && t.getMonthValue() == monthValue;
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

    private List<Map<String, Object>> buildMasteryDistribution() {
        Map<String, Long> buckets = new LinkedHashMap<>();
        buckets.put("未掌握", 0L);
        buckets.put("初步了解", 0L);
        buckets.put("基本掌握", 0L);
        buckets.put("熟练掌握", 0L);
        buckets.put("精通", 0L);

        for (UserProgress up : progressRepo.findAll()) {
            if (!Boolean.TRUE.equals(up.getCompleted())) continue;
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

    private Map<String, Object> buildSimulationEffect() {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("scoreDistribution", buildScoreDistribution());
        section.put("scenarioSuccessRate", buildScenarioSuccessRate());
        section.put("decisionRadar", buildDecisionRadar());
        return section;
    }

    private List<Map<String, Object>> buildScoreDistribution() {
        String[] labels = {"0-59分", "60-69分", "70-79分", "80-89分", "90-100分"};
        long[] counts = new long[5];

        for (SimulationSession s : simulationSessionRepo.findAll()) {
            if (s.getTotalScore() == null) continue;
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

    private List<Map<String, Object>> buildScenarioSuccessRate() {
        Map<String, long[]> stats = new LinkedHashMap<>();
        for (SimulationSession s : simulationSessionRepo.findAll()) {
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

    private Map<String, Object> buildDecisionRadar() {
        List<SimulationScoreReport> reports = scoreReportRepo.findAll();
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

    private Map<String, Object> buildUserActivity() {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("weeklyActiveTrend", buildWeeklyActiveTrend());
        section.put("studyDurationDistribution", buildStudyDurationDistribution());
        section.put("newUserTrend", buildNewUserTrend());
        return section;
    }

    private Map<String, Object> buildWeeklyActiveTrend() {
        LocalDate now = LocalDate.now();
        WeekFields wf = WeekFields.ISO;
        List<String> weeks = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            LocalDate weekStart = now.minusWeeks(i).with(wf.dayOfWeek(), 1);
            LocalDate weekEnd = weekStart.plusDays(6);
            weeks.add(weekStart.getMonthValue() + "/" + weekStart.getDayOfMonth());

            LocalDateTime start = weekStart.atStartOfDay();
            LocalDateTime end = weekEnd.plusDays(1).atStartOfDay();

            long active = progressRepo.findAll().stream()
                    .filter(p -> p.getLastAccessAt() != null)
                    .filter(p -> !p.getLastAccessAt().isBefore(start) && p.getLastAccessAt().isBefore(end))
                    .map(p -> p.getUser().getId())
                    .distinct()
                    .count();
            counts.add(active);
        }

        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("weeks", weeks);
        trend.put("counts", counts);
        return trend;
    }

    private List<Map<String, Object>> buildStudyDurationDistribution() {
        Map<String, Integer> userMinutes = new HashMap<>();

        for (UserProgress p : progressRepo.findAll()) {
            if (p.getUser() == null) continue;
            String uid = p.getUser().getId();
            userMinutes.merge(uid, chapterStudiedMinutes(p), Integer::sum);
        }
        for (TrainingRecord r : trainingRecordRepo.findAll()) {
            if (r.getUser() == null || r.getEndTime() == null) continue;
            userMinutes.merge(r.getUser().getId(), trainingStudiedMinutes(r), Integer::sum);
        }
        for (SimulationSession s : simulationSessionRepo.findAll()) {
            if (!"completed".equals(s.getStatus()) && s.getFinishedAt() == null) continue;
            int mins = s.getElapsedMs() != null && s.getElapsedMs() > 0
                    ? (int) Math.ceil(s.getElapsedMs() / 60000.0) : 5;
            userMinutes.merge(s.getUserId(), mins, Integer::sum);
        }

        long[] buckets = new long[5];
        String[] labels = {"0-1小时", "1-3小时", "3-5小时", "5-10小时", "10小时以上"};
        for (int mins : userMinutes.values()) {
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

    private Map<String, Object> buildNewUserTrend() {
        LocalDate now = LocalDate.now();
        List<String> months = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月");

        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            months.add(month.format(fmt));
            int year = month.getYear();
            int monthValue = month.getMonthValue();
            long count = userRepo.findAll().stream()
                    .filter(u -> !"admin".equalsIgnoreCase(u.getRole()))
                    .filter(u -> u.getCreatedAt() != null)
                    .filter(u -> u.getCreatedAt().getYear() == year && u.getCreatedAt().getMonthValue() == monthValue)
                    .count();
            counts.add(count);
        }

        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("months", months);
        trend.put("counts", counts);
        return trend;
    }

    // ── 4. 部门对比 ──

    private Map<String, Object> buildDepartmentCompare() {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("deptScoreRank", buildDeptScoreRank());
        section.put("deptActivityHeatmap", buildDeptActivityHeatmap());
        return section;
    }

    private List<Map<String, Object>> buildDeptScoreRank() {
        Map<String, List<Integer>> scoresByDept = new HashMap<>();
        Map<String, User> userMap = userRepo.findAll().stream()
                .filter(u -> !"admin".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        for (TrainingRecord r : trainingRecordRepo.findAll()) {
            if (r.getUser() == null || r.getTotalScore() == null) continue;
            String dept = deptName(r.getUser());
            scoresByDept.computeIfAbsent(dept, k -> new ArrayList<>()).add(r.getTotalScore());
        }
        for (SimulationSession s : simulationSessionRepo.findAll()) {
            if (s.getTotalScore() == null) continue;
            User u = userMap.get(s.getUserId());
            if (u == null) continue;
            String dept = deptName(u);
            scoresByDept.computeIfAbsent(dept, k -> new ArrayList<>()).add(s.getTotalScore());
        }

        List<Map<String, Object>> ranked = new ArrayList<>();
        for (var e : scoresByDept.entrySet()) {
            double avg = e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", e.getKey());
            item.put("value", (int) Math.round(avg));
            item.put("samples", e.getValue().size());
            ranked.add(item);
        }
        ranked.sort((a, b) -> Integer.compare((Integer) b.get("value"), (Integer) a.get("value")));

        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).put("color", CHART_COLORS.get(i % CHART_COLORS.size()));
        }
        return ranked.isEmpty() ? List.of(chartItem("暂无部门得分", 0, "#d1d5db")) : ranked;
    }

    private Map<String, Object> buildDeptActivityHeatmap() {
        Map<String, User> userMap = userRepo.findAll().stream()
                .filter(u -> !"admin".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        Set<String> deptSet = new LinkedHashSet<>();
        userMap.values().forEach(u -> deptSet.add(deptName(u)));
        List<String> departments = new ArrayList<>(deptSet);
        if (departments.isEmpty()) departments.add("未分配部门");

        LocalDate now = LocalDate.now();
        List<String> weeks = new ArrayList<>();
        for (int i = 7; i >= 0; i--) {
            LocalDate ws = now.minusWeeks(i).with(WeekFields.ISO.dayOfWeek(), 1);
            weeks.add(ws.getMonthValue() + "/" + ws.getDayOfMonth());
        }

        List<List<Object>> data = new ArrayList<>();
        for (int w = 0; w < 8; w++) {
            LocalDate weekStart = now.minusWeeks(7 - w).with(WeekFields.ISO.dayOfWeek(), 1);
            LocalDateTime start = weekStart.atStartOfDay();
            LocalDateTime end = weekStart.plusDays(7).atStartOfDay();

            for (int d = 0; d < departments.size(); d++) {
                String dept = departments.get(d);
                long active = progressRepo.findAll().stream()
                        .filter(p -> p.getLastAccessAt() != null && p.getUser() != null)
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

    private Map<String, Object> buildAiUsage() {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("qaTrend", buildQaTrend());
        section.put("qaCategoryDistribution", buildQaCategoryDistribution());
        return section;
    }

    private Map<String, Object> buildQaTrend() {
        LocalDate now = LocalDate.now();
        List<String> months = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月");

        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            months.add(month.format(fmt));
            int year = month.getYear();
            int monthValue = month.getMonthValue();
            long count = qaRecordRepo.findAll().stream()
                    .filter(q -> q.getCreatedAt() != null)
                    .filter(q -> q.getCreatedAt().getYear() == year && q.getCreatedAt().getMonthValue() == monthValue)
                    .count();
            counts.add(count);
        }

        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("months", months);
        trend.put("counts", counts);
        return trend;
    }

    private List<Map<String, Object>> buildQaCategoryDistribution() {
        Map<String, Long> categories = new LinkedHashMap<>();
        categories.put("消防安全", 0L);
        categories.put("隐患排查", 0L);
        categories.put("应急处置", 0L);
        categories.put("设备操作", 0L);
        categories.put("其他", 0L);

        for (QaRecord q : qaRecordRepo.findAll()) {
            String cat = classifyQuestion(q.getQuestion());
            categories.merge(cat, 1L, Long::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        int i = 0;
        for (var e : categories.entrySet()) {
            if (e.getValue() > 0) {
                result.add(chartItem(e.getKey(), e.getValue(), CHART_COLORS.get(i++ % CHART_COLORS.size())));
            }
        }
        return result.isEmpty() ? List.of(chartItem("暂无问答", 0, "#d1d5db")) : result;
    }

    // ── 6. 证书成就 ──

    private Map<String, Object> buildCertificates() {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("issueTrend", buildCertIssueTrend());
        section.put("typeDistribution", buildCertTypeDistribution());
        return section;
    }

    private Map<String, Object> buildCertIssueTrend() {
        LocalDate now = LocalDate.now();
        List<String> months = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月");

        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            months.add(month.format(fmt));
            int year = month.getYear();
            int monthValue = month.getMonthValue();
            long count = certificateRepo.findAll().stream()
                    .filter(c -> c.getIssuedAt() != null)
                    .filter(c -> c.getIssuedAt().getYear() == year && c.getIssuedAt().getMonthValue() == monthValue)
                    .count();
            counts.add(count);
        }

        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("months", months);
        trend.put("counts", counts);
        return trend;
    }

    private List<Map<String, Object>> buildCertTypeDistribution() {
        Map<String, Long> grouped = certificateRepo.findAll().stream()
                .collect(Collectors.groupingBy(c -> certLevelLabel(c.getCertLevel()), Collectors.counting()));

        if (grouped.isEmpty()) {
            return List.of(chartItem("暂无证书", 0, "#d1d5db"));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        int i = 0;
        for (var e : grouped.entrySet()) {
            result.add(chartItem(e.getKey(), e.getValue(), CHART_COLORS.get(i++ % CHART_COLORS.size())));
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
}

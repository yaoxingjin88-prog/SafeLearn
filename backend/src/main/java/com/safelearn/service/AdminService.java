package com.safelearn.service;

import com.safelearn.deduction.entity.SimulationSession;
import com.safelearn.entity.AccidentCase;
import com.safelearn.entity.TrainingRecord;
import com.safelearn.entity.User;
import com.safelearn.repository.*;
import com.safelearn.deduction.repository.SimulationSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final TrainingRecordRepository recordRepo;
    private final SimulationSessionRepository simulationSessionRepo;
    private final UserProgressRepository progressRepo;
    private final ChapterRepository chapterRepo;
    private final AccidentCaseRepository caseRepo;
    private final PasswordEncoder passwordEncoder;

    public List<Map<String, Object>> getUsers() {
        return userRepo.findAll().stream().map(this::toUserInfo).toList();
    }

    public Map<String, Object> createUser(Map<String, Object> data) {
        User user = new User();
        user.setUsername((String) data.get("username"));
        user.setEmail((String) data.get("email"));
        user.setPasswordHash(passwordEncoder.encode((String) data.getOrDefault("password", "123456")));
        user.setRole((String) data.getOrDefault("role", "trainee"));
        user.setCompany((String) data.get("company"));
        user.setDepartment((String) data.get("department"));
        user = userRepo.save(user);
        return toUserInfo(user);
    }

    public Map<String, Object> updateUser(String id, Map<String, Object> data) {
        User user = userRepo.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (data.containsKey("username")) user.setUsername((String) data.get("username"));
        if (data.containsKey("email")) user.setEmail((String) data.get("email"));
        if (data.containsKey("role")) user.setRole((String) data.get("role"));
        if (data.containsKey("company")) user.setCompany((String) data.get("company"));
        if (data.containsKey("department")) user.setDepartment((String) data.get("department"));
        userRepo.save(user);
        return Map.of("success", true);
    }

    public Map<String, Object> deleteUser(String id) {
        userRepo.deleteById(id);
        return Map.of("success", true);
    }

    public Map<String, Object> getStats() {
        long totalUsers = userRepo.count();
        long totalCourses = courseRepo.findByStatusOrderByCreatedAtDesc("published").size();
        long totalSimulations = simulationSessionRepo.count();
        long totalTrainings = recordRepo.count();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalCourses", totalCourses);
        stats.put("totalSimulations", totalSimulations);
        stats.put("totalTrainings", totalTrainings);
        Double dedAvg = simulationSessionRepo.findAll().stream()
                .filter(s -> s.getTotalScore() != null)
                .mapToInt(SimulationSession::getTotalScore)
                .average().orElse(0);
        stats.put("avgScore", dedAvg > 0 ? (int) Math.round(dedAvg) : roundAvg(recordRepo.avgCompletedScore()));
        return stats;
    }

    public Map<String, Object> getCharts() {
        Map<String, Object> charts = new HashMap<>();
        charts.put("completion", buildCompletionChart());
        charts.put("accidentTypes", buildAccidentTypeChart());
        charts.put("monthlyTrend", buildMonthlyTrendChart());
        charts.put("riskLevels", buildRiskLevelChart());
        return charts;
    }

    private List<Map<String, Object>> buildCompletionChart() {
        long completed = progressRepo.countByCompletedTrue();
        long inProgress = progressRepo.countInProgress();
        long totalChapters = chapterRepo.count();
        long totalUsers = Math.max(userRepo.count(), 1);
        long notStarted = Math.max(0, totalChapters * totalUsers - completed - inProgress);
        return List.of(
                chartItem("已完成", completed, "#10b981"),
                chartItem("进行中", inProgress, "#3b82f6"),
                chartItem("未开始", notStarted, "#d1d5db")
        );
    }

    private List<Map<String, Object>> buildAccidentTypeChart() {
        Map<String, Long> grouped = caseRepo.findAll().stream()
                .collect(Collectors.groupingBy(AccidentCase::getType, Collectors.counting()));
        Map<String, String> labels = Map.of(
                "fire", "火灾",
                "thermal_runaway", "热失控",
                "explosion", "爆炸",
                "gas_leak", "气体泄漏"
        );
        Map<String, String> colors = Map.of(
                "fire", "#ef4444",
                "thermal_runaway", "#f59e0b",
                "explosion", "#8b5cf6",
                "gas_leak", "#06b6d4"
        );
        if (grouped.isEmpty()) {
            return List.of(chartItem("暂无数据", 0, "#d1d5db"));
        }
        return grouped.entrySet().stream()
                .map(e -> chartItem(labels.getOrDefault(e.getKey(), e.getKey()), e.getValue(),
                        colors.getOrDefault(e.getKey(), "#6b7280")))
                .toList();
    }

    private Map<String, Object> buildMonthlyTrendChart() {
        LocalDate now = LocalDate.now();
        List<String> months = new ArrayList<>();
        List<Long> trainingUsers = new ArrayList<>();
        List<Long> simulationCounts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M月");

        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            months.add(month.format(formatter));
            int year = month.getYear();
            int monthValue = month.getMonthValue();

            long monthSims = simulationSessionRepo.findAll().stream()
                    .filter(s -> isInMonth(s.getStartedAt(), year, monthValue))
                    .count();
            simulationCounts.add(monthSims);
            trainingUsers.add(recordRepo.findAll().stream()
                    .filter(r -> isInMonth(r.getStartTime(), year, monthValue))
                    .map(r -> r.getUser().getId())
                    .distinct()
                    .count());
        }

        Map<String, Object> trend = new HashMap<>();
        trend.put("months", months);
        trend.put("trainingUsers", trainingUsers);
        trend.put("simulationCounts", simulationCounts);
        return trend;
    }

    private List<Map<String, Object>> buildRiskLevelChart() {
        Map<String, Long> grouped = caseRepo.findAll().stream()
                .collect(Collectors.groupingBy(c -> normalizeSeverity(c.getSeverity()), Collectors.counting()));
        List<String> order = List.of("低风险", "中风险", "高风险", "极高风险");
        List<String> colors = List.of("#10b981", "#f59e0b", "#f97316", "#ef4444");
        if (grouped.isEmpty()) {
            return order.stream()
                    .map(label -> chartItem(label, 0L, "#d1d5db"))
                    .toList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            String label = order.get(i);
            result.add(chartItem(label, grouped.getOrDefault(label, 0L), colors.get(i)));
        }
        return result;
    }

    private boolean isInMonth(LocalDateTime time, int year, int month) {
        return time != null && time.getYear() == year && time.getMonthValue() == month;
    }

    private String normalizeSeverity(String severity) {
        if (severity == null) return "中风险";
        return switch (severity) {
            case "minor" -> "低风险";
            case "moderate" -> "中风险";
            case "major" -> "高风险";
            case "critical", "severe" -> "极高风险";
            default -> "中风险";
        };
    }

    private Map<String, Object> chartItem(String name, Number value, String color) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("value", value);
        item.put("color", color);
        return item;
    }

    private int roundAvg(Double avg) {
        if (avg == null) return 0;
        return (int) Math.round(avg);
    }

    private Map<String, Object> toUserInfo(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("email", u.getEmail());
        m.put("role", u.getRole());
        m.put("company", u.getCompany());
        m.put("department", u.getDepartment());
        m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        return m;
    }
}

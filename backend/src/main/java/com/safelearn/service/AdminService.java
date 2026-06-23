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
    private final UserCertificateRepository certificateRepo;
    private final ChapterRepository chapterRepo;
    private final AccidentCaseRepository caseRepo;
    private final PasswordEncoder passwordEncoder;

    public List<Map<String, Object>> getUsers() {
        return userRepo.findAll().stream().map(u -> toUserListItem(u, buildProgressMap(), buildCertStatusMap())).toList();
    }

    public Map<String, Object> searchUsers(String keyword, String department, String role,
            String status, String certStatus, Integer progressMin, Integer progressMax,
            int page, int pageSize) {
        Map<String, Integer> progressByUser = buildProgressMap();
        Map<String, String> certStatusByUser = buildCertStatusMap();

        List<Map<String, Object>> filtered = userRepo.findAll().stream()
                .map(u -> toUserListItem(u, progressByUser, certStatusByUser))
                .filter(row -> matchUserKeyword(row, keyword))
                .filter(row -> matchUserDepartment(row, department))
                .filter(row -> matchUserRole(row, role))
                .filter(row -> matchUserStatus(row, status))
                .filter(row -> matchUserCertStatus(row, certStatus))
                .filter(row -> matchUserProgress(row, progressMin, progressMax))
                .sorted(Comparator.comparing(r -> String.valueOf(r.get("username"))))
                .toList();

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(Math.min(pageSize, 100), 1);
        int total = filtered.size();
        int from = Math.min((safePage - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<Map<String, Object>> items = filtered.subList(from, to);

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", safePage);
        result.put("pageSize", safeSize);
        result.put("totalPages", total == 0 ? 0 : (int) Math.ceil((double) total / safeSize));
        return result;
    }

    public Map<String, Object> getUserFilterOptions() {
        List<String> departments = userRepo.findAll().stream()
                .map(User::getDepartment)
                .filter(d -> d != null && !d.isBlank())
                .distinct()
                .sorted()
                .toList();
        Map<String, Object> options = new HashMap<>();
        options.put("departments", departments);
        return options;
    }

    public Map<String, Object> createUser(Map<String, Object> data) {
        User user = new User();
        user.setUsername((String) data.get("username"));
        user.setEmail((String) data.get("email"));
        user.setPasswordHash(passwordEncoder.encode((String) data.getOrDefault("password", "123456")));
        user.setRole((String) data.getOrDefault("role", "trainee"));
        user.setCompany((String) data.get("company"));
        user.setDepartment((String) data.get("department"));
        if (data.containsKey("phone")) user.setPhone((String) data.get("phone"));
        if (data.containsKey("position")) user.setPosition((String) data.get("position"));
        if (data.containsKey("employeeNo")) user.setEmployeeNo((String) data.get("employeeNo"));
        if (data.containsKey("avatarUrl")) user.setAvatarUrl((String) data.get("avatarUrl"));
        if (data.containsKey("enabled")) user.setEnabled(parseBoolean(data.get("enabled"), true));
        user = userRepo.save(user);
        return toUserListItem(user, buildProgressMap(), buildCertStatusMap());
    }

    public Map<String, Object> updateUser(String id, Map<String, Object> data) {
        User user = userRepo.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (data.containsKey("username")) user.setUsername((String) data.get("username"));
        if (data.containsKey("email")) user.setEmail((String) data.get("email"));
        if (data.containsKey("role")) user.setRole((String) data.get("role"));
        if (data.containsKey("company")) user.setCompany((String) data.get("company"));
        if (data.containsKey("department")) user.setDepartment((String) data.get("department"));
        if (data.containsKey("phone")) user.setPhone((String) data.get("phone"));
        if (data.containsKey("avatarUrl")) user.setAvatarUrl((String) data.get("avatarUrl"));
        if (data.containsKey("position")) user.setPosition((String) data.get("position"));
        if (data.containsKey("employeeNo")) user.setEmployeeNo((String) data.get("employeeNo"));
        if (data.containsKey("enabled")) user.setEnabled(parseBoolean(data.get("enabled"), user.getEnabled()));
        if (data.containsKey("password") && data.get("password") != null && !((String) data.get("password")).isBlank()) {
            user.setPasswordHash(passwordEncoder.encode((String) data.get("password")));
        }
        userRepo.save(user);
        return Map.of("success", true);
    }

    public Map<String, Object> updateUserStatus(String id, boolean enabled) {
        User user = userRepo.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setEnabled(enabled);
        userRepo.save(user);
        return Map.of("success", true, "enabled", enabled);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> batchOperateUsers(Map<String, Object> body) {
        List<String> ids = body.get("ids") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        String action = body.get("action") != null ? String.valueOf(body.get("action")) : "";
        int affected = 0;
        for (String id : ids) {
            Optional<User> opt = userRepo.findById(id);
            if (opt.isEmpty()) continue;
            User user = opt.get();
            switch (action) {
                case "enable" -> { user.setEnabled(true); userRepo.save(user); affected++; }
                case "disable" -> { user.setEnabled(false); userRepo.save(user); affected++; }
                case "delete" -> { userRepo.delete(user); affected++; }
                default -> { }
            }
        }
        return Map.of("success", true, "affected", affected);
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
        return toUserListItem(u, buildProgressMap(), buildCertStatusMap());
    }

    private Map<String, Object> toUserListItem(User u, Map<String, Integer> progressByUser, Map<String, String> certStatusByUser) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("email", u.getEmail());
        m.put("role", u.getRole());
        m.put("company", u.getCompany());
        m.put("department", u.getDepartment());
        m.put("position", u.getPosition());
        m.put("phone", u.getPhone());
        m.put("phoneMasked", maskPhone(u.getPhone()));
        m.put("avatarUrl", u.getAvatarUrl());
        m.put("employeeNo", resolveEmployeeNo(u));
        m.put("enabled", u.getEnabled() == null || u.getEnabled());
        m.put("accountStatus", (u.getEnabled() == null || u.getEnabled()) ? "active" : "disabled");
        m.put("trainingCompletionRate", progressByUser.getOrDefault(u.getId(), 0));
        m.put("certStatus", certStatusByUser.getOrDefault(u.getId(), "none"));
        m.put("lastLoginAt", u.getLastLoginAt() != null ? u.getLastLoginAt().toString() : null);
        m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Integer> buildProgressMap() {
        Map<String, Integer> map = new HashMap<>();
        for (Object[] row : progressRepo.aggregateProgressByUser()) {
            if (row.length < 3 || row[0] == null) continue;
            String userId = String.valueOf(row[0]);
            double avg = row[2] instanceof Number n ? n.doubleValue() : 0;
            map.put(userId, (int) Math.round(avg));
        }
        return map;
    }

    private Map<String, String> buildCertStatusMap() {
        Map<String, String> map = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (var cert : certificateRepo.findAll()) {
            String userId = cert.getUserId();
            String current = map.get(userId);
            boolean active = "active".equals(cert.getStatus())
                    && (cert.getExpiresAt() == null || cert.getExpiresAt().isAfter(now));
            if (active) {
                map.put(userId, "certified");
            } else if (!"certified".equals(current)) {
                map.put(userId, "expired");
            }
        }
        return map;
    }

    private String resolveEmployeeNo(User user) {
        if (user.getEmployeeNo() != null && !user.getEmployeeNo().isBlank()) {
            return user.getEmployeeNo();
        }
        return formatEmployeeNo(user);
    }

    private String formatEmployeeNo(User user) {
        if (user.getUsername() != null && user.getUsername().matches("(?i)[A-Z]{0,2}\\d+")) {
            return user.getUsername().toUpperCase();
        }
        String id = user.getId() != null ? user.getId().replace("-", "") : "000000";
        return "GZ" + id.substring(0, Math.min(8, id.length())).toUpperCase();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return "—";
        String p = phone.trim();
        if (p.length() >= 11) {
            return p.substring(0, 3) + "****" + p.substring(p.length() - 4);
        }
        if (p.length() >= 7) {
            return p.substring(0, 2) + "****" + p.substring(p.length() - 2);
        }
        return p;
    }

    private boolean matchUserKeyword(Map<String, Object> row, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String q = keyword.trim().toLowerCase();
        return containsIgnoreCase(String.valueOf(row.get("username")), q)
                || containsIgnoreCase(String.valueOf(row.get("employeeNo")), q)
                || containsIgnoreCase(String.valueOf(row.get("email")), q)
                || containsIgnoreCase(String.valueOf(row.get("phone")), q);
    }

    private boolean matchUserDepartment(Map<String, Object> row, String department) {
        if (department == null || department.isBlank() || "all".equals(department)) return true;
        return department.equals(String.valueOf(row.get("department")));
    }

    private boolean matchUserRole(Map<String, Object> row, String role) {
        if (role == null || role.isBlank() || "all".equals(role)) return true;
        return role.equals(String.valueOf(row.get("role")));
    }

    private boolean matchUserStatus(Map<String, Object> row, String status) {
        if (status == null || status.isBlank() || "all".equals(status)) return true;
        boolean enabled = Boolean.TRUE.equals(row.get("enabled"));
        return ("active".equals(status) && enabled) || ("disabled".equals(status) && !enabled);
    }

    private boolean matchUserCertStatus(Map<String, Object> row, String certStatus) {
        if (certStatus == null || certStatus.isBlank() || "all".equals(certStatus)) return true;
        return certStatus.equals(String.valueOf(row.get("certStatus")));
    }

    private boolean matchUserProgress(Map<String, Object> row, Integer progressMin, Integer progressMax) {
        int rate = row.get("trainingCompletionRate") instanceof Number n ? n.intValue() : 0;
        if (progressMin != null && rate < progressMin) return false;
        if (progressMax != null && rate > progressMax) return false;
        return true;
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private boolean parseBoolean(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value));
    }
}

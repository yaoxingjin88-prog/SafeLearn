package com.safelearn.service;

import com.safelearn.entity.*;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrganizationService {

    private final DepartmentRepository departmentRepo;
    private final DepartmentPositionRepository positionRepo;
    private final UserRepository userRepo;
    private final UserProgressRepository progressRepo;
    private final UserCertificateRepository certificateRepo;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOrgTree(String keyword) {
        List<Department> all = departmentRepo.findAll();
        Map<String, List<Department>> childrenMap = all.stream()
                .filter(d -> d.getParentId() != null)
                .collect(Collectors.groupingBy(Department::getParentId));
        List<Department> roots = all.stream()
                .filter(d -> d.getParentId() == null)
                .sorted(Comparator.comparing(Department::getSortOrder).thenComparing(Department::getName))
                .toList();
        return roots.stream()
                .map(root -> toTreeNode(root, childrenMap, keyword))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDepartmentDetail(String id) {
        Department dept = departmentRepo.findById(id).orElseThrow(() -> new RuntimeException("部门不存在"));
        Map<String, Object> stats = computeDepartmentStats(dept);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", dept.getId());
        detail.put("name", dept.getName());
        detail.put("parentId", dept.getParentId());
        detail.put("leaderName", dept.getLeaderName());
        detail.put("leaderTitle", dept.getLeaderTitle());
        detail.put("stats", stats);
        detail.put("memberCount", stats.get("totalMembers"));
        detail.put("trainingCompletionRate", stats.get("trainingCompletionRate"));
        detail.put("highRiskPositionCount", stats.get("highRiskPositionCount"));
        detail.put("certExpiringCount", stats.get("certExpiringCount"));
        detail.put("newMembersThisMonth", stats.get("newMembersThisMonth"));
        return detail;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDepartmentMembers(String id, int page, int pageSize, String keyword) {
        Department dept = departmentRepo.findById(id).orElseThrow(() -> new RuntimeException("部门不存在"));
        Set<String> deptNames = collectDepartmentNames(dept);
        Map<String, Integer> progressByUser = buildProgressMap();
        Map<String, String> certStatusByUser = buildCertStatusMap();

        List<Map<String, Object>> all = userRepo.findAll().stream()
                .filter(u -> u.getDepartment() != null && deptNames.contains(u.getDepartment()))
                .map(u -> toMemberRow(u, progressByUser, certStatusByUser))
                .filter(row -> matchMemberKeyword(row, keyword))
                .sorted(Comparator.comparing(r -> String.valueOf(r.get("username"))))
                .toList();

        return paginate(all, page, pageSize);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDepartmentPositions(String id) {
        departmentRepo.findById(id).orElseThrow(() -> new RuntimeException("部门不存在"));
        return positionRepo.findByDepartmentIdOrderBySortOrderAscNameAsc(id).stream()
                .map(this::toPositionRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDepartmentStats(String id) {
        Department dept = departmentRepo.findById(id).orElseThrow(() -> new RuntimeException("部门不存在"));
        Map<String, Object> stats = computeDepartmentStats(dept);
        Set<String> deptNames = collectDepartmentNames(dept);
        List<User> members = userRepo.findAll().stream()
                .filter(u -> u.getDepartment() != null && deptNames.contains(u.getDepartment()))
                .toList();

        Map<String, Integer> progressByUser = buildProgressMap();
        long completed = members.stream().filter(u -> progressByUser.getOrDefault(u.getId(), 0) >= 100).count();
        long inProgress = members.stream().filter(u -> {
            int p = progressByUser.getOrDefault(u.getId(), 0);
            return p > 0 && p < 100;
        }).count();
        long notStarted = members.size() - completed - inProgress;

        Map<String, Object> result = new LinkedHashMap<>(stats);
        result.put("trainingBreakdown", Map.of(
                "completed", completed,
                "inProgress", inProgress,
                "notStarted", Math.max(0, notStarted)
        ));
        result.put("roleDistribution", members.stream()
                .collect(Collectors.groupingBy(u -> u.getRole() != null ? u.getRole() : "trainee", Collectors.counting())));
        result.put("certStatusDistribution", members.stream()
                .collect(Collectors.groupingBy(
                        u -> certStatusLabel(buildCertStatusMap().getOrDefault(u.getId(), "none")),
                        Collectors.counting())));
        return result;
    }

    @Transactional
    public Map<String, Object> createDepartment(Map<String, Object> body) {
        Department dept = new Department();
        dept.setName(requiredString(body, "name"));
        dept.setParentId((String) body.get("parentId"));
        dept.setLeaderName((String) body.get("leaderName"));
        dept.setLeaderTitle((String) body.get("leaderTitle"));
        if (body.get("sortOrder") instanceof Number n) dept.setSortOrder(n.intValue());
        dept = departmentRepo.save(dept);
        return Map.of("id", dept.getId(), "name", dept.getName());
    }

    @Transactional
    public Map<String, Object> updateDepartment(String id, Map<String, Object> body) {
        Department dept = departmentRepo.findById(id).orElseThrow(() -> new RuntimeException("部门不存在"));
        if (body.containsKey("name")) dept.setName((String) body.get("name"));
        if (body.containsKey("parentId")) dept.setParentId((String) body.get("parentId"));
        if (body.containsKey("leaderName")) dept.setLeaderName((String) body.get("leaderName"));
        if (body.containsKey("leaderTitle")) dept.setLeaderTitle((String) body.get("leaderTitle"));
        if (body.get("sortOrder") instanceof Number n) dept.setSortOrder(n.intValue());
        departmentRepo.save(dept);
        return Map.of("success", true);
    }

    @Transactional
    public Map<String, Object> deleteDepartment(String id) {
        Department dept = departmentRepo.findById(id).orElseThrow(() -> new RuntimeException("部门不存在"));
        if (departmentRepo.countByParentId(id) > 0) {
            throw new RuntimeException("请先删除下级部门");
        }
        departmentRepo.delete(dept);
        positionRepo.findByDepartmentIdOrderBySortOrderAscNameAsc(id)
                .forEach(positionRepo::delete);
        return Map.of("success", true);
    }

    @Transactional
    public Map<String, Object> createPosition(String departmentId, Map<String, Object> body) {
        departmentRepo.findById(departmentId).orElseThrow(() -> new RuntimeException("部门不存在"));
        DepartmentPosition pos = new DepartmentPosition();
        pos.setDepartmentId(departmentId);
        pos.setName(requiredString(body, "name"));
        pos.setHighRisk(parseBoolean(body.get("highRisk"), false));
        if (body.get("sortOrder") instanceof Number n) pos.setSortOrder(n.intValue());
        pos = positionRepo.save(pos);
        return toPositionRow(pos);
    }

    @Transactional
    public Map<String, Object> updatePosition(String id, Map<String, Object> body) {
        DepartmentPosition pos = positionRepo.findById(id).orElseThrow(() -> new RuntimeException("岗位不存在"));
        if (body.containsKey("name")) pos.setName((String) body.get("name"));
        if (body.containsKey("highRisk")) pos.setHighRisk(parseBoolean(body.get("highRisk"), pos.getHighRisk()));
        if (body.get("sortOrder") instanceof Number n) pos.setSortOrder(n.intValue());
        positionRepo.save(pos);
        return toPositionRow(pos);
    }

    @Transactional
    public Map<String, Object> deletePosition(String id) {
        positionRepo.deleteById(id);
        return Map.of("success", true);
    }

    @Transactional
    public Map<String, Object> removeMemberFromDepartment(String departmentId, String userId) {
        Department dept = departmentRepo.findById(departmentId).orElseThrow(() -> new RuntimeException("部门不存在"));
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        Set<String> deptNames = collectDepartmentNames(dept);
        if (user.getDepartment() == null || !deptNames.contains(user.getDepartment())) {
            throw new RuntimeException("该用户不属于此部门");
        }
        user.setDepartment(null);
        userRepo.save(user);
        return Map.of("success", true);
    }

    private Map<String, Object> toTreeNode(Department dept, Map<String, List<Department>> childrenMap, String keyword) {
        List<Map<String, Object>> children = childrenMap.getOrDefault(dept.getId(), List.of()).stream()
                .sorted(Comparator.comparing(Department::getSortOrder).thenComparing(Department::getName))
                .map(child -> toTreeNode(child, childrenMap, keyword))
                .filter(Objects::nonNull)
                .toList();

        Set<String> names = collectDepartmentNames(dept);
        long memberCount = userRepo.findAll().stream()
                .filter(u -> u.getDepartment() != null && names.contains(u.getDepartment()))
                .count();
        for (Map<String, Object> child : children) {
            memberCount += ((Number) child.get("memberCount")).longValue();
        }

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", dept.getId());
        node.put("name", dept.getName());
        node.put("parentId", dept.getParentId());
        node.put("memberCount", memberCount);
        node.put("children", children);

        if (keyword != null && !keyword.isBlank()) {
            String q = keyword.trim().toLowerCase();
            boolean selfMatch = dept.getName().toLowerCase().contains(q);
            if (!selfMatch && children.isEmpty()) return null;
            if (!selfMatch && !children.isEmpty()) {
                node.put("children", children);
                if (children.isEmpty()) return null;
            }
        }
        return node;
    }

    private Map<String, Object> computeDepartmentStats(Department dept) {
        Set<String> deptNames = collectDepartmentNames(dept);
        List<User> members = userRepo.findAll().stream()
                .filter(u -> u.getDepartment() != null && deptNames.contains(u.getDepartment()))
                .toList();
        Map<String, Integer> progressByUser = buildProgressMap();
        LocalDateTime now = LocalDateTime.now();
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);

        int avgProgress = members.isEmpty() ? 0 : (int) Math.round(members.stream()
                .mapToInt(u -> progressByUser.getOrDefault(u.getId(), 0))
                .average().orElse(0));

        List<DepartmentPosition> positions = positionRepo.findByDepartmentIdOrderBySortOrderAscNameAsc(dept.getId());
        long highRiskCount = positions.stream().filter(p -> Boolean.TRUE.equals(p.getHighRisk())).count();
        if (highRiskCount == 0) {
            highRiskCount = members.stream()
                    .map(User::getPosition)
                    .filter(Objects::nonNull)
                    .filter(this::isHighRiskPositionName)
                    .distinct()
                    .count();
        }

        long certExpiring = members.stream()
                .filter(u -> hasCertExpiringSoon(u.getId(), now))
                .count();

        long newThisMonth = members.stream()
                .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().toLocalDate().isBefore(monthStart))
                .count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalMembers", members.size());
        stats.put("trainingCompletionRate", avgProgress);
        stats.put("highRiskPositionCount", highRiskCount);
        stats.put("certExpiringCount", certExpiring);
        stats.put("newMembersThisMonth", newThisMonth);
        return stats;
    }

    private Map<String, Object> toMemberRow(User u, Map<String, Integer> progressByUser, Map<String, String> certStatusByUser) {
        String certStatus = certStatusByUser.getOrDefault(u.getId(), "none");
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", u.getId());
        row.put("username", u.getUsername());
        row.put("employeeNo", resolveEmployeeNo(u));
        row.put("position", u.getPosition());
        row.put("role", u.getRole());
        row.put("avatarUrl", u.getAvatarUrl());
        row.put("trainingCompletionRate", progressByUser.getOrDefault(u.getId(), 0));
        row.put("certStatus", certStatus);
        row.put("certStatusLabel", certStatusLabel(certStatus));
        return row;
    }

    private Map<String, Object> toPositionRow(DepartmentPosition pos) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", pos.getId());
        row.put("departmentId", pos.getDepartmentId());
        row.put("name", pos.getName());
        row.put("highRisk", Boolean.TRUE.equals(pos.getHighRisk()));
        row.put("sortOrder", pos.getSortOrder());
        return row;
    }

    private Set<String> collectDepartmentNames(Department dept) {
        Set<String> names = new LinkedHashSet<>();
        names.add(dept.getName());
        departmentRepo.findByParentIdOrderBySortOrderAscNameAsc(dept.getId())
                .forEach(child -> names.addAll(collectDepartmentNames(child)));
        return names;
    }

    private Map<String, Integer> buildProgressMap() {
        Map<String, Integer> map = new HashMap<>();
        for (Object[] row : progressRepo.aggregateProgressByUser()) {
            if (row.length < 3 || row[0] == null) continue;
            double avg = row[2] instanceof Number n ? n.doubleValue() : 0;
            map.put(String.valueOf(row[0]), (int) Math.round(avg));
        }
        return map;
    }

    private Map<String, String> buildCertStatusMap() {
        Map<String, String> map = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (UserCertificate cert : certificateRepo.findAll()) {
            String userId = cert.getUserId();
            boolean active = "active".equals(cert.getStatus())
                    && (cert.getExpiresAt() == null || cert.getExpiresAt().isAfter(now));
            if (active) {
                if (cert.getExpiresAt() != null && cert.getExpiresAt().isBefore(now.plusDays(30))) {
                    map.put(userId, "expiring");
                } else if (!"expiring".equals(map.get(userId))) {
                    map.put(userId, "valid");
                }
            } else if (!"valid".equals(map.get(userId)) && !"expiring".equals(map.get(userId))) {
                map.put(userId, "expired");
            }
        }
        return map;
    }

    private String certStatusLabel(String status) {
        return switch (status) {
            case "valid" -> "全部有效";
            case "expiring" -> "部分临期";
            case "expired" -> "已过期";
            default -> "未持证";
        };
    }

    private boolean hasCertExpiringSoon(String userId, LocalDateTime now) {
        return certificateRepo.findByUserIdOrderByIssuedAtDesc(userId).stream()
                .anyMatch(c -> c.getExpiresAt() != null
                        && !c.getExpiresAt().isBefore(now)
                        && c.getExpiresAt().isBefore(now.plusDays(30)));
    }

    private boolean isHighRiskPositionName(String position) {
        if (position == null) return false;
        return position.contains("高压") || position.contains("动火") || position.contains("特种")
                || position.contains("值班") || position.contains("安全工程");
    }

    private String resolveEmployeeNo(User user) {
        if (user.getEmployeeNo() != null && !user.getEmployeeNo().isBlank()) return user.getEmployeeNo();
        if (user.getUsername() != null && user.getUsername().matches("(?i)[A-Z]{0,2}\\d+")) {
            return user.getUsername().toUpperCase();
        }
        String id = user.getId() != null ? user.getId().replace("-", "") : "000000";
        return "GZ" + id.substring(0, Math.min(8, id.length())).toUpperCase();
    }

    private boolean matchMemberKeyword(Map<String, Object> row, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String q = keyword.trim().toLowerCase();
        return String.valueOf(row.get("username")).toLowerCase().contains(q)
                || String.valueOf(row.get("employeeNo")).toLowerCase().contains(q);
    }

    private Map<String, Object> paginate(List<Map<String, Object>> all, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(Math.min(pageSize, 100), 1);
        int total = all.size();
        int from = Math.min((safePage - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", all.subList(from, to));
        result.put("total", total);
        result.put("page", safePage);
        result.put("pageSize", safeSize);
        result.put("totalPages", total == 0 ? 0 : (int) Math.ceil((double) total / safeSize));
        return result;
    }

    private String requiredString(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null || String.valueOf(val).isBlank()) {
            throw new RuntimeException(key + " 不能为空");
        }
        return String.valueOf(val).trim();
    }

    private boolean parseBoolean(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value));
    }
}

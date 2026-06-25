package com.safelearn.service;

import com.safelearn.entity.User;
import com.safelearn.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 学习报表筛选上下文：部门 + 时间范围 + 学员集合。 */
final class AnalyticsScope {

    final Set<String> userIds;
    final Map<String, User> usersById;
    final LocalDate from;
    final LocalDate to;
    final String department;
    final List<String> departments;

    private AnalyticsScope(Set<String> userIds, Map<String, User> usersById,
                           LocalDate from, LocalDate to, String department, List<String> departments) {
        this.userIds = userIds;
        this.usersById = usersById;
        this.from = from;
        this.to = to;
        this.department = department;
        this.departments = departments;
    }

    static AnalyticsScope create(UserRepository userRepo, String department, LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusMonths(6);
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        List<User> learners = userRepo.findAll().stream()
                .filter(user -> !"admin".equalsIgnoreCase(user.getRole()))
                .toList();

        Set<String> deptSet = new LinkedHashSet<>();
        learners.forEach(user -> deptSet.add(deptName(user)));
        List<String> allDepartments = new ArrayList<>(deptSet);

        List<User> scopedLearners = learners;
        String normalizedDept = normalizeDepartment(department);
        if (normalizedDept != null) {
            scopedLearners = learners.stream()
                    .filter(user -> deptName(user).equals(normalizedDept))
                    .toList();
        }

        Set<String> ids = scopedLearners.stream().map(User::getId).collect(Collectors.toSet());
        Map<String, User> userMap = scopedLearners.stream()
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left));

        return new AnalyticsScope(ids, userMap, start, end, normalizedDept, allDepartments);
    }

    static String normalizeDepartment(String department) {
        if (department == null || department.isBlank() || "all".equalsIgnoreCase(department)) {
            return null;
        }
        return department.trim();
    }

    static String deptName(User user) {
        if (user.getDepartment() == null || user.getDepartment().isBlank()) {
            return "未分配部门";
        }
        return user.getDepartment().trim();
    }

    boolean containsUser(User user) {
        return user != null && userIds.contains(user.getId());
    }

    boolean containsUserId(String userId) {
        return userId != null && userIds.contains(userId);
    }

    boolean inRange(LocalDateTime time) {
        if (time == null) {
            return true;
        }
        LocalDate date = time.toLocalDate();
        return !date.isBefore(from) && !date.isAfter(to);
    }

    List<YearMonth> monthsInRange(int maxMonths) {
        YearMonth start = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        List<YearMonth> months = new ArrayList<>();
        YearMonth cursor = start;
        while (!cursor.isAfter(end)) {
            months.add(cursor);
            cursor = cursor.plusMonths(1);
        }
        if (months.size() > maxMonths) {
            return months.subList(months.size() - maxMonths, months.size());
        }
        return months;
    }
}

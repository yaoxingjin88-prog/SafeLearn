package com.safelearn.security;

import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AdminApiPermissionRegistry {

    private record Rule(String prefix, String module, String viewAction, String writeAction) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("/api/admin/roles", "permission", "view", "edit"),
            new Rule("/api/admin/system-configs", "permission", "view", "edit"),
            new Rule("/api/admin/users", "organization", "view", "edit"),
            new Rule("/api/admin/org", "organization", "view", "edit"),
            new Rule("/api/admin/departments", "organization", "view", "edit"),
            new Rule("/api/admin/courses", "training", "view", "edit"),
            new Rule("/api/admin/course-categories", "training", "view", "edit"),
            new Rule("/api/admin/exams", "exam", "view", "edit"),
            new Rule("/api/admin/questions", "exam", "view", "edit"),
            new Rule("/api/admin/question-categories", "exam", "view", "edit"),
            new Rule("/api/admin/papers", "exam", "view", "edit"),
            new Rule("/api/admin/alerts", "hazard", "view", "edit"),
            new Rule("/api/admin/learning-report", "report", "view", "edit"),
            new Rule("/api/admin/analytics", "report", "view", "view"),
            new Rule("/api/admin/dashboard", "dashboard", "view", "edit"),
            new Rule("/api/admin/charts", "dashboard", "view", "view"),
            new Rule("/api/admin/learning-overview", "dashboard", "view", "view"),
            new Rule("/api/admin/notifications", "dashboard", "view", "edit"),
            new Rule("/api/admin/messages", "dashboard", "view", "edit"),
            new Rule("/api/admin/inbox", "dashboard", "view", "view")
    );

    private AdminApiPermissionRegistry() {
    }

    public static Optional<String> resolveRequiredPermission(String path, String httpMethod) {
        if (path == null || !path.startsWith("/api/admin/")) {
            return Optional.empty();
        }
        for (Rule rule : RULES) {
            if (!path.startsWith(rule.prefix())) {
                continue;
            }
            if (isExport(path) && HttpMethod.GET.matches(httpMethod)) {
                return Optional.of(PermissionCodes.build(resolveModule(path, rule.module()), "export"));
            }
            if (isDelete(httpMethod)) {
                return Optional.of(PermissionCodes.build(resolveModule(path, rule.module()), "delete"));
            }
            if (isCreate(httpMethod)) {
                return Optional.of(PermissionCodes.build(resolveModule(path, rule.module()), "create"));
            }
            if (isApprove(path, httpMethod)) {
                return Optional.of(PermissionCodes.build(resolveModule(path, rule.module()), "approve"));
            }
            if (isWrite(httpMethod)) {
                return Optional.of(PermissionCodes.build(resolveModule(path, rule.module()), rule.writeAction()));
            }
            return Optional.of(PermissionCodes.build(resolveModule(path, rule.module()), rule.viewAction()));
        }
        return Optional.of(PermissionCodes.build("dashboard", "view"));
    }

    private static String resolveModule(String path, String defaultModule) {
        if (path.contains("/learning-report")) {
            return "report";
        }
        if (path.contains("/alerts")) {
            return "hazard";
        }
        return defaultModule;
    }

    private static boolean isExport(String path) {
        return path.contains("/export");
    }

    private static boolean isApprove(String path, String method) {
        return path.contains("/publish")
                || path.contains("/approve")
                || path.contains("/status")
                || (path.contains("/remind") && HttpMethod.POST.matches(method));
    }

    private static boolean isDelete(String method) {
        return HttpMethod.DELETE.matches(method);
    }

    private static boolean isCreate(String method) {
        return HttpMethod.POST.matches(method);
    }

    private static boolean isWrite(String method) {
        return HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method);
    }

    public static List<String> allPermissionCodes() {
        List<String> modules = List.of(
                "dashboard", "training", "exam", "drill", "organization", "hazard", "report", "permission"
        );
        List<String> actions = List.of("view", "create", "edit", "delete", "export", "approve");
        List<String> codes = new ArrayList<>();
        for (String module : modules) {
            for (String action : actions) {
                codes.add(PermissionCodes.build(module, action));
            }
        }
        return codes;
    }
}

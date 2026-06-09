package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(dashboardService.getStats(userId));
    }

    @GetMapping("/recent-courses")
    public ApiResponse<List<Map<String, Object>>> getRecentCourses(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(dashboardService.getRecentCourses(userId));
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getOverview(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(dashboardService.getOverview(userId));
    }

    @GetMapping("/study-trend")
    public ApiResponse<Map<String, Object>> getStudyTrend(
            Authentication auth,
            @RequestParam(defaultValue = "7") int days) {
        String userId = auth.getPrincipal().toString();
        int range = days == 30 ? 30 : 7;
        return ApiResponse.success(dashboardService.getStudyTrend(userId, range));
    }

    @GetMapping("/ability-radar")
    public ApiResponse<Map<String, Object>> getAbilityRadar(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(dashboardService.getAbilityRadar(userId));
    }

    @GetMapping("/learning-calendar")
    public ApiResponse<Map<String, Object>> getLearningCalendar(
            Authentication auth,
            @RequestParam int year,
            @RequestParam int month) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(dashboardService.getLearningCalendar(userId, year, month));
    }

    @GetMapping("/learning-history")
    public ApiResponse<List<Map<String, Object>>> getLearningHistory(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(dashboardService.getLearningHistory(userId));
    }
}

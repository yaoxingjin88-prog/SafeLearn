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
}

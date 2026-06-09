package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.AdminService;
import com.safelearn.service.AdminCourseService;
import com.safelearn.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminCourseService adminCourseService;
    private final DashboardService dashboardService;

    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> getUsers() {
        List<Map<String, Object>> users = adminService.getUsers();
        return ApiResponse.success(Map.of("items", users, "total", users.size()));
    }

    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminService.createUser(body));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<Map<String, Object>> updateUser(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminService.updateUser(id, body));
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Map<String, Object>> deleteUser(@PathVariable String id) {
        return ApiResponse.success(adminService.deleteUser(id));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.success(adminService.getStats());
    }

    @GetMapping("/charts")
    public ApiResponse<Map<String, Object>> getCharts() {
        return ApiResponse.success(adminService.getCharts());
    }

    /** 学习总览：在线/总学习人数与各部门进度，用于管理端工作台。 */
    @GetMapping("/learning-overview")
    public ApiResponse<Map<String, Object>> getLearningOverview() {
        return ApiResponse.success(dashboardService.getAdminOverview());
    }

    @GetMapping("/courses")
    public ApiResponse<List<Map<String, Object>>> getCourses() {
        return ApiResponse.success(adminCourseService.getCourses());
    }

    @GetMapping("/courses/{id}")
    public ApiResponse<Map<String, Object>> getCourseById(@PathVariable String id) {
        return ApiResponse.success(adminCourseService.getCourseById(id));
    }

    @PostMapping("/courses")
    public ApiResponse<Map<String, Object>> createCourse(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminCourseService.createCourse(body));
    }

    @PutMapping("/courses/{id}")
    public ApiResponse<Map<String, Object>> updateCourse(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminCourseService.updateCourse(id, body));
    }

    @DeleteMapping("/courses/{id}")
    public ApiResponse<Map<String, Object>> deleteCourse(@PathVariable String id) {
        return ApiResponse.success(adminCourseService.deleteCourse(id));
    }

    @GetMapping("/courses/{courseId}/chapters")
    public ApiResponse<List<Map<String, Object>>> getChapters(@PathVariable String courseId) {
        return ApiResponse.success(adminCourseService.getChapters(courseId));
    }

    @PostMapping("/courses/{courseId}/chapters")
    public ApiResponse<Map<String, Object>> createChapter(@PathVariable String courseId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminCourseService.createChapter(courseId, body));
    }

    @PutMapping("/courses/{courseId}/chapters/{chapterId}")
    public ApiResponse<Map<String, Object>> updateChapter(
            @PathVariable String courseId,
            @PathVariable String chapterId,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminCourseService.updateChapter(courseId, chapterId, body));
    }

    @DeleteMapping("/courses/{courseId}/chapters/{chapterId}")
    public ApiResponse<Map<String, Object>> deleteChapter(
            @PathVariable String courseId,
            @PathVariable String chapterId) {
        return ApiResponse.success(adminCourseService.deleteChapter(courseId, chapterId));
    }
}

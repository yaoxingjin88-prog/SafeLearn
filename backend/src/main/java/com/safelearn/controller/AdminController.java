package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.AdminAnalyticsService;
import com.safelearn.service.AdminService;
import com.safelearn.service.AdminCourseMonitoringService;
import com.safelearn.service.AdminExamService;
import com.safelearn.service.AdminQuestionBankService;
import com.safelearn.service.AdminCourseService;
import com.safelearn.service.AdminDashboardService;
import com.safelearn.service.CourseCategoryService;
import com.safelearn.service.DashboardService;
import com.safelearn.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminAnalyticsService adminAnalyticsService;
    private final AdminCourseService adminCourseService;
    private final AdminCourseMonitoringService adminCourseMonitoringService;
    private final AdminExamService adminExamService;
    private final AdminQuestionBankService adminQuestionBankService;
    private final AdminDashboardService adminDashboardService;
    private final CourseCategoryService courseCategoryService;
    private final DashboardService dashboardService;
    private final SystemConfigService systemConfigService;

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

    /** 管理端首页驾驶舱：一次返回统计、趋势、计划、预警、公告与日历。 */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        return ApiResponse.success(adminDashboardService.getDashboard());
    }

    @GetMapping("/charts")
    public ApiResponse<Map<String, Object>> getCharts() {
        return ApiResponse.success(adminService.getCharts());
    }

    /** 管理端数据分析：学习效果、推演效果、活跃度、部门对比、AI、证书 */
    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> getAnalytics() {
        return ApiResponse.success(adminAnalyticsService.getAnalytics());
    }

    /** 学习总览：在线/总学习人数与各部门进度，用于管理端工作台。 */
    @GetMapping("/learning-overview")
    public ApiResponse<Map<String, Object>> getLearningOverview() {
        return ApiResponse.success(dashboardService.getAdminOverview());
    }

    @GetMapping("/course-categories")
    public ApiResponse<List<Map<String, Object>>> getCourseCategories() {
        return ApiResponse.success(courseCategoryService.listAll());
    }

    @PostMapping("/course-categories")
    public ApiResponse<Map<String, Object>> createCourseCategory(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(courseCategoryService.create(body));
    }

    @PutMapping("/course-categories/{id}")
    public ApiResponse<Map<String, Object>> updateCourseCategory(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.success(courseCategoryService.update(id, body));
    }

    @DeleteMapping("/course-categories/{id}")
    public ApiResponse<Map<String, Object>> deleteCourseCategory(@PathVariable String id) {
        return ApiResponse.success(courseCategoryService.delete(id));
    }

    @GetMapping("/courses")
    public ApiResponse<Map<String, Object>> getCourses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(adminCourseService.searchCourses(
                category, status, keyword, department, createdFrom, createdTo, page, pageSize));
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

    /** 课程学习监控：汇总统计 */
    @GetMapping("/courses/{courseId}/monitoring")
    public ApiResponse<Map<String, Object>> getCourseMonitoring(@PathVariable String courseId) {
        return ApiResponse.success(adminCourseMonitoringService.getMonitoringSummary(courseId));
    }

    /** 课程学习监控：学员分页列表 */
    @GetMapping("/courses/{courseId}/learners")
    public ApiResponse<Map<String, Object>> getCourseLearners(
            @PathVariable String courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String learningStatus,
            @RequestParam(required = false) String warningStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(adminCourseMonitoringService.searchLearners(
                courseId, keyword, department, learningStatus, warningStatus, page, pageSize));
    }

    /** 课程学习监控：学员详情 */
    @GetMapping("/courses/{courseId}/learners/{userId}")
    public ApiResponse<Map<String, Object>> getCourseLearnerDetail(
            @PathVariable String courseId,
            @PathVariable String userId) {
        return ApiResponse.success(adminCourseMonitoringService.getLearnerDetail(courseId, userId));
    }

    /** 考试题库：分页列表 */
    @GetMapping("/exams")
    public ApiResponse<Map<String, Object>> getExams(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String examType,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(adminExamService.searchExams(
                keyword, examType, department, status, createdFrom, createdTo, page, pageSize));
    }

    @GetMapping("/exams/{id}")
    public ApiResponse<Map<String, Object>> getExamById(@PathVariable String id) {
        return ApiResponse.success(adminExamService.getExamById(id));
    }

    @GetMapping("/exams/{id}/stats")
    public ApiResponse<Map<String, Object>> getExamStats(@PathVariable String id) {
        return ApiResponse.success(adminExamService.getExamStats(id));
    }

    @PutMapping("/exams/{id}")
    public ApiResponse<Map<String, Object>> updateExam(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminExamService.updateExam(id, body));
    }

    @DeleteMapping("/exams/{id}")
    public ApiResponse<Map<String, Object>> deleteExam(@PathVariable String id) {
        return ApiResponse.success(adminExamService.deleteExam(id));
    }

    /** 题库分类树 */
    @GetMapping("/question-categories")
    public ApiResponse<List<Map<String, Object>>> getQuestionCategories(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminQuestionBankService.getCategoryTree(keyword));
    }

    /** 题库题目列表 */
    @GetMapping("/questions")
    public ApiResponse<Map<String, Object>> getQuestions(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(adminQuestionBankService.searchQuestions(
                categoryId, type, difficulty, status, tags, keyword, page, pageSize));
    }

    /** 题库题型统计（用于 Tab） */
    @GetMapping("/questions/type-counts")
    public ApiResponse<Map<String, Long>> getQuestionTypeCounts(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminQuestionBankService.getTypeCounts(
                categoryId, difficulty, status, tags, keyword));
    }

    /** 题库全部标签 */
    @GetMapping("/questions/tags")
    public ApiResponse<List<String>> getQuestionTags() {
        return ApiResponse.success(adminQuestionBankService.getAllTags());
    }

    @GetMapping("/questions/{id}")
    public ApiResponse<Map<String, Object>> getQuestionById(@PathVariable String id) {
        return ApiResponse.success(adminQuestionBankService.getQuestionById(id));
    }

    @PostMapping("/questions")
    public ApiResponse<Map<String, Object>> createQuestion(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminQuestionBankService.createQuestion(body));
    }

    @PutMapping("/questions/{id}")
    public ApiResponse<Map<String, Object>> updateQuestion(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminQuestionBankService.updateQuestion(id, body));
    }

    @DeleteMapping("/questions/{id}")
    public ApiResponse<Map<String, Object>> deleteQuestion(@PathVariable String id) {
        return ApiResponse.success(adminQuestionBankService.deleteQuestion(id));
    }

    @PostMapping("/questions/batch")
    public ApiResponse<Map<String, Object>> batchOperateQuestions(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        String action = String.valueOf(body.get("action"));
        return ApiResponse.success(adminQuestionBankService.batchOperate(ids, action));
    }

    // ---------- 系统配置 ----------

    @GetMapping("/system-configs")
    public ApiResponse<List<Map<String, Object>>> getSystemConfigs() {
        return ApiResponse.success(systemConfigService.listAll());
    }

    @GetMapping("/system-configs/category/{category}")
    public ApiResponse<List<Map<String, Object>>> getSystemConfigsByCategory(@PathVariable String category) {
        return ApiResponse.success(systemConfigService.listByCategory(category));
    }

    @PutMapping("/system-configs/{id}")
    public ApiResponse<Map<String, Object>> updateSystemConfig(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.success(systemConfigService.update(id, body));
    }
}

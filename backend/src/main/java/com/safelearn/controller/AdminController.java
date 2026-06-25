package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.AdminAlertCenterService;
import com.safelearn.service.AdminLearningReportService;
import com.safelearn.service.AdminService;
import com.safelearn.service.AdminCourseMonitoringService;
import com.safelearn.service.AdminExamService;
import com.safelearn.service.AdminPaperAssemblyService;
import com.safelearn.service.AdminInboxService;
import com.safelearn.service.AdminInboxStreamService;
import com.safelearn.service.AdminOrganizationService;
import com.safelearn.service.AdminQuestionBankService;
import com.safelearn.service.AdminUserDetailService;
import com.safelearn.service.AdminCourseService;
import com.safelearn.service.AdminDashboardService;
import com.safelearn.service.AdminAnalyticsService;
import com.safelearn.service.CourseCategoryService;
import com.safelearn.service.DashboardService;
import com.safelearn.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminAnalyticsService adminAnalyticsService;
    private final AdminLearningReportService adminLearningReportService;
    private final AdminCourseService adminCourseService;
    private final AdminCourseMonitoringService adminCourseMonitoringService;
    private final AdminExamService adminExamService;
    private final AdminQuestionBankService adminQuestionBankService;
    private final AdminUserDetailService adminUserDetailService;
    private final AdminOrganizationService adminOrganizationService;
    private final AdminInboxService adminInboxService;
    private final AdminInboxStreamService adminInboxStreamService;
    private final AdminAlertCenterService adminAlertCenterService;
    private final AdminPaperAssemblyService adminPaperAssemblyService;
    private final AdminDashboardService adminDashboardService;
    private final CourseCategoryService courseCategoryService;
    private final DashboardService dashboardService;
    private final SystemConfigService systemConfigService;

    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String certStatus,
            @RequestParam(required = false) Integer progressMin,
            @RequestParam(required = false) Integer progressMax,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(adminService.searchUsers(
                keyword, department, role, status, certStatus, progressMin, progressMax, page, pageSize));
    }

    @GetMapping("/users/filter-options")
    public ApiResponse<Map<String, Object>> getUserFilterOptions() {
        return ApiResponse.success(adminService.getUserFilterOptions());
    }

    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminService.createUser(body));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<Map<String, Object>> updateUser(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminService.updateUser(id, body));
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<Map<String, Object>> updateUserStatus(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        boolean enabled = body.get("enabled") instanceof Boolean b
                ? b
                : Boolean.parseBoolean(String.valueOf(body.getOrDefault("enabled", true)));
        return ApiResponse.success(adminService.updateUserStatus(id, enabled));
    }

    @PostMapping("/users/batch")
    public ApiResponse<Map<String, Object>> batchOperateUsers(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminService.batchOperateUsers(body));
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Map<String, Object>> deleteUser(@PathVariable String id) {
        return ApiResponse.success(adminService.deleteUser(id));
    }

    @GetMapping("/users/{id}/detail")
    public ApiResponse<Map<String, Object>> getUserDetail(@PathVariable String id) {
        return ApiResponse.success(adminUserDetailService.getUserDetail(id));
    }

    @PutMapping("/users/{id}/tags")
    public ApiResponse<Map<String, Object>> updateUserTags(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        List<String> tags = body.get("tags") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        return ApiResponse.success(adminUserDetailService.updateUserTags(id, tags));
    }

    @GetMapping("/org/tree")
    public ApiResponse<List<Map<String, Object>>> getOrgTree(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminOrganizationService.getOrgTree(keyword));
    }

    @GetMapping("/departments/{id}")
    public ApiResponse<Map<String, Object>> getDepartmentDetail(@PathVariable String id) {
        return ApiResponse.success(adminOrganizationService.getDepartmentDetail(id));
    }

    @GetMapping("/departments/{id}/members")
    public ApiResponse<Map<String, Object>> getDepartmentMembers(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminOrganizationService.getDepartmentMembers(id, page, pageSize, keyword));
    }

    @GetMapping("/departments/{id}/positions")
    public ApiResponse<List<Map<String, Object>>> getDepartmentPositions(@PathVariable String id) {
        return ApiResponse.success(adminOrganizationService.getDepartmentPositions(id));
    }

    @GetMapping("/departments/{id}/stats")
    public ApiResponse<Map<String, Object>> getDepartmentStats(@PathVariable String id) {
        return ApiResponse.success(adminOrganizationService.getDepartmentStats(id));
    }

    @PostMapping("/departments")
    public ApiResponse<Map<String, Object>> createDepartment(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminOrganizationService.createDepartment(body));
    }

    @PutMapping("/departments/{id}")
    public ApiResponse<Map<String, Object>> updateDepartment(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminOrganizationService.updateDepartment(id, body));
    }

    @DeleteMapping("/departments/{id}")
    public ApiResponse<Map<String, Object>> deleteDepartment(@PathVariable String id) {
        return ApiResponse.success(adminOrganizationService.deleteDepartment(id));
    }

    @PostMapping("/departments/{id}/positions")
    public ApiResponse<Map<String, Object>> createDepartmentPosition(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminOrganizationService.createPosition(id, body));
    }

    @PutMapping("/departments/positions/{positionId}")
    public ApiResponse<Map<String, Object>> updateDepartmentPosition(
            @PathVariable String positionId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminOrganizationService.updatePosition(positionId, body));
    }

    @DeleteMapping("/departments/positions/{positionId}")
    public ApiResponse<Map<String, Object>> deleteDepartmentPosition(@PathVariable String positionId) {
        return ApiResponse.success(adminOrganizationService.deletePosition(positionId));
    }

    @PutMapping("/departments/{id}/members/{userId}")
    public ApiResponse<Map<String, Object>> updateDepartmentMember(
            @PathVariable String id, @PathVariable String userId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminOrganizationService.updateDepartmentMember(id, userId, body));
    }

    @DeleteMapping("/departments/{id}/members/{userId}")
    public ApiResponse<Map<String, Object>> removeDepartmentMember(
            @PathVariable String id, @PathVariable String userId) {
        return ApiResponse.success(adminOrganizationService.removeMemberFromDepartment(id, userId));
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

    @GetMapping("/alerts")
    public ApiResponse<Map<String, Object>> getAlerts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(adminAlertCenterService.searchAlerts(
                keyword, level, type, department, status, dateFrom, dateTo, page, pageSize));
    }

    @GetMapping("/alerts/stats")
    public ApiResponse<Map<String, Object>> getAlertStats() {
        return ApiResponse.success(adminAlertCenterService.getStats());
    }

    @GetMapping("/alerts/export")
    public ApiResponse<List<Map<String, Object>>> exportAlerts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return ApiResponse.success(adminAlertCenterService.exportAlerts(
                keyword, level, type, department, status, dateFrom, dateTo));
    }

    @GetMapping("/alerts/{id}")
    public ApiResponse<Map<String, Object>> getAlertDetail(@PathVariable String id) {
        return ApiResponse.success(adminAlertCenterService.getAlert(id));
    }

    @PostMapping("/alerts")
    public ApiResponse<Map<String, Object>> createAlert(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminAlertCenterService.createAlert(body));
    }

    @PutMapping("/alerts/{id}")
    public ApiResponse<Map<String, Object>> updateAlert(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String manualId = id.startsWith("manual:") ? id.substring("manual:".length()) : id;
        return ApiResponse.success(adminAlertCenterService.updateAlert(manualId, body));
    }

    @PatchMapping("/alerts/{id}/status")
    public ApiResponse<Map<String, Object>> updateAlertStatus(@PathVariable String id,
                                                                @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminAlertCenterService.updateStatus(id, body));
    }

    @GetMapping("/notifications/summary")
    public ApiResponse<Map<String, Object>> getNotificationSummary() {
        return ApiResponse.success(adminInboxService.getNotificationSummary());
    }

    @GetMapping("/messages/summary")
    public ApiResponse<Map<String, Object>> getMessageSummary() {
        return ApiResponse.success(adminInboxService.getMessageSummary());
    }

    @GetMapping("/notifications")
    public ApiResponse<Map<String, Object>> listNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return ApiResponse.success(adminInboxService.listNotifications(page, pageSize, unreadOnly));
    }

    @GetMapping("/messages")
    public ApiResponse<Map<String, Object>> listMessages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return ApiResponse.success(adminInboxService.listMessages(page, pageSize, unreadOnly));
    }

    @PostMapping("/messages")
    public ApiResponse<Map<String, Object>> createMessage(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminInboxService.createMessage(body));
    }

    @GetMapping("/messages/{id}")
    public ApiResponse<Map<String, Object>> getMessage(@PathVariable String id) {
        return ApiResponse.success(adminInboxService.getMessage(id));
    }

    @PutMapping("/messages/{id}")
    public ApiResponse<Map<String, Object>> updateMessage(@PathVariable String id,
                                                          @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminInboxService.updateMessage(id, body));
    }

    @DeleteMapping("/messages/{id}")
    public ApiResponse<Map<String, Object>> deleteMessage(@PathVariable String id) {
        return ApiResponse.success(adminInboxService.deleteMessage(id));
    }

    @PutMapping("/notifications/{id}/read")
    public ApiResponse<Map<String, Object>> markNotificationRead(@PathVariable String id) {
        return ApiResponse.success(adminInboxService.markNotificationRead(id, currentUserId()));
    }

    @PutMapping("/notifications/read-all")
    public ApiResponse<Map<String, Object>> markAllNotificationsRead() {
        return ApiResponse.success(adminInboxService.markAllNotificationsRead(currentUserId()));
    }

    @PutMapping("/messages/{id}/read")
    public ApiResponse<Map<String, Object>> markMessageRead(@PathVariable String id) {
        return ApiResponse.success(adminInboxService.markMessageRead(id, currentUserId()));
    }

    @PutMapping("/messages/read-all")
    public ApiResponse<Map<String, Object>> markAllMessagesRead() {
        return ApiResponse.success(adminInboxService.markAllMessagesRead(currentUserId()));
    }

    /** 管理端收件箱 SSE：有新通知/消息时推送 inbox 事件，前端收到后刷新 summary。 */
    @GetMapping(value = "/inbox/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter inboxStream() {
        return adminInboxStreamService.subscribe(currentUserId());
    }

    private String currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        return String.valueOf(auth.getPrincipal());
    }

    @GetMapping("/charts")
    public ApiResponse<Map<String, Object>> getCharts() {
        return ApiResponse.success(adminService.getCharts());
    }

    /** 管理端数据分析：学习效果、推演效果、活跃度、部门对比、AI、证书 */
    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> getAnalytics(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ApiResponse.success(adminAnalyticsService.getAnalytics(
                department, parseDate(from), parseDate(to)));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    /** 学习报表：概览、图表与待跟进学员。 */
    @GetMapping("/learning-report")
    public ApiResponse<Map<String, Object>> getLearningReport(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String learningStatus,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "7") int trendDays,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(adminLearningReportService.getReport(
                keyword, department, category, learningStatus,
                parseDate(from), parseDate(to), trendDays, page, pageSize));
    }

    /** 向待跟进学员发送学习提醒。 */
    @PostMapping("/learning-report/remind")
    public ApiResponse<Map<String, Object>> remindLearner(@RequestBody Map<String, Object> body) {
        String userId = body.get("userId") != null ? String.valueOf(body.get("userId")).trim() : "";
        String courseId = body.get("courseId") != null ? String.valueOf(body.get("courseId")).trim() : "";
        if (userId.isBlank() || courseId.isBlank()) {
            throw new RuntimeException("学员与课程不能为空");
        }
        String warningStatus = body.get("warningStatus") != null
                ? String.valueOf(body.get("warningStatus")).trim() : null;
        Integer progress = null;
        if (body.get("progress") instanceof Number number) {
            progress = number.intValue();
        } else if (body.get("progress") != null && !String.valueOf(body.get("progress")).isBlank()) {
            progress = Integer.parseInt(String.valueOf(body.get("progress")));
        }
        return ApiResponse.success(adminLearningReportService.sendReminder(
                userId, courseId, currentUserId(), warningStatus, progress));
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

    /** 组卷：分类选项 */
    @GetMapping("/papers/category-options")
    public ApiResponse<List<Map<String, Object>>> getPaperCategoryOptions() {
        return ApiResponse.success(adminPaperAssemblyService.getCategoryOptions());
    }

    /** 组卷：默认配置 */
    @GetMapping("/papers/default-config")
    public ApiResponse<Map<String, Object>> getPaperDefaultConfig() {
        return ApiResponse.success(adminPaperAssemblyService.getDefaultConfig());
    }

    /** 组卷：智能抽题 */
    @PostMapping("/papers/generate")
    public ApiResponse<Map<String, Object>> generatePaper(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminPaperAssemblyService.generatePaper(body));
    }

    @GetMapping("/papers/{id}")
    public ApiResponse<Map<String, Object>> getPaperById(@PathVariable String id) {
        return ApiResponse.success(adminPaperAssemblyService.getPaperById(id));
    }

    @PostMapping("/papers")
    public ApiResponse<Map<String, Object>> savePaper(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminPaperAssemblyService.savePaper(body));
    }

    @PutMapping("/papers/{id}")
    public ApiResponse<Map<String, Object>> updatePaper(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return ApiResponse.success(adminPaperAssemblyService.savePaper(body));
    }

    @PostMapping("/papers/{id}/publish")
    public ApiResponse<Map<String, Object>> publishPaper(@PathVariable String id) {
        return ApiResponse.success(adminPaperAssemblyService.publishPaper(id));
    }

    @PostMapping("/papers/publish")
    public ApiResponse<Map<String, Object>> publishPaperDirect(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminPaperAssemblyService.publishDirect(body));
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

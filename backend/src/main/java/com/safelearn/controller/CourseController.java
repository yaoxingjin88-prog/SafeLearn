package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.dto.ProgressHeartbeatRequest;
import com.safelearn.dto.ProgressRequest;
import com.safelearn.service.CourseCategoryService;
import com.safelearn.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseCategoryService courseCategoryService;

    @GetMapping("/course-categories")
    public ApiResponse<List<Map<String, Object>>> getCourseCategories() {
        return ApiResponse.success(courseCategoryService.listEnabled());
    }

    @GetMapping("/courses")
    public ApiResponse<Map<String, Object>> getCourses(
            Authentication auth,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize) {
        String userId = auth != null ? auth.getPrincipal().toString() : null;
        return ApiResponse.success(courseService.getCourses(category, keyword, userId, page, pageSize));
    }

    @GetMapping("/courses/{id}")
    public ApiResponse<Map<String, Object>> getCourseById(Authentication auth, @PathVariable String id) {
        String userId = auth != null ? auth.getPrincipal().toString() : null;
        return ApiResponse.success(courseService.getCourseDetailForUser(id, userId));
    }

    @GetMapping("/courses/{id}/related")
    public ApiResponse<List<Map<String, Object>>> getRelatedCourses(Authentication auth, @PathVariable String id) {
        String userId = auth != null ? auth.getPrincipal().toString() : null;
        return ApiResponse.success(courseService.getRelatedCourses(id, userId));
    }

    @GetMapping("/courses/{id}/reviews")
    public ApiResponse<Map<String, Object>> getCourseReviews(@PathVariable String id) {
        return ApiResponse.success(courseService.getCourseReviews(id));
    }

    @PostMapping("/courses/{id}/reviews")
    public ApiResponse<Map<String, Object>> submitCourseReview(
            Authentication auth,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        String userId = auth.getPrincipal().toString();
        int rating = body.get("rating") instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(body.get("rating")));
        String content = body.get("content") != null ? String.valueOf(body.get("content")) : "";
        return ApiResponse.success(courseService.submitCourseReview(userId, id, rating, content));
    }

    @GetMapping("/courses/{courseId}/chapters/{chapterId}")
    public ApiResponse<Map<String, Object>> getChapter(
            Authentication auth,
            @PathVariable String courseId,
            @PathVariable String chapterId) {
        String userId = auth != null ? auth.getPrincipal().toString() : null;
        return ApiResponse.success(courseService.getChapter(courseId, chapterId, userId));
    }

    @PostMapping("/progress")
    public ApiResponse<Map<String, Object>> updateProgress(Authentication auth, @RequestBody ProgressRequest req) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(courseService.updateProgress(userId, req));
    }

    @PostMapping("/progress/heartbeat")
    public ApiResponse<Map<String, Object>> heartbeatProgress(
            Authentication auth, @RequestBody ProgressHeartbeatRequest req) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(courseService.heartbeatProgress(userId, req));
    }

    @GetMapping("/progress/my-learning")
    public ApiResponse<List<Map<String, Object>>> getMyLearning(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(courseService.getMyLearningRecords(userId));
    }

    @GetMapping("/progress/{courseId}")
    public ApiResponse<List<Map<String, Object>>> getProgress(Authentication auth, @PathVariable String courseId) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(courseService.getProgress(userId, courseId));
    }

    @GetMapping("/skill-tree")
    public ApiResponse<Map<String, Object>> getSkillTree(Authentication auth) {
        String userId = auth != null ? auth.getPrincipal().toString() : null;
        return ApiResponse.success(courseService.getSkillTree(userId));
    }
}

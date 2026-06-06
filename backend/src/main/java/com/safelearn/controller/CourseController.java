package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.dto.ProgressRequest;
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

    @GetMapping("/courses")
    public ApiResponse<Map<String, Object>> getCourses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        List<Map<String, Object>> courses = courseService.getCourses(category, keyword);
        return ApiResponse.success(Map.of("items", courses, "total", courses.size()));
    }

    @GetMapping("/courses/{id}")
    public ApiResponse<Map<String, Object>> getCourseById(Authentication auth, @PathVariable String id) {
        String userId = auth != null ? auth.getPrincipal().toString() : null;
        return ApiResponse.success(courseService.getCourseDetailForUser(id, userId));
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

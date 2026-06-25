package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.PaperExamService;
import com.safelearn.service.UserExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class UserExamController {

    private final UserExamService userExamService;
    private final PaperExamService paperExamService;

    @GetMapping
    public ApiResponse<Map<String, Object>> listExams(
            Authentication auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(userExamService.listAvailableExams(userId, page, pageSize));
    }

    @GetMapping("/papers/{id}/status")
    public ApiResponse<Map<String, Object>> getPaperStatus(Authentication auth, @PathVariable String id) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(paperExamService.getPaperStatus(userId, id));
    }

    @PostMapping("/papers/{id}/start")
    public ApiResponse<Map<String, Object>> startPaperExam(Authentication auth, @PathVariable String id) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(paperExamService.startPaperExam(userId, id));
    }

    @PostMapping("/papers/attempts/{attemptId}/submit")
    public ApiResponse<Map<String, Object>> submitPaperExam(
            Authentication auth,
            @PathVariable String attemptId,
            @RequestBody Map<String, Object> body) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(paperExamService.submitPaperExam(userId, attemptId, body));
    }
}

package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.CaseProgressService;
import com.safelearn.service.CaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;
    private final CaseProgressService caseProgressService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getCases() {
        return ApiResponse.success(caseService.getCases());
    }

    @GetMapping("/progress/summary")
    public ApiResponse<List<Map<String, Object>>> getProgressSummary(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(caseProgressService.getSummary(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getCaseById(@PathVariable String id) {
        return ApiResponse.success(caseService.getCaseById(id));
    }

    @GetMapping("/{id}/related")
    public ApiResponse<List<Map<String, Object>>> getRelatedCases(@PathVariable String id) {
        return ApiResponse.success(caseService.getRelatedCases(id));
    }

    @GetMapping("/{id}/progress")
    public ApiResponse<Map<String, Object>> getCaseProgress(Authentication auth, @PathVariable String id) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(caseProgressService.getProgress(userId, id));
    }

    @PutMapping("/{id}/progress")
    public ApiResponse<Map<String, Object>> saveCaseProgress(
            Authentication auth,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(caseProgressService.saveProgress(userId, id, body));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<Map<String, Object>> completeCase(
            Authentication auth,
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(caseProgressService.complete(userId, id, body != null ? body : Map.of()));
    }

    @DeleteMapping("/{id}/progress")
    public ApiResponse<Map<String, Object>> resetCaseProgress(Authentication auth, @PathVariable String id) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(caseProgressService.resetProgress(userId, id));
    }
}

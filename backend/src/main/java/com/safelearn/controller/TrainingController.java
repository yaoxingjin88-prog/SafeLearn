package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.dto.TrainingDecisionRequest;
import com.safelearn.service.TrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @GetMapping("/scenarios")
    public ApiResponse<List<Map<String, Object>>> getScenarios() {
        return ApiResponse.success(trainingService.getScenarios());
    }

    @GetMapping("/scenarios/{id}")
    public ApiResponse<Map<String, Object>> getScenarioById(@PathVariable String id) {
        return ApiResponse.success(trainingService.getScenarioById(id));
    }

    @PostMapping("/start")
    public ApiResponse<Map<String, Object>> start(Authentication auth, @RequestBody Map<String, Object> body) {
        String userId = auth.getPrincipal().toString();
        String scenarioId = (String) body.get("scenarioId");
        return ApiResponse.success(trainingService.startTraining(userId, scenarioId));
    }

    @PostMapping("/decision")
    public ApiResponse<Map<String, Object>> decision(Authentication auth, @RequestBody TrainingDecisionRequest req) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(trainingService.submitDecision(userId, req));
    }

    @GetMapping("/records")
    public ApiResponse<List<Map<String, Object>>> getRecords(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(trainingService.getRecords(userId));
    }

    @GetMapping("/records/{id}")
    public ApiResponse<Map<String, Object>> getRecordById(@PathVariable String id) {
        return ApiResponse.success(trainingService.getRecordById(id));
    }

    @GetMapping("/records/{id}/report")
    public ApiResponse<Map<String, Object>> getReport(@PathVariable String id) {
        return ApiResponse.success(trainingService.getRecordById(id));
    }
}

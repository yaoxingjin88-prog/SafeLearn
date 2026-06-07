package com.safelearn.deduction.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.deduction.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/deduction")
@RequiredArgsConstructor
public class DeductionController {

    private final DeductionSessionService sessionService;
    private final DeductionReplayService replayService;
    private final DeductionScoringService scoringService;
    private final DeductionAnalyticsService analyticsService;

    @PostMapping("/sessions")
    public ApiResponse<Map<String, Object>> startSession(Authentication auth, @RequestBody Map<String, Object> body) {
        String userId = auth.getPrincipal().toString();
        String scenarioId = (String) body.get("scenarioId");
        return ApiResponse.success(sessionService.startSession(userId, scenarioId));
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<Map<String, Object>> getSession(Authentication auth, @PathVariable String id) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(sessionService.getSession(userId, id));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<Map<String, Object>>> listSessions(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(sessionService.listUserSessions(userId));
    }

    @PostMapping("/sessions/{id}/events")
    public ApiResponse<Void> appendEvents(
            Authentication auth,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        String userId = auth.getPrincipal().toString();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) body.get("events");
        sessionService.appendEvents(userId, id, events != null ? events : List.of());
        return ApiResponse.success(null);
    }

    @PostMapping("/sessions/{id}/finish")
    public ApiResponse<Map<String, Object>> finishSession(
            Authentication auth,
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body) {
        String userId = auth.getPrincipal().toString();
        sessionService.finishSession(userId, id, body);
        return ApiResponse.success(scoringService.scoreSession(userId, id));
    }

    @GetMapping("/sessions/{id}/replay")
    public ApiResponse<Map<String, Object>> getReplay(Authentication auth, @PathVariable String id) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(replayService.getReplayData(userId, id));
    }

    @GetMapping("/sessions/{id}/score")
    public ApiResponse<Map<String, Object>> getScore(Authentication auth, @PathVariable String id) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(scoringService.getScoreReport(userId, id));
    }

    @GetMapping("/analytics/me")
    public ApiResponse<Map<String, Object>> myAnalytics(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(analyticsService.getUserAnalytics(userId));
    }

    @GetMapping("/analytics/user/{userId}")
    public ApiResponse<Map<String, Object>> userAnalytics(Authentication auth, @PathVariable String userId) {
        auth.getPrincipal().toString();
        return ApiResponse.success(analyticsService.getUserAnalytics(userId));
    }

    @GetMapping("/analytics/scenario/{scenarioId}")
    public ApiResponse<Map<String, Object>> scenarioAnalytics(@PathVariable String scenarioId) {
        return ApiResponse.success(analyticsService.getScenarioAnalytics(scenarioId));
    }
}

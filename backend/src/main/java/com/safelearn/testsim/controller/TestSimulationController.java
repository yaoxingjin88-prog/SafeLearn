package com.safelearn.testsim.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.testsim.service.TestSimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test-simulation")
@RequiredArgsConstructor
public class TestSimulationController {

    private final TestSimulationService service;

    @PostMapping("/sessions")
    public ApiResponse<Map<String, Object>> startSession(Authentication auth, @RequestBody Map<String, Object> body) {
        String userId = auth.getPrincipal().toString();
        String code = body.get("scenarioCode") != null ? body.get("scenarioCode").toString() : "guangzhou_614";
        return ApiResponse.success(service.startSession(userId, code));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<Map<String, Object>>> listSessions(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(service.listUserSessions(userId));
    }

    @PostMapping("/sessions/{id}/decisions")
    public ApiResponse<Void> recordDecision(
            Authentication auth,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        service.recordDecision(auth.getPrincipal().toString(), id, body);
        return ApiResponse.success(null);
    }

    @PostMapping("/sessions/{id}/abandon")
    public ApiResponse<Void> abandonSession(Authentication auth, @PathVariable String id) {
        service.abandonSession(auth.getPrincipal().toString(), id);
        return ApiResponse.success(null);
    }

    @PostMapping("/sessions/{id}/finish")
    public ApiResponse<Map<String, Object>> finishSession(
            Authentication auth,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.success(service.finishSession(auth.getPrincipal().toString(), id, body));
    }

    @GetMapping("/sessions/{id}/score")
    public ApiResponse<Map<String, Object>> getScore(Authentication auth, @PathVariable String id) {
        return ApiResponse.success(service.getScore(auth.getPrincipal().toString(), id));
    }
}

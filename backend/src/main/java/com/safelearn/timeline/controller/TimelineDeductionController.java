package com.safelearn.timeline.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.timeline.service.TimelineDeductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timeline-deduction")
@RequiredArgsConstructor
public class TimelineDeductionController {

    private final TimelineDeductionService service;

    @GetMapping("/scenarios")
    public ApiResponse<List<Map<String, Object>>> listScenarios() {
        return ApiResponse.success(service.listScenarios());
    }

    @PostMapping("/sessions")
    public ApiResponse<Map<String, Object>> startSession(Authentication auth, @RequestBody Map<String, Object> body) {
        String userId = auth.getPrincipal().toString();
        String code = body.get("scenarioCode") != null ? body.get("scenarioCode").toString() : "beijing_416";
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
        String userId = auth.getPrincipal().toString();
        service.recordDecision(userId, id, body);
        return ApiResponse.success(null);
    }

    @PostMapping("/sessions/{id}/abandon")
    public ApiResponse<Void> abandonSession(Authentication auth, @PathVariable String id) {
        String userId = auth.getPrincipal().toString();
        service.abandonSession(userId, id);
        return ApiResponse.success(null);
    }

    @PostMapping("/sessions/{id}/finish")
    public ApiResponse<Map<String, Object>> finishSession(
            Authentication auth,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(service.finishSession(userId, id, body));
    }

    @GetMapping("/sessions/{id}/score")
    public ApiResponse<Map<String, Object>> getScore(Authentication auth, @PathVariable String id) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(service.getScore(userId, id));
    }
}

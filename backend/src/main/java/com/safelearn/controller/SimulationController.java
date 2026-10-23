package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.SimulationRecordsService;
import com.safelearn.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;
    private final SimulationRecordsService recordsService;

    @GetMapping("/scenarios")
    public ApiResponse<List<Map<String, Object>>> getScenarios() {
        return ApiResponse.success(simulationService.getScenarios());
    }

    @GetMapping("/scenarios/{id}")
    public ApiResponse<Map<String, Object>> getScenarioById(@PathVariable String id) {
        return ApiResponse.success(simulationService.getScenarioById(id));
    }

    @PostMapping("/start")
    public ApiResponse<Map<String, Object>> start(@RequestBody Map<String, Object> body) {
        String scenarioId = (String) body.get("scenarioId");
        return ApiResponse.success(simulationService.startSimulation(scenarioId, body));
    }

    @GetMapping("/records")
    public ApiResponse<List<Map<String, Object>>> listRecords(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(recordsService.listAllUserRecords(userId));
    }
}

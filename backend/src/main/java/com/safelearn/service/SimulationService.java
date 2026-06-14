package com.safelearn.service;

import com.safelearn.entity.Scenario;
import com.safelearn.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final ScenarioRepository scenarioRepo;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public List<Map<String, Object>> getScenarios() {
        return scenarioRepo.findAll().stream()
                .filter(this::isPlayableScenario)
                .map(this::toScenarioInfo)
                .toList();
    }

    private boolean isPlayableScenario(Scenario s) {
        if (s.getName() == null || s.getName().isBlank()) return false;
        String name = s.getName().trim().toLowerCase(Locale.ROOT);
        if ("test".equals(name) || name.startsWith("test_") || name.startsWith("test ")) return false;
        if (s.getDuration() == null || s.getDuration() <= 0) return false;
        if (isEmergencyTrainingScenario(s)) return false;
        int batteryCount = extractBatteryCount(s.getInitialConditions());
        return batteryCount > 0;
    }

    /** 应急决策训练专用场景（北京/广州案例等），已在时间轴/测试推演区展示 */
    @SuppressWarnings("unchecked")
    private boolean isEmergencyTrainingScenario(Scenario s) {
        Object parsed = parseJson(s.getInitialConditions());
        if (!(parsed instanceof Map<?, ?> map)) return false;
        return "emergency_case".equals(map.get("trainingKind"));
    }

    @SuppressWarnings("unchecked")
    private int extractBatteryCount(String initialConditionsJson) {
        Object parsed = parseJson(initialConditionsJson);
        if (!(parsed instanceof Map<?, ?> map)) return 0;
        Object count = map.get("batteryCount");
        if (count instanceof Number n) return n.intValue();
        return 0;
    }

    public Map<String, Object> getScenarioById(String id) {
        Scenario s = scenarioRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("场景不存在"));
        return toScenarioInfo(s);
    }

    public Map<String, Object> startSimulation(String scenarioId, Map<String, Object> customParams) {
        Scenario s = scenarioRepo.findById(scenarioId)
                .orElseThrow(() -> new RuntimeException("场景不存在"));
        Map<String, Object> result = new HashMap<>();
        result.put("simulationId", UUID.randomUUID().toString());
        result.put("scenario", toScenarioInfo(s));
        return result;
    }

    private Map<String, Object> toScenarioInfo(Scenario s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("description", s.getDescription());
        m.put("duration", s.getDuration());
        m.put("difficulty", mapDifficulty(s.getDifficulty()));
        Object conditions = parseJson(s.getInitialConditions());
        if (conditions == null) {
            conditions = Map.of("batteryCount", 1, "initialTemperature", 25, "batteryType", "lithium_iron_phosphate", "capacity", 0.1);
        }
        m.put("initialConditions", conditions);
        m.put("events", parseJson(s.getEvents()));
        return m;
    }

    private String mapDifficulty(Integer difficulty) {
        if (difficulty == null) return "easy";
        return switch (difficulty) {
            case 2 -> "medium";
            case 3 -> "hard";
            default -> "easy";
        };
    }

    private Object parseJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            try {
                return objectMapper.readValue(json, List.class);
            } catch (Exception e2) {
                return json;
            }
        }
    }
}

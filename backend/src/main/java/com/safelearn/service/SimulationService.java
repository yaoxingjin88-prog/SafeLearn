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
        return scenarioRepo.findAll().stream().map(this::toScenarioInfo).toList();
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
        m.put("difficulty", s.getDifficulty());
        Object conditions = parseJson(s.getInitialConditions());
        if (conditions == null) {
            conditions = Map.of("batteryCount", 1, "initialTemperature", 25, "batteryType", "lithium_iron_phosphate", "capacity", 0.1);
        }
        m.put("initialConditions", conditions);
        m.put("events", parseJson(s.getEvents()));
        return m;
    }

    private Object parseJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return json;
        }
    }
}

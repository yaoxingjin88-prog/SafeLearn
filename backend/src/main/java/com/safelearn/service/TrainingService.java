package com.safelearn.service;

import com.safelearn.dto.TrainingDecisionRequest;
import com.safelearn.entity.Scenario;
import com.safelearn.entity.TrainingRecord;
import com.safelearn.entity.User;
import com.safelearn.repository.ScenarioRepository;
import com.safelearn.repository.TrainingRecordRepository;
import com.safelearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final ScenarioRepository scenarioRepo;
    private final TrainingRecordRepository recordRepo;
    private final UserRepository userRepo;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public List<Map<String, Object>> getScenarios() {
        return scenarioRepo.findAll().stream().map(this::toScenarioInfo).toList();
    }

    public Map<String, Object> getScenarioById(String id) {
        Scenario s = scenarioRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("场景不存在"));
        return toScenarioInfo(s);
    }

    public Map<String, Object> startTraining(String userId, String scenarioId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        Scenario scenario = scenarioRepo.findById(scenarioId).orElseThrow(() -> new RuntimeException("场景不存在"));

        TrainingRecord record = new TrainingRecord();
        record.setUser(user);
        record.setScenario(scenario);
        record.setStartTime(LocalDateTime.now());
        record.setTotalScore(0);
        record = recordRepo.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", record.getId());
        result.put("firstDecisionPoint", Map.of(
                "id", "dp1",
                "description", "发现电池温度异常升高，应采取什么措施？",
                "options", List.of(
                        Map.of("id", "opt1", "text", "立即切断电源", "score", 30),
                        Map.of("id", "opt2", "text", "继续观察", "score", 0),
                        Map.of("id", "opt3", "text", "启动冷却系统", "score", 20)
                )
        ));
        return result;
    }

    public Map<String, Object> submitDecision(String userId, TrainingDecisionRequest req) {
        TrainingRecord record = recordRepo.findById(req.getRecordId())
                .orElseThrow(() -> new RuntimeException("训练记录不存在"));

        int score = 30;
        String consequence = "操作正确，成功控制险情";

        int totalScore = (record.getTotalScore() != null ? record.getTotalScore() : 0) + score;
        record.setTotalScore(totalScore);
        recordRepo.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("consequence", consequence);
        result.put("totalScore", totalScore);
        result.put("nextDecisionPoint", null);
        return result;
    }

    public List<Map<String, Object>> getRecords(String userId) {
        return recordRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toRecordInfo).toList();
    }

    public Map<String, Object> getRecordById(String id) {
        TrainingRecord record = recordRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("记录不存在"));
        return toRecordDetail(record);
    }

    private Map<String, Object> toScenarioInfo(Scenario s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("description", s.getDescription());
        m.put("duration", s.getDuration());
        m.put("timeLimit", s.getDuration());
        m.put("difficulty", s.getDifficulty());
        m.put("initialConditions", parseJson(s.getInitialConditions()));
        m.put("events", parseJson(s.getEvents()));
        m.put("decisionPoints", parseJson(s.getDecisionPoints()));
        return m;
    }

    private Map<String, Object> toRecordInfo(TrainingRecord r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("scenarioId", r.getScenario() != null ? r.getScenario().getId() : null);
        m.put("scenarioName", r.getScenario() != null ? r.getScenario().getName() : null);
        m.put("totalScore", r.getTotalScore());
        m.put("rating", r.getRating());
        m.put("startTime", r.getStartTime() != null ? r.getStartTime().toString() : null);
        m.put("endTime", r.getEndTime() != null ? r.getEndTime().toString() : null);
        m.put("completedAt", r.getEndTime() != null ? r.getEndTime().toString() : (r.getStartTime() != null ? r.getStartTime().toString() : null));
        return m;
    }

    private Map<String, Object> toRecordDetail(TrainingRecord r) {
        Map<String, Object> m = toRecordInfo(r);
        m.put("decisions", parseJsonList(r.getDecisions()));
        m.put("feedback", r.getFeedback());
        return m;
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

    private Object parseJsonList(String json) {
        if (json == null) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            try {
                return objectMapper.readValue(json, Map.class);
            } catch (Exception e2) {
                return new ArrayList<>();
            }
        }
    }
}

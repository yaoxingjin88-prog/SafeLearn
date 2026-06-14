package com.safelearn.timeline.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.timeline.entity.TdScore;
import com.safelearn.timeline.entity.TdSession;
import com.safelearn.timeline.repository.TdScoreRepository;
import com.safelearn.timeline.repository.TdSessionRepository;
import com.safelearn.timeline.util.TdDebriefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TimelineDeductionService {

    private static final String SCENARIO_ID = "td-beijing-416";

    private final TdSessionRepository sessionRepo;
    private final TdScoreRepository scoreRepo;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> listScenarios() {
        Map<String, Object> scenario = loadScenarioMeta();
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(scenario);
        return list;
    }

    @Transactional
    public Map<String, Object> startSession(String userId, String scenarioCode) {
        sessionRepo.findByUserIdAndScenarioIdAndStatus(userId, SCENARIO_ID, "running")
                .forEach(s -> {
                    s.setStatus("abandoned");
                    s.setEndedAt(LocalDateTime.now());
                    sessionRepo.save(s);
                });

        TdSession session = new TdSession();
        session.setUserId(userId);
        session.setScenarioId(SCENARIO_ID);
        session.setStatus("running");
        session.setCurrentPhase("normal");
        session.setCurrentNodeKey("n0");
        session.setRiskIndex(20);
        sessionRepo.save(session);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("scenarioId", SCENARIO_ID);
        result.put("status", session.getStatus());
        return result;
    }

    @Transactional
    public void recordDecision(String userId, String sessionId, Map<String, Object> body) {
        TdSession session = requireSession(userId, sessionId);
        int riskDelta = body.get("riskDelta") instanceof Number n ? n.intValue() : 0;
        int current = session.getRiskIndex() != null ? session.getRiskIndex() : 0;
        session.setRiskIndex(Math.max(0, Math.min(100, current + riskDelta)));
        sessionRepo.save(session);
    }

    @Transactional
    public Map<String, Object> finishSession(String userId, String sessionId, Map<String, Object> report) {
        TdSession session = requireSession(userId, sessionId);
        session.setStatus("finished");
        session.setEndedAt(LocalDateTime.now());
        if (report.get("outcome") != null) session.setOutcome(report.get("outcome").toString());
        if (report.get("branch") != null) session.setBranchPath(report.get("branch").toString());
        sessionRepo.save(session);

        TdScore score = scoreRepo.findBySessionId(sessionId).orElse(new TdScore());
        score.setSessionId(sessionId);
        score.setTotalScore(intVal(report.get("totalScore")));
        score.setAiComment(str(report.get("instructorComment")));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dims = (List<Map<String, Object>>) report.get("dimensions");
        if (dims != null) {
            for (Map<String, Object> d : dims) {
                String key = str(d.get("key"));
                int val = intVal(d.get("score"));
                switch (key) {
                    case "risk" -> score.setRiskIdentification(val);
                    case "decision" -> score.setDecisionMaking(val);
                    case "response" -> score.setEmergencyResponse(val);
                    case "analysis" -> score.setAccidentAnalysis(val);
                    default -> { }
                }
            }
        }

        try {
            score.setStrengthsJson(objectMapper.writeValueAsString(report.get("strengths")));
            score.setWeaknessesJson(objectMapper.writeValueAsString(report.get("weaknesses")));
            score.setRecommendationsJson(objectMapper.writeValueAsString(report.get("recommendations")));
            score.setFishboneJson(objectMapper.writeValueAsString(report.get("fishbone")));
            score.setFiveWhyJson(objectMapper.writeValueAsString(report.get("fiveWhy")));
        } catch (Exception ignored) { }

        scoreRepo.save(score);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("totalScore", score.getTotalScore());
        return result;
    }

    @Transactional
    public void abandonSession(String userId, String sessionId) {
        TdSession session = requireSession(userId, sessionId);
        if ("running".equals(session.getStatus())) {
            session.setStatus("abandoned");
            session.setEndedAt(LocalDateTime.now());
            sessionRepo.save(session);
        }
    }

    public List<Map<String, Object>> listUserSessions(String userId) {
        return sessionRepo.findByUserIdOrderByStartedAtDesc(userId).stream()
                .filter(s -> SCENARIO_ID.equals(s.getScenarioId()))
                .filter(s -> "finished".equals(s.getStatus()))
                .filter(s -> scoreRepo.findBySessionId(s.getId()).isPresent())
                .map(s -> {
                    TdScore sc = scoreRepo.findBySessionId(s.getId()).orElseThrow();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("sessionId", s.getId());
                    m.put("type", "timeline");
                    m.put("scenarioCode", "beijing_416");
                    m.put("scenarioName", "北京丰台 4·16 储能电站事故推演");
                    m.put("status", s.getStatus());
                    m.put("outcome", s.getOutcome());
                    m.put("branch", s.getBranchPath());
                    m.put("startedAt", s.getStartedAt());
                    m.put("finishedAt", s.getEndedAt());
                    m.putAll(TdDebriefMapper.toListSummary(s, sc, objectMapper));
                    return m;
                })
                .toList();
    }

    private String scoreToRating(Integer score) {
        if (score == null) return "pending";
        if (score >= 90) return "excellent";
        if (score >= 75) return "good";
        if (score >= 60) return "average";
        return "poor";
    }

    public Map<String, Object> getScore(String userId, String sessionId) {
        TdSession session = requireSession(userId, sessionId);
        return scoreRepo.findBySessionId(sessionId)
                .map(sc -> TdDebriefMapper.toDebriefMap(session, sc, objectMapper))
                .orElse(Map.of("sessionId", sessionId, "message", "评分尚未生成"));
    }

    private TdSession requireSession(String userId, String sessionId) {
        TdSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("推演会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该推演会话");
        }
        return session;
    }

    private Map<String, Object> loadScenarioMeta() {
        try (InputStream in = new ClassPathResource("data/beijing416_scenario.json").getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<>() {});
        } catch (Exception e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("id", SCENARIO_ID);
            fallback.put("code", "beijing_416");
            fallback.put("title", "北京丰台 4·16 储能电站事故推演");
            fallback.put("subtitle", "时间轴沉浸式决策训练");
            return fallback;
        }
    }

    private int intVal(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }

    private String str(Object o) {
        return o != null ? o.toString() : "";
    }

    private List<?> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Object>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}

package com.safelearn.testsim.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.timeline.entity.TdScore;
import com.safelearn.timeline.entity.TdSession;
import com.safelearn.timeline.repository.TdScoreRepository;
import com.safelearn.timeline.repository.TdSessionRepository;
import com.safelearn.timeline.util.TdDebriefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TestSimulationService {

    private static final String SCENARIO_ID = "ts-guangzhou-614";

    private final TdSessionRepository sessionRepo;
    private final TdScoreRepository scoreRepo;
    private final ObjectMapper objectMapper;

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
        session.setCurrentPhase("background");
        session.setCurrentNodeKey("n0");
        session.setRiskIndex(15);
        sessionRepo.save(session);

        return Map.of(
                "sessionId", session.getId(),
                "scenarioId", SCENARIO_ID,
                "status", session.getStatus()
        );
    }

    @Transactional
    public void recordDecision(String userId, String sessionId, Map<String, Object> body) {
        TdSession session = requireSession(userId, sessionId);
        int riskDelta = 0;
        if (body.get("riskDelta") instanceof Number n) riskDelta = n.intValue();
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
        sessionRepo.save(session);

        TdScore score = scoreRepo.findBySessionId(sessionId).orElse(new TdScore());
        score.setSessionId(sessionId);
        score.setTotalScore(intVal(report.get("totalScore")));
        Object comment = report.get("instructorComment");
        if (comment == null) comment = report.get("instructorSummary");
        score.setAiComment(str(comment));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dims = (List<Map<String, Object>>) report.get("dimensions");
        if (dims != null) {
            for (Map<String, Object> d : dims) {
                String key = str(d.get("key"));
                int val = intVal(d.get("score"));
                switch (key) {
                    case "risk" -> score.setRiskIdentification(val);
                    case "speed", "accuracy" -> score.setDecisionMaking(
                            Math.max(score.getDecisionMaking() != null ? score.getDecisionMaking() : 0, val));
                    case "emergency" -> score.setEmergencyResponse(val);
                    default -> { }
                }
            }
        }

        try {
            score.setStrengthsJson(objectMapper.writeValueAsString(report.get("strengths")));
            score.setWeaknessesJson(objectMapper.writeValueAsString(report.get("weaknesses")));
            score.setRecommendationsJson(objectMapper.writeValueAsString(report.get("courseLinks")));
        } catch (Exception ignored) { }

        scoreRepo.save(score);

        return Map.of("sessionId", sessionId, "totalScore", score.getTotalScore());
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
                    m.put("type", "test");
                    m.put("scenarioCode", "guangzhou_614");
                    m.put("scenarioName", "广州智光储能科技「6·14」锂电池包闪爆事故推演");
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
        if (!SCENARIO_ID.equals(session.getScenarioId())) {
            throw new RuntimeException("会话类型不匹配");
        }
        return session;
    }

    private String scoreToRating(Integer score) {
        if (score == null) return "pending";
        if (score >= 90) return "excellent";
        if (score >= 75) return "good";
        if (score >= 60) return "average";
        return "poor";
    }

    private int intVal(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }

    private String str(Object o) {
        return o != null ? o.toString() : "";
    }
}

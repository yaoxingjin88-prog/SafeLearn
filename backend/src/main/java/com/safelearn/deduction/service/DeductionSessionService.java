package com.safelearn.deduction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.deduction.entity.SimulationDecision;
import com.safelearn.deduction.entity.SimulationEventLog;
import com.safelearn.deduction.entity.SimulationSession;
import com.safelearn.deduction.repository.SimulationDecisionRepository;
import com.safelearn.deduction.repository.SimulationEventLogRepository;
import com.safelearn.deduction.repository.SimulationSessionRepository;
import com.safelearn.entity.Scenario;
import com.safelearn.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DeductionSessionService {

    private final SimulationSessionRepository sessionRepo;
    private final SimulationEventLogRepository eventLogRepo;
    private final SimulationDecisionRepository decisionRepo;
    private final ScenarioRepository scenarioRepo;
    private final ObjectMapper objectMapper;

    private static final Set<String> OPTIMAL_OPTIONS = Set.of(
            "opt-vent", "opt-isolate", "opt-confirm-off", "opt-extinguish", "opt-evacuate"
    );

    @Transactional
    public Map<String, Object> startSession(String userId, String scenarioId) {
        scenarioRepo.findById(scenarioId).orElseThrow(() -> new RuntimeException("场景不存在"));

        SimulationSession session = new SimulationSession();
        session.setUserId(userId);
        session.setScenarioId(scenarioId);
        session.setStatus("running");
        session.setStartedAt(LocalDateTime.now());
        session.setMachineState("monitoring");
        session = sessionRepo.save(session);

        return Map.of(
                "sessionId", session.getId(),
                "scenarioId", scenarioId,
                "status", session.getStatus()
        );
    }

    public Map<String, Object> getSession(String userId, String sessionId) {
        SimulationSession session = requireOwnedSession(userId, sessionId);
        return toSessionMap(session);
    }

    @Transactional
    public void appendEvents(String userId, String sessionId, List<Map<String, Object>> events) {
        requireOwnedSession(userId, sessionId);
        for (Map<String, Object> e : events) {
            int seq = ((Number) e.get("seq")).intValue();
            if (eventLogRepo.existsBySessionIdAndSeq(sessionId, seq)) {
                continue;
            }
            SimulationEventLog log = new SimulationEventLog();
            log.setSessionId(sessionId);
            log.setSeq(seq);
            log.setElapsedMs(((Number) e.get("elapsedMs")).longValue());
            log.setEventType((String) e.get("eventType"));
            log.setMachineState((String) e.get("machineState"));
            try {
                Object payload = e.get("payload");
                log.setPayload(payload instanceof String ? (String) payload : objectMapper.writeValueAsString(payload));
            } catch (Exception ex) {
                log.setPayload("{}");
            }
            eventLogRepo.save(log);
        }
    }

    @Transactional
    public Map<String, Object> finishSession(String userId, String sessionId, Map<String, Object> summary) {
        SimulationSession session = requireOwnedSession(userId, sessionId);
        session.setStatus("completed");
        session.setFinishedAt(LocalDateTime.now());
        if (summary != null) {
            if (summary.get("outcome") != null) session.setOutcome(String.valueOf(summary.get("outcome")));
            if (summary.get("branch") != null) session.setBranch(String.valueOf(summary.get("branch")));
            if (summary.get("elapsedMs") != null) session.setElapsedMs(((Number) summary.get("elapsedMs")).longValue());
            if (summary.get("maxTemperature") != null) {
                session.setMaxTemperature(new BigDecimal(String.valueOf(summary.get("maxTemperature"))));
            }
            if (summary.get("ruleScore") != null) session.setRuleScore(((Number) summary.get("ruleScore")).intValue());
            if (summary.get("machineState") != null) session.setMachineState(String.valueOf(summary.get("machineState")));
            saveDecisions(sessionId, summary.get("decisions"));
        }
        sessionRepo.save(session);
        return toSessionMap(session);
    }

    @SuppressWarnings("unchecked")
    private void saveDecisions(String sessionId, Object decisionsObj) {
        if (!(decisionsObj instanceof List<?> list)) return;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) continue;
            SimulationDecision d = new SimulationDecision();
            d.setSessionId(sessionId);
            d.setDecisionPointId(String.valueOf(m.get("decisionPointId")));
            d.setOptionId(String.valueOf(m.get("optionId")));
            d.setElapsedMs(((Number) m.get("elapsedMs")).longValue());
            d.setResponseTimeMs(m.get("responseTimeMs") != null
                    ? ((Number) m.get("responseTimeMs")).longValue()
                    : d.getElapsedMs());
            d.setScoreDelta(m.get("scoreDelta") != null ? ((Number) m.get("scoreDelta")).intValue() : 0);
            d.setOptimal(OPTIMAL_OPTIONS.contains(d.getOptionId()));
            decisionRepo.save(d);
        }
    }

    public List<Map<String, Object>> listUserSessions(String userId) {
        return sessionRepo.findByUserIdOrderByStartedAtDesc(userId).stream()
                .map(this::toSessionMap)
                .toList();
    }

    private SimulationSession requireOwnedSession(String userId, String sessionId) {
        SimulationSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("推演会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该推演会话");
        }
        return session;
    }

    private Map<String, Object> toSessionMap(SimulationSession s) {
        Map<String, Object> m = new HashMap<>();
        m.put("sessionId", s.getId());
        m.put("scenarioId", s.getScenarioId());
        m.put("status", s.getStatus());
        m.put("outcome", s.getOutcome());
        m.put("branch", s.getBranch());
        m.put("elapsedMs", s.getElapsedMs());
        m.put("maxTemperature", s.getMaxTemperature());
        m.put("ruleScore", s.getRuleScore());
        m.put("totalScore", s.getTotalScore());
        m.put("rating", s.getRating());
        m.put("machineState", s.getMachineState());
        m.put("startedAt", s.getStartedAt());
        m.put("finishedAt", s.getFinishedAt());
        Optional<Scenario> scenario = scenarioRepo.findById(s.getScenarioId());
        scenario.ifPresent(sc -> m.put("scenarioName", sc.getName()));
        return m;
    }
}

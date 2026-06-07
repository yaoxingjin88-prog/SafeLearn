package com.safelearn.deduction.service;

import com.safelearn.deduction.entity.SimulationEventLog;
import com.safelearn.deduction.entity.SimulationSession;
import com.safelearn.deduction.repository.SimulationEventLogRepository;
import com.safelearn.deduction.repository.SimulationSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DeductionReplayService {

    private final SimulationEventLogRepository eventLogRepo;
    private final SimulationSessionRepository sessionRepo;

    public Map<String, Object> getReplayData(String userId, String sessionId) {
        SimulationSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("推演会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该推演回放");
        }
        List<SimulationEventLog> logs = eventLogRepo.findBySessionIdOrderBySeqAsc(sessionId);
        List<Map<String, Object>> events = logs.stream().map(log -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("seq", log.getSeq());
            m.put("sessionId", log.getSessionId());
            m.put("elapsedMs", log.getElapsedMs());
            m.put("eventType", log.getEventType());
            m.put("machineState", log.getMachineState());
            m.put("payload", log.getPayload());
            m.put("createdAt", log.getCreatedAt());
            return m;
        }).toList();

        long durationMs = logs.isEmpty() ? 0 : logs.get(logs.size() - 1).getElapsedMs();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("scenarioId", session.getScenarioId());
        result.put("outcome", session.getOutcome());
        result.put("totalScore", session.getTotalScore());
        result.put("durationMs", durationMs);
        result.put("eventCount", events.size());
        result.put("events", events);
        return result;
    }
}

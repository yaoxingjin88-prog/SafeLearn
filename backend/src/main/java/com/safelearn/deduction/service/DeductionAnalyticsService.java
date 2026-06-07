package com.safelearn.deduction.service;

import com.safelearn.deduction.entity.SimulationSession;
import com.safelearn.deduction.repository.SimulationSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DeductionAnalyticsService {

    private final SimulationSessionRepository sessionRepo;

    public Map<String, Object> getUserAnalytics(String userId) {
        List<SimulationSession> sessions = sessionRepo.findByUserIdOrderByStartedAtDesc(userId);
        long total = sessions.size();
        long success = sessions.stream().filter(s -> "success".equals(s.getOutcome())).count();
        double avgScore = sessions.stream()
                .filter(s -> s.getTotalScore() != null)
                .mapToInt(SimulationSession::getTotalScore)
                .average()
                .orElse(0);

        return Map.of(
                "totalSessions", total,
                "successCount", success,
                "successRate", total == 0 ? 0 : Math.round(success * 1000.0 / total) / 10.0,
                "avgScore", Math.round(avgScore * 10) / 10.0,
                "recentSessions", sessions.stream().limit(10).map(this::brief).toList()
        );
    }

    public Map<String, Object> getScenarioAnalytics(String scenarioId) {
        long total = sessionRepo.countByScenarioId(scenarioId);
        return Map.of("scenarioId", scenarioId, "totalSessions", total);
    }

    private Map<String, Object> brief(SimulationSession s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sessionId", s.getId());
        m.put("outcome", s.getOutcome());
        m.put("totalScore", s.getTotalScore());
        m.put("rating", s.getRating());
        m.put("finishedAt", s.getFinishedAt());
        return m;
    }
}

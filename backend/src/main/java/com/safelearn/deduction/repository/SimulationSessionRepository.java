package com.safelearn.deduction.repository;

import com.safelearn.deduction.entity.SimulationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SimulationSessionRepository extends JpaRepository<SimulationSession, String> {
    List<SimulationSession> findByUserIdOrderByStartedAtDesc(String userId);
    long countByUserId(String userId);
    long countByScenarioId(String scenarioId);
    long countByUserIdAndStatus(String userId, String status);

    @Query("SELECT AVG(s.totalScore) FROM SimulationSession s WHERE s.userId = ?1 AND s.totalScore IS NOT NULL")
    Double avgTotalScoreByUserId(String userId);

    List<SimulationSession> findByStatus(String status);
}

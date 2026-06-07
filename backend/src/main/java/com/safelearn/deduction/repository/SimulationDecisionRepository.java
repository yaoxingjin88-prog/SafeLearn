package com.safelearn.deduction.repository;

import com.safelearn.deduction.entity.SimulationDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationDecisionRepository extends JpaRepository<SimulationDecision, String> {
    List<SimulationDecision> findBySessionIdOrderByElapsedMsAsc(String sessionId);
}

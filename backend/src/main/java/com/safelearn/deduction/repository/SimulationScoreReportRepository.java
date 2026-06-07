package com.safelearn.deduction.repository;

import com.safelearn.deduction.entity.SimulationScoreReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SimulationScoreReportRepository extends JpaRepository<SimulationScoreReport, String> {
    Optional<SimulationScoreReport> findBySessionId(String sessionId);
}

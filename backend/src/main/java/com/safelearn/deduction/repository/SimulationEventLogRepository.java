package com.safelearn.deduction.repository;

import com.safelearn.deduction.entity.SimulationEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationEventLogRepository extends JpaRepository<SimulationEventLog, Long> {
    List<SimulationEventLog> findBySessionIdOrderBySeqAsc(String sessionId);
    boolean existsBySessionIdAndSeq(String sessionId, int seq);
}

package com.safelearn.deduction.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "simulation_sessions")
public class SimulationSession {
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "scenario_id", nullable = false, columnDefinition = "CHAR(36)")
    private String scenarioId;

    @Column(nullable = false, length = 20)
    private String status = "running";

    @Column(length = 20)
    private String outcome;

    @Column(length = 30)
    private String branch = "none";

    @Column(name = "elapsed_ms")
    private Long elapsedMs = 0L;

    @Column(name = "max_temperature", precision = 8, scale = 2)
    private BigDecimal maxTemperature = BigDecimal.ZERO;

    @Column(name = "rule_score")
    private Integer ruleScore = 0;

    @Column(name = "ai_score")
    private Integer aiScore;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(length = 20)
    private String rating;

    @Column(name = "machine_state", length = 50)
    private String machineState = "idle";

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

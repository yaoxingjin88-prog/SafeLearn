package com.safelearn.timeline.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "td_sessions")
public class TdSession {
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (startedAt == null) startedAt = LocalDateTime.now();
    }

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "scenario_id", nullable = false, columnDefinition = "CHAR(36)")
    private String scenarioId;

    @Column(nullable = false)
    private String status = "running";

    @Column(name = "current_phase")
    private String currentPhase;

    @Column(name = "current_node_key")
    private String currentNodeKey;

    @Column(name = "branch_path")
    private String branchPath = "standard";

    private String outcome;

    @Column(name = "risk_index")
    private Integer riskIndex = 0;

    @Column(name = "engine_state", columnDefinition = "JSON")
    private String engineState;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}

package com.safelearn.deduction.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "simulation_score_reports")
public class SimulationScoreReport {
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(name = "session_id", nullable = false, unique = true, columnDefinition = "CHAR(36)")
    private String sessionId;

    @Column(name = "rule_score", nullable = false)
    private Integer ruleScore = 0;

    @Column(name = "ai_score")
    private Integer aiScore;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 0;

    @Column(nullable = false, length = 20)
    private String rating;

    @Column(columnDefinition = "JSON", nullable = false)
    private String dimensions;

    @Column(columnDefinition = "JSON")
    private String highlights;

    @Column(columnDefinition = "JSON")
    private String improvements;

    @Column(name = "instructor_summary", columnDefinition = "TEXT")
    private String instructorSummary;

    @Column(name = "raw_ai_response", columnDefinition = "TEXT")
    private String rawAiResponse;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

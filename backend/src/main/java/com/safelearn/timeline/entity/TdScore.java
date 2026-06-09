package com.safelearn.timeline.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "td_scores")
public class TdScore {
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @Column(name = "session_id", nullable = false, unique = true, columnDefinition = "CHAR(36)")
    private String sessionId;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Column(name = "risk_identification")
    private Integer riskIdentification;

    @Column(name = "decision_making")
    private Integer decisionMaking;

    @Column(name = "emergency_response")
    private Integer emergencyResponse;

    @Column(name = "accident_analysis")
    private Integer accidentAnalysis;

    @Column(name = "ai_comment", columnDefinition = "TEXT")
    private String aiComment;

    @Column(name = "strengths_json", columnDefinition = "JSON")
    private String strengthsJson;

    @Column(name = "weaknesses_json", columnDefinition = "JSON")
    private String weaknessesJson;

    @Column(name = "recommendations_json", columnDefinition = "JSON")
    private String recommendationsJson;

    @Column(name = "recommended_courses_json", columnDefinition = "JSON")
    private String recommendedCoursesJson;

    @Column(name = "fishbone_json", columnDefinition = "JSON")
    private String fishboneJson;

    @Column(name = "five_why_json", columnDefinition = "JSON")
    private String fiveWhyJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

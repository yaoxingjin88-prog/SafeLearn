package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "scenarios")
public class Scenario {
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "initial_conditions", columnDefinition = "JSON")
    private String initialConditions;

    @Column(columnDefinition = "JSON")
    private String events;

    @Column(name = "decision_points", columnDefinition = "JSON")
    private String decisionPoints;

    @Column(nullable = false)
    private Integer duration;

    @Column(nullable = false)
    private Integer difficulty = 1;

    @Column(name = "prerequisite_ids", columnDefinition = "JSON")
    private String prerequisiteIds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

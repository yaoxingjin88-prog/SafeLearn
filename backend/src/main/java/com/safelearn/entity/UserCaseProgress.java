package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "user_case_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "case_id"}))
public class UserCaseProgress {
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.lastAccessAt == null) this.lastAccessAt = LocalDateTime.now();
    }

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "case_id", nullable = false, columnDefinition = "CHAR(36)")
    private String caseId;

    @Column(name = "current_step", nullable = false)
    private Integer currentStep = 0;

    @Column(name = "total_steps", nullable = false)
    private Integer totalSteps = 0;

    @Column(nullable = false)
    private Boolean completed = false;

    /** 各步骤反思作答 JSON：{"stepKey":"answer"} */
    @Column(columnDefinition = "JSON")
    private String reflections;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @UpdateTimestamp
    @Column(name = "last_access_at")
    private LocalDateTime lastAccessAt;
}

package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "exam_paper_attempts")
public class ExamPaperAttempt {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "paper_id", nullable = false, columnDefinition = "CHAR(36)")
    private String paperId;

    @Column(columnDefinition = "JSON", nullable = false)
    private String questions;

    @Column(columnDefinition = "JSON")
    private String answers;

    private Integer score = 0;

    private Boolean passed = false;

    @Column(name = "pass_score")
    private Integer passScore = 60;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}

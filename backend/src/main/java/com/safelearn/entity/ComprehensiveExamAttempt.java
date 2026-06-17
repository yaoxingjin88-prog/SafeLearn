package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "comprehensive_exam_attempts")
public class ComprehensiveExamAttempt {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "course_id", nullable = false, length = 36)
    private String courseId;

    @Column(columnDefinition = "JSON", nullable = false)
    private String questions;

    @Column(columnDefinition = "JSON")
    private String answers;

    private Integer score = 0;

    private Boolean passed = false;

    @Column(name = "pass_score")
    private Integer passScore = 70;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}

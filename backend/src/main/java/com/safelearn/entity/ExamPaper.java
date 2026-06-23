package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "exam_papers")
public class ExamPaper {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(nullable = false, length = 200)
    private String title;

    /** smart / manual / import */
    @Column(nullable = false, length = 20)
    private String mode = "smart";

    /** formal / mock */
    @Column(name = "exam_type", length = 20)
    private String examType = "formal";

    /** draft / published / pending / ended */
    @Column(length = 20)
    private String status = "draft";

    @Column(name = "time_limit")
    private Integer timeLimit = 60;

    @Column(name = "total_score")
    private Integer totalScore = 100;

    @Column(name = "pass_score")
    private Integer passScore = 60;

    @Column(columnDefinition = "JSON")
    private String config;

    @Column(name = "question_ids", columnDefinition = "JSON")
    private String questionIds;

    @Column(name = "questions_snapshot", columnDefinition = "JSON")
    private String questionsSnapshot;

    @Column(length = 100)
    private String department = "全员";

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

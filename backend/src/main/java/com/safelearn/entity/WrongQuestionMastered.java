package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "wrong_question_mastered",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "chapter_id", "question_id"}))
public class WrongQuestionMastered {
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "chapter_id", nullable = false, columnDefinition = "CHAR(36)")
    private String chapterId;

    @Column(name = "question_id", nullable = false, columnDefinition = "CHAR(36)")
    private String questionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

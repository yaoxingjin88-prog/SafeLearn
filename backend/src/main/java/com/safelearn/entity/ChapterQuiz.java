package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chapter_quizzes")
public class ChapterQuiz {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "chapter_id", nullable = false, length = 36)
    private String chapterId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "JSON", nullable = false)
    private String questions;

    @Column(name = "pass_score")
    private Integer passScore = 60;

    @Column(name = "time_limit")
    private Integer timeLimit;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

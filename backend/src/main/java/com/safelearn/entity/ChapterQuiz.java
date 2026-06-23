package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    /** mock=模拟考试, formal=正式考试 */
    @Column(name = "exam_type", length = 20)
    private String examType = "mock";

    /** draft/published/pending/ended */
    @Column(length = 20)
    private String status = "published";

    @Column(name = "total_score")
    private Integer totalScore = 100;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

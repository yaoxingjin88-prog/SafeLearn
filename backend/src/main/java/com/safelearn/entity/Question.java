package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(name = "category_id", nullable = false, columnDefinition = "CHAR(36)")
    private String categoryId;

    /** single / multiple / truefalse / short / case */
    @Column(nullable = false, length = 20)
    private String type;

    /** easy / medium / hard */
    @Column(nullable = false, length = 20)
    private String difficulty = "medium";

    /** draft / published / disabled */
    @Column(nullable = false, length = 20)
    private String status = "published";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "JSON")
    private String options;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(columnDefinition = "JSON")
    private String tags;

    /** 附件：材料文件、图片、关联规范等 */
    @Column(columnDefinition = "JSON")
    private String attachments;

    /** 设置项：评论、举报、考试中显示解析等 */
    @Column(columnDefinition = "JSON")
    private String settings;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @Column(name = "source_quiz_id", length = 36)
    private String sourceQuizId;

    @Column(name = "source_question_key", length = 80)
    private String sourceQuestionKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

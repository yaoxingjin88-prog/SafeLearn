package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 章节依赖关系实体
 * 用于实现网状解锁逻辑：完成前置章节并达到指定分数后才能解锁目标章节
 */
@Data
@Entity
@Table(name = "chapter_dependencies")
public class ChapterDependency {
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    /**
     * 目标章节（需要解锁的章节）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_chapter_id", nullable = false)
    private Chapter targetChapter;

    @Column(name = "target_chapter_id", insertable = false, updatable = false)
    private String targetChapterId;

    /**
     * 前置章节（必须先完成的章节）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_chapter_id", nullable = false)
    private Chapter prerequisiteChapter;

    @Column(name = "prerequisite_chapter_id", insertable = false, updatable = false)
    private String prerequisiteChapterId;

    /**
     * 要求的最低分数（百分制）
     * 默认80分，即必须达到80%的正确率才算合格
     */
    @Column(name = "required_score", nullable = false)
    private Integer requiredScore = 80;

    /**
     * 是否要求100%进度
     */
    @Column(name = "require_full_progress", nullable = false)
    private Boolean requireFullProgress = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

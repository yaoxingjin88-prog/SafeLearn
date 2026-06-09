package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "user_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "chapter_id"}))
public class UserProgress {
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false)
    private Integer progress = 0;

    @Column(nullable = false)
    private Boolean completed = false;

    @Column(name = "mastery_level")
    private Integer masteryLevel = 0;

    @Column(name = "last_access_at")
    @UpdateTimestamp
    private LocalDateTime lastAccessAt;
}

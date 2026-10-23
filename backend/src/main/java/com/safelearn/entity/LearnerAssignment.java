package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/** 管理员指派给学员的学习/训练任务。 */
@Data
@Entity
@Table(name = "learner_assignments")
public class LearnerAssignment {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(nullable = false, length = 200)
    private String title;

    /** course | training | exam | custom */
    @Column(nullable = false, length = 20)
    private String type = "course";

    @Column(length = 500)
    private String link;

    @Column(length = 100)
    private String deadline;

    /** pending | in_progress | completed */
    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "action_text", length = 50)
    private String actionText = "去完成";

    @Column(name = "created_by", columnDefinition = "CHAR(36)")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

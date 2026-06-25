package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/** 管理员手动录入的培训预警。 */
@Data
@Entity
@Table(name = "admin_training_alerts")
public class AdminTrainingAlert {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(name = "alert_no", nullable = false, length = 32, unique = true)
    private String alertNo;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 16)
    private String level;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 80)
    private String department;

    @Column(name = "responsible_person", length = 80)
    private String responsiblePerson;

    @Column(name = "trainee_name", length = 80)
    private String traineeName;

    @Column(name = "course_name", length = 200)
    private String courseName;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "action_path", length = 500)
    private String actionPath;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

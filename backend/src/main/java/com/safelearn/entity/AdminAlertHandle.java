package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/** 计算预警与手动预警的处理状态覆盖。 */
@Data
@Entity
@Table(name = "admin_alert_handles")
public class AdminAlertHandle {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(name = "alert_key", nullable = false, unique = true, length = 120)
    private String alertKey;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "responsible_person", length = 80)
    private String responsiblePerson;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

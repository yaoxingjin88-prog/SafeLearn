package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "admin_roles")
public class AdminRole {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    /** system | custom */
    @Column(name = "role_type", nullable = false, length = 16)
    private String roleType = "custom";

    @Column(columnDefinition = "TEXT")
    private String description;

    /** all | department | dept_tree | self | custom */
    @Column(name = "data_scope", nullable = false, length = 32)
    private String dataScope = "all";

    /** 自定义数据范围时的部门 ID 列表（JSON 数组） */
    @Column(name = "custom_dept_ids", columnDefinition = "TEXT")
    private String customDeptIds;

    /** 功能权限矩阵 JSON：moduleCode -> { view, create, edit, delete, export, approve } */
    @Column(columnDefinition = "TEXT")
    private String permissions;

    /** draft | published */
    @Column(nullable = false, length = 16)
    private String status = "published";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 系统配置项：管理端可运行时修改、用户端读取生效的 key-value 配置。
 * 每行一个配置项，value 统一以字符串存储（列表/对象存 JSON 文本）。
 */
@Data
@Entity
@Table(name = "system_config")
public class SystemConfig {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    /** 配置键，点分命名，如 ai.enabled。 */
    @Column(name = "config_key", nullable = false, unique = true, length = 120)
    private String configKey;

    /** 配置值，统一字符串存储。 */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    /** 值类型：BOOLEAN / INT / STRING / TEXT / JSON_LIST / JSON。 */
    @Column(name = "value_type", nullable = false, length = 20)
    private String valueType;

    /** 分组：ai / learning / scenario / dashboard。 */
    @Column(nullable = false, length = 40)
    private String category;

    /** 中文显示名。 */
    @Column(length = 100)
    private String label;

    /** 配置说明。 */
    @Column(length = 255)
    private String description;

    /** 是否允许用户端读取（公开配置接口下发）。 */
    @Column(name = "is_public", nullable = false, columnDefinition = "TINYINT(1) NOT NULL DEFAULT 0")
    private Boolean isPublic = false;

    /** 是否敏感（读取时掩码，绝不进公开接口）。 */
    @Column(name = "is_sensitive", nullable = false, columnDefinition = "TINYINT(1) NOT NULL DEFAULT 0")
    private Boolean isSensitive = false;

    /** 是否允许管理端在线编辑。 */
    @Column(nullable = false, columnDefinition = "TINYINT(1) NOT NULL DEFAULT 1")
    private Boolean editable = true;

    /** 同组内排序。 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

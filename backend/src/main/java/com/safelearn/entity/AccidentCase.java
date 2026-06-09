package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "accident_cases")
public class AccidentCase {
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String location;

    private LocalDate date;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "JSON")
    private String timeline;

    @Column(name = "cause_analysis", columnDefinition = "TEXT")
    private String causeAnalysis;

    @Column(name = "loss_estimate", columnDefinition = "TEXT")
    private String lossEstimate;

    @Column(name = "lessons_learned", columnDefinition = "TEXT")
    private String lessonsLearned;

    // ===== 结构化字段（用于学习页可视化展示，均可空，向后兼容） =====

    /** 直接原因 */
    @Column(name = "direct_cause", columnDefinition = "TEXT")
    private String directCause;

    /** 间接原因 */
    @Column(name = "indirect_cause", columnDefinition = "TEXT")
    private String indirectCause;

    /** 根本原因 */
    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    /** 责任方 */
    @Column(name = "responsible_party", length = 200)
    private String responsibleParty;

    /** 伤亡情况，如"无人员伤亡" */
    @Column(length = 100)
    private String casualties;

    /** 直接经济损失（万元），用于大数字展示 */
    @Column(name = "loss_amount")
    private Integer lossAmount;

    /** 损失明细 JSON：[{"label":"设备损毁","amount":150}, ...] */
    @Column(name = "loss_breakdown", columnDefinition = "JSON")
    private String lossBreakdown;

    /** 结构化经验教训 JSON：["...","..."] */
    @Column(columnDefinition = "JSON")
    private String lessons;

    /** 案例难度：basic / intermediate / advanced */
    @Column(length = 20)
    private String difficulty;

    /** 建议学习时长（分钟） */
    @Column(name = "study_minutes")
    private Integer studyMinutes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

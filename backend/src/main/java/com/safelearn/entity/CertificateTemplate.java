package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "certificate_templates")
public class CertificateTemplate {

    @Id
    @Column(length = 32)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "badge_label", nullable = false, length = 50)
    private String badgeLabel;

    @Column(name = "title_suffix", nullable = false, length = 100)
    private String titleSuffix;

    @Column(name = "validity_months", nullable = false)
    private Integer validityMonths = 24;

    /** 到期前多少天可提前续证 */
    @Column(name = "early_renew_days", nullable = false)
    private Integer earlyRenewDays = 30;

    @Column(name = "border_color", length = 20)
    private String borderColor = "#c9a227";

    @Column(name = "accent_color", length = 20)
    private String accentColor = "#1e3a8a";

    @Column(name = "header_title", length = 50)
    private String headerTitle = "结业证书";

    @Column(name = "body_template", columnDefinition = "TEXT")
    private String bodyTemplate;

    /** comprehensive_exam | course_recomplete */
    @Column(name = "renewal_requirement", length = 30)
    private String renewalRequirement = "comprehensive_exam";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

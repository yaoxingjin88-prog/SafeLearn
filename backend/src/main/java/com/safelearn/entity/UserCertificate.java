package com.safelearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "user_certificates", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
public class UserCertificate {
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "course_id", nullable = false, columnDefinition = "CHAR(36)")
    private String courseId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "certificate_no", nullable = false, unique = true, length = 50)
    private String certificateNo;

    @Column(name = "cert_level", nullable = false, length = 20)
    private String certLevel = "advanced";

    @Column(name = "template_code", length = 32)
    private String templateCode = "advanced";

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** active | expired | revoked */
    @Column(length = 20)
    private String status = "active";

    @Column(name = "renewal_count")
    private Integer renewalCount = 0;

    @Column(name = "last_renewed_at")
    private LocalDateTime lastRenewedAt;

    /** course_complete | comprehensive_exam | renewal */
    @Column(name = "issue_source", length = 30)
    private String issueSource = "course_complete";

    @CreationTimestamp
    @Column(name = "issued_at", updatable = false)
    private LocalDateTime issuedAt;
}

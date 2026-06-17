package com.safelearn.service;

import com.safelearn.entity.*;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private static final int ADVANCED_LEVEL = 3;
    private static final Map<String, Integer> TEMPLATE_RANK = Map.of(
            "basic", 1,
            "advanced", 2,
            "professional", 3
    );

    private final UserCertificateRepository certRepo;
    private final CertificateTemplateRepository templateRepo;
    private final ChapterRepository chapterRepo;
    private final CourseRepository courseRepo;
    private final UserProgressRepository progressRepo;
    private final UserRepository userRepo;
    private final ComprehensiveExamAttemptRepository comprehensiveAttemptRepo;

    @Transactional
    public Optional<Map<String, Object>> tryIssueOnCourseComplete(String userId, String courseId) {
        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
        if (chapters.isEmpty()) return Optional.empty();

        boolean allDone = chapters.stream()
                .allMatch(ch -> progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId()));
        if (!allDone) return Optional.empty();

        boolean hasAdvanced = chapters.stream()
                .anyMatch(ch -> ch.getDifficultyLevel() != null && ch.getDifficultyLevel() >= ADVANCED_LEVEL);
        String templateCode = hasAdvanced ? "advanced" : "basic";

        Optional<UserCertificate> existing = certRepo.findByUserIdAndCourseId(userId, courseId);
        if (existing.isPresent()) {
            UserCertificate cert = existing.get();
            if (templateRank(templateCode) <= templateRank(cert.getTemplateCode())) {
                return Optional.empty();
            }
            return Optional.of(upgradeCertificate(cert, templateCode, "course_complete"));
        }

        Course course = courseRepo.findById(courseId).orElseThrow(() -> new RuntimeException("课程不存在"));
        UserCertificate cert = issueNew(userId, course, templateCode, "course_complete");
        return Optional.of(toDetailMap(cert, userRepo.findById(userId).orElse(null), course));
    }

    @Transactional
    public Optional<Map<String, Object>> tryIssueOnComprehensiveExamPass(String userId, String courseId, int score) {
        if (score < 60) return Optional.empty();

        Course course = courseRepo.findById(courseId).orElse(null);
        if (course == null) return Optional.empty();

        Optional<UserCertificate> existing = certRepo.findByUserIdAndCourseId(userId, courseId);
        if (existing.isPresent()) {
            UserCertificate cert = existing.get();
            if ("professional".equals(cert.getTemplateCode())) {
                return Optional.empty();
            }
            return Optional.of(upgradeCertificate(cert, "professional", "comprehensive_exam"));
        }

        UserCertificate cert = issueNew(userId, course, "professional", "comprehensive_exam");
        return Optional.of(toDetailMap(cert, userRepo.findById(userId).orElse(null), course));
    }

    public List<Map<String, Object>> listTemplates() {
        return templateRepo.findAll().stream()
                .sorted(Comparator.comparing(CertificateTemplate::getCode))
                .map(this::toTemplateMap)
                .toList();
    }

    public List<Map<String, Object>> listMine(String userId) {
        refreshExpiredStatuses(userId);
        return certRepo.findByUserIdOrderByIssuedAtDesc(userId).stream()
                .map(c -> {
                    Course course = courseRepo.findById(c.getCourseId()).orElse(null);
                    User user = userRepo.findById(userId).orElse(null);
                    return toDetailMap(c, user, course);
                }).toList();
    }

    public Map<String, Object> getById(String userId, String certId) {
        UserCertificate cert = certRepo.findById(certId)
                .orElseThrow(() -> new RuntimeException("证书不存在"));
        if (!cert.getUserId().equals(userId)) throw new RuntimeException("无权查看该证书");
        refreshStatus(cert);
        Course course = courseRepo.findById(cert.getCourseId()).orElse(null);
        User user = userRepo.findById(userId).orElse(null);
        Map<String, Object> detail = toDetailMap(cert, user, course);
        detail.put("renewal", getRenewalStatus(userId, cert));
        return detail;
    }

    public Map<String, Object> getRenewalStatus(String userId, String certId) {
        UserCertificate cert = certRepo.findById(certId)
                .orElseThrow(() -> new RuntimeException("证书不存在"));
        if (!cert.getUserId().equals(userId)) throw new RuntimeException("无权查看该证书");
        refreshStatus(cert);
        return getRenewalStatus(userId, cert);
    }

    @Transactional
    public Map<String, Object> renewCertificate(String userId, String certId) {
        UserCertificate cert = certRepo.findById(certId)
                .orElseThrow(() -> new RuntimeException("证书不存在"));
        if (!cert.getUserId().equals(userId)) throw new RuntimeException("无权操作该证书");

        refreshStatus(cert);
        Map<String, Object> renewal = getRenewalStatus(userId, cert);
        if (!Boolean.TRUE.equals(renewal.get("eligible"))) {
            throw new RuntimeException((String) renewal.getOrDefault("reason", "当前不符合续证条件"));
        }

        CertificateTemplate template = getTemplate(cert.getTemplateCode());
        LocalDateTime base = LocalDateTime.now();
        cert.setExpiresAt(base.plusMonths(template.getValidityMonths()));
        cert.setStatus("active");
        cert.setRenewalCount((cert.getRenewalCount() != null ? cert.getRenewalCount() : 0) + 1);
        cert.setLastRenewedAt(base);
        cert.setIssueSource("renewal");
        certRepo.save(cert);

        Course course = courseRepo.findById(cert.getCourseId()).orElse(null);
        User user = userRepo.findById(userId).orElse(null);
        Map<String, Object> result = toDetailMap(cert, user, course);
        result.put("renewed", true);
        result.put("message", "续证成功，有效期已延长至 " + formatDate(cert.getExpiresAt()));
        return result;
    }

    // ========== 内部方法 ==========

    private UserCertificate issueNew(String userId, Course course, String templateCode, String issueSource) {
        CertificateTemplate template = getTemplate(templateCode);
        LocalDateTime now = LocalDateTime.now();

        UserCertificate cert = new UserCertificate();
        cert.setUserId(userId);
        cert.setCourseId(course.getId());
        cert.setTitle(course.getTitle() + template.getTitleSuffix());
        cert.setCertLevel(templateCode);
        cert.setTemplateCode(templateCode);
        cert.setCertificateNo(generateCertNo());
        cert.setIssuedAt(now);
        cert.setExpiresAt(now.plusMonths(template.getValidityMonths()));
        cert.setStatus("active");
        cert.setRenewalCount(0);
        cert.setIssueSource(issueSource);
        return certRepo.save(cert);
    }

    private Map<String, Object> upgradeCertificate(UserCertificate cert, String templateCode, String issueSource) {
        CertificateTemplate template = getTemplate(templateCode);
        LocalDateTime now = LocalDateTime.now();
        Course course = courseRepo.findById(cert.getCourseId()).orElse(null);

        cert.setTemplateCode(templateCode);
        cert.setCertLevel(templateCode);
        cert.setTitle((course != null ? course.getTitle() : cert.getTitle()) + template.getTitleSuffix());
        cert.setExpiresAt(now.plusMonths(template.getValidityMonths()));
        cert.setStatus("active");
        cert.setIssueSource(issueSource);
        certRepo.save(cert);

        return toDetailMap(cert, userRepo.findById(cert.getUserId()).orElse(null), course);
    }

    private Map<String, Object> getRenewalStatus(String userId, UserCertificate cert) {
        CertificateTemplate template = getTemplate(cert.getTemplateCode());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("certId", cert.getId());
        m.put("status", cert.getStatus());
        m.put("expiresAt", cert.getExpiresAt() != null ? cert.getExpiresAt().toString() : null);
        m.put("renewalRequirement", template.getRenewalRequirement());
        m.put("earlyRenewDays", template.getEarlyRenewDays());

        if (cert.getExpiresAt() == null) {
            m.put("eligible", false);
            m.put("reason", "证书缺少有效期信息");
            return m;
        }

        long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), cert.getExpiresAt().toLocalDate());
        m.put("daysUntilExpiry", daysUntilExpiry);
        m.put("expiringSoon", daysUntilExpiry >= 0 && daysUntilExpiry <= template.getEarlyRenewDays());

        boolean inRenewWindow = "expired".equals(cert.getStatus())
                || (daysUntilExpiry >= 0 && daysUntilExpiry <= template.getEarlyRenewDays());
        if (!inRenewWindow) {
            m.put("eligible", false);
            m.put("reason", daysUntilExpiry > template.getEarlyRenewDays()
                    ? "距到期还有 " + daysUntilExpiry + " 天，可在到期前 " + template.getEarlyRenewDays() + " 天内续证"
                    : "证书仍在有效期内，暂无需续证");
            return m;
        }

        LocalDateTime since = cert.getLastRenewedAt() != null ? cert.getLastRenewedAt() : cert.getIssuedAt();
        boolean requirementMet = meetsRenewalRequirement(userId, cert.getCourseId(), template.getRenewalRequirement(), since);
        m.put("requirementMet", requirementMet);
        m.put("eligible", requirementMet);
        if (!requirementMet) {
            m.put("reason", "comprehensive_exam".equals(template.getRenewalRequirement())
                    ? "请先通过该课程的综合考试后再续证"
                    : "请先重新完成该课程全部章节后再续证");
        } else {
            m.put("reason", "已满足续证条件，可一键续证");
        }
        return m;
    }

    private boolean meetsRenewalRequirement(String userId, String courseId, String requirement, LocalDateTime since) {
        if ("course_recomplete".equals(requirement)) {
            List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
            return chapters.stream()
                    .allMatch(ch -> progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId()));
        }
        return comprehensiveAttemptRepo.existsByUserIdAndCourseIdAndPassedTrueAndCompletedAtAfter(
                userId, courseId, since != null ? since : LocalDateTime.MIN);
    }

    private void refreshExpiredStatuses(String userId) {
        certRepo.findByUserIdOrderByIssuedAtDesc(userId).forEach(this::refreshStatus);
    }

    private void refreshStatus(UserCertificate cert) {
        backfillLegacyFields(cert);
        if (cert.getExpiresAt() == null || "revoked".equals(cert.getStatus())) return;
        if (LocalDateTime.now().isAfter(cert.getExpiresAt())) {
            if (!"expired".equals(cert.getStatus())) {
                cert.setStatus("expired");
                certRepo.save(cert);
            }
        } else if ("expired".equals(cert.getStatus())) {
            cert.setStatus("active");
            certRepo.save(cert);
        }
    }

    private CertificateTemplate getTemplate(String code) {
        return templateRepo.findById(code != null ? code : "advanced")
                .orElseGet(() -> templateRepo.findById("advanced")
                        .orElseThrow(() -> new RuntimeException("证书模板未配置")));
    }

    private int templateRank(String code) {
        return TEMPLATE_RANK.getOrDefault(code, 0);
    }

    private void backfillLegacyFields(UserCertificate cert) {
        boolean changed = false;
        if (cert.getTemplateCode() == null || cert.getTemplateCode().isBlank()) {
            cert.setTemplateCode(cert.getCertLevel() != null ? cert.getCertLevel() : "advanced");
            changed = true;
        }
        if (cert.getExpiresAt() == null && cert.getIssuedAt() != null) {
            CertificateTemplate template = getTemplate(cert.getTemplateCode());
            cert.setExpiresAt(cert.getIssuedAt().plusMonths(template.getValidityMonths()));
            changed = true;
        }
        if (cert.getStatus() == null || cert.getStatus().isBlank()) {
            cert.setStatus("active");
            changed = true;
        }
        if (cert.getRenewalCount() == null) {
            cert.setRenewalCount(0);
            changed = true;
        }
        if (changed) {
            certRepo.save(cert);
        }
    }

    private String generateCertNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = String.format("%06d", new Random().nextInt(999999));
        return "SL-" + date + "-" + suffix;
    }

    private Map<String, Object> toTemplateMap(CertificateTemplate t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", t.getCode());
        m.put("name", t.getName());
        m.put("badgeLabel", t.getBadgeLabel());
        m.put("titleSuffix", t.getTitleSuffix());
        m.put("validityMonths", t.getValidityMonths());
        m.put("earlyRenewDays", t.getEarlyRenewDays());
        m.put("borderColor", t.getBorderColor());
        m.put("accentColor", t.getAccentColor());
        m.put("headerTitle", t.getHeaderTitle());
        m.put("bodyTemplate", t.getBodyTemplate());
        m.put("renewalRequirement", t.getRenewalRequirement());
        return m;
    }

    private Map<String, Object> toDetailMap(UserCertificate c, User user, Course course) {
        CertificateTemplate template = getTemplate(c.getTemplateCode());
        refreshStatus(c);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("courseId", c.getCourseId());
        m.put("courseTitle", course != null ? course.getTitle() : "");
        m.put("title", c.getTitle());
        m.put("certificateNo", c.getCertificateNo());
        m.put("certLevel", c.getCertLevel());
        m.put("templateCode", c.getTemplateCode());
        m.put("template", toTemplateMap(template));
        m.put("issuedAt", c.getIssuedAt() != null ? c.getIssuedAt().toString() : null);
        m.put("expiresAt", c.getExpiresAt() != null ? c.getExpiresAt().toString() : null);
        m.put("status", c.getStatus());
        m.put("renewalCount", c.getRenewalCount() != null ? c.getRenewalCount() : 0);
        m.put("lastRenewedAt", c.getLastRenewedAt() != null ? c.getLastRenewedAt().toString() : null);
        m.put("issueSource", c.getIssueSource());
        m.put("userName", resolveDisplayName(user));
        m.put("company", user != null ? user.getCompany() : "");
        m.put("bodyText", renderBody(template, user, course, c));
        m.put("daysUntilExpiry", c.getExpiresAt() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), c.getExpiresAt().toLocalDate()) : null);
        m.put("renewal", getRenewalStatus(c.getUserId(), c));
        return m;
    }

    private String renderBody(CertificateTemplate template, User user, Course course, UserCertificate cert) {
        String body = template.getBodyTemplate() != null ? template.getBodyTemplate() : "";
        String courseTitle = course != null ? course.getTitle() : "";
        String validUntil = cert.getExpiresAt() != null ? formatDate(cert.getExpiresAt()) : "长期有效";
        return body
                .replace("{userName}", resolveDisplayName(user))
                .replace("{courseTitle}", courseTitle)
                .replace("{validUntil}", validUntil);
    }

    private String formatDate(LocalDateTime dt) {
        return dt.toLocalDate().toString();
    }

    private String resolveDisplayName(User user) {
        if (user == null) return "学员";
        if (user.getUsername() == null) return "学员";
        return switch (user.getUsername()) {
            case "zhanggong" -> "张工";
            case "lisi" -> "李工";
            case "wangwu" -> "王工";
            default -> user.getUsername();
        };
    }
}

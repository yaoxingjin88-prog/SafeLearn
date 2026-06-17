package com.safelearn.config;

import com.safelearn.entity.CertificateTemplate;
import com.safelearn.repository.CertificateTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateTemplateInitializer implements ApplicationRunner {

    private final CertificateTemplateRepository templateRepo;

    @Override
    public void run(ApplicationArguments args) {
        if (templateRepo.count() > 0) return;

        templateRepo.save(build(
                "basic",
                "基础结业",
                "基础结业",
                " · 基础结业证书",
                12,
                30,
                "#94a3b8",
                "#475569",
                "基础结业证书",
                "已完成 {courseTitle} 全部课程学习，达到基础结业标准，特发此证。证书有效期至 {validUntil}。",
                "course_recomplete"
        ));
        templateRepo.save(build(
                "advanced",
                "高级结业",
                "高级结业",
                " · 高级结业证书",
                24,
                30,
                "#c9a227",
                "#1e3a8a",
                "结业证书",
                "已完成 {courseTitle} 全部高级课程学习，经考核达到结业标准，特发此证。证书有效期至 {validUntil}。",
                "comprehensive_exam"
        ));
        templateRepo.save(build(
                "professional",
                "专业认证",
                "专业认证",
                " · 专业认证证书",
                36,
                60,
                "#b45309",
                "#7c2d12",
                "专业认证证书",
                "已通过 {courseTitle} 综合考试，具备跨章节综合应用能力，特发此证。证书有效期至 {validUntil}。",
                "comprehensive_exam"
        ));
        log.info("Initialized default certificate templates");
    }

    private CertificateTemplate build(
            String code, String name, String badge, String suffix,
            int months, int earlyDays, String border, String accent,
            String header, String body, String renewalReq) {
        CertificateTemplate t = new CertificateTemplate();
        t.setCode(code);
        t.setName(name);
        t.setBadgeLabel(badge);
        t.setTitleSuffix(suffix);
        t.setValidityMonths(months);
        t.setEarlyRenewDays(earlyDays);
        t.setBorderColor(border);
        t.setAccentColor(accent);
        t.setHeaderTitle(header);
        t.setBodyTemplate(body);
        t.setRenewalRequirement(renewalReq);
        return t;
    }
}

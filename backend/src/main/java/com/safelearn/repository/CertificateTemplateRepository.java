package com.safelearn.repository;

import com.safelearn.entity.CertificateTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, String> {
}

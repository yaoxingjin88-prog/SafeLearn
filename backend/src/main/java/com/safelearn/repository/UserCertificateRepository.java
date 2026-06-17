package com.safelearn.repository;

import com.safelearn.entity.UserCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCertificateRepository extends JpaRepository<UserCertificate, String> {
    List<UserCertificate> findByUserIdOrderByIssuedAtDesc(String userId);

    Optional<UserCertificate> findByUserIdAndCourseId(String userId, String courseId);

    Optional<UserCertificate> findByCertificateNo(String certificateNo);

    long countByTemplateCode(String templateCode);
}

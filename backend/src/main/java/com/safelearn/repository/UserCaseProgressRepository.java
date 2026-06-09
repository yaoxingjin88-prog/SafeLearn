package com.safelearn.repository;

import com.safelearn.entity.UserCaseProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCaseProgressRepository extends JpaRepository<UserCaseProgress, String> {
    Optional<UserCaseProgress> findByUserIdAndCaseId(String userId, String caseId);

    List<UserCaseProgress> findByUserId(String userId);

    long countByCaseIdAndCompletedTrue(String caseId);
}

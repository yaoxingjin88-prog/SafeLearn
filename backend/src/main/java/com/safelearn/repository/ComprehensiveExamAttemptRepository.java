package com.safelearn.repository;

import com.safelearn.entity.ComprehensiveExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComprehensiveExamAttemptRepository extends JpaRepository<ComprehensiveExamAttempt, String> {

    List<ComprehensiveExamAttempt> findByUserIdAndCourseIdOrderByStartedAtDesc(String userId, String courseId);

    Optional<ComprehensiveExamAttempt> findTopByUserIdAndCourseIdAndPassedTrueOrderByScoreDesc(
            String userId, String courseId);

    boolean existsByUserIdAndCourseIdAndPassedTrueAndCompletedAtAfter(
            String userId, String courseId, java.time.LocalDateTime after);
}

package com.safelearn.repository;

import com.safelearn.entity.ComprehensiveExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ComprehensiveExamAttemptRepository extends JpaRepository<ComprehensiveExamAttempt, String> {

    @Query("SELECT c FROM ComprehensiveExamAttempt c WHERE c.user.id = :userId AND c.courseId = :courseId ORDER BY c.startedAt DESC")
    List<ComprehensiveExamAttempt> findByUserIdAndCourseIdOrderByStartedAtDesc(
            @Param("userId") String userId, @Param("courseId") String courseId);

    @Query("SELECT c FROM ComprehensiveExamAttempt c WHERE c.user.id = :userId AND c.courseId = :courseId AND c.passed = true ORDER BY c.score DESC")
    Optional<ComprehensiveExamAttempt> findTopByUserIdAndCourseIdAndPassedTrueOrderByScoreDesc(
            @Param("userId") String userId, @Param("courseId") String courseId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ComprehensiveExamAttempt c "
            + "WHERE c.user.id = :userId AND c.courseId = :courseId AND c.passed = true AND c.completedAt > :after")
    boolean existsByUserIdAndCourseIdAndPassedTrueAndCompletedAtAfter(
            @Param("userId") String userId,
            @Param("courseId") String courseId,
            @Param("after") LocalDateTime after);

    @Query("SELECT COUNT(c) FROM ComprehensiveExamAttempt c WHERE c.user.id = :userId AND c.completedAt IS NULL")
    long countByUserIdAndCompletedAtIsNull(@Param("userId") String userId);

    @Query("SELECT c FROM ComprehensiveExamAttempt c WHERE c.user.id = :userId AND c.courseId = :courseId "
            + "AND c.completedAt IS NULL ORDER BY c.startedAt DESC")
    Optional<ComprehensiveExamAttempt> findTopByUserIdAndCourseIdAndCompletedAtIsNullOrderByStartedAtDesc(
            @Param("userId") String userId, @Param("courseId") String courseId);

    @Query("SELECT c FROM ComprehensiveExamAttempt c WHERE c.user.id = :userId AND c.completedAt IS NOT NULL")
    List<ComprehensiveExamAttempt> findByUserIdAndCompletedAtIsNotNull(@Param("userId") String userId);
}

package com.safelearn.repository;

import com.safelearn.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserProgressRepository extends JpaRepository<UserProgress, String> {
    Optional<UserProgress> findByUserIdAndChapterId(String userId, String chapterId);

    @Query("SELECT up FROM UserProgress up WHERE up.user.id = :userId AND up.course.id = :courseId")
    List<UserProgress> findByUserIdAndCourseId(@Param("userId") String userId, @Param("courseId") String courseId);

    long countByUserIdAndCompletedTrue(String userId);

    @Query("SELECT up FROM UserProgress up WHERE up.user.id = :userId ORDER BY up.lastAccessAt DESC")
    List<UserProgress> findRecentByUserId(@Param("userId") String userId);
}

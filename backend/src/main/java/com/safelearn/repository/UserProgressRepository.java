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

    @Query("SELECT CASE WHEN COUNT(up) > 0 THEN true ELSE false END FROM UserProgress up " +
           "WHERE up.user.id = :userId AND up.chapter.id = :chapterId AND up.completed = true")
    boolean existsByUserIdAndChapterIdAndCompletedTrue(
            @Param("userId") String userId, @Param("chapterId") String chapterId);

    @Query("SELECT CASE WHEN COUNT(up) > 0 THEN true ELSE false END FROM UserProgress up " +
           "WHERE up.user.id = :userId AND up.chapter.id = :chapterId AND up.completed = true " +
           "AND (up.masteryLevel IS NULL OR up.masteryLevel >= :minMastery)")
    boolean existsByUserIdAndChapterIdAndCompletedWithMastery(
            @Param("userId") String userId, @Param("chapterId") String chapterId,
            @Param("minMastery") int minMastery);

    @Query("SELECT up.chapter.id FROM UserProgress up WHERE up.user.id = :userId AND up.completed = true")
    List<String> findCompletedChapterIdsByUserId(@Param("userId") String userId);

    @Query("SELECT up.chapter.id FROM UserProgress up WHERE up.user.id = :userId AND up.completed = true " +
           "AND (up.masteryLevel IS NULL OR up.masteryLevel >= :minMastery)")
    List<String> findQualifiedChapterIdsByUserId(@Param("userId") String userId, @Param("minMastery") int minMastery);

    long countByCompletedTrue();

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.completed = false AND up.progress > 0")
    long countInProgress();
}

package com.safelearn.repository;

import com.safelearn.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserProgressRepository extends JpaRepository<UserProgress, String> {
    Optional<UserProgress> findByUserIdAndChapterId(String userId, String chapterId);

    @Query("SELECT up FROM UserProgress up WHERE up.user.id = :userId AND up.course.id = :courseId")
    List<UserProgress> findByUserIdAndCourseId(@Param("userId") String userId, @Param("courseId") String courseId);

    long countByUserIdAndCompletedTrue(String userId);

    @Query("SELECT up FROM UserProgress up WHERE up.user.id = :userId ORDER BY up.lastAccessAt DESC")
    List<UserProgress> findRecentByUserId(@Param("userId") String userId);

    @Query("SELECT up FROM UserProgress up JOIN FETCH up.chapter JOIN FETCH up.course " +
           "WHERE up.user.id = :userId ORDER BY up.lastAccessAt DESC")
    List<UserProgress> findByUserIdWithDetails(@Param("userId") String userId);

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

    @Query("SELECT COUNT(DISTINCT up.user.id) FROM UserProgress up WHERE up.course.id = :courseId")
    long countDistinctUsersByCourseId(@Param("courseId") String courseId);

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.user.id = :userId AND up.course.id = :courseId AND up.completed = true")
    long countCompletedChaptersByUserIdAndCourseId(@Param("userId") String userId, @Param("courseId") String courseId);

    /** 最近活跃用户去重计数（用于"在线/今日活跃"统计），按 lastAccessAt 截止时间过滤。 */
    @Query("SELECT COUNT(DISTINCT up.user.id) FROM UserProgress up WHERE up.lastAccessAt >= :since")
    long countDistinctActiveUsersSince(@Param("since") LocalDateTime since);

    /** 最近活跃用户 ID 集合，用于按部门拆分在线人数。 */
    @Query("SELECT DISTINCT up.user.id FROM UserProgress up WHERE up.lastAccessAt >= :since")
    List<String> findDistinctActiveUserIdsSince(@Param("since") LocalDateTime since);

    /**
     * 按用户聚合学习进度：返回 [userId, 完成章节数, 平均进度]。
     * 平均进度为该用户所有进度记录 progress 的均值。
     */
    @Query("SELECT up.user.id, " +
           "SUM(CASE WHEN up.completed = true THEN 1 ELSE 0 END), " +
           "AVG(COALESCE(up.progress, 0)) " +
           "FROM UserProgress up GROUP BY up.user.id")
    List<Object[]> aggregateProgressByUser();
}

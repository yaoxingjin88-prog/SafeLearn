package com.safelearn.repository;

import com.safelearn.entity.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseReviewRepository extends JpaRepository<CourseReview, String> {

    List<CourseReview> findByCourseIdOrderByCreatedAtDesc(String courseId);

    Optional<CourseReview> findByCourseIdAndUserId(String courseId, String userId);

    long countByCourseId(String courseId);

    @Query("SELECT AVG(r.rating) FROM CourseReview r WHERE r.courseId = :courseId")
    Double avgRatingByCourseId(@Param("courseId") String courseId);
}

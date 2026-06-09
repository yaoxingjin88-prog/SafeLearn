package com.safelearn.repository;

import com.safelearn.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, String> {
    List<Course> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT c FROM Course c WHERE c.status = :status " +
           "AND (:category IS NULL OR c.category = :category) " +
           "AND (:keyword IS NULL OR c.title LIKE %:keyword% OR c.description LIKE %:keyword%) " +
           "ORDER BY c.createdAt DESC")
    List<Course> findByFilters(@Param("status") String status,
                               @Param("category") String category,
                               @Param("keyword") String keyword);

    long countByCategory(String category);
}

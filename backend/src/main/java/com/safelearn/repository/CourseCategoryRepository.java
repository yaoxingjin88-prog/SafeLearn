package com.safelearn.repository;

import com.safelearn.entity.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, String> {
    List<CourseCategory> findAllByOrderBySortOrderAscNameAsc();

    @Query("SELECT c FROM CourseCategory c WHERE c.enabled = true ORDER BY c.sortOrder ASC, c.name ASC")
    List<CourseCategory> findEnabledOrdered();

    Optional<CourseCategory> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);
}

package com.safelearn.repository;

import com.safelearn.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, String> {
    List<Chapter> findByCourseIdOrderByOrderNumAsc(String courseId);
    Optional<Chapter> findByIdAndCourseId(String id, String courseId);
}

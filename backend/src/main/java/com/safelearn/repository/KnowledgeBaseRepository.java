package com.safelearn.repository;

import com.safelearn.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, String> {
    List<KnowledgeBase> findByCategory(String category);

    @Query("SELECT k FROM KnowledgeBase k WHERE " +
           "LOWER(k.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(k.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(k.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<KnowledgeBase> searchByKeyword(@Param("keyword") String keyword);
}

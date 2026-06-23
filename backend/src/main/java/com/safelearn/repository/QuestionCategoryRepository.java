package com.safelearn.repository;

import com.safelearn.entity.QuestionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionCategoryRepository extends JpaRepository<QuestionCategory, String> {

    List<QuestionCategory> findAllByOrderBySortOrderAscNameAsc();

    List<QuestionCategory> findByParentIdOrderBySortOrderAscNameAsc(String parentId);

    List<QuestionCategory> findByParentIdIsNullOrderBySortOrderAscNameAsc();
}

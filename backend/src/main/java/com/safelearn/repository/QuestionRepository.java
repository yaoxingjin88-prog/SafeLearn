package com.safelearn.repository;

import com.safelearn.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, String> {

    long countByCategoryId(String categoryId);

    long countByCategoryIdIn(Iterable<String> categoryIds);

    List<Question> findByCategoryIdIn(Iterable<String> categoryIds);

    Optional<Question> findBySourceQuestionKey(String sourceQuestionKey);

    List<Question> findBySourceQuizId(String sourceQuizId);
}

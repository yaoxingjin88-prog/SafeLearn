package com.safelearn.repository;

import com.safelearn.entity.WrongQuestionMastered;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WrongQuestionMasteredRepository extends JpaRepository<WrongQuestionMastered, String> {
    List<WrongQuestionMastered> findByUserId(String userId);

    List<WrongQuestionMastered> findByUserIdAndChapterId(String userId, String chapterId);

    boolean existsByUserIdAndChapterIdAndQuestionId(String userId, String chapterId, String questionId);

    void deleteByUserId(String userId);

    void deleteByUserIdAndChapterIdAndQuestionId(String userId, String chapterId, String questionId);
}

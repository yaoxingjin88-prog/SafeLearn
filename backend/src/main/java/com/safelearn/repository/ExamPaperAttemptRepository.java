package com.safelearn.repository;

import com.safelearn.entity.ExamPaperAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamPaperAttemptRepository extends JpaRepository<ExamPaperAttempt, String> {

    List<ExamPaperAttempt> findByUserIdAndPaperIdOrderByStartedAtDesc(String userId, String paperId);

    long countByUserIdAndPaperId(String userId, String paperId);

    Optional<ExamPaperAttempt> findTopByUserIdAndPaperIdAndPassedTrueOrderByScoreDesc(String userId, String paperId);
}

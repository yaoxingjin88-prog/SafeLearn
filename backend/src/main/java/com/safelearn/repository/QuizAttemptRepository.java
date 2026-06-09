package com.safelearn.repository;

import com.safelearn.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, String> {

    List<QuizAttempt> findByUserIdAndQuizIdOrderByStartedAtDesc(String userId, String quizId);

    Optional<QuizAttempt> findTopByUserIdAndQuizIdOrderByScoreDesc(String userId, String quizId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.quiz.chapterId = :chapterId ORDER BY qa.startedAt DESC")
    List<QuizAttempt> findByUserIdAndChapterId(@Param("userId") String userId, @Param("chapterId") String chapterId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.passed = true")
    List<QuizAttempt> findPassedAttemptsByUserId(@Param("userId") String userId);
}

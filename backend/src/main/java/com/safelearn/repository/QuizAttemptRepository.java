package com.safelearn.repository;

import com.safelearn.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, String> {

    long countByQuizId(String quizId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.quiz.id = :quizId ORDER BY qa.startedAt DESC")
    List<QuizAttempt> findByUserIdAndQuizIdOrderByStartedAtDesc(
            @Param("userId") String userId, @Param("quizId") String quizId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId ORDER BY qa.startedAt DESC")
    List<QuizAttempt> findByUserIdOrderByStartedAtDesc(@Param("userId") String userId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.quiz.id = :quizId ORDER BY qa.score DESC")
    Optional<QuizAttempt> findTopByUserIdAndQuizIdOrderByScoreDesc(
            @Param("userId") String userId, @Param("quizId") String quizId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.quiz.chapterId = :chapterId ORDER BY qa.startedAt DESC")
    List<QuizAttempt> findByUserIdAndChapterId(@Param("userId") String userId, @Param("chapterId") String chapterId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.passed = true")
    List<QuizAttempt> findPassedAttemptsByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.completedAt IS NULL")
    long countInProgressByUserId(@Param("userId") String userId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.completedAt IS NOT NULL")
    List<QuizAttempt> findCompletedByUserId(@Param("userId") String userId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.quiz.id = :quizId AND qa.completedAt IS NULL ORDER BY qa.startedAt DESC")
    List<QuizAttempt> findInProgressByUserIdAndQuizId(@Param("userId") String userId, @Param("quizId") String quizId);

    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.quiz.id = :quizId")
    long countByUserIdAndQuizId(@Param("userId") String userId, @Param("quizId") String quizId);
}

package com.safelearn.repository;

import com.safelearn.entity.ChapterQuiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChapterQuizRepository extends JpaRepository<ChapterQuiz, String> {
    Optional<ChapterQuiz> findByChapterId(String chapterId);
    boolean existsByChapterId(String chapterId);
    List<ChapterQuiz> findByChapterIdIn(Collection<String> chapterIds);
}

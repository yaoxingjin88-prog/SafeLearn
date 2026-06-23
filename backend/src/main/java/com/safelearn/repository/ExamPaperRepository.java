package com.safelearn.repository;

import com.safelearn.entity.ExamPaper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamPaperRepository extends JpaRepository<ExamPaper, String> {

    List<ExamPaper> findAllByOrderByUpdatedAtDesc();
}

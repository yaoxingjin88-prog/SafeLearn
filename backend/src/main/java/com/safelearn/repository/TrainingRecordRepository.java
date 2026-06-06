package com.safelearn.repository;

import com.safelearn.entity.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, String> {
    List<TrainingRecord> findByUserIdOrderByCreatedAtDesc(String userId);
    long countByUserId(String userId);

    @Query("SELECT AVG(r.totalScore) FROM TrainingRecord r WHERE r.endTime IS NOT NULL AND r.totalScore IS NOT NULL")
    Double avgCompletedScore();

    @Query("SELECT AVG(r.totalScore) FROM TrainingRecord r WHERE r.user.id = :userId AND r.endTime IS NOT NULL AND r.totalScore IS NOT NULL")
    Double avgCompletedScoreByUserId(@Param("userId") String userId);
}

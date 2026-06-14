package com.safelearn.repository;

import com.safelearn.entity.TrainingRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, String> {
    @Query("SELECT r FROM TrainingRecord r JOIN FETCH r.user JOIN FETCH r.scenario WHERE r.id = :id")
    Optional<TrainingRecord> findByIdWithDetails(@Param("id") String id);

    List<TrainingRecord> findByUserIdOrderByCreatedAtDesc(String userId);

    long deleteByScenario_Id(String scenarioId);

    Page<TrainingRecord> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    long countByUserId(String userId);

    @Query("SELECT AVG(r.totalScore) FROM TrainingRecord r WHERE r.endTime IS NOT NULL AND r.totalScore IS NOT NULL")
    Double avgCompletedScore();

    @Query("SELECT AVG(r.totalScore) FROM TrainingRecord r WHERE r.user.id = :userId AND r.endTime IS NOT NULL AND r.totalScore IS NOT NULL")
    Double avgCompletedScoreByUserId(@Param("userId") String userId);
}

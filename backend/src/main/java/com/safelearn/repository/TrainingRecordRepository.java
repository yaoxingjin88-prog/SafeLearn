package com.safelearn.repository;

import com.safelearn.entity.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, String> {
    List<TrainingRecord> findByUserIdOrderByCreatedAtDesc(String userId);
    long countByUserId(String userId);
}

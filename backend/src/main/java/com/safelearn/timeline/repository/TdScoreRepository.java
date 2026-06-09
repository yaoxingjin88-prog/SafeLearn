package com.safelearn.timeline.repository;

import com.safelearn.timeline.entity.TdScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TdScoreRepository extends JpaRepository<TdScore, String> {
    Optional<TdScore> findBySessionId(String sessionId);
}

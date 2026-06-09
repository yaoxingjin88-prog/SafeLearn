package com.safelearn.timeline.repository;

import com.safelearn.timeline.entity.TdSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TdSessionRepository extends JpaRepository<TdSession, String> {
    List<TdSession> findByUserIdOrderByStartedAtDesc(String userId);
}

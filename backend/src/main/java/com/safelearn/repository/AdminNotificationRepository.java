package com.safelearn.repository;

import com.safelearn.entity.AdminNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, String> {

    List<AdminNotification> findByUserIdIsNullOrUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdIsNullOrUserIdAndReadAtIsNull(String userId);

    Optional<AdminNotification> findBySourceKey(String sourceKey);

    boolean existsBySourceKey(String sourceKey);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE AdminNotification n SET n.readAt = :readAt
            WHERE (n.userId IS NULL OR n.userId = :userId) AND n.readAt IS NULL
            """)
    int markAllRead(@Param("userId") String userId, @Param("readAt") LocalDateTime readAt);
}

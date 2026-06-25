package com.safelearn.repository;

import com.safelearn.entity.UserNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, String> {

    List<UserNotification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(String userId);

    Optional<UserNotification> findBySourceKey(String sourceKey);

    boolean existsBySourceKey(String sourceKey);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE UserNotification n SET n.readAt = :readAt
            WHERE n.userId = :userId AND n.readAt IS NULL
            """)
    int markAllRead(@Param("userId") String userId, @Param("readAt") LocalDateTime readAt);
}

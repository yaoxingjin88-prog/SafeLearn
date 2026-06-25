package com.safelearn.repository;

import com.safelearn.entity.AdminMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdminMessageRepository extends JpaRepository<AdminMessage, String> {

    List<AdminMessage> findByReceiverIdIsNullOrReceiverIdOrderByCreatedAtDesc(String receiverId, Pageable pageable);

    long countByReceiverIdIsNullOrReceiverIdAndReadAtIsNull(String receiverId);

    Optional<AdminMessage> findBySourceKey(String sourceKey);

    boolean existsBySourceKey(String sourceKey);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE AdminMessage m SET m.readAt = :readAt
            WHERE (m.receiverId IS NULL OR m.receiverId = :receiverId) AND m.readAt IS NULL
            """)
    int markAllRead(@Param("receiverId") String receiverId, @Param("readAt") LocalDateTime readAt);
}

package com.safelearn.repository;

import com.safelearn.entity.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, String> {
    Optional<UserFavorite> findByUserIdAndTargetTypeAndTargetId(String userId, String targetType, String targetId);

    List<UserFavorite> findByUserIdOrderByCreatedAtDesc(String userId);

    List<UserFavorite> findByUserIdAndTargetTypeOrderByCreatedAtDesc(String userId, String targetType);

    boolean existsByUserIdAndTargetTypeAndTargetId(String userId, String targetType, String targetId);
}

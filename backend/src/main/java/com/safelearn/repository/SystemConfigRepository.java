package com.safelearn.repository;

import com.safelearn.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {

    Optional<SystemConfig> findByConfigKey(String configKey);

    boolean existsByConfigKey(String configKey);

    List<SystemConfig> findAllByOrderByCategoryAscSortOrderAsc();

    List<SystemConfig> findByIsPublicTrue();

    List<SystemConfig> findByCategoryOrderBySortOrderAsc(String category);
}

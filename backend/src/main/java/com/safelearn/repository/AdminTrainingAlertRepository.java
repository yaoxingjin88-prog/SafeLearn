package com.safelearn.repository;

import com.safelearn.entity.AdminTrainingAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminTrainingAlertRepository extends JpaRepository<AdminTrainingAlert, String> {

    Optional<AdminTrainingAlert> findByAlertNo(String alertNo);

    long countByAlertNoStartingWith(String prefix);
}

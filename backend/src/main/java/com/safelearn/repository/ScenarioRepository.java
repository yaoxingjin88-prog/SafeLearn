package com.safelearn.repository;

import com.safelearn.entity.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioRepository extends JpaRepository<Scenario, String> {
}

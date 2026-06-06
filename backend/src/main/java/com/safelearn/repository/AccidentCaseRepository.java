package com.safelearn.repository;

import com.safelearn.entity.AccidentCase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccidentCaseRepository extends JpaRepository<AccidentCase, String> {
}

package com.safelearn.repository;

import com.safelearn.entity.LearnerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearnerAssignmentRepository extends JpaRepository<LearnerAssignment, String> {

    List<LearnerAssignment> findByUserIdAndStatusNotOrderByCreatedAtDesc(String userId, String status);
}

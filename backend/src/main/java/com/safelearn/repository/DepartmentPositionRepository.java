package com.safelearn.repository;

import com.safelearn.entity.DepartmentPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentPositionRepository extends JpaRepository<DepartmentPosition, String> {
    List<DepartmentPosition> findByDepartmentIdOrderBySortOrderAscNameAsc(String departmentId);

    long countByDepartmentId(String departmentId);
}

package com.safelearn.repository;

import com.safelearn.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, String> {
    List<Department> findByParentIdOrderBySortOrderAscNameAsc(String parentId);

    List<Department> findByParentIdIsNullOrderBySortOrderAscNameAsc();

    boolean existsByNameAndParentId(String name, String parentId);

    long countByParentId(String parentId);
}

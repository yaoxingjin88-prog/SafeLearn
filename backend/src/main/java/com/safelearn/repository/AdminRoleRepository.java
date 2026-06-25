package com.safelearn.repository;

import com.safelearn.entity.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRoleRepository extends JpaRepository<AdminRole, String> {

    List<AdminRole> findAllByOrderByRoleTypeAscNameAsc();

    Optional<AdminRole> findByCode(String code);

    boolean existsByCode(String code);
}

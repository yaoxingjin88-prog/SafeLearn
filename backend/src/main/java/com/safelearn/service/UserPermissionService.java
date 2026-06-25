package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.AdminRole;
import com.safelearn.entity.Department;
import com.safelearn.entity.User;
import com.safelearn.repository.AdminRoleRepository;
import com.safelearn.repository.DepartmentRepository;
import com.safelearn.security.AdminApiPermissionRegistry;
import com.safelearn.security.PermissionCodes;
import com.safelearn.security.PermissionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private final AdminRoleRepository roleRepo;
    private final DepartmentRepository departmentRepo;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ResolvedUserPermission resolve(User user) {
        if (user == null || !"admin".equalsIgnoreCase(user.getRole())) {
            return ResolvedUserPermission.empty();
        }
        AdminRole role = resolveAssignedRole(user);
        if (role == null) {
            return ResolvedUserPermission.empty();
        }
        List<String> codes = flattenPermissions(role.getPermissions());
        List<String> customDeptNames = resolveCustomDeptNames(role.getCustomDeptIds());
        return new ResolvedUserPermission(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDataScope(),
                customDeptNames,
                codes
        );
    }

    @Transactional(readOnly = true)
    public void bindContext(User user) {
        ResolvedUserPermission resolved = resolve(user);
        PermissionContext.set(new PermissionContext.UserPermissionSnapshot(
                user.getId(),
                user.getDepartment(),
                resolved.dataScope(),
                resolved.customDeptNames(),
                resolved.permissionCodes()
        ));
    }

    public boolean currentHasPermission(String permissionCode) {
        PermissionContext.UserPermissionSnapshot snapshot = PermissionContext.get();
        if (snapshot == null || snapshot.permissionCodes() == null) {
            return false;
        }
        return snapshot.permissionCodes().contains(permissionCode);
    }

    public boolean userInDataScope(User target, User operator, ResolvedUserPermission operatorPermission) {
        if (target == null || operator == null || operatorPermission == null) {
            return true;
        }
        String scope = operatorPermission.dataScope() == null ? "all" : operatorPermission.dataScope();
        return switch (scope) {
            case "all" -> true;
            case "department" -> sameDepartment(target, operator);
            case "dept_tree" -> sameDepartment(target, operator);
            case "self" -> Objects.equals(target.getId(), operator.getId());
            case "custom" -> operatorPermission.customDeptNames().contains(normalizeDept(target.getDepartment()));
            default -> true;
        };
    }

    private AdminRole resolveAssignedRole(User user) {
        if (user.getPermissionRoleId() != null && !user.getPermissionRoleId().isBlank()) {
            Optional<AdminRole> assigned = roleRepo.findById(user.getPermissionRoleId())
                    .filter(role -> "published".equals(role.getStatus()));
            if (assigned.isPresent()) {
                return assigned.get();
            }
        }
        return roleRepo.findByCode("ROLE_SUPER_ADMIN").orElse(null);
    }

    private List<String> flattenPermissions(String json) {
        if (json == null || json.isBlank()) {
            return AdminApiPermissionRegistry.allPermissionCodes();
        }
        try {
            Map<String, Map<String, Boolean>> matrix = objectMapper.readValue(json, new TypeReference<>() {});
            List<String> codes = new ArrayList<>();
            matrix.forEach((module, actions) -> actions.forEach((action, enabled) -> {
                if (Boolean.TRUE.equals(enabled)) {
                    codes.add(PermissionCodes.build(module, action));
                }
            }));
            return codes;
        } catch (Exception exception) {
            return AdminApiPermissionRegistry.allPermissionCodes();
        }
    }

    private List<String> resolveCustomDeptNames(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> ids = objectMapper.readValue(json, new TypeReference<>() {});
            return departmentRepo.findAllById(ids).stream()
                    .map(Department::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private boolean sameDepartment(User target, User operator) {
        return normalizeDept(target.getDepartment()).equals(normalizeDept(operator.getDepartment()));
    }

    private String normalizeDept(String department) {
        if (department == null || department.isBlank()) {
            return "未分配部门";
        }
        return department.trim();
    }

    public record ResolvedUserPermission(
            String permissionRoleId,
            String permissionRoleCode,
            String permissionRoleName,
            String dataScope,
            List<String> customDeptNames,
            List<String> permissionCodes
    ) {
        static ResolvedUserPermission empty() {
            return new ResolvedUserPermission(null, null, null, "all", List.of(), List.of());
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("permissionRoleId", permissionRoleId);
            map.put("permissionRoleCode", permissionRoleCode);
            map.put("permissionRoleName", permissionRoleName);
            map.put("dataScope", dataScope);
            map.put("customDeptNames", customDeptNames);
            map.put("permissions", permissionCodes);
            return map;
        }
    }
}

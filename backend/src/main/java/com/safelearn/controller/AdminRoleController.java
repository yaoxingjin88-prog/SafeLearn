package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.AdminRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listRoles() {
        return ApiResponse.success(adminRoleService.listRoles());
    }

    @GetMapping("/modules")
    public ApiResponse<Map<String, Object>> getModules() {
        return ApiResponse.success(adminRoleService.getModules());
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getRole(@PathVariable String id) {
        return ApiResponse.success(adminRoleService.getRole(id));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createRole(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminRoleService.createRole(body));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> updateRole(@PathVariable String id,
                                                       @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminRoleService.updateRole(id, body));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Map<String, Object>> publishRole(@PathVariable String id) {
        return ApiResponse.success(adminRoleService.publishRole(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> deleteRole(@PathVariable String id) {
        return ApiResponse.success(adminRoleService.deleteRole(id));
    }
}

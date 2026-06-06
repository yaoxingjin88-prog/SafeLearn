package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> getUsers() {
        List<Map<String, Object>> users = adminService.getUsers();
        return ApiResponse.success(Map.of("items", users, "total", users.size()));
    }

    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminService.createUser(body));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<Map<String, Object>> updateUser(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminService.updateUser(id, body));
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Map<String, Object>> deleteUser(@PathVariable String id) {
        return ApiResponse.success(adminService.deleteUser(id));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.success(adminService.getStats());
    }
}

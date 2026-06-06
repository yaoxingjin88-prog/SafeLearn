package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.dto.LoginRequest;
import com.safelearn.dto.RegisterRequest;
import com.safelearn.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.success(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.success(authService.login(req));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(authService.getUserInfo(userId));
    }

    @GetMapping("/user-info")
    public ApiResponse<Map<String, Object>> userInfo(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(authService.getUserInfo(userId));
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refresh(@RequestHeader("Authorization") String header) {
        String token = header.replace("Bearer ", "");
        return ApiResponse.success(authService.refreshToken(token));
    }
}

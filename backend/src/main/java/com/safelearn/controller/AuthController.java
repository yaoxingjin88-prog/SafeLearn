package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.dto.*;
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

    @PutMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest req) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(authService.updateProfile(userId, req));
    }

    @PostMapping("/change-password")
    public ApiResponse<Map<String, Object>> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest req) {
        String userId = auth.getPrincipal().toString();
        authService.changePassword(userId, req);
        return ApiResponse.success(Map.of("message", "密码修改成功"));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return ApiResponse.success(authService.forgotPassword(req));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ApiResponse.success(Map.of("message", "密码重置成功，请使用新密码登录"));
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refresh(@RequestHeader("Authorization") String header) {
        String token = header.replace("Bearer ", "");
        return ApiResponse.success(authService.refreshToken(token));
    }
}

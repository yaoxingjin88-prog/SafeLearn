package com.safelearn.service;

import com.safelearn.common.JwtUtil;
import com.safelearn.dto.*;
import com.safelearn.entity.PasswordResetToken;
import com.safelearn.entity.User;
import com.safelearn.repository.PasswordResetTokenRepository;
import com.safelearn.repository.UserRepository;
import com.safelearn.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int RESET_TOKEN_MINUTES = 30;

    private final UserRepository userRepo;
    private final PasswordResetTokenRepository resetTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MailNotificationService mailService;
    private final UserPermissionService userPermissionService;

    public Map<String, Object> register(RegisterRequest req) {
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole("trainee");
        user.setCompany(req.getCompany());
        user.setDepartment(req.getDepartment());
        user = userRepo.save(user);

        UserPermissionService.ResolvedUserPermission resolved = userPermissionService.resolve(user);
        String token = jwtUtil.generateToken(
                user.getId(), user.getUsername(), user.getRole(),
                resolved.permissionCodes(), resolved.permissionRoleId(), resolved.permissionRoleCode());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", toUserInfo(user, resolved));
        return result;
    }

    @Transactional
    public Map<String, Object> login(LoginRequest req) {
        User user = userRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepo.save(user);

        UserPermissionService.ResolvedUserPermission resolved = userPermissionService.resolve(user);
        String token = jwtUtil.generateToken(
                user.getId(), user.getUsername(), user.getRole(),
                resolved.permissionCodes(), resolved.permissionRoleId(), resolved.permissionRoleCode());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", toUserInfo(user, resolved));
        return result;
    }

    public Map<String, Object> getUserInfo(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        UserPermissionService.ResolvedUserPermission resolved = userPermissionService.resolve(user);
        return toUserInfo(user, resolved);
    }

    @Transactional
    public Map<String, Object> updateProfile(String userId, UpdateProfileRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            userRepo.findByEmail(req.getEmail().trim())
                    .filter(u -> !u.getId().equals(userId))
                    .ifPresent(u -> { throw new RuntimeException("邮箱已被其他账号使用"); });
            user.setEmail(req.getEmail().trim());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone().isBlank() ? null : req.getPhone().trim());
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl().isBlank() ? null : req.getAvatarUrl().trim());
        }

        userRepo.save(user);
        UserPermissionService.ResolvedUserPermission resolved = userPermissionService.resolve(user);
        return toUserInfo(user, resolved);
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("当前密码不正确");
        }
        if (passwordEncoder.matches(req.getNewPassword(), user.getPasswordHash())) {
            throw new RuntimeException("新密码不能与当前密码相同");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(user);
    }

    @Transactional
    public Map<String, Object> forgotPassword(ForgotPasswordRequest req) {
        String email = req.getEmail().trim();
        Map<String, Object> result = new HashMap<>();
        result.put("message", "若该邮箱已注册，重置链接将发送至您的邮箱，请查收（有效期 30 分钟）");

        userRepo.findByEmailIgnoreCase(email).ifPresent(user -> {
            String token = generateResetToken();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setEmail(user.getEmail());
            resetToken.setToken(token);
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_MINUTES));
            resetTokenRepo.save(resetToken);

            mailService.sendPasswordResetEmail(user.getEmail(), token);

            if (mailService.isDevExposeToken()) {
                result.put("devResetUrl", mailService.buildResetUrl(token));
            }
        });

        return result;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetToken resetToken = resetTokenRepo.findByTokenAndUsedFalse(req.getToken())
                .orElseThrow(() -> new RuntimeException("重置链接无效或已使用"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("重置链接已过期，请重新申请");
        }

        User user = userRepo.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(user);

        resetToken.setUsed(true);
        resetTokenRepo.save(resetToken);
    }

    public Map<String, Object> refreshToken(String oldToken) {
        String userId = jwtUtil.getUserId(oldToken);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        UserPermissionService.ResolvedUserPermission resolved = userPermissionService.resolve(user);
        String newToken = jwtUtil.generateToken(
                user.getId(), user.getUsername(), user.getRole(),
                resolved.permissionCodes(), resolved.permissionRoleId(), resolved.permissionRoleCode());
        return Map.of("token", newToken);
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Map<String, Object> toUserInfo(User user, UserPermissionService.ResolvedUserPermission resolved) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("email", user.getEmail());
        info.put("role", user.getRole());
        info.put("company", user.getCompany());
        info.put("department", user.getDepartment());
        info.put("phone", user.getPhone());
        info.put("avatarUrl", user.getAvatarUrl());
        info.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null);
        info.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        if (resolved != null) {
            info.putAll(resolved.toMap());
        }
        return info;
    }
}

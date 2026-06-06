package com.safelearn.service;

import com.safelearn.common.JwtUtil;
import com.safelearn.dto.LoginRequest;
import com.safelearn.dto.RegisterRequest;
import com.safelearn.entity.User;
import com.safelearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", toUserInfo(user));
        return result;
    }

    public Map<String, Object> login(LoginRequest req) {
        User user = userRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", toUserInfo(user));
        return result;
    }

    public Map<String, Object> getUserInfo(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return toUserInfo(user);
    }

    public Map<String, Object> refreshToken(String oldToken) {
        String userId = jwtUtil.getUserId(oldToken);
        String username = jwtUtil.getUsername(oldToken);
        String role = jwtUtil.getRole(oldToken);
        String newToken = jwtUtil.generateToken(userId, username, role);
        return Map.of("token", newToken);
    }

    private Map<String, Object> toUserInfo(User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("email", user.getEmail());
        info.put("role", user.getRole());
        info.put("company", user.getCompany());
        info.put("department", user.getDepartment());
        info.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        return info;
    }
}

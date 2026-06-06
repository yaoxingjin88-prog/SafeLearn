package com.safelearn.service;

import com.safelearn.entity.User;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final TrainingRecordRepository recordRepo;
    private final PasswordEncoder passwordEncoder;

    public List<Map<String, Object>> getUsers() {
        return userRepo.findAll().stream().map(this::toUserInfo).toList();
    }

    public Map<String, Object> createUser(Map<String, Object> data) {
        User user = new User();
        user.setUsername((String) data.get("username"));
        user.setEmail((String) data.get("email"));
        user.setPasswordHash(passwordEncoder.encode((String) data.getOrDefault("password", "123456")));
        user.setRole((String) data.getOrDefault("role", "trainee"));
        user.setCompany((String) data.get("company"));
        user.setDepartment((String) data.get("department"));
        user = userRepo.save(user);
        return toUserInfo(user);
    }

    public Map<String, Object> updateUser(String id, Map<String, Object> data) {
        User user = userRepo.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (data.containsKey("username")) user.setUsername((String) data.get("username"));
        if (data.containsKey("email")) user.setEmail((String) data.get("email"));
        if (data.containsKey("role")) user.setRole((String) data.get("role"));
        if (data.containsKey("company")) user.setCompany((String) data.get("company"));
        if (data.containsKey("department")) user.setDepartment((String) data.get("department"));
        userRepo.save(user);
        return Map.of("success", true);
    }

    public Map<String, Object> deleteUser(String id) {
        userRepo.deleteById(id);
        return Map.of("success", true);
    }

    public Map<String, Object> getStats() {
        long totalUsers = userRepo.count();
        long totalCourses = courseRepo.findByStatusOrderByCreatedAtDesc("published").size();
        long totalSimulations = recordRepo.count();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalCourses", totalCourses);
        stats.put("totalSimulations", totalSimulations);
        stats.put("avgScore", 82);
        return stats;
    }

    private Map<String, Object> toUserInfo(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("email", u.getEmail());
        m.put("role", u.getRole());
        m.put("company", u.getCompany());
        m.put("department", u.getDepartment());
        m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        return m;
    }
}

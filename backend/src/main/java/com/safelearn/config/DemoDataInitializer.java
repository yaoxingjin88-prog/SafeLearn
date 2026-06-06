package com.safelearn.config;

import com.safelearn.entity.User;
import com.safelearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 确保演示账号存在（data.sql 可能因历史库或编码问题未写入测试用户）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "admin123";

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedDemoUser(
                "660e8400-e29b-41d4-a716-446655440001",
                "zhanggong",
                "zhanggong@safelearn.com",
                "trainee",
                "华能储能电站",
                "运维部",
                LocalDateTime.now().minusDays(44)
        );
        seedDemoUser(
                "660e8400-e29b-41d4-a716-446655440002",
                "lisi",
                "lisi@safelearn.com",
                "trainee",
                "国电新能源",
                "安全部",
                LocalDateTime.now().minusDays(20)
        );
        seedDemoUser(
                "660e8400-e29b-41d4-a716-446655440003",
                "wangwu",
                "wangwu@safelearn.com",
                "trainee",
                "南方电网储能",
                "培训部",
                LocalDateTime.now().minusDays(60)
        );
    }

    private void seedDemoUser(String id, String username, String email, String role,
                              String company, String department, LocalDateTime createdAt) {
        if (userRepo.existsByUsername(username)) {
            return;
        }
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(role);
        user.setCompany(company);
        user.setDepartment(department);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(LocalDateTime.now());
        userRepo.save(user);
        log.info("已创建演示账号: {} / {}", username, DEMO_PASSWORD);
    }
}

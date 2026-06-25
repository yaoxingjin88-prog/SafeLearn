package com.safelearn.config;

import com.safelearn.entity.User;
import com.safelearn.repository.AdminRoleRepository;
import com.safelearn.repository.UserRepository;
import com.safelearn.service.AdminRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RolePermissionInitializer implements ApplicationRunner {

    private final AdminRoleService adminRoleService;
    private final AdminRoleRepository roleRepo;
    private final UserRepository userRepo;

    @Override
    public void run(ApplicationArguments args) {
        adminRoleService.ensureDefaultRoles();
        roleRepo.findByCode("ROLE_SUPER_ADMIN").ifPresent(superRole ->
                userRepo.findAll().stream()
                        .filter(user -> "admin".equalsIgnoreCase(user.getRole()))
                        .filter(user -> user.getPermissionRoleId() == null || user.getPermissionRoleId().isBlank())
                        .forEach(user -> {
                            user.setPermissionRoleId(superRole.getId());
                            userRepo.save(user);
                        }));
    }
}

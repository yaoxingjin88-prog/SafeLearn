package com.safelearn.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.common.ApiResponse;
import com.safelearn.entity.User;
import com.safelearn.repository.UserRepository;
import com.safelearn.service.UserPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminPermissionInterceptor implements HandlerInterceptor {

    private final UserRepository userRepo;
    private final UserPermissionService userPermissionService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/admin/")) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return true;
        }

        String userId = String.valueOf(authentication.getPrincipal());
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty() || !"admin".equalsIgnoreCase(userOpt.get().getRole())) {
            return true;
        }

        User user = userOpt.get();
        userPermissionService.bindContext(user);

        Optional<String> required = AdminApiPermissionRegistry.resolveRequiredPermission(path, request.getMethod());
        if (required.isEmpty()) {
            return true;
        }

        if (userPermissionService.currentHasPermission(required.get())) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(403, "权限不足")));
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        PermissionContext.clear();
    }
}

package com.safelearn.filter;

import com.safelearn.common.JwtUtil;
import com.safelearn.entity.User;
import com.safelearn.repository.UserRepository;
import com.safelearn.service.UserPermissionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;
    private final UserPermissionService userPermissionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String userId = jwtUtil.getUserId(token);
                String username = jwtUtil.getUsername(token);
                String role = jwtUtil.getRole(token);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));

                userRepo.findById(userId).ifPresent(user -> {
                    if ("admin".equalsIgnoreCase(user.getRole())) {
                        UserPermissionService.ResolvedUserPermission resolved = userPermissionService.resolve(user);
                        resolved.permissionCodes().forEach(code ->
                                authorities.add(new SimpleGrantedAuthority(code)));
                    }
                });

                if (authorities.stream().noneMatch(item -> item.getAuthority().startsWith("perm:"))) {
                    jwtUtil.getPermissions(token).forEach(code ->
                            authorities.add(new SimpleGrantedAuthority(code)));
                }

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities
                );
                auth.setDetails(username);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
            }
        }
        filterChain.doFilter(request, response);
    }
}

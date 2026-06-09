package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            Authentication auth,
            @RequestParam(required = false) String targetType) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(favoriteService.list(userId, targetType));
    }

    @GetMapping("/ids")
    public ApiResponse<List<String>> favoriteIds(
            Authentication auth,
            @RequestParam String targetType) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(favoriteService.getFavoriteIds(userId, targetType));
    }

    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> check(
            Authentication auth,
            @RequestParam String targetType,
            @RequestParam String targetId) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(Map.of(
                "favorited", favoriteService.isFavorited(userId, targetType, targetId)));
    }

    @PostMapping("/toggle")
    public ApiResponse<Map<String, Object>> toggle(
            Authentication auth,
            @RequestBody Map<String, String> body) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(favoriteService.toggle(
                userId, body.get("targetType"), body.get("targetId")));
    }
}

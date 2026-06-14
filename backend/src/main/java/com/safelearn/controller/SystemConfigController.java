package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户端公开配置读取接口（仅下发 is_public 且非敏感的配置）。
 * 管理端 CRUD 见 AdminController 的 /api/admin/system-configs。
 */
@RestController
@RequestMapping("/api/system-configs")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/public")
    public ApiResponse<Map<String, Object>> getPublic() {
        return ApiResponse.success(systemConfigService.listPublic());
    }
}

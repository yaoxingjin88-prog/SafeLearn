package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping("/mine")
    public ApiResponse<List<Map<String, Object>>> mine(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(certificateService.listMine(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(Authentication auth, @PathVariable String id) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(certificateService.getById(userId, id));
    }
}

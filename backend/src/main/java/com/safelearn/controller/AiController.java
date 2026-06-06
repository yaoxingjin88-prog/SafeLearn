package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.dto.AiAskRequest;
import com.safelearn.dto.FeedbackRequest;
import com.safelearn.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/ask")
    public ApiResponse<Map<String, Object>> ask(Authentication auth, @RequestBody AiAskRequest req) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(aiService.ask(userId, req));
    }

    @GetMapping("/history")
    public ApiResponse<Map<String, Object>> history(Authentication auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(aiService.getHistory(userId, page, pageSize));
    }

    @PostMapping("/feedback")
    public ApiResponse<Map<String, Object>> feedback(@RequestBody FeedbackRequest req) {
        return ApiResponse.success(aiService.feedback(req));
    }
}

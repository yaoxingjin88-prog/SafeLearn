package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.CaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getCases() {
        return ApiResponse.success(caseService.getCases());
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getCaseById(@PathVariable String id) {
        return ApiResponse.success(caseService.getCaseById(id));
    }
}

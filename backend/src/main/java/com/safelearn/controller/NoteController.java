package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            Authentication auth,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId) {
        String userId = auth.getPrincipal().toString();
        if (targetType != null && targetId != null) {
            return ApiResponse.success(noteService.list(userId, targetType, targetId));
        }
        return ApiResponse.success(noteService.listAll(userId));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> save(Authentication auth, @RequestBody Map<String, Object> body) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(noteService.save(userId, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable String id) {
        String userId = auth.getPrincipal().toString();
        noteService.delete(userId, id);
        return ApiResponse.success(null);
    }
}

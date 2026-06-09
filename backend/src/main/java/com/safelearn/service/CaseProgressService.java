package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.UserCaseProgress;
import com.safelearn.repository.UserCaseProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CaseProgressService {

    private final UserCaseProgressRepository progressRepo;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> getSummary(String userId) {
        return progressRepo.findByUserId(userId).stream().map(this::toSummary).toList();
    }

    public Map<String, Object> getProgress(String userId, String caseId) {
        return progressRepo.findByUserIdAndCaseId(userId, caseId)
                .map(this::toDetail)
                .orElseGet(() -> Map.of(
                        "caseId", caseId,
                        "currentStep", 0,
                        "totalSteps", 0,
                        "completed", false,
                        "reflections", Map.of()
                ));
    }

    @Transactional
    public Map<String, Object> saveProgress(String userId, String caseId, Map<String, Object> body) {
        UserCaseProgress progress = progressRepo.findByUserIdAndCaseId(userId, caseId)
                .orElseGet(() -> {
                    UserCaseProgress p = new UserCaseProgress();
                    p.setUserId(userId);
                    p.setCaseId(caseId);
                    return p;
                });

        if (body.get("currentStep") instanceof Number n) {
            progress.setCurrentStep(n.intValue());
        }
        if (body.get("totalSteps") instanceof Number n) {
            progress.setTotalSteps(n.intValue());
        }
        if (body.get("reflections") != null) {
            try {
                progress.setReflections(objectMapper.writeValueAsString(body.get("reflections")));
            } catch (Exception e) {
                throw new RuntimeException("反思数据格式错误");
            }
        }
        progress.setLastAccessAt(LocalDateTime.now());
        progressRepo.save(progress);
        return toDetail(progress);
    }

    @Transactional
    public Map<String, Object> complete(String userId, String caseId, Map<String, Object> body) {
        UserCaseProgress progress = progressRepo.findByUserIdAndCaseId(userId, caseId)
                .orElseGet(() -> {
                    UserCaseProgress p = new UserCaseProgress();
                    p.setUserId(userId);
                    p.setCaseId(caseId);
                    return p;
                });

        if (body.get("totalSteps") instanceof Number n) {
            progress.setTotalSteps(n.intValue());
            progress.setCurrentStep(n.intValue());
        }
        if (body.get("reflections") != null) {
            try {
                progress.setReflections(objectMapper.writeValueAsString(body.get("reflections")));
            } catch (Exception e) {
                throw new RuntimeException("反思数据格式错误");
            }
        }
        progress.setCompleted(true);
        progress.setCompletedAt(LocalDateTime.now());
        progress.setLastAccessAt(LocalDateTime.now());
        progressRepo.save(progress);
        return toDetail(progress);
    }

    @Transactional
    public Map<String, Object> resetProgress(String userId, String caseId) {
        progressRepo.findByUserIdAndCaseId(userId, caseId)
                .ifPresent(progressRepo::delete);
        return Map.of(
                "caseId", caseId,
                "currentStep", 0,
                "totalSteps", 0,
                "completed", false,
                "reflections", Map.of()
        );
    }

    private Map<String, Object> toSummary(UserCaseProgress p) {
        Map<String, Object> m = new HashMap<>();
        m.put("caseId", p.getCaseId());
        m.put("completed", p.getCompleted());
        m.put("currentStep", p.getCurrentStep());
        m.put("totalSteps", p.getTotalSteps());
        m.put("lastAccessAt", p.getLastAccessAt() != null ? p.getLastAccessAt().toString() : null);
        return m;
    }

    private Map<String, Object> toDetail(UserCaseProgress p) {
        Map<String, Object> m = toSummary(p);
        m.put("completedAt", p.getCompletedAt() != null ? p.getCompletedAt().toString() : null);
        m.put("reflections", parseReflections(p.getReflections()));
        return m;
    }

    private Map<String, Object> parseReflections(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}

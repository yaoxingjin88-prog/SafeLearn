package com.safelearn.deduction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.deduction.entity.SimulationScoreReport;
import com.safelearn.deduction.entity.SimulationSession;
import com.safelearn.deduction.repository.SimulationScoreReportRepository;
import com.safelearn.deduction.repository.SimulationSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DeductionScoringService {

    private final SimulationSessionRepository sessionRepo;
    private final SimulationScoreReportRepository reportRepo;
    private final AiInstructorService aiInstructorService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> scoreSession(String userId, String sessionId) {
        SimulationSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("推演会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该推演会话");
        }

        int ruleScore = Math.max(0, Math.min(100, session.getRuleScore() != null ? session.getRuleScore() : 0));
        Map<String, Object> aiResult = aiInstructorService.evaluate(session, ruleScore);
        int aiScore = ((Number) aiResult.getOrDefault("totalScore", ruleScore)).intValue();
        int totalScore = (int) Math.round(ruleScore * 0.4 + aiScore * 0.6);

        String rating = toRating(totalScore);
        session.setAiScore(aiScore);
        session.setTotalScore(totalScore);
        session.setRating(rating);
        sessionRepo.save(session);

        SimulationScoreReport report = reportRepo.findBySessionId(sessionId).orElse(new SimulationScoreReport());
        report.setSessionId(sessionId);
        report.setRuleScore(ruleScore);
        report.setAiScore(aiScore);
        report.setTotalScore(totalScore);
        report.setRating(rating);
        try {
            report.setDimensions(objectMapper.writeValueAsString(aiResult.get("dimensions")));
            report.setHighlights(objectMapper.writeValueAsString(aiResult.get("highlights")));
            report.setImprovements(objectMapper.writeValueAsString(aiResult.get("improvements")));
            report.setInstructorSummary((String) aiResult.get("instructorSummary"));
            report.setRawAiResponse(objectMapper.writeValueAsString(aiResult));
        } catch (Exception e) {
            report.setDimensions("{}");
        }
        reportRepo.save(report);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("ruleScore", ruleScore);
        result.put("aiScore", aiScore);
        result.put("totalScore", totalScore);
        result.put("rating", rating);
        result.put("dimensions", aiResult.get("dimensions"));
        result.put("highlights", aiResult.get("highlights"));
        result.put("improvements", aiResult.get("improvements"));
        result.put("instructorSummary", aiResult.get("instructorSummary"));
        return result;
    }

    public Map<String, Object> getScoreReport(String userId, String sessionId) {
        SimulationSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("推演会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该推演会话");
        }
        return reportRepo.findBySessionId(sessionId)
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("sessionId", sessionId);
                    m.put("ruleScore", r.getRuleScore());
                    m.put("aiScore", r.getAiScore());
                    m.put("totalScore", r.getTotalScore());
                    m.put("rating", r.getRating());
                    m.put("instructorSummary", r.getInstructorSummary());
                    m.put("dimensions", parseJson(r.getDimensions()));
                    m.put("highlights", parseJsonList(r.getHighlights()));
                    m.put("improvements", parseJsonList(r.getImprovements()));
                    return m;
                })
                .orElse(Map.of("sessionId", sessionId, "message", "评分报告尚未生成"));
    }

    private String toRating(int score) {
        if (score >= 90) return "excellent";
        if (score >= 75) return "good";
        if (score >= 60) return "average";
        return "poor";
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}

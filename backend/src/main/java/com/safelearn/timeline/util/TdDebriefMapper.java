package com.safelearn.timeline.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.timeline.entity.TdScore;
import com.safelearn.timeline.entity.TdSession;

import java.util.*;

public final class TdDebriefMapper {

    private TdDebriefMapper() {}

    public static Map<String, Object> toDebriefMap(
            TdSession session,
            TdScore score,
            ObjectMapper objectMapper) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalScore", score.getTotalScore());
        m.put("rating", scoreToRatingLabel(score.getTotalScore()));
        m.put("ratingCode", scoreToRatingCode(score.getTotalScore()));
        m.put("instructorComment", score.getAiComment() != null ? score.getAiComment() : "");
        if (session != null) {
            m.put("branch", session.getBranchPath());
            m.put("outcome", session.getOutcome());
        }
        m.put("dimensions", buildDimensions(score));
        m.put("strengths", parseJsonList(score.getStrengthsJson(), objectMapper));
        m.put("weaknesses", parseJsonList(score.getWeaknessesJson(), objectMapper));
        m.put("fiveWhy", parseJsonList(score.getFiveWhyJson(), objectMapper));
        m.put("recommendations", parseRecommendations(score.getRecommendationsJson(), objectMapper));
        return m;
    }

    /** 列表摘要：含总评与四维分，便于历史页展示 */
    public static Map<String, Object> toListSummary(
            TdSession session,
            TdScore score,
            ObjectMapper objectMapper) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalScore", score.getTotalScore());
        m.put("rating", scoreToRatingCode(score.getTotalScore()));
        m.put("instructorComment", score.getAiComment());
        if (session != null) {
            m.put("branch", session.getBranchPath());
            m.put("outcome", session.getOutcome());
        }
        m.put("dimensions", buildDimensions(score));
        return m;
    }

    private static List<Map<String, Object>> buildDimensions(TdScore score) {
        List<Map<String, Object>> dims = new ArrayList<>();
        addDim(dims, "risk", "风险识别", score.getRiskIdentification());
        addDim(dims, "decision", "决策判断", score.getDecisionMaking());
        addDim(dims, "response", "应急响应", score.getEmergencyResponse());
        addDim(dims, "analysis", "事故分析", score.getAccidentAnalysis());
        return dims;
    }

    private static void addDim(List<Map<String, Object>> dims, String key, String label, Integer val) {
        if (val == null) return;
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("key", key);
        d.put("label", label);
        d.put("score", val);
        d.put("max", 100);
        dims.add(d);
    }

    private static String scoreToRatingCode(Integer score) {
        if (score == null) return "pending";
        if (score >= 90) return "excellent";
        if (score >= 75) return "good";
        if (score >= 60) return "average";
        return "poor";
    }

    private static String scoreToRatingLabel(Integer score) {
        return switch (scoreToRatingCode(score)) {
            case "excellent" -> "优秀";
            case "good" -> "良好";
            case "average" -> "合格";
            case "poor" -> "待提升";
            default -> "—";
        };
    }

    private static List<Object> parseJsonList(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Object>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<Map<String, Object>> parseRecommendations(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            List<Map<String, Object>> out = new ArrayList<>();
            for (Map<String, Object> item : raw) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("title", item.getOrDefault("title", item.get("name")));
                r.put("reason", item.getOrDefault("reason", item.get("description")));
                out.add(r);
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }
}

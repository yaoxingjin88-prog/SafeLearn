package com.safelearn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.AccidentCase;
import com.safelearn.repository.AccidentCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CaseService {

    private final AccidentCaseRepository caseRepo;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> getCases() {
        return caseRepo.findAll().stream().map(this::toCaseInfo).toList();
    }

    public Map<String, Object> getCaseById(String id) {
        AccidentCase c = caseRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("案例不存在"));
        return toCaseInfo(c);
    }

    /** 相关案例：优先同类型或同严重程度，其它案例兜底，最多 3 条。 */
    public List<Map<String, Object>> getRelatedCases(String id) {
        AccidentCase current = caseRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("案例不存在"));
        List<AccidentCase> others = caseRepo.findAll().stream()
                .filter(c -> !c.getId().equals(id))
                .toList();
        // 先按相关度排序：同类型且同严重度 > 同类型/同严重度 > 其它
        List<AccidentCase> ranked = others.stream()
                .sorted(Comparator.comparingInt((AccidentCase c) -> relevanceScore(current, c)).reversed())
                .limit(3)
                .toList();
        return ranked.stream().map(this::toCaseSummary).toList();
    }

    private int relevanceScore(AccidentCase current, AccidentCase other) {
        int score = 0;
        if (Objects.equals(current.getType(), other.getType())) score += 2;
        if (Objects.equals(current.getSeverity(), other.getSeverity())) score += 1;
        return score;
    }

    private Map<String, Object> toCaseSummary(AccidentCase c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("location", c.getLocation());
        m.put("date", c.getDate() != null ? c.getDate().toString() : null);
        m.put("type", c.getType());
        m.put("severity", c.getSeverity());
        return m;
    }

    private Map<String, Object> toCaseInfo(AccidentCase c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("location", c.getLocation());
        m.put("date", c.getDate() != null ? c.getDate().toString() : null);
        m.put("type", c.getType());
        m.put("severity", c.getSeverity());
        m.put("description", c.getDescription());
        m.put("timeline", parseJsonList(c.getTimeline()));
        m.put("causeAnalysis", c.getCauseAnalysis());
        m.put("lossEstimate", c.getLossEstimate());
        m.put("lessonsLearned", c.getLessonsLearned());
        // 结构化字段
        m.put("directCause", c.getDirectCause());
        m.put("indirectCause", c.getIndirectCause());
        m.put("rootCause", c.getRootCause());
        m.put("responsibleParty", c.getResponsibleParty());
        m.put("casualties", c.getCasualties());
        m.put("lossAmount", c.getLossAmount());
        m.put("lossBreakdown", parseJsonList(c.getLossBreakdown()));
        m.put("lessons", parseJsonList(c.getLessons()));
        m.put("difficulty", c.getDifficulty());
        m.put("studyMinutes", c.getStudyMinutes());
        m.put("references", List.of(
                "储能电站安全管理规定",
                "锂离子电池储能系统技术规范",
                "储能电站事故应急预案编制导则"
        ));
        return m;
    }

    private Object parseJsonList(String json) {
        if (json == null) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

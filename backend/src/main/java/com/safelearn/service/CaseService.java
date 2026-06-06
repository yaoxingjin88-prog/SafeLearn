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

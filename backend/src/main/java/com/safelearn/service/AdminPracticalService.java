package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.AccidentCase;
import com.safelearn.entity.Scenario;
import com.safelearn.repository.AccidentCaseRepository;
import com.safelearn.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminPracticalService {

    private final AccidentCaseRepository caseRepo;
    private final ScenarioRepository scenarioRepo;
    private final ObjectMapper objectMapper;

    // ===== 案例管理 =====

    @Transactional(readOnly = true)
    public Map<String, Object> searchCases(String keyword, String type, String severity, int page, int pageSize) {
        List<AccidentCase> filtered = caseRepo.findAll().stream()
                .sorted(Comparator.comparing(AccidentCase::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .filter(c -> keyword == null || keyword.isBlank()
                        || contains(c.getTitle(), keyword)
                        || contains(c.getLocation(), keyword))
                .filter(c -> type == null || type.isBlank() || "all".equals(type) || type.equals(c.getType()))
                .filter(c -> severity == null || severity.isBlank() || "all".equals(severity) || severity.equals(c.getSeverity()))
                .toList();
        return paginate(filtered.stream().map(this::toCaseListItem).toList(), page, pageSize);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCaseById(String id) {
        AccidentCase c = caseRepo.findById(id).orElseThrow(() -> new RuntimeException("案例不存在"));
        return toCaseDetail(c);
    }

    @Transactional
    public Map<String, Object> createCase(Map<String, Object> data) {
        AccidentCase c = new AccidentCase();
        applyCaseFields(c, data);
        if (c.getTitle() == null || c.getTitle().isBlank()) {
            throw new RuntimeException("案例标题不能为空");
        }
        if (c.getType() == null || c.getType().isBlank()) c.setType("thermal_runaway");
        if (c.getSeverity() == null || c.getSeverity().isBlank()) c.setSeverity("major");
        return toCaseDetail(caseRepo.save(c));
    }

    @Transactional
    public Map<String, Object> updateCase(String id, Map<String, Object> data) {
        AccidentCase c = caseRepo.findById(id).orElseThrow(() -> new RuntimeException("案例不存在"));
        applyCaseFields(c, data);
        return toCaseDetail(caseRepo.save(c));
    }

    @Transactional
    public void deleteCase(String id) {
        if (!caseRepo.existsById(id)) throw new RuntimeException("案例不存在");
        caseRepo.deleteById(id);
    }

    // ===== 场景管理 =====

    @Transactional(readOnly = true)
    public Map<String, Object> searchScenarios(String keyword, String kind, int page, int pageSize) {
        List<Map<String, Object>> items = scenarioRepo.findAll().stream()
                .map(this::toScenarioListItem)
                .filter(m -> keyword == null || keyword.isBlank()
                        || contains(String.valueOf(m.get("name")), keyword)
                        || contains(String.valueOf(m.get("description")), keyword))
                .filter(m -> kind == null || kind.isBlank() || "all".equals(kind) || kind.equals(m.get("kind")))
                .toList();
        return paginate(items, page, pageSize);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getScenarioById(String id) {
        Scenario s = scenarioRepo.findById(id).orElseThrow(() -> new RuntimeException("场景不存在"));
        return toScenarioDetail(s);
    }

    @Transactional
    public Map<String, Object> createScenario(Map<String, Object> data) {
        Scenario s = new Scenario();
        applyScenarioFields(s, data);
        if (s.getName() == null || s.getName().isBlank()) {
            throw new RuntimeException("场景名称不能为空");
        }
        if (s.getDuration() == null) s.setDuration(30);
        if (s.getDifficulty() == null) s.setDifficulty(2);
        return toScenarioDetail(scenarioRepo.save(s));
    }

    @Transactional
    public Map<String, Object> updateScenario(String id, Map<String, Object> data) {
        Scenario s = scenarioRepo.findById(id).orElseThrow(() -> new RuntimeException("场景不存在"));
        applyScenarioFields(s, data);
        return toScenarioDetail(scenarioRepo.save(s));
    }

    @Transactional
    public void deleteScenario(String id) {
        if (!scenarioRepo.existsById(id)) throw new RuntimeException("场景不存在");
        scenarioRepo.deleteById(id);
    }

    private void applyCaseFields(AccidentCase c, Map<String, Object> data) {
        if (data.containsKey("title")) c.setTitle(str(data.get("title")));
        if (data.containsKey("location")) c.setLocation(str(data.get("location")));
        if (data.containsKey("date")) {
            String d = str(data.get("date"));
            c.setDate(d != null && !d.isBlank() ? LocalDate.parse(d) : null);
        }
        if (data.containsKey("type")) c.setType(str(data.get("type")));
        if (data.containsKey("severity")) c.setSeverity(str(data.get("severity")));
        if (data.containsKey("description")) c.setDescription(str(data.get("description")));
        if (data.containsKey("causeAnalysis")) c.setCauseAnalysis(str(data.get("causeAnalysis")));
        if (data.containsKey("lessonsLearned")) c.setLessonsLearned(str(data.get("lessonsLearned")));
        if (data.containsKey("difficulty")) c.setDifficulty(str(data.get("difficulty")));
        if (data.containsKey("studyMinutes") && data.get("studyMinutes") != null) {
            c.setStudyMinutes(((Number) data.get("studyMinutes")).intValue());
        }
        if (data.containsKey("timeline")) c.setTimeline(json(data.get("timeline")));
    }

    private void applyScenarioFields(Scenario s, Map<String, Object> data) {
        if (data.containsKey("name")) s.setName(str(data.get("name")));
        if (data.containsKey("description")) s.setDescription(str(data.get("description")));
        if (data.containsKey("duration") && data.get("duration") != null) {
            s.setDuration(((Number) data.get("duration")).intValue());
        }
        if (data.containsKey("difficulty") && data.get("difficulty") != null) {
            s.setDifficulty(((Number) data.get("difficulty")).intValue());
        }
        if (data.containsKey("initialConditions")) s.setInitialConditions(json(data.get("initialConditions")));
        if (data.containsKey("decisionPoints")) s.setDecisionPoints(json(data.get("decisionPoints")));
        if (data.containsKey("events")) s.setEvents(json(data.get("events")));
    }

    private Map<String, Object> toCaseListItem(AccidentCase c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("location", c.getLocation());
        m.put("date", c.getDate() != null ? c.getDate().toString() : null);
        m.put("type", c.getType());
        m.put("severity", c.getSeverity());
        m.put("difficulty", c.getDifficulty());
        m.put("studyMinutes", c.getStudyMinutes());
        return m;
    }

    private Map<String, Object> toCaseDetail(AccidentCase c) {
        Map<String, Object> m = toCaseListItem(c);
        m.put("description", c.getDescription());
        m.put("causeAnalysis", c.getCauseAnalysis());
        m.put("lessonsLearned", c.getLessonsLearned());
        m.put("timeline", parseJson(c.getTimeline()));
        return m;
    }

    private Map<String, Object> toScenarioListItem(Scenario s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("description", s.getDescription());
        m.put("duration", s.getDuration());
        m.put("difficulty", s.getDifficulty());
        m.put("kind", resolveScenarioKind(s));
        m.put("caseId", extractCaseId(s));
        return m;
    }

    private Map<String, Object> toScenarioDetail(Scenario s) {
        Map<String, Object> m = toScenarioListItem(s);
        m.put("initialConditions", parseJson(s.getInitialConditions()));
        m.put("decisionPoints", parseJson(s.getDecisionPoints()));
        m.put("events", parseJson(s.getEvents()));
        return m;
    }

    @SuppressWarnings("unchecked")
    private String resolveScenarioKind(Scenario s) {
        Object parsed = parseJson(s.getInitialConditions());
        if (!(parsed instanceof Map<?, ?> map)) return "training";
        if ("emergency_case".equals(map.get("trainingKind"))) return "emergency";
        Object count = map.get("batteryCount");
        if (count instanceof Number n && n.intValue() > 0) return "simulation";
        return "training";
    }

    @SuppressWarnings("unchecked")
    private String extractCaseId(Scenario s) {
        Object parsed = parseJson(s.getInitialConditions());
        if (parsed instanceof Map<?, ?> map && map.get("caseId") != null) {
            return String.valueOf(map.get("caseId"));
        }
        return null;
    }

    private Map<String, Object> paginate(List<Map<String, Object>> all, int page, int pageSize) {
        int total = all.size();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        int from = Math.max(0, (safePage - 1) * safeSize);
        int to = Math.min(total, from + safeSize);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", from >= total ? List.of() : all.subList(from, to));
        result.put("total", total);
        result.put("page", safePage);
        result.put("pageSize", safeSize);
        result.put("totalPages", safeSize == 0 ? 0 : (int) Math.ceil(total * 1.0 / safeSize));
        return result;
    }

    private String json(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("JSON 格式错误");
        }
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    private String str(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }
}

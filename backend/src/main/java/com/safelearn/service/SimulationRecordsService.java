package com.safelearn.service;

import com.safelearn.deduction.service.DeductionSessionService;
import com.safelearn.testsim.service.TestSimulationService;
import com.safelearn.timeline.service.TimelineDeductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SimulationRecordsService {

    private final DeductionSessionService deductionSessionService;
    private final TimelineDeductionService timelineDeductionService;
    private final TestSimulationService testSimulationService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllUserRecords(String userId) {
        List<Map<String, Object>> records = new ArrayList<>();

        for (Map<String, Object> row : deductionSessionService.listUserSessions(userId)) {
            if (!"completed".equals(String.valueOf(row.get("status")))) continue;
            if (row.get("totalScore") == null) continue;
            Map<String, Object> item = new LinkedHashMap<>(row);
            item.put("type", "classic");
            records.add(item);
        }

        for (Map<String, Object> row : timelineDeductionService.listUserSessions(userId)) {
            if (row.get("totalScore") == null) continue;
            Map<String, Object> item = new LinkedHashMap<>(row);
            item.put("type", "timeline");
            if (item.get("scenarioName") == null) {
                item.put("scenarioName", "北京丰台 4·16 储能电站事故推演");
            }
            records.add(item);
        }

        for (Map<String, Object> row : testSimulationService.listUserSessions(userId)) {
            if (row.get("totalScore") == null) continue;
            Map<String, Object> item = new LinkedHashMap<>(row);
            item.put("type", "test");
            if (item.get("scenarioName") == null) {
                item.put("scenarioName", "广州智光储能科技「6·14」锂电池包闪爆事故推演");
            }
            records.add(item);
        }

        records.sort((a, b) -> recordTime(b).compareTo(recordTime(a)));
        return records;
    }

    private LocalDateTime recordTime(Map<String, Object> row) {
        Object finished = row.get("finishedAt");
        Object started = row.get("startedAt");
        String raw = finished != null ? String.valueOf(finished)
                : (started != null ? String.valueOf(started) : null);
        if (raw == null || raw.isBlank()) return LocalDateTime.MIN;
        try {
            return LocalDateTime.parse(raw.length() >= 19 ? raw.substring(0, 19) : raw);
        } catch (Exception e) {
            return LocalDateTime.MIN;
        }
    }
}

package com.safelearn.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class TrainingRecordInfo {
    private String id;
    private String scenarioId;
    private String scenarioName;
    private Integer totalScore;
    private String rating;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime completedAt;
    private List<Map<String, Object>> decisions;
    private String feedback;
}

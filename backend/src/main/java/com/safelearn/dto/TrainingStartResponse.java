package com.safelearn.dto;

import lombok.Data;
import java.util.Map;

@Data
public class TrainingStartResponse {
    private String recordId;
    private Map<String, Object> firstDecisionPoint;
}

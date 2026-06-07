package com.safelearn.dto;

import lombok.Data;
import java.util.Map;

@Data
public class TrainingDecisionResponse {
    private Integer score;
    private String consequence;
    private Integer totalScore;
    private Map<String, Object> nextDecisionPoint;
}

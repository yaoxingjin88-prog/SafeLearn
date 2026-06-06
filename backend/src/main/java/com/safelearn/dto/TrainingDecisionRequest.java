package com.safelearn.dto;

import lombok.Data;

@Data
public class TrainingDecisionRequest {
    private String recordId;
    private String decisionPointId;
    private String optionId;
    private Integer responseTime;
}

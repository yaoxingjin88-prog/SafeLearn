package com.safelearn.dto;

import lombok.Data;

@Data
public class FeedbackRequest {
    private String recordId;
    private Integer rating;
}

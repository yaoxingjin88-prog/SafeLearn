package com.safelearn.dto;

import lombok.Data;

@Data
public class ProgressRequest {
    private String courseId;
    private String chapterId;
    private Integer progress;
    private Boolean completed;
    private Integer masteryLevel;
}

package com.safelearn.dto;

import lombok.Data;

@Data
public class ProgressHeartbeatRequest {
    private String courseId;
    private String chapterId;
    /** 本次上报的有效学习秒数，通常 30 */
    private Integer elapsedSeconds;
    /** 当前阅读进度 0-100，可选 */
    private Integer progress;
}

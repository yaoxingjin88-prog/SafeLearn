package com.safelearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCourseRequest {
    @NotBlank(message = "课程标题不能为空")
    @Size(max = 200, message = "标题最长200个字符")
    private String title;

    @Size(max = 2000, message = "描述最长2000个字符")
    private String description;

    private String coverImage;
    private String category;
    private Integer totalDuration;
    private String status;
}

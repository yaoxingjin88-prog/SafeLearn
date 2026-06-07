package com.safelearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateChapterRequest {
    @NotBlank(message = "章节标题不能为空")
    @Size(max = 200, message = "标题最长200个字符")
    private String title;

    @Size(max = 10000, message = "内容最长10000个字符")
    private String content;

    private String videoUrl;
    private Integer duration;
    private Integer orderNum;
    private Integer difficultyLevel;
    private String prerequisiteIds;
    private String scenarioId;
}

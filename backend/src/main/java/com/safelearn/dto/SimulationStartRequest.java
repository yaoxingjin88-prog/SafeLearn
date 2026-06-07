package com.safelearn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SimulationStartRequest {
    @NotBlank(message = "场景ID不能为空")
    private String scenarioId;
}

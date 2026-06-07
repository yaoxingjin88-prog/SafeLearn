package com.safelearn.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ScenarioInfo {
    private String id;
    private String name;
    private String description;
    private Integer duration;
    private Integer difficulty;
    private String difficultyLabel;
    private List<String> prerequisiteIds;
    private Object initialConditions;
    private Object events;
    private Object decisionPoints;
    private Boolean unlocked;
}

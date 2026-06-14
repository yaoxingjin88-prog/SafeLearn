package com.safelearn.service;

import com.safelearn.dto.TrainingDecisionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class TrainingServiceDecisionTest {

    @Autowired
    private TrainingService trainingService;

    @Test
    void submitDecision_shouldNotThrow() {
        String userId = "660e8400-e29b-41d4-a716-446655440001";
        Map<String, Object> start = trainingService.startTraining(userId, "50000000-0000-0000-0000-000000000001");
        String recordId = String.valueOf(start.get("recordId"));
        assertNotNull(recordId);

        TrainingDecisionRequest req = new TrainingDecisionRequest();
        req.setRecordId(recordId);
        req.setDecisionPointId("bj_dp1");
        req.setOptionId("bj_dp1_a");
        req.setResponseTime(5);

        assertDoesNotThrow(() -> trainingService.submitDecision(userId, req));
    }
}

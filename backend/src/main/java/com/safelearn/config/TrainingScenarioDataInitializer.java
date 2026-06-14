package com.safelearn.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.Scenario;
import com.safelearn.repository.ScenarioRepository;
import com.safelearn.repository.TrainingRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 启动时同步应急决策训练场景（L1/L2/L3 分级 + 北京/广州专项案例）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingScenarioDataInitializer implements ApplicationRunner {

    private final ScenarioRepository scenarioRepo;
    private final TrainingRecordRepository trainingRecordRepo;
    private final ObjectMapper objectMapper;

    private record ScenarioSync(String scenarioId, String decisionsPath, String metaPath) {}

    private static final ScenarioSync[] GRADED_TARGETS = {
            new ScenarioSync("30000000-0000-0000-0000-000000000001", "data/shenzhen_l1_decisions.json", "data/shenzhen_l1_meta.json"),
            new ScenarioSync("30000000-0000-0000-0000-000000000002", "data/nanjing_l2_decisions.json", "data/nanjing_l2_meta.json"),
            new ScenarioSync("30000000-0000-0000-0000-000000000003", "data/gateway_l3_decisions.json", "data/gateway_l3_meta.json"),
    };

    private static final ScenarioSync[] EMERGENCY_TARGETS = {
            new ScenarioSync("50000000-0000-0000-0000-000000000001", "data/beijing416_decisions.json", "data/beijing416_meta.json"),
            new ScenarioSync("50000000-0000-0000-0000-000000000002", "data/guangzhou614_decisions.json", "data/guangzhou614_meta.json"),
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        removeTestScenarios();
        for (ScenarioSync target : GRADED_TARGETS) {
            syncScenario(target);
        }
        for (ScenarioSync target : EMERGENCY_TARGETS) {
            syncScenario(target, true);
        }
    }

    private void removeTestScenarios() {
        List<Scenario> junk = scenarioRepo.findAll().stream()
                .filter(s -> isTestScenarioName(s.getName()))
                .toList();
        for (Scenario scenario : junk) {
            try {
                long removedRecords = trainingRecordRepo.deleteByScenario_Id(scenario.getId());
                scenarioRepo.delete(scenario);
                log.info("已删除测试训练场景: {} ({})，关联训练记录 {} 条",
                        scenario.getName(), scenario.getId(), removedRecords);
            } catch (Exception e) {
                log.warn("删除测试训练场景失败: {} ({}) — {}",
                        scenario.getName(), scenario.getId(), e.getMessage());
            }
        }
    }

    private boolean isTestScenarioName(String name) {
        if (name == null || name.isBlank()) return true;
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return "test".equals(normalized)
                || normalized.startsWith("test_")
                || normalized.startsWith("test ");
    }

    private void syncScenario(ScenarioSync target) throws Exception {
        syncScenario(target, false);
    }

    private void syncScenario(ScenarioSync target, boolean createIfMissing) throws Exception {
        Optional<Scenario> optional = scenarioRepo.findById(target.scenarioId());
        Scenario scenario;
        if (optional.isEmpty()) {
            if (!createIfMissing) {
                log.warn("训练场景不存在，跳过同步: {}", target.scenarioId());
                return;
            }
            scenario = new Scenario();
            scenario.setId(target.scenarioId());
            scenario.setEvents("[]");
            scenario.setPrerequisiteIds(null);
            scenario.setCreatedAt(LocalDateTime.now());
        } else {
            scenario = optional.get();
        }

        Map<String, Object> meta = readMeta(target.metaPath());
        String decisionsJson = readResource(target.decisionsPath());
        scenario.setDecisionPoints(decisionsJson.trim());

        if (meta.get("name") instanceof String name) {
            scenario.setName(name);
        }
        if (meta.get("description") instanceof String desc) {
            scenario.setDescription(desc);
        }
        if (meta.get("duration") instanceof Number duration) {
            scenario.setDuration(duration.intValue());
        }
        if (meta.get("difficulty") instanceof Number difficulty) {
            scenario.setDifficulty(difficulty.intValue());
        }
        if (meta.get("initialConditions") instanceof Map<?, ?> ic) {
            @SuppressWarnings("unchecked")
            Map<String, Object> merged = mergeInitialConditions(scenario.getInitialConditions(), (Map<String, Object>) ic);
            if (meta.get("reportMeta") != null) {
                merged.put("trainingReportMeta", meta.get("reportMeta"));
            }
            scenario.setInitialConditions(objectMapper.writeValueAsString(merged));
        }

        scenarioRepo.save(scenario);
        log.info("已同步训练场景: {} ({})", scenario.getName(), target.scenarioId());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeInitialConditions(String existingJson, Map<String, Object> patch) throws Exception {
        Map<String, Object> base = new LinkedHashMap<>();
        if (existingJson != null && !existingJson.isBlank()) {
            base.putAll(objectMapper.readValue(existingJson, new TypeReference<Map<String, Object>>() {}));
        }
        base.putAll(patch);
        return base;
    }

    private Map<String, Object> readMeta(String path) throws Exception {
        String json = readResource(path);
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    private String readResource(String path) throws Exception {
        return StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
    }
}

package com.safelearn.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.ChapterQuiz;
import com.safelearn.entity.Question;
import com.safelearn.entity.QuestionCategory;
import com.safelearn.repository.ChapterQuizRepository;
import com.safelearn.repository.QuestionCategoryRepository;
import com.safelearn.repository.QuestionRepository;
import com.safelearn.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 启动时初始化题库分类，并从章节测验 JSON 导入题目。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionBankInitializer implements ApplicationRunner {

    private final QuestionCategoryRepository categoryRepo;
    private final QuestionRepository questionRepo;
    private final ChapterQuizRepository quizRepo;
    private final QuizAttemptRepository attemptRepo;
    private final ObjectMapper objectMapper;

    private static final String CAT_FIRE = "qc000001-0000-0000-0000-000000000001";
    private static final String CAT_FIRE_BASIC = "qc000002-0000-0000-0000-000000000001";
    private static final String CAT_FIRE_FACILITY = "qc000003-0000-0000-0000-000000000001";
    private static final String CAT_FIRE_EMERGENCY = "qc000004-0000-0000-0000-000000000001";
    private static final String CAT_FIRE_BATTERY = "qc000005-0000-0000-0000-000000000001";

    private static final String CAT_ELECTRIC = "qc000006-0000-0000-0000-000000000001";
    private static final String CAT_ELECTRIC_HV = "qc000011-0000-0000-0000-000000000001";
    private static final String CAT_ELECTRIC_LV = "qc000012-0000-0000-0000-000000000001";
    private static final String CAT_ELECTRIC_FIRE = "qc000013-0000-0000-0000-000000000001";

    private static final String CAT_EQUIPMENT = "qc000007-0000-0000-0000-000000000001";
    private static final String CAT_EQUIP_SYSTEM = "qc000021-0000-0000-0000-000000000001";
    private static final String CAT_EQUIP_BATTERY = "qc000022-0000-0000-0000-000000000001";
    private static final String CAT_EQUIP_MONITOR = "qc000023-0000-0000-0000-000000000001";

    private static final String CAT_INSPECTION = "qc000008-0000-0000-0000-000000000001";
    private static final String CAT_INSP_DAILY = "qc000031-0000-0000-0000-000000000001";
    private static final String CAT_INSP_MAINT = "qc000032-0000-0000-0000-000000000001";

    private static final String CAT_EMERGENCY = "qc000009-0000-0000-0000-000000000001";
    private static final String CAT_EMERG_RESPONSE = "qc000041-0000-0000-0000-000000000001";
    private static final String CAT_EMERG_RESCUE = "qc000042-0000-0000-0000-000000000001";

    private static final String CAT_REGULATION = "qc000010-0000-0000-0000-000000000001";
    private static final String CAT_REG_NATIONAL = "qc000051-0000-0000-0000-000000000001";
    private static final String CAT_REG_INDUSTRY = "qc000052-0000-0000-0000-000000000001";

    private static final List<String> LEAF_CATEGORIES = List.of(
            CAT_FIRE_BASIC, CAT_FIRE_FACILITY, CAT_FIRE_EMERGENCY, CAT_FIRE_BATTERY,
            CAT_ELECTRIC_HV, CAT_ELECTRIC_LV, CAT_ELECTRIC_FIRE,
            CAT_EQUIP_SYSTEM, CAT_EQUIP_BATTERY, CAT_EQUIP_MONITOR,
            CAT_INSP_DAILY, CAT_INSP_MAINT,
            CAT_EMERG_RESPONSE, CAT_EMERG_RESCUE,
            CAT_REG_NATIONAL, CAT_REG_INDUSTRY
    );

    private static final String[] DIFFICULTIES = {"easy", "medium", "hard"};
    private static final String[][] TAG_POOLS = {
            {"消防安全", "火灾应急处置"},
            {"消防安全", "消防设施"},
            {"储能安全", "电池热失控"},
            {"电气安全", "配电系统"},
            {"设备安全", "储能系统"},
            {"巡检规范", "运维管理"},
            {"应急处置", "事故响应"},
            {"法规标准", "合规要求"},
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategories();
        ensureSubCategories();
        importFromQuizzes();
    }

    private void seedCategories() {
        if (categoryRepo.count() > 0) return;

        saveCategory(CAT_FIRE, null, "消防安全", 1);
        saveCategory(CAT_FIRE_BASIC, CAT_FIRE, "火灾基础知识", 1);
        saveCategory(CAT_FIRE_FACILITY, CAT_FIRE, "消防设施与器材", 2);
        saveCategory(CAT_FIRE_EMERGENCY, CAT_FIRE, "火灾应急处置", 3);
        saveCategory(CAT_FIRE_BATTERY, CAT_FIRE, "电池热失控与消防", 4);

        saveCategory(CAT_ELECTRIC, null, "电气安全", 2);
        saveCategory(CAT_EQUIPMENT, null, "设备与系统安全", 3);
        saveCategory(CAT_INSPECTION, null, "巡检与维护规范", 4);
        saveCategory(CAT_EMERGENCY, null, "应急处置", 5);
        saveCategory(CAT_REGULATION, null, "法规与标准", 6);

        log.info("已初始化题库分类");
    }

    /** 为一级分类补充二级子分类，并将挂在根节点上的题目迁移到子分类。 */
    private void ensureSubCategories() {
        ensureChildren(CAT_ELECTRIC, List.of(
                new SubDef(CAT_ELECTRIC_HV, "高压配电安全", 1),
                new SubDef(CAT_ELECTRIC_LV, "低压用电规范", 2),
                new SubDef(CAT_ELECTRIC_FIRE, "电气防火措施", 3)
        ));
        ensureChildren(CAT_EQUIPMENT, List.of(
                new SubDef(CAT_EQUIP_SYSTEM, "储能系统组成", 1),
                new SubDef(CAT_EQUIP_BATTERY, "电池舱安全", 2),
                new SubDef(CAT_EQUIP_MONITOR, "监控与报警系统", 3)
        ));
        ensureChildren(CAT_INSPECTION, List.of(
                new SubDef(CAT_INSP_DAILY, "日常巡检要点", 1),
                new SubDef(CAT_INSP_MAINT, "维护保养规程", 2)
        ));
        ensureChildren(CAT_EMERGENCY, List.of(
                new SubDef(CAT_EMERG_RESPONSE, "事故应急响应", 1),
                new SubDef(CAT_EMERG_RESCUE, "疏散与救援", 2)
        ));
        ensureChildren(CAT_REGULATION, List.of(
                new SubDef(CAT_REG_NATIONAL, "国家标准", 1),
                new SubDef(CAT_REG_INDUSTRY, "行业规范", 2)
        ));
    }

    private void ensureChildren(String parentId, List<SubDef> subs) {
        boolean created = false;
        for (SubDef sub : subs) {
            if (!categoryRepo.existsById(sub.id())) {
                saveCategory(sub.id(), parentId, sub.name(), sub.sortOrder());
                created = true;
            }
        }
        List<String> leafIds = subs.stream().map(SubDef::id).toList();
        int migrated = migrateQuestionsFromRoot(parentId, leafIds);
        if (created) {
            log.info("已补全分类「{}」的 {} 个子分类", parentId, subs.size());
        }
        if (migrated > 0) {
            log.info("已将 {} 道题目从根分类迁移至子分类", migrated);
        }
    }

    private int migrateQuestionsFromRoot(String rootId, List<String> targetLeafIds) {
        if (targetLeafIds.isEmpty()) return 0;
        List<Question> direct = questionRepo.findByCategoryIdIn(List.of(rootId));
        int i = 0;
        for (Question q : direct) {
            q.setCategoryId(targetLeafIds.get(i % targetLeafIds.size()));
            questionRepo.save(q);
            i++;
        }
        return i;
    }

    private void saveCategory(String id, String parentId, String name, int sortOrder) {
        QuestionCategory cat = new QuestionCategory();
        cat.setId(id);
        cat.setParentId(parentId);
        cat.setName(name);
        cat.setSortOrder(sortOrder);
        categoryRepo.save(cat);
    }

    private void importFromQuizzes() {
        List<ChapterQuiz> quizzes = quizRepo.findAll();
        if (quizzes.isEmpty()) return;

        int imported = 0;
        int index = 0;
        for (ChapterQuiz quiz : quizzes) {
            try {
                List<Map<String, Object>> questions = objectMapper.readValue(
                        quiz.getQuestions(), new TypeReference<List<Map<String, Object>>>() {});
                int usage = (int) attemptRepo.countByQuizId(quiz.getId());
                for (Map<String, Object> raw : questions) {
                    String sourceKey = quiz.getId() + ":" + raw.get("id");
                    if (questionRepo.findBySourceQuestionKey(sourceKey).isPresent()) continue;

                    String categoryId = LEAF_CATEGORIES.get(index % LEAF_CATEGORIES.size());
                    index++;

                    Question q = new Question();
                    q.setCategoryId(categoryId);
                    q.setType(normalizeType(String.valueOf(raw.get("type"))));
                    q.setDifficulty(DIFFICULTIES[index % DIFFICULTIES.length]);
                    q.setStatus("published");
                    q.setContent(String.valueOf(raw.get("question")));
                    q.setOptions(objectMapper.writeValueAsString(raw.get("options")));
                    Object explanation = raw.get("explanation");
                    q.setExplanation(explanation != null ? String.valueOf(explanation) : null);
                    q.setTags(objectMapper.writeValueAsString(buildTags(categoryId, index)));
                    q.setUsageCount(Math.max(usage, 10 + (index * 7) % 200));
                    q.setSourceQuizId(quiz.getId());
                    q.setSourceQuestionKey(sourceKey);
                    questionRepo.save(q);
                    imported++;
                }
            } catch (Exception e) {
                log.warn("导入测验题目失败 quizId={}: {}", quiz.getId(), e.getMessage());
            }
        }

        if (imported > 0) {
            log.info("已从章节测验导入 {} 道题目", imported);
        }
        seedDemoQuestions();
    }

    private void seedDemoQuestions() {
        if (questionRepo.count() >= 80) return;

        String[][] demos = {
                {CAT_FIRE_EMERGENCY, "single", "medium", "储能电站发生火灾时，正确的初步处置措施是？", "消防安全", "火灾应急处置", "储能安全"},
                {CAT_FIRE_BATTERY, "multiple", "hard", "电池热失控的早期征兆包括哪些？（多选）", "电池热失控", "消防安全", "预警监测"},
                {CAT_ELECTRIC_LV, "truefalse", "easy", "储能系统检修前必须执行停电、验电、挂接地线等安全措施。", "电气安全", "检修规范"},
                {CAT_EQUIP_BATTERY, "short", "medium", "简述储能电池舱日常巡检应重点检查的项目。", "设备安全", "巡检规范"},
                {CAT_REG_INDUSTRY, "case", "hard", "某储能电站因运维不到位导致热失控事故，请分析主要原因及整改措施。", "法规标准", "事故案例"},
        };

        int base = (int) questionRepo.count();
        for (int i = 0; i < demos.length; i++) {
            String[] d = demos[i];
            String content = d[3];
            if (questionRepo.findAll().stream().anyMatch(q -> content.equals(q.getContent()))) continue;

            try {
                Question q = new Question();
                q.setCategoryId(d[0]);
                q.setType(d[1]);
                q.setDifficulty(d[2]);
                q.setStatus("published");
                q.setContent(content);
                q.setTags(objectMapper.writeValueAsString(List.of(d[4], d[5], d.length > 6 ? d[6] : d[5])));
                q.setUsageCount(50 + (base + i) * 3);
                if ("single".equals(d[1]) || "multiple".equals(d[1]) || "truefalse".equals(d[1])) {
                    q.setOptions(buildDemoOptions(d[1]));
                }
                q.setExplanation("请参考相关安全培训教材与操作规程。");
                questionRepo.save(q);
            } catch (Exception e) {
                log.warn("创建演示题目失败: {}", e.getMessage());
            }
        }
    }

    private String buildDemoOptions(String type) throws Exception {
        if ("truefalse".equals(type)) {
            return objectMapper.writeValueAsString(List.of(
                    Map.of("id", "a", "text", "正确", "correct", true),
                    Map.of("id", "b", "text", "错误", "correct", false)
            ));
        }
        if ("multiple".equals(type)) {
            return objectMapper.writeValueAsString(List.of(
                    Map.of("id", "a", "text", "温度异常升高", "correct", true),
                    Map.of("id", "b", "text", "气体浓度异常", "correct", true),
                    Map.of("id", "c", "text", "外观无任何变化", "correct", false),
                    Map.of("id", "d", "text", "电压波动异常", "correct", true)
            ));
        }
        return objectMapper.writeValueAsString(List.of(
                Map.of("id", "a", "text", "立即启动应急预案并报警", "correct", true),
                Map.of("id", "b", "text", "等待上级指示", "correct", false),
                Map.of("id", "c", "text", "自行灭火后汇报", "correct", false),
                Map.of("id", "d", "text", "继续观察不做处理", "correct", false)
        ));
    }

    private List<String> buildTags(String categoryId, int index) {
        int poolIndex = LEAF_CATEGORIES.indexOf(categoryId);
        if (poolIndex < 0) poolIndex = 0;
        String[] pool = TAG_POOLS[poolIndex % TAG_POOLS.length];
        return List.of(pool);
    }

    private String normalizeType(String type) {
        return switch (type) {
            case "single", "multiple", "truefalse", "short", "case" -> type;
            default -> "single";
        };
    }

    private record SubDef(String id, String name, int sortOrder) {}
}

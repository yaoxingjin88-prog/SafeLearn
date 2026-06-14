package com.safelearn.config;

import com.safelearn.entity.SystemConfig;
import com.safelearn.repository.SystemConfigRepository;
import com.safelearn.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时幂等写入系统配置默认值。
 * 逐项 existsByConfigKey 判断，保证后续新增配置项时能补种、且不覆盖管理员改过的值。
 */
@Slf4j
@Order(50)
@Component
@RequiredArgsConstructor
public class SystemConfigInitializer implements ApplicationRunner {

    private final SystemConfigRepository repo;
    private final SystemConfigService systemConfigService;

    private static final String DEFAULT_AI_PROMPT = """
            你是「储能安全 AI 助手」，专注于储能电站安全、锂电池热失控、消防安全、BMS、应急处置等领域。
            请遵循以下要求回答：
            1. 优先依据提供的【知识库参考资料】作答，资料不足时可结合储能安全行业通识补充，但需保证专业、准确。
            2. 回答使用简体中文，条理清晰，可使用分点说明，避免冗长空话。
            3. 涉及应急处置时，给出具体、可执行的步骤与安全注意事项。
            4. 若问题与储能/电池安全无关，礼貌引导用户回到相关主题。
            """;

    private static final String DEFAULT_RELATED_QUESTIONS =
            "[\"储能柜冒烟如何处理？\",\"热失控前兆有哪些？\",\"锂电池储能消防系统有哪些类型？\","
                    + "\"BMS系统的主要功能是什么？\",\"储能电站安全检查的重点是什么？\"]";

    @Override
    public void run(ApplicationArguments args) {
        int created = 0;

        // AI 问答
        created += seed("ai.enabled", "true", "BOOLEAN", "ai", "AI 问答开关",
                "关闭后问答走本地知识库回退", false, false, true, 1);
        created += seed("ai.model", "qwen-plus", "STRING", "ai", "AI 模型",
                "通义千问模型标识，如 qwen-plus / qwen-max", false, false, true, 2);
        created += seed("ai.apiKey", "", "STRING", "ai", "API Key",
                "留空则使用配置文件中的默认密钥", false, true, true, 3);
        created += seed("ai.systemPrompt", DEFAULT_AI_PROMPT.trim(), "TEXT", "ai", "系统提示词",
                "决定 AI 回答的角色与风格", false, false, true, 4);
        created += seed("ai.relatedQuestions", DEFAULT_RELATED_QUESTIONS, "JSON_LIST", "ai", "推荐问题",
                "用户端问答页展示的推荐问题列表", true, false, true, 5);

        // 学习规则
        created += seed("learning.masteryThreshold", "60", "INT", "learning", "掌握度解锁阈值(%)",
                "高级章节/训练解锁所需的掌握度百分比", true, false, true, 1);

        // 场景开放
        created += seed("scenario.hiddenIds", "[]", "JSON_LIST", "scenario", "隐藏场景",
                "被隐藏的推演场景 id 或名称列表，用户端不显示", false, false, true, 1);

        // 首页展示
        created += seed("dashboard.announcement", "", "TEXT", "dashboard", "首页公告",
                "用户端首页展示的公告文本，留空则不显示", true, false, true, 1);
        created += seed("dashboard.recommendBanner", "{}", "JSON", "dashboard", "推荐位",
                "首页推荐 banner 配置(JSON)", true, false, true, 2);
        created += seed("dashboard.showRecommendedCourses", "true", "BOOLEAN", "dashboard", "显示推荐课程",
                "是否在用户端首页展示推荐课程", true, false, true, 3);

        if (created > 0) {
            systemConfigService.reloadCache();
            log.info("系统配置初始化：新增 {} 项默认配置", created);
        }
    }

    private int seed(String key, String value, String type, String category, String label,
                     String description, boolean isPublic, boolean sensitive, boolean editable, int sort) {
        if (repo.existsByConfigKey(key)) return 0;
        SystemConfig c = new SystemConfig();
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setValueType(type);
        c.setCategory(category);
        c.setLabel(label);
        c.setDescription(description);
        c.setIsPublic(isPublic);
        c.setIsSensitive(sensitive);
        c.setEditable(editable);
        c.setSortOrder(sort);
        repo.save(c);
        return 1;
    }
}

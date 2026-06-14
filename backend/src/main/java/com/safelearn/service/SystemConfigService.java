package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.SystemConfig;
import com.safelearn.repository.SystemConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置中心：带内存缓存的类型化读取 + 默认值降级 + 写后立即生效。
 *
 * 读取策略：先查缓存 → miss 则查 DB 回填 → 仍无或解析失败 → 返回调用方传入的默认值
 * （默认值通常来自现有 @Value 注入或代码常量，从而保留 yml/常量的兜底能力）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository repo;
    private final ObjectMapper objectMapper;

    /** key → 原始字符串值（null 表示已查过但 DB 无此项）。 */
    private final Map<String, Optional<String>> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void warmUp() {
        try {
            reloadCache();
        } catch (Exception e) {
            // 启动期 DB 未就绪时忽略，后续懒加载会补
            log.debug("配置缓存预热跳过: {}", e.getMessage());
        }
    }

    /** 重新加载全部配置进缓存（管理端写入后调用，立即生效）。 */
    public void reloadCache() {
        Map<String, Optional<String>> fresh = new ConcurrentHashMap<>();
        for (SystemConfig c : repo.findAll()) {
            fresh.put(c.getConfigKey(), Optional.ofNullable(c.getConfigValue()));
        }
        cache.clear();
        cache.putAll(fresh);
    }

    /** 取原始字符串值；缓存 miss 时查库回填。返回 null 表示无此配置项。 */
    private String rawValue(String key) {
        Optional<String> cached = cache.get(key);
        if (cached != null) {
            return cached.orElse(null);
        }
        Optional<SystemConfig> found = repo.findByConfigKey(key);
        Optional<String> value = found.map(SystemConfig::getConfigValue);
        cache.put(key, value);
        return value.orElse(null);
    }

    // ---------- 类型化读取（带默认值降级） ----------

    public String getString(String key, String defaultValue) {
        String raw = rawValue(key);
        return (raw == null || raw.isBlank()) ? defaultValue : raw;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String raw = rawValue(key);
        if (raw == null || raw.isBlank()) return defaultValue;
        return "true".equalsIgnoreCase(raw.trim()) || "1".equals(raw.trim());
    }

    public int getInt(String key, int defaultValue) {
        String raw = rawValue(key);
        if (raw == null || raw.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public List<String> getStringList(String key, List<String> defaultValue) {
        String raw = rawValue(key);
        if (raw == null || raw.isBlank()) return defaultValue;
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Map<String, Object> getJson(String key, Map<String, Object> defaultValue) {
        String raw = rawValue(key);
        if (raw == null || raw.isBlank()) return defaultValue;
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // ---------- 管理端 ----------

    /** 全部配置（管理端列表），敏感项掩码。 */
    public List<Map<String, Object>> listAll() {
        return repo.findAllByOrderByCategoryAscSortOrderAsc().stream()
                .map(this::toAdminView)
                .toList();
    }

    /** 按分组列出配置。 */
    public List<Map<String, Object>> listByCategory(String category) {
        return repo.findByCategoryOrderBySortOrderAsc(category).stream()
                .map(this::toAdminView)
                .toList();
    }

    /** 更新配置值，仅 editable=true 可改，按类型校验，写后清缓存。 */
    public Map<String, Object> update(String id, Map<String, Object> data) {
        SystemConfig config = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("配置项不存在"));
        if (!Boolean.TRUE.equals(config.getEditable())) {
            throw new RuntimeException("该配置项不允许修改");
        }
        Object value = data.get("value");
        String normalized = normalizeValue(config.getValueType(), value);
        config.setConfigValue(normalized);
        repo.save(config);
        reloadCache();
        return toAdminView(config);
    }

    /** 用户端公开配置：仅 is_public 且非敏感，返回 {key: typedValue}。 */
    public Map<String, Object> listPublic() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (SystemConfig c : repo.findByIsPublicTrue()) {
            if (Boolean.TRUE.equals(c.getIsSensitive())) continue; // 双保险
            result.put(c.getConfigKey(), toTyped(c.getConfigValue(), c.getValueType()));
        }
        return result;
    }

    // ---------- 辅助 ----------

    private Map<String, Object> toAdminView(SystemConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("configKey", c.getConfigKey());
        boolean sensitive = Boolean.TRUE.equals(c.getIsSensitive());
        m.put("configValue", sensitive ? maskSensitive(c.getConfigValue()) : c.getConfigValue());
        m.put("valueType", c.getValueType());
        m.put("category", c.getCategory());
        m.put("label", c.getLabel());
        m.put("description", c.getDescription());
        m.put("isPublic", c.getIsPublic());
        m.put("isSensitive", sensitive);
        m.put("editable", c.getEditable());
        m.put("sortOrder", c.getSortOrder());
        return m;
    }

    /** 把原始字符串按类型转成 JSON 友好的值，用于公开接口下发。 */
    private Object toTyped(String raw, String type) {
        if (raw == null) return null;
        try {
            return switch (type) {
                case "BOOLEAN" -> "true".equalsIgnoreCase(raw.trim()) || "1".equals(raw.trim());
                case "INT" -> Integer.parseInt(raw.trim());
                case "JSON_LIST" -> objectMapper.readValue(raw, new TypeReference<List<Object>>() {});
                case "JSON" -> objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
                default -> raw; // STRING / TEXT
            };
        } catch (Exception e) {
            return raw;
        }
    }

    /** 校验并规范化写入值：JSON 类型确认可解析，原样存字符串。 */
    private String normalizeValue(String type, Object value) {
        if (value == null) return "";
        try {
            return switch (type) {
                case "BOOLEAN" -> String.valueOf(Boolean.parseBoolean(String.valueOf(value)));
                case "INT" -> String.valueOf(Integer.parseInt(String.valueOf(value).trim()));
                case "JSON_LIST", "JSON" -> {
                    // value 可能已是 List/Map（前端传 JSON），或字符串
                    if (value instanceof String s) {
                        objectMapper.readTree(s); // 校验合法 JSON
                        yield s;
                    }
                    yield objectMapper.writeValueAsString(value);
                }
                default -> String.valueOf(value); // STRING / TEXT
            };
        } catch (Exception e) {
            throw new RuntimeException("配置值格式不正确（类型 " + type + "）: " + e.getMessage());
        }
    }

    private String maskSensitive(String v) {
        if (v == null || v.isBlank()) return "";
        if (v.length() <= 6) return "****";
        return v.substring(0, 6) + "****";
    }
}

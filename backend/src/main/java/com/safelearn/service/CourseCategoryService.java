package com.safelearn.service;

import com.safelearn.entity.CourseCategory;
import com.safelearn.repository.CourseCategoryRepository;
import com.safelearn.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CourseCategoryService {

    private final CourseCategoryRepository categoryRepo;
    private final CourseRepository courseRepo;

    public List<Map<String, Object>> listAll() {
        return categoryRepo.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(this::toInfo)
                .toList();
    }

    public List<Map<String, Object>> listEnabled() {
        try {
            return categoryRepo.findEnabledOrdered().stream()
                    .map(this::toInfo)
                    .toList();
        } catch (Exception ex) {
            return categoryRepo.findAllByOrderBySortOrderAscNameAsc().stream()
                    .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                    .map(this::toInfo)
                    .toList();
        }
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> data) {
        String code = normalizeCode((String) data.get("code"));
        if (code == null || code.isBlank()) {
            throw new RuntimeException("分类编码不能为空");
        }
        if (categoryRepo.existsByCode(code)) {
            throw new RuntimeException("分类编码已存在");
        }
        CourseCategory category = new CourseCategory();
        applyFields(category, data, true);
        category.setCode(code);
        return toInfo(categoryRepo.save(category));
    }

    @Transactional
    public Map<String, Object> update(String id, Map<String, Object> data) {
        CourseCategory category = categoryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        if (data.containsKey("code")) {
            String code = normalizeCode((String) data.get("code"));
            if (code == null || code.isBlank()) {
                throw new RuntimeException("分类编码不能为空");
            }
            if (categoryRepo.existsByCodeAndIdNot(code, id)) {
                throw new RuntimeException("分类编码已存在");
            }
            String oldCode = category.getCode();
            if (!oldCode.equals(code)) {
                long used = courseRepo.countByCategory(oldCode);
                if (used > 0) {
                    throw new RuntimeException("该分类下仍有 " + used + " 门课程，无法修改编码");
                }
            }
            category.setCode(code);
        }
        applyFields(category, data, false);
        return toInfo(categoryRepo.save(category));
    }

    @Transactional
    public Map<String, Object> delete(String id) {
        CourseCategory category = categoryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        long used = courseRepo.countByCategory(category.getCode());
        if (used > 0) {
            throw new RuntimeException("该分类下仍有 " + used + " 门课程，无法删除");
        }
        categoryRepo.delete(category);
        return Map.of("success", true);
    }

    public Optional<Map<String, Object>> findByCode(String code) {
        return categoryRepo.findByCode(code).map(this::toInfo);
    }

    private void applyFields(CourseCategory category, Map<String, Object> data, boolean creating) {
        if (data.containsKey("name")) {
            String name = (String) data.get("name");
            if (name == null || name.isBlank()) throw new RuntimeException("分类名称不能为空");
            category.setName(name.trim());
        } else if (creating) {
            throw new RuntimeException("分类名称不能为空");
        }
        if (data.containsKey("tagType")) category.setTagType(nullToEmpty((String) data.get("tagType")));
        if (data.containsKey("sortOrder")) category.setSortOrder(toInteger(data.get("sortOrder"), 0));
        if (data.containsKey("enabled")) category.setEnabled(toBoolean(data.get("enabled"), true));
    }

    private String normalizeCode(String code) {
        if (code == null) return null;
        return code.trim().toLowerCase();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Integer toInteger(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(value.toString());
    }

    private boolean toBoolean(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }

    private Map<String, Object> toInfo(CourseCategory c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("code", c.getCode());
        m.put("name", c.getName());
        m.put("tagType", c.getTagType() != null ? c.getTagType() : "");
        m.put("sortOrder", c.getSortOrder());
        m.put("enabled", c.getEnabled());
        m.put("courseCount", courseRepo.countByCategory(c.getCode()));
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        m.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null);
        return m;
    }
}

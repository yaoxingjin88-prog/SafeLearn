package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.AdminRole;
import com.safelearn.repository.AdminRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<String> ACTIONS = List.of("view", "create", "edit", "delete", "export", "approve");

    private final AdminRoleRepository roleRepo;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRoles() {
        return roleRepo.findAllByOrderByRoleTypeAscNameAsc().stream()
                .map(this::toSummaryMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRole(String id) {
        AdminRole role = roleRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        return toDetailMap(role);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getModules() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("modules", permissionModules());
        result.put("actions", actionDefinitions());
        result.put("dataScopes", dataScopeOptions());
        return result;
    }

    @Transactional
    public Map<String, Object> createRole(Map<String, Object> body) {
        String code = requireText(body.get("code"), "角色编码不能为空");
        if (roleRepo.existsByCode(code)) {
            throw new RuntimeException("角色编码已存在");
        }
        AdminRole role = new AdminRole();
        role.setCode(code);
        role.setName(requireText(body.get("name"), "角色名称不能为空"));
        role.setRoleType("custom");
        role.setDescription(textOrEmpty(body.get("description")));
        role.setDataScope(textOrDefault(body.get("dataScope"), "all"));
        role.setCustomDeptIds(encodeJson(body.get("customDeptIds")));
        role.setPermissions(encodePermissions(body.get("permissions")));
        role.setStatus("draft");
        return toDetailMap(roleRepo.save(role));
    }

    @Transactional
    public Map<String, Object> updateRole(String id, Map<String, Object> body) {
        AdminRole role = roleRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        if (body.containsKey("name")) {
            role.setName(requireText(body.get("name"), "角色名称不能为空"));
        }
        if (body.containsKey("description")) {
            role.setDescription(textOrEmpty(body.get("description")));
        }
        if (body.containsKey("dataScope")) {
            role.setDataScope(textOrDefault(body.get("dataScope"), "all"));
        }
        if (body.containsKey("customDeptIds")) {
            role.setCustomDeptIds(encodeJson(body.get("customDeptIds")));
        }
        if (body.containsKey("permissions")) {
            role.setPermissions(encodePermissions(body.get("permissions")));
        }
        if ("published".equals(role.getStatus())) {
            role.setStatus("draft");
        }
        return toDetailMap(roleRepo.save(role));
    }

    @Transactional
    public Map<String, Object> publishRole(String id) {
        AdminRole role = roleRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        role.setStatus("published");
        Map<String, Object> result = toDetailMap(roleRepo.save(role));
        result.put("message", "权限已发布");
        return result;
    }

    @Transactional
    public Map<String, Object> deleteRole(String id) {
        AdminRole role = roleRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        if ("system".equals(role.getRoleType())) {
            throw new RuntimeException("系统内置角色不可删除");
        }
        roleRepo.delete(role);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    @Transactional
    public void ensureDefaultRoles() {
        seedRole(
                "a1000000-0000-4000-8000-000000000001",
                "ROLE_SUPER_ADMIN",
                "系统管理员",
                "system",
                "拥有平台全部功能与数据权限。",
                "all",
                allPermissions(true)
        );
        seedRole(
                "a1000000-0000-4000-8000-000000000002",
                "ROLE_TRAIN_ADMIN",
                "培训管理员",
                "system",
                "负责培训课程、考试题库、学习数据管理及相关报表查看。",
                "all",
                trainAdminPermissions()
        );
        seedRole(
                "a1000000-0000-4000-8000-000000000003",
                "ROLE_DEPT_ADMIN",
                "部门管理员",
                "system",
                "可查看本部门培训数据，管理本部门学员学习进度。",
                "department",
                deptAdminPermissions()
        );
    }

    private void seedRole(String id, String code, String name, String roleType,
                          String description, String dataScope,
                          Map<String, Map<String, Boolean>> permissions) {
        if (roleRepo.existsByCode(code)) {
            return;
        }
        AdminRole role = new AdminRole();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        role.setRoleType(roleType);
        role.setDescription(description);
        role.setDataScope(dataScope);
        role.setPermissions(encodePermissions(permissions));
        role.setStatus("published");
        roleRepo.save(role);
    }

    private Map<String, Map<String, Boolean>> trainAdminPermissions() {
        Map<String, Map<String, Boolean>> matrix = emptyMatrix();
        setModule(matrix, "dashboard", true, false, false, false, false, false);
        setModule(matrix, "training", true, true, true, true, true, false);
        setModule(matrix, "exam", true, true, true, true, true, false);
        setModule(matrix, "drill", true, false, false, false, false, true);
        setModule(matrix, "organization", true, false, false, false, false, false);
        setModule(matrix, "hazard", true, false, true, false, true, false);
        setModule(matrix, "report", true, false, false, false, true, false);
        setModule(matrix, "permission", true, false, false, false, false, false);
        return matrix;
    }

    private Map<String, Map<String, Boolean>> deptAdminPermissions() {
        Map<String, Map<String, Boolean>> matrix = emptyMatrix();
        setModule(matrix, "dashboard", true, false, false, false, false, false);
        setModule(matrix, "training", true, false, false, false, false, false);
        setModule(matrix, "exam", true, false, false, false, false, false);
        setModule(matrix, "drill", true, false, false, false, false, false);
        setModule(matrix, "organization", true, false, false, false, false, false);
        setModule(matrix, "hazard", true, false, false, false, false, false);
        setModule(matrix, "report", true, false, false, false, true, false);
        setModule(matrix, "permission", false, false, false, false, false, false);
        return matrix;
    }

    private Map<String, Map<String, Boolean>> allPermissions(boolean enabled) {
        Map<String, Map<String, Boolean>> matrix = emptyMatrix();
        for (String module : permissionModules().stream().map(m -> String.valueOf(m.get("code"))).toList()) {
            setModule(matrix, module, enabled, enabled, enabled, enabled, enabled, enabled);
        }
        return matrix;
    }

    private Map<String, Map<String, Boolean>> emptyMatrix() {
        Map<String, Map<String, Boolean>> matrix = new LinkedHashMap<>();
        for (Map<String, Object> module : permissionModules()) {
            String code = String.valueOf(module.get("code"));
            Map<String, Boolean> actions = new LinkedHashMap<>();
            for (String action : ACTIONS) {
                actions.put(action, false);
            }
            matrix.put(code, actions);
        }
        return matrix;
    }

    private void setModule(Map<String, Map<String, Boolean>> matrix, String code,
                           boolean view, boolean create, boolean edit,
                           boolean delete, boolean export, boolean approve) {
        Map<String, Boolean> actions = matrix.computeIfAbsent(code, key -> new LinkedHashMap<>());
        actions.put("view", view);
        actions.put("create", create);
        actions.put("edit", edit);
        actions.put("delete", delete);
        actions.put("export", export);
        actions.put("approve", approve);
    }

    private List<Map<String, Object>> permissionModules() {
        return List.of(
                module("dashboard", "首页总览", true, false),
                module("training", "培训课程", true, false),
                module("exam", "考试题库", true, false),
                module("drill", "应急演练", true, true),
                module("organization", "人员与组织", true, false),
                module("hazard", "隐患整改", true, false),
                module("report", "数据报表", true, false),
                module("permission", "权限设置", true, false)
        );
    }

    private Map<String, Object> module(String code, String name, boolean supportsApprove, boolean approveOnly) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("name", name);
        item.put("supportsApprove", supportsApprove);
        item.put("approveOnly", approveOnly);
        return item;
    }

    private List<Map<String, String>> actionDefinitions() {
        return List.of(
                action("view", "查看"),
                action("create", "新增"),
                action("edit", "编辑"),
                action("delete", "删除"),
                action("export", "导出"),
                action("approve", "审批")
        );
    }

    private Map<String, String> action(String code, String label) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("label", label);
        return item;
    }

    private List<Map<String, String>> dataScopeOptions() {
        return List.of(
                scope("all", "全部数据"),
                scope("department", "本部门数据"),
                scope("dept_tree", "本部门及下级部门数据"),
                scope("self", "仅本人数据"),
                scope("custom", "自定义数据范围")
        );
    }

    private Map<String, String> scope(String code, String label) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("label", label);
        return item;
    }

    private Map<String, Object> toSummaryMap(AdminRole role) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", role.getId());
        map.put("code", role.getCode());
        map.put("name", role.getName());
        map.put("roleType", role.getRoleType());
        map.put("status", role.getStatus());
        return map;
    }

    private Map<String, Object> toDetailMap(AdminRole role) {
        Map<String, Object> map = toSummaryMap(role);
        map.put("description", role.getDescription());
        map.put("dataScope", role.getDataScope());
        map.put("customDeptIds", decodeJsonList(role.getCustomDeptIds()));
        map.put("permissions", decodePermissions(role.getPermissions()));
        map.put("createdAt", role.getCreatedAt() != null ? role.getCreatedAt().format(DATE_TIME) : null);
        map.put("updatedAt", role.getUpdatedAt() != null ? role.getUpdatedAt().format(DATE_TIME) : null);
        map.put("roleTypeLabel", "system".equals(role.getRoleType()) ? "系统角色" : "自定义角色");
        map.put("statusLabel", "published".equals(role.getStatus()) ? "已发布" : "草稿");
        return map;
    }

    private Map<String, Map<String, Boolean>> decodePermissions(String json) {
        if (json == null || json.isBlank()) {
            return emptyMatrix();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            return emptyMatrix();
        }
    }

    private String encodePermissions(Object value) {
        if (value == null) {
            return encodePermissions(emptyMatrix());
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new RuntimeException("权限数据格式无效");
        }
    }

    private List<String> decodeJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String encodeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new RuntimeException("JSON 格式无效");
        }
    }

    private String requireText(Object value, String message) {
        String text = textOrEmpty(value);
        if (text.isBlank()) {
            throw new RuntimeException(message);
        }
        return text;
    }

    private String textOrDefault(Object value, String defaultValue) {
        String text = textOrEmpty(value);
        return text.isBlank() ? defaultValue : text;
    }

    private String textOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}

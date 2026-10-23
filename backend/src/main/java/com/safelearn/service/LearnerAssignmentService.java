package com.safelearn.service;

import com.safelearn.entity.LearnerAssignment;
import com.safelearn.entity.User;
import com.safelearn.repository.LearnerAssignmentRepository;
import com.safelearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LearnerAssignmentService {

    private final LearnerAssignmentRepository assignmentRepo;
    private final UserRepository userRepo;
    private final UserNotificationService userNotificationService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByUser(String userId) {
        return assignmentRepo.findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, "completed").stream()
                .map(this::toInfo)
                .toList();
    }

    @Transactional
    public Map<String, Object> create(String adminId, Map<String, Object> body) {
        String userId = String.valueOf(body.get("userId"));
        userRepo.findById(userId).orElseThrow(() -> new RuntimeException("学员不存在"));

        LearnerAssignment assignment = new LearnerAssignment();
        assignment.setUserId(userId);
        assignment.setTitle(String.valueOf(body.get("title")));
        assignment.setType(body.get("type") != null ? String.valueOf(body.get("type")) : "course");
        assignment.setLink(normalizeAssignmentLink(body.get("link") != null ? String.valueOf(body.get("link")) : null));
        assignment.setDeadline(body.get("deadline") != null ? String.valueOf(body.get("deadline")) : null);
        assignment.setStatus(body.get("status") != null ? String.valueOf(body.get("status")) : "pending");
        assignment.setActionText(body.get("actionText") != null ? String.valueOf(body.get("actionText")) : "去完成");
        assignment.setCreatedBy(adminId);
        assignmentRepo.save(assignment);
        userNotificationService.sendAssignmentNotice(
                userId, adminId, assignment.getTitle(), assignment.getLink());
        return toInfo(assignment);
    }

    @Transactional
    public Map<String, Object> batchCreate(String adminId, Map<String, Object> body) {
        List<String> userIds = parseUserIds(body.get("userIds"));
        if (userIds.isEmpty()) {
            throw new RuntimeException("请至少选择一名学员");
        }
        String title = body.get("title") != null ? String.valueOf(body.get("title")).trim() : "";
        if (title.isBlank()) {
            throw new RuntimeException("任务标题不能为空");
        }
        String type = body.get("type") != null ? String.valueOf(body.get("type")) : "course";
        String link = normalizeAssignmentLink(body.get("link") != null ? String.valueOf(body.get("link")) : null);
        String deadline = body.get("deadline") != null ? String.valueOf(body.get("deadline")) : null;
        String actionText = body.get("actionText") != null ? String.valueOf(body.get("actionText")) : "去完成";

        int created = 0;
        int skipped = 0;
        for (String userId : userIds) {
            User user = userRepo.findById(userId).orElse(null);
            if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
                skipped++;
                continue;
            }
            LearnerAssignment assignment = new LearnerAssignment();
            assignment.setUserId(userId);
            assignment.setTitle(title);
            assignment.setType(type);
            assignment.setLink(link);
            assignment.setDeadline(deadline);
            assignment.setStatus("pending");
            assignment.setActionText(actionText);
            assignment.setCreatedBy(adminId);
            assignmentRepo.save(assignment);
            userNotificationService.sendAssignmentNotice(userId, adminId, title, link);
            created++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("total", userIds.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseUserIds(Object raw) {
        if (raw == null) return List.of();
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(id -> !id.isBlank())
                    .distinct()
                    .toList();
        }
        return List.of();
    }

    @Transactional
    public Map<String, Object> updateStatus(String id, String status) {
        LearnerAssignment assignment = assignmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        assignment.setStatus(status);
        assignmentRepo.save(assignment);
        return toInfo(assignment);
    }

    @Transactional
    public void delete(String id) {
        if (!assignmentRepo.existsById(id)) {
            throw new RuntimeException("任务不存在");
        }
        assignmentRepo.deleteById(id);
    }

    private Map<String, Object> toInfo(LearnerAssignment assignment) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", assignment.getId());
        m.put("userId", assignment.getUserId());
        m.put("title", assignment.getTitle());
        m.put("type", assignment.getType());
        m.put("link", assignment.getLink());
        m.put("deadline", assignment.getDeadline());
        m.put("status", assignment.getStatus());
        m.put("actionText", assignment.getActionText());
        m.put("createdAt", assignment.getCreatedAt() != null ? assignment.getCreatedAt().toString() : null);
        userRepo.findById(assignment.getUserId()).ifPresent(u -> {
            m.put("username", u.getUsername());
            m.put("department", u.getDepartment());
        });
        return m;
    }

    private String normalizeAssignmentLink(String link) {
        if (link == null || link.isBlank()) return null;
        String trimmed = link.trim();
        if (trimmed.startsWith("/user/")) {
            return trimmed.substring("/user".length());
        }
        if (trimmed.startsWith("/user")) {
            return trimmed.substring(5);
        }
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}

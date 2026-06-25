package com.safelearn.service;

import com.safelearn.entity.AdminMessage;
import com.safelearn.entity.AdminNotification;
import com.safelearn.repository.AdminMessageRepository;
import com.safelearn.repository.AdminNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AdminNotificationRepository notificationRepo;
    private final AdminMessageRepository messageRepo;
    private final AdminInboxStreamService inboxStreamService;

    @Transactional
    public AdminNotification createNotification(String type, String level, String title,
                                                String content, String actionPath, String userId) {
        return createNotification(null, type, level, title, content, actionPath, userId);
    }

    @Transactional
    public AdminNotification createNotification(String sourceKey, String type, String level, String title,
                                                String content, String actionPath, String userId) {
        if (sourceKey != null && !sourceKey.isBlank() && notificationRepo.existsBySourceKey(sourceKey)) {
            return notificationRepo.findBySourceKey(sourceKey).orElseThrow();
        }
        AdminNotification item = new AdminNotification();
        item.setSourceKey(blankToNull(sourceKey));
        item.setType(type);
        item.setLevel(level);
        item.setTitle(title);
        item.setContent(content);
        item.setActionPath(actionPath);
        item.setUserId(userId);
        AdminNotification saved = notificationRepo.save(item);
        inboxStreamService.publishUpdate("notification");
        return saved;
    }

    @Transactional
    public AdminMessage createMessage(String type, String title, String body,
                                      String actionPath, boolean pinned, String receiverId) {
        return createMessage(null, type, title, body, actionPath, pinned, receiverId);
    }

    @Transactional
    public AdminMessage createMessage(String sourceKey, String type, String title, String body,
                                      String actionPath, boolean pinned, String receiverId) {
        if (sourceKey != null && !sourceKey.isBlank() && messageRepo.existsBySourceKey(sourceKey)) {
            return messageRepo.findBySourceKey(sourceKey).orElseThrow();
        }
        AdminMessage item = new AdminMessage();
        item.setSourceKey(blankToNull(sourceKey));
        item.setType(type);
        item.setTitle(title);
        item.setBody(body);
        item.setActionPath(actionPath);
        item.setPinned(pinned);
        item.setReceiverId(receiverId);
        AdminMessage saved = messageRepo.save(item);
        inboxStreamService.publishUpdate("message");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRecentNotifications(String userId, int limit) {
        return notificationRepo
                .findByUserIdIsNullOrUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
                .stream()
                .map(this::toNotificationMap)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRecentMessages(String receiverId, int limit) {
        return messageRepo
                .findByReceiverIdIsNullOrReceiverIdOrderByCreatedAtDesc(receiverId, PageRequest.of(0, limit))
                .stream()
                .map(this::toMessageMap)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countUnreadNotifications(String userId) {
        return notificationRepo.countByUserIdIsNullOrUserIdAndReadAtIsNull(userId);
    }

    @Transactional(readOnly = true)
    public long countUnreadMessages(String receiverId) {
        return messageRepo.countByReceiverIdIsNullOrReceiverIdAndReadAtIsNull(receiverId);
    }

    @Transactional
    public Map<String, Object> markNotificationRead(String id, String userId) {
        AdminNotification item = notificationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("通知不存在"));
        if (item.getUserId() != null && userId != null && !item.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该通知");
        }
        if (item.getReadAt() == null) {
            item.setReadAt(LocalDateTime.now());
            notificationRepo.save(item);
        }
        return toNotificationMap(item);
    }

    @Transactional
    public Map<String, Object> markMessageRead(String id, String receiverId) {
        AdminMessage item = messageRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        if (item.getReceiverId() != null && receiverId != null && !item.getReceiverId().equals(receiverId)) {
            throw new RuntimeException("无权操作该消息");
        }
        if (item.getReadAt() == null) {
            item.setReadAt(LocalDateTime.now());
            messageRepo.save(item);
        }
        return toMessageMap(item);
    }

    @Transactional
    public int markAllNotificationsRead(String userId) {
        return notificationRepo.markAllRead(userId, LocalDateTime.now());
    }

    @Transactional
    public int markAllMessagesRead(String receiverId) {
        return messageRepo.markAllRead(receiverId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMessage(String rawId) {
        String id = resolveMessageId(rawId);
        AdminMessage item = messageRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        return toMessageMap(item);
    }

    @Transactional
    public Map<String, Object> createManagedMessage(Map<String, Object> body) {
        String title = requireText(body.get("title"), "标题不能为空");
        String type = textOrDefault(body.get("type"), "announcement");
        String content = textOrDefault(body.get("body"), "");
        String actionPath = blankToNull(textOrDefault(body.get("actionPath"), ""));
        boolean pinned = booleanValue(body.get("pinned"), false);
        AdminMessage saved = createMessage(null, type, title, content, actionPath, pinned, null);
        return toMessageMap(saved);
    }

    @Transactional
    public Map<String, Object> updateMessage(String rawId, Map<String, Object> body) {
        String id = resolveMessageId(rawId);
        AdminMessage item = messageRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        if (body.containsKey("title")) {
            item.setTitle(requireText(body.get("title"), "标题不能为空"));
        }
        if (body.containsKey("body")) {
            item.setBody(textOrDefault(body.get("body"), ""));
        }
        if (body.containsKey("type")) {
            item.setType(textOrDefault(body.get("type"), "announcement"));
        }
        if (body.containsKey("pinned")) {
            item.setPinned(booleanValue(body.get("pinned"), false));
        }
        if (body.containsKey("actionPath")) {
            item.setActionPath(blankToNull(textOrDefault(body.get("actionPath"), "")));
        }
        AdminMessage saved = messageRepo.save(item);
        inboxStreamService.publishUpdate("message");
        return toMessageMap(saved);
    }

    @Transactional
    public Map<String, Object> deleteMessage(String rawId) {
        String id = resolveMessageId(rawId);
        if (!messageRepo.existsById(id)) {
            throw new RuntimeException("消息不存在");
        }
        messageRepo.deleteById(id);
        inboxStreamService.publishUpdate("message");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    public Map<String, Object> toNotificationMap(AdminNotification item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", "db:notification:" + item.getId());
        map.put("type", item.getType());
        map.put("title", item.getTitle());
        map.put("description", item.getContent());
        map.put("level", item.getLevel());
        map.put("time", item.getCreatedAt() != null ? item.getCreatedAt().format(DATE_TIME) : null);
        map.put("actionPath", item.getActionPath());
        map.put("read", item.getReadAt() != null);
        map.put("persisted", true);
        return map;
    }

    public Map<String, Object> toMessageMap(AdminMessage item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", "db:message:" + item.getId());
        map.put("title", item.getTitle());
        map.put("body", item.getBody());
        map.put("date", item.getCreatedAt() != null ? item.getCreatedAt().toLocalDate().toString() : null);
        map.put("pinned", Boolean.TRUE.equals(item.getPinned()));
        map.put("type", item.getType());
        map.put("actionPath", item.getActionPath());
        map.put("read", item.getReadAt() != null);
        map.put("persisted", true);
        return map;
    }

    public String resolveNotificationId(String rawId) {
        if (rawId == null) return null;
        return rawId.startsWith("db:notification:") ? rawId.substring("db:notification:".length()) : rawId;
    }

    public String resolveMessageId(String rawId) {
        if (rawId == null) return null;
        return rawId.startsWith("db:message:") ? rawId.substring("db:message:".length()) : rawId;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String requireText(Object value, String message) {
        String text = textOrDefault(value, "");
        if (text.isBlank()) {
            throw new RuntimeException(message);
        }
        return text.trim();
    }

    private String textOrDefault(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value).trim();
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value));
    }
}

package com.safelearn.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminInboxService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int SUMMARY_LIMIT = 30;

    private final AdminDashboardService dashboardService;
    private final AdminNotificationService notificationService;

    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationSummary() {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> items = buildMergedNotifications();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items.stream().limit(SUMMARY_LIMIT).toList());
        result.put("total", items.size());
        result.put("unreadCount", countUnreadNotifications(items));
        result.put("generatedAt", now.format(DATE_TIME));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMessageSummary() {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> items = buildMergedMessages();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items.stream().limit(SUMMARY_LIMIT).toList());
        result.put("total", items.size());
        result.put("unreadCount", countUnreadMessages(items));
        result.put("generatedAt", now.format(DATE_TIME));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listNotifications(int page, int pageSize, boolean unreadOnly) {
        List<Map<String, Object>> all = buildMergedNotifications();
        if (unreadOnly) {
            all = all.stream().filter(item -> !Boolean.TRUE.equals(item.get("read"))).toList();
        }
        return paginate(all, page, pageSize, false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listMessages(int page, int pageSize, boolean unreadOnly) {
        List<Map<String, Object>> all = buildMergedMessages();
        if (unreadOnly) {
            all = all.stream().filter(item -> !Boolean.TRUE.equals(item.get("read"))).toList();
        }
        return paginate(all, page, pageSize, true);
    }

    @Transactional
    public Map<String, Object> markNotificationRead(String rawId, String userId) {
        return notificationService.markNotificationRead(notificationService.resolveNotificationId(rawId), userId);
    }

    @Transactional
    public Map<String, Object> markMessageRead(String rawId, String receiverId) {
        return notificationService.markMessageRead(notificationService.resolveMessageId(rawId), receiverId);
    }

    @Transactional
    public Map<String, Object> markAllNotificationsRead(String userId) {
        notificationService.markAllNotificationsRead(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    @Transactional
    public Map<String, Object> markAllMessagesRead(String receiverId) {
        notificationService.markAllMessagesRead(receiverId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMessage(String id) {
        return notificationService.getMessage(id);
    }

    @Transactional
    public Map<String, Object> createMessage(Map<String, Object> body) {
        return notificationService.createManagedMessage(body);
    }

    @Transactional
    public Map<String, Object> updateMessage(String id, Map<String, Object> body) {
        return notificationService.updateMessage(id, body);
    }

    @Transactional
    public Map<String, Object> deleteMessage(String id) {
        return notificationService.deleteMessage(id);
    }

    private List<Map<String, Object>> buildMergedNotifications() {
        Map<String, Object> center = dashboardService.getAlertCenter();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> computed = ((List<Map<String, Object>>) center.get("items")).stream()
                .filter(item -> !"normal".equals(String.valueOf(item.get("type"))))
                .map(this::markComputedNotification)
                .toList();
        List<Map<String, Object>> persisted = notificationService.listRecentNotifications(null, 100);
        return mergeItems(persisted, computed, 200);
    }

    private List<Map<String, Object>> buildMergedMessages() {
        Map<String, Object> computedSummary = dashboardService.getMessageSummary();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> computed = ((List<Map<String, Object>>) computedSummary.get("items")).stream()
                .map(this::markComputedMessage)
                .toList();
        List<Map<String, Object>> persisted = notificationService.listRecentMessages(null, 100);
        return mergeItems(persisted, computed, 200);
    }

    private Map<String, Object> markComputedNotification(Map<String, Object> item) {
        Map<String, Object> copy = new LinkedHashMap<>(item);
        copy.putIfAbsent("read", false);
        copy.put("persisted", false);
        return copy;
    }

    private Map<String, Object> markComputedMessage(Map<String, Object> item) {
        Map<String, Object> copy = new LinkedHashMap<>(item);
        copy.putIfAbsent("read", false);
        copy.put("persisted", false);
        return copy;
    }

    private long countUnreadNotifications(List<Map<String, Object>> items) {
        return items.stream().filter(item -> !Boolean.TRUE.equals(item.get("read"))).count();
    }

    private long countUnreadMessages(List<Map<String, Object>> items) {
        return items.stream().filter(item -> !Boolean.TRUE.equals(item.get("read"))).count();
    }

    private Map<String, Object> paginate(List<Map<String, Object>> all, int page, int pageSize, boolean messages) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(pageSize, 100));
        int total = all.size();
        int from = (safePage - 1) * safeSize;
        int to = Math.min(total, from + safeSize);
        List<Map<String, Object>> items = from < total ? all.subList(from, to) : List.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", safePage);
        result.put("pageSize", safeSize);
        result.put("totalPages", safeSize == 0 ? 0 : (int) Math.ceil(total * 1.0 / safeSize));
        result.put("unreadCount", messages ? countUnreadMessages(all) : countUnreadNotifications(all));
        result.put("generatedAt", LocalDateTime.now().format(DATE_TIME));
        return result;
    }

    private List<Map<String, Object>> mergeItems(List<Map<String, Object>> primary,
                                                  List<Map<String, Object>> secondary,
                                                  int limit) {
        List<Map<String, Object>> merged = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        appendUnique(merged, seen, primary, limit);
        appendUnique(merged, seen, secondary, limit);
        return merged;
    }

    private void appendUnique(List<Map<String, Object>> target, Set<String> seen,
                              List<Map<String, Object>> source, int limit) {
        for (Map<String, Object> item : source) {
            if (target.size() >= limit) break;
            String key = dedupeKey(item);
            if (seen.add(key)) {
                target.add(item);
            }
        }
    }

    private String dedupeKey(Map<String, Object> item) {
        Object id = item.get("id");
        if (id != null && !String.valueOf(id).isBlank()) {
            return String.valueOf(id);
        }
        Object actionPath = item.get("actionPath");
        Object title = item.get("title");
        return String.valueOf(actionPath) + "|" + String.valueOf(title);
    }
}

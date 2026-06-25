package com.safelearn.service;

import com.safelearn.entity.Course;
import com.safelearn.entity.User;
import com.safelearn.entity.UserNotification;
import com.safelearn.repository.CourseRepository;
import com.safelearn.repository.UserNotificationRepository;
import com.safelearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserNotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;

    @Transactional
    public Map<String, Object> sendLearningReminder(String userId, String courseId, String senderId,
                                                    String warningStatus, Integer progress) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("学员不存在"));
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new RuntimeException("学员账号已停用，无法发送提醒");
        }

        String sourceKey = "learning-remind:" + userId + ":" + courseId + ":" + LocalDate.now();
        if (notificationRepo.existsBySourceKey(sourceKey)) {
            UserNotification existing = notificationRepo.findBySourceKey(sourceKey).orElseThrow();
            Map<String, Object> result = toMap(existing);
            result.put("duplicate", true);
            result.put("message", "今日已向该学员发送过该课程提醒");
            return result;
        }

        String title = "学习提醒：" + course.getTitle();
        String content = buildReminderContent(user.getUsername(), course.getTitle(), warningStatus, progress);
        UserNotification item = new UserNotification();
        item.setUserId(userId);
        item.setSenderId(senderId);
        item.setCourseId(courseId);
        item.setType("learning_reminder");
        item.setTitle(title);
        item.setContent(content);
        item.setActionPath("/user/courses/" + courseId);
        item.setSourceKey(sourceKey);
        UserNotification saved = notificationRepo.save(item);

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            log.info("[学习提醒] 学员: {} | 邮箱: {} | 课程: {} | 内容: {}",
                    user.getUsername(), user.getEmail(), course.getTitle(), content);
        }

        Map<String, Object> result = toMap(saved);
        result.put("duplicate", false);
        result.put("message", "提醒已发送");
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(String userId) {
        List<Map<String, Object>> items = notificationRepo
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 30))
                .stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", items.size());
        result.put("unreadCount", notificationRepo.countByUserIdAndReadAtIsNull(userId));
        result.put("generatedAt", LocalDateTime.now().format(DATE_TIME));
        return result;
    }

    @Transactional
    public Map<String, Object> markRead(String id, String userId) {
        UserNotification item = notificationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("通知不存在"));
        if (!item.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该通知");
        }
        if (item.getReadAt() == null) {
            item.setReadAt(LocalDateTime.now());
            notificationRepo.save(item);
        }
        return toMap(item);
    }

    @Transactional
    public Map<String, Object> markAllRead(String userId) {
        notificationRepo.markAllRead(userId, LocalDateTime.now());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    private String buildReminderContent(String username, String courseTitle,
                                        String warningStatus, Integer progress) {
        String reason = switch (warningStatus != null ? warningStatus : "") {
            case "exam_fail" -> "最近考试未达标";
            case "incomplete" -> "必修课程尚未完成";
            case "low_progress" -> "学习进度滞后";
            case "retake" -> "需参加补考或复训";
            case "not_started" -> "尚未开始必修课程";
            default -> "请及时完成学习";
        };
        int percent = progress != null ? progress : 0;
        return String.format("您好 %s，管理员提醒您：「%s」当前完成进度 %d%%，%s。请尽快登录平台继续学习。",
                username, courseTitle, percent, reason);
    }

    private Map<String, Object> toMap(UserNotification item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("type", item.getType());
        map.put("title", item.getTitle());
        map.put("content", item.getContent());
        map.put("courseId", item.getCourseId());
        map.put("actionPath", item.getActionPath());
        map.put("time", item.getCreatedAt() != null ? item.getCreatedAt().format(DATE_TIME) : null);
        map.put("read", item.getReadAt() != null);
        return map;
    }
}

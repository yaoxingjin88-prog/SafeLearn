package com.safelearn.service;

import com.safelearn.entity.Course;
import com.safelearn.entity.QuizAttempt;
import com.safelearn.entity.User;
import com.safelearn.entity.UserProgress;
import com.safelearn.repository.CourseRepository;
import com.safelearn.repository.QuizAttemptRepository;
import com.safelearn.repository.UserProgressRepository;
import com.safelearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInboxScheduler {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AdminNotificationService notificationService;
    private final UserRepository userRepo;
    private final UserProgressRepository progressRepo;
    private final CourseRepository courseRepo;
    private final QuizAttemptRepository quizAttemptRepo;

    /** 每日 8 点检查全局培训进度预警。 */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void dailyLowProgressCheck() {
        notifyGlobalLowProgress(LocalDate.now());
    }

    /** 每月 1 日 9 点汇总上月考试未通过人次。 */
    @Scheduled(cron = "0 0 9 1 * ?")
    @Transactional
    public void monthlyExamFailSummary() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDateTime start = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime end = lastMonth.plusMonths(1).atDay(1).atStartOfDay();
        long failCount = quizAttemptRepo.findAll().stream()
                .filter(item -> !Boolean.TRUE.equals(item.getPassed()))
                .filter(item -> item.getCompletedAt() != null)
                .filter(item -> !item.getCompletedAt().isBefore(start) && item.getCompletedAt().isBefore(end))
                .count();
        if (failCount <= 0) {
            return;
        }
        String monthKey = lastMonth.format(YEAR_MONTH);
        notificationService.createNotification(
                "exam-fail-month:" + monthKey,
                "exam",
                failCount >= 5 ? "danger" : "warning",
                "考试未通过汇总",
                "上月共 " + failCount + " 人次章节测验未通过",
                "/admin/learning/exams",
                null
        );
        log.info("Admin inbox: monthly exam fail summary {} = {}", monthKey, failCount);
    }

    @Transactional
    public void notifyGlobalLowProgress(LocalDate date) {
        List<User> learners = userRepo.findAll().stream()
                .filter(user -> "trainee".equalsIgnoreCase(user.getRole()))
                .toList();
        if (learners.isEmpty()) {
            return;
        }
        List<UserProgress> progress = progressRepo.findAll();
        Map<String, Double> averages = progress.stream().collect(Collectors.groupingBy(
                item -> item.getUser().getId(),
                Collectors.averagingInt(item -> Optional.ofNullable(item.getProgress()).orElse(0))
        ));
        long lowProgress = learners.stream()
                .filter(user -> averages.getOrDefault(user.getId(), 0.0) < 40)
                .count();
        if (lowProgress <= 0) {
            return;
        }
        String dateKey = date.toString();
        notificationService.createNotification(
                "progress-global:" + dateKey,
                "progress",
                "warning",
                "培训进度预警",
                lowProgress + " 名学员培训进度低于 40%",
                "/admin/learning/monitoring?warningStatus=low_progress",
                null
        );
        log.info("Admin inbox: global low progress {} learners on {}", lowProgress, dateKey);
    }

    @Transactional
    public void notifyCourseLowProgress(Course course, long warningCount) {
        if (course == null || warningCount <= 0) {
            return;
        }
        String dateKey = LocalDate.now().toString();
        String title = course.getTitle() != null ? course.getTitle() : "未命名课程";
        notificationService.createNotification(
                "progress-course:" + course.getId() + ":" + dateKey,
                "progress",
                "warning",
                "课程培训进度预警",
                warningCount + " 名学员在《" + title + "》中进度偏低",
                "/admin/learning/monitoring?warningStatus=low_progress",
                null
        );
    }

    @Transactional
    public void notifyQuizFail(QuizAttempt attempt, int score) {
        if (attempt == null || Boolean.TRUE.equals(attempt.getPassed()) || attempt.getUser() == null) {
            return;
        }
        User user = attempt.getUser();
        String username = user.getUsername() != null ? user.getUsername() : "学员";
        notificationService.createNotification(
                "exam-fail:" + attempt.getId(),
                "exam",
                "danger",
                "考试异常预警",
                username + " 章节测验未通过，得分 " + score,
                "/admin/learning/exams",
                null
        );
    }

    @Transactional
    public void notifyComprehensiveExamFail(String attemptId, String userId, int score) {
        userRepo.findById(userId).ifPresent(user -> {
            String username = user.getUsername() != null ? user.getUsername() : "学员";
            notificationService.createNotification(
                    "exam-fail-comprehensive:" + attemptId,
                    "exam",
                    "danger",
                    "综合考试未通过",
                    username + " 综合考试未通过，得分 " + score,
                    "/admin/learning/exams",
                    null
            );
        });
    }
}

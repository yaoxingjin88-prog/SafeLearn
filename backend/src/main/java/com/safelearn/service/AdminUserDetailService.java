package com.safelearn.service;

import com.safelearn.entity.*;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserDetailService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserRepository userRepo;
    private final UserProgressRepository progressRepo;
    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final QuizAttemptRepository quizAttemptRepo;
    private final UserCertificateRepository certificateRepo;
    private final ChapterQuizRepository chapterQuizRepo;

    @Transactional(readOnly = true)
    public Map<String, Object> getUserDetail(String userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        LocalDateTime now = LocalDateTime.now();
        List<Course> publishedCourses = courseRepo.findByStatusOrderByCreatedAtDesc("published");
        List<UserProgress> allProgress = progressRepo.findByUserIdWithDetails(userId);
        List<QuizAttempt> attempts = quizAttemptRepo.findByUserIdOrderByStartedAtDesc(userId);
        List<UserCertificate> certificates = certificateRepo.findByUserIdOrderByIssuedAtDesc(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profile", buildProfile(user));
        result.put("stats", buildStats(user, publishedCourses, allProgress, attempts, certificates, now));
        result.put("trainingArchive", buildTrainingArchive(userId, publishedCourses, allProgress, now));
        result.put("examSummary", buildExamSummary(attempts));
        result.put("certificateReminders", buildCertificateReminders(certificates, now));
        result.put("recentCourses", buildRecentCourses(userId, allProgress));
        result.put("warnings", buildWarnings(user, allProgress, attempts, certificates, now));
        return result;
    }

    @Transactional
    public Map<String, Object> updateUserTags(String userId, List<String> tags) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setTags(serializeTags(tags));
        userRepo.save(user);
        return Map.of("success", true, "tags", parseTags(user.getTags()));
    }

    private Map<String, Object> buildProfile(User user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("role", user.getRole());
        profile.put("enabled", user.getEnabled() == null || user.getEnabled());
        profile.put("employeeNo", resolveEmployeeNo(user));
        profile.put("department", user.getDepartment());
        profile.put("position", user.getPosition());
        profile.put("phone", user.getPhone());
        profile.put("phoneMasked", maskPhone(user.getPhone()));
        profile.put("email", user.getEmail());
        profile.put("avatarUrl", user.getAvatarUrl());
        profile.put("entryDate", user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate().format(DATE_FMT) : null);
        profile.put("accountSource", accountSourceLabel(user.getAccountSource()));
        profile.put("tags", parseTags(user.getTags()));
        return profile;
    }

    private Map<String, Object> buildStats(User user, List<Course> publishedCourses,
                                              List<UserProgress> allProgress, List<QuizAttempt> attempts,
                                              List<UserCertificate> certificates, LocalDateTime now) {
        long completedCourses = countCompletedCourses(user.getId(), publishedCourses);
        double avgScore = attempts.stream()
                .mapToInt(a -> a.getScore() != null ? a.getScore() : 0)
                .average().orElse(0);
        List<Map<String, Object>> warnings = buildWarnings(user, allProgress, attempts, certificates, now);
        long warningCount = warnings.stream().filter(w -> "pending".equals(w.get("status"))).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("completedCourses", completedCourses);
        stats.put("avgScore", round(avgScore, 1));
        stats.put("certificateCount", certificates.size());
        stats.put("warningCount", warningCount);
        return stats;
    }

    private Map<String, Object> buildTrainingArchive(String userId, List<Course> publishedCourses,
                                                      List<UserProgress> allProgress, LocalDateTime now) {
        int completed = 0;
        int inProgress = 0;
        int notStarted = 0;
        int expired = 0;

        for (Course course : publishedCourses) {
            List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId());
            if (chapters.isEmpty()) continue;

            List<UserProgress> courseProgress = allProgress.stream()
                    .filter(p -> p.getCourse() != null && course.getId().equals(p.getCourse().getId()))
                    .toList();
            long completedChapters = courseProgress.stream().filter(p -> Boolean.TRUE.equals(p.getCompleted())).count();
            boolean hasStarted = courseProgress.stream().anyMatch(p ->
                    Boolean.TRUE.equals(p.getCompleted()) || (p.getProgress() != null && p.getProgress() > 0));
            LocalDateTime lastAccess = courseProgress.stream()
                    .map(UserProgress::getLastAccessAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            if (completedChapters >= chapters.size()) {
                completed++;
            } else if (hasStarted) {
                if (lastAccess != null && lastAccess.isBefore(now.minusDays(90))) {
                    expired++;
                } else {
                    inProgress++;
                }
            } else if (course.getCreatedAt() != null && course.getCreatedAt().isBefore(now.minusDays(180))) {
                expired++;
            } else {
                notStarted++;
            }
        }

        int total = completed + inProgress + notStarted + expired;
        int progressRate = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
        if (progressRate == 0 && !allProgress.isEmpty()) {
            progressRate = (int) Math.round(allProgress.stream()
                    .mapToInt(p -> p.getProgress() != null ? p.getProgress() : 0)
                    .average().orElse(0));
        }

        Map<String, Object> archive = new LinkedHashMap<>();
        archive.put("progressRate", progressRate);
        archive.put("completed", completed);
        archive.put("inProgress", inProgress);
        archive.put("notStarted", notStarted);
        archive.put("expired", expired);
        return archive;
    }

    private Map<String, Object> buildExamSummary(List<QuizAttempt> attempts) {
        long attemptCount = attempts.size();
        double avgScore = attempts.stream()
                .mapToInt(a -> a.getScore() != null ? a.getScore() : 0)
                .average().orElse(0);
        long passedCount = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();
        double passRate = attemptCount == 0 ? 0 : round(passedCount * 100.0 / attemptCount, 1);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("avgScore", round(avgScore, 1));
        summary.put("attemptCount", attemptCount);
        summary.put("passRate", passRate);

        attempts.stream()
                .max(Comparator.comparing(a -> Optional.ofNullable(a.getCompletedAt()).orElse(a.getStartedAt())))
                .ifPresent(latest -> {
                    Map<String, Object> recent = new LinkedHashMap<>();
                    recent.put("title", resolveExamTitle(latest));
                    recent.put("score", latest.getScore() != null ? latest.getScore() : 0);
                    LocalDateTime time = Optional.ofNullable(latest.getCompletedAt()).orElse(latest.getStartedAt());
                    recent.put("date", time != null ? time.toLocalDate().format(DATE_FMT) : null);
                    summary.put("latestExam", recent);
                });

        return summary;
    }

    private List<Map<String, Object>> buildCertificateReminders(List<UserCertificate> certificates, LocalDateTime now) {
        return certificates.stream()
                .filter(c -> c.getExpiresAt() != null)
                .sorted(Comparator.comparing(UserCertificate::getExpiresAt))
                .limit(5)
                .map(cert -> {
                    long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), cert.getExpiresAt().toLocalDate());
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", cert.getId());
                    item.put("title", cert.getTitle());
                    item.put("expiresAt", cert.getExpiresAt().toLocalDate().format(DATE_FMT));
                    item.put("daysLeft", daysLeft);
                    item.put("urgency", daysLeft <= 30 ? "danger" : daysLeft <= 90 ? "warning" : "normal");
                    return item;
                })
                .toList();
    }

    private List<Map<String, Object>> buildRecentCourses(String userId, List<UserProgress> allProgress) {
        Map<String, List<UserProgress>> byCourse = allProgress.stream()
                .filter(p -> p.getCourse() != null)
                .collect(Collectors.groupingBy(p -> p.getCourse().getId()));

        return byCourse.entrySet().stream()
                .map(entry -> {
                    Course course = entry.getValue().get(0).getCourse();
                    List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId());
                    long completedChapters = entry.getValue().stream().filter(p -> Boolean.TRUE.equals(p.getCompleted())).count();
                    int progressPercent;
                    if (chapters.isEmpty()) {
                        progressPercent = (int) Math.round(entry.getValue().stream()
                                .mapToInt(p -> p.getProgress() != null ? p.getProgress() : 0)
                                .average().orElse(0));
                    } else if (completedChapters >= chapters.size()) {
                        progressPercent = 100;
                    } else {
                        progressPercent = (int) Math.round(completedChapters * 100.0 / chapters.size());
                    }
                    LocalDateTime completedAt = entry.getValue().stream()
                            .filter(p -> Boolean.TRUE.equals(p.getCompleted()))
                            .map(UserProgress::getLastAccessAt)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);
                    LocalDateTime lastAccess = entry.getValue().stream()
                            .map(UserProgress::getLastAccessAt)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", course.getId());
                    item.put("title", course.getTitle());
                    item.put("coverImage", course.getCoverImage());
                    item.put("progress", progressPercent);
                    item.put("completedAt", progressPercent >= 100 && completedAt != null
                            ? completedAt.toLocalDate().format(DATE_FMT) : null);
                    item.put("lastAccessAt", lastAccess);
                    return item;
                })
                .sorted(Comparator.comparing(
                        (Map<String, Object> m) -> (LocalDateTime) m.get("lastAccessAt"),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .peek(m -> m.remove("lastAccessAt"))
                .toList();
    }

    private List<Map<String, Object>> buildWarnings(User user, List<UserProgress> allProgress,
                                                     List<QuizAttempt> attempts,
                                                     List<UserCertificate> certificates,
                                                     LocalDateTime now) {
        List<Map<String, Object>> warnings = new ArrayList<>();

        for (UserCertificate cert : certificates) {
            if (cert.getExpiresAt() == null) continue;
            long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), cert.getExpiresAt().toLocalDate());
            if (daysLeft < 0) {
                warnings.add(warning("证书到期", cert.getTitle() + " 已过期", cert.getExpiresAt(), "processed"));
            } else if (daysLeft <= 30) {
                warnings.add(warning("证书到期", cert.getTitle() + " 将在 " + daysLeft + " 天后到期",
                        cert.getExpiresAt(), "pending"));
            }
        }

        double avgProgress = allProgress.stream()
                .mapToInt(p -> p.getProgress() != null ? p.getProgress() : 0)
                .average().orElse(0);
        if (avgProgress > 0 && avgProgress < 40) {
            warnings.add(warning("培训过期", "培训完成率低于 40%，需尽快补训", now.minusDays(7), "pending"));
        }

        attempts.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getPassed()))
                .max(Comparator.comparing(a -> Optional.ofNullable(a.getCompletedAt()).orElse(a.getStartedAt())))
                .ifPresent(failed -> {
                    LocalDateTime time = Optional.ofNullable(failed.getCompletedAt()).orElse(failed.getStartedAt());
                    warnings.add(warning("考试未通过", resolveExamTitle(failed) + " 未达及格线",
                            time != null ? time : now, "pending"));
                });

        if (warnings.isEmpty()) {
            warnings.add(warning("运行正常", "当前没有需要处理的风险预警", now, "processed"));
        }

        return warnings.stream()
                .sorted(Comparator.comparing(
                        (Map<String, Object> w) -> String.valueOf(w.get("time")),
                        Comparator.reverseOrder()))
                .limit(5)
                .toList();
    }

    private Map<String, Object> warning(String type, String content, LocalDateTime time, String status) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("content", content);
        item.put("time", time != null ? time.toLocalDate().format(DATE_FMT) : null);
        item.put("status", status);
        return item;
    }

    private String resolveExamTitle(QuizAttempt attempt) {
        if (attempt.getQuiz() == null) return "考试";
        ChapterQuiz quiz = attempt.getQuiz();
        if (quiz.getTitle() != null && !quiz.getTitle().isBlank()) {
            return quiz.getTitle();
        }
        return chapterQuizRepo.findById(quiz.getId()).map(ChapterQuiz::getTitle).orElse("考试");
    }

    private long countCompletedCourses(String userId, List<Course> courses) {
        return courses.stream().filter(course -> {
            List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId());
            if (chapters.isEmpty()) return false;
            return chapters.stream().allMatch(ch ->
                    progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId()));
        }).count();
    }

    private String resolveEmployeeNo(User user) {
        if (user.getEmployeeNo() != null && !user.getEmployeeNo().isBlank()) {
            return user.getEmployeeNo();
        }
        if (user.getUsername() != null && user.getUsername().matches("(?i)[A-Z]{0,2}\\d+")) {
            return user.getUsername().toUpperCase();
        }
        String id = user.getId() != null ? user.getId().replace("-", "") : "000000";
        return "GZ" + id.substring(0, Math.min(8, id.length())).toUpperCase();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return "—";
        String p = phone.trim();
        if (p.length() >= 11) {
            return p.substring(0, 3) + "****" + p.substring(p.length() - 4);
        }
        return p;
    }

    private String accountSourceLabel(String source) {
        if (source == null || source.isBlank() || "manual".equals(source)) return "手动创建";
        if ("import".equals(source)) return "内部导入";
        return source;
    }

    private List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.replace("[", "").replace("]", "").replace("\"", "");
        }
        if (trimmed.isBlank()) return new ArrayList<>();
        return Arrays.stream(trimmed.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return String.join(",", tags.stream().map(String::trim).filter(s -> !s.isBlank()).toList());
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}

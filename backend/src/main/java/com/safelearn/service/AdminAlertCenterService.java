package com.safelearn.service;

import com.safelearn.entity.*;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAlertCenterService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ALERT_NO_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final ChapterQuizRepository quizRepo;
    private final UserProgressRepository progressRepo;
    private final QuizAttemptRepository quizAttemptRepo;
    private final TrainingRecordRepository trainingRecordRepo;
    private final UserCertificateRepository certificateRepo;
    private final AdminTrainingAlertRepository manualAlertRepo;
    private final AdminAlertHandleRepository handleRepo;

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        List<Map<String, Object>> all = loadAllAlerts();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        long total = all.size();
        long totalYesterday = all.stream()
                .filter(item -> occurredOn(item, yesterday))
                .count();
        long examFail = countByType(all, "exam_fail", "exam_abnormal");
        long progressLag = countByType(all, "progress_lag", "course_incomplete", "task_overdue");
        long pending = all.stream().filter(item -> "pending".equals(item.get("status"))).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("totalDelta", total - totalYesterday);
        stats.put("examFailCount", examFail);
        stats.put("examFailRate", percent(examFail, total));
        stats.put("progressLagCount", progressLag);
        stats.put("progressLagRate", percent(progressLag, total));
        stats.put("pendingCount", pending);
        stats.put("pendingRate", percent(pending, total));
        stats.put("dangerCount", all.stream().filter(i -> "danger".equals(i.get("level"))).count());
        stats.put("warningCount", all.stream().filter(i -> "warning".equals(i.get("level"))).count());
        stats.put("infoCount", all.stream().filter(i -> "info".equals(i.get("level"))).count());
        stats.put("generatedAt", LocalDateTime.now().format(DATE_TIME));
        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> searchAlerts(String keyword, String level, String type, String department,
                                             String status, String dateFrom, String dateTo,
                                             int page, int pageSize) {
        List<Map<String, Object>> filtered = filterAlerts(loadAllAlerts(), keyword, level, type, department, status, dateFrom, dateTo);
        return paginate(filtered, page, pageSize);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAlertCenterLegacy() {
        List<Map<String, Object>> items = loadAllAlerts();
        Map<String, Object> stats = getStats();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items.stream().map(this::toLegacyItem).toList());
        result.put("total", items.size());
        result.put("dangerCount", stats.get("dangerCount"));
        result.put("warningCount", stats.get("warningCount"));
        result.put("infoCount", stats.get("infoCount"));
        result.put("stats", stats);
        result.put("departments", listDepartments(items));
        result.put("generatedAt", stats.get("generatedAt"));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAlert(String id) {
        return loadAllAlerts().stream()
                .filter(item -> id.equals(item.get("id")))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("预警不存在"));
    }

    @Transactional
    public Map<String, Object> createAlert(Map<String, Object> body) {
        AdminTrainingAlert alert = new AdminTrainingAlert();
        alert.setAlertNo(nextAlertNo());
        applyManualFields(alert, body);
        if (alert.getOccurredAt() == null) {
            alert.setOccurredAt(LocalDateTime.now());
        }
        if (alert.getStatus() == null || alert.getStatus().isBlank()) {
            alert.setStatus("pending");
        }
        manualAlertRepo.save(alert);
        return toManualMap(alert);
    }

    @Transactional
    public Map<String, Object> updateAlert(String id, Map<String, Object> body) {
        AdminTrainingAlert alert = manualAlertRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("仅支持编辑手动录入的预警"));
        applyManualFields(alert, body);
        manualAlertRepo.save(alert);
        return getAlert("manual:" + alert.getId());
    }

    @Transactional
    public Map<String, Object> updateStatus(String id, Map<String, Object> body) {
        String status = String.valueOf(body.getOrDefault("status", "pending"));
        String responsiblePerson = body.containsKey("responsiblePerson")
                ? String.valueOf(body.get("responsiblePerson")) : null;
        String remark = body.containsKey("remark") ? String.valueOf(body.get("remark")) : null;

        if (id.startsWith("manual:")) {
            AdminTrainingAlert alert = manualAlertRepo.findById(id.substring("manual:".length()))
                    .orElseThrow(() -> new RuntimeException("预警不存在"));
            alert.setStatus(status);
            if (responsiblePerson != null && !responsiblePerson.isBlank()) {
                alert.setResponsiblePerson(responsiblePerson);
            }
            manualAlertRepo.save(alert);
        }

        AdminAlertHandle handle = handleRepo.findByAlertKey(id).orElseGet(AdminAlertHandle::new);
        handle.setAlertKey(id);
        handle.setStatus(status);
        if (responsiblePerson != null && !responsiblePerson.isBlank()) {
            handle.setResponsiblePerson(responsiblePerson);
        }
        if (remark != null) {
            handle.setRemark(remark);
        }
        handleRepo.save(handle);
        return getAlert(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> exportAlerts(String keyword, String level, String type, String department,
                                                  String status, String dateFrom, String dateTo) {
        return filterAlerts(loadAllAlerts(), keyword, level, type, department, status, dateFrom, dateTo);
    }

    private List<Map<String, Object>> loadAllAlerts() {
        LocalDateTime now = LocalDateTime.now();
        List<User> learners = userRepo.findAll().stream()
                .filter(user -> !"admin".equalsIgnoreCase(user.getRole()))
                .toList();
        Map<String, Chapter> chapters = chapterRepo.findAll().stream()
                .collect(Collectors.toMap(Chapter::getId, Function.identity(), (a, b) -> a));
        Map<String, Course> courses = courseRepo.findAll().stream()
                .collect(Collectors.toMap(Course::getId, Function.identity(), (a, b) -> a));
        Map<String, ChapterQuiz> quizzes = quizRepo.findAll().stream()
                .collect(Collectors.toMap(ChapterQuiz::getId, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> items = new ArrayList<>();
        items.addAll(buildExamFailAlerts(quizAttemptRepo.findAll(), quizzes, chapters, courses));
        items.addAll(buildProgressAlerts(learners, progressRepo.findAll(), chapters, courses, now));
        items.addAll(buildTrainingAlerts(trainingRecordRepo.findAll()));
        items.addAll(buildCertificateAlerts(certificateRepo.findAll(), courses, now));
        items.addAll(buildNotStartedAlerts(learners, progressRepo.findAll(), courses, chapterRepo.findAll(), now));
        items.addAll(manualAlertRepo.findAll().stream().map(this::toManualMap).toList());

        items.sort(Comparator.comparing(
                (Map<String, Object> item) -> parseTime(String.valueOf(item.get("time"))),
                Comparator.nullsLast(Comparator.reverseOrder())));

        assignAlertNos(items);
        applyHandles(items);
        return items;
    }

    private List<Map<String, Object>> buildExamFailAlerts(List<QuizAttempt> attempts,
                                                            Map<String, ChapterQuiz> quizzes,
                                                            Map<String, Chapter> chapters,
                                                            Map<String, Course> courses) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (QuizAttempt attempt : attempts) {
            if (Boolean.TRUE.equals(attempt.getPassed()) || attempt.getUser() == null || attempt.getQuiz() == null) {
                continue;
            }
            LocalDateTime time = Optional.ofNullable(attempt.getCompletedAt()).orElse(attempt.getStartedAt());
            if (time == null) continue;

            User user = attempt.getUser();
            ChapterQuiz quiz = quizzes.getOrDefault(attempt.getQuiz().getId(), attempt.getQuiz());
            Chapter chapter = chapters.get(quiz.getChapterId());
            Course course = null;
            if (chapter != null && chapter.getCourse() != null) {
                course = chapter.getCourse();
            }
            String courseTitle = course != null ? course.getTitle()
                    : (chapter != null ? chapter.getTitle() : quiz.getTitle());
            int score = attempt.getScore() != null ? attempt.getScore() : 0;
            String level = score < 50 ? "danger" : "warning";
            String username = displayName(user);

            Map<String, Object> item = baseAlert(
                    "exam-fail:" + attempt.getId(),
                    "exam_fail",
                    level,
                    "考试不合格预警：" + username + "《" + courseTitle + "》成绩" + score + "分",
                    username + " 在《" + courseTitle + "》测验中得分为 " + score + " 分，未达到合格线。",
                    user.getDepartment(),
                    "安全培训管理员",
                    username,
                    courseTitle,
                    time,
                    "/admin/learning/exams",
                    false
            );
            items.add(item);
        }
        return items;
    }

    private List<Map<String, Object>> buildProgressAlerts(List<User> learners, List<UserProgress> progressList,
                                                           Map<String, Chapter> chapters,
                                                           Map<String, Course> courses, LocalDateTime now) {
        Map<String, List<UserProgress>> byUserCourse = progressList.stream()
                .filter(p -> p.getUser() != null && p.getCourse() != null)
                .collect(Collectors.groupingBy(p -> p.getUser().getId() + ":" + p.getCourse().getId()));

        List<Map<String, Object>> items = new ArrayList<>();
        for (User user : learners) {
            for (Course course : courses.values()) {
                if (!"published".equals(course.getStatus())) continue;
                List<Chapter> courseChapters = chapters.values().stream()
                        .filter(ch -> ch.getCourse() != null && course.getId().equals(ch.getCourse().getId()))
                        .toList();
                if (courseChapters.isEmpty()) continue;

                List<UserProgress> rows = byUserCourse.getOrDefault(user.getId() + ":" + course.getId(), List.of());
                if (rows.isEmpty()) continue;
                int avg = (int) Math.round(rows.stream()
                        .mapToInt(p -> Optional.ofNullable(p.getProgress()).orElse(0))
                        .average().orElse(0));
                if (avg >= 50) continue;

                LocalDateTime time = rows.stream()
                        .map(UserProgress::getLastAccessAt)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(now);
                String level = avg < 20 ? "danger" : "warning";
                String username = displayName(user);

                items.add(baseAlert(
                        "progress-lag:" + user.getId() + ":" + course.getId(),
                        "progress_lag",
                        level,
                        "培训进度滞后：" + username + "《" + course.getTitle() + "》进度" + avg + "%",
                        username + " 在必修课程《" + course.getTitle() + "》中的学习进度为 " + avg + "%，低于计划要求。",
                        user.getDepartment(),
                        "安全培训管理员",
                        username,
                        course.getTitle(),
                        time,
                        "/admin/learning/monitoring?warningStatus=low_progress",
                        false
                ));
            }
        }
        return items;
    }

    private List<Map<String, Object>> buildTrainingAlerts(List<TrainingRecord> records) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (TrainingRecord record : records) {
            if (record.getUser() == null || record.getTotalScore() == null || record.getTotalScore() >= 60) {
                continue;
            }
            LocalDateTime time = Optional.ofNullable(record.getEndTime()).orElse(record.getStartTime());
            if (time == null) continue;
            User user = record.getUser();
            String username = displayName(user);
            items.add(baseAlert(
                    "training-low:" + record.getId(),
                    "exam_abnormal",
                    "warning",
                    "应急训练低分预警：" + username + " 得分" + record.getTotalScore(),
                    username + " 应急训练得分 " + record.getTotalScore() + " 分，建议安排复训。",
                    user.getDepartment(),
                    "安全培训管理员",
                    username,
                    "应急训练",
                    time,
                    "/admin/learning/monitoring",
                    false
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildCertificateAlerts(List<UserCertificate> certificates,
                                                              Map<String, Course> courses, LocalDateTime now) {
        Map<String, User> users = userRepo.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        List<Map<String, Object>> items = new ArrayList<>();
        for (UserCertificate cert : certificates) {
            if (cert.getExpiresAt() == null) continue;
            if (cert.getExpiresAt().isBefore(now) || cert.getExpiresAt().isAfter(now.plusDays(30))) continue;

            User user = users.get(cert.getUserId());
            if (user == null) continue;
            Course course = courses.get(cert.getCourseId());
            String courseTitle = cert.getTitle() != null ? cert.getTitle()
                    : (course != null ? course.getTitle() : "安全证书");
            long days = java.time.Duration.between(now, cert.getExpiresAt()).toDays();
            String level = days <= 7 ? "danger" : "info";
            String username = displayName(user);

            items.add(baseAlert(
                    "cert-expire:" + cert.getId(),
                    "certificate_expire",
                    level,
                    "证书即将到期：" + username + "《" + (courseTitle != null ? courseTitle : "安全证书") + "》",
                    username + " 的安全证书将在 " + cert.getExpiresAt().toLocalDate() + " 到期，请及时安排复训。",
                    user.getDepartment(),
                    "安全培训管理员",
                    username,
                    courseTitle,
                    cert.getExpiresAt(),
                    "/admin/org",
                    false
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildNotStartedAlerts(List<User> learners, List<UserProgress> progressList,
                                                             Map<String, Course> courses, List<Chapter> allChapters,
                                                             LocalDateTime now) {
        Set<String> startedKeys = progressList.stream()
                .filter(p -> p.getUser() != null && p.getCourse() != null)
                .map(p -> p.getUser().getId() + ":" + p.getCourse().getId())
                .collect(Collectors.toSet());

        Map<String, Long> chapterCountByCourse = allChapters.stream()
                .filter(ch -> ch.getCourse() != null)
                .collect(Collectors.groupingBy(ch -> ch.getCourse().getId(), Collectors.counting()));

        List<Map<String, Object>> items = new ArrayList<>();
        for (User user : learners) {
            for (Course course : courses.values()) {
                if (!"published".equals(course.getStatus())) continue;
                if (chapterCountByCourse.getOrDefault(course.getId(), 0L) <= 0) continue;
                String key = user.getId() + ":" + course.getId();
                if (startedKeys.contains(key)) continue;

                String username = displayName(user);
                items.add(baseAlert(
                        "not-started:" + key,
                        "task_overdue",
                        "warning",
                        "学习任务未开始：" + username + "《" + course.getTitle() + "》",
                        username + " 尚未开始必修课程《" + course.getTitle() + "》的学习。",
                        user.getDepartment(),
                        "安全培训管理员",
                        username,
                        course.getTitle(),
                        now.minusDays(1),
                        "/admin/learning/monitoring?warningStatus=not_started",
                        false
                ));
            }
        }
        return items.stream().limit(20).toList();
    }

    private Map<String, Object> baseAlert(String id, String type, String level, String title, String description,
                                           String department, String responsiblePerson, String traineeName,
                                           String courseName, LocalDateTime time, String actionPath, boolean manual) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("type", type);
        item.put("level", level);
        item.put("title", title);
        item.put("description", description);
        item.put("department", department != null && !department.isBlank() ? department : "未分配部门");
        item.put("responsiblePerson", responsiblePerson != null ? responsiblePerson : "安全培训管理员");
        item.put("traineeName", traineeName);
        item.put("courseName", courseName);
        item.put("time", time != null ? time.format(DATE_TIME) : null);
        item.put("status", "pending");
        item.put("actionPath", actionPath);
        item.put("manual", manual);
        return item;
    }

    private Map<String, Object> toManualMap(AdminTrainingAlert alert) {
        Map<String, Object> item = baseAlert(
                "manual:" + alert.getId(),
                alert.getType(),
                alert.getLevel(),
                alert.getTitle(),
                alert.getDescription(),
                alert.getDepartment(),
                alert.getResponsiblePerson(),
                alert.getTraineeName(),
                alert.getCourseName(),
                alert.getOccurredAt(),
                alert.getActionPath() != null ? alert.getActionPath() : "/dashboard/alerts",
                true
        );
        item.put("alertNo", alert.getAlertNo());
        item.put("status", alert.getStatus());
        return item;
    }

    private Map<String, Object> toLegacyItem(Map<String, Object> item) {
        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("id", item.get("id"));
        legacy.put("type", mapLegacyType(String.valueOf(item.get("type"))));
        legacy.put("title", item.get("title"));
        legacy.put("description", item.get("description"));
        legacy.put("level", item.get("level"));
        legacy.put("time", item.get("time"));
        legacy.put("actionPath", item.get("actionPath"));
        return legacy;
    }

    private String mapLegacyType(String type) {
        return switch (type) {
            case "exam_fail", "exam_abnormal" -> "exam";
            case "progress_lag", "course_incomplete", "task_overdue" -> "progress";
            case "certificate_expire" -> "certificate";
            default -> type;
        };
    }

    private void assignAlertNos(List<Map<String, Object>> items) {
        String day = LocalDate.now().format(ALERT_NO_DAY);
        int index = 1;
        for (Map<String, Object> item : items) {
            if (item.containsKey("alertNo")) continue;
            item.put("alertNo", "TW" + day + String.format("%03d", index++));
        }
    }

    private void applyHandles(List<Map<String, Object>> items) {
        List<String> keys = items.stream().map(item -> String.valueOf(item.get("id"))).toList();
        Map<String, AdminAlertHandle> handles = handleRepo.findByAlertKeyIn(keys).stream()
                .collect(Collectors.toMap(AdminAlertHandle::getAlertKey, Function.identity(), (a, b) -> a));
        for (Map<String, Object> item : items) {
            AdminAlertHandle handle = handles.get(String.valueOf(item.get("id")));
            if (handle == null) continue;
            item.put("status", handle.getStatus());
            if (handle.getResponsiblePerson() != null && !handle.getResponsiblePerson().isBlank()) {
                item.put("responsiblePerson", handle.getResponsiblePerson());
            }
            if (handle.getRemark() != null) {
                item.put("remark", handle.getRemark());
            }
        }
    }

    private List<Map<String, Object>> filterAlerts(List<Map<String, Object>> items, String keyword, String level,
                                                    String type, String department, String status,
                                                    String dateFrom, String dateTo) {
        String q = keyword != null ? keyword.trim().toLowerCase() : "";
        LocalDateTime from = parseDateStart(dateFrom);
        LocalDateTime to = parseDateEnd(dateTo);

        return items.stream().filter(item -> {
            if (level != null && !level.isBlank() && !"all".equals(level) && !level.equals(item.get("level"))) {
                return false;
            }
            if (type != null && !type.isBlank() && !"all".equals(type) && !type.equals(item.get("type"))) {
                return false;
            }
            if (department != null && !department.isBlank() && !"all".equals(department)
                    && !department.equals(item.get("department"))) {
                return false;
            }
            if (status != null && !status.isBlank() && !"all".equals(status) && !status.equals(item.get("status"))) {
                return false;
            }
            if (!q.isEmpty()) {
                String text = String.join(" ",
                        String.valueOf(item.get("alertNo")),
                        String.valueOf(item.get("title")),
                        String.valueOf(item.get("description")),
                        String.valueOf(item.get("traineeName")),
                        String.valueOf(item.get("courseName")),
                        String.valueOf(item.get("department"))
                ).toLowerCase();
                if (!text.contains(q)) return false;
            }
            if (from != null || to != null) {
                LocalDateTime time = parseTime(String.valueOf(item.get("time")));
                if (time == null) return false;
                if (from != null && time.isBefore(from)) return false;
                if (to != null && time.isAfter(to)) return false;
            }
            return true;
        }).toList();
    }

    private Map<String, Object> paginate(List<Map<String, Object>> items, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(pageSize, 100));
        int total = items.size();
        int from = (safePage - 1) * safeSize;
        int to = Math.min(total, from + safeSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", from < total ? items.subList(from, to) : List.of());
        result.put("total", total);
        result.put("page", safePage);
        result.put("pageSize", safeSize);
        result.put("totalPages", safeSize == 0 ? 0 : (int) Math.ceil(total * 1.0 / safeSize));
        result.put("stats", getStats());
        result.put("departments", listDepartments(items));
        result.put("generatedAt", LocalDateTime.now().format(DATE_TIME));
        return result;
    }

    private List<String> listDepartments(List<Map<String, Object>> items) {
        LinkedHashSet<String> departments = new LinkedHashSet<>();
        departments.add("all");
        items.stream()
                .map(item -> String.valueOf(item.get("department")))
                .filter(dep -> dep != null && !dep.isBlank() && !"null".equals(dep))
                .sorted()
                .forEach(departments::add);
        return new ArrayList<>(departments);
    }

    private void applyManualFields(AdminTrainingAlert alert, Map<String, Object> body) {
        if (body.containsKey("type")) alert.setType(String.valueOf(body.get("type")));
        if (body.containsKey("level")) alert.setLevel(String.valueOf(body.get("level")));
        if (body.containsKey("title")) alert.setTitle(String.valueOf(body.get("title")));
        if (body.containsKey("description")) alert.setDescription(String.valueOf(body.get("description")));
        if (body.containsKey("department")) alert.setDepartment(String.valueOf(body.get("department")));
        if (body.containsKey("responsiblePerson")) alert.setResponsiblePerson(String.valueOf(body.get("responsiblePerson")));
        if (body.containsKey("traineeName")) alert.setTraineeName(String.valueOf(body.get("traineeName")));
        if (body.containsKey("courseName")) alert.setCourseName(String.valueOf(body.get("courseName")));
        if (body.containsKey("status")) alert.setStatus(String.valueOf(body.get("status")));
        if (body.containsKey("actionPath")) alert.setActionPath(String.valueOf(body.get("actionPath")));
        if (body.containsKey("occurredAt")) alert.setOccurredAt(parseTime(String.valueOf(body.get("occurredAt"))));
    }

    private String nextAlertNo() {
        String prefix = "TW" + LocalDate.now().format(ALERT_NO_DAY);
        long count = manualAlertRepo.countByAlertNoStartingWith(prefix);
        return prefix + String.format("%03d", count + 1);
    }

    private String displayName(User user) {
        return user.getUsername() != null ? user.getUsername() : "学员";
    }

    private long countByType(List<Map<String, Object>> items, String... types) {
        Set<String> set = Set.of(types);
        return items.stream().filter(item -> set.contains(String.valueOf(item.get("type")))).count();
    }

    private double percent(long part, long total) {
        if (total <= 0) return 0;
        return Math.round(part * 10000.0 / total) / 100.0;
    }

    private boolean occurredOn(Map<String, Object> item, LocalDate date) {
        LocalDateTime time = parseTime(String.valueOf(item.get("time")));
        return time != null && time.toLocalDate().equals(date);
    }

    private LocalDateTime parseTime(String raw) {
        if (raw == null || raw.isBlank() || "null".equals(raw)) return null;
        try {
            if (raw.length() >= 16) {
                return LocalDateTime.parse(raw.substring(0, 16), DATE_TIME);
            }
            return LocalDateTime.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime parseDateStart(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return LocalDate.parse(raw).atStartOfDay();
    }

    private LocalDateTime parseDateEnd(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return LocalDate.parse(raw).atTime(23, 59, 59);
    }
}

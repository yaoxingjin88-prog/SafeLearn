package com.safelearn.service;

import com.safelearn.deduction.repository.SimulationSessionRepository;
import com.safelearn.entity.*;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final UserProgressRepository progressRepo;
    private final TrainingRecordRepository recordRepo;
    private final SimulationSessionRepository simulationSessionRepo;
    private final ScenarioRepository scenarioRepo;
    private final UserRepository userRepo;

    public Map<String, Object> getStats(String userId) {
        long courseCount = courseRepo.findByStatusOrderByCreatedAtDesc("published").size();
        long completedCount = progressRepo.countByUserIdAndCompletedTrue(userId);
        long simulationCount = simulationSessionRepo.countByUserIdAndStatus(userId, "completed");
        long trainingCount = recordRepo.countByUserId(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("courseCount", courseCount);
        stats.put("completedCount", completedCount);
        stats.put("simulationCount", simulationCount);
        stats.put("trainingCount", trainingCount);
        Double dedAvg = simulationSessionRepo.avgTotalScoreByUserId(userId);
        Double trainAvg = recordRepo.avgCompletedScoreByUserId(userId);
        int avgScore = dedAvg != null ? (int) Math.round(dedAvg)
                : (trainAvg != null ? (int) Math.round(trainAvg) : 0);
        stats.put("avgScore", avgScore);
        return stats;
    }

    public Map<String, Object> getOverview(String userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        List<UserProgress> allProgress = progressRepo.findRecentByUserId(userId);
        List<Course> publishedCourses = courseRepo.findByStatusOrderByCreatedAtDesc("published");

        long completedChapters = progressRepo.countByUserIdAndCompletedTrue(userId);
        int studyMinutes = allProgress.stream()
                .mapToInt(p -> {
                    int duration = p.getChapter() != null && p.getChapter().getDuration() != null
                            ? p.getChapter().getDuration() : 30;
                    return duration * (p.getProgress() != null ? p.getProgress() : 0) / 100;
                })
                .sum();
        Double dedAvg = simulationSessionRepo.avgTotalScoreByUserId(userId);
        Double trainAvg = recordRepo.avgCompletedScoreByUserId(userId);
        int avgScore = dedAvg != null ? (int) Math.round(dedAvg)
                : (trainAvg != null ? (int) Math.round(trainAvg) : 0);
        long deductionCount = simulationSessionRepo.countByUserIdAndStatus(userId, "completed");
        long deductionSuccess = simulationSessionRepo.findByUserIdOrderByStartedAtDesc(userId).stream()
                .filter(s -> "success".equals(s.getOutcome())).count();

        long pendingMandatory = publishedCourses.stream()
                .mapToLong(course -> countIncompleteChapters(userId, course.getId()))
                .sum();

        Map<String, Object> overview = new HashMap<>();
        overview.put("displayName", resolveDisplayName(user));
        overview.put("joinDays", calcJoinDays(user.getCreatedAt()));
        overview.put("streakDays", calcStreakDays(allProgress));
        overview.put("studyHours", Math.round(studyMinutes / 60.0 * 10) / 10.0);
        overview.put("completedCourses", countCompletedCourses(userId, publishedCourses));
        overview.put("completedChapters", completedChapters);
        overview.put("avgScore", avgScore);
        overview.put("pendingMandatory", pendingMandatory);
        overview.put("deductionCount", deductionCount);
        overview.put("deductionSuccessRate", deductionCount == 0 ? 0
                : Math.round(deductionSuccess * 1000.0 / deductionCount) / 10.0);
        overview.put("deductionAvgScore", dedAvg != null ? Math.round(dedAvg * 10) / 10.0 : 0);
        overview.put("continueLearning", buildContinueLearning(userId, allProgress));
        overview.put("mandatoryTasks", buildMandatoryTasks(userId));
        overview.put("recommendedCourses", buildRecommendedCourses(userId, publishedCourses));
        overview.put("recentDeductions", buildRecentDeductions(userId));
        return overview;
    }

    public List<Map<String, Object>> getRecentCourses(String userId) {
        List<UserProgress> progressList = progressRepo.findRecentByUserId(userId);

        Map<String, Integer> courseProgress = new LinkedHashMap<>();
        for (UserProgress p : progressList) {
            String courseId = p.getCourse().getId();
            courseProgress.merge(courseId, p.getProgress(), Math::max);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (var entry : courseProgress.entrySet()) {
            courseRepo.findById(entry.getKey()).ifPresent(course -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", course.getId());
                m.put("title", course.getTitle());
                m.put("category", course.getCategory());
                m.put("progress", entry.getValue());
                result.add(m);
            });
        }

        if (result.isEmpty()) {
            courseRepo.findByStatusOrderByCreatedAtDesc("published").stream().limit(5).forEach(course -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", course.getId());
                m.put("title", course.getTitle());
                m.put("category", course.getCategory());
                m.put("progress", 0);
                result.add(m);
            });
        }

        return result;
    }

    private String resolveDisplayName(User user) {
        return switch (user.getUsername()) {
            case "zhanggong" -> "张工";
            case "lisi" -> "李工";
            case "wangwu" -> "王工";
            default -> user.getUsername();
        };
    }

    private long calcJoinDays(LocalDateTime createdAt) {
        if (createdAt == null) return 1;
        return Math.max(1, ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDate.now()) + 1);
    }

    private int calcStreakDays(List<UserProgress> progressList) {
        if (progressList.isEmpty()) return 0;
        Set<LocalDate> activeDays = new HashSet<>();
        for (UserProgress p : progressList) {
            if (p.getLastAccessAt() != null) {
                activeDays.add(p.getLastAccessAt().toLocalDate());
            }
        }
        int streak = 0;
        LocalDate day = LocalDate.now();
        while (activeDays.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return Math.max(streak, activeDays.isEmpty() ? 0 : 1);
    }

    private long countIncompleteChapters(String userId, String courseId) {
        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
        long incomplete = 0;
        for (Chapter ch : chapters) {
            boolean done = progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId());
            if (!done) incomplete++;
        }
        return incomplete;
    }

    private long countCompletedCourses(String userId, List<Course> courses) {
        return courses.stream().filter(course -> {
            List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId());
            if (chapters.isEmpty()) return false;
            return chapters.stream().allMatch(ch ->
                    progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId()));
        }).count();
    }

    private Map<String, Object> buildContinueLearning(String userId, List<UserProgress> progressList) {
        Optional<UserProgress> latest = progressList.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getCompleted()))
                .filter(p -> p.getProgress() != null && p.getProgress() > 0)
                .findFirst();

        if (latest.isEmpty()) {
            return courseRepo.findByStatusOrderByCreatedAtDesc("published").stream().findFirst()
                    .map(course -> {
                        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId());
                        Chapter first = chapters.isEmpty() ? null : chapters.get(0);
                        Map<String, Object> m = new HashMap<>();
                        m.put("courseId", course.getId());
                        m.put("chapterId", first != null ? first.getId() : null);
                        m.put("courseTitle", course.getTitle());
                        m.put("chapterTitle", first != null ? first.getTitle() : "开始学习");
                        m.put("progress", 0);
                        m.put("remainingMinutes", first != null && first.getDuration() != null ? first.getDuration() : 30);
                        return m;
                    }).orElse(null);
        }

        UserProgress p = latest.get();
        Chapter chapter = p.getChapter();
        Course course = p.getCourse();
        int duration = chapter != null && chapter.getDuration() != null ? chapter.getDuration() : 30;
        int progress = p.getProgress() != null ? p.getProgress() : 0;
        int remaining = Math.max(1, duration * (100 - progress) / 100);

        Map<String, Object> m = new HashMap<>();
        m.put("courseId", course != null ? course.getId() : null);
        m.put("chapterId", chapter != null ? chapter.getId() : null);
        m.put("courseTitle", course != null ? course.getTitle() : "");
        m.put("chapterTitle", chapter != null ? chapter.getTitle() : "");
        m.put("progress", progress);
        m.put("remainingMinutes", remaining);
        return m;
    }

    private List<Map<String, Object>> buildMandatoryTasks(String userId) {
        List<Map<String, Object>> tasks = new ArrayList<>();

        List<Scenario> scenarios = scenarioRepo.findAll();
        long trainedCount = recordRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(r -> r.getEndTime() != null).count();

        if (trainedCount < scenarios.size()) {
            Scenario next = scenarios.stream()
                    .filter(s -> recordRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                            .noneMatch(r -> r.getScenario() != null && s.getId().equals(r.getScenario().getId()) && r.getEndTime() != null))
                    .findFirst().orElse(scenarios.get(0));
            Map<String, Object> task = new HashMap<>();
            task.put("id", next.getId());
            task.put("title", next.getName());
            task.put("type", "training");
            task.put("deadline", "截止：明天 23:59");
            task.put("status", "待考核");
            task.put("actionText", "去训练");
            task.put("link", "/training/" + next.getId());
            tasks.add(task);
        }

        courseRepo.findByStatusOrderByCreatedAtDesc("published").stream()
                .filter(course -> countIncompleteChapters(userId, course.getId()) > 0)
                .limit(2)
                .forEach(course -> {
                    List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId());
                    long done = chapters.stream()
                            .filter(ch -> progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId()))
                            .count();
                    Map<String, Object> task = new HashMap<>();
                    task.put("id", course.getId());
                    task.put("title", course.getTitle());
                    task.put("type", "course");
                    task.put("deadline", "未开始 (" + done + "/" + chapters.size() + " 课时)");
                    task.put("status", done == 0 ? "未开始" : "进行中");
                    task.put("actionText", "去学习");
                    task.put("link", "/courses/" + course.getId());
                    tasks.add(task);
                });

        return tasks.stream().limit(3).toList();
    }

    private List<Map<String, Object>> buildRecommendedCourses(String userId, List<Course> courses) {
        return courses.stream()
                .filter(course -> !isCourseFullyCompleted(userId, course.getId()))
                .limit(4)
                .map(course -> {
                    List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId());
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", course.getId());
                    m.put("title", course.getTitle());
                    m.put("description", course.getDescription());
                    m.put("coverImage", course.getCoverImage());
                    m.put("totalDuration", course.getTotalDuration());
                    m.put("chapterCount", chapters.size());
                    m.put("tag", course.getCategory().equals("thermal") ? "3D 互动" : "视频课程");
                    m.put("meta", (course.getTotalDuration() != null ? course.getTotalDuration() : 0) + "分钟 · "
                            + chapters.size() + "章节");
                    return m;
                }).toList();
    }

    private List<Map<String, Object>> buildRecentDeductions(String userId) {
        return simulationSessionRepo.findByUserIdOrderByStartedAtDesc(userId).stream()
                .filter(s -> "completed".equals(s.getStatus()))
                .limit(5)
                .map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("sessionId", s.getId());
                    m.put("scenarioId", s.getScenarioId());
                    m.put("outcome", s.getOutcome());
                    m.put("totalScore", s.getTotalScore());
                    m.put("rating", s.getRating());
                    m.put("finishedAt", s.getFinishedAt() != null ? s.getFinishedAt().toString() : null);
                    scenarioRepo.findById(s.getScenarioId()).ifPresent(sc -> m.put("scenarioName", sc.getName()));
                    return m;
                }).toList();
    }

    private boolean isCourseFullyCompleted(String userId, String courseId) {
        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
        if (chapters.isEmpty()) return false;
        return chapters.stream().allMatch(ch ->
                progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId()));
    }
}

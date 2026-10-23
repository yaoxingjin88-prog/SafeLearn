package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.common.DifficultyLevel;
import com.safelearn.dto.ProgressHeartbeatRequest;
import com.safelearn.dto.ProgressRequest;
import com.safelearn.entity.*;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final UserProgressRepository progressRepo;
    private final UserRepository userRepo;
    private final CourseReviewRepository reviewRepo;
    private final CertificateService certificateService;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfig;
    private final AssignmentCompletionService assignmentCompletionService;
    private final ScenarioRepository scenarioRepo;
    private final AccidentCaseRepository caseRepo;

    private static final int MASTERY_THRESHOLD = 60;

    /** 掌握度解锁阈值，可由管理端配置覆盖。 */
    private int masteryThreshold() {
        return systemConfig.getInt("learning.masteryThreshold", MASTERY_THRESHOLD);
    }

    public Map<String, Object> getCourses(String category, String keyword, String userId, int page, int pageSize) {
        List<Course> courses = courseRepo.findByFilters("published",
                (category == null || category.equals("all")) ? null : category,
                (keyword == null || keyword.isBlank()) ? null : keyword);
        List<Map<String, Object>> all = courses.stream().map(c -> toCourseSummary(c, userId)).toList();

        int total = all.size();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        int from = Math.max(0, (safePage - 1) * safeSize);
        int to = Math.min(total, from + safeSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", from >= total ? List.of() : all.subList(from, to));
        result.put("total", total);
        result.put("page", safePage);
        result.put("pageSize", safeSize);
        result.put("totalPages", safeSize == 0 ? 0 : (int) Math.ceil(total * 1.0 / safeSize));
        return result;
    }

    public List<Map<String, Object>> getCourses(String category, String keyword, String userId) {
        return (List<Map<String, Object>>) getCourses(category, keyword, userId, 1, Integer.MAX_VALUE).get("items");
    }

    public Map<String, Object> getCourseById(String id) {
        Course course = courseRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        return toCourseDetail(course);
    }

    public Map<String, Object> getCourseDetailForUser(String id, String userId) {
        Course course = courseRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("课程不存在"));

        final Set<String> completedIds = userId != null
                ? new HashSet<>(progressRepo.findCompletedChapterIdsByUserId(userId))
                : new HashSet<>();
        final Set<String> qualifiedIds = userId != null
                ? new HashSet<>(progressRepo.findQualifiedChapterIdsByUserId(userId, masteryThreshold()))
                : new HashSet<>();

        Map<String, Object> m = toCourseSummary(course, userId);
        m.put("chapters", course.getChapters().stream().map(ch -> {
            Map<String, Object> cm = toChapterInfo(ch);
            cm.put("unlocked", isChapterUnlocked(ch, userId, completedIds, qualifiedIds));
            return cm;
        }).toList());
        return m;
    }

    @Transactional
    public Map<String, Object> getChapter(String courseId, String chapterId, String userId) {
        Chapter chapter = chapterRepo.findByIdAndCourseId(chapterId, courseId)
                .orElseThrow(() -> new RuntimeException("章节不存在"));

        if (userId != null) {
            touchChapterAccess(userId, courseId, chapter);
        }

        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);

        final Set<String> completedIds = userId != null
                ? new HashSet<>(progressRepo.findCompletedChapterIdsByUserId(userId))
                : new HashSet<>();
        final Set<String> qualifiedIds = userId != null
                ? new HashSet<>(progressRepo.findQualifiedChapterIdsByUserId(userId, masteryThreshold()))
                : new HashSet<>();

        Map<String, Object> result = new HashMap<>();
        result.put("chapter", toChapterInfo(chapter));
        result.put("nextSteps", buildChapterNextSteps(chapter));
        result.put("chapters", chapters.stream().map(ch -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ch.getId());
            m.put("title", ch.getTitle());
            m.put("difficultyLevel", ch.getDifficultyLevel());
            m.put("scenarioId", ch.getScenarioId());
            m.put("unlocked", isChapterUnlocked(ch, userId, completedIds, qualifiedIds));
            return m;
        }).toList());
        return result;
    }

    public Map<String, Object> updateProgress(String userId, ProgressRequest req) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        Course course = courseRepo.findById(req.getCourseId()).orElseThrow(() -> new RuntimeException("课程不存在"));
        Chapter chapter = chapterRepo.findById(req.getChapterId()).orElseThrow(() -> new RuntimeException("章节不存在"));

        List<String> prereqIds = parsePrereqIds(chapter.getPrerequisiteIds());
        if (!prereqIds.isEmpty()) {
            for (String prereqId : prereqIds) {
                if (needsMastery(chapter)) {
                    int threshold = masteryThreshold();
                    boolean met = progressRepo.existsByUserIdAndChapterIdAndCompletedWithMastery(
                            userId, prereqId, threshold);
                    if (!met) {
                        throw new RuntimeException("请先完成前置章节并达到掌握度" + threshold + "%");
                    }
                } else {
                    boolean met = progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, prereqId);
                    if (!met) {
                        throw new RuntimeException("请先完成前置章节");
                    }
                }
            }
        }

        Optional<UserProgress> existing = progressRepo.findByUserIdAndChapterId(userId, req.getChapterId());
        if (existing.isPresent() && Boolean.TRUE.equals(existing.get().getCompleted())
                && Boolean.TRUE.equals(req.getCompleted())) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("alreadyCompleted", true);
            return result;
        }

        UserProgress progress = existing.orElse(new UserProgress());
        progress.setUser(user);
        progress.setCourse(course);
        progress.setChapter(chapter);
        progress.setProgress(req.getProgress());
        progress.setCompleted(req.getCompleted());
        if (req.getMasteryLevel() != null) {
            progress.setMasteryLevel(req.getMasteryLevel());
        }
        progress.setLastAccessAt(LocalDateTime.now());
        progressRepo.save(progress);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        if (Boolean.TRUE.equals(req.getCompleted())) {
            certificateService.tryIssueOnCourseComplete(userId, req.getCourseId())
                    .ifPresent(cert -> result.put("newCertificate", cert));
            assignmentCompletionService.syncForUser(userId);
        }
        return result;
    }

    /** 学习心跳：累加有效学习时长，同步阅读进度（不自动标记完成） */
    public Map<String, Object> heartbeatProgress(String userId, ProgressHeartbeatRequest req) {
        if (req.getCourseId() == null || req.getChapterId() == null) {
            throw new RuntimeException("课程与章节不能为空");
        }
        int elapsed = req.getElapsedSeconds() != null ? req.getElapsedSeconds() : 30;
        elapsed = Math.max(1, Math.min(elapsed, 120));

        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        Course course = courseRepo.findById(req.getCourseId()).orElseThrow(() -> new RuntimeException("课程不存在"));
        Chapter chapter = chapterRepo.findById(req.getChapterId()).orElseThrow(() -> new RuntimeException("章节不存在"));

        UserProgress progress = progressRepo.findByUserIdAndChapterId(userId, req.getChapterId())
                .orElseGet(() -> {
                    UserProgress p = new UserProgress();
                    p.setUser(user);
                    p.setCourse(course);
                    p.setChapter(chapter);
                    p.setProgress(0);
                    p.setCompleted(false);
                    p.setMasteryLevel(0);
                    p.setStudySeconds(0);
                    return p;
                });

        int currentStudy = progress.getStudySeconds() != null ? progress.getStudySeconds() : 0;
        progress.setStudySeconds(currentStudy + elapsed);
        progress.setLastAccessAt(LocalDateTime.now());

        if (!Boolean.TRUE.equals(progress.getCompleted()) && req.getProgress() != null) {
            int reported = Math.max(0, Math.min(req.getProgress(), 99));
            int current = progress.getProgress() != null ? progress.getProgress() : 0;
            progress.setProgress(Math.max(current, reported));
        }

        progressRepo.save(progress);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("studySeconds", progress.getStudySeconds());
        result.put("progress", progress.getProgress());
        return result;
    }

    public List<Map<String, Object>> getProgress(String userId, String courseId) {
        userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        courseRepo.findById(courseId).orElseThrow(() -> new RuntimeException("课程不存在"));

        return progressRepo.findByUserIdAndCourseId(userId, courseId).stream().map(up -> {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", userId);
            m.put("courseId", courseId);
            m.put("chapterId", up.getChapter() != null ? up.getChapter().getId() : null);
            m.put("chapterTitle", up.getChapter() != null ? up.getChapter().getTitle() : null);
            m.put("progress", up.getProgress());
            m.put("completed", up.getCompleted());
            m.put("masteryLevel", up.getMasteryLevel());
            m.put("studySeconds", up.getStudySeconds() != null ? up.getStudySeconds() : 0);
            m.put("lastAccessAt", up.getLastAccessAt() != null ? up.getLastAccessAt().toString() : null);
            m.put("completedAt", up.getCompleted() ? up.getLastAccessAt() != null ? up.getLastAccessAt().toString() : null : null);
            return m;
        }).toList();
    }

    public List<Map<String, Object>> getMyLearningRecords(String userId) {
        return progressRepo.findByUserIdWithDetails(userId).stream()
                .filter(up -> (up.getProgress() != null && up.getProgress() > 0)
                        || Boolean.TRUE.equals(up.getCompleted()))
                .sorted(Comparator.comparing(
                        (UserProgress up) -> up.getLastAccessAt() != null ? up.getLastAccessAt() : LocalDateTime.MIN,
                        Comparator.reverseOrder()))
                .map(up -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("courseId", up.getCourse() != null ? up.getCourse().getId() : null);
                    m.put("courseTitle", up.getCourse() != null ? up.getCourse().getTitle() : "");
                    m.put("chapterId", up.getChapter() != null ? up.getChapter().getId() : null);
                    m.put("chapterTitle", up.getChapter() != null ? up.getChapter().getTitle() : "");
                    m.put("progress", up.getProgress() != null ? up.getProgress() : 0);
                    m.put("completed", Boolean.TRUE.equals(up.getCompleted()));
                    m.put("masteryLevel", up.getMasteryLevel() != null ? up.getMasteryLevel() : 0);
                    m.put("lastAccessAt", up.getLastAccessAt() != null ? up.getLastAccessAt().toString() : null);
                    return m;
                })
                .toList();
    }

    public List<Map<String, Object>> getRelatedCourses(String id, String userId) {
        Course course = courseRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        String category = course.getCategory();
        return courseRepo.findByFilters("published", category, null).stream()
                .filter(c -> !c.getId().equals(id))
                .limit(3)
                .map(c -> toCourseSummary(c, userId))
                .toList();
    }

    public Map<String, Object> getSkillTree(String userId) {
        List<Course> courses = courseRepo.findByStatusOrderByCreatedAtDesc("published");

        final Set<String> completedIds = userId != null
                ? new HashSet<>(progressRepo.findCompletedChapterIdsByUserId(userId))
                : new HashSet<>();
        final Set<String> qualifiedIds = userId != null
                ? new HashSet<>(progressRepo.findQualifiedChapterIdsByUserId(userId, masteryThreshold()))
                : new HashSet<>();

        List<Map<String, Object>> basicNodes = new ArrayList<>();
        List<Map<String, Object>> intermediateNodes = new ArrayList<>();
        List<Map<String, Object>> advancedNodes = new ArrayList<>();
        List<Map<String, Object>> connections = new ArrayList<>();

        for (Course course : courses) {
            for (Chapter ch : course.getChapters()) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", ch.getId());
                node.put("courseId", course.getId());
                node.put("courseTitle", course.getTitle());
                node.put("title", ch.getTitle());
                int level = ch.getDifficultyLevel() != null ? ch.getDifficultyLevel() : 1;
                String levelName = switch (level) {
                    case 1 -> "BASIC";
                    case 2 -> "INTERMEDIATE";
                    case 3 -> "ADVANCED";
                    default -> "BASIC";
                };
                node.put("difficultyLevel", levelName);
                node.put("prerequisiteIds", parsePrereqIds(ch.getPrerequisiteIds()));

                boolean unlocked = isChapterUnlocked(ch, userId, completedIds, qualifiedIds);
                boolean completed = completedIds.contains(ch.getId());
                int progress = 0;
                if (userId != null) {
                    progress = progressRepo.findByUserIdAndChapterId(userId, ch.getId())
                            .map(UserProgress::getProgress)
                            .orElse(0);
                }
                node.put("unlocked", unlocked);
                node.put("completed", completed);
                node.put("progress", completed ? 100 : progress);

                // 根据难度等级分类节点
                switch (level) {
                    case 1 -> basicNodes.add(node);
                    case 2 -> intermediateNodes.add(node);
                    case 3 -> advancedNodes.add(node);
                    default -> basicNodes.add(node);
                }

                // 构建连接线
                List<String> prereqIds = parsePrereqIds(ch.getPrerequisiteIds());
                for (String prereqId : prereqIds) {
                    Map<String, Object> conn = new HashMap<>();
                    conn.put("from", prereqId);
                    conn.put("to", ch.getId());
                    connections.add(conn);
                }
            }
        }

        Map<String, Object> levels = new HashMap<>();
        levels.put("BASIC", basicNodes);
        levels.put("INTERMEDIATE", intermediateNodes);
        levels.put("ADVANCED", advancedNodes);

        Map<String, Object> result = new HashMap<>();
        result.put("levels", levels);
        result.put("connections", connections);
        return result;
    }

    private boolean isChapterUnlocked(Chapter ch, String userId,
                                      Set<String> completedIds, Set<String> qualifiedIds) {
        if (userId == null) return true;
        List<String> prereqIds = parsePrereqIds(ch.getPrerequisiteIds());
        if (prereqIds.isEmpty()) return true;
        for (String prereqId : prereqIds) {
            if (needsMastery(ch)) {
                if (!qualifiedIds.contains(prereqId)) return false;
            } else {
                if (!completedIds.contains(prereqId)) return false;
            }
        }
        return true;
    }

    private boolean needsMastery(Chapter ch) {
        return ch.getDifficultyLevel() != null && ch.getDifficultyLevel() > DifficultyLevel.BASIC;
    }

    private List<String> parsePrereqIds(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> toCourseSummary(Course c, String userId) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("description", c.getDescription());
        m.put("coverImage", c.getCoverImage());
        m.put("category", c.getCategory());
        m.put("totalDuration", c.getTotalDuration());
        m.put("status", c.getStatus());
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        m.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null);
        m.put("learnerCount", progressRepo.countDistinctUsersByCourseId(c.getId()));
        mergeCourseMetadata(m, c.getExtraMetadata());
        m.put("chapters", c.getChapters() != null ? c.getChapters().stream().map(ch -> {
            Map<String, Object> cm = new HashMap<>();
            cm.put("id", ch.getId());
            cm.put("title", ch.getTitle());
            return cm;
        }).toList() : List.of());

        // 计算用户进度
        if (userId != null && c.getChapters() != null && !c.getChapters().isEmpty()) {
            int totalChapters = c.getChapters().size();
            long completedChapters = progressRepo.countCompletedChaptersByUserIdAndCourseId(userId, c.getId());
            int progress = (int) Math.round((double) completedChapters / totalChapters * 100);
            m.put("progress", progress);
        } else {
            m.put("progress", 0);
        }

        return m;
    }

    private Map<String, Object> toCourseDetail(Course c) {
        Map<String, Object> m = toCourseSummary(c, null);
        m.put("chapters", c.getChapters().stream().map(this::toChapterInfo).toList());
        return m;
    }

    private Map<String, Object> toChapterInfo(Chapter ch) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", ch.getId());
        m.put("courseId", ch.getCourse() != null ? ch.getCourse().getId() : null);
        m.put("title", ch.getTitle());
        m.put("content", ch.getContent());
        m.put("videoUrl", ch.getVideoUrl());
        m.put("duration", ch.getDuration());
        m.put("order", ch.getOrderNum());
        m.put("difficultyLevel", ch.getDifficultyLevel());
        m.put("difficultyLabel", DifficultyLevel.label(ch.getDifficultyLevel() != null ? ch.getDifficultyLevel() : 1));
        m.put("prerequisiteIds", parsePrereqIds(ch.getPrerequisiteIds()));
        m.put("scenarioId", ch.getScenarioId());
        m.put("caseId", ch.getCaseId());
        m.put("summary", ch.getSummary());
        m.put("contentType", ch.getContentType() != null ? ch.getContentType() : "html");
        m.put("required", ch.getRequired() != null ? ch.getRequired() : true);
        m.put("allowDownload", ch.getAllowDownload() != null ? ch.getAllowDownload() : false);
        return m;
    }

    private List<Map<String, Object>> buildChapterNextSteps(Chapter chapter) {
        List<Map<String, Object>> steps = new ArrayList<>();
        if (chapter.getCaseId() != null && !chapter.getCaseId().isBlank()) {
            caseRepo.findById(chapter.getCaseId()).ifPresent(c -> {
                Map<String, Object> step = new LinkedHashMap<>();
                step.put("type", "case");
                step.put("title", c.getTitle());
                step.put("description", "复盘关联事故案例，理解成因与处置教训");
                step.put("link", "/cases/" + c.getId());
                steps.add(step);
            });
        }
        if (chapter.getScenarioId() != null && !chapter.getScenarioId().isBlank()) {
            scenarioRepo.findById(chapter.getScenarioId()).ifPresent(s -> {
                Map<String, Object> step = new LinkedHashMap<>();
                String kind = resolveScenarioKind(s);
                step.put("type", "simulation".equals(kind) ? "simulation" : "training");
                step.put("title", s.getName());
                step.put("description", "simulation".equals(kind)
                        ? "进入三维事故推演，观察热失控演化过程"
                        : "进入应急决策训练，练习分支处置");
                step.put("link", "simulation".equals(kind)
                        ? "/simulation/" + s.getId()
                        : "/training/" + s.getId());
                steps.add(step);
            });
        }
        return steps;
    }

    @SuppressWarnings("unchecked")
    private String resolveScenarioKind(Scenario s) {
        if (s.getInitialConditions() == null || s.getInitialConditions().isBlank()) {
            return "training";
        }
        try {
            Map<String, Object> cond = objectMapper.readValue(s.getInitialConditions(), Map.class);
            if ("emergency_case".equals(cond.get("trainingKind"))) return "emergency";
            Object count = cond.get("batteryCount");
            if (count instanceof Number n && n.intValue() > 0) return "simulation";
        } catch (Exception ignored) {
        }
        return "training";
    }

    private void mergeCourseMetadata(Map<String, Object> target, String json) {
        if (json == null || json.isBlank()) return;
        try {
            Map<String, Object> meta = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            target.putAll(meta);
        } catch (Exception ignored) {
        }
    }

    /** 记录章节访问时间，供工作台「上次学到」使用 */
    private void touchChapterAccess(String userId, String courseId, Chapter chapter) {
        User user = userRepo.findById(userId).orElse(null);
        Course course = courseRepo.findById(courseId).orElse(null);
        if (user == null || course == null) return;

        UserProgress progress = progressRepo.findByUserIdAndChapterId(userId, chapter.getId())
                .orElseGet(() -> {
                    UserProgress p = new UserProgress();
                    p.setUser(user);
                    p.setCourse(course);
                    p.setChapter(chapter);
                    p.setProgress(0);
                    p.setCompleted(false);
                    p.setMasteryLevel(0);
                    return p;
                });
        progress.setLastAccessAt(LocalDateTime.now());
        progressRepo.save(progress);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCourseReviews(String courseId) {
        courseRepo.findById(courseId).orElseThrow(() -> new RuntimeException("课程不存在"));
        List<Map<String, Object>> items = reviewRepo.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(this::toReviewInfo)
                .toList();
        Double avg = reviewRepo.avgRatingByCourseId(courseId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", items.size());
        result.put("averageRating", avg != null ? Math.round(avg * 10.0) / 10.0 : 0);
        return result;
    }

    @Transactional
    public Map<String, Object> submitCourseReview(String userId, String courseId, int rating, String content) {
        if (rating < 1 || rating > 5) throw new RuntimeException("评分需在 1-5 之间");
        if (content == null || content.isBlank()) throw new RuntimeException("请填写评价内容");
        courseRepo.findById(courseId).orElseThrow(() -> new RuntimeException("课程不存在"));
        userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));

        CourseReview review = reviewRepo.findByCourseIdAndUserId(courseId, userId).orElseGet(CourseReview::new);
        review.setCourseId(courseId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setContent(content.trim());
        reviewRepo.save(review);
        return toReviewInfo(review);
    }

    private Map<String, Object> toReviewInfo(CourseReview review) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", review.getId());
        m.put("courseId", review.getCourseId());
        m.put("userId", review.getUserId());
        m.put("rating", review.getRating());
        m.put("content", review.getContent());
        m.put("createdAt", review.getCreatedAt() != null ? review.getCreatedAt().toString() : null);
        userRepo.findById(review.getUserId()).ifPresent(u -> {
            m.put("username", u.getUsername());
            m.put("displayName", resolveReviewerName(u));
        });
        return m;
    }

    private String resolveReviewerName(User user) {
        return switch (user.getUsername()) {
            case "zhanggong" -> "张工";
            case "lisi" -> "李工";
            case "wangwu" -> "王工";
            default -> user.getUsername();
        };
    }
}

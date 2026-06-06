package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.common.DifficultyLevel;
import com.safelearn.dto.ProgressRequest;
import com.safelearn.entity.*;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final UserProgressRepository progressRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;

    private static final int MASTERY_THRESHOLD = 60;

    public List<Map<String, Object>> getCourses(String category, String keyword) {
        List<Course> courses = courseRepo.findByFilters("published",
                (category == null || category.equals("all")) ? null : category,
                (keyword == null || keyword.isBlank()) ? null : keyword);
        return courses.stream().map(this::toCourseSummary).toList();
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
                ? new HashSet<>(progressRepo.findQualifiedChapterIdsByUserId(userId, MASTERY_THRESHOLD))
                : new HashSet<>();

        Map<String, Object> m = toCourseSummary(course);
        m.put("chapters", course.getChapters().stream().map(ch -> {
            Map<String, Object> cm = toChapterInfo(ch);
            cm.put("unlocked", isChapterUnlocked(ch, userId, completedIds, qualifiedIds));
            return cm;
        }).toList());
        return m;
    }

    public Map<String, Object> getChapter(String courseId, String chapterId, String userId) {
        Chapter chapter = chapterRepo.findByIdAndCourseId(chapterId, courseId)
                .orElseThrow(() -> new RuntimeException("章节不存在"));

        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);

        final Set<String> completedIds = userId != null
                ? new HashSet<>(progressRepo.findCompletedChapterIdsByUserId(userId))
                : new HashSet<>();
        final Set<String> qualifiedIds = userId != null
                ? new HashSet<>(progressRepo.findQualifiedChapterIdsByUserId(userId, MASTERY_THRESHOLD))
                : new HashSet<>();

        Map<String, Object> result = new HashMap<>();
        result.put("chapter", toChapterInfo(chapter));
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
                    boolean met = progressRepo.existsByUserIdAndChapterIdAndCompletedWithMastery(
                            userId, prereqId, MASTERY_THRESHOLD);
                    if (!met) {
                        throw new RuntimeException("请先完成前置章节并达到掌握度" + MASTERY_THRESHOLD + "%");
                    }
                } else {
                    boolean met = progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, prereqId);
                    if (!met) {
                        throw new RuntimeException("请先完成前置章节");
                    }
                }
            }
        }

        UserProgress progress = progressRepo.findByUserIdAndChapterId(userId, req.getChapterId())
                .orElse(new UserProgress());
        progress.setUser(user);
        progress.setCourse(course);
        progress.setChapter(chapter);
        progress.setProgress(req.getProgress());
        progress.setCompleted(req.getCompleted());
        if (req.getMasteryLevel() != null) {
            progress.setMasteryLevel(req.getMasteryLevel());
        }
        progressRepo.save(progress);

        return Map.of("success", true);
    }

    public List<Map<String, Object>> getProgress(String userId, String courseId) {
        userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        courseRepo.findById(courseId).orElseThrow(() -> new RuntimeException("课程不存在"));

        return progressRepo.findByUserIdAndCourseId(userId, courseId).stream().map(up -> {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", userId);
            m.put("courseId", courseId);
            m.put("chapterId", up.getChapter() != null ? up.getChapter().getId() : null);
            m.put("progress", up.getProgress());
            m.put("completed", up.getCompleted());
            m.put("masteryLevel", up.getMasteryLevel());
            m.put("lastAccessAt", up.getLastAccessAt() != null ? up.getLastAccessAt().toString() : null);
            m.put("completedAt", up.getCompleted() ? up.getLastAccessAt() != null ? up.getLastAccessAt().toString() : null : null);
            return m;
        }).toList();
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

    private Map<String, Object> toCourseSummary(Course c) {
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
        m.put("chapters", c.getChapters() != null ? c.getChapters().stream().map(ch -> {
            Map<String, Object> cm = new HashMap<>();
            cm.put("id", ch.getId());
            cm.put("title", ch.getTitle());
            return cm;
        }).toList() : List.of());
        return m;
    }

    private Map<String, Object> toCourseDetail(Course c) {
        Map<String, Object> m = toCourseSummary(c);
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
        return m;
    }
}

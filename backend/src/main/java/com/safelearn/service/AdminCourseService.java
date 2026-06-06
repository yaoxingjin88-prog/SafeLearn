package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.Chapter;
import com.safelearn.entity.Course;
import com.safelearn.repository.ChapterRepository;
import com.safelearn.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminCourseService {

    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> getCourses() {
        return courseRepo.findAll().stream()
                .sorted(Comparator.comparing(Course::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(course -> {
                    Map<String, Object> info = toCourseInfo(course);
                    info.put("chapterCount", chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId()).size());
                    return info;
                })
                .toList();
    }

    public Map<String, Object> getCourseById(String id) {
        Course course = courseRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        Map<String, Object> info = toCourseInfo(course);
        info.put("chapters", course.getChapters().stream().map(this::toChapterInfo).toList());
        return info;
    }

    @Transactional
    public Map<String, Object> createCourse(Map<String, Object> data) {
        Course course = new Course();
        applyCourseFields(course, data);
        if (course.getStatus() == null || course.getStatus().isBlank()) {
            course.setStatus("published");
        }
        course = courseRepo.save(course);
        return toCourseInfo(course);
    }

    @Transactional
    public Map<String, Object> updateCourse(String id, Map<String, Object> data) {
        Course course = courseRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        applyCourseFields(course, data);
        courseRepo.save(course);
        return toCourseInfo(course);
    }

    @Transactional
    public Map<String, Object> deleteCourse(String id) {
        if (!courseRepo.existsById(id)) {
            throw new RuntimeException("课程不存在");
        }
        courseRepo.deleteById(id);
        return Map.of("success", true);
    }

    public List<Map<String, Object>> getChapters(String courseId) {
        courseRepo.findById(courseId).orElseThrow(() -> new RuntimeException("课程不存在"));
        return chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId).stream()
                .map(this::toChapterInfo)
                .toList();
    }

    @Transactional
    public Map<String, Object> createChapter(String courseId, Map<String, Object> data) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        Chapter chapter = new Chapter();
        chapter.setCourse(course);
        applyChapterFields(chapter, data);
        if (chapter.getOrderNum() == null) {
            int nextOrder = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId).size() + 1;
            chapter.setOrderNum(nextOrder);
        }
        if (chapter.getDifficultyLevel() == null) {
            chapter.setDifficultyLevel(1);
        }
        chapter = chapterRepo.save(chapter);
        refreshCourseDuration(course);
        return toChapterInfo(chapter);
    }

    @Transactional
    public Map<String, Object> updateChapter(String courseId, String chapterId, Map<String, Object> data) {
        Chapter chapter = chapterRepo.findByIdAndCourseId(chapterId, courseId)
                .orElseThrow(() -> new RuntimeException("章节不存在"));
        applyChapterFields(chapter, data);
        chapterRepo.save(chapter);
        refreshCourseDuration(chapter.getCourse());
        return toChapterInfo(chapter);
    }

    @Transactional
    public Map<String, Object> deleteChapter(String courseId, String chapterId) {
        Chapter chapter = chapterRepo.findByIdAndCourseId(chapterId, courseId)
                .orElseThrow(() -> new RuntimeException("章节不存在"));
        Course course = chapter.getCourse();
        chapterRepo.delete(chapter);
        refreshCourseDuration(course);
        return Map.of("success", true);
    }

    private void applyCourseFields(Course course, Map<String, Object> data) {
        if (data.containsKey("title")) course.setTitle((String) data.get("title"));
        if (data.containsKey("description")) course.setDescription((String) data.get("description"));
        if (data.containsKey("coverImage")) course.setCoverImage((String) data.get("coverImage"));
        if (data.containsKey("category")) course.setCategory((String) data.get("category"));
        if (data.containsKey("status")) course.setStatus((String) data.get("status"));
        if (data.containsKey("totalDuration")) course.setTotalDuration(toInteger(data.get("totalDuration")));
    }

    private void applyChapterFields(Chapter chapter, Map<String, Object> data) {
        if (data.containsKey("title")) chapter.setTitle((String) data.get("title"));
        if (data.containsKey("content")) chapter.setContent((String) data.get("content"));
        if (data.containsKey("videoUrl")) chapter.setVideoUrl((String) data.get("videoUrl"));
        if (data.containsKey("duration")) chapter.setDuration(toInteger(data.get("duration")));
        if (data.containsKey("orderNum")) chapter.setOrderNum(toInteger(data.get("orderNum")));
        if (data.containsKey("order")) chapter.setOrderNum(toInteger(data.get("order")));
        if (data.containsKey("difficultyLevel")) chapter.setDifficultyLevel(toInteger(data.get("difficultyLevel")));
        if (data.containsKey("scenarioId")) chapter.setScenarioId((String) data.get("scenarioId"));
        if (data.containsKey("prerequisiteIds")) {
            chapter.setPrerequisiteIds(serializePrerequisiteIds(data.get("prerequisiteIds")));
        }
    }

    private void refreshCourseDuration(Course course) {
        if (course == null) return;
        int total = chapterRepo.findByCourseIdOrderByOrderNumAsc(course.getId()).stream()
                .map(Chapter::getDuration)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        course.setTotalDuration(total);
        courseRepo.save(course);
    }

    private String serializePrerequisiteIds(Object value) {
        if (value == null) return null;
        if (value instanceof String str) return str.isBlank() ? null : str;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("前置章节格式错误");
        }
    }

    private List<String> parsePrerequisiteIds(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(value.toString());
    }

    private Map<String, Object> toCourseInfo(Course course) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", course.getId());
        m.put("title", course.getTitle());
        m.put("description", course.getDescription());
        m.put("coverImage", course.getCoverImage());
        m.put("category", course.getCategory());
        m.put("totalDuration", course.getTotalDuration());
        m.put("status", course.getStatus());
        m.put("chapterCount", course.getChapters() != null ? course.getChapters().size() : 0);
        m.put("createdAt", course.getCreatedAt() != null ? course.getCreatedAt().toString() : null);
        m.put("updatedAt", course.getUpdatedAt() != null ? course.getUpdatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> toChapterInfo(Chapter chapter) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", chapter.getId());
        m.put("courseId", chapter.getCourse() != null ? chapter.getCourse().getId() : null);
        m.put("title", chapter.getTitle());
        m.put("content", chapter.getContent());
        m.put("videoUrl", chapter.getVideoUrl());
        m.put("duration", chapter.getDuration());
        m.put("orderNum", chapter.getOrderNum());
        m.put("order", chapter.getOrderNum());
        m.put("difficultyLevel", chapter.getDifficultyLevel());
        m.put("prerequisiteIds", parsePrerequisiteIds(chapter.getPrerequisiteIds()));
        m.put("scenarioId", chapter.getScenarioId());
        return m;
    }
}

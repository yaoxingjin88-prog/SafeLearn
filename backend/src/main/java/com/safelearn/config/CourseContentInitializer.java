package com.safelearn.config;

import com.safelearn.entity.Chapter;
import com.safelearn.entity.Course;
import com.safelearn.repository.ChapterRepository;
import com.safelearn.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * 启动时同步课程章节教学内容（resources/course-content/*.html）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseContentInitializer implements ApplicationRunner {

    private final ChapterRepository chapterRepo;
    private final CourseRepository courseRepo;

    private record ChapterDef(
            String id, String courseId, String title, String htmlFile,
            int duration, int orderNum, int difficultyLevel,
            String prerequisiteIds, String scenarioId
    ) {}

    private static final List<ChapterDef> DEFINITIONS = List.of(
            new ChapterDef("20000000-0000-0000-0000-000000000001",
                    "10000000-0000-0000-0000-000000000001", "储能概述", "ch-001.html",
                    35, 1, 1, null, null),
            new ChapterDef("20000000-0000-0000-0000-000000000002",
                    "10000000-0000-0000-0000-000000000001", "电池类型与特点", "ch-002.html",
                    40, 2, 1, "[\"20000000-0000-0000-0000-000000000001\"]", null),
            new ChapterDef("20000000-0000-0000-0000-000000000003",
                    "10000000-0000-0000-0000-000000000001", "储能系统组成", "ch-003.html",
                    45, 3, 1, "[\"20000000-0000-0000-0000-000000000002\"]", null),
            new ChapterDef("20000000-0000-0000-0000-000000000004",
                    "10000000-0000-0000-0000-000000000002", "热失控概述", "ch-004.html",
                    35, 1, 2, null, "30000000-0000-0000-0000-000000000001"),
            new ChapterDef("20000000-0000-0000-0000-000000000005",
                    "10000000-0000-0000-0000-000000000002", "热失控诱因与预防", "ch-005.html",
                    40, 2, 2, "[\"20000000-0000-0000-0000-000000000004\"]", "30000000-0000-0000-0000-000000000002"),
            new ChapterDef("20000000-0000-0000-0000-000000000010",
                    "10000000-0000-0000-0000-000000000002", "热扩散与阻断措施", "ch-010.html",
                    35, 3, 2, "[\"20000000-0000-0000-0000-000000000005\"]", null),
            new ChapterDef("20000000-0000-0000-0000-000000000006",
                    "10000000-0000-0000-0000-000000000003", "消防系统概述", "ch-006.html",
                    35, 1, 2, null, null),
            new ChapterDef("20000000-0000-0000-0000-000000000007",
                    "10000000-0000-0000-0000-000000000003", "灭火剂选择与适用", "ch-007.html",
                    40, 2, 3, "[\"20000000-0000-0000-0000-000000000006\"]", "30000000-0000-0000-0000-000000000003"),
            new ChapterDef("20000000-0000-0000-0000-000000000011",
                    "10000000-0000-0000-0000-000000000003", "消防联动与应急演练", "ch-011.html",
                    30, 3, 3, "[\"20000000-0000-0000-0000-000000000007\"]", null),
            new ChapterDef("20000000-0000-0000-0000-000000000008",
                    "10000000-0000-0000-0000-000000000004", "BMS系统架构与功能", "ch-008.html",
                    35, 1, 1, null, null),
            new ChapterDef("20000000-0000-0000-0000-000000000009",
                    "10000000-0000-0000-0000-000000000004", "BMS告警策略与响应", "ch-009.html",
                    40, 2, 2, "[\"20000000-0000-0000-0000-000000000008\"]", null),
            new ChapterDef("20000000-0000-0000-0000-000000000012",
                    "10000000-0000-0000-0000-000000000005", "日常巡检规范", "ch-012.html",
                    30, 1, 1, null, null),
            new ChapterDef("20000000-0000-0000-0000-000000000013",
                    "10000000-0000-0000-0000-000000000005", "异常处理流程", "ch-013.html",
                    35, 2, 2, "[\"20000000-0000-0000-0000-000000000012\"]", null)
    );

    @Override
    public void run(ApplicationArguments args) {
        int updated = 0;
        for (ChapterDef def : DEFINITIONS) {
            try {
                String content = loadHtml(def.htmlFile);
                if (content.isBlank()) continue;
                Optional<Chapter> existing = chapterRepo.findById(def.id);
                if (existing.isPresent()) {
                    Chapter ch = existing.get();
                    if (content.equals(ch.getContent()) && def.title.equals(ch.getTitle())) continue;
                    ch.setTitle(def.title);
                    ch.setContent(content);
                    ch.setDuration(def.duration);
                    chapterRepo.save(ch);
                    updated++;
                } else {
                    Course course = courseRepo.findById(def.courseId).orElse(null);
                    if (course == null) {
                        log.warn("课程不存在，跳过章节: {}", def.title);
                        continue;
                    }
                    Chapter ch = new Chapter();
                    ch.setId(def.id);
                    ch.setCourse(course);
                    ch.setTitle(def.title);
                    ch.setContent(content);
                    ch.setDuration(def.duration);
                    ch.setOrderNum(def.orderNum);
                    ch.setDifficultyLevel(def.difficultyLevel);
                    ch.setPrerequisiteIds(def.prerequisiteIds);
                    ch.setScenarioId(def.scenarioId);
                    chapterRepo.save(ch);
                    updated++;
                }
            } catch (Exception e) {
                log.warn("同步章节内容失败: {} - {}", def.title, e.getMessage());
            }
        }
        syncCourseMeta();
        if (updated > 0) {
            log.info("已同步 {} 个课程章节教学内容", updated);
        }
    }

    private void syncCourseMeta() {
        updateCourseDuration("10000000-0000-0000-0000-000000000001", 120);
        updateCourseDuration("10000000-0000-0000-0000-000000000002", 110);
        updateCourseDuration("10000000-0000-0000-0000-000000000003", 105);
        updateCourseDuration("10000000-0000-0000-0000-000000000004", 75);
        updateCourseDuration("10000000-0000-0000-0000-000000000005", 65);
    }

    private void updateCourseDuration(String courseId, int minutes) {
        courseRepo.findById(courseId).ifPresent(c -> {
            if (!Integer.valueOf(minutes).equals(c.getTotalDuration())) {
                c.setTotalDuration(minutes);
                courseRepo.save(c);
            }
        });
    }

    private String loadHtml(String fileName) throws Exception {
        ClassPathResource resource = new ClassPathResource("course-content/" + fileName);
        if (!resource.exists()) return "";
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }
}

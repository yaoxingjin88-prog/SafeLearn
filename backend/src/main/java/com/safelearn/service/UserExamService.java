package com.safelearn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.*;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserExamService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int COMPREHENSIVE_MIN_POOL = 5;
    private static final int COMPREHENSIVE_DRAW_COUNT = 20;

    private final UserRepository userRepo;
    private final ExamPaperRepository paperRepo;
    private final ChapterQuizRepository quizRepo;
    private final ChapterRepository chapterRepo;
    private final CourseRepository courseRepo;
    private final QuizAttemptRepository attemptRepo;
    private final ComprehensiveExamAttemptRepository comprehensiveAttemptRepo;
    private final ExamPaperAttemptRepository paperAttemptRepo;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Map<String, Object> listAvailableExams(String userId, int page, int pageSize) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        String dept = user.getDepartment() != null ? user.getDepartment().trim() : "";

        List<Map<String, Object>> all = new ArrayList<>();
        all.addAll(listPublishedPapers(dept, userId));
        all.addAll(listFormalChapterExams(dept));
        all.addAll(listComprehensiveExams(userId, dept));

        all.sort(Comparator.comparing(row -> String.valueOf(row.get("createdAt")), Comparator.reverseOrder()));

        int total = all.size();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
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

    private List<Map<String, Object>> listPublishedPapers(String dept, String userId) {
        return paperRepo.findAll().stream()
                .filter(p -> "published".equalsIgnoreCase(p.getStatus()))
                .filter(p -> matchesDepartment(p.getDepartment(), dept))
                .map(p -> toPaperRow(p, userId))
                .toList();
    }

    private List<Map<String, Object>> listFormalChapterExams(String dept) {
        Map<String, Chapter> chapterMap = chapterRepo.findAll().stream()
                .collect(Collectors.toMap(Chapter::getId, c -> c, (a, b) -> a));
        Map<String, Course> courseMap = courseRepo.findAll().stream()
                .collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (ChapterQuiz quiz : quizRepo.findAll()) {
            if (!"formal".equalsIgnoreCase(quiz.getExamType())) continue;
            if (!"published".equalsIgnoreCase(quiz.getStatus())) continue;
            Chapter chapter = chapterMap.get(quiz.getChapterId());
            if (chapter == null || chapter.getCourse() == null) continue;
            Course course = courseMap.get(chapter.getCourse().getId());
            if (course == null || !"published".equalsIgnoreCase(course.getStatus())) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", quiz.getId());
            row.put("sourceType", "chapter");
            row.put("title", quiz.getTitle());
            row.put("courseId", course.getId());
            row.put("courseTitle", course.getTitle());
            row.put("chapterId", chapter.getId());
            row.put("chapterTitle", chapter.getTitle());
            row.put("examType", "formal");
            row.put("timeLimit", quiz.getTimeLimit());
            row.put("passScore", quiz.getPassScore());
            row.put("totalScore", quiz.getTotalScore());
            row.put("status", quiz.getStatus());
            row.put("attemptCount", attemptRepo.countByQuizId(quiz.getId()));
            row.put("link", "/user/courses/" + course.getId() + "/chapters/" + chapter.getId() + "/quiz");
            row.put("createdAt", quiz.getCreatedAt() != null ? quiz.getCreatedAt().format(DT) : "");
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> listComprehensiveExams(String userId, String dept) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Course course : courseRepo.findByStatusOrderByCreatedAtDesc("published")) {
            int poolSize = countQuestionPool(course.getId());
            if (poolSize < COMPREHENSIVE_MIN_POOL) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "comprehensive:" + course.getId());
            row.put("sourceType", "comprehensive");
            row.put("title", course.getTitle() + " · 综合考试");
            row.put("courseId", course.getId());
            row.put("courseTitle", course.getTitle());
            row.put("examType", "formal");
            row.put("timeLimit", 45);
            row.put("passScore", 70);
            row.put("totalScore", 100);
            row.put("questionCount", COMPREHENSIVE_DRAW_COUNT);
            row.put("status", "published");
            long attempts = comprehensiveAttemptRepo.findByUserIdAndCourseIdOrderByStartedAtDesc(userId, course.getId()).size();
            boolean passed = comprehensiveAttemptRepo
                    .findTopByUserIdAndCourseIdAndPassedTrueOrderByScoreDesc(userId, course.getId()).isPresent();
            row.put("attemptCount", attempts);
            row.put("examPassed", passed);
            row.put("link", "/user/courses/" + course.getId() + "/comprehensive-exam");
            row.put("createdAt", course.getCreatedAt() != null ? course.getCreatedAt().format(DT) : "");
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> toPaperRow(ExamPaper paper, String userId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", paper.getId());
        row.put("sourceType", "paper");
        row.put("title", paper.getTitle());
        row.put("examType", paper.getExamType());
        row.put("department", paper.getDepartment());
        row.put("timeLimit", paper.getTimeLimit());
        row.put("passScore", paper.getPassScore());
        row.put("totalScore", paper.getTotalScore());
        row.put("status", paper.getStatus());
        row.put("attemptCount", userId != null ? paperAttemptRepo.countByUserIdAndPaperId(userId, paper.getId()) : 0);
        row.put("examPassed", userId != null && paperAttemptRepo
                .findTopByUserIdAndPaperIdAndPassedTrueOrderByScoreDesc(userId, paper.getId()).isPresent());
        row.put("link", "/user/exams/" + paper.getId());
        row.put("createdAt", paper.getPublishedAt() != null ? paper.getPublishedAt().format(DT)
                : (paper.getCreatedAt() != null ? paper.getCreatedAt().format(DT) : ""));
        return row;
    }

    private boolean matchesDepartment(String targetDept, String userDept) {
        if (targetDept == null || targetDept.isBlank() || "全员".equals(targetDept.trim())) return true;
        if (userDept == null || userDept.isBlank()) return true;
        return targetDept.trim().equals(userDept.trim());
    }

    private int countQuestionPool(String courseId) {
        List<String> chapterIds = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId).stream()
                .map(Chapter::getId).toList();
        if (chapterIds.isEmpty()) return 0;
        int total = 0;
        for (ChapterQuiz quiz : quizRepo.findByChapterIdIn(chapterIds)) {
            total += countQuestions(quiz.getQuestions());
        }
        return total;
    }

    private int countQuestions(String questionsJson) {
        if (questionsJson == null || questionsJson.isBlank()) return 0;
        try {
            List<?> list = objectMapper.readValue(questionsJson, List.class);
            return list.size();
        } catch (Exception e) {
            return 0;
        }
    }
}

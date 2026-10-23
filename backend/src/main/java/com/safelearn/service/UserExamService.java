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
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

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
    public Map<String, Object> listAvailableExams(String userId, int page, int pageSize, String tab) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        String dept = user.getDepartment() != null ? user.getDepartment().trim() : "";

        List<Map<String, Object>> all = new ArrayList<>();
        all.addAll(listPublishedPapers(dept, userId));
        all.addAll(listFormalChapterExams(dept, userId));
        all.addAll(listComprehensiveExams(userId, dept));

        all.sort(Comparator.comparing(row -> String.valueOf(row.get("createdAt")), Comparator.reverseOrder()));

        List<Map<String, Object>> filtered = filterByTab(all, tab);
        int total = filtered.size();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        int from = Math.max(0, (safePage - 1) * safeSize);
        int to = Math.min(total, from + safeSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", from >= total ? List.of() : filtered.subList(from, to));
        result.put("total", total);
        result.put("page", safePage);
        result.put("pageSize", safeSize);
        result.put("totalPages", safeSize == 0 ? 0 : (int) Math.ceil(total * 1.0 / safeSize));
        result.put("stats", buildStats(userId, all));
        return result;
    }

    private List<Map<String, Object>> filterByTab(List<Map<String, Object>> all, String tab) {
        if (tab == null || tab.isBlank() || "all".equalsIgnoreCase(tab)) {
            return all;
        }
        return all.stream().filter(row -> {
            String sourceType = String.valueOf(row.getOrDefault("sourceType", ""));
            String examType = String.valueOf(row.getOrDefault("examType", ""));
            return switch (tab.toLowerCase()) {
                case "formal" -> "paper".equals(sourceType) && "formal".equalsIgnoreCase(examType);
                case "comprehensive" -> "comprehensive".equals(sourceType);
                case "chapter" -> "chapter".equals(sourceType);
                default -> true;
            };
        }).toList();
    }

    private Map<String, Object> buildStats(String userId, List<Map<String, Object>> allExams) {
        long inProgress = paperAttemptRepo.countByUserIdAndCompletedAtIsNull(userId)
                + attemptRepo.countInProgressByUserId(userId)
                + comprehensiveAttemptRepo.countByUserIdAndCompletedAtIsNull(userId);

        long completed = allExams.stream()
                .filter(row -> Boolean.TRUE.equals(row.get("examPassed")))
                .count();

        long pending = allExams.stream()
                .filter(row -> "available".equals(row.get("examStatus")) || "in_progress".equals(row.get("examStatus")))
                .count();

        double averageScore = computeAverageScore(userId);
        int percentile = computePercentileRank(userId, averageScore);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("pendingExams", pending);
        stats.put("inProgressExams", inProgress);
        stats.put("completedExams", completed);
        stats.put("averageScore", Math.round(averageScore * 10.0) / 10.0);
        stats.put("percentileRank", percentile);
        return stats;
    }

    private double computeAverageScore(String userId) {
        List<Integer> scores = new ArrayList<>();
        for (ExamPaperAttempt attempt : paperAttemptRepo.findByUserIdAndCompletedAtIsNotNull(userId)) {
            if (attempt.getScore() != null) scores.add(attempt.getScore());
        }
        for (QuizAttempt attempt : attemptRepo.findCompletedByUserId(userId)) {
            if (attempt.getScore() != null) scores.add(attempt.getScore());
        }
        for (ComprehensiveExamAttempt attempt : comprehensiveAttemptRepo.findByUserIdAndCompletedAtIsNotNull(userId)) {
            if (attempt.getScore() != null) scores.add(attempt.getScore());
        }
        if (scores.isEmpty()) return 0;
        return scores.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private int computePercentileRank(String userId, double userAverage) {
        if (userAverage <= 0) return 0;
        Map<String, List<Integer>> userScores = new HashMap<>();
        for (ExamPaperAttempt attempt : paperAttemptRepo.findAll()) {
            if (attempt.getCompletedAt() == null || attempt.getScore() == null) continue;
            userScores.computeIfAbsent(attempt.getUserId(), k -> new ArrayList<>()).add(attempt.getScore());
        }
        for (QuizAttempt attempt : attemptRepo.findAll()) {
            if (attempt.getCompletedAt() == null || attempt.getScore() == null || attempt.getUser() == null) continue;
            userScores.computeIfAbsent(attempt.getUser().getId(), k -> new ArrayList<>()).add(attempt.getScore());
        }
        for (ComprehensiveExamAttempt attempt : comprehensiveAttemptRepo.findAll()) {
            if (attempt.getCompletedAt() == null || attempt.getScore() == null || attempt.getUser() == null) continue;
            userScores.computeIfAbsent(attempt.getUser().getId(), k -> new ArrayList<>()).add(attempt.getScore());
        }
        if (userScores.size() <= 1) return 50;

        List<Double> averages = userScores.values().stream()
                .map(list -> list.stream().mapToInt(Integer::intValue).average().orElse(0))
                .sorted()
                .toList();
        long lower = averages.stream().filter(avg -> avg < userAverage).count();
        return (int) Math.round(lower * 100.0 / averages.size());
    }

    private List<Map<String, Object>> listPublishedPapers(String dept, String userId) {
        return paperRepo.findAll().stream()
                .filter(p -> "published".equalsIgnoreCase(p.getStatus()))
                .filter(p -> matchesDepartment(p.getDepartment(), dept))
                .map(p -> toPaperRow(p, userId))
                .toList();
    }

    private List<Map<String, Object>> listFormalChapterExams(String dept, String userId) {
        Map<String, Chapter> chapterMap = chapterRepo.findAll().stream()
                .collect(Collectors.toMap(Chapter::getId, c -> c, (a, b) -> a));
        Map<String, Course> courseMap = courseRepo.findAll().stream()
                .collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));
        Set<String> passedQuizIds = attemptRepo.findPassedAttemptsByUserId(userId).stream()
                .filter(a -> a.getQuiz() != null)
                .map(a -> a.getQuiz().getId())
                .collect(Collectors.toSet());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (ChapterQuiz quiz : quizRepo.findAll()) {
            if (!"formal".equalsIgnoreCase(quiz.getExamType())) continue;
            if (!"published".equalsIgnoreCase(quiz.getStatus())) continue;
            Chapter chapter = chapterMap.get(quiz.getChapterId());
            if (chapter == null || chapter.getCourse() == null) continue;
            Course course = courseMap.get(chapter.getCourse().getId());
            if (course == null || !"published".equalsIgnoreCase(course.getStatus())) continue;

            long usedAttempts = attemptRepo.countByUserIdAndQuizId(userId, quiz.getId());
            boolean passed = passedQuizIds.contains(quiz.getId());
            boolean inProgress = !attemptRepo.findInProgressByUserIdAndQuizId(userId, quiz.getId()).isEmpty();
            int maxAttempts = DEFAULT_MAX_ATTEMPTS;
            Integer bestScore = attemptRepo.findTopByUserIdAndQuizIdOrderByScoreDesc(userId, quiz.getId())
                    .map(QuizAttempt::getScore)
                    .orElse(null);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", quiz.getId());
            row.put("sourceType", "chapter");
            row.put("title", quiz.getTitle());
            row.put("description", course.getTitle() + " · " + chapter.getTitle() + " 章节测验");
            row.put("courseId", course.getId());
            row.put("courseTitle", course.getTitle());
            row.put("chapterId", chapter.getId());
            row.put("chapterTitle", chapter.getTitle());
            row.put("examType", "formal");
            row.put("timeLimit", quiz.getTimeLimit());
            row.put("passScore", quiz.getPassScore());
            row.put("totalScore", quiz.getTotalScore());
            row.put("questionCount", countQuestions(quiz.getQuestions()));
            row.put("status", quiz.getStatus());
            row.put("attemptCount", usedAttempts);
            row.put("usedAttempts", usedAttempts);
            row.put("maxAttempts", maxAttempts);
            row.put("examPassed", passed);
            row.put("bestScore", bestScore);
            row.put("validityText", "长期有效");
            row.put("examStatus", resolveExamStatus(passed, inProgress, usedAttempts, maxAttempts));
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

            long usedAttempts = comprehensiveAttemptRepo.findByUserIdAndCourseIdOrderByStartedAtDesc(userId, course.getId()).size();
            boolean passed = comprehensiveAttemptRepo
                    .findTopByUserIdAndCourseIdAndPassedTrueOrderByScoreDesc(userId, course.getId()).isPresent();
            boolean inProgress = comprehensiveAttemptRepo
                    .findTopByUserIdAndCourseIdAndCompletedAtIsNullOrderByStartedAtDesc(userId, course.getId())
                    .isPresent();
            int maxAttempts = DEFAULT_MAX_ATTEMPTS;
            Integer bestScore = comprehensiveAttemptRepo
                    .findTopByUserIdAndCourseIdAndPassedTrueOrderByScoreDesc(userId, course.getId())
                    .map(ComprehensiveExamAttempt::getScore)
                    .orElse(null);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "comprehensive:" + course.getId());
            row.put("sourceType", "comprehensive");
            row.put("title", course.getTitle() + " · 综合考试");
            row.put("description", "覆盖《" + course.getTitle() + "》全部章节知识点。");
            row.put("courseId", course.getId());
            row.put("courseTitle", course.getTitle());
            row.put("examType", "formal");
            row.put("timeLimit", 45);
            row.put("passScore", 70);
            row.put("totalScore", 100);
            row.put("questionCount", COMPREHENSIVE_DRAW_COUNT);
            row.put("status", "published");
            row.put("attemptCount", usedAttempts);
            row.put("usedAttempts", usedAttempts);
            row.put("maxAttempts", maxAttempts);
            row.put("examPassed", passed);
            row.put("bestScore", bestScore);
            row.put("validityText", "长期有效");
            row.put("examStatus", resolveExamStatus(passed, inProgress, usedAttempts, maxAttempts));
            row.put("link", "/user/courses/" + course.getId() + "/comprehensive-exam");
            row.put("createdAt", course.getCreatedAt() != null ? course.getCreatedAt().format(DT) : "");
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> toPaperRow(ExamPaper paper, String userId) {
        long usedAttempts = userId != null ? paperAttemptRepo.countByUserIdAndPaperId(userId, paper.getId()) : 0;
        boolean passed = userId != null && paperAttemptRepo
                .findTopByUserIdAndPaperIdAndPassedTrueOrderByScoreDesc(userId, paper.getId()).isPresent();
        boolean inProgress = userId != null && paperAttemptRepo
                .findTopByUserIdAndPaperIdAndCompletedAtIsNullOrderByStartedAtDesc(userId, paper.getId())
                .isPresent();
        int maxAttempts = parseAttemptLimit(paper);
        Integer bestScore = userId != null
                ? paperAttemptRepo.findTopByUserIdAndPaperIdAndPassedTrueOrderByScoreDesc(userId, paper.getId())
                .map(ExamPaperAttempt::getScore)
                .orElseGet(() -> paperAttemptRepo.findByUserIdAndPaperIdOrderByStartedAtDesc(userId, paper.getId()).stream()
                        .filter(a -> a.getCompletedAt() != null && a.getScore() != null)
                        .map(ExamPaperAttempt::getScore)
                        .max(Integer::compareTo)
                        .orElse(null))
                : null;

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", paper.getId());
        row.put("sourceType", "paper");
        row.put("title", paper.getTitle());
        row.put("description", parsePaperDescription(paper));
        row.put("examType", paper.getExamType());
        row.put("department", paper.getDepartment());
        row.put("timeLimit", paper.getTimeLimit());
        row.put("passScore", paper.getPassScore());
        row.put("totalScore", paper.getTotalScore());
        row.put("questionCount", countQuestions(paper.getQuestionsSnapshot()));
        row.put("status", paper.getStatus());
        row.put("attemptCount", usedAttempts);
        row.put("usedAttempts", usedAttempts);
        row.put("maxAttempts", maxAttempts);
        row.put("examPassed", passed);
        row.put("bestScore", bestScore);
        row.put("validityText", "长期有效");
        row.put("examStatus", resolveExamStatus(passed, inProgress, usedAttempts, maxAttempts));
        row.put("link", "/user/exams/" + paper.getId());
        row.put("createdAt", paper.getPublishedAt() != null ? paper.getPublishedAt().format(DT)
                : (paper.getCreatedAt() != null ? paper.getCreatedAt().format(DT) : ""));
        return row;
    }

    private String resolveExamStatus(boolean passed, boolean inProgress, long usedAttempts, int maxAttempts) {
        if (passed) return "passed";
        if (inProgress) return "in_progress";
        if (usedAttempts >= maxAttempts) return "exhausted";
        return "available";
    }

    private int parseAttemptLimit(ExamPaper paper) {
        try {
            if (paper.getConfig() == null || paper.getConfig().isBlank()) return DEFAULT_MAX_ATTEMPTS;
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.readValue(paper.getConfig(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> antiCheat = (Map<String, Object>) config.get("antiCheat");
            if (antiCheat != null && antiCheat.get("attemptLimit") instanceof Number n) {
                return Math.max(1, n.intValue());
            }
        } catch (Exception ignored) {
            // fall through
        }
        return DEFAULT_MAX_ATTEMPTS;
    }

    private String parsePaperDescription(ExamPaper paper) {
        try {
            if (paper.getConfig() != null && !paper.getConfig().isBlank()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = objectMapper.readValue(paper.getConfig(), Map.class);
                @SuppressWarnings("unchecked")
                Map<String, Object> rules = (Map<String, Object>) config.get("rules");
                if (rules != null && rules.get("description") instanceof String desc && !desc.isBlank()) {
                    return desc.trim();
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "检验储能安全知识掌握情况，请认真作答。";
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

package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.*;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private static final int COMPREHENSIVE_DRAW_COUNT = 20;
    private static final int COMPREHENSIVE_PASS_SCORE = 70;
    private static final int COMPREHENSIVE_TIME_LIMIT = 45;
    private static final int COMPREHENSIVE_MIN_POOL = 5;
    private static final double COMPREHENSIVE_MIN_COMPLETION_RATIO = 0.6;

    private final ChapterQuizRepository quizRepo;
    private final ComprehensiveExamAttemptRepository comprehensiveAttemptRepo;
    private final QuizAttemptRepository attemptRepo;
    private final UserRepository userRepo;
    private final UserProgressRepository progressRepo;
    private final ChapterRepository chapterRepo;
    private final CourseRepository courseRepo;
    private final CertificateService certificateService;
    private final ObjectMapper objectMapper;
    private final AdminInboxScheduler inboxScheduler;

    /**
     * 获取章节测验（不含正确答案）
     */
    public Map<String, Object> getQuizByChapterId(String chapterId) {
        ChapterQuiz quiz = quizRepo.findByChapterId(chapterId)
                .orElseThrow(() -> new RuntimeException("该章节暂无测验"));

        Map<String, Object> result = new HashMap<>();
        result.put("id", quiz.getId());
        result.put("chapterId", quiz.getChapterId());
        result.put("title", quiz.getTitle());
        result.put("passScore", quiz.getPassScore());
        result.put("timeLimit", quiz.getTimeLimit());
        result.put("questions", sanitizeQuestions(quiz.getQuestions()));
        return result;
    }

    /**
     * 检查章节是否有测验
     */
    public boolean hasQuiz(String chapterId) {
        return quizRepo.existsByChapterId(chapterId);
    }

    /**
     * 章节测验与学习状态（用于前端展示）
     */
    public Map<String, Object> getChapterQuizStatus(String userId, String chapterId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("exists", hasQuiz(chapterId));
        if (userId == null) {
            return result;
        }

        boolean chapterCompleted = progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, chapterId);
        result.put("chapterCompleted", chapterCompleted);

        progressRepo.findByUserIdAndChapterId(userId, chapterId).ifPresent(p -> {
            if (p.getMasteryLevel() != null) {
                result.put("masteryLevel", p.getMasteryLevel());
            }
        });

        if (!hasQuiz(chapterId)) {
            return result;
        }

        List<QuizAttempt> attempts = attemptRepo.findByUserIdAndChapterId(userId, chapterId);
        boolean quizPassed = attempts.stream()
                .anyMatch(a -> a.getCompletedAt() != null && Boolean.TRUE.equals(a.getPassed()));
        result.put("quizPassed", quizPassed);
        if (quizPassed) {
            int bestScore = attempts.stream()
                    .filter(a -> a.getCompletedAt() != null && Boolean.TRUE.equals(a.getPassed()))
                    .mapToInt(a -> a.getScore() != null ? a.getScore() : 0)
                    .max()
                    .orElse(0);
            result.put("bestScore", bestScore);
        }
        return result;
    }

    /**
     * 开始测验
     */
    @Transactional
    public Map<String, Object> startQuiz(String userId, String quizId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        ChapterQuiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("测验不存在"));

        String chapterId = quiz.getChapterId();
        if (progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, chapterId)) {
            throw new RuntimeException("本章已完成学习，无需重复参加测验");
        }
        boolean alreadyPassed = attemptRepo.findByUserIdAndChapterId(userId, chapterId).stream()
                .anyMatch(a -> a.getCompletedAt() != null && Boolean.TRUE.equals(a.getPassed()));
        if (alreadyPassed) {
            throw new RuntimeException("本章测验已通过，无需重复参加");
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(UUID.randomUUID().toString());
        attempt.setUser(user);
        attempt.setQuiz(quiz);
        attempt.setStartedAt(LocalDateTime.now());
        attempt = attemptRepo.save(attempt);

        Map<String, Object> result = new HashMap<>();
        result.put("attemptId", attempt.getId());
        result.put("quiz", sanitizeQuiz(quiz));
        result.put("startedAt", attempt.getStartedAt());
        return result;
    }

    /**
     * 提交测验答案
     */
    @Transactional
    public Map<String, Object> submitQuiz(String userId, String attemptId, Map<String, Object> userAnswers) {
        QuizAttempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("测验记录不存在"));

        if (!attempt.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权操作该测验记录");
        }
        if (attempt.getCompletedAt() != null) {
            throw new RuntimeException("该测验已提交");
        }

        ChapterQuiz quiz = attempt.getQuiz();
        List<Map<String, Object>> questions = parseQuestions(quiz.getQuestions());

        // 评分
        int totalScore = 0;
        int maxScore = questions.size() * 100 / questions.size(); // 每题等分
        List<Map<String, Object>> results = new ArrayList<>();
        List<Map<String, Object>> wrongQuestions = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Map<String, String> answers = (Map<String, String>) userAnswers.get("answers");

        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> q = questions.get(i);
            String questionId = (String) q.get("id");
            String userAnswer = answers != null ? answers.get(questionId) : null;

            boolean correct = checkAnswer(q, userAnswer);
            int questionScore = correct ? (100 / questions.size()) : 0;
            totalScore += questionScore;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("questionId", questionId);
            result.put("correct", correct);
            result.put("userAnswer", userAnswer);
            result.put("correctAnswer", getCorrectAnswer(q));
            result.put("explanation", q.get("explanation"));
            results.add(result);

            if (!correct) {
                Map<String, Object> wrong = new LinkedHashMap<>();
                wrong.put("questionId", questionId);
                wrong.put("question", q.get("question"));
                wrong.put("options", q.get("options"));
                wrong.put("userAnswer", userAnswer);
                wrong.put("correctAnswer", getCorrectAnswer(q));
                wrong.put("explanation", q.get("explanation"));
                wrong.put("type", q.get("type"));
                wrongQuestions.add(wrong);
            }
        }

        // 确保不超过100分
        totalScore = Math.min(totalScore, 100);
        boolean passed = totalScore >= quiz.getPassScore();

        // 保存答题记录
        Map<String, Object> answersData = new HashMap<>();
        answersData.put("answers", answers);
        answersData.put("results", results);
        answersData.put("wrongQuestions", wrongQuestions);

        attempt.setAnswers(toJson(answersData));
        attempt.setScore(totalScore);
        attempt.setPassed(passed);
        attempt.setMasteryLevel(totalScore);
        attempt.setCompletedAt(LocalDateTime.now());
        attemptRepo.save(attempt);

        if (!passed) {
            inboxScheduler.notifyQuizFail(attempt, totalScore);
        }

        // 更新用户进度中的掌握度
        updateMasteryLevel(userId, quiz.getChapterId(), totalScore);
        if (passed) {
            markChapterCompleted(userId, quiz.getChapterId(), totalScore);
        }

        // 构建返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("attemptId", attempt.getId());
        response.put("score", totalScore);
        response.put("passed", passed);
        response.put("passScore", quiz.getPassScore());
        response.put("masteryLevel", totalScore);
        response.put("rating", calculateRating(totalScore));
        response.put("feedback", buildFeedback(totalScore, wrongQuestions.size(), questions.size(), passed));
        response.put("results", results);
        response.put("wrongQuestions", wrongQuestions);
        return response;
    }

    /**
     * 获取测验历史
     */
    public List<Map<String, Object>> getQuizHistory(String userId, String chapterId) {
        List<QuizAttempt> attempts = attemptRepo.findByUserIdAndChapterId(userId, chapterId);
        return attempts.stream().map(this::toAttemptInfo).toList();
    }

    /**
     * 获取用户的错题本
     */
    public Map<String, Object> getWrongQuestions(String userId) {
        List<QuizAttempt> passedAttempts = attemptRepo.findPassedAttemptsByUserId(userId);
        List<QuizAttempt> allAttempts = attemptRepo.findAll().stream()
                .filter(a -> a.getUser().getId().equals(userId))
                .toList();

        // 按章节分组收集错题
        Map<String, List<Map<String, Object>>> chapterWrongQuestions = new LinkedHashMap<>();

        for (QuizAttempt attempt : allAttempts) {
            if (attempt.getAnswers() == null) continue;

            Map<String, Object> answersData = parseJson(attempt.getAnswers());
            if (answersData == null) continue;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> wrongQuestions = (List<Map<String, Object>>) answersData.get("wrongQuestions");
            if (wrongQuestions == null || wrongQuestions.isEmpty()) continue;

            String chapterId = attempt.getQuiz().getChapterId();
            String chapterTitle = attempt.getQuiz().getTitle().replace("测验：", "");
            String courseId = chapterRepo.findById(chapterId)
                    .map(ch -> ch.getCourse().getId())
                    .orElse(null);

            chapterWrongQuestions.computeIfAbsent(chapterId, k -> new ArrayList<>());
            for (Map<String, Object> wq : wrongQuestions) {
                Map<String, Object> entry = new LinkedHashMap<>(wq);
                entry.put("chapterId", chapterId);
                entry.put("chapterTitle", chapterTitle);
                if (courseId != null) entry.put("courseId", courseId);
                entry.put("attemptId", attempt.getId());
                entry.put("attemptDate", attempt.getCompletedAt());
                chapterWrongQuestions.get(chapterId).add(entry);
            }
        }

        // 去重（同一道题只保留最新的错误记录）
        Map<String, List<Map<String, Object>>> deduplicated = new LinkedHashMap<>();
        for (var entry : chapterWrongQuestions.entrySet()) {
            Map<String, Map<String, Object>> latestByQuestion = new LinkedHashMap<>();
            for (Map<String, Object> wq : entry.getValue()) {
                String questionId = (String) wq.get("questionId");
                Map<String, Object> existing = latestByQuestion.get(questionId);
                if (existing == null || isLaterAttempt(wq, existing)) {
                    latestByQuestion.put(questionId, wq);
                }
            }
            deduplicated.put(entry.getKey(), new ArrayList<>(latestByQuestion.values()));
        }

        int totalWrong = deduplicated.values().stream().mapToInt(List::size).sum();

        Map<String, Object> result = new HashMap<>();
        result.put("chapters", deduplicated);
        result.put("totalWrong", totalWrong);
        return result;
    }

    /**
     * 获取测验详情（含正确答案）
     */
    public Map<String, Object> getQuizDetail(String quizId) {
        ChapterQuiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("测验不存在"));
        return sanitizeQuiz(quiz);
    }

    // ========== 综合考试 ==========

    public Map<String, Object> getComprehensiveExamStatus(String userId, String courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
        int totalChapters = chapters.size();
        long completedCount = chapters.stream()
                .filter(ch -> progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId()))
                .count();

        int poolSize = countQuestionPool(courseId);
        boolean available = poolSize >= COMPREHENSIVE_MIN_POOL;
        boolean eligible = available && totalChapters > 0
                && completedCount >= Math.ceil(totalChapters * COMPREHENSIVE_MIN_COMPLETION_RATIO);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available", available);
        result.put("eligible", eligible);
        result.put("questionPoolSize", poolSize);
        result.put("drawCount", Math.min(COMPREHENSIVE_DRAW_COUNT, poolSize));
        result.put("passScore", COMPREHENSIVE_PASS_SCORE);
        result.put("timeLimit", COMPREHENSIVE_TIME_LIMIT);
        result.put("completedChapterCount", completedCount);
        result.put("totalChapterCount", totalChapters);
        result.put("requiredCompletionRatio", COMPREHENSIVE_MIN_COMPLETION_RATIO);
        result.put("courseTitle", course.getTitle());

        if (!available) {
            result.put("eligibilityHint", "课程章节测验不足，暂无法参加综合考试");
        } else if (!eligible) {
            int required = (int) Math.ceil(totalChapters * COMPREHENSIVE_MIN_COMPLETION_RATIO);
            result.put("eligibilityHint", "需完成至少 " + required + " 个章节（当前 " + completedCount + " 个）");
        }

        comprehensiveAttemptRepo.findTopByUserIdAndCourseIdAndPassedTrueOrderByScoreDesc(userId, courseId)
                .ifPresentOrElse(attempt -> {
                    result.put("examPassed", true);
                    result.put("bestScore", attempt.getScore());
                }, () -> {
                    result.put("examPassed", false);
                    List<ComprehensiveExamAttempt> attempts =
                            comprehensiveAttemptRepo.findByUserIdAndCourseIdOrderByStartedAtDesc(userId, courseId);
                    attempts.stream()
                            .filter(a -> a.getCompletedAt() != null)
                            .mapToInt(a -> a.getScore() != null ? a.getScore() : 0)
                            .max()
                            .ifPresent(best -> result.put("bestScore", best));
                });

        return result;
    }

    @Transactional
    public Map<String, Object> startComprehensiveExam(String userId, String courseId) {
        Map<String, Object> status = getComprehensiveExamStatus(userId, courseId);
        if (!Boolean.TRUE.equals(status.get("available"))) {
            throw new RuntimeException("该课程暂不支持综合考试");
        }
        if (!Boolean.TRUE.equals(status.get("eligible"))) {
            throw new RuntimeException((String) status.getOrDefault("eligibilityHint", "暂不符合参加条件"));
        }
        if (Boolean.TRUE.equals(status.get("examPassed"))) {
            throw new RuntimeException("综合考试已通过，无需重复参加");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));

        int drawCount = (int) status.get("drawCount");
        List<Map<String, Object>> drawn = drawCrossChapterQuestions(courseId, drawCount);
        if (drawn.isEmpty()) {
            throw new RuntimeException("题库为空，无法开始综合考试");
        }

        ComprehensiveExamAttempt attempt = new ComprehensiveExamAttempt();
        attempt.setId(UUID.randomUUID().toString());
        attempt.setUser(user);
        attempt.setCourseId(courseId);
        attempt.setQuestions(toJson(drawn));
        attempt.setPassScore(COMPREHENSIVE_PASS_SCORE);
        attempt = comprehensiveAttemptRepo.save(attempt);

        Map<String, Object> quiz = new LinkedHashMap<>();
        quiz.put("id", "comprehensive-" + courseId);
        quiz.put("courseId", courseId);
        quiz.put("title", "综合考试：" + course.getTitle());
        quiz.put("passScore", COMPREHENSIVE_PASS_SCORE);
        quiz.put("timeLimit", COMPREHENSIVE_TIME_LIMIT);
        quiz.put("questions", sanitizeDrawnQuestions(drawn));

        Map<String, Object> result = new HashMap<>();
        result.put("attemptId", attempt.getId());
        result.put("quiz", quiz);
        result.put("startedAt", attempt.getStartedAt());
        return result;
    }

    @Transactional
    public Map<String, Object> submitComprehensiveExam(String userId, String attemptId, Map<String, Object> userAnswers) {
        ComprehensiveExamAttempt attempt = comprehensiveAttemptRepo.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("考试记录不存在"));

        if (!attempt.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权操作该考试记录");
        }
        if (attempt.getCompletedAt() != null) {
            throw new RuntimeException("该考试已提交");
        }

        List<Map<String, Object>> questions = parseQuestions(attempt.getQuestions());
        int passScore = attempt.getPassScore() != null ? attempt.getPassScore() : COMPREHENSIVE_PASS_SCORE;

        int totalScore = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        List<Map<String, Object>> wrongQuestions = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Map<String, String> answers = (Map<String, String>) userAnswers.get("answers");

        for (Map<String, Object> q : questions) {
            String questionId = (String) q.get("id");
            String userAnswer = answers != null ? answers.get(questionId) : null;
            boolean correct = checkAnswer(q, userAnswer);
            int questionScore = correct ? (100 / questions.size()) : 0;
            totalScore += questionScore;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("questionId", questionId);
            result.put("correct", correct);
            result.put("userAnswer", userAnswer);
            result.put("correctAnswer", getCorrectAnswer(q));
            result.put("explanation", q.get("explanation"));
            result.put("sourceChapterTitle", q.get("sourceChapterTitle"));
            results.add(result);

            if (!correct) {
                Map<String, Object> wrong = new LinkedHashMap<>();
                wrong.put("questionId", questionId);
                wrong.put("question", q.get("question"));
                wrong.put("options", q.get("options"));
                wrong.put("userAnswer", userAnswer);
                wrong.put("correctAnswer", getCorrectAnswer(q));
                wrong.put("explanation", q.get("explanation"));
                wrong.put("type", q.get("type"));
                wrong.put("sourceChapterTitle", q.get("sourceChapterTitle"));
                wrong.put("sourceChapterId", q.get("sourceChapterId"));
                wrongQuestions.add(wrong);
            }
        }

        totalScore = Math.min(totalScore, 100);
        boolean passed = totalScore >= passScore;

        Map<String, Object> answersData = new HashMap<>();
        answersData.put("answers", answers);
        answersData.put("results", results);
        answersData.put("wrongQuestions", wrongQuestions);

        attempt.setAnswers(toJson(answersData));
        attempt.setScore(totalScore);
        attempt.setPassed(passed);
        attempt.setCompletedAt(LocalDateTime.now());
        comprehensiveAttemptRepo.save(attempt);

        if (!passed) {
            inboxScheduler.notifyComprehensiveExamFail(attempt.getId(), userId, totalScore);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("attemptId", attempt.getId());
        response.put("examType", "comprehensive");
        response.put("courseId", attempt.getCourseId());
        response.put("score", totalScore);
        response.put("passed", passed);
        response.put("passScore", passScore);
        response.put("masteryLevel", totalScore);
        response.put("rating", calculateRating(totalScore));
        response.put("feedback", buildComprehensiveFeedback(totalScore, wrongQuestions.size(), questions.size(), passed));
        response.put("results", results);
        response.put("wrongQuestions", wrongQuestions);
        response.put("questions", sanitizeDrawnQuestions(questions));

        if (passed) {
            certificateService.tryIssueOnComprehensiveExamPass(userId, attempt.getCourseId(), totalScore)
                    .ifPresent(cert -> response.put("newCertificate", cert));
        }

        return response;
    }

    public List<Map<String, Object>> getComprehensiveExamHistory(String userId, String courseId) {
        return comprehensiveAttemptRepo.findByUserIdAndCourseIdOrderByStartedAtDesc(userId, courseId).stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("courseId", a.getCourseId());
                    m.put("score", a.getScore());
                    m.put("passed", a.getPassed());
                    m.put("startedAt", a.getStartedAt());
                    m.put("completedAt", a.getCompletedAt());
                    return m;
                }).toList();
    }

    // ========== 私有辅助方法 ==========

    private void markChapterCompleted(String userId, String chapterId, int masteryLevel) {
        Chapter chapter = chapterRepo.findById(chapterId).orElse(null);
        if (chapter == null) return;

        User user = userRepo.findById(userId).orElse(null);
        Course course = chapter.getCourse();
        if (user == null || course == null) return;

        UserProgress progress = progressRepo.findByUserIdAndChapterId(userId, chapterId)
                .orElseGet(() -> {
                    UserProgress p = new UserProgress();
                    p.setUser(user);
                    p.setCourse(course);
                    p.setChapter(chapter);
                    return p;
                });

        if (!Boolean.TRUE.equals(progress.getCompleted())) {
            progress.setProgress(100);
            progress.setCompleted(true);
        }
        if (progress.getMasteryLevel() == null || masteryLevel > progress.getMasteryLevel()) {
            progress.setMasteryLevel(masteryLevel);
        }
        progress.setLastAccessAt(LocalDateTime.now());
        progressRepo.save(progress);
        certificateService.tryIssueOnCourseComplete(userId, chapter.getCourse().getId());
    }

    private void updateMasteryLevel(String userId, String chapterId, int masteryLevel) {
        // 查找或创建用户进度记录
        progressRepo.findByUserIdAndChapterId(userId, chapterId).ifPresentOrElse(
                progress -> {
                    // 只有新掌握度更高时才更新
                    if (progress.getMasteryLevel() == null || masteryLevel > progress.getMasteryLevel()) {
                        progress.setMasteryLevel(masteryLevel);
                        progressRepo.save(progress);
                    }
                },
                () -> {
                    // 如果没有进度记录，需要知道 courseId
                    // 这种情况不应该发生，因为用户应该先开始学习章节
                    log.warn("No progress record found for user {} chapter {}", userId, chapterId);
                }
        );
    }

    private boolean checkAnswer(Map<String, Object> question, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) return false;

        String type = (String) question.getOrDefault("type", "single");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) question.get("options");
        if (options == null) return false;

        if ("multiple".equals(type)) {
            // 多选题：需要选中所有正确选项
            Set<String> correctIds = new HashSet<>();
            for (Map<String, Object> opt : options) {
                if (Boolean.TRUE.equals(opt.get("correct"))) {
                    correctIds.add((String) opt.get("id"));
                }
            }
            Set<String> userSelected = new HashSet<>(Arrays.asList(userAnswer.split(",")));
            return correctIds.equals(userSelected);
        } else {
            // 单选题和判断题：匹配正确选项
            for (Map<String, Object> opt : options) {
                if (Boolean.TRUE.equals(opt.get("correct"))) {
                    return opt.get("id").equals(userAnswer);
                }
            }
            return false;
        }
    }

    private String getCorrectAnswer(Map<String, Object> question) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) question.get("options");
        if (options == null) return "";

        List<String> correctIds = new ArrayList<>();
        for (Map<String, Object> opt : options) {
            if (Boolean.TRUE.equals(opt.get("correct"))) {
                correctIds.add((String) opt.get("id"));
            }
        }
        return String.join(",", correctIds);
    }

    private boolean isLaterAttempt(Map<String, Object> candidate, Map<String, Object> existing) {
        Object cDate = candidate.get("attemptDate");
        Object eDate = existing.get("attemptDate");
        if (cDate == null) return false;
        if (eDate == null) return true;
        return cDate.toString().compareTo(eDate.toString()) >= 0;
    }

    private List<Map<String, Object>> sanitizeQuestions(String questionsJson) {
        List<Map<String, Object>> questions = parseQuestions(questionsJson);
        // 移除正确答案标记，只保留题目和选项文本
        return questions.stream().map(q -> {
            Map<String, Object> sanitized = new LinkedHashMap<>(q);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options = (List<Map<String, Object>>) sanitized.get("options");
            if (options != null) {
                sanitized.put("options", options.stream().map(opt -> {
                    Map<String, Object> cleanOpt = new LinkedHashMap<>();
                    cleanOpt.put("id", opt.get("id"));
                    cleanOpt.put("text", opt.get("text"));
                    return cleanOpt;
                }).toList());
            }
            sanitized.remove("explanation");
            return sanitized;
        }).toList();
    }

    private Map<String, Object> sanitizeQuiz(ChapterQuiz quiz) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", quiz.getId());
        result.put("chapterId", quiz.getChapterId());
        result.put("title", quiz.getTitle());
        result.put("passScore", quiz.getPassScore());
        result.put("timeLimit", quiz.getTimeLimit());
        result.put("questions", sanitizeQuestions(quiz.getQuestions()));
        return result;
    }

    private List<Map<String, Object>> parseQuestions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to parse questions JSON", e);
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String calculateRating(int score) {
        if (score >= 90) return "excellent";
        if (score >= 80) return "good";
        if (score >= 60) return "average";
        return "poor";
    }

    private String buildFeedback(int score, int wrongCount, int totalCount, boolean passed) {
        StringBuilder feedback = new StringBuilder();

        if (passed) {
            if (score >= 90) {
                feedback.append("表现优秀！掌握度很高。");
            } else if (score >= 80) {
                feedback.append("表现良好，知识点掌握扎实。");
            } else {
                feedback.append("已达到及格标准，继续加油。");
            }
        } else {
            feedback.append("未达到及格标准，建议复习后重考。");
        }

        feedback.append(" 本次得分 ").append(score).append("分");
        feedback.append("（").append(totalCount - wrongCount).append("/").append(totalCount).append(" 题正确）");

        if (wrongCount > 0) {
            feedback.append("，有 ").append(wrongCount).append(" 道错题已加入错题本。");
        }

        return feedback.toString();
    }

    private String buildComprehensiveFeedback(int score, int wrongCount, int totalCount, boolean passed) {
        String base = buildFeedback(score, wrongCount, totalCount, passed);
        return "综合考试：" + base;
    }

    private int countQuestionPool(String courseId) {
        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
        if (chapters.isEmpty()) return 0;
        List<String> chapterIds = chapters.stream().map(Chapter::getId).toList();
        List<ChapterQuiz> quizzes = quizRepo.findByChapterIdIn(chapterIds);
        int total = 0;
        for (ChapterQuiz quiz : quizzes) {
            total += parseQuestions(quiz.getQuestions()).size();
        }
        return total;
    }

    private List<Map<String, Object>> drawCrossChapterQuestions(String courseId, int drawCount) {
        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
        Map<String, String> chapterTitles = new HashMap<>();
        for (Chapter ch : chapters) {
            chapterTitles.put(ch.getId(), ch.getTitle());
        }

        List<String> chapterIds = chapters.stream().map(Chapter::getId).toList();
        List<ChapterQuiz> quizzes = quizRepo.findByChapterIdIn(chapterIds);

        Map<String, List<Map<String, Object>>> byChapter = new LinkedHashMap<>();
        for (ChapterQuiz quiz : quizzes) {
            String chapterId = quiz.getChapterId();
            List<Map<String, Object>> chapterQuestions = new ArrayList<>();
            for (Map<String, Object> q : parseQuestions(quiz.getQuestions())) {
                Map<String, Object> copy = new LinkedHashMap<>(q);
                String originalId = (String) copy.get("id");
                copy.put("sourceChapterId", chapterId);
                copy.put("sourceChapterTitle", chapterTitles.getOrDefault(chapterId, "未知章节"));
                copy.put("id", chapterId + "__" + originalId);
                chapterQuestions.add(copy);
            }
            if (!chapterQuestions.isEmpty()) {
                Collections.shuffle(chapterQuestions);
                byChapter.put(chapterId, chapterQuestions);
            }
        }

        List<Map<String, Object>> drawn = new ArrayList<>();
        List<String> chapterKeys = new ArrayList<>(byChapter.keySet());
        Collections.shuffle(chapterKeys);

        // 先按章节轮询，保证跨章节覆盖
        int safety = 0;
        while (drawn.size() < drawCount && safety++ < drawCount * chapterKeys.size() + 10) {
            boolean added = false;
            for (String chapterId : chapterKeys) {
                List<Map<String, Object>> pool = byChapter.get(chapterId);
                if (pool == null || pool.isEmpty()) continue;
                drawn.add(pool.remove(0));
                added = true;
                if (drawn.size() >= drawCount) break;
            }
            if (!added) break;
        }

        Collections.shuffle(drawn);
        return drawn.size() > drawCount ? drawn.subList(0, drawCount) : drawn;
    }

    private List<Map<String, Object>> sanitizeDrawnQuestions(List<Map<String, Object>> questions) {
        return questions.stream().map(q -> {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            sanitized.put("id", q.get("id"));
            sanitized.put("type", q.get("type"));
            sanitized.put("question", q.get("question"));
            sanitized.put("sourceChapterId", q.get("sourceChapterId"));
            sanitized.put("sourceChapterTitle", q.get("sourceChapterTitle"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options = (List<Map<String, Object>>) q.get("options");
            if (options != null) {
                sanitized.put("options", options.stream().map(opt -> {
                    Map<String, Object> cleanOpt = new LinkedHashMap<>();
                    cleanOpt.put("id", opt.get("id"));
                    cleanOpt.put("text", opt.get("text"));
                    return cleanOpt;
                }).toList());
            }
            return sanitized;
        }).toList();
    }

    private Map<String, Object> toAttemptInfo(QuizAttempt a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("quizId", a.getQuiz().getId());
        m.put("score", a.getScore());
        m.put("passed", a.getPassed());
        m.put("masteryLevel", a.getMasteryLevel());
        m.put("startedAt", a.getStartedAt());
        m.put("completedAt", a.getCompletedAt());
        return m;
    }
}

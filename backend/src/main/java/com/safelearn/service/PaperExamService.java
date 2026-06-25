package com.safelearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.ExamPaper;
import com.safelearn.entity.ExamPaperAttempt;
import com.safelearn.entity.User;
import com.safelearn.repository.ExamPaperAttemptRepository;
import com.safelearn.repository.ExamPaperRepository;
import com.safelearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaperExamService {

    private final ExamPaperRepository paperRepo;
    private final ExamPaperAttemptRepository attemptRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Map<String, Object> getPaperStatus(String userId, String paperId) {
        ExamPaper paper = requirePublishedPaper(paperId, userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", paper.getId());
        result.put("title", paper.getTitle());
        result.put("examType", paper.getExamType());
        result.put("timeLimit", paper.getTimeLimit());
        result.put("passScore", paper.getPassScore());
        result.put("totalScore", paper.getTotalScore());
        result.put("questionCount", parseQuestions(paper.getQuestionsSnapshot()).size());

        List<ExamPaperAttempt> attempts = attemptRepo.findByUserIdAndPaperIdOrderByStartedAtDesc(userId, paperId);
        boolean passed = attempts.stream().anyMatch(a -> Boolean.TRUE.equals(a.getPassed()));
        result.put("examPassed", passed);
        result.put("attemptCount", attempts.size());
        attempts.stream()
                .filter(a -> a.getCompletedAt() != null)
                .mapToInt(a -> a.getScore() != null ? a.getScore() : 0)
                .max()
                .ifPresent(best -> result.put("bestScore", best));
        return result;
    }

    @Transactional
    public Map<String, Object> startPaperExam(String userId, String paperId) {
        ExamPaper paper = requirePublishedPaper(paperId, userId);
        List<Map<String, Object>> questions = parseQuestions(paper.getQuestionsSnapshot());
        if (questions.isEmpty()) {
            throw new RuntimeException("试卷暂无题目");
        }

        boolean alreadyPassed = attemptRepo.findTopByUserIdAndPaperIdAndPassedTrueOrderByScoreDesc(userId, paperId).isPresent();
        if (alreadyPassed) {
            throw new RuntimeException("该考试已通过，无需重复参加");
        }

        ExamPaperAttempt attempt = new ExamPaperAttempt();
        attempt.setUserId(userId);
        attempt.setPaperId(paperId);
        attempt.setPassScore(paper.getPassScore() != null ? paper.getPassScore() : 60);
        attempt.setQuestions(toJson(questions));
        attemptRepo.save(attempt);

        Map<String, Object> quiz = new LinkedHashMap<>();
        quiz.put("id", paper.getId());
        quiz.put("title", paper.getTitle());
        quiz.put("passScore", paper.getPassScore());
        quiz.put("timeLimit", paper.getTimeLimit());
        quiz.put("questions", sanitizeQuestions(questions));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attemptId", attempt.getId());
        result.put("quiz", quiz);
        result.put("startedAt", attempt.getStartedAt());
        return result;
    }

    @Transactional
    public Map<String, Object> submitPaperExam(String userId, String attemptId, Map<String, Object> body) {
        ExamPaperAttempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("考试记录不存在"));
        if (!attempt.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该考试记录");
        }
        if (attempt.getCompletedAt() != null) {
            throw new RuntimeException("该考试已提交");
        }

        ExamPaper paper = paperRepo.findById(attempt.getPaperId())
                .orElseThrow(() -> new RuntimeException("试卷不存在"));
        List<Map<String, Object>> questions = parseQuestions(attempt.getQuestions());

        @SuppressWarnings("unchecked")
        Map<String, String> answers = body.get("answers") instanceof Map<?, ?> map
                ? (Map<String, String>) map
                : null;

        int earnedScore = 0;
        int maxScore = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        List<Map<String, Object>> wrongQuestions = new ArrayList<>();

        for (Map<String, Object> q : questions) {
            int questionScore = q.get("score") instanceof Number n ? n.intValue() : 1;
            maxScore += questionScore;
            String questionId = String.valueOf(q.get("id"));
            String userAnswer = answers != null ? answers.get(questionId) : null;
            boolean correct = checkAnswer(q, userAnswer);
            if (correct) earnedScore += questionScore;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", questionId);
            item.put("correct", correct);
            item.put("userAnswer", userAnswer);
            item.put("correctAnswer", getCorrectAnswer(q));
            item.put("explanation", q.get("explanation"));
            results.add(item);

            if (!correct) {
                Map<String, Object> wrong = new LinkedHashMap<>(item);
                wrong.put("question", q.get("question"));
                wrong.put("options", q.get("options"));
                wrong.put("type", q.get("type"));
                wrongQuestions.add(wrong);
            }
        }

        int passScore = attempt.getPassScore() != null ? attempt.getPassScore() : 60;
        int displayScore = maxScore > 0
                ? Math.min(100, (int) Math.round(earnedScore * 100.0 / maxScore))
                : 0;
        boolean passed = displayScore >= passScore;

        Map<String, Object> answersData = new LinkedHashMap<>();
        answersData.put("answers", answers);
        answersData.put("results", results);
        answersData.put("wrongQuestions", wrongQuestions);
        attempt.setAnswers(toJson(answersData));
        attempt.setScore(displayScore);
        attempt.setPassed(passed);
        attempt.setCompletedAt(LocalDateTime.now());
        attemptRepo.save(attempt);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("attemptId", attempt.getId());
        response.put("paperId", paper.getId());
        response.put("paperTitle", paper.getTitle());
        response.put("score", displayScore);
        response.put("passScore", passScore);
        response.put("passed", passed);
        response.put("rating", ratingForScore(displayScore));
        response.put("masteryLevel", displayScore);
        response.put("feedback", passed ? "恭喜通过考试！" : "未达到及格标准，请复习后再次尝试。");
        response.put("results", results);
        response.put("wrongQuestions", wrongQuestions);
        response.put("questions", sanitizeQuestions(questions));
        return response;
    }

    private ExamPaper requirePublishedPaper(String paperId, String userId) {
        ExamPaper paper = paperRepo.findById(paperId)
                .orElseThrow(() -> new RuntimeException("试卷不存在"));
        if (!"published".equalsIgnoreCase(paper.getStatus())) {
            throw new RuntimeException("考试未发布或已结束");
        }
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!matchesDepartment(paper.getDepartment(), user.getDepartment())) {
            throw new RuntimeException("您不在该考试的参加范围内");
        }
        return paper;
    }

    private boolean matchesDepartment(String targetDept, String userDept) {
        if (targetDept == null || targetDept.isBlank() || "全员".equals(targetDept.trim())) return true;
        if (userDept == null || userDept.isBlank()) return true;
        return targetDept.trim().equals(userDept.trim());
    }

    private List<Map<String, Object>> sanitizeQuestions(List<Map<String, Object>> questions) {
        return questions.stream().map(q -> {
            Map<String, Object> sanitized = new LinkedHashMap<>(q);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options = (List<Map<String, Object>>) sanitized.get("options");
            if (options != null) {
                sanitized.put("options", options.stream().map(opt -> {
                    Map<String, Object> clean = new LinkedHashMap<>();
                    clean.put("id", opt.get("id"));
                    clean.put("text", opt.get("text"));
                    return clean;
                }).toList());
            }
            sanitized.remove("explanation");
            return sanitized;
        }).toList();
    }

    private boolean checkAnswer(Map<String, Object> question, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) return false;
        String type = String.valueOf(question.getOrDefault("type", "single"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) question.get("options");
        if (options == null) return false;

        if ("multiple".equals(type)) {
            Set<String> correctIds = new HashSet<>();
            for (Map<String, Object> opt : options) {
                if (Boolean.TRUE.equals(opt.get("correct"))) {
                    correctIds.add(String.valueOf(opt.get("id")));
                }
            }
            Set<String> userSelected = new HashSet<>(Arrays.asList(userAnswer.split(",")));
            return correctIds.equals(userSelected);
        }
        for (Map<String, Object> opt : options) {
            if (Boolean.TRUE.equals(opt.get("correct"))) {
                return String.valueOf(opt.get("id")).equals(userAnswer);
            }
        }
        return false;
    }

    private String getCorrectAnswer(Map<String, Object> question) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) question.get("options");
        if (options == null) return "";
        List<String> correctIds = new ArrayList<>();
        for (Map<String, Object> opt : options) {
            if (Boolean.TRUE.equals(opt.get("correct"))) {
                correctIds.add(String.valueOf(opt.get("id")));
            }
        }
        return String.join(",", correctIds);
    }

    private String ratingForScore(int score) {
        if (score >= 90) return "excellent";
        if (score >= 80) return "good";
        if (score >= 60) return "average";
        return "poor";
    }

    private List<Map<String, Object>> parseQuestions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("数据序列化失败");
        }
    }
}

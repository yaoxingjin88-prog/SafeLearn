package com.safelearn.controller;

import com.safelearn.common.ApiResponse;
import com.safelearn.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /**
     * 获取章节测验
     */
    @GetMapping("/chapter/{chapterId}")
    public ApiResponse<Map<String, Object>> getQuizByChapter(@PathVariable String chapterId) {
        return ApiResponse.success(quizService.getQuizByChapterId(chapterId));
    }

    /**
     * 检查章节是否有测验
     */
    @GetMapping("/chapter/{chapterId}/exists")
    public ApiResponse<Map<String, Object>> checkQuizExists(
            Authentication auth,
            @PathVariable String chapterId) {
        String userId = auth != null ? auth.getPrincipal().toString() : null;
        return ApiResponse.success(quizService.getChapterQuizStatus(userId, chapterId));
    }

    /**
     * 开始测验
     */
    @PostMapping("/{quizId}/start")
    public ApiResponse<Map<String, Object>> startQuiz(Authentication auth, @PathVariable String quizId) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(quizService.startQuiz(userId, quizId));
    }

    /**
     * 提交测验答案
     */
    @PostMapping("/attempts/{attemptId}/submit")
    public ApiResponse<Map<String, Object>> submitQuiz(
            Authentication auth,
            @PathVariable String attemptId,
            @RequestBody Map<String, Object> body) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(quizService.submitQuiz(userId, attemptId, body));
    }

    /**
     * 获取测验历史
     */
    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> getHistory(
            Authentication auth,
            @RequestParam String chapterId) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(quizService.getQuizHistory(userId, chapterId));
    }

    /**
     * 获取错题本
     */
    @GetMapping("/wrong-questions")
    public ApiResponse<Map<String, Object>> getWrongQuestions(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(quizService.getWrongQuestions(userId));
    }

    /**
     * 获取测验详情（含答案，用于结果展示）
     */
    @GetMapping("/{quizId}/detail")
    public ApiResponse<Map<String, Object>> getQuizDetail(@PathVariable String quizId) {
        return ApiResponse.success(quizService.getQuizDetail(quizId));
    }

    /** 综合考试状态 */
    @GetMapping("/comprehensive/{courseId}/status")
    public ApiResponse<Map<String, Object>> getComprehensiveExamStatus(
            Authentication auth, @PathVariable String courseId) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(quizService.getComprehensiveExamStatus(userId, courseId));
    }

    /** 开始综合考试 */
    @PostMapping("/comprehensive/{courseId}/start")
    public ApiResponse<Map<String, Object>> startComprehensiveExam(
            Authentication auth, @PathVariable String courseId) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(quizService.startComprehensiveExam(userId, courseId));
    }

    /** 提交综合考试 */
    @PostMapping("/comprehensive/attempts/{attemptId}/submit")
    public ApiResponse<Map<String, Object>> submitComprehensiveExam(
            Authentication auth,
            @PathVariable String attemptId,
            @RequestBody Map<String, Object> body) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(quizService.submitComprehensiveExam(userId, attemptId, body));
    }

    /** 综合考试历史 */
    @GetMapping("/comprehensive/history")
    public ApiResponse<List<Map<String, Object>>> getComprehensiveExamHistory(
            Authentication auth,
            @RequestParam String courseId) {
        String userId = auth.getPrincipal().toString();
        return ApiResponse.success(quizService.getComprehensiveExamHistory(userId, courseId));
    }
}

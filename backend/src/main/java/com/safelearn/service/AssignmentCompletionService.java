package com.safelearn.service;

import com.safelearn.entity.*;
import com.safelearn.repository.*;
import com.safelearn.deduction.repository.SimulationSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 根据学员学习行为自动同步管理员指派任务状态。
 */
@Service
@RequiredArgsConstructor
public class AssignmentCompletionService {

    private static final Pattern COURSE_LINK = Pattern.compile("/courses/([^/]+)");
    private static final Pattern TRAINING_LINK = Pattern.compile("/training/([^/]+)");
    private static final Pattern SIMULATION_LINK = Pattern.compile("/simulation/([^/]+)");
    private static final Pattern CASE_LINK = Pattern.compile("/cases/([^/]+)");

    private final LearnerAssignmentRepository assignmentRepo;
    private final ChapterRepository chapterRepo;
    private final UserProgressRepository progressRepo;
    private final TrainingRecordRepository trainingRecordRepo;
    private final SimulationSessionRepository simulationSessionRepo;
    private final UserCaseProgressRepository caseProgressRepo;
    private final ComprehensiveExamAttemptRepository comprehensiveExamRepo;
    private final ExamPaperAttemptRepository examPaperAttemptRepo;
    private final QuizAttemptRepository quizAttemptRepo;

    /** 检查并更新该学员所有未完成任务。 */
    @Transactional
    public void syncForUser(String userId) {
        if (userId == null || userId.isBlank()) return;
        List<LearnerAssignment> open = assignmentRepo.findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, "completed");
        for (LearnerAssignment assignment : open) {
            if (isComplete(userId, assignment)) {
                assignment.setStatus("completed");
                assignmentRepo.save(assignment);
            } else if (isInProgress(userId, assignment) && !"in_progress".equals(assignment.getStatus())) {
                assignment.setStatus("in_progress");
                assignmentRepo.save(assignment);
            }
        }
    }

    private boolean isComplete(String userId, LearnerAssignment assignment) {
        String type = assignment.getType() != null ? assignment.getType() : "custom";
        Optional<String> targetId = extractTargetId(assignment.getLink());
        return switch (type) {
            case "course" -> targetId.map(id -> isCourseFullyCompleted(userId, id)).orElse(false);
            case "training" -> targetId.map(id -> hasCompletedTraining(userId, id)).orElse(false);
            case "exam" -> isExamAssignmentComplete(userId, assignment);
            case "case" -> targetId.map(id -> isCaseCompleted(userId, id)).orElse(false);
            case "simulation" -> targetId.map(id -> hasCompletedSimulation(userId, id)).orElse(false);
            default -> matchByLink(userId, assignment.getLink());
        };
    }

    private boolean isInProgress(String userId, LearnerAssignment assignment) {
        Optional<String> targetId = extractTargetId(assignment.getLink());
        if (targetId.isEmpty()) return false;
        String id = targetId.get();
        String link = assignment.getLink() != null ? assignment.getLink() : "";
        if (link.contains("/courses/")) {
            return progressRepo.findByUserIdWithDetails(userId).stream()
                    .anyMatch(p -> p.getCourse() != null && id.equals(p.getCourse().getId())
                            && (p.getProgress() != null && p.getProgress() > 0));
        }
        if (link.contains("/training/")) {
            return trainingRecordRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                    .anyMatch(r -> r.getScenario() != null && id.equals(r.getScenario().getId()));
        }
        if (link.contains("/simulation/")) {
            return simulationSessionRepo.findByUserIdOrderByStartedAtDesc(userId).stream()
                    .anyMatch(s -> id.equals(s.getScenarioId()));
        }
        return false;
    }

    private boolean matchByLink(String userId, String link) {
        if (link == null || link.isBlank()) return false;
        Optional<String> courseId = extract(COURSE_LINK, link);
        if (courseId.isPresent()) return isCourseFullyCompleted(userId, courseId.get());
        Optional<String> trainingId = extract(TRAINING_LINK, link);
        if (trainingId.isPresent()) return hasCompletedTraining(userId, trainingId.get());
        Optional<String> simId = extract(SIMULATION_LINK, link);
        if (simId.isPresent()) return hasCompletedSimulation(userId, simId.get());
        Optional<String> caseId = extract(CASE_LINK, link);
        if (caseId.isPresent()) return isCaseCompleted(userId, caseId.get());
        if (link.contains("/exams") || link.contains("/comprehensive-exam")) {
            return comprehensiveExamRepo.findByUserIdAndCompletedAtIsNotNull(userId).stream()
                    .anyMatch(a -> Boolean.TRUE.equals(a.getPassed()));
        }
        return false;
    }

    private boolean isExamAssignmentComplete(String userId, LearnerAssignment assignment) {
        String link = assignment.getLink() != null ? assignment.getLink() : "";
        if (link.contains("/papers/")) {
            Optional<String> paperId = extract(Pattern.compile("/papers/([^/]+)"), link);
            if (paperId.isPresent()) {
                return examPaperAttemptRepo.findByUserIdAndCompletedAtIsNotNull(userId).stream()
                        .anyMatch(a -> paperId.get().equals(a.getPaperId()) && Boolean.TRUE.equals(a.getPassed()));
            }
        }
        if (link.contains("/comprehensive-exam") || link.contains("/courses/")) {
            Optional<String> courseId = extract(COURSE_LINK, link);
            if (courseId.isPresent()) {
                return comprehensiveExamRepo.findByUserIdAndCourseIdOrderByStartedAtDesc(userId, courseId.get()).stream()
                        .anyMatch(a -> Boolean.TRUE.equals(a.getPassed()));
            }
        }
        return !quizAttemptRepo.findPassedAttemptsByUserId(userId).isEmpty();
    }

    private boolean isCourseFullyCompleted(String userId, String courseId) {
        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
        if (chapters.isEmpty()) return false;
        return chapters.stream().allMatch(ch ->
                progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId()));
    }

    private boolean hasCompletedTraining(String userId, String scenarioId) {
        return trainingRecordRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .anyMatch(r -> r.getEndTime() != null && r.getScenario() != null
                        && scenarioId.equals(r.getScenario().getId()));
    }

    private boolean hasCompletedSimulation(String userId, String scenarioId) {
        return simulationSessionRepo.findByUserIdOrderByStartedAtDesc(userId).stream()
                .anyMatch(s -> "completed".equals(s.getStatus()) && scenarioId.equals(s.getScenarioId()));
    }

    private boolean isCaseCompleted(String userId, String caseId) {
        return caseProgressRepo.findByUserIdAndCaseId(userId, caseId)
                .map(p -> Boolean.TRUE.equals(p.getCompleted()))
                .orElse(false);
    }

    private Optional<String> extractTargetId(String link) {
        if (link == null || link.isBlank()) return Optional.empty();
        return extract(COURSE_LINK, link)
                .or(() -> extract(TRAINING_LINK, link))
                .or(() -> extract(SIMULATION_LINK, link))
                .or(() -> extract(CASE_LINK, link));
    }

    private Optional<String> extract(Pattern pattern, String link) {
        Matcher m = pattern.matcher(link);
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }
}

package com.safelearn.service;

import com.safelearn.entity.UserProgress;
import com.safelearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CourseRepository courseRepo;
    private final UserProgressRepository progressRepo;
    private final TrainingRecordRepository recordRepo;

    public Map<String, Object> getStats(String userId) {
        long courseCount = courseRepo.findByStatusOrderByCreatedAtDesc("published").size();
        long completedCount = progressRepo.countByUserIdAndCompletedTrue(userId);
        long simulationCount = recordRepo.countByUserId(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("courseCount", courseCount);
        stats.put("completedCount", completedCount);
        stats.put("simulationCount", simulationCount);
        stats.put("avgScore", 82);
        return stats;
    }

    public List<Map<String, Object>> getRecentCourses(String userId) {
        List<UserProgress> progressList = progressRepo.findRecentByUserId(userId);

        Map<String, Integer> courseProgress = new LinkedHashMap<>();
        for (UserProgress p : progressList) {
            String courseId = p.getCourse().getId();
            courseProgress.merge(courseId, p.getProgress(), Math::max);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (var entry : courseProgress.entrySet()) {
            courseRepo.findById(entry.getKey()).ifPresent(course -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", course.getId());
                m.put("title", course.getTitle());
                m.put("category", course.getCategory());
                m.put("progress", entry.getValue());
                result.add(m);
            });
        }

        if (result.isEmpty()) {
            courseRepo.findByStatusOrderByCreatedAtDesc("published").stream().limit(5).forEach(course -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", course.getId());
                m.put("title", course.getTitle());
                m.put("category", course.getCategory());
                m.put("progress", 0);
                result.add(m);
            });
        }

        return result;
    }
}

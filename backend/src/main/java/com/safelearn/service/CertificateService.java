package com.safelearn.service;

import com.safelearn.entity.Chapter;
import com.safelearn.entity.Course;
import com.safelearn.entity.User;
import com.safelearn.entity.UserCertificate;
import com.safelearn.repository.ChapterRepository;
import com.safelearn.repository.CourseRepository;
import com.safelearn.repository.UserCertificateRepository;
import com.safelearn.repository.UserProgressRepository;
import com.safelearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final UserCertificateRepository certRepo;
    private final ChapterRepository chapterRepo;
    private final CourseRepository courseRepo;
    private final UserProgressRepository progressRepo;
    private final UserRepository userRepo;

    private static final int ADVANCED_LEVEL = 3;

    @Transactional
    public Optional<Map<String, Object>> tryIssueOnCourseComplete(String userId, String courseId) {
        if (certRepo.findByUserIdAndCourseId(userId, courseId).isPresent()) {
            return Optional.empty();
        }
        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderNumAsc(courseId);
        if (chapters.isEmpty()) return Optional.empty();

        boolean hasAdvanced = chapters.stream()
                .anyMatch(ch -> ch.getDifficultyLevel() != null && ch.getDifficultyLevel() >= ADVANCED_LEVEL);
        if (!hasAdvanced) return Optional.empty();

        boolean allDone = chapters.stream()
                .allMatch(ch -> progressRepo.existsByUserIdAndChapterIdAndCompletedTrue(userId, ch.getId()));
        if (!allDone) return Optional.empty();

        Course course = courseRepo.findById(courseId).orElseThrow(() -> new RuntimeException("课程不存在"));
        UserCertificate cert = new UserCertificate();
        cert.setUserId(userId);
        cert.setCourseId(courseId);
        cert.setTitle(course.getTitle() + " · 高级结业证书");
        cert.setCertLevel("advanced");
        cert.setCertificateNo(generateCertNo());
        cert = certRepo.save(cert);
        return Optional.of(toDetailMap(cert, userRepo.findById(userId).orElse(null), course));
    }

    public List<Map<String, Object>> listMine(String userId) {
        return certRepo.findByUserIdOrderByIssuedAtDesc(userId).stream()
                .map(c -> {
                    Course course = courseRepo.findById(c.getCourseId()).orElse(null);
                    User user = userRepo.findById(userId).orElse(null);
                    return toDetailMap(c, user, course);
                }).toList();
    }

    public Map<String, Object> getById(String userId, String certId) {
        UserCertificate cert = certRepo.findById(certId)
                .orElseThrow(() -> new RuntimeException("证书不存在"));
        if (!cert.getUserId().equals(userId)) throw new RuntimeException("无权查看该证书");
        Course course = courseRepo.findById(cert.getCourseId()).orElse(null);
        User user = userRepo.findById(userId).orElse(null);
        return toDetailMap(cert, user, course);
    }

    private String generateCertNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = String.format("%06d", new Random().nextInt(999999));
        return "SL-" + date + "-" + suffix;
    }

    private Map<String, Object> toDetailMap(UserCertificate c, User user, Course course) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("courseId", c.getCourseId());
        m.put("courseTitle", course != null ? course.getTitle() : "");
        m.put("title", c.getTitle());
        m.put("certificateNo", c.getCertificateNo());
        m.put("certLevel", c.getCertLevel());
        m.put("issuedAt", c.getIssuedAt() != null ? c.getIssuedAt().toString() : null);
        m.put("userName", resolveDisplayName(user));
        m.put("company", user != null ? user.getCompany() : "");
        return m;
    }

    private String resolveDisplayName(User user) {
        if (user == null) return "学员";
        return switch (user.getUsername()) {
            case "zhanggong" -> "张工";
            case "lisi" -> "李工";
            case "wangwu" -> "王工";
            default -> user.getUsername();
        };
    }
}

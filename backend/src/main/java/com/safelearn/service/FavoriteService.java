package com.safelearn.service;

import com.safelearn.entity.UserFavorite;
import com.safelearn.repository.AccidentCaseRepository;
import com.safelearn.repository.CourseRepository;
import com.safelearn.repository.UserFavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserFavoriteRepository favoriteRepo;
    private final CourseRepository courseRepo;
    private final AccidentCaseRepository caseRepo;

    @Transactional
    public Map<String, Object> toggle(String userId, String targetType, String targetId) {
        Optional<UserFavorite> existing = favoriteRepo.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
        if (existing.isPresent()) {
            favoriteRepo.delete(existing.get());
            return Map.of("favorited", false, "targetType", targetType, "targetId", targetId);
        }
        validateTarget(targetType, targetId);
        UserFavorite fav = new UserFavorite();
        fav.setUserId(userId);
        fav.setTargetType(targetType);
        fav.setTargetId(targetId);
        favoriteRepo.save(fav);
        return Map.of("favorited", true, "targetType", targetType, "targetId", targetId);
    }

    public boolean isFavorited(String userId, String targetType, String targetId) {
        return favoriteRepo.existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
    }

    public List<String> getFavoriteIds(String userId, String targetType) {
        return favoriteRepo.findByUserIdAndTargetTypeOrderByCreatedAtDesc(userId, targetType)
                .stream().map(UserFavorite::getTargetId).toList();
    }

    public List<Map<String, Object>> list(String userId, String targetType) {
        List<UserFavorite> favs = targetType != null && !targetType.isBlank()
                ? favoriteRepo.findByUserIdAndTargetTypeOrderByCreatedAtDesc(userId, targetType)
                : favoriteRepo.findByUserIdOrderByCreatedAtDesc(userId);
        return favs.stream().map(f -> enrichFavorite(f)).toList();
    }

    private Map<String, Object> enrichFavorite(UserFavorite f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.getId());
        m.put("targetType", f.getTargetType());
        m.put("targetId", f.getTargetId());
        m.put("createdAt", f.getCreatedAt() != null ? f.getCreatedAt().toString() : null);
        if ("course".equals(f.getTargetType())) {
            courseRepo.findById(f.getTargetId()).ifPresent(c -> {
                m.put("title", c.getTitle());
                m.put("category", c.getCategory());
                m.put("coverImage", c.getCoverImage());
            });
        } else if ("case".equals(f.getTargetType())) {
            caseRepo.findById(f.getTargetId()).ifPresent(c -> {
                m.put("title", c.getTitle());
                m.put("type", c.getType());
                m.put("severity", c.getSeverity());
                m.put("location", c.getLocation());
                m.put("date", c.getDate() != null ? c.getDate().toString() : null);
            });
        }
        return m;
    }

    private void validateTarget(String targetType, String targetId) {
        switch (targetType) {
            case "course" -> courseRepo.findById(targetId).orElseThrow(() -> new RuntimeException("课程不存在"));
            case "case" -> caseRepo.findById(targetId).orElseThrow(() -> new RuntimeException("案例不存在"));
            default -> throw new RuntimeException("不支持的收藏类型");
        }
    }
}

package com.safelearn.service;

import com.safelearn.entity.UserNote;
import com.safelearn.repository.UserNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final UserNoteRepository noteRepo;

    public List<Map<String, Object>> list(String userId, String targetType, String targetId) {
        return noteRepo.findByUserIdAndTargetTypeAndTargetIdOrderByUpdatedAtDesc(userId, targetType, targetId)
                .stream().map(this::toMap).toList();
    }

    public List<Map<String, Object>> listAll(String userId) {
        return noteRepo.findByUserIdOrderByUpdatedAtDesc(userId).stream().map(this::toMap).toList();
    }

    @Transactional
    public Map<String, Object> save(String userId, Map<String, Object> body) {
        String content = String.valueOf(body.getOrDefault("content", "")).trim();
        if (content.isBlank()) throw new RuntimeException("笔记内容不能为空");

        String targetType = String.valueOf(body.get("targetType"));
        String targetId = String.valueOf(body.get("targetId"));
        String id = body.get("id") != null ? String.valueOf(body.get("id")) : null;

        UserNote note;
        if (id != null && !id.isBlank()) {
            note = noteRepo.findById(id).orElseThrow(() -> new RuntimeException("笔记不存在"));
            if (!note.getUserId().equals(userId)) throw new RuntimeException("无权修改该笔记");
        } else {
            note = new UserNote();
            note.setUserId(userId);
            note.setTargetType(targetType);
            note.setTargetId(targetId);
            if (body.get("courseId") != null) note.setCourseId(String.valueOf(body.get("courseId")));
        }
        note.setContent(content);
        return toMap(noteRepo.save(note));
    }

    @Transactional
    public void delete(String userId, String noteId) {
        UserNote note = noteRepo.findById(noteId).orElseThrow(() -> new RuntimeException("笔记不存在"));
        if (!note.getUserId().equals(userId)) throw new RuntimeException("无权删除该笔记");
        noteRepo.delete(note);
    }

    private Map<String, Object> toMap(UserNote n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("targetType", n.getTargetType());
        m.put("targetId", n.getTargetId());
        m.put("courseId", n.getCourseId());
        m.put("content", n.getContent());
        m.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
        m.put("updatedAt", n.getUpdatedAt() != null ? n.getUpdatedAt().toString() : null);
        return m;
    }
}

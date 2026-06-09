package com.safelearn.repository;

import com.safelearn.entity.UserNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserNoteRepository extends JpaRepository<UserNote, String> {
    List<UserNote> findByUserIdAndTargetTypeAndTargetIdOrderByUpdatedAtDesc(
            String userId, String targetType, String targetId);

    List<UserNote> findByUserIdOrderByUpdatedAtDesc(String userId);
}

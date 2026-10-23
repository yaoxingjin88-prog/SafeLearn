package com.safelearn.repository;

import com.safelearn.entity.AiChatDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiChatDraftRepository extends JpaRepository<AiChatDraft, String> {

    Optional<AiChatDraft> findByUserId(String userId);
}

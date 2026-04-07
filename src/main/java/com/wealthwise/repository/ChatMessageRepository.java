package com.wealthwise.repository;

import com.wealthwise.entity.ChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findTop20ByUserIdOrderByCreatedAtAsc(UUID userId);

    void deleteByUserId(UUID userId);
}

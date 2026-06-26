package com.ragdoc.platform.conversation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 채팅 메시지 {@link ChatMessage} JPA 저장소. */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}

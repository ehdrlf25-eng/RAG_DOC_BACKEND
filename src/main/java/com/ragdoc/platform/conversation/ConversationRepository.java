package com.ragdoc.platform.conversation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 대화 {@link Conversation} JPA 저장소. */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
}

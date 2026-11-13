
package com.teamchat.realtime_chat.repository;

import com.teamchat.realtime_chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {

    List<Conversation> findByCreatedBy_Id(Long userId);
}

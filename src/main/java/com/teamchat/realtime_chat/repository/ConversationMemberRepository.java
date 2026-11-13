
package com.teamchat.realtime_chat.repository;

import com.teamchat.realtime_chat.entity.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationMemberRepository
        extends JpaRepository<ConversationMember, Long> {

    List<ConversationMember> findByConversation_Id(Long conversationId);

    List<ConversationMember> findByUser_Id(Long userId);

    Optional<ConversationMember> findByConversation_IdAndUser_Id(
            Long conversationId,
            Long userId
    );

    boolean existsByConversation_IdAndUser_Id(
            Long conversationId,
            Long userId
    );

    void deleteByConversation_IdAndUser_Id(
            Long conversationId,
            Long userId
    );
}

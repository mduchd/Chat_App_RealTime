
package com.teamchat.realtime_chat.service;

import com.teamchat.realtime_chat.entity.ConversationMember;
import com.teamchat.realtime_chat.repository.ConversationMemberRepository;
import com.teamchat.realtime_chat.repository.MessageRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnreadMessageService {

    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;

    public UnreadMessageService(
            ConversationMemberRepository memberRepository,
            MessageRepository messageRepository) {
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
    }

    // Đếm số tin nhắn chưa đọc
    @Transactional(readOnly = true)
    public long countUnreadMessages(
            Long conversationId,
            Long userId) {

        ConversationMember member = memberRepository
                .findByConversation_IdAndUser_Id(
                        conversationId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bạn không thuộc cuộc trò chuyện này"));

        if (member.getLastReadMessage() == null) {
            return messageRepository
                    .countByConversation_Id(conversationId);
        }

        return messageRepository
                .countByConversation_IdAndIdGreaterThan(
                        conversationId,
                        member.getLastReadMessage().getId()
                );
    }
}

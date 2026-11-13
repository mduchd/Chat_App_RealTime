
package com.teamchat.realtime_chat.service;

import com.teamchat.realtime_chat.entity.ConversationMember;
import com.teamchat.realtime_chat.entity.Message;
import com.teamchat.realtime_chat.repository.ConversationMemberRepository;
import com.teamchat.realtime_chat.repository.MessageRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadStatusService {

    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;

    public ReadStatusService(
            ConversationMemberRepository memberRepository,
            MessageRepository messageRepository) {
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
    }

    // Đánh dấu đã đọc đến một tin nhắn
    @Transactional
    public void markAsRead(
            Long conversationId,
            Long userId,
            Long messageId) {

        ConversationMember member = memberRepository
                .findByConversation_IdAndUser_Id(
                        conversationId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bạn không thuộc cuộc trò chuyện này"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tin nhắn"));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new IllegalArgumentException(
                    "Tin nhắn không thuộc cuộc trò chuyện này");
        }

        Message currentLastRead = member.getLastReadMessage();

        // Không cập nhật lùi trạng thái đã đọc
        if (currentLastRead != null
                && currentLastRead.getId() >= messageId) {
            return;
        }

        member.setLastReadMessage(message);
        memberRepository.save(member);
    }
}

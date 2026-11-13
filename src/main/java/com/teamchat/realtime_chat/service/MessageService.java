
package com.teamchat.realtime_chat.service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.teamchat.realtime_chat.entity.Conversation;
import com.teamchat.realtime_chat.entity.Message;
import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.repository.ConversationMemberRepository;
import com.teamchat.realtime_chat.repository.ConversationRepository;
import com.teamchat.realtime_chat.repository.MessageRepository;
import com.teamchat.realtime_chat.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;

    public MessageService(
            MessageRepository messageRepository,
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository) {

        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
    }

    // Lưu tin nhắn mới
    @Transactional
    public Message sendMessage(
            Long conversationId,
            Long senderId,
            String content,
            String clientMessageId) {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }

        // Kiểm tra người gửi có thuộc cuộc trò chuyện không
        boolean isMember = memberRepository
                .existsByConversation_IdAndUser_Id(conversationId, senderId);

        if (!isMember) {
            throw new IllegalArgumentException(
                    "Người dùng không thuộc cuộc trò chuyện này");
        }

        // Kiểm tra tin nhắn đã được gửi trước đó chưa
        if (clientMessageId != null && !clientMessageId.isBlank()) {
            var existing = messageRepository
                    .findBySender_IdAndClientMessageId(
                            senderId, clientMessageId);

            if (existing.isPresent()) {
                return existing.get();
            }
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy người gửi"));

        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy cuộc trò chuyện"));

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        message.setClientMessageId(clientMessageId);

        return messageRepository.save(message);
    }

    // Lấy lịch sử tin nhắn của cuộc trò chuyện

    // Lấy lịch sử tin nhắn mới nhất, tối đa 30 tin
    @Transactional(readOnly = true)
    public Page<Message> getMessages(Long conversationId, Long userId) {

        checkMembership(conversationId, userId);

        PageRequest pageable = PageRequest.of(
                0,
                30,
                Sort.by(Sort.Direction.DESC, "id")
        );

        return messageRepository
                .findByConversation_IdOrderByIdDesc(
                        conversationId, pageable);
    }

    // Tải thêm tin nhắn cũ hơn
    @Transactional(readOnly = true)
    public Page<Message> getOlderMessages(
            Long conversationId,
            Long userId,
            Long beforeId) {

        checkMembership(conversationId, userId);

        PageRequest pageable = PageRequest.of(
                0,
                30,
                Sort.by(Sort.Direction.DESC, "id")
        );

        return messageRepository
                .findByConversation_IdAndIdLessThanOrderByIdDesc(
                        conversationId,
                        beforeId,
                        pageable
                );
    }

    // Kiểm tra quyền truy cập hội thoại
    private void checkMembership(Long conversationId, Long userId) {

        boolean isMember = memberRepository
                .existsByConversation_IdAndUser_Id(
                        conversationId, userId);

        if (!isMember) {
            throw new IllegalArgumentException(
                    "Bạn không có quyền truy cập cuộc trò chuyện này");
        }
    }
}

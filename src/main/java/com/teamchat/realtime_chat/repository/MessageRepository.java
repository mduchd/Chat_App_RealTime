
package com.teamchat.realtime_chat.repository;

import com.teamchat.realtime_chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // Lấy 30 tin nhắn mới nhất
    Page<Message> findByConversation_IdOrderByIdDesc(
            Long conversationId,
            Pageable pageable
    );

    // Lấy tin nhắn cũ hơn ID được cung cấp
    Page<Message> findByConversation_IdAndIdLessThanOrderByIdDesc(
            Long conversationId,
            Long beforeId,
            Pageable pageable
    );

    // Kiểm tra tin nhắn đã được gửi trước đó
    Optional<Message> findBySender_IdAndClientMessageId(
            Long senderId,
            String clientMessageId
    );
    
    // Đếm tin nhắn chưa đọc trong một cuộc trò chuyện
    long countByConversation_IdAndIdGreaterThan(
            Long conversationId,
            Long lastReadMessageId
    );

    // Đếm toàn bộ tin nhắn trong cuộc trò chuyện
    long countByConversation_Id(Long conversationId);

}


package com.teamchat.realtime_chat.dto;

import com.teamchat.realtime_chat.entity.Message;
import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime createdAt
) {
    public static MessageResponse fromEntity(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getSender().getDisplayName(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}

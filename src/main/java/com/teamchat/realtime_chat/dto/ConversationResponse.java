package com.teamchat.realtime_chat.dto;




import com.teamchat.realtime_chat.entity.Conversation;
import com.teamchat.realtime_chat.enums.ConversationType;
import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        ConversationType type,
        String name,
        Long createdById,
        LocalDateTime createdAt
) {
    public static ConversationResponse fromEntity(
            Conversation conversation) {

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getName(),
                conversation.getCreatedBy().getId(),
                conversation.getCreatedAt()
        );
    }
}

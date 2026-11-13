
package com.teamchat.realtime_chat.dto;

public record UnreadCountResponse(
        Long conversationId,
        Long userId,
        long unreadCount
) {
}

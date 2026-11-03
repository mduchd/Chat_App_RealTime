
package com.teamchat.realtime_chat.dto;

public record PrivateMessageRequest(
        String recipient,
        String content
) {}

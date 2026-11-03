package com.teamchat.realtime_chat.dto;
import java.time.Instant;

public record PrivateMessageEvent(
        String id,
        String sender,
        String recipient,
        String content,
        Instant sentAt
) {}

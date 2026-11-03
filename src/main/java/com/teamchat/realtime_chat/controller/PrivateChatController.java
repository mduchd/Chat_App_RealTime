
package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.dto.PrivateMessageRequest;
import com.teamchat.realtime_chat.dto.PrivateMessageEvent;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class PrivateChatController {

    private final SimpMessagingTemplate messagingTemplate;

    public PrivateChatController(
            SimpMessagingTemplate messagingTemplate) {

        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.private")
    public void sendPrivateMessage(
            PrivateMessageRequest request,
            Principal principal) {

        // 1. Check authentication
        if (principal == null) {
            throw new IllegalStateException(
                    "Authentication required"
            );
        }

        // 2. Validate message
        if (request == null
                || request.recipient() == null
                || request.recipient().isBlank()
                || request.content() == null
                || request.content().isBlank()) {

            throw new IllegalArgumentException(
                    "Invalid message"
            );
        }

        // 3. Get sender and recipient
        String sender = principal.getName();
        String recipient = request.recipient().trim();

        // 4. Create message event
        PrivateMessageEvent event = new PrivateMessageEvent(
                UUID.randomUUID().toString(),
                sender,
                recipient,
                request.content().trim(),
                Instant.now()
        );

        // 5. Send to recipient
        messagingTemplate.convertAndSendToUser(
                recipient,
                "/queue/messages",
                event
        );

        // 6. Send a copy to sender
        if (!sender.equals(recipient)) {
            messagingTemplate.convertAndSendToUser(
                    sender,
                    "/queue/messages",
                    event
            );
        }
    }
}

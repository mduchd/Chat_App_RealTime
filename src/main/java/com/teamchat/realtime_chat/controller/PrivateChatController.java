
package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.dto.PrivateMessageRequest;
import com.teamchat.realtime_chat.dto.PrivateMessageEvent;
import com.teamchat.realtime_chat.repository.UserRepository;
import com.teamchat.realtime_chat.entity.User;
import java.util.Optional;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class PrivateChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository users;

    public PrivateChatController(
            SimpMessagingTemplate messagingTemplate, UserRepository users) {

        this.messagingTemplate = messagingTemplate;
        this.users = users;
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

        // Resolve the authenticated sender here so notifications do not depend on a second HTTP request.
        Optional<User> profile = users.findByGoogleSub(sender);
        if (profile.isEmpty()) {
            try { profile = users.findById(Long.parseLong(sender)); }
            catch (NumberFormatException ignored) { }
        }

        // 4. Create message event
        PrivateMessageEvent event = new PrivateMessageEvent(
                UUID.randomUUID().toString(),
                sender,
                recipient,
                request.content().trim(),
                Instant.now(),
                profile.map(User::getDisplayName).orElse(null),
                profile.map(User::getAvatarUrl).orElse(null)
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

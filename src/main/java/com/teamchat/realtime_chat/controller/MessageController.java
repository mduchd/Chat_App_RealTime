
package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.dto.MessageResponse;
import com.teamchat.realtime_chat.service.MessageService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import com.teamchat.realtime_chat.security.AccountIdentity;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
public class MessageController {

    private final AccountIdentity identity;
    private final MessageService messageService;

    public MessageController(MessageService messageService, AccountIdentity identity) {
        this.identity=identity;
        this.messageService = messageService;
    }

    // Lấy lịch sử tin nhắn, tối đa 30 tin mỗi lần
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable Long conversationId,
            Principal principal,
            @RequestParam(required = false) Long before) {
        Long userId=identity.id(principal);

        Page<MessageResponse> messages;

        if (before == null) {
            messages = messageService
                    .getMessages(conversationId, userId)
                    .map(MessageResponse::fromEntity);
        } else {
            if (before <= 0) {
                return ResponseEntity.badRequest().build();
            }

            messages = messageService
                    .getOlderMessages(conversationId, userId, before)
                    .map(MessageResponse::fromEntity);
        }

        return ResponseEntity.ok(messages);
    }
}

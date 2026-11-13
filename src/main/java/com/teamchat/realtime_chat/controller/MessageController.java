
package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.dto.MessageResponse;
import com.teamchat.realtime_chat.service.MessageService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // Lấy lịch sử tin nhắn, tối đa 30 tin mỗi lần
    // userId chỉ phục vụ kiểm thử nội bộ
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam Long userId,
            @RequestParam(required = false) Long before) {

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


package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.service.ReadStatusService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
public class ReadStatusController {

    private final ReadStatusService readStatusService;

    public ReadStatusController(ReadStatusService readStatusService) {
        this.readStatusService = readStatusService;
    }

    // Đánh dấu tin nhắn đã đọc
    // userId tạm dùng cho kiểm thử nội bộ
    @PutMapping("/{conversationId}/read/{messageId}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            @RequestParam Long userId) {

        readStatusService.markAsRead(
                conversationId,
                userId,
                messageId
        );

        return ResponseEntity.ok().build();
    }
}

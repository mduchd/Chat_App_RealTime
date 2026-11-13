
package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.dto.UnreadCountResponse;
import com.teamchat.realtime_chat.service.UnreadMessageService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
public class UnreadMessageController {

    private final UnreadMessageService unreadMessageService;

    public UnreadMessageController(
            UnreadMessageService unreadMessageService) {
        this.unreadMessageService = unreadMessageService;
    }

    // Lấy số tin nhắn chưa đọc
    // userId tạm dùng cho kiểm thử nội bộ
    @GetMapping("/{conversationId}/unread")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @PathVariable Long conversationId,
            @RequestParam Long userId) {

        long count = unreadMessageService.countUnreadMessages(
                conversationId, userId);

        return ResponseEntity.ok(
                new UnreadCountResponse(
                        conversationId,
                        userId,
                        count
                )
        );
    }
}

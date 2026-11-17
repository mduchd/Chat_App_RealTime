
package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.dto.UnreadCountResponse;
import com.teamchat.realtime_chat.service.UnreadMessageService;

import org.springframework.http.ResponseEntity;
import com.teamchat.realtime_chat.security.AccountIdentity;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
public class UnreadMessageController {

    private final AccountIdentity identity;
    private final UnreadMessageService unreadMessageService;

    public UnreadMessageController(
            UnreadMessageService unreadMessageService, AccountIdentity identity) {
        this.identity=identity;
        this.unreadMessageService = unreadMessageService;
    }

    // Lấy số tin nhắn chưa đọc
    @GetMapping("/{conversationId}/unread")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @PathVariable Long conversationId,
            Principal principal) {
        Long userId=identity.id(principal);

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


package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.service.ReadStatusService;

import org.springframework.http.ResponseEntity;
import com.teamchat.realtime_chat.security.AccountIdentity;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
public class ReadStatusController {

    private final AccountIdentity identity;
    private final ReadStatusService readStatusService;

    public ReadStatusController(ReadStatusService readStatusService, AccountIdentity identity) {
        this.identity=identity;
        this.readStatusService = readStatusService;
    }

    // Đánh dấu tin nhắn đã đọc
    @PutMapping("/{conversationId}/read/{messageId}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            Principal principal) {
        Long userId=identity.id(principal);

        readStatusService.markAsRead(
                conversationId,
                userId,
                messageId
        );

        return ResponseEntity.ok().build();
    }
}

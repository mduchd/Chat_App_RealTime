
package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.dto.ConversationResponse;
import com.teamchat.realtime_chat.service.ConversationService;

import org.springframework.http.ResponseEntity;
import com.teamchat.realtime_chat.security.AccountIdentity;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final AccountIdentity identity;
    private final ConversationService conversationService;

    public ConversationController(
            ConversationService conversationService, AccountIdentity identity) {
        this.identity=identity;
        this.conversationService = conversationService;
    }

    // Lấy danh sách cuộc trò chuyện
    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getConversations(
            Principal principal) {
        Long userId=identity.id(principal);

        List<ConversationResponse> result = conversationService
                .getUserConversations(userId)
                .stream()
                .map(ConversationResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(result);
    }
}

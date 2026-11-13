
package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.dto.ConversationResponse;
import com.teamchat.realtime_chat.service.ConversationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(
            ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    // Lấy danh sách cuộc trò chuyện
    // Tạm thời dùng userId để kiểm thử nội bộ
    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getConversations(
            @RequestParam Long userId) {

        List<ConversationResponse> result = conversationService
                .getUserConversations(userId)
                .stream()
                .map(ConversationResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(result);
    }
}

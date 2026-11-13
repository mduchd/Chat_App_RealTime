
package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.dto.AddMemberRequest;
import com.teamchat.realtime_chat.dto.ConversationResponse;
import com.teamchat.realtime_chat.dto.CreateGroupRequest;
import com.teamchat.realtime_chat.service.ConversationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final ConversationService conversationService;

    public GroupController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    // Tạo nhóm chat
    // creatorId tạm dùng để kiểm thử nội bộ
    @PostMapping
    public ResponseEntity<ConversationResponse> createGroup(
            @RequestParam Long creatorId,
            @Valid @RequestBody CreateGroupRequest request) {

        var conversation = conversationService.createGroup(
                creatorId,
                request.groupName(),
                request.memberIds()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ConversationResponse.fromEntity(conversation));
    }

    // Thêm thành viên vào nhóm
    // requesterId tạm dùng để kiểm thử nội bộ
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable Long id,
            @RequestParam Long requesterId,
            @Valid @RequestBody AddMemberRequest request) {

        conversationService.addMember(
                id,
                requesterId,
                request.userId()
        );

        return ResponseEntity.ok().build();
    }
}

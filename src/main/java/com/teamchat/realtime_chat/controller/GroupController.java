
package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.dto.AddMemberRequest;
import com.teamchat.realtime_chat.dto.ConversationResponse;
import com.teamchat.realtime_chat.dto.CreateGroupRequest;
import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.service.ConversationService;

import jakarta.validation.Valid;
import com.teamchat.realtime_chat.security.AccountIdentity;
import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final AccountIdentity identity;
    private final ConversationService conversationService;

    public GroupController(ConversationService conversationService, AccountIdentity identity) {
        this.identity=identity;
        this.conversationService = conversationService;
    }

    // Tạo nhóm chat
    @PostMapping
    public ResponseEntity<ConversationResponse> createGroup(
            Principal principal,
            @Valid @RequestBody CreateGroupRequest request) {

        var conversation = conversationService.createGroup(
                identity.id(principal),
                request.groupName(),
                request.memberIds().stream()
                        .map(member -> identity.lookup(member).orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy thành viên.")))
                        .map(User::getId)
                        .distinct()
                        .toList()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ConversationResponse.fromEntity(conversation));
    }

    // Thêm thành viên vào nhóm
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable Long id,
            Principal principal,
            @Valid @RequestBody AddMemberRequest request) {

        conversationService.addMember(
                id,
                identity.id(principal),
                request.userId()
        );

        return ResponseEntity.ok().build();
    }
}

package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.dto.PrivateMessageRequest;
import com.teamchat.realtime_chat.service.ChatStorageService;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class PrivateChatController {
    private final ChatStorageService storage;
    public PrivateChatController(ChatStorageService storage) { this.storage=storage; }
    @MessageMapping("/chat.private")
    public void sendPrivateMessage(PrivateMessageRequest request,Principal principal) {
        if(principal==null) throw new IllegalStateException("Authentication required");
        if(request==null || request.recipient()==null || request.recipient().isBlank()
            || request.content()==null || request.content().isBlank()) throw new IllegalArgumentException("Invalid message");
        storage.sendPrivate(principal,request.recipient().trim(),request.content().trim());
    }
}

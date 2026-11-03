package com.teamchat.realtime_chat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;

@Controller
public class DemoChatController {
    public record DemoMessage(String content, String sender) {
    }

    @MessageMapping("/demo.send")
    @SendTo("/topic/demo")
    public DemoMessage sendMessage(DemoMessage message) {
        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be null or blank");
        }

        return new DemoMessage(message.content().trim(), message.sender());
    }
}

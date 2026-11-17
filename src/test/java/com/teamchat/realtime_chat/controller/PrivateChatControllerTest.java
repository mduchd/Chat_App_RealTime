package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.dto.PrivateMessageEvent;
import com.teamchat.realtime_chat.dto.PrivateMessageRequest;
import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.mockito.ArgumentCaptor;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrivateChatControllerTest {
    @Test
    void googleSenderProfileIsIncludedWithoutTrustingClientNames() {
        var users = mock(UserRepository.class);
        var messaging = mock(SimpMessagingTemplate.class);
        User user = new User();
        user.setDisplayName("Lan");
        user.setAvatarUrl("https://example.com/avatar.jpg");
        when(users.findByGoogleSub("google-sub")).thenReturn(Optional.of(user));
        new PrivateChatController(messaging, users).sendPrivateMessage(
                new PrivateMessageRequest("recipient", "hello"), () -> "google-sub");
        var event = ArgumentCaptor.forClass(PrivateMessageEvent.class);
        verify(messaging).convertAndSendToUser(eq("recipient"), eq("/queue/messages"), event.capture());
        assertEquals("Lan", event.getValue().senderName());
        assertEquals(user.getAvatarUrl(), event.getValue().senderAvatar());
        verify(messaging).convertAndSendToUser("google-sub", "/queue/messages", event.getValue());
    }

    @Test
    void emailSenderUsesNumericAccountProfile() {
        var users = mock(UserRepository.class);
        var messaging = mock(SimpMessagingTemplate.class);
        User user = new User(); user.setDisplayName("Binh");
        when(users.findByGoogleSub("42")).thenReturn(Optional.empty());
        when(users.findById(42L)).thenReturn(Optional.of(user));
        new PrivateChatController(messaging, users).sendPrivateMessage(
                new PrivateMessageRequest("recipient", "hello"), () -> "42");
        var event = ArgumentCaptor.forClass(PrivateMessageEvent.class);
        verify(messaging).convertAndSendToUser(eq("recipient"), eq("/queue/messages"), event.capture());
        assertEquals("Binh", event.getValue().senderName());
    }
}

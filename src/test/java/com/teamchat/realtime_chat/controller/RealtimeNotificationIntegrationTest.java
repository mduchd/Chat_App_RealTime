package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealtimeNotificationIntegrationTest {
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    @Test
    void recipientReceivesPrivateMessageOverAuthenticatedWebSocket() throws Exception {
        User sender = createUser();
        User recipient = createUser();
        try {
            String senderCookie = login(sender);
            String recipientCookie = login(recipient);
            StandardWebSocketClient client = new StandardWebSocketClient();
            LinkedBlockingQueue<String> received = new LinkedBlockingQueue<>();
            WebSocketHttpHeaders senderHeaders = new WebSocketHttpHeaders();
            senderHeaders.add(HttpHeaders.COOKIE, senderCookie);
            WebSocketHttpHeaders recipientHeaders = new WebSocketHttpHeaders();
            recipientHeaders.add(HttpHeaders.COOKIE, recipientCookie);
            URI uri = URI.create("ws://localhost:" + port + "/ws");
            WebSocketSession senderSocket = client.execute(new TextWebSocketHandler(), senderHeaders, uri).get(5, TimeUnit.SECONDS);
            WebSocketSession recipientSocket = client.execute(new TextWebSocketHandler() {
                @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    received.add(message.getPayload());
                }
            }, recipientHeaders, uri).get(5, TimeUnit.SECONDS);
            try {
                senderSocket.sendMessage(new TextMessage("CONNECT\naccept-version:1.2\nhost:localhost\n\n\0"));
                recipientSocket.sendMessage(new TextMessage("CONNECT\naccept-version:1.2\nhost:localhost\n\n\0"));
                assertTrue(received.poll(5, TimeUnit.SECONDS).startsWith("CONNECTED"));
                recipientSocket.sendMessage(new TextMessage("SUBSCRIBE\nid:messages\ndestination:/user/queue/messages\n\n\0"));
                senderSocket.sendMessage(new TextMessage("SEND\ndestination:/app/chat.private\ncontent-type:application/json\n\n{\"recipient\":\""
                        + recipient.getId() + "\",\"content\":\"notification-check\"}\0"));
                String event = received.poll(5, TimeUnit.SECONDS);
                assertNotNull(event);
                assertTrue(event.contains("notification-check"), event);
                assertTrue(event.contains(sender.getId().toString()), event);
            } finally {
                senderSocket.close();
                recipientSocket.close();
            }
        } finally {
            users.delete(sender);
            users.delete(recipient);
        }
    }

    private User createUser() {
        User user = new User();
        user.setEmail("ws-" + UUID.randomUUID() + "@test.com");
        user.setDisplayName("WS Test");
        user.setPasswordHash(passwords.encode("test-password"));
        return users.saveAndFlush(user);
    }

    private String login(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = new RestTemplate().postForEntity("http://localhost:" + port + "/api/auth/login",
                new HttpEntity<>("{\"email\":\"" + user.getEmail() + "\",\"password\":\"test-password\"}", headers), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(cookie);
        return cookie.split(";", 2)[0];
    }
}

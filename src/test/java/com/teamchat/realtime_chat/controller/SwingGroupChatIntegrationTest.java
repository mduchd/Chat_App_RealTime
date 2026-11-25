package com.teamchat.realtime_chat.controller;

import com.google.gson.JsonParser;
import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.repository.UserRepository;
import com.teamchat.swing.api.ApiClient;
import com.teamchat.swing.api.StompClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:swing-group;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "spring.session.jdbc.initialize-schema=always", "spring.session.jdbc.platform=h2",
        "chat.session.cookie-secure=false", "chat.session.cookie-same-site=Lax"
})
@Sql(scripts = "/db/migration/V6__server_chat_storage.sql", statements = {
        "ALTER TABLE users ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP",
        "ALTER TABLE conversations ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP",
        "ALTER TABLE conversation_members ALTER COLUMN joined_at SET DEFAULT CURRENT_TIMESTAMP",
        "ALTER TABLE messages ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class SwingGroupChatIntegrationTest {
    private static final String PASSWORD = "test-password";
    private static final String DESTINATION = "/user/queue/messages";
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired SimpUserRegistry sessions;

    @Test
    void swingCreatesAGroupWithGoogleAccountsAndSendsToItsOriginalMembers() throws Exception {
        User sender = account("sender", "100000000000000000001");
        User recipient = account("recipient", "100000000000000000002");
        String previousBackend = ApiClient.getBaseUrl();
        var socket = new StompClient();
        try (var http = HttpClient.newHttpClient()) {
            ApiClient.setBaseUrl("http://localhost:" + port);
            assertTrue(ApiClient.login(sender.getEmail(), PASSWORD).success);
            var group = ApiClient.createGroup("Swing fixture", List.of(recipient.getGoogleSub()));
            assertEquals(201, group.httpCode, group.rawBody);
            String groupId = "db-" + group.json.get("id").getAsString();

            var login = http.send(HttpRequest.newBuilder(uri("/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("""
                            {"email":"%s","password":"%s"}
                            """.formatted(recipient.getEmail(), PASSWORD))).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, login.statusCode(), login.body());
            String recipientCookie = login.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
            var received = new CompletableFuture<String>();
            socket.setConnectionListener(connected -> {
                if (connected) {
                    socket.subscribe(DESTINATION, (destination, body) -> {
                        var event = JsonParser.parseString(body).getAsJsonObject();
                        if (event.getAsJsonObject("message").has("content")) received.complete(body);
                    });
                }
            });
            socket.connect("ws://localhost:" + port + "/ws", Map.of("Cookie", recipientCookie));
            awaitSubscription(recipient);

            String content = "Chào nhóm 👍";
            ApiClient.syncState(Map.of("conversations", List.of(Map.of("id", groupId, "type", "GROUP",
                    "messages", List.of(Map.of("id", "swing-fixture-message", "senderId", sender.getGoogleSub(),
                            "content", content))))));

            var event = JsonParser.parseString(received.get(5, TimeUnit.SECONDS)).getAsJsonObject();
            assertEquals(groupId, event.get("conversationId").getAsString());
            assertEquals(content, event.getAsJsonObject("message").get("content").getAsString());
            var snapshot = http.send(HttpRequest.newBuilder(uri("/api/chat/snapshot"))
                    .header("Cookie", recipientCookie).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, snapshot.statusCode());
            var conversations = JsonParser.parseString(snapshot.body()).getAsJsonObject().getAsJsonArray("conversations");
            assertEquals(1, conversations.size());
            var saved = conversations.get(0).getAsJsonObject();
            assertEquals(groupId, saved.get("id").getAsString());
            assertEquals(2, saved.getAsJsonArray("members").size());
            assertEquals(content, saved.getAsJsonArray("messages").get(0).getAsJsonObject().get("content").getAsString());
        } finally {
            socket.disconnect();
            ApiClient.setBaseUrl(previousBackend);
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private User account(String name, String googleSub) {
        var user = new User();
        user.setEmail(name + "@group-test.example");
        user.setDisplayName(name);
        user.setGoogleSub(googleSub);
        user.setPasswordHash(passwords.encode(PASSWORD));
        return users.saveAndFlush(user);
    }

    private void awaitSubscription(User recipient) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            var session = sessions.getUser(recipient.getId().toString());
            if (session != null && session.getSessions().stream()
                    .flatMap(connection -> connection.getSubscriptions().stream())
                    .anyMatch(subscription -> DESTINATION.equals(subscription.getDestination()))) return;
            Thread.sleep(20);
        }
        throw new AssertionError("The Swing recipient did not subscribe");
    }
}

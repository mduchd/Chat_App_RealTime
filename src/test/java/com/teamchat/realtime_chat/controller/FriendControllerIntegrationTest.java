package com.teamchat.realtime_chat.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FriendControllerIntegrationTest {
    @Autowired FriendController friends;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    private OAuth2User user(String sub) {
        jdbc.update("INSERT INTO users (google_sub, email, display_name) VALUES (?, ?, ?)",
                sub, sub + "@example.com", sub);
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getName()).thenReturn(sub);
        return principal;
    }

    @Test
    void friendEndpointsRejectUnauthenticatedAndUntrustedOrigins() throws Exception {
        mvc.perform(get("/api/friends")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/api/friends/requests").header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestsRequireRecipientAndBothUsersSeeTheFriendship() {
        var alice = user("test-alice");
        var bob = user("test-bob");
        var eve = user("test-eve");
        assertEquals(1, friends.search(alice, "test-bob@example.com").size());
        friends.send(alice, new FriendController.SendRequest("test-bob"));
        var pending = friends.list(bob).requests().get(0);
        assertEquals("test-alice", pending.fromId());
        assertEquals("test-bob", pending.toId());
        long id = Long.parseLong(pending.id());
        assertThrows(ResponseStatusException.class, () -> friends.accept(alice, id));
        assertThrows(ResponseStatusException.class, () -> friends.accept(eve, id));
        assertThrows(ResponseStatusException.class, () -> friends.send(bob, new FriendController.SendRequest("test-alice")));
        friends.accept(bob, id);
        assertEquals(1, friends.list(alice).accepted().size());
        assertEquals(1, friends.list(bob).accepted().size());
        assertThrows(ResponseStatusException.class, () -> friends.remove(alice, "test-alice"));
        assertThrows(ResponseStatusException.class, () -> friends.remove(eve, "test-alice"));
        friends.remove(bob, "test-alice");
        assertTrue(friends.list(alice).accepted().isEmpty());
    }
}

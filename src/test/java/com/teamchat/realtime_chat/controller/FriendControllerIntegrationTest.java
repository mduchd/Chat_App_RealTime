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
import java.security.Principal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FriendControllerIntegrationTest {
    @Autowired FriendController friends;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    private Principal user(String sub) {
        jdbc.update("INSERT INTO users (google_sub, email, display_name) VALUES (?, ?, ?)",
                sub, sub + "@example.com", sub);
        return () -> sub;
    }

    @Test
    void friendEndpointsRejectUnauthenticatedAndUntrustedOrigins() throws Exception {
        mvc.perform(get("/api/friends")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/api/friends/requests").header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void discoverFindsPartialNamesAndExcludesRelationshipsFromSuggestions() {
        var alice = user("discover-alice");
        user("discover-bob");
        user("discover-carol");
        jdbc.update("UPDATE users SET display_name=? WHERE google_sub=?", "Nguyễn Văn Bình", "discover-bob");
        jdbc.update("UPDATE users SET display_name=? WHERE google_sub=?", "Trần Lan", "discover-carol");
        assertEquals("discover-bob", friends.discover(alice, "nguyen van").get(0).id());
        assertEquals("discover-bob", friends.discover(alice, "Bì").get(0).id());
        assertEquals("discover-bob", friends.discover(alice, "discover-bob@example.com").get(0).id());
        assertTrue(friends.discover(alice, "%").isEmpty());
        assertTrue(friends.discover(alice, "").stream().anyMatch(c -> c.id().equals("discover-bob")));
        friends.send(alice, new FriendController.SendRequest("discover-bob"));
        assertTrue(friends.discover(alice, "").stream().noneMatch(c -> c.id().equals("discover-bob")));
        assertEquals("discover-bob", friends.discover(alice, "Bình").get(0).id());
        assertTrue(friends.discover(alice, "discover-alice@example.com").isEmpty());
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

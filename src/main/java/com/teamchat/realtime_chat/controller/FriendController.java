package com.teamchat.realtime_chat.controller;

import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/friends")
public class FriendController {
    private final JdbcTemplate jdbc;

    public FriendController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    record Contact(String id, String name, String email, String avatar) {}
    record Request(String id, String fromId, String toId) {}
    record Snapshot(List<Contact> contacts, List<List<String>> accepted, List<Request> requests) {}
    record SendRequest(String userId) {}

    private long currentId(OAuth2User principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return jdbc.query("SELECT id FROM users WHERE google_sub = ?", rs -> {
            if (!rs.next()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again");
            return rs.getLong(1);
        }, principal.getName());
    }

    @GetMapping
    public Snapshot list(@AuthenticationPrincipal OAuth2User principal) {
        long me = currentId(principal);
        var rows = jdbc.query("SELECT f.id, f.status, f.requester_id, "
                + "lo.google_sub AS low_sub, hi.google_sub AS high_sub, "
                + "other.google_sub AS other_sub, other.display_name, other.email, other.avatar_url "
                + "FROM friendships f JOIN users lo ON lo.id=f.user_low_id "
                + "JOIN users hi ON hi.id=f.user_high_id "
                + "JOIN users other ON other.id=IF(f.user_low_id=?, f.user_high_id, f.user_low_id) "
                + "WHERE f.user_low_id=? OR f.user_high_id=?",
                (rs, n) -> new Object[]{rs.getString("id"), rs.getString("status"),
                        rs.getLong("requester_id"), rs.getString("low_sub"), rs.getString("high_sub"),
                        new Contact(rs.getString("other_sub"), rs.getString("display_name"),
                                rs.getString("email"), rs.getString("avatar_url"))}, me, me, me);
        var contacts = rows.stream().map(r -> (Contact) r[5]).toList();
        var accepted = rows.stream().filter(r -> "ACCEPTED".equals(r[1]))
                .map(r -> List.of((String) r[3], (String) r[4])).toList();
        var requests = rows.stream().filter(r -> "PENDING".equals(r[1]))
                .map(r -> new Request((String) r[0], ((Long) r[2]) == me ? principal.getName() : ((Contact) r[5]).id(),
                        ((Long) r[2]) == me ? ((Contact) r[5]).id() : principal.getName())).toList();
        return new Snapshot(contacts, accepted, requests);
    }

    @GetMapping("/search")
    public List<Contact> search(@AuthenticationPrincipal OAuth2User principal, @RequestParam String email) {
        long me = currentId(principal);
        if (email.isBlank() || email.length() > 255) return List.of();
        return jdbc.query("SELECT google_sub, display_name, email, avatar_url FROM users "
                        + "WHERE email = ? AND id <> ? LIMIT 1",
                (rs, n) -> new Contact(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                email.trim(), me);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public void send(@AuthenticationPrincipal OAuth2User principal, @RequestBody SendRequest request) {
        long me = currentId(principal);
        if (request == null || request.userId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var ids = jdbc.query("SELECT id FROM users WHERE google_sub=?", (rs, n) -> rs.getLong(1), request.userId());
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        long other = ids.get(0);
        if (other == me) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        try {
            jdbc.update("INSERT INTO friendships (user_low_id, user_high_id, requester_id) VALUES (?, ?, ?)",
                    Math.min(me, other), Math.max(me, other), me);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friendship or request already exists");
        }
    }

    @PostMapping("/requests/{id}/accept")
    public void accept(@AuthenticationPrincipal OAuth2User principal, @PathVariable long id) {
        long me = currentId(principal);
        int count = jdbc.update("UPDATE friendships SET status='ACCEPTED' WHERE id=? AND status='PENDING' "
                + "AND requester_id<>? AND (user_low_id=? OR user_high_id=?)", id, me, me, me);
        if (count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/requests/{id}")
    public void cancel(@AuthenticationPrincipal OAuth2User principal, @PathVariable long id) {
        long me = currentId(principal);
        int count = jdbc.update("DELETE FROM friendships WHERE id=? AND status='PENDING' "
                + "AND (user_low_id=? OR user_high_id=?)", id, me, me);
        if (count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{userId}")
    public void remove(@AuthenticationPrincipal OAuth2User principal, @PathVariable String userId) {
        long me = currentId(principal);
        if (principal.getName().equals(userId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        int count = jdbc.update("DELETE FROM friendships WHERE status='ACCEPTED' AND (user_low_id=? OR user_high_id=?) "
                + "AND (user_low_id=(SELECT id FROM users WHERE google_sub=?) "
                + "OR user_high_id=(SELECT id FROM users WHERE google_sub=?))", me, me, userId, userId);
        if (count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}

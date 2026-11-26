package com.teamchat.realtime_chat.controller;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.teamchat.realtime_chat.security.AccountIdentity;
import java.security.Principal;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/friends")
public class FriendController {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private final JdbcTemplate jdbc;
    private final AccountIdentity identity;
    private final SimpMessagingTemplate messages;
    private final boolean accentInsensitiveSearch;

    public FriendController(JdbcTemplate jdbc, AccountIdentity identity, SimpMessagingTemplate messages,
            @Value("${chat.friends.accent-insensitive-search:false}") boolean accentInsensitiveSearch) {
        this.jdbc = jdbc;
        this.identity = identity;
        this.messages = messages;
        this.accentInsensitiveSearch = accentInsensitiveSearch;
    }

    record Contact(String id, String name, String email, String avatar) {}
    record Request(String id, String fromId, String toId) {}
    record Snapshot(List<Contact> contacts, List<List<String>> accepted, List<Request> requests) {}
    record SendRequest(String userId) {}

    private long currentId(Principal principal) { return identity.id(principal); }
    private String external(Principal principal) { return AccountIdentity.external(identity.user(principal)); }

    @GetMapping
    public Snapshot list(Principal principal) {
        long me = currentId(principal);
        var rows = jdbc.query("SELECT f.id, f.status, f.requester_id, "
                + "COALESCE(lo.google_sub, CONCAT('', lo.id)) AS low_sub, COALESCE(hi.google_sub, CONCAT('', hi.id)) AS high_sub, "
                + "COALESCE(other.google_sub, CONCAT('', other.id)) AS other_sub, other.display_name, other.email, other.avatar_url "
                + "FROM friendships f JOIN users lo ON lo.id=f.user_low_id "
                + "JOIN users hi ON hi.id=f.user_high_id "
                + "JOIN users other ON other.id=CASE WHEN f.user_low_id=? THEN f.user_high_id ELSE f.user_low_id END "
                + "WHERE f.user_low_id=? OR f.user_high_id=?",
                (rs, n) -> new Object[]{rs.getString("id"), rs.getString("status"),
                        rs.getLong("requester_id"), rs.getString("low_sub"), rs.getString("high_sub"),
                        new Contact(rs.getString("other_sub"), rs.getString("display_name"),
                                rs.getString("email"), rs.getString("avatar_url"))}, me, me, me);
        var contacts = rows.stream().map(r -> (Contact) r[5]).toList();
        var accepted = rows.stream().filter(r -> "ACCEPTED".equals(r[1]))
                .map(r -> List.of((String) r[3], (String) r[4])).toList();
        var requests = rows.stream().filter(r -> "PENDING".equals(r[1]))
                .map(r -> new Request((String) r[0], ((Long) r[2]) == me ? external(principal) : ((Contact) r[5]).id(),
                        ((Long) r[2]) == me ? ((Contact) r[5]).id() : external(principal))).toList();
        return new Snapshot(contacts, accepted, requests);
    }

    @GetMapping("/search")
    public List<Contact> search(Principal principal, @RequestParam String email) {
        long me = currentId(principal);
        if (email.isBlank() || email.length() > 255) return List.of();
        return jdbc.query("SELECT COALESCE(google_sub, CONCAT('', id)), display_name, email, avatar_url FROM users "
                        + "WHERE email = ? AND id <> ? LIMIT 1",
                (rs, n) -> new Contact(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                email.trim(), me);
    }

    @GetMapping("/discover")
    public List<Contact> discover(Principal principal,
                                  @RequestParam(defaultValue = "") String q) {
        long me = currentId(principal);
        String query = q.trim();
        if (query.length() > 150) return List.of();
        var mapper = (org.springframework.jdbc.core.RowMapper<Contact>) (rs, n) ->
                new Contact(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4));
        if (query.isEmpty()) {
            return jdbc.query("SELECT COALESCE(u.google_sub, CONCAT('', u.id)), u.display_name, u.email, u.avatar_url FROM users u "
                    + "WHERE u.id<>? AND NOT EXISTS (SELECT 1 FROM friendships f "
                    + "WHERE (f.user_low_id=? AND f.user_high_id=u.id) "
                    + "OR (f.user_high_id=? AND f.user_low_id=u.id)) ORDER BY u.id DESC LIMIT 20",
                    mapper, me, me, me);
        }
        if (query.contains("@")) {
            return jdbc.query("SELECT COALESCE(google_sub, CONCAT('', id)), display_name, email, avatar_url FROM users "
                    + "WHERE id<>? AND email=? LIMIT 20", mapper, me, query);
        }
        if (accentInsensitiveSearch) {
            String name = normalizeName(query);
            if (name.isBlank()) return List.of();
            try (var candidates = jdbc.queryForStream(
                    "SELECT COALESCE(google_sub, CONCAT('', id)), display_name, email, avatar_url FROM users "
                            + "WHERE id<>? ORDER BY display_name, id", mapper, me)) {
                return candidates.filter(contact -> normalizeName(contact.name()).contains(name)).limit(20).toList();
            }
        }
        String pattern = "%" + query.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return jdbc.query("SELECT COALESCE(google_sub, CONCAT('', id)), display_name, email, avatar_url FROM users "
                + "WHERE id<>? AND display_name LIKE ? ESCAPE '!' ORDER BY display_name, id LIMIT 20",
                mapper, me, pattern);
    }

    private static String normalizeName(String name) {
        String decomposed = Normalizer.normalize(name.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed).replaceAll("").replace('đ', 'd');
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public void send(Principal principal, @RequestBody SendRequest request) {
        long me = currentId(principal);
        if (request == null || request.userId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        long other = identity.lookup(request.userId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).getId();
        if (other == me) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        try {
            jdbc.update("INSERT INTO friendships (user_low_id, user_high_id, requester_id) VALUES (?, ?, ?)",
                    Math.min(me, other), Math.max(me, other), me);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friendship or request already exists");
        }
        publishFriendshipsChanged(me, other);
    }

    @PostMapping("/requests/{id}/accept")
    public void accept(Principal principal, @PathVariable long id) {
        long me = currentId(principal);
        long other = requestPartner(id, me);
        int count = jdbc.update("UPDATE friendships SET status='ACCEPTED' WHERE id=? AND status='PENDING' "
                + "AND requester_id<>? AND (user_low_id=? OR user_high_id=?)", id, me, me, me);
        if (count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        publishFriendshipsChanged(me, other);
    }

    @DeleteMapping("/requests/{id}")
    public void cancel(Principal principal, @PathVariable long id) {
        long me = currentId(principal);
        long other = requestPartner(id, me);
        int count = jdbc.update("DELETE FROM friendships WHERE id=? AND status='PENDING' "
                + "AND (user_low_id=? OR user_high_id=?)", id, me, me);
        if (count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        publishFriendshipsChanged(me, other);
    }

    @DeleteMapping("/{userId}")
    public void remove(Principal principal, @PathVariable String userId) {
        long me = currentId(principal);
        long other=identity.lookup(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).getId();
        if (other==me) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        int count = jdbc.update("DELETE FROM friendships WHERE status='ACCEPTED' AND user_low_id=? AND user_high_id=?",Math.min(me,other),Math.max(me,other));
        if (count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        publishFriendshipsChanged(me, other);
    }

    private long requestPartner(long id, long me) {
        return jdbc.query("SELECT CASE WHEN user_low_id=? THEN user_high_id ELSE user_low_id END "
                        + "FROM friendships WHERE id=? AND status='PENDING' AND (user_low_id=? OR user_high_id=?)",
                (rs, row) -> rs.getLong(1), me, id, me, me).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void publishFriendshipsChanged(long first, long second) {
        Runnable notify = () -> {
            var event = Map.of("type", "FRIENDS_UPDATED");
            messages.convertAndSendToUser(Long.toString(first), "/queue/friends", event);
            messages.convertAndSendToUser(Long.toString(second), "/queue/friends", event);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notify.run();
                }
            });
        } else {
            notify.run();
        }
    }
}

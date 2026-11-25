package com.teamchat.realtime_chat.security;

import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.repository.UserRepository;
import java.security.Principal;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AccountIdentity {
    private final UserRepository users;
    private final JdbcTemplate jdbc;

    public AccountIdentity(UserRepository users, JdbcTemplate jdbc) {
        this.users = users;
        this.jdbc = jdbc;
    }

    public Optional<User> syncGoogleUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");
        if (email != null && !email.isBlank()) {
            jdbc.update("INSERT INTO users (google_sub, email, display_name, avatar_url, last_login_at) "
                    + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE "
                    + "email = VALUES(email), display_name = VALUES(display_name), "
                    + "avatar_url = VALUES(avatar_url), last_login_at = CURRENT_TIMESTAMP(6)",
                    principal.getName(), email, name == null || name.isBlank() ? email : name, picture);
        }
        return users.findByGoogleSub(principal.getName());
    }
    public User user(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return lookup(principal.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    public Optional<User> lookup(String external) {
        var found = users.findByGoogleSub(external);
        if (found.isEmpty()) {
            try { found = users.findById(Long.parseLong(external)); }
            catch (NumberFormatException ignored) { }
        }
        return found;
    }
    public long id(Principal principal) { return user(principal).getId(); }
    public static String external(User user) { return user.getGoogleSub() == null ? user.getId().toString() : user.getGoogleSub(); }
}

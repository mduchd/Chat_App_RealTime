package com.teamchat.realtime_chat.security;

import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.repository.UserRepository;
import java.security.Principal;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AccountIdentity {
    private final UserRepository users;
    public AccountIdentity(UserRepository users) { this.users = users; }
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

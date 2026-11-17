package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ContactProfileController {
    private final UserRepository users;

    public ContactProfileController(UserRepository users) {
        this.users = users;
    }

    record Profile(String id, String name, String avatar) {}

    @GetMapping("/api/contact-profiles/{id}")
    public Profile profile(@PathVariable String id) {
        var user = users.findByGoogleSub(id);
        if (user.isEmpty()) {
            try {
                user = users.findById(Long.parseLong(id));
            } catch (NumberFormatException ignored) {
                // The identifier is neither a Google subject nor a numeric account ID.
            }
        }
        var found = user.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new Profile(id, found.getDisplayName(), found.getAvatarUrl());
    }
}

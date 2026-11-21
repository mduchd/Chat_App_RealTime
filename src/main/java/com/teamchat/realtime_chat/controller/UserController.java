package com.teamchat.realtime_chat.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import com.teamchat.realtime_chat.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

@RestController
@RequestMapping("/api")
public class UserController {

    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;

    public UserController(JdbcTemplate jdbc, UserRepository userRepository) {
        this.jdbc = jdbc;
        this.userRepository = userRepository;
    }

    // Trả về hồ sơ người đăng nhập theo đúng contract GET /api/me
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal OAuth2User principal,
            Authentication authentication) {
        Map<String, Object> userInfo = new HashMap<>();
        if (principal != null) {
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
            userRepository.findByGoogleSub(principal.getName()).ifPresent(user -> userInfo.put("accountId",user.getId().toString()));
            userInfo.put("id", principal.getName());
            userInfo.put("name", principal.getAttribute("name"));
            userInfo.put("email", principal.getAttribute("email"));
            userInfo.put("picture", principal.getAttribute("picture")); 
            userInfo.put("isAuthenticated", true);
        } else if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            try {
                userRepository.findById(Long.parseLong(authentication.getName())).ifPresent(user -> {
                    userInfo.put("id", user.getGoogleSub()==null ? user.getId().toString() : user.getGoogleSub());
                    userInfo.put("accountId",user.getId().toString());
                    userInfo.put("name", user.getDisplayName());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("picture", user.getAvatarUrl());
                    userInfo.put("isAuthenticated", true);
                });
            } catch (NumberFormatException ignored) {
                // Only numeric IDs are used for email/password sessions.
            }
        }
        if (!userInfo.containsKey("isAuthenticated")) {
            userInfo.put("isAuthenticated", false);
        }
        return userInfo;
    }
}

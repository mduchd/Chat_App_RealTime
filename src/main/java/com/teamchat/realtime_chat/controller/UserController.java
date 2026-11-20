package com.teamchat.realtime_chat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    @PostMapping("/me/avatar")
    public ResponseEntity<Map<String, Object>> updateAvatar(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal OAuth2User principal,
            Authentication authentication) {
        Map<String, Object> res = new HashMap<>();
        String avatarUrl = request.get("avatarUrl");
        if (avatarUrl == null) avatarUrl = request.get("avatar");
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            res.put("success", false);
            res.put("message", "Đường dẫn ảnh đại diện không được để trống!");
            return ResponseEntity.badRequest().body(res);
        }
        avatarUrl = avatarUrl.trim();

        User user = resolveCurrentUser(principal, authentication);
        if (user == null) {
            res.put("success", false);
            res.put("message", "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }

        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        res.put("success", true);
        res.put("message", "Cập nhật ảnh đại diện thành công!");
        res.put("avatarUrl", avatarUrl);
        return ResponseEntity.ok(res);
    }

    @PostMapping(value = "/me/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal OAuth2User principal,
            Authentication authentication) {
        Map<String, Object> res = new HashMap<>();
        if (file == null || file.isEmpty()) {
            res.put("success", false);
            res.put("message", "Vui lòng chọn tệp ảnh hợp lệ!");
            return ResponseEntity.badRequest().body(res);
        }

        User user = resolveCurrentUser(principal, authentication);
        if (user == null) {
            res.put("success", false);
            res.put("message", "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }

        try {
            String originalName = file.getOriginalFilename();
            String ext = "png";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
            }
            if (!Set.of("png", "jpg", "jpeg", "gif", "webp").contains(ext)) {
                res.put("success", false);
                res.put("message", "Chỉ hỗ trợ file ảnh định dạng PNG, JPG, JPEG, GIF, WEBP!");
                return ResponseEntity.badRequest().body(res);
            }

            File uploadDir = new File("uploads/avatars");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            String fileName = "avatar_" + user.getId() + "_" + System.currentTimeMillis() + "." + ext;
            File dest = new File(uploadDir, fileName);
            file.transferTo(dest.toPath());

            String avatarUrl = "/uploads/avatars/" + fileName;
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            res.put("success", true);
            res.put("message", "Tải lên ảnh đại diện thành công!");
            res.put("avatarUrl", avatarUrl);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi lưu file ảnh: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    private User resolveCurrentUser(OAuth2User principal, Authentication authentication) {
        if (principal != null) {
            return userRepository.findByGoogleSub(principal.getName())
                    .orElseGet(() -> {
                        String email = principal.getAttribute("email");
                        return email != null ? userRepository.findByEmail(email).orElse(null) : null;
                    });
        } else if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            try {
                Long uid = Long.parseLong(authentication.getName());
                return userRepository.findById(uid).orElse(null);
            } catch (Exception ignored) {}
        }
        return null;
    }
}

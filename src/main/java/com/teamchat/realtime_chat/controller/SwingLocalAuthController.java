package com.teamchat.realtime_chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile("swing-local")
public class SwingLocalAuthController {
    private final AuthController auth;

    public SwingLocalAuthController(AuthController auth) {
        this.auth = auth;
    }

    @PostMapping(value = "/api/auth/local/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> form,
            HttpServletRequest request, HttpServletResponse response) {
        if (!"127.0.0.1".equals(request.getLocalAddr())
                || !"127.0.0.1".equals(request.getRemoteAddr())
                || !"127.0.0.1".equals(request.getServerName())
                || request.getHeader("Origin") != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Chỉ dành cho ChatSpace chạy trên máy này."));
        }
        return auth.registerLocal(form.get("email"), form.get("password"), request, response);
    }
}

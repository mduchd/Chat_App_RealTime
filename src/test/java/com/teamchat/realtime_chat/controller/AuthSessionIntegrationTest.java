package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import jakarta.servlet.http.Cookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSessionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    @Test
    void rejectsCrossSiteMutationFromUntrustedOrigin() throws Exception {
        mvc.perform(post("/api/auth/logout").header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void passwordLoginCreatesSessionForSubsequentRequests() throws Exception {
        String email = "session-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setEmail(email);
        user.setDisplayName("Session Test");
        user.setPasswordHash(passwords.encode("test-password"));
        user = users.saveAndFlush(user);

        try {
            var login = mvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"test-password\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.id").value(user.getId().toString()))
                    .andReturn();
            Cookie cookie = login.getResponse().getCookie("SESSION");
            assertNotNull(cookie);
            String setCookie = login.getResponse().getHeader("Set-Cookie");
            assertNotNull(setCookie);
            assertTrue(setCookie.contains("Max-Age=2592000"));
            assertTrue(setCookie.contains("HttpOnly"));
            assertTrue(setCookie.contains("Secure"));
            assertTrue(setCookie.contains("SameSite=None"));
            mvc.perform(get("/api/me").cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isAuthenticated").value(true))
                    .andExpect(jsonPath("$.id").value(user.getId().toString()));
            mvc.perform(post("/api/auth/logout").cookie(cookie))
                    .andExpect(status().isNoContent());
            mvc.perform(get("/api/me").cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isAuthenticated").value(false));
        } finally {
            users.delete(user);
        }
    }
}

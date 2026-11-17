package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSessionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

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
            MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
            mvc.perform(get("/api/me").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isAuthenticated").value(true))
                    .andExpect(jsonPath("$.id").value(user.getId().toString()));
        } finally {
            users.delete(user);
        }
    }
}

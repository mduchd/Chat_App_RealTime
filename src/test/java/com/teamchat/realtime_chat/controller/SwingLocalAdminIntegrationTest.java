package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.repository.UserRepository;
import com.teamchat.realtime_chat.security.SwingLocalAdmin;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("swing-local")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:swing-admin;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "chat.swing-local.admin.account-id=7001",
        "chat.swing-local.admin.email=local-operator@fixture.invalid"
})
@AutoConfigureMockMvc
class SwingLocalAdminIntegrationTest {
    private static final String ADMIN_EMAIL = "local-operator@fixture.invalid";
    private static final String PASSWORD = "test-password";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    @Test
    void onlyTheConfiguredAccountCanUseLocalAdminEndpoints() throws Exception {
        insertUser(7001, ADMIN_EMAIL, "Local Operator");
        insertUser(7002, "ordinary@fixture.invalid", "Admin");

        mvc.perform(get("/api/admin/stats")).andExpect(status().is3xxRedirection());

        Cookie ordinary = login("ordinary@fixture.invalid", false);
        mvc.perform(get("/api/admin/stats").cookie(ordinary)).andExpect(status().isForbidden());
        mvc.perform(get("/admin.html").cookie(ordinary)).andExpect(status().isForbidden());

        Cookie admin = login(ADMIN_EMAIL, true);
        mvc.perform(get("/api/admin/stats").cookie(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        jdbc.update("DELETE FROM users WHERE id=?", 7001);
        mvc.perform(get("/api/admin/stats").cookie(admin)).andExpect(status().isForbidden());
    }

    @Test
    void loadsConversationAndMessageDetailsForTheLocalAdmin() throws Exception {
        // A separate fixture account owns the conversation and sends the message.
        insertUser(7001, ADMIN_EMAIL, "Local Operator");
        insertUser(7020, "sender@fixture.invalid", "Fixture Sender");
        jdbc.update("INSERT INTO conversations (id, type, name, created_by) VALUES (?, ?, ?, ?)",
                7100, "GROUP", "Fixture Group", 7020);
        jdbc.update("INSERT INTO conversation_members (conversation_id, user_id, role) VALUES (?, ?, ?)",
                7100, 7020, "OWNER");
        jdbc.update("INSERT INTO messages (id, conversation_id, sender_id, content) VALUES (?, ?, ?, ?)",
                7200, 7100, 7020, "Fixture message");

        try {
            Cookie admin = login(ADMIN_EMAIL, true);
            mvc.perform(get("/api/admin/stats").cookie(admin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalGroups").value(1))
                    .andExpect(jsonPath("$.totalMessages").value(1));

            assertAll(
                    () -> mvc.perform(get("/api/admin/conversations").cookie(admin))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.conversations[0].name").value("Fixture Group"))
                            .andExpect(jsonPath("$.conversations[0].createdByName").value("Fixture Sender"))
                            .andExpect(jsonPath("$.conversations[0].memberCount").value(1)),
                    () -> mvc.perform(get("/api/admin/messages").cookie(admin))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.messages[0].conversationName").value("Fixture Group"))
                            .andExpect(jsonPath("$.messages[0].senderName").value("Fixture Sender"))
                            .andExpect(jsonPath("$.messages[0].content").value("Fixture message"))
            );
            mvc.perform(get("/api/admin/conversations").param("search", "fixture group").cookie(admin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conversations[0].id").value(7100));
        } finally {
            jdbc.update("DELETE FROM conversations WHERE id = ?", 7100);
            jdbc.update("DELETE FROM users WHERE id IN (?, ?)", 7001, 7020);
        }
    }

    @Test
    void bootstrapsAdminAndResumesAnInterruptedSetup(@TempDir Path stateDirectory) throws Exception {
        insertUser(7010, "qt@local.test", "Existing user");
        var setup = new SwingLocalAdmin(users, passwords, stateDirectory, 0, "");
        setup.run(null);

        Path noteFile = stateDirectory.resolve("swing-local-admin-login.txt");
        Path configFile = stateDirectory.resolve("swing-local-admin.properties");
        var localLogin = Files.readAllLines(noteFile);
        assertEquals("Email: qt2@local.test", localLogin.get(0));
        var generatedAdmin = users.findByEmail("qt2@local.test").orElseThrow();
        assertTrue(passwords.matches(localLogin.get(1).substring("Password: ".length()), generatedAdmin.getPasswordHash()));
        assertTrue(setup.matches(generatedAdmin));
        assertFalse(setup.matches(users.findById(7010L).orElseThrow()));
        assertTrue(Files.readString(configFile).contains("account-id=" + generatedAdmin.getId()));

        Files.delete(configFile);
        var resumed = new SwingLocalAdmin(users, passwords, stateDirectory, 0, "");
        resumed.run(null);
        assertTrue(resumed.matches(generatedAdmin));
        assertTrue(localLogin.equals(Files.readAllLines(noteFile)));

        var restarted = new SwingLocalAdmin(users, passwords, stateDirectory, generatedAdmin.getId(), generatedAdmin.getEmail());
        restarted.run(null);
        assertTrue(restarted.matches(generatedAdmin));
        assertTrue(localLogin.equals(Files.readAllLines(noteFile)));
    }

    private void insertUser(long id, String email, String name) {
        jdbc.update("INSERT INTO users (id, email, display_name, password_hash) VALUES (?, ?, ?, ?)",
                id, email, name, passwords.encode(PASSWORD));
    }

    private Cookie login(String email, boolean admin) throws Exception {
        var login = mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.admin").value(admin))
                .andReturn();
        Cookie cookie = login.getResponse().getCookie("SESSION");
        assertNotNull(cookie);
        return cookie;
    }
}

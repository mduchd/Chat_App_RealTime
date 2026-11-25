package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.security.DesktopGoogleSignIn;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:desktop-google;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "spring.session.jdbc.initialize-schema=always", "spring.session.jdbc.platform=h2"
})
@AutoConfigureMockMvc
@Sql(statements = "ALTER TABLE users ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class DesktopGoogleSignInIntegrationTest {
    private static final String VERIFIER = "a".repeat(43);

    @Autowired MockMvc mvc;
    @Autowired DesktopGoogleSignIn desktopGoogle;

    @Test
    void googleCancellationReturnsToLoopbackOnlyForDesktopRequests() throws Exception {
        String challenge = challenge();
        var desktop = mvc.perform(get("/oauth2/authorization/google")
                        .param("desktop_port", "56500").param("desktop_challenge", challenge))
                .andExpect(status().isFound()).andReturn().getResponse();
        mvc.perform(get("/login/oauth2/code/google").cookie(desktop.getCookie("SESSION"))
                        .param("state", oauthState(desktop)).param("error", "access_denied"))
                .andExpect(redirectedUrl("http://127.0.0.1:56500/oauth/callback?state=" + challenge + "&error=access_denied"));

        var web = mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isFound()).andReturn().getResponse();
        mvc.perform(get("/login/oauth2/code/google").cookie(web.getCookie("SESSION"))
                        .param("state", oauthState(web)).param("error", "access_denied"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void requiresTheDesktopProofAndExchangesTheCodeOnlyOnceForASession() throws Exception {
        var authorization = OAuth2AuthorizationRequest.authorizationCode().clientId("test-client")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth").state("provider-state").build();
        var browser = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        browser.addParameter("desktop_port", "56500");
        browser.addParameter("desktop_challenge", challenge());
        desktopGoogle.saveAuthorizationRequest(authorization, browser, new MockHttpServletResponse());

        var callback = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        callback.setSession((MockHttpSession) browser.getSession(false));
        callback.addParameter("state", "wrong-state");
        var response = new MockHttpServletResponse();
        assertNull(desktopGoogle.removeAuthorizationRequest(callback, response));
        callback.setParameter("state", "provider-state");
        assertNotNull(desktopGoogle.removeAuthorizationRequest(callback, response));

        OidcUser google = mock(OidcUser.class);
        when(google.getName()).thenReturn("desktop-test-google-id");
        when(google.getEmailVerified()).thenReturn(true);
        when(google.getAttribute("email")).thenReturn("desktop-google@example.test");
        when(google.getAttribute("name")).thenReturn("Desktop Google Test");
        assertTrue(desktopGoogle.complete(callback, response,
                UsernamePasswordAuthenticationToken.authenticated(google, null, java.util.List.of())));
        URI redirect = URI.create(response.getRedirectedUrl());
        assertEquals("127.0.0.1", redirect.getHost());
        String code = UriComponentsBuilder.fromUri(redirect).build().getQueryParams().getFirst("code");
        assertNotNull(code);

        mvc.perform(exchange(code, "b".repeat(43))).andExpect(status().isUnauthorized());
        var signedIn = mvc.perform(exchange(code, VERIFIER)).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value("desktop-test-google-id")).andReturn().getResponse();
        mvc.perform(get("/api/me").cookie(signedIn.getCookie("SESSION")))
                .andExpect(jsonPath("$.isAuthenticated").value(true))
                .andExpect(jsonPath("$.id").value("desktop-test-google-id"));
        mvc.perform(exchange(code, VERIFIER)).andExpect(status().isUnauthorized());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder exchange(String code, String verifier) {
        return post("/api/auth/desktop/google").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"%s","verifier":"%s"}
                        """.formatted(code, verifier));
    }

    private static String challenge() throws Exception {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(VERIFIER.getBytes(StandardCharsets.US_ASCII)));
    }

    private static String oauthState(MockHttpServletResponse response) {
        String state = UriComponentsBuilder.fromUriString(response.getRedirectedUrl()).build()
                .getQueryParams().getFirst("state");
        return URLDecoder.decode(state, StandardCharsets.UTF_8);
    }
}

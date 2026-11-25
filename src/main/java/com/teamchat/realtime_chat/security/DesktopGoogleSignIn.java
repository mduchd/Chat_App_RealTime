package com.teamchat.realtime_chat.security;

import com.teamchat.realtime_chat.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DesktopGoogleSignIn implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private static final String PORT = "desktop_port";
    private static final String CHALLENGE = "desktop_challenge";
    private static final String CALLBACK_ATTRIBUTE = DesktopGoogleSignIn.class.getName() + ".callback";
    private final HttpSessionOAuth2AuthorizationRequestRepository browserRequests =
            new HttpSessionOAuth2AuthorizationRequestRepository();
    private final ConcurrentHashMap<String, Grant> grants = new ConcurrentHashMap<>();
    private final AccountIdentity accounts;

    public DesktopGoogleSignIn(AccountIdentity accounts) {
        this.accounts = accounts;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return browserRequests.loadAuthorizationRequest(request);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorization, HttpServletRequest request,
            HttpServletResponse response) {
        String port = request.getParameter(PORT);
        String challenge = request.getParameter(CHALLENGE);
        if (authorization != null && request.getRequestURI().equals(request.getContextPath() + "/oauth2/authorization/google")
                && validPort(port) && validProof(challenge)) {
            authorization = OAuth2AuthorizationRequest.from(authorization).attributes(attributes -> {
                attributes.put(PORT, port);
                attributes.put(CHALLENGE, challenge);
            }).build();
        }
        browserRequests.saveAuthorizationRequest(authorization, request, response);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorization = browserRequests.removeAuthorizationRequest(request, response);
        // The Spring repository only returns the authorization request when the Google state matches.
        if (authorization != null && authorization.getAttributes().containsKey(PORT)) {
            request.setAttribute(CALLBACK_ATTRIBUTE,
                    new Callback(authorization.getAttribute(PORT), authorization.getAttribute(CHALLENGE)));
        }
        return authorization;
    }

    public boolean complete(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        if (!(request.getAttribute(CALLBACK_ATTRIBUTE) instanceof Callback callback)) {
            return false;
        }
        if (authentication.getPrincipal() instanceof OidcUser google && Boolean.TRUE.equals(google.getEmailVerified())) {
            Optional<User> user = accounts.syncGoogleUser(google);
            if (user.isPresent()) {
                Instant now = Instant.now();
                grants.values().removeIf(grant -> !grant.expiresAt().isAfter(now));
                String code = UUID.randomUUID().toString();
                grants.put(code, new Grant(user.get().getId(), callback.challenge(), now.plusSeconds(60)));
                redirect(response, callback, "code=" + code);
                return true;
            }
        }
        redirect(response, callback, "error=access_denied");
        return true;
    }

    public boolean fail(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!(request.getAttribute(CALLBACK_ATTRIBUTE) instanceof Callback callback)) {
            return false;
        }
        redirect(response, callback, "error=access_denied");
        return true;
    }

    public Optional<Long> exchange(String code, String verifier) {
        if (code == null || !validProof(verifier)) {
            return Optional.empty();
        }
        Grant grant = grants.get(code);
        if (grant == null || !grant.expiresAt().isAfter(Instant.now())
                || !MessageDigest.isEqual(grant.challenge().getBytes(StandardCharsets.US_ASCII), challenge(verifier))
                || !grants.remove(code, grant)) {
            return Optional.empty();
        }
        return Optional.of(grant.userId());
    }

    private static byte[] challenge(String verifier) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encode(
                    MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static boolean validProof(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{43}");
    }

    private static boolean validPort(String port) {
        if (port == null || !port.matches("[1-9][0-9]{3,4}")) {
            return false;
        }
        int number = Integer.parseInt(port);
        return number >= 1024 && number <= 65535;
    }

    private static void redirect(HttpServletResponse response, Callback callback, String result) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.sendRedirect("http://127.0.0.1:" + callback.port() + "/oauth/callback?state="
                + callback.challenge() + "&" + result);
    }

    private record Callback(String port, String challenge) {}
    private record Grant(long userId, String challenge, Instant expiresAt) {}
}

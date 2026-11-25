package com.teamchat.swing.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import okhttp3.HttpUrl;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class GoogleSignIn {
    private static final String CALLBACK_PATH = "/oauth/callback";

    private GoogleSignIn() {}

    public static ApiClient.ApiResponse signIn() throws Exception {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new IOException("Không thể mở trình duyệt mặc định để đăng nhập Google.");
        }

        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String verifier = encoder.encodeToString(random);
        String challenge = encoder.encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));

        HttpServer listener = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CompletableFuture<String> result = new CompletableFuture<>();
        String code;
        try {
            listener.createContext(CALLBACK_PATH, exchange -> receiveCallback(exchange, challenge, result));
            listener.start();
            Desktop.getDesktop().browse(authorizationUrl(listener.getAddress().getPort(), challenge).uri());
            code = result.get(3, TimeUnit.MINUTES);
        } catch (TimeoutException timeout) {
            throw new IOException("Đã hết thời gian chờ đăng nhập Google. Vui lòng thử lại.");
        } finally {
            listener.stop(0);
        }
        if (code.isEmpty()) {
            throw new IOException("Đăng nhập Google chưa hoàn tất. Vui lòng thử lại.");
        }
        return ApiClient.exchangeGoogleCode(code, verifier);
    }

    private static HttpUrl authorizationUrl(int port, String challenge) {
        HttpUrl backend = HttpUrl.get(ApiClient.getBaseUrl());
        HttpUrl.Builder builder = backend.newBuilder();
        if (!backend.isHttps() && "127.0.0.1".equals(backend.host())) {
            builder.host("localhost");
        }
        return builder.addPathSegments("oauth2/authorization/google")
                .addQueryParameter("desktop_port", Integer.toString(port))
                .addQueryParameter("desktop_challenge", challenge)
                .build();
    }

    private static void receiveCallback(HttpExchange exchange, String challenge, CompletableFuture<String> result)
            throws IOException {
        try {
            String query = exchange.getRequestURI().getRawQuery();
            String expectedState = "state=" + challenge + "&";
            if (!"GET".equals(exchange.getRequestMethod()) || !CALLBACK_PATH.equals(exchange.getRequestURI().getPath())
                    || query == null || !query.startsWith(expectedState) || result.isDone()) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }
            String outcome = query.substring(expectedState.length());
            if (!(outcome.matches("code=[0-9a-f-]{36}") || "error=access_denied".equals(outcome))) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            boolean authorized = outcome.startsWith("code=");
            String message = authorized ? "Đã xác thực với Google. Bạn có thể quay lại ChatSpace."
                    : "Đăng nhập Google chưa hoàn tất. Hãy quay lại ChatSpace để thử lại.";
            byte[] body = message.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
            exchange.sendResponseHeaders(200, body.length);
            try (var stream = exchange.getResponseBody()) {
                stream.write(body);
            }
            result.complete(authorized ? outcome.substring("code=".length()) : "");
        } finally {
            exchange.close();
        }
    }
}

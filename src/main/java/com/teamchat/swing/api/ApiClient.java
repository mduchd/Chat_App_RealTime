package com.teamchat.swing.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.teamchat.swing.model.ChatMessage;
import com.teamchat.swing.model.ConversationItem;
import com.teamchat.swing.model.UserInfo;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * =========================================================================
 * FILE: ApiClient.java - GỌI HTTP REST API & QUẢN LÝ SESSION COOKIE
 * =========================================================================
 * 
 * [MỤC ĐÍCH & CHỨC NĂNG]
 * - Sử dụng thư viện `com.squareup.okhttp3:okhttp` để gửi các yêu cầu HTTP (GET, POST, DELETE)
 *   tới các RestController của Spring Boot Backend.
 * - Sử dụng CookieManager với chính sách ACCEPT_ALL để tự động lưu và gửi kèm
 *   Cookie `JSESSIONID` trong mọi request sau khi đăng nhập thành công.
 * - Cung cấp hàm `getSessionCookieString()` để chuyển Cookie sang cho StompClient (WebSocket)
 *   giúp xác thực danh tính người dùng qua Spring Security.
 * - Sử dụng Google Gson (`com.google.code.gson:gson`) để đóng gói và bóc tách dữ liệu JSON.
 */
public class ApiClient {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:8080";
    private static String baseUrl = DEFAULT_BASE_URL;

    private static final Gson gson = new Gson();
    
    // CookieManager lưu trữ JSESSIONID của phiên làm việc
    private static final CookieManager cookieManager = new CookieManager();
    private static OkHttpClient client;
    private static String lastSessionCookie = "";

    static {
        // Chấp nhận toàn bộ Cookie trả về từ server
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        
        // Khởi tạo OkHttpClient với cấu hình Timeout và CookieJar tự động
        client = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .followRedirects(false)
                .build();
    }

    public static void setBaseUrl(String url) {
        baseUrl = url.replaceAll("/+$", "");
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Lấy chuỗi Cookie Session hiện tại (ví dụ: "JSESSIONID=ABC123...")
     * để truyền vào Header cho WebSocket Handshake.
     */
    public static String getSessionCookieString() {
        if (!lastSessionCookie.isEmpty()) {
            return lastSessionCookie;
        }
        try {
            java.net.URI uri = java.net.URI.create(baseUrl);
            Map<String, List<String>> headers = cookieManager.get(uri, Collections.emptyMap());
            List<String> cookies = headers.get("Cookie");
            if (cookies != null && !cookies.isEmpty()) {
                return String.join("; ", cookies);
            }
            List<java.net.HttpCookie> storeCookies = cookieManager.getCookieStore().getCookies();
            if (storeCookies != null && !storeCookies.isEmpty()) {
                List<String> cookieParts = new ArrayList<>();
                for (java.net.HttpCookie c : storeCookies) {
                    cookieParts.add(c.getName() + "=" + c.getValue());
                }
                return String.join("; ", cookieParts);
            }
        } catch (Exception ignored) {}
        return "";
    }

    // =====================================================================
    // 1. NHÓM API XÁC THỰC TÀI KHOẢN (AUTH CONTROLLER)
    // =====================================================================

    /**
     * Đăng nhập: POST /api/auth/login
     * @param email    Email của người dùng
     * @param password Mật khẩu
     */
    public static ApiResponse login(String email, String password) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        return post("/api/auth/login", body);
    }

    static ApiResponse exchangeGoogleCode(String code, String verifier) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("code", code);
        body.addProperty("verifier", verifier);
        return post("/api/auth/desktop/google", body);
    }

    public static ApiResponse registerLocal(String email, String password) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        return post("/api/auth/local/register", body);
    }

    /**
     * Đăng xuất: POST /api/auth/logout (Hủy session trên server)
     */
    public static void logout() throws IOException {
        post("/api/auth/logout", new JsonObject());
    }

    /**
     * Cập nhật ảnh đại diện bằng URL hoặc Preset: POST /api/me/avatar
     */
    public static ApiResponse updateAvatar(String avatarUrl) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("avatarUrl", avatarUrl);
        return post("/api/me/avatar", body);
    }

    /**
     * Tải tệp ảnh đại diện từ máy tính lên server: POST /api/me/avatar/upload
     */
    public static ApiResponse uploadAvatar(java.io.File file) throws IOException {
        String mime = "image/png";
        String lower = file.getName().toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            mime = "image/jpeg";
        } else if (lower.endsWith(".gif")) {
            mime = "image/gif";
        } else if (lower.endsWith(".webp")) {
            mime = "image/webp";
        }

        RequestBody fileBody = RequestBody.create(file, MediaType.parse(mime));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(baseUrl + "/api/me/avatar/upload")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            ApiResponse apiResponse = new ApiResponse();
            apiResponse.httpCode = response.code();
            apiResponse.rawBody = responseBody;
            try {
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                apiResponse.json = json;
                if (json.has("success")) {
                    apiResponse.success = json.get("success").getAsBoolean();
                } else {
                    apiResponse.success = response.isSuccessful();
                }
                if (json.has("message")) {
                    apiResponse.message = json.get("message").getAsString();
                }
            } catch (Exception e) {
                apiResponse.success = response.isSuccessful();
            }
            return apiResponse;
        }
    }

    // =====================================================================
    // 2. NHÓM API DỮ LIỆU CHAT & ĐỒNG BỘ (CHAT STORAGE CONTROLLER)
    // =====================================================================

    /**
     * Lấy toàn bộ dữ liệu người dùng (Hội thoại, tin nhắn, danh bạ, cài đặt):
     * GET /api/chat/snapshot
     */
    public static JsonObject getSnapshot() throws IOException {
        String json = get("/api/chat/snapshot");
        return JsonParser.parseString(json).getAsJsonObject();
    }

    /**
     * Đồng bộ trạng thái mới lên server: POST /api/chat/sync
     */
    public static void syncState(Map<String, Object> state) throws IOException {
        requireSuccess(post("/api/chat/sync", gson.toJsonTree(state).getAsJsonObject()));
    }

    public static void addMembersToGroup(String groupId, List<String> memberIds) throws IOException {
        syncState(Map.of("conversations", List.of(Map.of("id", groupId, "addMembers", memberIds))));
    }

    public static String continueConversation(String conversationId) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("conversationId", conversationId);
        ApiResponse response = post("/api/chat/continue", body);
        requireSuccess(response);
        if (response.json == null || !response.json.has("conversationId")) {
            throw new IOException("Máy chủ không trả về hội thoại để tiếp tục.");
        }
        return response.json.get("conversationId").getAsString();
    }

    private static void requireSuccess(ApiResponse response) throws IOException {
        if (!response.success) {
            throw new IOException(response.message != null ? response.message : "Máy chủ trả về HTTP " + response.httpCode);
        }
    }

    // =====================================================================
    // 3. NHÓM API HỘI THOẠI & TIN NHẮN (CONVERSATION / MESSAGE CONTROLLER)
    // =====================================================================

    /**
     * Lấy danh sách hội thoại: GET /api/conversations
     */
    public static String getConversationsJson() throws IOException {
        return get("/api/conversations");
    }

    /**
     * Lấy lịch sử tin nhắn của một hội thoại: GET /api/conversations/{id}/messages
     */
    public static String getMessagesJson(long conversationId, Long before) throws IOException {
        String path = "/api/conversations/" + conversationId + "/messages";
        if (before != null) path += "?before=" + before;
        return get(path);
    }

    // =====================================================================
    // 4. NHÓM API NHÓM CHAT (GROUP CONTROLLER)
    // =====================================================================

    /**
     * Tạo nhóm chat mới: POST /api/groups
     * @param groupName Tên nhóm chat
     * @param memberIds Danh sách ID tài khoản thành viên
     */
    public static ApiResponse createGroup(String groupName, List<String> memberIds) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("groupName", groupName);
        body.add("memberIds", gson.toJsonTree(memberIds));
        return post("/api/groups", body);
    }

    /**
     * Thêm thành viên vào nhóm chat: POST /api/groups/{id}/members
     */
    public static void addMember(long groupId, String userId) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("userId", userId);
        post("/api/groups/" + groupId + "/members", body);
    }

    // =====================================================================
    // 5. NHÓM API BẠN BÈ & LỜI MỜI (FRIEND CONTROLLER)
    // =====================================================================

    /**
     * Lấy danh sách bạn bè và lời mời: GET /api/friends
     */
    public static String getFriends() throws IOException {
        return get("/api/friends");
    }

    /**
     * Tìm kiếm người dùng theo tên hoặc email: GET /api/friends/discover?q=...
     */
    public static String discoverUsers(String query) throws IOException {
        String encoded = java.net.URLEncoder.encode(query, "UTF-8");
        return get("/api/friends/discover?q=" + encoded);
    }

    /**
     * Gửi lời mời kết bạn: POST /api/friends/requests
     */
    public static void sendFriendRequest(String userId) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("userId", userId);
        requireSuccess(post("/api/friends/requests", body));
    }

    /**
     * Chấp nhận lời mời kết bạn: POST /api/friends/requests/{id}/accept
     */
    public static void acceptFriendRequest(long requestId) throws IOException {
        requireSuccess(post("/api/friends/requests/" + requestId + "/accept", new JsonObject()));
    }

    /**
     * Từ chối / Hủy lời mời kết bạn: DELETE /api/friends/requests/{id}
     */
    public static void cancelFriendRequest(long requestId) throws IOException {
        delete("/api/friends/requests/" + requestId);
    }

    /**
     * Xóa bạn bè: DELETE /api/friends/{userId}
     */
    public static void removeFriend(String userId) throws IOException {
        delete("/api/friends/" + userId);
    }

    // =====================================================================
    // 6. NHÓM API QUẢN TRỊ VIÊN (ADMIN CONTROLLER)
    // =====================================================================

    public static JsonObject getAdminStats() throws IOException {
        String res = get("/api/admin/stats");
        return JsonParser.parseString(res).getAsJsonObject();
    }

    public static JsonObject getAdminUsers(String search) throws IOException {
        String path = "/api/admin/users";
        if (search != null && !search.isEmpty()) {
            path += "?search=" + java.net.URLEncoder.encode(search, "UTF-8");
        }
        String res = get(path);
        return JsonParser.parseString(res).getAsJsonObject();
    }

    public static void deleteAdminUser(long userId) throws IOException {
        delete("/api/admin/users/" + userId);
    }

    public static JsonObject getAdminConversations(String search) throws IOException {
        String path = "/api/admin/conversations";
        if (search != null && !search.isEmpty()) {
            path += "?search=" + java.net.URLEncoder.encode(search, "UTF-8");
        }
        String res = get(path);
        return JsonParser.parseString(res).getAsJsonObject();
    }

    public static void deleteAdminConversation(long convId) throws IOException {
        delete("/api/admin/conversations/" + convId);
    }

    public static JsonObject getAdminMessages() throws IOException {
        String res = get("/api/admin/messages");
        return JsonParser.parseString(res).getAsJsonObject();
    }

    // =====================================================================
    // 7. CÁC HÀM TIỆN ÍCH GỬI HTTP THỰC TẾ (HELPER METHODS)
    // =====================================================================

    private static void extractSessionCookie(Response response) {
        String setCookie = response.header("Set-Cookie");
        if (setCookie != null && setCookie.contains("SESSION=")) {
            for (String part : setCookie.split(";")) {
                part = part.trim();
                if (part.startsWith("SESSION=")) {
                    lastSessionCookie = part;
                    break;
                }
            }
        }
    }

    /**
     * Thực hiện HTTP GET request
     */
    private static String get(String path) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            extractSessionCookie(response);
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + responseBody);
            }
            return responseBody;
        }
    }

    /**
     * Thực hiện HTTP POST request với body định dạng JSON
     */
    private static ApiResponse post(String path, JsonObject body) throws IOException {
        RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .post(requestBody)
                .build();
        try (Response response = client.newCall(request).execute()) {
            extractSessionCookie(response);
            String responseBody = response.body() != null ? response.body().string() : "";
            ApiResponse apiResponse = new ApiResponse();
            apiResponse.httpCode = response.code();
            apiResponse.rawBody = responseBody;

            // Bóc tách JSON trả về từ server
            try {
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                apiResponse.json = json;
                if (json.has("success")) {
                    apiResponse.success = json.get("success").getAsBoolean();
                } else {
                    apiResponse.success = response.isSuccessful();
                }
                if (json.has("message")) {
                    apiResponse.message = json.get("message").getAsString();
                }
                if (json.has("user")) {
                    apiResponse.user = gson.fromJson(json.get("user"), UserInfo.class);
                }
            } catch (Exception e) {
                apiResponse.success = response.isSuccessful();
            }
            return apiResponse;
        }
    }

    /**
     * Thực hiện HTTP DELETE request
     */
    private static void delete(String path) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .delete()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new IOException("HTTP " + response.code() + ": " + body);
            }
        }
    }

    // =====================================================================
    // LỚP DTO BAO GÓI KẾT QUẢ TRẢ VỀ (RESPONSE WRAPPER)
    // =====================================================================
    public static class ApiResponse {
        public int httpCode;
        public boolean success;
        public String message;
        public String rawBody;
        public JsonObject json;
        public UserInfo user;
    }
}

package com.teamchat.swing.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * =========================================================================
 * FILE: StompClient.java - XỬ LÝ WEBSOCKET & GIAO THỨC STOMP REALTIME
 * =========================================================================
 * 
 * [MỤC ĐÍCH & CHỨC NĂNG]
 * - Sử dụng thư viện `org.java-websocket:Java-WebSocket` để thiết lập kết nối
 *   TCP WebSocket liên tục với máy chủ Backend Spring Boot (endpoint: /ws).
 * - Tự động cài đặt giao thức STOMP 1.1 (Simple Text Oriented Messaging Protocol)
 *   phía trên WebSocket để tương thích 100% với Spring WebSocket Message Broker.
 * 
 * [CƠ CHẾ HOẠT ĐỘNG CỦA STOMP]
 * 1. Bắt tay WebSocket (Handshake): Kèm Cookie Session (JSESSIONID) để Spring xác thực.
 * 2. Gửi Frame "CONNECT": Báo cho Spring Boot biết client đã sẵn sàng giao tiếp.
 * 3. Nhận Frame "CONNECTED": Máy chủ đồng ý, client bắt đầu gửi/nhận tin.
 * 4. Gửi Frame "SUBSCRIBE": Đăng ký lắng nghe kênh (/user/queue/private hoặc /topic/room.{id}).
 * 5. Gửi Frame "SEND": Bắn tin nhắn qua kênh đích (/app/chat.private).
 * 6. Nhận Frame "MESSAGE": Máy chủ đẩy tin nhắn mới về theo thời gian thực.
 * 7. Tự động kết nối lại (Auto Reconnect): Nếu mất mạng, tự thử lại sau mỗi 5 giây.
 */
public class StompClient {

    // Đối tượng WebSocketClient nền tảng của thư viện Java-WebSocket
    private WebSocketClient ws;

    // Quản lý các kênh đăng ký: Map<Mã_Subscription_ID, Hàm_Callback_Xử_Lý_Tin_Nhắn>
    private final Map<String, BiConsumer<String, String>> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, String> subscriptionDestinations = new ConcurrentHashMap<>();
    private int subscriptionCounter = 0;

    // Trạng thái kết nối STOMP hiện tại
    private volatile boolean connected = false;
    private Consumer<Boolean> connectionListener;

    // Bộ lập lịch chạy ngầm cho tính năng tự động kết nối lại (Heartbeat / Reconnect)
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "stomp-reconnect");
        t.setDaemon(true); // Thread daemon tự tắt khi ứng dụng đóng
        return t;
    });
    private ScheduledFuture<?> reconnectTask;

    private String lastUrl;
    private Map<String, String> customHeaders;

    /**
     * Lắng nghe sự thay đổi trạng thái kết nối (Dùng để đổi màu chấm WebSocket xanh/xám trên giao diện)
     */
    public void setConnectionListener(Consumer<Boolean> listener) {
        this.connectionListener = listener;
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Kết nối WebSocket mặc định không truyền header phụ
     */
    public void connect(String wsUrl) {
        connect(wsUrl, Collections.emptyMap());
    }

    /**
     * -----------------------------------------------------------------
     * BƯỚC 1: KẾT NỐI WEBSOCKET CÓ KÈM HEADER COOKIE PHỤC VỤ XÁC THỰC
     * -----------------------------------------------------------------
     * @param wsUrl   Ví dụ: "ws://localhost:8080/ws"
     * @param headers Chứa "Cookie: JSESSIONID=..." lấy từ ApiClient
     */
    public void connect(String wsUrl, Map<String, String> headers) {
        lastUrl = wsUrl;
        this.customHeaders = headers != null ? new HashMap<>(headers) : Collections.emptyMap();

        try {
            // Nếu có kết nối cũ thì đóng trước khi tạo kết nối mới
            if (ws != null) {
                try { ws.closeBlocking(); } catch (Exception ignored) {}
            }

            // Khởi tạo WebSocketClient với URL và Headers xác thực
            ws = new WebSocketClient(new URI(wsUrl), this.customHeaders) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    // Khi bắt tay HTTP WebSocket thành công, gửi ngay Frame STOMP CONNECT:
                    // Chuẩn STOMP kết thúc bằng ký tự null (\0)
                    String connectFrame = "CONNECT\n" +
                            "accept-version:1.1,1.0\n" +
                            "heart-beat:10000,10000\n" +
                            "\n\0";
                    send(connectFrame);
                }

                @Override
                public void onMessage(String message) {
                    // Xử lý gói tin dạng Text nhận được từ máy chủ
                    handleStompFrame(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    // Khi bị ngắt kết nối: thông báo ra giao diện và hẹn giờ kết nối lại
                    connected = false;
                    if (connectionListener != null) connectionListener.accept(false);
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("Lỗi WebSocket: " + ex.getMessage());
                }
            };

            // Bắt đầu kết nối bất đồng bộ trong nền
            ws.connect();
        } catch (Exception e) {
            System.err.println("Không thể mở kết nối WebSocket: " + e.getMessage());
            scheduleReconnect();
        }
    }

    /**
     * -----------------------------------------------------------------
     * BƯỚC 2: PHÂN TÍCH FRAME STOMP (PARSER)
     * -----------------------------------------------------------------
     * Cấu trúc một frame STOMP gồm 3 phần:
     * COMMAND (CONNECT, CONNECTED, MESSAGE, SEND...)
     * Headers (destination:/..., content-type:...)
     * \n\n (Dòng trống phân cách)
     * Body (Nội dung JSON của tin nhắn) kết thúc bằng \0
     */
    private void handleStompFrame(String raw) {
        if (raw == null || raw.isEmpty()) return;

        // Xử lý gói tin Heartbeat giữ kết nối (ping/pong dạng dòng trống \n)
        if (raw.equals("\n") || raw.equals("\r\n")) {
            if (ws != null && ws.isOpen()) ws.send("\n");
            return;
        }

        // Tách phần Header và Body dựa vào 2 dấu xuống dòng "\n\n"
        String[] parts = raw.split("\n\n", 2);
        String headerSection = parts[0];
        String body = parts.length > 1 ? parts[1].replace("\0", "") : "";

        // Đọc từng dòng Header
        String[] headerLines = headerSection.split("\n");
        String command = headerLines[0].trim();
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < headerLines.length; i++) {
            int colon = headerLines[i].indexOf(':');
            if (colon > 0) {
                headers.put(headerLines[i].substring(0, colon), headerLines[i].substring(colon + 1));
            }
        }

        // Xử lý theo loại Command của STOMP:
        switch (command) {
            case "CONNECTED":
                // Máy chủ xác nhận kết nối STOMP thành công
                connected = true;
                if (connectionListener != null) connectionListener.accept(true);
                // Đăng ký lại tất cả các kênh nếu đây là lần kết nối lại (reconnect)
                resubscribeAll();
                break;

            case "MESSAGE":
                // Nhận được tin nhắn mới từ một kênh đã đăng ký (subscribe)
                String subId = headers.get("subscription");
                String destination = headers.get("destination");
                if (subId != null && subscriptions.containsKey(subId)) {
                    // Chuyển nội dung body cho hàm xử lý của giao diện
                    subscriptions.get(subId).accept(destination, body);
                }
                break;

            case "ERROR":
                System.err.println("STOMP Báo Lỗi: " + body);
                break;
        }
    }

    /**
     * -----------------------------------------------------------------
     * BƯỚC 3: ĐĂNG KÝ LẮNG NGHE KÊNH TIN NHẮN (SUBSCRIBE)
     * -----------------------------------------------------------------
     * @param destination Kênh đích, ví dụ "/user/queue/private" hoặc "/topic/room.1"
     * @param handler     Hàm callback nhận (kênh, nội dung_json)
     * @return Mã ID của subscription để hủy đăng ký nếu cần
     */
    public String subscribe(String destination, BiConsumer<String, String> handler) {
        String subId = "sub-" + (subscriptionCounter++);
        subscriptions.put(subId, handler);
        subscriptionDestinations.put(subId, destination);

        // Nếu WebSocket đã mở thì gửi ngay frame SUBSCRIBE sang Spring Boot
        if (connected && ws != null && ws.isOpen()) {
            sendSubscribeFrame(subId, destination);
        }

        return subId;
    }

    private void sendSubscribeFrame(String subId, String destination) {
        String frame = "SUBSCRIBE\n" +
                "id:" + subId + "\n" +
                "destination:" + destination + "\n" +
                "\n\0";
        ws.send(frame);
    }

    private void resubscribeAll() {
        for (Map.Entry<String, String> entry : subscriptionDestinations.entrySet()) {
            sendSubscribeFrame(entry.getKey(), entry.getValue());
        }
    }

    public void unsubscribe(String subId) {
        subscriptions.remove(subId);
        subscriptionDestinations.remove(subId);
        if (connected && ws != null && ws.isOpen()) {
            String frame = "UNSUBSCRIBE\n" +
                    "id:" + subId + "\n" +
                    "\n\0";
            ws.send(frame);
        }
    }

    /**
     * -----------------------------------------------------------------
     * BƯỚC 4: GỬI TIN NHẮN SANG BACKEND (SEND)
     * -----------------------------------------------------------------
     * @param destination Kênh xử lý trên Spring Controller (@MessageMapping("/chat.private"))
     * @param jsonBody    Dữ liệu JSON: {"recipient":"...","content":"..."}
     */
    public void send(String destination, String jsonBody) {
        if (!connected || ws == null || !ws.isOpen()) {
            System.err.println("Không thể gửi tin: WebSocket STOMP chưa kết nối!");
            return;
        }
        String frame = "SEND\n" +
                "destination:" + destination + "\n" +
                "content-type:application/json\n" +
                "\n" + jsonBody + "\0";
        ws.send(frame);
    }

    /**
     * Ngắt kết nối an toàn khi người dùng Đăng xuất hoặc tắt ứng dụng
     */
    public void disconnect() {
        connected = false;
        if (reconnectTask != null) reconnectTask.cancel(false);
        if (ws != null) {
            try {
                String frame = "DISCONNECT\n\n\0";
                ws.send(frame);
                ws.closeBlocking();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Tự động thử kết nối lại sau 5 giây nếu mạng bị ngắt
     */
    private void scheduleReconnect() {
        if (reconnectTask != null && !reconnectTask.isDone()) return;
        reconnectTask = scheduler.schedule(() -> {
            if (!connected && lastUrl != null) {
                System.out.println("Đang thử kết nối lại STOMP WebSocket...");
                connect(lastUrl, customHeaders);
            }
        }, 5, TimeUnit.SECONDS);
    }
}

package com.teamchat.swing.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teamchat.swing.api.ApiClient;
import com.teamchat.swing.api.StompClient;
import com.teamchat.swing.model.ChatMessage;
import com.teamchat.swing.model.ConversationItem;
import com.teamchat.swing.model.UserInfo;
import com.teamchat.swing.ui.admin.AdminFrame;
import com.teamchat.swing.ui.components.RoundedPanel;
import com.teamchat.swing.ui.dialogs.CreateGroupDialog;
import com.teamchat.swing.ui.dialogs.FriendsDialog;
import com.teamchat.swing.ui.dialogs.SettingsDialog;
import com.teamchat.swing.ui.panels.ChatPanel;
import com.teamchat.swing.ui.panels.InfoDrawerPanel;
import com.teamchat.swing.ui.panels.SidebarPanel;
import com.teamchat.swing.ui.panels.TopBarPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * =========================================================================
 * FILE: MainFrame.java - CỬA SỔ CHÍNH ĐIỀU PHỐI GIAO DIỆN & REALTIME
 * =========================================================================
 * 
 * [MỤC ĐÍCH & CHỨC NĂNG]
 * - Cửa sổ chính của ChatSpace Desktop sau khi đăng nhập thành công.
 * - Tái hiện 100% cấu trúc giao diện của bản Web (index.html):
 *     + Header trên cùng: TopBarPanel (Màu xanh đậm #16263d, logo, nút bạn bè, cài đặt, 3 chấm tròn).
 *     + Cột trái: SidebarPanel (Màu nền #f2f6fb, tìm kiếm, 4 tab lọc, danh sách hội thoại).
 *     + Khung giữa: ChatPanel (Header chat, luồng bong bóng tin nhắn, ô nhập tin nhắn).
 *     + Ngăn phải: InfoDrawerPanel (Thông tin hội thoại, thành viên nhóm, mở khi bấm nút ⋮).
 * - Kết nối WebSocket STOMP tới Backend để gửi và nhận tin nhắn thời gian thực.
 * - Đăng ký các kênh lắng nghe:
 *     + /user/queue/private: Nhận tin nhắn cá nhân từ người khác gửi đến.
 *     + /topic/room.{id}: Nhận tin nhắn từ các nhóm chat đang tham gia.
 */
public class MainFrame extends JFrame {

    // Thông tin tài khoản người dùng hiện tại
    private final UserInfo currentUser;

    // Đối tượng quản lý kết nối STOMP WebSocket
    private final StompClient stompClient = new StompClient();

    // Các thành phần giao diện chính
    private JPanel viewport;
    private RoundedPanel windowCard;
    private JPanel bodyPanel;
    private RoundedPanel chatCard;
    private TopBarPanel topBar;
    private SidebarPanel sidebar;
    private ChatPanel chatPanel;
    private InfoDrawerPanel infoDrawer;

    // Dữ liệu bộ nhớ cục bộ
    private final List<ConversationItem> conversations = new ArrayList<>();
    private final Map<Long, List<ChatMessage>> conversationMessages = new HashMap<>();

    public MainFrame(UserInfo user) {
        this.currentUser = user;
        setTitle("CHATSPACE — " + (user != null ? user.getName() : "Ứng dụng Chat"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 800);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null); // Canh giữa màn hình
        getContentPane().setBackground(UITheme.PAGE_BG);

        // 1. Dựng khung giao diện
        initUI();

        // 2. Tải dữ liệu ban đầu từ Backend qua REST API
        loadDataFromBackend();

        // 3. Kết nối WebSocket STOMP để chat thời gian thực
        connectWebSocket();
    }

    /**
     * Khởi tạo bố cục giao diện tương tự thẻ container của web
     */
    private void initUI() {
        // Lớp đệm ngoài cùng màu #e8f1f8 (viewport)
        viewport = new JPanel(new BorderLayout());
        viewport.setBackground(UITheme.PAGE_BG);
        viewport.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Khung cửa sổ trắng bo góc 16px (chatspace-window)
        windowCard = new RoundedPanel(16, new Color(255, 255, 255, 120));
        windowCard.setBackground(UITheme.WINDOW_BG);
        windowCard.setLayout(new BorderLayout());

        // 1. THANH TIÊU ĐỀ TRÊN CÙNG (TOPBAR)
        topBar = new TopBarPanel(currentUser);
        topBar.setOnFriendsClick(this::openFriendsModal);
        topBar.setOnSettingsClick(this::openSettingsModal);
        topBar.setOnAdminClick(this::openAdminPortal);
        topBar.setOnLogoutClick(this::handleLogout);
        topBar.setOnDarkModeToggle(this::applyTheme);
        windowCard.add(topBar, BorderLayout.NORTH);

        // 2. THÂN CỬA SỔ (BODY): CHIA 2 CỘT CHÍNH
        bodyPanel = new JPanel(new BorderLayout(16, 0));
        bodyPanel.setBackground(UITheme.PAGE_BG);
        bodyPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Cột trái: Danh sách hội thoại (Sidebar)
        sidebar = new SidebarPanel();
        sidebar.setOnConversationSelected(this::onConversationSelected);
        sidebar.setOnCreateGroupClick(this::openCreateGroupModal);
        sidebar.setOnFriendsClick(this::openFriendsModal);
        bodyPanel.add(sidebar, BorderLayout.WEST);

        // Khung giữa & phải: Khung chat và Ngăn thông tin
        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setOpaque(false);

        chatCard = new RoundedPanel(14, UITheme.BORDER_LIGHT);
        chatCard.setBackground(UITheme.WINDOW_BG);
        chatCard.setLayout(new BorderLayout());

        chatPanel = new ChatPanel();
        chatPanel.setOnSendMessage(this::sendMessage);
        chatPanel.setOnToggleInfoDrawer(this::toggleInfoDrawer);
        chatPanel.setOnAddMemberClick(this::openCreateGroupModal);
        chatCard.add(chatPanel, BorderLayout.CENTER);

        // Ngăn thông tin hội thoại (mặc định ẩn giống web, bấm ⋮ để hiện)
        infoDrawer = new InfoDrawerPanel();
        infoDrawer.setVisible(false);
        infoDrawer.setOnAddMemberClick(this::openCreateGroupModal);
        chatCard.add(infoDrawer, BorderLayout.EAST);

        centerWrap.add(chatCard, BorderLayout.CENTER);
        bodyPanel.add(centerWrap, BorderLayout.CENTER);

        windowCard.add(bodyPanel, BorderLayout.CENTER);
        viewport.add(windowCard, BorderLayout.CENTER);
        setContentPane(viewport);
    }

    // =====================================================================
    // TẢI DỮ LIỆU BAN ĐẦU (REST API: GET /api/chat/snapshot)
    // =====================================================================

    /**
     * Dùng SwingWorker chạy ngầm trên nền để không làm đơ giao diện
     */
    private void loadDataFromBackend() {
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                // Gọi API lấy toàn bộ snapshot dữ liệu
                return ApiClient.getSnapshot();
            }

            @Override
            protected void done() {
                try {
                    JsonObject snapshot = get();
                    parseSnapshot(snapshot);
                } catch (Exception ex) {
                    System.err.println("Lỗi tải snapshot: " + ex.getMessage());
                    conversations.clear();
                    conversationMessages.clear();
                    sidebar.setConversations(conversations);
                    chatPanel.setConversation(null);
                    infoDrawer.setConversation(null);
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Không thể tải hội thoại. Vui lòng kiểm tra kết nối và thử lại.",
                            "Lỗi tải dữ liệu", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Bóc tách JSON snapshot và đưa vào model của Sidebar và ChatPanel
     */
    private void parseSnapshot(JsonObject snapshot) {
        conversations.clear();
        conversationMessages.clear();

        if (snapshot.has("conversations") && snapshot.get("conversations").isJsonArray()) {
            JsonArray arr = snapshot.getAsJsonArray("conversations");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                ConversationItem item = new ConversationItem();
                item.setId(obj.has("id") ? obj.get("id").getAsLong() : 1);
                item.setName(obj.has("name") ? obj.get("name").getAsString() : "Hội thoại");
                item.setType(obj.has("type") ? obj.get("type").getAsString() : "PRIVATE");
                item.setLastMessage(obj.has("lastMessage") && !obj.get("lastMessage").isJsonNull() ? obj.get("lastMessage").getAsString() : "");
                item.setLastMessageTime(obj.has("lastMessageTime") && !obj.get("lastMessageTime").isJsonNull() ? obj.get("lastMessageTime").getAsString() : "");
                item.setUnreadCount(obj.has("unreadCount") ? obj.get("unreadCount").getAsInt() : 0);

                // Nạp lịch sử tin nhắn của từng hội thoại
                if (obj.has("messages") && obj.get("messages").isJsonArray()) {
                    List<ChatMessage> msgList = new ArrayList<>();
                    for (JsonElement mEl : obj.getAsJsonArray("messages")) {
                        JsonObject mObj = mEl.getAsJsonObject();
                        ChatMessage m = new ChatMessage();
                        m.setId(mObj.has("id") ? mObj.get("id").getAsLong() : 0);
                        m.setConversationId(item.getId());
                        m.setContent(mObj.has("content") ? mObj.get("content").getAsString() : "");
                        m.setSenderId(mObj.has("senderId") ? mObj.get("senderId").getAsString() : "");
                        m.setSenderName(mObj.has("senderName") ? mObj.get("senderName").getAsString() : "");
                        m.setCreatedAt(mObj.has("createdAt") ? mObj.get("createdAt").getAsString() : "");

                        // Kiểm tra tin nhắn này do chính tôi gửi hay do người khác gửi
                        boolean sentByMe = currentUser != null &&
                                (Objects.equals(m.getSenderId(), currentUser.getId()) ||
                                 Objects.equals(m.getSenderId(), currentUser.getEmail()));
                        m.setSentByMe(sentByMe);
                        msgList.add(m);
                    }
                    item.setMessages(msgList);
                    conversationMessages.put(item.getId(), msgList);
                }

                conversations.add(item);
            }
        }

        sidebar.setConversations(conversations);
        if (conversations.isEmpty()) {
            chatPanel.setConversation(null);
            infoDrawer.setConversation(null);
        } else {
            chatPanel.setConversation(conversations.get(0));
            infoDrawer.setConversation(conversations.get(0));
        }
    }

    // =====================================================================
    // KẾT NỐI & XỬ LÝ WEBSOCKET STOMP REALTIME
    // =====================================================================

    /**
     * Mở kết nối STOMP tới ws://localhost:8080/ws
     */
    private void connectWebSocket() {
        String base = ApiClient.getBaseUrl();
        // Đổi http:// thành ws:// (hoặc https:// thành wss://)
        String wsUrl = base.replaceFirst("^http", "ws") + "/ws";

        // Lấy Session Cookie để Spring WebSocket xác thực người dùng
        Map<String, String> headers = new HashMap<>();
        String cookieStr = ApiClient.getSessionCookieString();
        if (!cookieStr.isEmpty()) {
            headers.put("Cookie", cookieStr);
        }

        // Lắng nghe trạng thái kết nối để cập nhật màu chấm xanh trên TopBar
        stompClient.setConnectionListener(connected -> SwingUtilities.invokeLater(() -> {
            topBar.setWsConnected(connected);
            if (connected) {
                // Đăng ký nhận tin từ các kênh
                subscribeChannels();
            }
        }));

        stompClient.connect(wsUrl, headers);
    }

    /**
     * Đăng ký nhận tin nhắn từ các kênh STOMP
     */
    private void subscribeChannels() {
        // 1. Kênh tin nhắn riêng tư của người dùng: /user/queue/private
        stompClient.subscribe("/user/queue/private", (dest, body) -> {
            SwingUtilities.invokeLater(() -> handleIncomingMessage(body));
        });

        // 2. Kênh của các nhóm chat: /topic/room.{conversationId}
        for (ConversationItem conv : conversations) {
            if ("GROUP".equalsIgnoreCase(conv.getType())) {
                stompClient.subscribe("/topic/room." + conv.getId(), (dest, body) -> {
                    SwingUtilities.invokeLater(() -> handleIncomingMessage(body));
                });
            }
        }
    }

    /**
     * Xử lý khi có tin nhắn mới từ WebSocket đẩy về
     */
    private void handleIncomingMessage(String json) {
        try {
            JsonObject obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            String content = obj.has("content") ? obj.get("content").getAsString() : "";
            String senderName = obj.has("senderName") ? obj.get("senderName").getAsString() : "Người dùng";
            String senderId = obj.has("senderId") ? obj.get("senderId").getAsString() : "";
            long convId = obj.has("conversationId") ? obj.get("conversationId").getAsLong() : 0;

            ChatMessage msg = new ChatMessage();
            msg.setContent(content);
            msg.setSenderName(senderName);
            msg.setSenderId(senderId);
            msg.setConversationId(convId);
            msg.setCreatedAt(DateTimeFormatter.ofPattern("HH:mm").format(LocalDateTime.now()));

            boolean sentByMe = currentUser != null &&
                    (Objects.equals(senderId, currentUser.getId()) || Objects.equals(senderId, currentUser.getEmail()));
            msg.setSentByMe(sentByMe);

            // Phát âm thanh và thông báo đẩy khi có tin nhắn mới (nếu không phải do mình gửi)
            if (!sentByMe) {
                com.teamchat.swing.util.SoundService.playNotificationSound();
                com.teamchat.swing.util.NotificationService.showNotification(senderName, content);
            }

            // Cập nhật lên khung chat nếu đang mở đúng cuộc trò chuyện đó
            ConversationItem currentConv = chatPanel.getCurrentConversation();
            if (currentConv != null && (convId == 0 || currentConv.getId() == convId)) {
                chatPanel.addMessage(msg);
                currentConv.setLastMessage(content);
                currentConv.setLastMessageTime(msg.getTimeDisplay());
                sidebar.updateConversation(currentConv);
            }
        } catch (Exception ex) {
            System.err.println("Lỗi xử lý tin nhắn đến: " + ex.getMessage());
        }
    }

    /**
     * Gửi tin nhắn:
     * 1. Hiển thị ngay lên giao diện (Optimistic UI update)
     * 2. Gửi Frame SEND qua STOMP /app/chat.private tới Spring Boot Controller
     */
    private void sendMessage(String text) {
        ConversationItem conv = chatPanel.getCurrentConversation();
        if (conv == null) return;

        // 1. Thêm tin nhắn của chính mình vào giao diện trước
        ChatMessage myMsg = new ChatMessage();
        myMsg.setContent(text);
        myMsg.setSenderName(currentUser != null ? currentUser.getName() : "Tôi");
        myMsg.setSenderId(currentUser != null ? currentUser.getId() : "");
        myMsg.setConversationId(conv.getId());
        myMsg.setCreatedAt(DateTimeFormatter.ofPattern("HH:mm").format(LocalDateTime.now()));
        myMsg.setSentByMe(true);

        // Lưu vào danh sách tin nhắn của chính hội thoại này
        if (conv.getMessages() == null) {
            conv.setMessages(new ArrayList<>());
        }
        conv.getMessages().add(myMsg);

        // Lưu vào bộ nhớ cục bộ map
        List<ChatMessage> list = conversationMessages.computeIfAbsent(conv.getId(), k -> new ArrayList<>());
        list.add(myMsg);

        // Hiển thị trực tiếp lên màn hình chat
        chatPanel.addMessage(myMsg);

        // Cập nhật dòng tóm tắt ở cột hội thoại bên trái
        conv.setLastMessage(text);
        conv.setLastMessageTime(myMsg.getTimeDisplay());
        sidebar.updateConversation(conv);

        // 2. Gửi qua WebSocket STOMP
        JsonObject payload = new JsonObject();
        payload.addProperty("recipient", conv.getName());
        payload.addProperty("content", text);
        payload.addProperty("conversationId", conv.getId());
        stompClient.send("/app/chat.private", payload.toString());

        // 3. Lưu lịch sử tin nhắn vào database qua REST API
        new Thread(() -> {
            try {
                String msgUuid = UUID.randomUUID().toString();
                Map<String, Object> syncMsg = new HashMap<>();
                syncMsg.put("id", msgUuid);
                syncMsg.put("senderId", currentUser != null ? currentUser.getId() : "1");
                syncMsg.put("content", text);

                Map<String, Object> syncConv = new HashMap<>();
                syncConv.put("id", String.valueOf(conv.getId()));
                syncConv.put("type", conv.getType());
                syncConv.put("messages", List.of(syncMsg));

                Map<String, Object> state = new HashMap<>();
                state.put("conversations", List.of(syncConv));
                ApiClient.syncState(state);
            } catch (Exception ignored) {}
        }, "MessageSyncThread").start();
    }

    // =====================================================================
    // ĐIỀU HƯỚNG SỰ KIỆN & CỬA SỔ HỘP THOẠI (DIALOGS)
    // =====================================================================

    private void onConversationSelected(ConversationItem conv) {
        if (conv == null) return;
        ConversationItem current = chatPanel.getCurrentConversation();
        if (current != null && current.getId() == conv.getId()) {
            return; // Đang mở đúng hội thoại này, không tải lại để tránh xóa mất tin nhắn vừa gửi
        }
        chatPanel.setConversation(conv);
        infoDrawer.setConversation(conv);
    }

    private void toggleInfoDrawer() {
        boolean visible = !infoDrawer.isVisible();
        infoDrawer.setVisible(visible);
        revalidate();
        repaint();
    }

    private void openCreateGroupModal() {
        CreateGroupDialog dlg = new CreateGroupDialog(this);
        dlg.setOnGroupCreated(groupJson -> {
            loadDataFromBackend(); // Tải lại danh sách sau khi tạo nhóm
        });
        dlg.setVisible(true);
    }

    private void openFriendsModal() {
        FriendsDialog dlg = new FriendsDialog(this);
        dlg.setOnStartChatWithUser(userId -> {
            loadDataFromBackend(); // Tải lại danh sách khi bắt đầu nhắn tin với người mới
        });
        dlg.setVisible(true);
    }

    private void openSettingsModal() {
        SettingsDialog dlg = new SettingsDialog(this, currentUser);
        dlg.setOnLogout(this::handleLogout);
        dlg.setOnAvatarChanged(newAvatarUrl -> {
            if (currentUser != null) {
                currentUser.setAvatar(newAvatarUrl);
            }
            if (topBar != null) {
                topBar.updateUserAvatar(newAvatarUrl);
            }
        });
        dlg.setVisible(true);
    }

    /**
     * Cập nhật toàn bộ giao diện theo chế độ Sáng / Tối (Dark / Light Mode)
     */
    public void applyTheme() {
        getContentPane().setBackground(UITheme.PAGE_BG);
        if (viewport != null) viewport.setBackground(UITheme.PAGE_BG);
        if (windowCard != null) {
            windowCard.setBackground(UITheme.WINDOW_BG);
            windowCard.setBorderColor(UITheme.BORDER_LIGHT);
        }
        if (bodyPanel != null) bodyPanel.setBackground(UITheme.PAGE_BG);
        if (chatCard != null) {
            chatCard.setBackground(UITheme.WINDOW_BG);
            chatCard.setBorderColor(UITheme.BORDER_LIGHT);
        }
        if (topBar != null) topBar.setBackground(UITheme.TOPBAR_BG);

        if (sidebar != null) sidebar.applyTheme();
        if (chatPanel != null) chatPanel.applyTheme();
        if (infoDrawer != null) infoDrawer.applyTheme();

        SwingUtilities.updateComponentTreeUI(this);
        repaint();
    }

    private void openAdminPortal() {
        SwingUtilities.invokeLater(() -> {
            AdminFrame adminFrame = new AdminFrame();
            adminFrame.setVisible(true);
        });
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đăng xuất khỏi ChatSpace?", "Đăng xuất", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try { ApiClient.logout(); } catch (Exception ignored) {}
            }).start();

            // Ngắt kết nối WebSocket
            stompClient.disconnect();
            dispose();

            // Mở lại màn hình đăng nhập
            SwingUtilities.invokeLater(() -> {
                AuthFrame auth = new AuthFrame();
                auth.setVisible(true);
            });
        }
    }
}

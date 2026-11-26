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
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
 * - Gửi tin nhắn qua API lưu trữ và nhận cập nhật qua WebSocket STOMP.
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
    private FriendsDialog friendsDialog;

    private long snapshotVersion;
    private long directChatVersion;
    private final Map<String, ReadStatusUpdate> pendingReadUpdates = new HashMap<>();
    private final Set<String> pendingConversationPreferences = new HashSet<>();
    private boolean loadedSnapshot;
    private String conversationToSelect;

    private record ReadStatusUpdate(boolean unread, String readThrough) {}

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
        sidebar.setOnConversationOpened(this::markConversationRead);
        sidebar.setOnReadStatusToggle(this::toggleReadStatus);
        sidebar.setOnPinToggle(this::togglePin);
        sidebar.setOnMuteToggle(this::toggleMute);
        sidebar.setOnArchiveToggle(this::toggleArchive);
        sidebar.setOnDeleteConversation(this::deleteConversation);
        sidebar.setOnBlockToggle(this::toggleBlock);
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
        chatPanel.setOnAddMemberClick(this::openAddMembersModal);
        chatCard.add(chatPanel, BorderLayout.CENTER);

        // Ngăn thông tin hội thoại (mặc định ẩn giống web, bấm ⋮ để hiện)
        infoDrawer = new InfoDrawerPanel();
        infoDrawer.setVisible(false);
        infoDrawer.setOnPinToggle(this::togglePin);
        infoDrawer.setOnMuteToggle(this::toggleMute);
        infoDrawer.setOnArchiveToggle(this::toggleArchive);
        infoDrawer.setOnRenameConversation(this::renameConversation);
        infoDrawer.setOnDeleteConversation(this::deleteConversation);
        infoDrawer.setOnBlockToggle(this::toggleBlock);
        infoDrawer.setOnAddMemberClick(this::openAddMembersModal);
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
        long version = ++snapshotVersion;
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                // Gọi API lấy toàn bộ snapshot dữ liệu
                return ApiClient.getSnapshot();
            }

            @Override
            protected void done() {
                if (version != snapshotVersion || !isDisplayable()) return;
                try {
                    List<ConversationItem> conversations = parseSnapshot(get());
                    sidebar.setConversations(conversations);
                    if (conversationToSelect != null) {
                        sidebar.selectConversation(conversationToSelect);
                        conversationToSelect = null;
                    }
                    onConversationSelected(sidebar.getSelectedConversation());
                    loadedSnapshot = true;
                } catch (Exception ex) {
                    System.err.println("Lỗi tải snapshot: " + ex.getMessage());
                    if (!loadedSnapshot) {
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Không thể tải hội thoại. Vui lòng kiểm tra kết nối và thử lại.",
                                "Lỗi tải dữ liệu", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    /**
     * Bóc tách JSON snapshot và đưa vào model của Sidebar và ChatPanel
     */
    private List<ConversationItem> parseSnapshot(JsonObject snapshot) {
        List<ConversationItem> conversations = new ArrayList<>();
        for (JsonElement element : array(snapshot, "conversations")) {
            JsonObject data = element.getAsJsonObject();
            String id = value(data, "id", "");
            if (id.isBlank()) continue;

            ConversationItem conversation = new ConversationItem();
            conversation.setId(id);
            conversation.setType(value(data, "type", "DIRECT"));
            String nickname = conversation.isDirect() ? value(data, "nickname", "") : "";
            conversation.setName(nickname.isBlank() ? value(data, "name", "Hội thoại") : nickname);
            conversation.setAvatarText(value(data, "avatarText", ""));
            conversation.setAvatarUrl(value(data, "avatar", value(data, "avatarUrl", "")));
            conversation.setUnreadCount(Integer.parseInt(value(data, "unreadCount", "0")));
            conversation.setPinned(Boolean.parseBoolean(value(data, "pinned", "false")));
            conversation.setMuted(Boolean.parseBoolean(value(data, "muted", "false")));
            conversation.setArchived(Boolean.parseBoolean(value(data, "archived", "false")));
            conversation.setBlocked(Boolean.parseBoolean(value(data, "blocked", "false")));
            conversation.setSendBlocked(Boolean.parseBoolean(value(data, "sendBlocked", "false")));
            conversation.setLegacyReadOnly(Boolean.parseBoolean(value(data, "legacyReadOnly", "false")));

            for (JsonElement memberElement : array(data, "members")) {
                JsonObject memberData = memberElement.getAsJsonObject();
                ConversationItem.MemberInfo member = new ConversationItem.MemberInfo();
                member.setId(value(memberData, "id", ""));
                member.setName(value(memberData, "name", ""));
                member.setRole(value(memberData, "role", ""));
                member.setAvatarUrl(value(memberData, "avatar", value(memberData, "avatarUrl", "")));
                conversation.getMembers().add(member);
            }
            List<ChatMessage> messages = conversation.getMessages();
            for (JsonElement messageElement : array(data, "messages")) {
                JsonObject messageData = messageElement.getAsJsonObject();
                ChatMessage message = new ChatMessage();
                message.setId(value(messageData, "id", ""));
                message.setConversationId(id);
                message.setContent(value(messageData, "content", ""));
                message.setSenderId(value(messageData, "senderId", ""));
                message.setSenderName(value(messageData, "senderName", ""));
                message.setCreatedAt(value(messageData, "timestamp", value(messageData, "createdAt", "")));
                message.setSentByMe(isCurrentUser(message.getSenderId()));
                messages.add(message);
            }
            ChatMessage latest = messages.isEmpty() ? null : messages.getLast();
            ReadStatusUpdate pendingRead = pendingReadUpdates.get(id);
            if (pendingRead != null) {
                if (pendingRead.unread()) {
                    conversation.setUnreadCount(Math.max(1, conversation.getUnreadCount()));
                } else if (pendingRead.readThrough().equals(latest == null ? "" : latest.getId())) {
                    conversation.setUnreadCount(0);
                }
            }
            conversation.setLastMessage(value(data, "lastMessage", latest == null ? "" : latest.getContent()));
            conversation.setLastMessageTime(value(data, "lastMessageTime", latest == null ? "" : latest.getTimeDisplay()));
            conversations.add(conversation);
        }
        return conversations;
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    private static String value(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private boolean isCurrentUser(String senderId) {
        return currentUser != null && (Objects.equals(senderId, currentUser.getId())
                || Objects.equals(senderId, currentUser.getEmail()));
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
        stompClient.subscribe("/user/queue/messages", (destination, body) ->
                SwingUtilities.invokeLater(() -> handleIncomingMessage(body)));
        stompClient.subscribe("/user/queue/friends", (destination, body) ->
                SwingUtilities.invokeLater(this::refreshOpenFriendsDialog));
        stompClient.setConnectionListener(connected -> SwingUtilities.invokeLater(() -> {
            topBar.setWsConnected(connected);
            if (connected) {
                if (loadedSnapshot) loadDataFromBackend();
                refreshOpenFriendsDialog();
            }
        }));

        stompClient.connect(wsUrl, headers);
    }

    private void handleIncomingMessage(String json) {
        if (!isDisplayable()) return;
        try {
            JsonObject event = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            if (!"CHAT_UPDATED".equals(value(event, "type", ""))) return;
            loadDataFromBackend();
            if (Boolean.parseBoolean(value(event, "muted", "false"))) return;
            if (event.has("message") && event.get("message").isJsonObject()) {
                JsonObject message = event.getAsJsonObject("message");
                String sender = value(message, "senderId", "");
                if (!sender.isEmpty() && !isCurrentUser(sender)) {
                    com.teamchat.swing.util.SoundService.playNotificationSound();
                    com.teamchat.swing.util.NotificationService.showNotification(
                            value(message, "senderName", "Người dùng"), value(message, "content", ""));
                }
            }
        } catch (Exception ex) {
            System.err.println("Lỗi xử lý tin nhắn đến: " + ex.getMessage());
        }
    }

    private void sendMessage(String text) {
        ConversationItem conv = chatPanel.getCurrentConversation();
        if (conv == null || conv.isSendBlocked()) return;
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String id = conv.isLegacyReadOnly() ? ApiClient.continueConversation(conv.getId()) : conv.getId();
                Map<String, Object> message = Map.of("id", UUID.randomUUID().toString(), "senderId", currentUser.getId(),
                        "content", text);
                ApiClient.syncState(Map.of("conversations", List.of(Map.of(
                        "id", id, "type", conv.getType(), "messages", List.of(message)))));
                return id;
            }

            @Override
            protected void done() {
                if (!isDisplayable()) return;
                try {
                    String id = get();
                    ConversationItem selected = chatPanel.getCurrentConversation();
                    if (conv.isLegacyReadOnly() && selected != null && Objects.equals(conv.getId(), selected.getId())) {
                        conversationToSelect = id;
                    }
                    loadDataFromBackend();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    chatPanel.restoreDraftIfEmpty(conv.getId(), text);
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Không thể gửi tin nhắn. " + ex.getCause().getMessage(),
                            "Lỗi gửi tin nhắn", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // =====================================================================
    // ĐIỀU HƯỚNG SỰ KIỆN & CỬA SỔ HỘP THOẠI (DIALOGS)
    // =====================================================================

    private void onConversationSelected(ConversationItem conv) {
        if (chatPanel.getCurrentConversation() == conv) return;
        chatPanel.setConversation(conv);
        infoDrawer.setConversation(conv);
    }

    private void togglePin(ConversationItem conversation) {
        saveConversationPreference(conversation, "pinned", !conversation.isPinned());
    }

    private void toggleMute(ConversationItem conversation) {
        saveConversationPreference(conversation, "muted", !conversation.isMuted());
    }

    private void toggleArchive(ConversationItem conversation) {
        saveConversationPreference(conversation, "archived", !conversation.isArchived());
    }

    private void renameConversation(ConversationItem conversation) {
        if (!conversation.isDirect() && !isGroupOwner(conversation)) {
            JOptionPane.showMessageDialog(this, "Chỉ trưởng nhóm mới có thể đổi tên nhóm.",
                    "Đổi tên hội thoại", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String newName = JOptionPane.showInputDialog(this, "Nhập tên mới cho hội thoại:", conversation.getName());
        if (newName == null) return;

        newName = newName.strip();
        if (newName.isEmpty() || newName.equals(conversation.getName())) return;
        if (newName.length() > 150) {
            JOptionPane.showMessageDialog(this, "Tên hội thoại dài tối đa 150 ký tự.",
                    "Đổi tên hội thoại", JOptionPane.WARNING_MESSAGE);
            return;
        }
        saveConversationPreference(conversation, conversation.isDirect() ? "nickname" : "name", newName);
    }

    private void deleteConversation(ConversationItem conversation) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xóa hội thoại '" + conversation.getName() + "' khỏi danh sách của bạn?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            saveConversationPreference(conversation, "hidden", true);
        }
    }

    private void toggleBlock(ConversationItem conversation) {
        if (conversation.isDirect()) {
            saveConversationPreference(conversation, "blocked", !conversation.isBlocked());
        }
    }

    private void saveConversationPreference(ConversationItem conversation, String field, Object value) {
        String request = conversation.getId() + "/" + field;
        if (!pendingConversationPreferences.add(request)) return;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ApiClient.syncState(Map.of("conversations", List.of(Map.of(
                        "id", conversation.getId(), "fields", Map.of(field, value)))));
                return null;
            }

            @Override
            protected void done() {
                pendingConversationPreferences.remove(request);
                if (!isDisplayable()) return;
                try {
                    get();
                    loadDataFromBackend();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, "Không lưu được thay đổi. " + ex.getCause().getMessage(),
                            "Lỗi cập nhật hội thoại", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private void markConversationRead(ConversationItem conversation) {
        if (conversation != null && conversation.getUnreadCount() > 0) {
            saveReadStatus(conversation, false);
        }
    }

    private void toggleReadStatus(ConversationItem conversation) {
        saveReadStatus(conversation, conversation.getUnreadCount() == 0);
    }

    private void saveReadStatus(ConversationItem conversation, boolean unread) {
        if (conversation.isLegacyReadOnly()) return;

        String conversationId = conversation.getId();
        List<ChatMessage> messages = conversation.getMessages();
        String readThrough = messages.isEmpty() ? "" : messages.getLast().getId();
        var update = new ReadStatusUpdate(unread, readThrough);
        if (pendingReadUpdates.putIfAbsent(conversationId, update) != null) return;

        Map<String, Object> fields = unread ? Map.of("markedUnread", true)
                : Map.of("unreadCount", 0, "readThrough", readThrough);
        conversation.setUnreadCount(unread ? 1 : 0);
        sidebar.repaint();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ApiClient.syncState(Map.of("conversations", List.of(Map.of(
                        "id", conversationId, "fields", fields))));
                return null;
            }

            @Override
            protected void done() {
                pendingReadUpdates.remove(conversationId, update);
                if (!isDisplayable()) return;
                loadDataFromBackend();
                try {
                    get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, "Không lưu được trạng thái đọc của hội thoại.",
                            "Lỗi cập nhật hội thoại", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private void toggleInfoDrawer() {
        boolean visible = !infoDrawer.isVisible();
        infoDrawer.setVisible(visible);
        revalidate();
        repaint();
    }

    private void openCreateGroupModal() {
        openGroupDialog(null);
    }

    private void openAddMembersModal() {
        ConversationItem group = chatPanel.getCurrentConversation();
        if (group == null || !"GROUP".equals(group.getType())) return;

        if (!isGroupOwner(group)) {
            JOptionPane.showMessageDialog(this, "Chỉ trưởng nhóm mới có thể thêm thành viên.",
                    "Thêm thành viên", JOptionPane.WARNING_MESSAGE);
            return;
        }
        openGroupDialog(group);
    }

    private boolean isGroupOwner(ConversationItem group) {
        return group.getMembers().stream().anyMatch(member ->
                Objects.equals(member.getId(), currentUser.getId()) && "Trưởng nhóm".equals(member.getRole()));
    }

    private void openGroupDialog(ConversationItem group) {
        CreateGroupDialog dialog = new CreateGroupDialog(this, group);
        dialog.setOnGroupSaved(groupId -> {
            conversationToSelect = groupId;
            loadDataFromBackend();
        });
        dialog.setVisible(true);
    }

    private void openFriendsModal() {
        FriendsDialog dlg = new FriendsDialog(this, currentUser.getId());
        dlg.setOnStartChatWithUser(this::openDirectChat);
        friendsDialog = dlg;
        try {
            dlg.setVisible(true);
        } finally {
            friendsDialog = null;
            dlg.dispose();
        }
    }

    private void openDirectChat(String friendId) {
        long version = ++directChatVersion;
        String existingId = sidebar.findDirectConversationId(friendId);
        if (existingId != null) {
            conversationToSelect = null;
            sidebar.selectConversation(existingId);
            markConversationRead(sidebar.getSelectedConversation());
            return;
        }

        List<String> participants = List.of(currentUser.getId(), friendId).stream().sorted().toList();
        String conversationId = "conv-" + String.join("-", participants);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ApiClient.syncState(Map.of("conversations", List.of(Map.of(
                        "id", conversationId, "type", "DIRECT", "participants", participants,
                        "fields", Map.of("hidden", false)))));
                return null;
            }

            @Override
            protected void done() {
                if (!isDisplayable() || version != directChatVersion) return;
                try {
                    get();
                    conversationToSelect = conversationId;
                    loadDataFromBackend();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Không thể mở hội thoại. " + ex.getCause().getMessage(),
                            "Lỗi mở hội thoại", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshOpenFriendsDialog() {
        if (friendsDialog != null && friendsDialog.isDisplayable()) {
            friendsDialog.refreshFriendships();
        }
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

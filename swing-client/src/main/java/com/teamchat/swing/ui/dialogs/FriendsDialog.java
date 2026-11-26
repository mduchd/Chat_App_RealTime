package com.teamchat.swing.ui.dialogs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teamchat.swing.api.ApiClient;
import com.teamchat.swing.ui.UITheme;
import com.teamchat.swing.ui.components.AvatarCircle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Friends & Contact Management modal matching ChatSpace web design.
 * 3 tabs: Discover, Requests, Friends list.
 */
public class FriendsDialog extends JDialog {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentCards = new JPanel(cardLayout);
    private final String currentUserId;
    private long friendshipsVersion;

    // Tab buttons
    private JButton btnTabDiscover;
    private JButton btnTabRequests;
    private JButton btnTabFriends;

    // Discover panel
    private JTextField searchInput;
    private JPanel discoverResultsPanel;

    // Requests panel
    private JPanel requestsListPanel;

    // Friends panel
    private JPanel friendsListPanel;

    private Consumer<String> onStartChatWithUser;

    public FriendsDialog(Frame parent, String currentUserId) {
        super(parent, "Kết bạn & Danh bạ tài khoản", true);
        this.currentUserId = currentUserId;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(480, 560);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setBackground(UITheme.WINDOW_BG);
        main.setBorder(new EmptyBorder(16, 18, 16, 18));

        // 1. Tab buttons row
        JPanel tabRow = new JPanel(new GridLayout(1, 3, 6, 0));
        tabRow.setOpaque(false);
        tabRow.setPreferredSize(new Dimension(440, 34));

        btnTabDiscover = createTabBtn("Tìm tài khoản", true);
        btnTabRequests = createTabBtn("Lời mời", false);
        btnTabFriends = createTabBtn("Bạn bè", false);

        btnTabDiscover.addActionListener(e -> switchTab("discover"));
        btnTabRequests.addActionListener(e -> switchTab("requests"));
        btnTabFriends.addActionListener(e -> switchTab("friends"));

        tabRow.add(btnTabDiscover);
        tabRow.add(btnTabRequests);
        tabRow.add(btnTabFriends);
        main.add(tabRow, BorderLayout.NORTH);

        // 2. Card Content
        contentCards.setOpaque(false);
        contentCards.add(createDiscoverView(), "discover");
        contentCards.add(createRequestsView(), "requests");
        contentCards.add(createFriendsView(), "friends");

        main.add(contentCards, BorderLayout.CENTER);
        setContentPane(main);

        loadDiscoverUsers("");
        refreshFriendships();
    }

    private JButton createTabBtn(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(UITheme.FONT_SUBHEADING);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        updateTabBtnStyle(btn, active);
        return btn;
    }

    private void updateTabBtnStyle(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(UITheme.THEME_PRIMARY);
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        } else {
            btn.setBackground(UITheme.SIDEBAR_CARD_BG);
            btn.setForeground(UITheme.TEXT_MUTED);
            btn.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        }
    }

    private void switchTab(String tab) {
        cardLayout.show(contentCards, tab);
        updateTabBtnStyle(btnTabDiscover, "discover".equals(tab));
        updateTabBtnStyle(btnTabRequests, "requests".equals(tab));
        updateTabBtnStyle(btnTabFriends, "friends".equals(tab));
    }

    // ==================== DISCOVER VIEW ====================
    private JPanel createDiscoverView() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        // Search bar
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setOpaque(false);

        searchInput = new JTextField();
        searchInput.setFont(UITheme.FONT_REGULAR);
        searchInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        JButton btnSearch = new JButton("Tìm");
        btnSearch.setFont(UITheme.FONT_HEADING);
        btnSearch.setBackground(UITheme.THEME_PRIMARY);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> loadDiscoverUsers(searchInput.getText().trim()));

        searchBar.add(searchInput, BorderLayout.CENTER);
        searchBar.add(btnSearch, BorderLayout.EAST);
        panel.add(searchBar, BorderLayout.NORTH);

        // Results list
        discoverResultsPanel = new JPanel();
        discoverResultsPanel.setLayout(new BoxLayout(discoverResultsPanel, BoxLayout.Y_AXIS));
        discoverResultsPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(discoverResultsPanel);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ==================== REQUESTS VIEW ====================
    private JPanel createRequestsView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        requestsListPanel = new JPanel();
        requestsListPanel.setLayout(new BoxLayout(requestsListPanel, BoxLayout.Y_AXIS));
        requestsListPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(requestsListPanel);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ==================== FRIENDS VIEW ====================
    private JPanel createFriendsView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        friendsListPanel = new JPanel();
        friendsListPanel.setLayout(new BoxLayout(friendsListPanel, BoxLayout.Y_AXIS));
        friendsListPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(friendsListPanel);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ==================== DATA LOADING ====================

    private void loadDiscoverUsers(String query) {
        discoverResultsPanel.removeAll();
        discoverResultsPanel.add(new JLabel("Đang tìm kiếm..."));
        discoverResultsPanel.revalidate();
        discoverResultsPanel.repaint();

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ApiClient.discoverUsers(query);
            }

            @Override
            protected void done() {
                discoverResultsPanel.removeAll();
                try {
                    String json = get();
                    JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                    if (arr.isEmpty()) {
                        discoverResultsPanel.add(new JLabel("Không tìm thấy người dùng phù hợp."));
                    } else {
                        for (JsonElement el : arr) {
                            JsonObject u = el.getAsJsonObject();
                            String id = u.get("id").getAsString();
                            String name = u.get("name").getAsString();
                            String email = u.has("email") && !u.get("email").isJsonNull() ? u.get("email").getAsString() : "";
                            discoverResultsPanel.add(createDiscoverUserRow(id, name, email));
                        }
                    }
                } catch (Exception ex) {
                    discoverResultsPanel.add(new JLabel("Lỗi tải danh sách: " + ex.getMessage()));
                }
                discoverResultsPanel.revalidate();
                discoverResultsPanel.repaint();
            }
        }.execute();
    }

    private JPanel createDiscoverUserRow(String id, String name, String email) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(new EmptyBorder(8, 10, 8, 10));
        row.setMaximumSize(new Dimension(440, 50));

        AvatarCircle av = new AvatarCircle(name, 36);
        row.add(av, BorderLayout.WEST);

        JPanel info = new JPanel(new GridLayout(2, 1));
        info.setOpaque(false);
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(UITheme.FONT_SUBHEADING);
        JLabel emailLbl = new JLabel(email);
        emailLbl.setFont(UITheme.FONT_XS);
        emailLbl.setForeground(UITheme.TEXT_MUTED);
        info.add(nameLbl);
        info.add(emailLbl);
        row.add(info, BorderLayout.CENTER);

        JButton btnAdd = new JButton("Kết bạn");
        btnAdd.setFont(UITheme.FONT_MEDIUM);
        btnAdd.setBackground(UITheme.THEME_PRIMARY);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdd.addActionListener(e -> {
            btnAdd.setEnabled(false);
            btnAdd.setText("Đang gửi…");
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    ApiClient.sendFriendRequest(id);
                    return null;
                }

                @Override
                protected void done() {
                    if (!isDisplayable()) return;
                    try {
                        get();
                        btnAdd.setText("Đã gửi");
                        refreshFriendships();
                    } catch (ExecutionException ex) {
                        btnAdd.setEnabled(true);
                        btnAdd.setText("Kết bạn");
                        JOptionPane.showMessageDialog(FriendsDialog.this, "Không gửi được lời mời kết bạn.",
                                "Kết bạn", JOptionPane.WARNING_MESSAGE);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }.execute();
        });
        row.add(btnAdd, BorderLayout.EAST);

        return row;
    }

    public void refreshFriendships() {
        long version = ++friendshipsVersion;
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                return JsonParser.parseString(ApiClient.getFriends()).getAsJsonObject();
            }

            @Override
            protected void done() {
                if (version != friendshipsVersion || !isDisplayable()) return;
                try {
                    renderFriendships(get());
                } catch (ExecutionException ex) {
                    requestsListPanel.removeAll();
                    friendsListPanel.removeAll();
                    requestsListPanel.add(new JLabel("Không tải được lời mời kết bạn."));
                    friendsListPanel.add(new JLabel("Không tải được danh sách bạn bè."));
                    repaintFriendshipLists();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void renderFriendships(JsonObject data) {
        Map<String, JsonObject> contacts = new HashMap<>();
        for (JsonElement element : data.getAsJsonArray("contacts")) {
            JsonObject contact = element.getAsJsonObject();
            contacts.put(contact.get("id").getAsString(), contact);
        }

        requestsListPanel.removeAll();
        JsonArray requests = data.getAsJsonArray("requests");
        for (JsonElement element : requests) {
            JsonObject request = element.getAsJsonObject();
            boolean incoming = currentUserId.equals(request.get("toId").getAsString());
            String partnerId = request.get(incoming ? "fromId" : "toId").getAsString();
            JsonObject partner = contacts.get(partnerId);
            String name = partner == null ? partnerId : partner.get("name").getAsString();
            requestsListPanel.add(createRequestRow(request.get("id").getAsLong(), name, incoming));
        }
        btnTabRequests.setText(requests.isEmpty() ? "Lời mời" : "Lời mời (" + requests.size() + ")");
        if (requests.isEmpty()) requestsListPanel.add(new JLabel("Không có lời mời kết bạn nào."));

        Set<String> accepted = new HashSet<>();
        for (JsonElement element : data.getAsJsonArray("accepted")) {
            for (JsonElement member : element.getAsJsonArray()) {
                String id = member.getAsString();
                if (!currentUserId.equals(id)) accepted.add(id);
            }
        }
        friendsListPanel.removeAll();
        for (JsonElement element : data.getAsJsonArray("contacts")) {
            JsonObject contact = element.getAsJsonObject();
            String id = contact.get("id").getAsString();
            if (accepted.contains(id)) {
                String email = contact.has("email") && !contact.get("email").isJsonNull() ? contact.get("email").getAsString() : "";
                friendsListPanel.add(createFriendRow(id, contact.get("name").getAsString(), email));
            }
        }
        btnTabFriends.setText("Bạn bè (" + friendsListPanel.getComponentCount() + ")");
        if (friendsListPanel.getComponentCount() == 0) friendsListPanel.add(new JLabel("Chưa có bạn bè nào."));
        repaintFriendshipLists();
    }

    private void repaintFriendshipLists() {
        requestsListPanel.revalidate();
        requestsListPanel.repaint();
        friendsListPanel.revalidate();
        friendsListPanel.repaint();
    }

    private JPanel createRequestRow(long reqId, String name, boolean incoming) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(new EmptyBorder(8, 10, 8, 10));
        row.setMaximumSize(new Dimension(440, 48));

        AvatarCircle av = new AvatarCircle(name, 32);
        row.add(av, BorderLayout.WEST);

        JLabel lbl = new JLabel((incoming ? "Lời mời từ: " : "Đã gửi tới: ") + name);
        lbl.setFont(UITheme.FONT_SUBHEADING);
        row.add(lbl, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btns.setOpaque(false);

        JButton btnReject = new JButton(incoming ? "Từ chối" : "Hủy");
        btnReject.setFont(UITheme.FONT_MEDIUM);
        if (incoming) {
            JButton btnAccept = new JButton("Chấp nhận");
            btnAccept.setFont(UITheme.FONT_MEDIUM);
            btnAccept.setBackground(UITheme.THEME_PRIMARY);
            btnAccept.setForeground(Color.WHITE);
            btnAccept.addActionListener(e -> updateRequest(reqId, true, btnAccept, btnReject));
            btnReject.addActionListener(e -> updateRequest(reqId, false, btnAccept, btnReject));
            btns.add(btnAccept);
        } else {
            btnReject.addActionListener(e -> updateRequest(reqId, false, btnReject));
        }
        btns.add(btnReject);
        row.add(btns, BorderLayout.EAST);
        return row;
    }

    private void updateRequest(long reqId, boolean accept, JButton... buttons) {
        for (JButton button : buttons) button.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (accept) {
                    ApiClient.acceptFriendRequest(reqId);
                } else {
                    ApiClient.cancelFriendRequest(reqId);
                }
                return null;
            }

            @Override
            protected void done() {
                if (!isDisplayable()) return;
                try {
                    get();
                } catch (ExecutionException ex) {
                    JOptionPane.showMessageDialog(FriendsDialog.this, "Không cập nhật được lời mời kết bạn.",
                            "Kết bạn", JOptionPane.WARNING_MESSAGE);
                    for (JButton button : buttons) button.setEnabled(true);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                refreshFriendships();
            }
        }.execute();
    }

    private JPanel createFriendRow(String id, String name, String email) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(new EmptyBorder(8, 10, 8, 10));
        row.setMaximumSize(new Dimension(440, 50));

        AvatarCircle av = new AvatarCircle(name, 36);
        row.add(av, BorderLayout.WEST);

        JPanel info = new JPanel(new GridLayout(2, 1));
        info.setOpaque(false);
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(UITheme.FONT_SUBHEADING);
        JLabel emailLbl = new JLabel(email);
        emailLbl.setFont(UITheme.FONT_SMALL);
        emailLbl.setForeground(UITheme.TEXT_MUTED);
        info.add(nameLbl);
        info.add(emailLbl);
        row.add(info, BorderLayout.CENTER);

        JButton btnChat = new JButton("Nhắn tin");
        btnChat.setFont(UITheme.FONT_MEDIUM);
        btnChat.setBackground(UITheme.SIDEBAR_CARD_BG);
        btnChat.setForeground(UITheme.THEME_PRIMARY);
        btnChat.addActionListener(e -> {
            dispose();
            if (onStartChatWithUser != null) onStartChatWithUser.accept(id);
        });
        row.add(btnChat, BorderLayout.EAST);

        return row;
    }

    public void setOnStartChatWithUser(Consumer<String> callback) { this.onStartChatWithUser = callback; }
}

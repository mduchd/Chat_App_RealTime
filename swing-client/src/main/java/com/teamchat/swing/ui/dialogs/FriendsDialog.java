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
import java.util.function.Consumer;

/**
 * Friends & Contact Management modal matching ChatSpace web design.
 * 3 tabs: Discover, Requests, Friends list.
 */
public class FriendsDialog extends JDialog {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentCards = new JPanel(cardLayout);

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

    public FriendsDialog(Frame parent) {
        super(parent, "Kết bạn & Danh bạ tài khoản", true);
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
        loadFriendships();
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
            btnAdd.setText("Đã gửi");
            new Thread(() -> {
                try { ApiClient.sendFriendRequest(id); } catch (Exception ignored) {}
            }).start();
        });
        row.add(btnAdd, BorderLayout.EAST);

        return row;
    }

    private void loadFriendships() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ApiClient.getFriends();
            }

            @Override
            protected void done() {
                try {
                    String json = get();
                    JsonObject root = JsonParser.parseString(json).getAsJsonObject();

                    // Render Requests
                    requestsListPanel.removeAll();
                    JsonArray reqs = root.getAsJsonArray("requests");
                    if (reqs != null && !reqs.isEmpty()) {
                        btnTabRequests.setText("Lời mời (" + reqs.size() + ")");
                        for (JsonElement el : reqs) {
                            JsonObject r = el.getAsJsonObject();
                            long reqId = r.get("id").getAsLong();
                            String fromId = r.get("fromId").getAsString();
                            requestsListPanel.add(createRequestRow(reqId, fromId));
                        }
                    } else {
                        btnTabRequests.setText("Lời mời");
                        requestsListPanel.add(new JLabel("Không có lời mời kết bạn nào."));
                    }
                    requestsListPanel.revalidate();
                    requestsListPanel.repaint();

                    // Render Contacts/Friends
                    friendsListPanel.removeAll();
                    JsonArray contacts = root.getAsJsonArray("contacts");
                    if (contacts != null && !contacts.isEmpty()) {
                        btnTabFriends.setText("Bạn bè (" + contacts.size() + ")");
                        for (JsonElement el : contacts) {
                            JsonObject c = el.getAsJsonObject();
                            String cId = c.get("id").getAsString();
                            String cName = c.get("name").getAsString();
                            String cEmail = c.has("email") && !c.get("email").isJsonNull() ? c.get("email").getAsString() : "";
                            friendsListPanel.add(createFriendRow(cId, cName, cEmail));
                        }
                    } else {
                        btnTabFriends.setText("Bạn bè (0)");
                        friendsListPanel.add(new JLabel("Chưa có bạn bè nào."));
                    }
                    friendsListPanel.revalidate();
                    friendsListPanel.repaint();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private JPanel createRequestRow(long reqId, String fromId) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(new EmptyBorder(8, 10, 8, 10));
        row.setMaximumSize(new Dimension(440, 48));

        AvatarCircle av = new AvatarCircle(fromId, 32);
        row.add(av, BorderLayout.WEST);

        JLabel lbl = new JLabel("Lời mời từ: " + fromId);
        lbl.setFont(UITheme.FONT_SUBHEADING);
        row.add(lbl, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btns.setOpaque(false);

        JButton btnAccept = new JButton("Chấp nhận");
        btnAccept.setFont(UITheme.FONT_MEDIUM);
        btnAccept.setBackground(UITheme.THEME_PRIMARY);
        btnAccept.setForeground(Color.WHITE);
        btnAccept.addActionListener(e -> {
            new Thread(() -> {
                try {
                    ApiClient.acceptFriendRequest(reqId);
                    SwingUtilities.invokeLater(this::loadFriendships);
                } catch (Exception ignored) {}
            }).start();
        });

        JButton btnReject = new JButton("Từ chối");
        btnReject.setFont(UITheme.FONT_MEDIUM);
        btnReject.addActionListener(e -> {
            new Thread(() -> {
                try {
                    ApiClient.cancelFriendRequest(reqId);
                    SwingUtilities.invokeLater(this::loadFriendships);
                } catch (Exception ignored) {}
            }).start();
        });

        btns.add(btnAccept);
        btns.add(btnReject);
        row.add(btns, BorderLayout.EAST);
        return row;
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

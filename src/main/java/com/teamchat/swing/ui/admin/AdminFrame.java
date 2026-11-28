package com.teamchat.swing.ui.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teamchat.swing.api.ApiClient;
import com.teamchat.swing.ui.UITheme;
import com.teamchat.swing.ui.components.RoundedButton;
import com.teamchat.swing.ui.components.RoundedPanel;
import com.teamchat.swing.ui.components.VectorIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================
 * FILE: AdminFrame.java - GIAO DIỆN QUẢN TRỊ VIÊN HỆ THỐNG (SWING ADMIN)
 * =========================================================================
 * Cung cấp đầy đủ tính năng:
 * 1. Dashboard tổng quan (4 thẻ thống kê số liệu).
 * 2. Quản lý danh sách người dùng (Tìm kiếm, xem chi tiết, xóa tài khoản).
 * 3. Quản lý hội thoại & nhóm chat (Tìm kiếm, xóa nhóm vi phạm).
 * 4. Giám sát nhật ký tin nhắn thời gian thực.
 */
public class AdminFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentCards = new JPanel(cardLayout);

    // Labels thống kê
    private JLabel lblStatUsers;
    private JLabel lblStatGroups;
    private JLabel lblStatMessages;
    private JLabel lblStatStatus;

    // Table models
    private DefaultTableModel usersTableModel;
    private JTable usersTable;
    private JTextField txtSearchUsers;

    private DefaultTableModel groupsTableModel;
    private JTable groupsTable;
    private JTextField txtSearchGroups;

    private DefaultTableModel messagesTableModel;
    private JTable messagesTable;

    // Tabs navigation buttons
    private JButton btnTabOverview;
    private JButton btnTabUsers;
    private JButton btnTabGroups;
    private JButton btnTabMessages;

    public AdminFrame() {
        setTitle("CHATSPACE — Quản Trị Hệ Thống (Admin Portal)");
        setSize(1160, 740);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(UITheme.PAGE_BG);

        initUI();
        loadAllData();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 1. THANH TOPBAR QUẢN TRỊ
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UITheme.TOPBAR_BG);
        topBar.setPreferredSize(new Dimension(1160, 58));
        topBar.setBorder(new EmptyBorder(0, 20, 0, 20));

        // Bên trái: Logo + Title + Badge Admin
        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 14));
        leftTop.setOpaque(false);

        JLabel brandIcon = new JLabel(new VectorIcon(VectorIcon.Type.CHAT, 22, Color.WHITE, 2.0f));
        JLabel brandTitle = new JLabel("CHATSPACE");
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        brandTitle.setForeground(Color.WHITE);

        JLabel badgeAdmin = new JLabel(" ADMIN PORTAL ");
        badgeAdmin.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badgeAdmin.setForeground(Color.WHITE);
        badgeAdmin.setBackground(UITheme.DANGER_RED);
        badgeAdmin.setOpaque(true);
        badgeAdmin.setBorder(new EmptyBorder(2, 6, 2, 6));

        leftTop.add(brandIcon);
        leftTop.add(brandTitle);
        leftTop.add(badgeAdmin);
        topBar.add(leftTop, BorderLayout.WEST);

        // Bên phải: Nút làm mới + Nút đóng
        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        rightTop.setOpaque(false);

        RoundedButton btnRefresh = new RoundedButton("Làm mới dữ liệu", 8, new Color(255, 255, 255, 25), Color.WHITE);
        btnRefresh.setFont(UITheme.FONT_BOLD_SMALL);
        btnRefresh.addActionListener(e -> loadAllData());
        rightTop.add(btnRefresh);

        RoundedButton btnClose = new RoundedButton("Đóng quản trị", 8, Color.WHITE, UITheme.TEXT_MAIN);
        btnClose.setFont(UITheme.FONT_BOLD_SMALL);
        btnClose.addActionListener(e -> dispose());
        rightTop.add(btnClose);

        topBar.add(rightTop, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // 2. THANH ĐIỀU HƯỚNG TABS
        JPanel navTabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        navTabs.setBackground(Color.WHITE);
        navTabs.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_LIGHT));

        btnTabOverview = createNavTab("Tổng quan", VectorIcon.Type.GRID, true);
        btnTabUsers = createNavTab("Quản lý người dùng", VectorIcon.Type.FRIENDS, false);
        btnTabGroups = createNavTab("Hội thoại & Nhóm", VectorIcon.Type.CHAT, false);
        btnTabMessages = createNavTab("Nhật ký tin nhắn", VectorIcon.Type.LIST, false);

        btnTabOverview.addActionListener(e -> switchTab("overview", btnTabOverview));
        btnTabUsers.addActionListener(e -> switchTab("users", btnTabUsers));
        btnTabGroups.addActionListener(e -> switchTab("groups", btnTabGroups));
        btnTabMessages.addActionListener(e -> switchTab("messages", btnTabMessages));

        navTabs.add(btnTabOverview);
        navTabs.add(btnTabUsers);
        navTabs.add(btnTabGroups);
        navTabs.add(btnTabMessages);
        add(navTabs, BorderLayout.BEFORE_FIRST_LINE);

        // Đặt layout chính gồm TopBar ở trên, NavTabs ở dưới topbar
        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.add(topBar, BorderLayout.NORTH);
        headerWrap.add(navTabs, BorderLayout.SOUTH);
        add(headerWrap, BorderLayout.NORTH);

        // 3. NỘI DUNG CHÍNH (CONTENT CARDS)
        contentCards.setOpaque(false);
        contentCards.setBorder(new EmptyBorder(16, 20, 16, 20));

        contentCards.add(createOverviewPanel(), "overview");
        contentCards.add(createUsersPanel(), "users");
        contentCards.add(createGroupsPanel(), "groups");
        contentCards.add(createMessagesPanel(), "messages");

        add(contentCards, BorderLayout.CENTER);
    }

    private JButton createNavTab(String text, VectorIcon.Type iconType, boolean active) {
        JButton btn = new JButton(text, new VectorIcon(iconType, 16, active ? UITheme.THEME_PRIMARY : UITheme.TEXT_MUTED));
        btn.setFont(UITheme.FONT_BOLD_SMALL);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setIconTextGap(8);
        updateNavTabStyle(btn, active, iconType);
        return btn;
    }

    private void updateNavTabStyle(JButton btn, boolean active, VectorIcon.Type iconType) {
        if (active) {
            btn.setBackground(new Color(29, 114, 184, 20));
            btn.setForeground(UITheme.THEME_PRIMARY);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(29, 114, 184, 80), 1),
                    new EmptyBorder(6, 14, 6, 14)
            ));
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(UITheme.TEXT_MUTED);
            btn.setBorder(new EmptyBorder(7, 14, 7, 14));
        }
        btn.setIcon(new VectorIcon(iconType, 16, active ? UITheme.THEME_PRIMARY : UITheme.TEXT_MUTED));
    }

    private void switchTab(String name, JButton selectedBtn) {
        cardLayout.show(contentCards, name);
        updateNavTabStyle(btnTabOverview, btnTabOverview == selectedBtn, VectorIcon.Type.GRID);
        updateNavTabStyle(btnTabUsers, btnTabUsers == selectedBtn, VectorIcon.Type.FRIENDS);
        updateNavTabStyle(btnTabGroups, btnTabGroups == selectedBtn, VectorIcon.Type.CHAT);
        updateNavTabStyle(btnTabMessages, btnTabMessages == selectedBtn, VectorIcon.Type.LIST);
    }

    // =====================================================================
    // TAB 1: TỔNG QUAN HỆ THỐNG (DASHBOARD OVERVIEW)
    // =====================================================================
    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);

        // 4 Thẻ KPI thống kê
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setPreferredSize(new Dimension(1100, 100));

        lblStatUsers = new JLabel("0", SwingConstants.LEFT);
        lblStatGroups = new JLabel("0", SwingConstants.LEFT);
        lblStatMessages = new JLabel("0", SwingConstants.LEFT);
        lblStatStatus = new JLabel("ONLINE", SwingConstants.LEFT);
        lblStatStatus.setForeground(UITheme.ONLINE_GREEN);

        kpiGrid.add(createKpiCard("Tổng người dùng", lblStatUsers, VectorIcon.Type.FRIENDS, new Color(2, 132, 199)));
        kpiGrid.add(createKpiCard("Nhóm trò chuyện", lblStatGroups, VectorIcon.Type.CHAT, new Color(217, 119, 6)));
        kpiGrid.add(createKpiCard("Tổng tin nhắn", lblStatMessages, VectorIcon.Type.SEND, new Color(22, 163, 74)));
        kpiGrid.add(createKpiCard("Trạng thái máy chủ", lblStatStatus, VectorIcon.Type.SETTINGS, new Color(147, 51, 234)));

        panel.add(kpiGrid, BorderLayout.NORTH);

        // Khung thông tin giới thiệu hệ thống
        RoundedPanel infoCard = new RoundedPanel(12, UITheme.BORDER_LIGHT);
        infoCard.setBackground(Color.WHITE);
        infoCard.setLayout(new BorderLayout());
        infoCard.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel infoTitle = new JLabel("Trung tâm điều khiển & Giám sát ChatSpace");
        infoTitle.setFont(UITheme.FONT_HEADING);
        infoTitle.setForeground(UITheme.TEXT_MAIN);

        JTextArea infoText = new JTextArea("""
                Hệ thống ChatSpace Realtime đang vận hành trên kiến trúc Client-Server:
                • Máy chủ Backend: Spring Boot 3.5 + Spring Security + Spring Data JPA.
                • Cơ sở dữ liệu: MySQL Database (Flyway Migration V1..V6).
                • Giao thức thời gian thực: WebSocket STOMP Message Broker (/topic, /queue).
                • Khách hàng kết nối: Trình duyệt Web (HTML5/Bootstrap) & Ứng dụng Desktop (Java Swing FlatLaf).
                
                Bạn có thể sử dụng các tab phía trên để tra cứu người dùng, quản lý nhóm hoặc giám sát tin nhắn tức thời.
                """);
        infoText.setFont(UITheme.FONT_REGULAR);
        infoText.setForeground(UITheme.TEXT_MUTED);
        infoText.setEditable(false);
        infoText.setOpaque(false);
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);

        infoCard.add(infoTitle, BorderLayout.NORTH);
        infoCard.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        infoCard.add(infoText, BorderLayout.SOUTH);

        panel.add(infoCard, BorderLayout.CENTER);
        return panel;
    }

    private RoundedPanel createKpiCard(String label, JLabel valueLabel, VectorIcon.Type iconType, Color iconColor) {
        RoundedPanel card = new RoundedPanel(12, UITheme.BORDER_LIGHT);
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(14, 0));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        JLabel iconLabel = new JLabel(new VectorIcon(iconType, 28, iconColor, 2.0f));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        textPanel.setOpaque(false);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        if (valueLabel.getForeground() == null || valueLabel.getForeground() == Color.BLACK) {
            valueLabel.setForeground(UITheme.TEXT_MAIN);
        }

        JLabel titleLabel = new JLabel(label);
        titleLabel.setFont(UITheme.FONT_SMALL);
        titleLabel.setForeground(UITheme.TEXT_MUTED);

        textPanel.add(valueLabel);
        textPanel.add(titleLabel);

        card.add(iconLabel, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    // =====================================================================
    // TAB 2: QUẢN LÝ NGƯỜI DÙNG (USER MANAGEMENT)
    // =====================================================================
    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        // Thanh công cụ trên bảng
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchWrap.setOpaque(false);
        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(UITheme.FONT_BOLD_SMALL);

        txtSearchUsers = new JTextField(20);
        txtSearchUsers.setFont(UITheme.FONT_REGULAR);
        JButton btnSearch = new JButton("Tìm", new VectorIcon(VectorIcon.Type.SEARCH, 14, Color.WHITE));
        btnSearch.setBackground(UITheme.THEME_PRIMARY);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.addActionListener(e -> searchUsers());

        searchWrap.add(lblSearch);
        searchWrap.add(txtSearchUsers);
        searchWrap.add(btnSearch);
        toolbar.add(searchWrap, BorderLayout.WEST);

        RoundedButton btnDeleteUser = new RoundedButton("Xóa tài khoản đã chọn", 8, UITheme.DANGER_RED, Color.WHITE);
        btnDeleteUser.setFont(UITheme.FONT_BOLD_SMALL);
        btnDeleteUser.addActionListener(e -> handleDeleteUser());
        toolbar.add(btnDeleteUser, BorderLayout.EAST);

        panel.add(toolbar, BorderLayout.NORTH);

        // Bảng dữ liệu người dùng
        String[] columns = {"ID", "Tên hiển thị", "Địa chỉ Gmail", "Số điện thoại", "Ngày đăng ký", "Lần đăng nhập cuối"};
        usersTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        usersTable = createStyledTable(usersTableModel);

        JScrollPane scroll = new JScrollPane(usersTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // =====================================================================
    // TAB 3: QUẢN LÝ HỘI THOẠI & NHÓM (CONVERSATIONS)
    // =====================================================================
    private JPanel createGroupsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchWrap.setOpaque(false);
        JLabel lblSearch = new JLabel("Tìm hội thoại:");
        lblSearch.setFont(UITheme.FONT_BOLD_SMALL);

        txtSearchGroups = new JTextField(20);
        txtSearchGroups.setFont(UITheme.FONT_REGULAR);
        JButton btnSearch = new JButton("Tìm", new VectorIcon(VectorIcon.Type.SEARCH, 14, Color.WHITE));
        btnSearch.setBackground(UITheme.THEME_PRIMARY);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.addActionListener(e -> searchGroups());

        searchWrap.add(lblSearch);
        searchWrap.add(txtSearchGroups);
        searchWrap.add(btnSearch);
        toolbar.add(searchWrap, BorderLayout.WEST);

        RoundedButton btnDeleteGroup = new RoundedButton("Xóa hội thoại / nhóm", 8, UITheme.DANGER_RED, Color.WHITE);
        btnDeleteGroup.setFont(UITheme.FONT_BOLD_SMALL);
        btnDeleteGroup.addActionListener(e -> handleDeleteGroup());
        toolbar.add(btnDeleteGroup, BorderLayout.EAST);

        panel.add(toolbar, BorderLayout.NORTH);

        String[] columns = {"ID", "Tên Hội thoại / Nhóm", "Phân loại", "Người tạo", "Số thành viên", "Ngày tạo"};
        groupsTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        groupsTable = createStyledTable(groupsTableModel);

        JScrollPane scroll = new JScrollPane(groupsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // =====================================================================
    // TAB 4: NHẬT KÝ TIN NHẮN (MESSAGES LOG)
    // =====================================================================
    private JPanel createMessagesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        JLabel lblTitle = new JLabel("50 tin nhắn gần nhất trên toàn hệ thống ChatSpace");
        lblTitle.setFont(UITheme.FONT_HEADING);
        lblTitle.setForeground(UITheme.TEXT_MAIN);
        panel.add(lblTitle, BorderLayout.NORTH);

        String[] columns = {"ID", "Hội thoại", "Người gửi", "Nội dung tin nhắn", "Thời gian gửi"};
        messagesTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        messagesTable = createStyledTable(messagesTableModel);

        JScrollPane scroll = new JScrollPane(messagesTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(UITheme.FONT_REGULAR);
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setGridColor(new Color(241, 245, 249));

        JTableHeader header = table.getTableHeader();
        header.setFont(UITheme.FONT_BOLD_SMALL);
        header.setBackground(new Color(248, 250, 252));
        header.setForeground(UITheme.TEXT_MUTED);
        header.setPreferredSize(new Dimension(header.getWidth(), 36));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        return table;
    }

    // =====================================================================
    // TẢI VÀ BƠM DỮ LIỆU TỪ BACKEND
    // =====================================================================
    private void loadAllData() {
        new SwingWorker<Void, Void>() {
            JsonObject statsObj;
            JsonObject usersObj;
            JsonObject groupsObj;
            JsonObject msgsObj;

                                    @Override
            protected Void doInBackground() throws Exception {
                try { statsObj = ApiClient.getAdminStats(); } catch (Exception ex) { ex.printStackTrace(); }
                try { usersObj = ApiClient.getAdminUsers(""); } catch (Exception ex) { ex.printStackTrace(); }
                try { groupsObj = ApiClient.getAdminConversations(""); } catch (Exception ex) { ex.printStackTrace(); }
                try { msgsObj = ApiClient.getAdminMessages(); } catch (Exception ex) { ex.printStackTrace(); }
                return null;
            }

            @Override
            protected void done() {
                // 1. Stats
                if (statsObj != null && statsObj.has("totalUsers")) {
                    lblStatUsers.setText(String.valueOf(statsObj.get("totalUsers").getAsLong()));
                    lblStatGroups.setText(String.valueOf(statsObj.get("totalGroups").getAsLong()));
                    lblStatMessages.setText(String.valueOf(statsObj.get("totalMessages").getAsLong()));
                }

                // 2. Users Table
                if (usersObj != null && usersObj.has("users")) {
                    usersTableModel.setRowCount(0);
                    JsonArray arr = usersObj.getAsJsonArray("users");
                    for (JsonElement el : arr) {
                        JsonObject u = el.getAsJsonObject();
                        usersTableModel.addRow(new Object[]{
                                u.get("id").getAsLong(),
                                u.get("name").getAsString(),
                                u.get("email").getAsString(),
                                u.get("phone").getAsString(),
                                u.get("createdAt").getAsString(),
                                u.get("lastLoginAt").getAsString()
                        });
                    }
                }

                // 3. Groups Table
                if (groupsObj != null && groupsObj.has("conversations")) {
                    groupsTableModel.setRowCount(0);
                    JsonArray arr = groupsObj.getAsJsonArray("conversations");
                    for (JsonElement el : arr) {
                        JsonObject g = el.getAsJsonObject();
                        groupsTableModel.addRow(new Object[]{
                                g.get("id").getAsLong(),
                                g.get("name").getAsString(),
                                "GROUP".equals(g.get("type").getAsString()) ? "Nhóm chat" : "Cá nhân",
                                g.get("createdByName").getAsString(),
                                g.get("memberCount").getAsInt() + " người",
                                g.get("createdAt").getAsString()
                        });
                    }
                }

                // 4. Messages Table
                if (msgsObj != null && msgsObj.has("messages")) {
                    messagesTableModel.setRowCount(0);
                    JsonArray arr = msgsObj.getAsJsonArray("messages");
                    for (JsonElement el : arr) {
                        JsonObject m = el.getAsJsonObject();
                        messagesTableModel.addRow(new Object[]{
                                m.get("id").getAsLong(),
                                m.get("conversationName").getAsString(),
                                m.get("senderName").getAsString(),
                                m.get("content").getAsString(),
                                m.get("createdAt").getAsString()
                        });
                    }
                }
            }
        }.execute();
    }

    private void searchUsers() {
        String query = txtSearchUsers.getText().trim();
        new SwingWorker<JsonObject, Void>() {
            @Override protected JsonObject doInBackground() throws Exception {
                return ApiClient.getAdminUsers(query);
            }
            @Override protected void done() {
                try {
                    JsonObject res = get();
                    if (res != null && res.has("users")) {
                        usersTableModel.setRowCount(0);
                        for (JsonElement el : res.getAsJsonArray("users")) {
                            JsonObject u = el.getAsJsonObject();
                            usersTableModel.addRow(new Object[]{
                                    u.get("id").getAsLong(),
                                    u.get("name").getAsString(),
                                    u.get("email").getAsString(),
                                    u.get("phone").getAsString(),
                                    u.get("createdAt").getAsString(),
                                    u.get("lastLoginAt").getAsString()
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void searchGroups() {
        String query = txtSearchGroups.getText().trim();
        new SwingWorker<JsonObject, Void>() {
            @Override protected JsonObject doInBackground() throws Exception {
                return ApiClient.getAdminConversations(query);
            }
            @Override protected void done() {
                try {
                    JsonObject res = get();
                    if (res != null && res.has("conversations")) {
                        groupsTableModel.setRowCount(0);
                        for (JsonElement el : res.getAsJsonArray("conversations")) {
                            JsonObject g = el.getAsJsonObject();
                            groupsTableModel.addRow(new Object[]{
                                    g.get("id").getAsLong(),
                                    g.get("name").getAsString(),
                                    "GROUP".equals(g.get("type").getAsString()) ? "Nhóm chat" : "Cá nhân",
                                    g.get("createdByName").getAsString(),
                                    g.get("memberCount").getAsInt() + " người",
                                    g.get("createdAt").getAsString()
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void handleDeleteUser() {
        int row = usersTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người dùng trên bảng để xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long userId = (Long) usersTableModel.getValueAt(row, 0);
        String name = (String) usersTableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa tài khoản '" + name + "' (ID: " + userId + ")?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try {
                    ApiClient.deleteAdminUser(userId);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Đã xóa người dùng thành công!");
                        loadAllData();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi khi xóa: " + ex.getMessage()));
                }
            }).start();
        }
    }

    private void handleDeleteGroup() {
        int row = groupsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một hội thoại/nhóm trên bảng để xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long convId = (Long) groupsTableModel.getValueAt(row, 0);
        String name = (String) groupsTableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa hội thoại/nhóm '" + name + "' (ID: " + convId + ")?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try {
                    ApiClient.deleteAdminConversation(convId);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Đã xóa hội thoại thành công!");
                        loadAllData();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi khi xóa: " + ex.getMessage()));
                }
            }).start();
        }
    }
}

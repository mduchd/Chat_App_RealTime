package com.teamchat.swing.ui.panels;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.teamchat.swing.model.UserInfo;
import com.teamchat.swing.ui.UITheme;
import com.teamchat.swing.ui.components.AvatarCircle;
import com.teamchat.swing.ui.components.VectorIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Top bar matching ChatSpace web design:
 * Dark blue background #16263d, brand logo, STOMP status pill,
 * user profile, friends, notifications, dark mode toggle, settings, and window dots.
 */
public class TopBarPanel extends JPanel {

    private final UserInfo currentUser;
    private final JLabel wsDot;
    private final JLabel wsText;
    private AvatarCircle userAvatar;

    private boolean isDarkMode = false;
    private JButton btnDarkMode;

    private Runnable onFriendsClick;
    private Runnable onSettingsClick;
    private Runnable onLogoutClick;
    private Runnable onAdminClick;
    private Runnable onDarkModeToggle;

    public TopBarPanel(UserInfo user) {
        this.currentUser = user;
        setLayout(new BorderLayout());
        setBackground(UITheme.TOPBAR_BG);
        setPreferredSize(new Dimension(1000, 54));
        setBorder(new EmptyBorder(0, 18, 0, 18));

        // ===================== LEFT: Brand + WebSocket Status =====================
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        leftPanel.setOpaque(false);

        // Brand icon (VectorIcon CHAT)
        JLabel brandIcon = new JLabel(new VectorIcon(VectorIcon.Type.CHAT, 22, Color.WHITE, 2.0f));
        leftPanel.add(brandIcon);

        JLabel brandLabel = new JLabel("CHATSPACE");
        brandLabel.setFont(UITheme.FONT_HEADING);
        brandLabel.setForeground(Color.WHITE);
        leftPanel.add(brandLabel);

        // WS Pill
        JPanel wsPill = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 25));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.dispose();
            }
        };
        wsPill.setOpaque(false);
        wsPill.setBorder(new EmptyBorder(2, 8, 2, 8));

        wsDot = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getForeground());
                g2.fill(new Ellipse2D.Float(0, 2, 7, 7));
                g2.dispose();
            }
        };
        wsDot.setPreferredSize(new Dimension(8, 11));
        wsDot.setForeground(new Color(0x94, 0xa3, 0xb8));

        wsText = new JLabel("WebSocket đang kết nối...");
        wsText.setFont(UITheme.FONT_SMALL);
        wsText.setForeground(new Color(0xcb, 0xd5, 0xe1));

        wsPill.add(wsDot);
        wsPill.add(wsText);
        leftPanel.add(wsPill);

        add(leftPanel, BorderLayout.WEST);

        // ===================== RIGHT: User + Actions + Dots =====================
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rightPanel.setOpaque(false);

        // User profile pill
        JPanel userPill = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
            }
        };
        userPill.setOpaque(false);
        userPill.setCursor(new Cursor(Cursor.HAND_CURSOR));
        userPill.setBorder(new EmptyBorder(2, 4, 2, 10));

        userAvatar = new AvatarCircle(user != null ? user.getInitials() : "CS", 24, UITheme.THEME_PRIMARY, Color.WHITE);
        if (user != null && user.getAvatar() != null && !user.getAvatar().isBlank()) {
            userAvatar.setImageUrl(user.getAvatar());
        }
        JLabel userName = new JLabel(user != null ? user.getName() : "Người dùng");
        userName.setFont(UITheme.FONT_SUBHEADING);
        userName.setForeground(Color.WHITE);

        JLabel userArrow = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 180));
                int[] xPoints = {1, 9, 5};
                int[] yPoints = {3, 3, 7};
                g2.fillPolygon(xPoints, yPoints, 3);
                g2.dispose();
            }
        };
        userArrow.setPreferredSize(new Dimension(10, 10));

        userPill.add(userAvatar);
        userPill.add(userName);
        userPill.add(userArrow);
        userPill.setToolTipText("Tài khoản: " + (user != null ? user.getName() : "") + " (Bấm để mở menu)");

        userPill.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showAccountMenu(userPill, 0, userPill.getHeight() + 4);
            }
        });
        rightPanel.add(userPill);

        // 1. Nút Xem danh sách bạn bè và kết bạn (VectorIcon FRIENDS)
        JButton btnFriends = createVectorButton(VectorIcon.Type.FRIENDS, "Xem danh sách bạn bè & kết bạn");
        btnFriends.addActionListener(e -> {
            if (onFriendsClick != null) onFriendsClick.run();
        });
        rightPanel.add(btnFriends);

        // 2. Nút Thông báo hệ thống (VectorIcon BELL)
        JButton btnNotifications = createVectorButton(VectorIcon.Type.BELL, "Thông báo hệ thống");
        btnNotifications.addActionListener(e -> showNotifications(btnNotifications));
        rightPanel.add(btnNotifications);

        // 3. Nút Chế độ Sáng / Tối (VectorIcon MOON / SUN)
        btnDarkMode = createVectorButton(VectorIcon.Type.MOON, "Chế độ Sáng / Tối");
        btnDarkMode.addActionListener(e -> toggleDarkMode());
        rightPanel.add(btnDarkMode);

        // 4. Nút Cài đặt tài khoản (VectorIcon SETTINGS - Bánh răng cơ khí)
        JButton btnSettings = createVectorButton(VectorIcon.Type.SETTINGS, "Cài đặt tài khoản");
        btnSettings.addActionListener(e -> {
            if (onSettingsClick != null) onSettingsClick.run();
        });
        rightPanel.add(btnSettings);

        // 3 Window Dots
        JPanel windowDots = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 8));
        windowDots.setOpaque(false);
        windowDots.add(createDot(UITheme.WIN_DOT_BLUE));
        windowDots.add(createDot(UITheme.WIN_DOT_YELLOW));
        windowDots.add(createDot(UITheme.WIN_DOT_GREEN));
        rightPanel.add(windowDots);

        add(rightPanel, BorderLayout.EAST);
    }

    public void setWsConnected(boolean connected) {
        if (connected) {
            wsDot.setForeground(new Color(0x34, 0xd3, 0x99));
            wsText.setText("WebSocket sẵn sàng");
        } else {
            wsDot.setForeground(new Color(0x94, 0xa3, 0xb8));
            wsText.setText("Mất kết nối WebSocket");
        }
        repaint();
    }

    public void setOnFriendsClick(Runnable callback) { this.onFriendsClick = callback; }
    public void setOnSettingsClick(Runnable callback) { this.onSettingsClick = callback; }
    public void setOnLogoutClick(Runnable callback) { this.onLogoutClick = callback; }
    public void setOnAdminClick(Runnable callback) { this.onAdminClick = callback; }
    public void setOnDarkModeToggle(Runnable callback) { this.onDarkModeToggle = callback; }

    private JButton createVectorButton(VectorIcon.Type type, String tooltip) {
        JButton btn = new JButton(new VectorIcon(type, 18, new Color(0xcb, 0xd5, 0xe1)));
        btn.setPreferredSize(new Dimension(32, 32));
        btn.setBackground(new Color(255, 255, 255, 18));
        btn.setBorder(BorderFactory.createEmptyBorder());
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setToolTipText(tooltip);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setIcon(new VectorIcon(type, 18, Color.WHITE));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setIcon(new VectorIcon(type, 18, new Color(0xcb, 0xd5, 0xe1)));
            }
        });
        return btn;
    }

    private JComponent createDot(Color color) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(13, 13));
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fill(new Ellipse2D.Float(0, 0, 13, 13));
                g2.dispose();
            }
        };
    }

    /**
     * Menu thả xuống khi click vào pill tài khoản (Khớp #accountMenu trong HTML)
     */
    private void showAccountMenu(Component invoker, int x, int y) {
        JPopupMenu menu = new JPopupMenu();

        String name = currentUser != null ? currentUser.getName() : "Người dùng";
        String email = currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail() : "";
        JLabel profileHeader = new JLabel("<html><b>" + name + "</b><br><small><font color='#64748b'>" + email + "</font></small></html>");
        profileHeader.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        menu.add(profileHeader);
        menu.addSeparator();

        if (currentUser != null && currentUser.isAdmin()) {
            JMenuItem mAdmin = new JMenuItem("Quản trị hệ thống (Admin)", new VectorIcon(VectorIcon.Type.GRID, 14, UITheme.THEME_PRIMARY));
            mAdmin.addActionListener(e -> {
                if (onAdminClick != null) onAdminClick.run();
            });
            menu.add(mAdmin);
        }

        JMenuItem mSettings = new JMenuItem("Cài đặt tài khoản", new VectorIcon(VectorIcon.Type.SETTINGS, 14, UITheme.TEXT_MAIN));
        mSettings.addActionListener(e -> {
            if (onSettingsClick != null) onSettingsClick.run();
        });
        menu.add(mSettings);

        JMenuItem mFriends = new JMenuItem("Bạn bè & Lời mời", new VectorIcon(VectorIcon.Type.FRIENDS, 14, UITheme.TEXT_MAIN));
        mFriends.addActionListener(e -> {
            if (onFriendsClick != null) onFriendsClick.run();
        });
        menu.add(mFriends);

        menu.addSeparator();

        JMenuItem mLogout = new JMenuItem("Đăng xuất", new VectorIcon(VectorIcon.Type.LOGOUT, 14, UITheme.DANGER_RED));
        mLogout.setForeground(UITheme.DANGER_RED);
        mLogout.addActionListener(e -> {
            if (onLogoutClick != null) onLogoutClick.run();
        });
        menu.add(mLogout);

        menu.show(invoker, x, y);
    }

    /**
     * Chuyển đổi qua lại giữa giao diện Sáng (Light) và Tối (Dark)
     */
    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        UITheme.setDarkMode(isDarkMode);
        try {
            if (isDarkMode) {
                FlatDarkLaf.setup();
                btnDarkMode.setIcon(new VectorIcon(VectorIcon.Type.SUN, 18, Color.YELLOW));
                btnDarkMode.setToolTipText("Chuyển sang giao diện Sáng");
            } else {
                FlatLightLaf.setup();
                btnDarkMode.setIcon(new VectorIcon(VectorIcon.Type.MOON, 18, new Color(0xcb, 0xd5, 0xe1)));
                btnDarkMode.setToolTipText("Chuyển sang giao diện Tối");
            }
            FlatLaf.updateUI();
        } catch (Exception ex) {
            System.err.println("Lỗi chuyển đổi theme: " + ex.getMessage());
        }
        if (onDarkModeToggle != null) {
            onDarkModeToggle.run();
        }
    }

    /**
     * Hiển thị bảng thông báo hệ thống khi bấm nút Chuông (Bell)
     */
    private void showNotifications(Component invoker) {
        JPopupMenu notifMenu = new JPopupMenu();
        notifMenu.setBackground(Color.WHITE);
        notifMenu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel title = new JLabel("Thông báo hệ thống");
        title.setFont(UITheme.FONT_HEADING);
        title.setForeground(UITheme.TEXT_MAIN);
        notifMenu.add(title);
        notifMenu.addSeparator();

        JLabel item1 = new JLabel("<html><b>Hệ thống ChatSpace</b><br><font color='#64748b' size='2'>Chào mừng bạn đến với ChatSpace Realtime!</font></html>");
        item1.setBorder(new EmptyBorder(6, 4, 6, 4));
        notifMenu.add(item1);

        JLabel item2 = new JLabel("<html><b>Quản trị viên</b><br><font color='#64748b' size='2'>Tất cả tính năng đã sẵn sàng sử dụng.</font></html>");
        item2.setBorder(new EmptyBorder(6, 4, 6, 4));
        notifMenu.add(item2);

        notifMenu.show(invoker, 0, invoker.getHeight() + 4);
    }

    public void updateUserAvatar(String avatarUrl) {
        if (userAvatar != null) {
            userAvatar.setImageUrl(avatarUrl);
        }
    }
}

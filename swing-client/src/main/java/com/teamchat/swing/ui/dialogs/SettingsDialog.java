package com.teamchat.swing.ui.dialogs;

import com.teamchat.swing.model.UserInfo;
import com.teamchat.swing.ui.UITheme;
import com.teamchat.swing.ui.components.AvatarCircle;
import com.teamchat.swing.util.SoundService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Settings modal matching ChatSpace web design.
 */
public class SettingsDialog extends JDialog {

    private final UserInfo user;
    private Runnable onLogout;
    private Consumer<String> onAvatarChanged;

    public SettingsDialog(Frame parent, UserInfo user) {
        super(parent, "Cài đặt tài khoản", true);
        this.user = user;
        setSize(450, 540);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(UITheme.WINDOW_BG);
        main.setBorder(new EmptyBorder(18, 20, 18, 20));

        // 1. Profile Box (tăng chiều cao và không giới hạn 80px để không bị lấp nút đổi avatar)
        JPanel profileBox = new JPanel(new BorderLayout(14, 0));
        profileBox.setBackground(UITheme.SIDEBAR_CARD_BG);
        profileBox.setBorder(new EmptyBorder(12, 14, 12, 14));
        profileBox.setPreferredSize(new Dimension(410, 96));
        profileBox.setMaximumSize(new Dimension(Short.MAX_VALUE, 98));

        AvatarCircle av = new AvatarCircle(user != null ? user.getInitials() : "CS", 48, UITheme.THEME_PRIMARY, Color.WHITE);
        if (user != null && user.getAvatar() != null && !user.getAvatar().isBlank()) {
            av.setImageUrl(user.getAvatar());
        }
        av.setCursor(new Cursor(Cursor.HAND_CURSOR));
        av.setToolTipText("Bấm vào đây để thay đổi ảnh đại diện");
        av.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openChangeAvatarDialog(av);
            }
        });
        profileBox.add(av, BorderLayout.WEST);

        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);
        namePanel.setBorder(new EmptyBorder(2, 0, 2, 0));
        JLabel nameLbl = new JLabel(user != null ? user.getName() : "Người dùng");
        nameLbl.setFont(UITheme.FONT_HEADING);
        nameLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel emailLbl = new JLabel(user != null ? user.getEmail() : "");
        emailLbl.setFont(UITheme.FONT_SMALL);
        emailLbl.setForeground(UITheme.TEXT_MUTED);

        JButton btnChangeAvatar = new JButton("Đổi ảnh đại diện") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(new Color(0xbf, 0xdb, 0xfe));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(0xdb, 0xea, 0xfe));
                } else {
                    g2.setColor(new Color(0xef, 0xf6, 0xff));
                }
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 6, 6));
                g2.setColor(new Color(0x93, 0xc5, 0xfd));
                g2.draw(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 6, 6));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnChangeAvatar.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnChangeAvatar.setForeground(UITheme.THEME_PRIMARY);
        btnChangeAvatar.setContentAreaFilled(false);
        btnChangeAvatar.setBorder(new EmptyBorder(2, 8, 2, 8));
        btnChangeAvatar.setFocusPainted(false);
        btnChangeAvatar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChangeAvatar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnChangeAvatar.addActionListener(e -> openChangeAvatarDialog(av));

        namePanel.add(nameLbl);
        namePanel.add(Box.createVerticalStrut(2));
        namePanel.add(emailLbl);
        namePanel.add(Box.createVerticalStrut(4));
        namePanel.add(btnChangeAvatar);
        profileBox.add(namePanel, BorderLayout.CENTER);

        JButton btnLogout = new JButton("Đăng xuất") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(new Color(0xfe, 0xe2, 0xe2));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(0xff, 0xf1, 0xf2));
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(new Color(0xfc, 0xa5, 0xa5));
                g2.draw(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnLogout.setFont(UITheme.FONT_MEDIUM);
        btnLogout.setForeground(new Color(0xdc, 0x26, 0x26));
        btnLogout.setContentAreaFilled(false);
        btnLogout.setBorder(new EmptyBorder(6, 14, 6, 14));
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> {
            dispose();
            if (onLogout != null) onLogout.run();
        });
        JPanel logoutWrapper = new JPanel(new GridBagLayout());
        logoutWrapper.setOpaque(false);
        logoutWrapper.add(btnLogout);
        profileBox.add(logoutWrapper, BorderLayout.EAST);

        main.add(profileBox);
        main.add(Box.createVerticalStrut(18));

        // 2. Settings Switches
        main.add(createSettingRow("Trạng thái trực tuyến", "Hiển thị chấm xanh đang hoạt động", true, null));
        main.add(Box.createVerticalStrut(8));
        main.add(createSettingRow("Thông báo tin nhắn mới", "Hiển thị thông báo trên màn hình", true, null));
        main.add(Box.createVerticalStrut(8));

        // Âm thanh thông báo (kèm preview âm thanh khi bật)
        main.add(createSettingRow("Âm thanh thông báo", "Phát âm báo nhẹ khi nhận tin nhắn mới", SoundService.isSoundEnabled(), selected -> {
            SoundService.setSoundEnabled(selected);
            if (selected) {
                SoundService.playNotificationSound();
            }
        }));
        main.add(Box.createVerticalStrut(22));

        // Close button
        JButton btnClose = new JButton("Đóng") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(UITheme.THEME_PRIMARY.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(0x25, 0x63, 0xeb));
                } else {
                    g2.setColor(UITheme.THEME_PRIMARY);
                }
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnClose.setFont(UITheme.FONT_SUBHEADING);
        btnClose.setForeground(Color.WHITE);
        btnClose.setContentAreaFilled(false);
        btnClose.setBorder(new EmptyBorder(8, 20, 8, 20));
        btnClose.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnClose.setMaximumSize(new Dimension(390, 38));
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dispose());
        main.add(btnClose);

        setContentPane(main);
    }

    private void openChangeAvatarDialog(AvatarCircle av) {
        ChangeAvatarDialog dlg = new ChangeAvatarDialog(this, user, newUrl -> {
            av.setImageUrl(newUrl);
            if (onAvatarChanged != null) {
                onAvatarChanged.accept(newUrl);
            }
        });
        dlg.setVisible(true);
    }

    private JPanel createSettingRow(String title, String desc, boolean defaultChecked, Consumer<Boolean> onToggle) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(410, 44));
        row.setBorder(new EmptyBorder(4, 4, 4, 4));

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);

        JLabel tLbl = new JLabel(title);
        tLbl.setFont(UITheme.FONT_SUBHEADING);
        tLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel dLbl = new JLabel(desc);
        dLbl.setFont(UITheme.FONT_XS);
        dLbl.setForeground(UITheme.TEXT_MUTED);

        textPanel.add(tLbl);
        textPanel.add(dLbl);
        row.add(textPanel, BorderLayout.CENTER);

        JCheckBox toggle = new JCheckBox();
        toggle.setSelected(defaultChecked);
        toggle.setOpaque(false);
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggle.addActionListener(e -> {
            if (onToggle != null) {
                onToggle.accept(toggle.isSelected());
            }
        });
        row.add(toggle, BorderLayout.EAST);

        return row;
    }

    public void setOnLogout(Runnable callback) { this.onLogout = callback; }
    public void setOnAvatarChanged(Consumer<String> callback) { this.onAvatarChanged = callback; }
}

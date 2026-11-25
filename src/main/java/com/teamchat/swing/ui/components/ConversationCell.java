package com.teamchat.swing.ui.components;

import com.teamchat.swing.model.ConversationItem;
import com.teamchat.swing.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * List cell renderer for conversation item matching ChatSpace web sidebar.
 */
public class ConversationCell extends JPanel implements ListCellRenderer<ConversationItem> {

    private final AvatarCircle avatar;
    private final JLabel nameLabel;
    private final JLabel timeLabel;
    private final JLabel lastMsgLabel;
    private final JLabel unreadBadge;

    public ConversationCell() {
        setLayout(new BorderLayout(10, 0));
        setBorder(new EmptyBorder(8, 10, 8, 10));
        setOpaque(true);

        // Avatar
        avatar = new AvatarCircle("?", 40);
        add(avatar, BorderLayout.WEST);

        // Center: Name + Last Message
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // Top Row: Name + Time
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        nameLabel = new JLabel();
        nameLabel.setFont(UITheme.FONT_SUBHEADING);
        nameLabel.setForeground(UITheme.TEXT_MAIN);

        timeLabel = new JLabel();
        timeLabel.setFont(UITheme.FONT_SMALL);
        timeLabel.setForeground(UITheme.TEXT_MUTED);

        topRow.add(nameLabel, BorderLayout.CENTER);
        topRow.add(timeLabel, BorderLayout.EAST);
        centerPanel.add(topRow);
        centerPanel.add(Box.createVerticalStrut(3));

        // Bottom Row: Last Msg + Unread badge
        JPanel bottomRow = new JPanel(new BorderLayout(6, 0));
        bottomRow.setOpaque(false);

        lastMsgLabel = new JLabel();
        lastMsgLabel.putClientProperty("html.disable", true);
        lastMsgLabel.setFont(UITheme.FONT_CHAT_TEXT.deriveFont(UITheme.FONT_SMALL.getSize2D()));
        lastMsgLabel.setForeground(UITheme.TEXT_MUTED);

        unreadBadge = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.THEME_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        unreadBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        unreadBadge.setForeground(Color.WHITE);
        unreadBadge.setHorizontalAlignment(SwingConstants.CENTER);
        unreadBadge.setBorder(new EmptyBorder(1, 6, 1, 6));
        unreadBadge.setVisible(false);

        bottomRow.add(lastMsgLabel, BorderLayout.CENTER);
        bottomRow.add(unreadBadge, BorderLayout.EAST);
        centerPanel.add(bottomRow);

        add(centerPanel, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ConversationItem> list,
                                                  ConversationItem item,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        if (item != null) {
            avatar.setText(item.getDisplayInitials());
            avatar.setImageUrl(item.getAvatarUrl(), list);
            nameLabel.setText(item.getName() != null ? item.getName() : "Hội thoại");
            timeLabel.setText(item.getLastMessageTime() != null ? item.getLastMessageTime() : "");
            lastMsgLabel.setText(item.getLastMessage() != null ? item.getLastMessage() : "Bắt đầu cuộc trò chuyện...");

            if (item.getUnreadCount() > 0) {
                unreadBadge.setText(String.valueOf(item.getUnreadCount()));
                unreadBadge.setVisible(true);
                nameLabel.setFont(UITheme.FONT_HEADING);
            } else {
                unreadBadge.setVisible(false);
                nameLabel.setFont(UITheme.FONT_SUBHEADING);
            }

            nameLabel.setForeground(UITheme.TEXT_MAIN);
            timeLabel.setForeground(UITheme.TEXT_MUTED);
            lastMsgLabel.setForeground(UITheme.TEXT_MUTED);

            if (isSelected) {
                setBackground(UITheme.SIDEBAR_ITEM_ACTIVE);
                avatar.setBgColor(UITheme.THEME_PRIMARY);
            } else {
                setBackground(UITheme.SIDEBAR_CARD_BG);
                avatar.setBgColor(UITheme.AVATAR_INACTIVE);
            }
        }
        return this;
    }
}

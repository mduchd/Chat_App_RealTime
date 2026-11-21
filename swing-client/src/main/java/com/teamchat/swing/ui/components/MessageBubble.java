package com.teamchat.swing.ui.components;

import com.teamchat.swing.model.ChatMessage;
import com.teamchat.swing.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Chat bubble matching ChatSpace design:
 * Sent message: Blue #1d72b8 with white text, aligned right
 * Received message: Light gray #edf2f7 with dark text #1e293b, aligned left
 */
public class MessageBubble extends JPanel {

    private final ChatMessage message;

    public MessageBubble(ChatMessage msg) {
        this.message = msg;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(3, 10, 3, 10));

        initUI();
    }

    private void initUI() {
        boolean sentByMe = message.isSentByMe();

        // Bubble inner panel
        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(sentByMe ? UITheme.BUBBLE_SENT_BG : UITheme.BUBBLE_RECEIVED_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
            }
        };
        bubble.setOpaque(false);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(6, 12, 6, 12));

        // Sender name for received messages (especially in group chats)
        if (!sentByMe && message.getSenderName() != null && !message.getSenderName().isBlank()) {
            JLabel senderLabel = new JLabel(message.getSenderName());
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            senderLabel.setForeground(UITheme.THEME_PRIMARY);
            senderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(senderLabel);
            bubble.add(Box.createVerticalStrut(2));
        }

        // Message content (support multi-line wrapping with HTML and crisp Segoe UI font for Vietnamese)
        String safeText = escapeHtml(message.getContent()).replace("\r\n", "<br>").replace("\n", "<br>");
        String textColorHex = sentByMe ? "#ffffff" : (UITheme.isDarkMode ? "#f1f5f9" : "#1e293b");
        // font-size 10px trong Swing HTML render chuẩn nét tương đương font 12.5-13pt chuẩn hệ thống, không bị to thô
        JLabel contentLabel = new JLabel("<html><body style='width: 310px; font-family: \"Segoe UI\", Tahoma, Arial, sans-serif; font-size: 10px; line-height: 1.35; color: " + textColorHex + ";'>" + safeText + "</body></html>");
        contentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        contentLabel.setForeground(sentByMe ? UITheme.BUBBLE_SENT_TEXT : UITheme.BUBBLE_RECEIVED_TEXT);
        contentLabel.setAlignmentX(sentByMe ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        bubble.add(contentLabel);

        // Time display
        String timeStr = message.getTimeDisplay();
        if (!timeStr.isEmpty()) {
            JLabel timeLabel = new JLabel(timeStr);
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            timeLabel.setForeground(sentByMe ? new Color(255, 255, 255, 180) : UITheme.TEXT_MUTED);
            timeLabel.setAlignmentX(sentByMe ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
            bubble.add(Box.createVerticalStrut(2));
            bubble.add(timeLabel);
        }

        // Wrapper to align left or right
        JPanel alignWrapper = new JPanel(new FlowLayout(sentByMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        alignWrapper.setOpaque(false);
        alignWrapper.add(bubble);

        add(alignWrapper, BorderLayout.CENTER);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}

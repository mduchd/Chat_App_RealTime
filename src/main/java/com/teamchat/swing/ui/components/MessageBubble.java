package com.teamchat.swing.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
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

    private static final int MAX_BUBBLE_WIDTH = 400;

    private final JPanel bubble;
    private final JTextArea content;
    private final int preferredBubbleWidth;
    private int bubbleWidth;

    public MessageBubble(ChatMessage message) {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(3, 10, 3, 10));

        boolean sentByMe = message.isSentByMe();

        bubble = new JPanel(new BorderLayout(0, 2)) {
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
        bubble.setBorder(new EmptyBorder(6, 12, 6, 12));

        // Sender name for received messages (especially in group chats)
        if (!sentByMe && message.getSenderName() != null && !message.getSenderName().isBlank()) {
            JLabel senderLabel = new JLabel(message.getSenderName());
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            senderLabel.setForeground(UITheme.THEME_PRIMARY);
            bubble.add(senderLabel, BorderLayout.NORTH);
        }

        content = new JTextArea(message.getContent());
        content.setFont(UITheme.FONT_CHAT_TEXT);
        content.setForeground(sentByMe ? UITheme.BUBBLE_SENT_TEXT : UITheme.BUBBLE_RECEIVED_TEXT);
        content.putClientProperty(FlatClientProperties.MINIMUM_WIDTH, 0);
        content.setOpaque(false);
        content.setBorder(null);
        content.setEditable(false);
        content.setFocusable(false);
        bubble.add(content, BorderLayout.CENTER);

        // Time display
        String timeStr = message.getTimeDisplay();
        if (!timeStr.isEmpty()) {
            JLabel timeLabel = new JLabel(timeStr);
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            timeLabel.setForeground(sentByMe ? new Color(255, 255, 255, 180) : UITheme.TEXT_MUTED);
            timeLabel.setHorizontalAlignment(sentByMe ? SwingConstants.RIGHT : SwingConstants.LEFT);
            bubble.add(timeLabel, BorderLayout.SOUTH);
        }

        preferredBubbleWidth = bubble.getPreferredSize().width;
        content.setLineWrap(true);
        content.setWrapStyleWord(true);

        // Wrapper to align left or right
        JPanel alignWrapper = new JPanel(new FlowLayout(sentByMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        alignWrapper.setOpaque(false);
        alignWrapper.add(bubble);

        add(alignWrapper, BorderLayout.CENTER);
        fitToWidth(Integer.MAX_VALUE);
    }

    public void fitToWidth(int rowWidth) {
        Insets rowInsets = getInsets();
        Insets padding = bubble.getInsets();
        int horizontalPadding = padding.left + padding.right;
        int availableWidth = Math.max(horizontalPadding + 1, rowWidth - rowInsets.left - rowInsets.right);
        int width = Math.min(Math.min(preferredBubbleWidth, MAX_BUBBLE_WIDTH), availableWidth);
        if (width == bubbleWidth) {
            return;
        }

        bubbleWidth = width;
        // Measure the wrapped height at the width BorderLayout will give the text.
        content.setSize(width - horizontalPadding, Short.MAX_VALUE);
        int height = bubble.getLayout().preferredLayoutSize(bubble).height;
        bubble.setPreferredSize(new Dimension(width, height));
    }
}

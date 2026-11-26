package com.teamchat.swing.ui.panels;

import com.teamchat.swing.model.ChatMessage;
import com.teamchat.swing.model.ConversationItem;
import com.teamchat.swing.ui.UITheme;
import com.teamchat.swing.ui.components.AvatarCircle;
import com.teamchat.swing.ui.components.EmojiIcon;
import com.teamchat.swing.ui.components.MessageBubble;
import com.teamchat.swing.ui.components.RoundedPanel;
import com.teamchat.swing.ui.components.VectorIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * =========================================================================
 * FILE: ChatPanel.java - KHUNG HIỂN THỊ TIN NHẮN & SOẠN THẢO (CHAT VIEW)
 * =========================================================================
 * Tái hiện 100% tính năng của web (#activeChatView trong index.html):
 * 1. Header: Avatar, Tên, Badge (Cá nhân/Nhóm), Trạng thái xanh online, Ghim/Tắt TB.
 * 2. Nút tìm tin nhắn trong cuộc trò chuyện (In-chat Search).
 * 3. Nút thêm thành viên nhóm (+ Thêm người).
 * 4. Nút 3 chấm mở/đóng ngăn thông tin (InfoDrawer).
 * 5. Bảng Emoji chuẩn đồ họa vector màu sắc (👍, ❤️, 😂, 😮, 😢, 🔥, 🎉, 🙌).
 * 6. Khung soạn thảo tin nhắn bo góc viền sáng, hỗ trợ gõ Enter để gửi.
 */
public class ChatPanel extends JPanel {

    private ConversationItem currentConversation;
    private final List<ChatMessage> messages = new ArrayList<>();

    // Các thành phần trên Header
    private final AvatarCircle headerAvatar;
    private final JLabel headerNameLabel;
    private final JLabel headerBadge;
    private final JLabel pinnedIcon;
    private final JLabel mutedIcon;
    private final JLabel statusDot;
    private final JLabel statusText;
    private final JButton btnHeaderAddMember;
    private final JButton btnSearchChat;
    private final JButton btnToggleInfo;

    // Thanh tìm kiếm tin nhắn trong hội thoại (In-chat Search Bar)
    private final JPanel searchBarPanel;
    private final JTextField inChatSearchField;
    private String inChatQuery = "";

    // Vùng chứa tin nhắn cuộn
    private final JPanel messagesContainer;
    private final JScrollPane scrollPane;

    // Vùng soạn thảo tin nhắn
    private final JTextField messageInput;
    private final Map<String, String> conversationDrafts = new HashMap<>();
    private final JButton btnSend;
    private final JButton btnEmoji;
    private JPanel header;
    private JPanel footer;
    private RoundedPanel composerBar;

    // Callbacks ra ngoài
    private Consumer<String> onSendMessage;
    private Runnable onToggleInfoDrawer;
    private Runnable onAddMemberClick;

    public ChatPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.WINDOW_BG);

        // ===================== HEADER =====================
        JPanel headerContainer = new JPanel();
        headerContainer.setLayout(new BoxLayout(headerContainer, BoxLayout.Y_AXIS));
        headerContainer.setOpaque(false);

        header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.WINDOW_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_LIGHT),
                new EmptyBorder(8, 16, 10, 16)
        ));

        // Bên trái header: Avatar + Tên + Badge + Pinned + Muted
        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftHeader.setOpaque(false);

        headerAvatar = new AvatarCircle("?", 36, UITheme.THEME_PRIMARY, Color.WHITE);
        leftHeader.add(headerAvatar);

        headerNameLabel = new JLabel("Chọn một hội thoại");
        headerNameLabel.setFont(UITheme.FONT_HEADING);
        headerNameLabel.setForeground(UITheme.TEXT_MAIN);
        leftHeader.add(headerNameLabel);

        headerBadge = new JLabel("Cá nhân");
        headerBadge.setFont(UITheme.FONT_SMALL);
        headerBadge.setForeground(UITheme.TEXT_MUTED);
        headerBadge.setBackground(UITheme.SIDEBAR_CARD_BG);
        headerBadge.setOpaque(true);
        headerBadge.setBorder(new EmptyBorder(3, 8, 3, 8));
        leftHeader.add(headerBadge);

        // Biểu tượng Ghim & Tắt thông báo
        pinnedIcon = new JLabel(new VectorIcon(VectorIcon.Type.PIN, 13, UITheme.THEME_PRIMARY));
        pinnedIcon.setToolTipText("Đã ghim hội thoại");
        pinnedIcon.setVisible(false);
        leftHeader.add(pinnedIcon);

        mutedIcon = new JLabel(new VectorIcon(VectorIcon.Type.BELL, 13, UITheme.TEXT_MUTED));
        mutedIcon.setToolTipText("Đã tắt thông báo");
        mutedIcon.setVisible(false);
        leftHeader.add(mutedIcon);

        header.add(leftHeader, BorderLayout.WEST);

        // Bên phải header: Trạng thái Online + Nút Thêm người + Tìm tin + Nút 3 chấm
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHeader.setOpaque(false);

        JPanel statusWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        statusWrap.setOpaque(false);

        statusDot = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.ONLINE_GREEN);
                g2.fill(new Ellipse2D.Float(0, 4, 8, 8));
                g2.dispose();
            }
        };
        statusDot.setPreferredSize(new Dimension(8, 16));

        statusText = new JLabel("Đang hoạt động");
        statusText.setFont(UITheme.FONT_SMALL);
        statusText.setForeground(UITheme.TEXT_MUTED);

        statusWrap.add(statusDot);
        statusWrap.add(statusText);
        rightHeader.add(statusWrap);

        // Nút thêm thành viên vào nhóm (cho nhóm chat)
        btnHeaderAddMember = new JButton(new VectorIcon(VectorIcon.Type.ADD_GROUP, 16, UITheme.THEME_PRIMARY));
        btnHeaderAddMember.setPreferredSize(new Dimension(28, 28));
        btnHeaderAddMember.setBackground(Color.WHITE);
        btnHeaderAddMember.setBorder(BorderFactory.createEmptyBorder());
        btnHeaderAddMember.setFocusPainted(false);
        btnHeaderAddMember.setContentAreaFilled(false);
        btnHeaderAddMember.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnHeaderAddMember.setToolTipText("Thêm thành viên vào nhóm");
        btnHeaderAddMember.setVisible(false);
        btnHeaderAddMember.addActionListener(e -> {
            if (onAddMemberClick != null) onAddMemberClick.run();
        });
        rightHeader.add(btnHeaderAddMember);

        // Nút tìm tin nhắn trong cuộc trò chuyện
        btnSearchChat = new JButton(new VectorIcon(VectorIcon.Type.SEARCH, 15, UITheme.TEXT_MUTED));
        btnSearchChat.setPreferredSize(new Dimension(28, 28));
        btnSearchChat.setBackground(Color.WHITE);
        btnSearchChat.setBorder(BorderFactory.createEmptyBorder());
        btnSearchChat.setFocusPainted(false);
        btnSearchChat.setContentAreaFilled(false);
        btnSearchChat.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearchChat.setToolTipText("Tìm kiếm nội dung tin nhắn");
        btnSearchChat.addActionListener(e -> toggleInChatSearch());
        rightHeader.add(btnSearchChat);

        // Nút ba chấm ⋮ (mở/đóng InfoDrawer)
        btnToggleInfo = new JButton(new VectorIcon(VectorIcon.Type.MORE_VERTICAL, 16, UITheme.TEXT_MUTED));
        btnToggleInfo.setPreferredSize(new Dimension(28, 28));
        btnToggleInfo.setBackground(Color.WHITE);
        btnToggleInfo.setBorder(BorderFactory.createEmptyBorder());
        btnToggleInfo.setFocusPainted(false);
        btnToggleInfo.setContentAreaFilled(false);
        btnToggleInfo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToggleInfo.setToolTipText("Tùy chọn & Thông tin hội thoại");
        btnToggleInfo.addActionListener(e -> {
            if (onToggleInfoDrawer != null) onToggleInfoDrawer.run();
        });
        rightHeader.add(btnToggleInfo);

        header.add(rightHeader, BorderLayout.EAST);
        headerContainer.add(header);

        // Thanh tìm kiếm tin nhắn trong hội thoại (In-Chat Search Bar, mặc định ẩn)
        searchBarPanel = new JPanel(new BorderLayout(8, 0));
        searchBarPanel.setBackground(new Color(248, 250, 252));
        searchBarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_LIGHT),
                new EmptyBorder(6, 16, 6, 16)
        ));
        searchBarPanel.setVisible(false);

        JLabel searchPrompt = new JLabel("Tìm tin nhắn:");
        searchPrompt.setFont(UITheme.FONT_XS);
        searchPrompt.setForeground(UITheme.TEXT_MUTED);
        searchBarPanel.add(searchPrompt, BorderLayout.WEST);

        inChatSearchField = new JTextField();
        inChatSearchField.setFont(UITheme.FONT_SMALL);
        inChatSearchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_LIGHT),
                new EmptyBorder(3, 6, 3, 6)
        ));
        inChatSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterMessages(); }
            @Override public void removeUpdate(DocumentEvent e) { filterMessages(); }
            @Override public void changedUpdate(DocumentEvent e) { filterMessages(); }
        });
        searchBarPanel.add(inChatSearchField, BorderLayout.CENTER);

        JButton btnCloseSearch = new JButton("Đóng");
        btnCloseSearch.setFont(UITheme.FONT_XS);
        btnCloseSearch.setForeground(UITheme.THEME_PRIMARY);
        btnCloseSearch.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
        btnCloseSearch.setContentAreaFilled(false);
        btnCloseSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCloseSearch.addActionListener(e -> toggleInChatSearch());
        searchBarPanel.add(btnCloseSearch, BorderLayout.EAST);

        headerContainer.add(searchBarPanel);
        add(headerContainer, BorderLayout.NORTH);

        // ===================== MESSAGES LOG =====================
        messagesContainer = new JPanel();
        messagesContainer.setLayout(new BoxLayout(messagesContainer, BoxLayout.Y_AXIS));
        messagesContainer.setBackground(UITheme.WINDOW_BG);
        messagesContainer.setBorder(new EmptyBorder(12, 10, 12, 10));

        scrollPane = new JScrollPane(messagesContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.WINDOW_BG);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        add(scrollPane, BorderLayout.CENTER);

        // ===================== COMPOSER =====================
        footer = new JPanel(new BorderLayout(8, 0));
        footer.setBackground(UITheme.WINDOW_BG);
        footer.setBorder(new EmptyBorder(10, 14, 14, 14));

        composerBar = new RoundedPanel(12, UITheme.BORDER_LIGHT);
        composerBar.setBackground(UITheme.WINDOW_BG);
        composerBar.setLayout(new BorderLayout(8, 0));
        composerBar.setBorder(new EmptyBorder(4, 10, 4, 8));

        // Nút mở bảng Emoji
        btnEmoji = new JButton(new VectorIcon(VectorIcon.Type.SMILE, 20, UITheme.TEXT_MUTED));
        btnEmoji.setPreferredSize(new Dimension(28, 28));
        btnEmoji.setBorder(BorderFactory.createEmptyBorder());
        btnEmoji.setBackground(Color.WHITE);
        btnEmoji.setFocusPainted(false);
        btnEmoji.setContentAreaFilled(false);
        btnEmoji.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEmoji.setToolTipText("Chèn biểu tượng cảm xúc");
        btnEmoji.addActionListener(e -> showEmojiMenu(btnEmoji));
        composerBar.add(btnEmoji, BorderLayout.WEST);

        messageInput = new JTextField();
        messageInput.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        messageInput.setFont(UITheme.FONT_CHAT_TEXT);
        messageInput.setOpaque(false);
        messageInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    handleSend();
                }
            }
        });
        messageInput.addActionListener(e -> handleSend());
        composerBar.add(messageInput, BorderLayout.CENTER);

        btnSend = new JButton("Gửi", new VectorIcon(VectorIcon.Type.SEND, 14, Color.WHITE)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnSend.setFont(UITheme.FONT_MEDIUM);
        btnSend.setBackground(UITheme.THEME_PRIMARY);
        btnSend.setForeground(Color.WHITE);
        btnSend.setIconTextGap(6);
        btnSend.setFocusPainted(false);
        btnSend.setContentAreaFilled(false);
        btnSend.setOpaque(false);
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSend.setBorder(new EmptyBorder(6, 16, 6, 16));
        btnSend.addActionListener(e -> handleSend());
        composerBar.add(btnSend, BorderLayout.EAST);

        footer.add(composerBar, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    public void setConversation(ConversationItem conv) {
        String previousId = currentConversation == null ? null : currentConversation.getId();
        String nextId = conv == null ? null : conv.getId();
        if (!Objects.equals(previousId, nextId)) {
            String text = messageInput.getText();
            if (previousId != null && !text.isBlank()) {
                conversationDrafts.put(previousId, text);
            }
            String nextDraft = nextId == null ? null : conversationDrafts.remove(nextId);
            messageInput.setText(nextDraft == null ? "" : nextDraft);
        }
        this.currentConversation = conv;
        boolean blocked = conv != null && conv.isSendBlocked();
        messageInput.setEditable(!blocked);
        btnSend.setEnabled(!blocked);
        btnEmoji.setEnabled(!blocked);
        String blockedHint = blocked ? "Hội thoại đang bị chặn." : null;
        messageInput.setToolTipText(blockedHint);
        btnSend.setToolTipText(blockedHint);
        if (conv == null) {
            headerNameLabel.setText("Chọn một hội thoại");
            headerAvatar.setText("?");
            headerAvatar.setImageUrl(null);
            headerBadge.setVisible(false);
            btnHeaderAddMember.setVisible(false);
            pinnedIcon.setVisible(false);
            mutedIcon.setVisible(false);
            messages.clear();
            refreshMessagesUI();
            return;
        }

        headerNameLabel.setText(conv.getName() != null ? conv.getName() : "Hội thoại");
        headerAvatar.setText(conv.getDisplayInitials());
        headerAvatar.setImageUrl(conv.getAvatarUrl());
        boolean isGroup = "GROUP".equalsIgnoreCase(conv.getType());
        headerBadge.setText(isGroup ? "Nhóm" : "Cá nhân");
        headerBadge.setVisible(true);

        btnHeaderAddMember.setVisible(isGroup);
        pinnedIcon.setVisible(conv.isPinned());
        mutedIcon.setVisible(conv.isMuted());

        this.messages.clear();
        if (conv.getMessages() != null) {
            this.messages.addAll(conv.getMessages());
        }
        refreshMessagesUI();
    }

    public void addMessage(ChatMessage msg) {
        if (!messages.contains(msg)) {
            messages.add(msg);
        }
        messagesContainer.add(new MessageBubble(msg));
        messagesContainer.add(Box.createVerticalStrut(4));
        messagesContainer.revalidate();
        messagesContainer.repaint();
        if (scrollPane != null) {
            scrollPane.revalidate();
            scrollPane.repaint();
        }
        scrollToBottom();
    }

    public void setMessages(List<ChatMessage> list) {
        this.messages.clear();
        if (list != null) {
            this.messages.addAll(list);
        }
        refreshMessagesUI();
    }

    private void filterMessages() {
        inChatQuery = inChatSearchField.getText().trim().toLowerCase();
        refreshMessagesUI();
    }

    private void toggleInChatSearch() {
        boolean visible = !searchBarPanel.isVisible();
        searchBarPanel.setVisible(visible);
        if (visible) {
            inChatSearchField.requestFocusInWindow();
        } else {
            inChatSearchField.setText("");
            inChatQuery = "";
            refreshMessagesUI();
        }
        revalidate();
        repaint();
    }

    private void refreshMessagesUI() {
        messagesContainer.removeAll();
        for (ChatMessage msg : messages) {
            if (!inChatQuery.isEmpty()) {
                String c = msg.getContent() != null ? msg.getContent().toLowerCase() : "";
                if (!c.contains(inChatQuery)) continue;
            }
            messagesContainer.add(new MessageBubble(msg));
            messagesContainer.add(Box.createVerticalStrut(4));
        }
        messagesContainer.revalidate();
        messagesContainer.repaint();
        if (scrollPane != null) {
            scrollPane.revalidate();
            scrollPane.repaint();
        }
        scrollToBottom();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
            SwingUtilities.invokeLater(() -> {
                vertical.setValue(vertical.getMaximum());
            });
        });
    }

    private void handleSend() {
        String text = messageInput.getText().trim();
        if (text.isEmpty() || currentConversation == null || currentConversation.isSendBlocked() || onSendMessage == null) {
            return;
        }
        messageInput.setText("");
        onSendMessage.accept(text);
    }

    public void restoreDraftIfEmpty(String conversationId, String text) {
        if (currentConversation != null && conversationId.equals(currentConversation.getId())) {
            if (messageInput.getText().isEmpty()) messageInput.setText(text);
        } else {
            conversationDrafts.putIfAbsent(conversationId, text);
        }
    }

    /**
     * Menu Emoji chuyên nghiệp với 8 Vector Emoji màu sắc (khớp 100% web HTML)
     */
    private void showEmojiMenu(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1),
                new EmptyBorder(6, 6, 6, 6)
        ));

        EmojiIcon.Type[] types = {
                EmojiIcon.Type.LIKE,
                EmojiIcon.Type.HEART,
                EmojiIcon.Type.LAUGH,
                EmojiIcon.Type.WOW,
                EmojiIcon.Type.SAD,
                EmojiIcon.Type.FIRE,
                EmojiIcon.Type.PARTY,
                EmojiIcon.Type.CLAP
        };
        String[] textInserts = {"👍", "❤️", "😂", "😮", "😢", "🔥", "🎉", "🙌"};
        String[] tooltips = {
                "Thích (Like)", "Yêu thích (Heart)", "Cười vui (Haha)", "Ngạc nhiên (Wow)",
                "Buồn (Sad)", "Tuyệt vời (Fire)", "Ăn mừng (Party)", "Vỗ tay (Clap)"
        };

        JPanel p = new JPanel(new GridLayout(2, 4, 8, 8));
        p.setOpaque(false);

        for (int i = 0; i < types.length; i++) {
            final EmojiIcon icon = new EmojiIcon(types[i], 26);
            JButton b = new JButton(icon) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                    // Vẽ icon chính giữa
                    int ix = (getWidth() - icon.getIconWidth()) / 2;
                    int iy = (getHeight() - icon.getIconHeight()) / 2;
                    icon.paintIcon(this, g, ix, iy);
                }
            };
            b.setText(null); // Đảm bảo KHÔNG có text nào
            b.setPreferredSize(new Dimension(40, 40));
            b.setMinimumSize(new Dimension(40, 40));
            b.setBackground(Color.WHITE);
            b.setBorder(BorderFactory.createLineBorder(new Color(241, 245, 249), 1));
            b.setFocusPainted(false);
            b.setContentAreaFilled(false); // Tắt vẽ nền mặc định, ta vẽ thủ công
            b.setOpaque(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            b.setToolTipText(tooltips[i]);
            b.getAccessibleContext().setAccessibleName(tooltips[i]);

            final JButton btnRef = b;
            b.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { btnRef.setBackground(UITheme.SIDEBAR_CARD_BG); btnRef.repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { btnRef.setBackground(Color.WHITE); btnRef.repaint(); }
            });

            final String emo = textInserts[i];
            b.addActionListener(ev -> {
                messageInput.replaceSelection(emo);
                menu.setVisible(false);
                messageInput.requestFocusInWindow();
            });
            p.add(b);
        }

        menu.add(p);
        menu.show(invoker, 0, -menu.getPreferredSize().height - 8);
    }

    public void setOnSendMessage(Consumer<String> callback) { this.onSendMessage = callback; }
    public void setOnToggleInfoDrawer(Runnable callback) { this.onToggleInfoDrawer = callback; }
    public void setOnAddMemberClick(Runnable callback) { this.onAddMemberClick = callback; }
    public ConversationItem getCurrentConversation() { return currentConversation; }

    public void applyTheme() {
        setBackground(UITheme.WINDOW_BG);
        if (header != null) {
            header.setBackground(UITheme.WINDOW_BG);
            header.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_LIGHT),
                    new EmptyBorder(8, 16, 10, 16)
            ));
        }
        if (headerNameLabel != null) headerNameLabel.setForeground(UITheme.TEXT_MAIN);
        if (headerBadge != null) {
            headerBadge.setBackground(UITheme.SIDEBAR_CARD_BG);
            headerBadge.setForeground(UITheme.TEXT_MUTED);
        }
        if (statusText != null) statusText.setForeground(UITheme.TEXT_MUTED);
        if (messagesContainer != null) messagesContainer.setBackground(UITheme.WINDOW_BG);
        if (scrollPane != null && scrollPane.getViewport() != null) {
            scrollPane.getViewport().setBackground(UITheme.WINDOW_BG);
        }
        if (searchBarPanel != null) {
            searchBarPanel.setBackground(UITheme.SIDEBAR_CARD_BG);
            searchBarPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_LIGHT),
                    new EmptyBorder(6, 16, 6, 16)
            ));
        }
        if (footer != null) footer.setBackground(UITheme.WINDOW_BG);
        if (composerBar != null) {
            composerBar.setBackground(UITheme.isDarkMode ? UITheme.SIDEBAR_CARD_BG : UITheme.WINDOW_BG);
            composerBar.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        }
        if (messageInput != null) {
            messageInput.setForeground(UITheme.TEXT_MAIN);
            messageInput.setCaretColor(UITheme.TEXT_MAIN);
        }
        refreshMessagesUI();
        repaint();
    }
}

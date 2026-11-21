package com.teamchat.swing.ui.panels;

import com.teamchat.swing.model.ConversationItem;
import com.teamchat.swing.ui.UITheme;
import com.teamchat.swing.ui.components.ConversationCell;
import com.teamchat.swing.ui.components.RoundedPanel;
import com.teamchat.swing.ui.components.VectorIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * =========================================================================
 * FILE: SidebarPanel.java - CỘT TRÁI HIỂN THỊ DANH SÁCH CUỘC TRÒ CHUYỆN
 * =========================================================================
 */
public class SidebarPanel extends RoundedPanel {

    private final DefaultListModel<ConversationItem> listModel = new DefaultListModel<>();
    private final JList<ConversationItem> conversationList = new JList<>(listModel);
    private final List<ConversationItem> allConversations = new ArrayList<>();

    private JTextField searchField;
    private String currentFilter = "all";

    private Consumer<ConversationItem> onConversationSelected;
    private Runnable onCreateGroupClick;
    private Runnable onFriendsClick;

    private JButton btnFilterAll;
    private JButton btnFilterGroup;
    private JButton btnFilterUnread;
    private JButton btnFilterArchived;
    private JLabel heading;

    public SidebarPanel() {
        super(14);
        setBackground(UITheme.SIDEBAR_CARD_BG);
        setLayout(new BorderLayout(0, 10));
        setPreferredSize(new Dimension(310, 600));
        setBorder(new EmptyBorder(16, 12, 16, 12));

        initUI();
    }

    private void initUI() {
        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        // 1. DÒNG TIÊU ĐỀ: "Hội thoại" + Các nút hành động
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        heading = new JLabel("Hội thoại");
        heading.setFont(UITheme.FONT_HEADING);
        heading.setForeground(UITheme.TEXT_MAIN);
        titleRow.add(heading, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setOpaque(false);

        // Friends button - VectorIcon FRIENDS
        JButton btnFriends = createActionButton(VectorIcon.Type.FRIENDS, "Bạn bè & Lời mời kết bạn");
        btnFriends.addActionListener(e -> {
            if (onFriendsClick != null) onFriendsClick.run();
        });

        // Create group button - VectorIcon ADD_GROUP
        JButton btnCreateGroup = createActionButton(VectorIcon.Type.ADD_GROUP, "Tạo nhóm chat mới");
        btnCreateGroup.addActionListener(e -> {
            if (onCreateGroupClick != null) onCreateGroupClick.run();
        });

        actions.add(btnFriends);
        actions.add(btnCreateGroup);
        titleRow.add(actions, BorderLayout.EAST);

        topSection.add(titleRow);
        topSection.add(Box.createVerticalStrut(10));

        // 2. Ô TÌM KIẾM HỘI THOẠI
        JPanel searchWrap = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.isDarkMode ? new Color(0x27, 0x35, 0x49) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        searchWrap.setOpaque(false);
        searchWrap.setBorder(new EmptyBorder(2, 10, 2, 8));
        searchWrap.setMaximumSize(new Dimension(290, 36));
        searchWrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Search icon (VectorIcon SEARCH)
        JLabel searchIcon = new JLabel(new VectorIcon(VectorIcon.Type.SEARCH, 16, UITheme.TEXT_MUTED));

        searchField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(148, 163, 184));
                    g2.setFont(getFont());
                    Insets ins = getInsets();
                    g2.drawString("Tìm kiếm hội thoại...", ins.left, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 1);
                    g2.dispose();
                }
            }
        };
        searchField.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        searchField.setFont(UITheme.FONT_REGULAR);
        searchField.setOpaque(false);

        // Nút xóa tìm kiếm (Khớp #btnClearSearch trong HTML)
        JButton btnClearSearch = new JButton(new VectorIcon(VectorIcon.Type.BLOCK, 12, UITheme.TEXT_MUTED)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(203, 213, 225));
                g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.drawLine(cx - 3, cy - 3, cx + 3, cy + 3);
                g2.drawLine(cx + 3, cy - 3, cx - 3, cy + 3);
                g2.dispose();
            }
        };
        btnClearSearch.setPreferredSize(new Dimension(18, 18));
        btnClearSearch.setBorder(BorderFactory.createEmptyBorder());
        btnClearSearch.setContentAreaFilled(false);
        btnClearSearch.setFocusPainted(false);
        btnClearSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearSearch.setToolTipText("Xóa tìm kiếm");
        btnClearSearch.setVisible(false);
        btnClearSearch.addActionListener(e -> {
            searchField.setText("");
            searchField.requestFocusInWindow();
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void updateClearBtn() {
                btnClearSearch.setVisible(!searchField.getText().trim().isEmpty());
            }
            @Override public void insertUpdate(DocumentEvent e) { filterList(); updateClearBtn(); }
            @Override public void removeUpdate(DocumentEvent e) { filterList(); updateClearBtn(); }
            @Override public void changedUpdate(DocumentEvent e) { filterList(); updateClearBtn(); }
        });

        searchWrap.add(searchIcon, BorderLayout.WEST);
        searchWrap.add(searchField, BorderLayout.CENTER);
        searchWrap.add(btnClearSearch, BorderLayout.EAST);
        topSection.add(searchWrap);
        topSection.add(Box.createVerticalStrut(10));

        // 3. 4 NÚT TABS BỘ LỌC
        JPanel tabsPanel = new JPanel(new GridLayout(1, 4, 4, 0));
        tabsPanel.setOpaque(false);
        tabsPanel.setMaximumSize(new Dimension(290, 32));
        tabsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnFilterAll = createTabPill("Tất cả", true);
        btnFilterGroup = createTabPill("Nhóm", false);
        btnFilterUnread = createTabPill("Chưa đọc", false);
        btnFilterArchived = createTabPill("Lưu trữ", false);

        btnFilterAll.addActionListener(e -> setFilter("all"));
        btnFilterGroup.addActionListener(e -> setFilter("GROUP"));
        btnFilterUnread.addActionListener(e -> setFilter("unread"));
        btnFilterArchived.addActionListener(e -> setFilter("archived"));

        tabsPanel.add(btnFilterAll);
        tabsPanel.add(btnFilterGroup);
        tabsPanel.add(btnFilterUnread);
        tabsPanel.add(btnFilterArchived);

        topSection.add(tabsPanel);
        add(topSection, BorderLayout.NORTH);

        // 4. DANH SÁCH CUỘC TRÒ CHUYỆN
        conversationList.setCellRenderer(new ConversationCell());
        conversationList.setBackground(UITheme.SIDEBAR_CARD_BG);
        conversationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conversationList.setBorder(BorderFactory.createEmptyBorder());

        conversationList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ConversationItem selected = conversationList.getSelectedValue();
                if (selected != null && onConversationSelected != null) {
                    onConversationSelected.accept(selected);
                }
            }
        });

        // Bắt chuột phải để mở menu ngữ cảnh (Context Menu giống web)
        conversationList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = conversationList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        conversationList.setSelectedIndex(index);
                        showContextMenu(conversationList.getModel().getElementAt(index), e.getX(), e.getY());
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(conversationList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(5, 0));
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setConversations(List<ConversationItem> items) {
        this.allConversations.clear();
        if (items != null) {
            this.allConversations.addAll(items);
        }
        filterList();
        if (!listModel.isEmpty() && conversationList.getSelectedIndex() < 0) {
            conversationList.setSelectedIndex(0);
        }
    }

    public void updateConversation(ConversationItem updated) {
        for (int i = 0; i < allConversations.size(); i++) {
            if (allConversations.get(i).getId() == updated.getId()) {
                allConversations.set(i, updated);
                break;
            }
        }
        filterList();
    }

    public void selectConversation(long convId) {
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i).getId() == convId) {
                conversationList.setSelectedIndex(i);
                break;
            }
        }
    }

    public void applyTheme() {
        setBackground(UITheme.SIDEBAR_CARD_BG);
        conversationList.setBackground(UITheme.SIDEBAR_CARD_BG);
        if (heading != null) heading.setForeground(UITheme.TEXT_MAIN);
        if (searchField != null) searchField.setForeground(UITheme.TEXT_MAIN);
        setFilter(currentFilter);
        filterList();
        repaint();
    }

    public void setOnConversationSelected(Consumer<ConversationItem> callback) {
        this.onConversationSelected = callback;
    }

    public void setOnCreateGroupClick(Runnable callback) { this.onCreateGroupClick = callback; }
    public void setOnFriendsClick(Runnable callback) { this.onFriendsClick = callback; }

    private void setFilter(String filter) {
        this.currentFilter = filter;
        updateTabPill(btnFilterAll, "all".equals(filter));
        updateTabPill(btnFilterGroup, "GROUP".equals(filter));
        updateTabPill(btnFilterUnread, "unread".equals(filter));
        updateTabPill(btnFilterArchived, "archived".equals(filter));
        filterList();
    }

    private void filterList() {
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        ConversationItem currentSelected = conversationList.getSelectedValue();

        listModel.clear();
        for (ConversationItem item : allConversations) {
            if ("GROUP".equals(currentFilter) && !"GROUP".equalsIgnoreCase(item.getType())) continue;
            if ("unread".equals(currentFilter) && item.getUnreadCount() <= 0) continue;
            if ("archived".equals(currentFilter) && !item.isArchived()) continue;
            if (!"archived".equals(currentFilter) && item.isArchived()) continue;

            if (!query.isEmpty()) {
                String name = item.getName() != null ? item.getName().toLowerCase() : "";
                String last = item.getLastMessage() != null ? item.getLastMessage().toLowerCase() : "";
                if (!name.contains(query) && !last.contains(query)) continue;
            }

            listModel.addElement(item);
        }

        if (currentSelected != null) {
            for (int i = 0; i < listModel.size(); i++) {
                if (listModel.get(i).getId() == currentSelected.getId()) {
                    conversationList.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private JButton createActionButton(VectorIcon.Type type, String tooltip) {
        JButton btn = new JButton(new VectorIcon(type, 16, UITheme.THEME_PRIMARY));
        btn.setPreferredSize(new Dimension(32, 32));
        btn.setBackground(new Color(29, 114, 184, 25));
        btn.setBorder(BorderFactory.createLineBorder(new Color(29, 114, 184, 45)));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setToolTipText(tooltip);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createTabPill(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(UITheme.FONT_MEDIUM);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        updateTabPill(btn, active);
        return btn;
    }

    private void updateTabPill(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(UITheme.isDarkMode ? new Color(0x27, 0x35, 0x49) : Color.WHITE);
            btn.setForeground(UITheme.THEME_PRIMARY);
            btn.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        } else {
            btn.setBackground(UITheme.SIDEBAR_CARD_BG);
            btn.setForeground(UITheme.TEXT_MUTED);
            btn.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
        }
    }

    /**
     * Menu chuột phải vào hội thoại (Khớp 100% #conversationContextMenu trong HTML)
     */
    private void showContextMenu(ConversationItem item, int x, int y) {
        if (item == null) return;
        JPopupMenu menu = new JPopupMenu();

        JMenuItem mPin = new JMenuItem(item.isPinned() ? "Bỏ ghim hội thoại" : "Ghim hội thoại",
                new VectorIcon(VectorIcon.Type.PIN, 14, UITheme.TEXT_MAIN));
        mPin.addActionListener(e -> {
            item.setPinned(!item.isPinned());
            filterList();
        });

        JMenuItem mUnread = new JMenuItem(item.getUnreadCount() > 0 ? "Đánh dấu đã đọc" : "Đánh dấu chưa đọc",
                new VectorIcon(VectorIcon.Type.CHAT, 14, UITheme.TEXT_MAIN));
        mUnread.addActionListener(e -> {
            item.setUnreadCount(item.getUnreadCount() > 0 ? 0 : 1);
            filterList();
        });

        JMenuItem mMute = new JMenuItem(item.isMuted() ? "Bật thông báo" : "Tắt thông báo",
                new VectorIcon(VectorIcon.Type.BELL, 14, UITheme.TEXT_MAIN));
        mMute.addActionListener(e -> {
            item.setMuted(!item.isMuted());
            filterList();
        });

        JMenuItem mArchive = new JMenuItem(item.isArchived() ? "Bỏ lưu trữ" : "Lưu trữ",
                new VectorIcon(VectorIcon.Type.ARCHIVE, 14, UITheme.TEXT_MAIN));
        mArchive.addActionListener(e -> {
            item.setArchived(!item.isArchived());
            filterList();
        });

        JMenuItem mBlock = new JMenuItem("Chặn hội thoại",
                new VectorIcon(VectorIcon.Type.BLOCK, 14, UITheme.DANGER_RED));
        mBlock.setForeground(UITheme.DANGER_RED);
        mBlock.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Đã cập nhật trạng thái chặn hội thoại.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });

        JMenuItem mDelete = new JMenuItem("Xóa hội thoại",
                new VectorIcon(VectorIcon.Type.TRASH, 14, UITheme.DANGER_RED));
        mDelete.setForeground(UITheme.DANGER_RED);
        mDelete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa hội thoại '" + item.getName() + "'?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                allConversations.remove(item);
                filterList();
            }
        });

        menu.add(mPin);
        menu.add(mUnread);
        menu.add(mMute);
        menu.add(mArchive);
        menu.addSeparator();
        menu.add(mBlock);
        menu.add(mDelete);

        menu.show(conversationList, x, y);
    }
}

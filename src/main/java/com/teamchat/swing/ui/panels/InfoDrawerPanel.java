package com.teamchat.swing.ui.panels;

import com.teamchat.swing.model.ConversationItem;
import com.teamchat.swing.ui.UITheme;
import com.teamchat.swing.ui.components.AvatarCircle;
import com.teamchat.swing.ui.components.VectorIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Right drawer panel showing conversation details, actions, and members list,
 * matching ChatSpace web design with professional vector icons.
 */
public class InfoDrawerPanel extends JPanel {

    private ConversationItem conversation;

    private final AvatarCircle avatar;
    private final JLabel nameLabel;
    private final JLabel statusLabel;

    private final JButton btnPin;
    private final JButton btnBlock;
    private final JButton btnMute;
    private final JButton btnArchive;

    private final JPanel membersSection;
    private final JPanel membersListPanel;
    private final JLabel memberCountLabel;

    private Consumer<ConversationItem> onArchiveToggle;
    private Consumer<ConversationItem> onRenameConversation;
    private Consumer<ConversationItem> onDeleteConversation;
    private Consumer<ConversationItem> onPinToggle;
    private Consumer<ConversationItem> onMuteToggle;
    private Consumer<ConversationItem> onBlockToggle;
    private Runnable onAddMemberClick;

    public InfoDrawerPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.WINDOW_BG);
        setPreferredSize(new Dimension(260, 600));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, UITheme.BORDER_LIGHT),
                new EmptyBorder(14, 14, 14, 14)
        ));

        // Top: Profile details
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Thông tin hội thoại");
        title.setFont(UITheme.FONT_HEADING);
        title.setForeground(UITheme.TEXT_MAIN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(title);
        topPanel.add(Box.createVerticalStrut(14));

        avatar = new AvatarCircle("?", 56, UITheme.THEME_PRIMARY, Color.WHITE);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(avatar);
        topPanel.add(Box.createVerticalStrut(8));

        nameLabel = new JLabel("Tên hội thoại");
        nameLabel.setFont(UITheme.FONT_HEADING);
        nameLabel.setForeground(UITheme.TEXT_MAIN);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(nameLabel);
        topPanel.add(Box.createVerticalStrut(2));

        statusLabel = new JLabel("Đang hoạt động");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(statusLabel);
        topPanel.add(Box.createVerticalStrut(14));

        // Quick Actions Row (Ghim, Tắt TB, Lưu trữ) with VectorIcon
        JPanel quickActions = new JPanel(new GridLayout(1, 3, 6, 0));
        quickActions.setOpaque(false);
        quickActions.setMaximumSize(new Dimension(240, 52));

        btnPin = createQuickActionBtn(VectorIcon.Type.PIN, "Ghim");
        btnMute = createQuickActionBtn(VectorIcon.Type.BELL, "Tắt TB");
        btnArchive = createQuickActionBtn(VectorIcon.Type.ARCHIVE, "Lưu trữ");

        btnPin.addActionListener(e -> togglePin());
        btnMute.addActionListener(e -> toggleMute());
        btnArchive.addActionListener(e -> toggleArchive());

        quickActions.add(btnPin);
        quickActions.add(btnMute);
        quickActions.add(btnArchive);
        topPanel.add(quickActions);
        topPanel.add(Box.createVerticalStrut(14));

        add(topPanel, BorderLayout.NORTH);

        // Center: Members Section (if group)
        membersSection = new JPanel();
        membersSection.setLayout(new BoxLayout(membersSection, BoxLayout.Y_AXIS));
        membersSection.setOpaque(false);

        JPanel memberHeader = new JPanel(new BorderLayout());
        memberHeader.setOpaque(false);
        memberHeader.setMaximumSize(new Dimension(240, 24));

        memberCountLabel = new JLabel("Thành viên");
        memberCountLabel.setFont(UITheme.FONT_BOLD_SMALL);
        memberCountLabel.setForeground(UITheme.TEXT_MAIN);

        JButton btnAddMember = new JButton("+ Thêm");
        btnAddMember.setFont(UITheme.FONT_SMALL);
        btnAddMember.setForeground(UITheme.THEME_PRIMARY);
        btnAddMember.setBorder(BorderFactory.createEmptyBorder());
        btnAddMember.setContentAreaFilled(false);
        btnAddMember.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddMember.addActionListener(e -> {
            if (onAddMemberClick != null) onAddMemberClick.run();
        });

        memberHeader.add(memberCountLabel, BorderLayout.WEST);
        memberHeader.add(btnAddMember, BorderLayout.EAST);
        membersSection.add(memberHeader);
        membersSection.add(Box.createVerticalStrut(8));

        membersListPanel = new JPanel();
        membersListPanel.setLayout(new BoxLayout(membersListPanel, BoxLayout.Y_AXIS));
        membersListPanel.setOpaque(false);

        JScrollPane memberScroll = new JScrollPane(membersListPanel);
        memberScroll.setBorder(BorderFactory.createEmptyBorder());
        memberScroll.setOpaque(false);
        memberScroll.getViewport().setOpaque(false);
        membersSection.add(memberScroll);

        add(membersSection, BorderLayout.CENTER);

        // Bottom: Privacy / Delete actions
        JPanel bottomActions = new JPanel();
        bottomActions.setLayout(new BoxLayout(bottomActions, BoxLayout.Y_AXIS));
        bottomActions.setOpaque(false);
        bottomActions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_LIGHT),
                new EmptyBorder(8, 0, 0, 0)
        ));

        JButton btnRename = createListActionBtn(VectorIcon.Type.SETTINGS, "Đổi tên hội thoại", UITheme.TEXT_MAIN);
        btnRename.addActionListener(e -> {
            if (conversation != null && onRenameConversation != null) onRenameConversation.accept(conversation);
        });

        btnBlock = createListActionBtn(VectorIcon.Type.BLOCK, "Chặn hội thoại", UITheme.DANGER_RED);
        btnBlock.addActionListener(e -> {
            if (conversation != null && conversation.isDirect() && onBlockToggle != null) {
                onBlockToggle.accept(conversation);
            }
        });

        JButton btnDelete = createListActionBtn(VectorIcon.Type.TRASH, "Xóa hội thoại", UITheme.DANGER_RED);
        btnDelete.addActionListener(e -> {
            if (conversation != null && onDeleteConversation != null) onDeleteConversation.accept(conversation);
        });

        bottomActions.add(btnRename);
        bottomActions.add(Box.createVerticalStrut(4));
        bottomActions.add(btnBlock);
        bottomActions.add(Box.createVerticalStrut(4));
        bottomActions.add(btnDelete);

        add(bottomActions, BorderLayout.SOUTH);
    }

    public void setConversation(ConversationItem item) {
        this.conversation = item;
        if (item == null) {
            avatar.setImageUrl(null);
            setVisible(false);
            return;
        }

        avatar.setText(item.getDisplayInitials());
        avatar.setImageUrl(item.getAvatarUrl());
        nameLabel.setText(item.getName() != null ? item.getName() : "Hội thoại");
        boolean isGroup = "GROUP".equalsIgnoreCase(item.getType());
        statusLabel.setText(isGroup ? "Nhóm chat" : "Đang hoạt động");

        btnPin.setText(item.isPinned() ? "Bỏ ghim" : "Ghim");
        btnBlock.setVisible(item.isDirect());
        btnBlock.setText(item.isBlocked() ? "Bỏ chặn hội thoại" : "Chặn hội thoại");
        btnMute.setText(item.isMuted() ? "Bật TB" : "Tắt TB");
        btnArchive.setText(item.isArchived() ? "Bỏ LT" : "Lưu trữ");

        membersSection.setVisible(isGroup);
        if (isGroup) {
            membersListPanel.removeAll();
            int count = item.getMembers() != null ? item.getMembers().size() : 0;
            memberCountLabel.setText("Thành viên (" + count + ")");

            if (item.getMembers() != null) {
                for (ConversationItem.MemberInfo m : item.getMembers()) {
                    JPanel mRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
                    mRow.setOpaque(false);
                    mRow.setMaximumSize(new Dimension(240, 28));

                    AvatarCircle mAv = new AvatarCircle(m.getDisplayInitials(), 20, UITheme.AVATAR_INACTIVE, Color.WHITE);
                    mAv.setImageUrl(m.getAvatarUrl());
                    JLabel mName = new JLabel(m.getName());
                    mName.setFont(UITheme.FONT_SMALL);

                    mRow.add(mAv);
                    mRow.add(mName);
                    membersListPanel.add(mRow);
                }
            }
            membersListPanel.revalidate();
            membersListPanel.repaint();
        }
    }

    private void togglePin() {
        if (conversation != null && onPinToggle != null) onPinToggle.accept(conversation);
    }

    private void toggleMute() {
        if (conversation != null && onMuteToggle != null) onMuteToggle.accept(conversation);
    }

    private void toggleArchive() {
        if (conversation != null && onArchiveToggle != null) onArchiveToggle.accept(conversation);
    }

    private JButton createQuickActionBtn(VectorIcon.Type type, String label) {
        JButton btn = new JButton(label, new VectorIcon(type, 18, UITheme.TEXT_MAIN));
        btn.setFont(UITheme.FONT_XS);
        btn.setBackground(UITheme.SIDEBAR_CARD_BG);
        btn.setForeground(UITheme.TEXT_MAIN);
        btn.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setIconTextGap(4);
        btn.setPreferredSize(new Dimension(72, 48));
        return btn;
    }

    private JButton createListActionBtn(VectorIcon.Type type, String text, Color fgColor) {
        JButton btn = new JButton(text, new VectorIcon(type, 16, fgColor));
        btn.setFont(UITheme.FONT_SMALL);
        btn.setForeground(fgColor);
        btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(8);
        btn.setMaximumSize(new Dimension(240, 32));
        return btn;
    }

    public void setOnAddMemberClick(Runnable callback) { this.onAddMemberClick = callback; }
    public void setOnPinToggle(Consumer<ConversationItem> callback) { this.onPinToggle = callback; }
    public void setOnMuteToggle(Consumer<ConversationItem> callback) { this.onMuteToggle = callback; }
    public void setOnArchiveToggle(Consumer<ConversationItem> callback) { this.onArchiveToggle = callback; }
    public void setOnRenameConversation(Consumer<ConversationItem> callback) { this.onRenameConversation = callback; }
    public void setOnDeleteConversation(Consumer<ConversationItem> callback) { this.onDeleteConversation = callback; }
    public void setOnBlockToggle(Consumer<ConversationItem> callback) { this.onBlockToggle = callback; }

    public void applyTheme() {
        setBackground(UITheme.WINDOW_BG);
        nameLabel.setForeground(UITheme.TEXT_MAIN);
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        memberCountLabel.setForeground(UITheme.TEXT_MAIN);
        repaint();
    }
}

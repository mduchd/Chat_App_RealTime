package com.teamchat.swing.ui.dialogs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teamchat.swing.api.ApiClient;
import com.teamchat.swing.model.ConversationItem;
import com.teamchat.swing.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Hộp thoại chọn bạn bè để tạo nhóm hoặc thêm vào nhóm đang mở.
 */
public class CreateGroupDialog extends JDialog {

    private final JTextField groupNameField;
    private final JTextField avatarTextField;
    private final JTextField searchMemberField;
    private final DefaultListModel<MemberCheckItem> memberListModel = new DefaultListModel<>();
    private final JList<MemberCheckItem> memberJList = new JList<>(memberListModel);
    private final JLabel selectedCountBadge;
    private final JScrollPane memberScroll;
    private final JButton btnSubmit;
    private final JButton btnCancel;
    private final ConversationItem existingGroup;

    private final List<MemberCheckItem> friends = new ArrayList<>();
    private final Set<String> selectedUserIds = new HashSet<>();
    private boolean friendsLoaded;
    private Consumer<String> onGroupSaved;

    public CreateGroupDialog(Frame parent) {
        this(parent, null);
    }

    public CreateGroupDialog(Frame parent, ConversationItem existingGroup) {
        super(parent, existingGroup == null ? "Tạo nhóm chat mới" : "Thêm thành viên", true);
        this.existingGroup = existingGroup;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(420, 520);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(UITheme.WINDOW_BG);
        content.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Form Section
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        JLabel lblName = new JLabel("Tên nhóm");
        lblName.setFont(UITheme.FONT_BOLD_SMALL);
        form.add(lblName);
        form.add(Box.createVerticalStrut(4));

        JPanel nameRow = new JPanel(new BorderLayout(8, 0));
        nameRow.setOpaque(false);
        nameRow.setMaximumSize(new Dimension(380, 34));

        groupNameField = new JTextField();
        groupNameField.setFont(UITheme.FONT_REGULAR);
        groupNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        avatarTextField = new JTextField("GR");
        avatarTextField.setPreferredSize(new Dimension(50, 34));
        avatarTextField.setFont(new Font("Segoe UI", Font.BOLD, 12));
        avatarTextField.setHorizontalAlignment(JTextField.CENTER);
        avatarTextField.setToolTipText("Ký hiệu Avatar");
        if (existingGroup != null) {
            groupNameField.setText(existingGroup.getName());
            groupNameField.setEditable(false);
            avatarTextField.setText(existingGroup.getDisplayInitials());
            avatarTextField.setEditable(false);
        }

        nameRow.add(groupNameField, BorderLayout.CENTER);
        nameRow.add(avatarTextField, BorderLayout.EAST);
        form.add(nameRow);
        form.add(Box.createVerticalStrut(12));

        // Member picker header
        JPanel pickHeader = new JPanel(new BorderLayout());
        pickHeader.setOpaque(false);
        pickHeader.setMaximumSize(new Dimension(380, 20));

        JLabel lblMembers = new JLabel("Chọn thành viên");
        lblMembers.setFont(UITheme.FONT_BOLD_SMALL);

        selectedCountBadge = new JLabel("Đã chọn: 0");
        selectedCountBadge.setFont(UITheme.FONT_XS);
        selectedCountBadge.setForeground(UITheme.THEME_PRIMARY);

        pickHeader.add(lblMembers, BorderLayout.WEST);
        pickHeader.add(selectedCountBadge, BorderLayout.EAST);
        form.add(pickHeader);
        form.add(Box.createVerticalStrut(6));

        // Search member input
        searchMemberField = new JTextField();
        searchMemberField.setFont(UITheme.FONT_SMALL);
        searchMemberField.setMaximumSize(new Dimension(380, 30));
        searchMemberField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        searchMemberField.setToolTipText("Tìm theo tên hoặc email");
        searchMemberField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { filterMembers(); }
            @Override public void removeUpdate(DocumentEvent event) { filterMembers(); }
            @Override public void changedUpdate(DocumentEvent event) { filterMembers(); }
        });
        form.add(searchMemberField);
        form.add(Box.createVerticalStrut(8));

        content.add(form, BorderLayout.NORTH);

        // Member list with checkboxes
        memberJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        memberJList.setCellRenderer(new MemberListRenderer());
        memberJList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                int index = memberJList.locationToIndex(e.getPoint());
                if (index >= 0 && memberJList.getCellBounds(index, index).contains(e.getPoint())) {
                    toggleMember(index);
                }
            }
        });

        memberJList.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "toggleMember");
        memberJList.getActionMap().put("toggleMember", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                toggleMember(memberJList.getSelectedIndex());
            }
        });

        memberScroll = new JScrollPane();
        memberScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        showMemberStatus("Đang tải danh sách bạn bè...");
        content.add(memberScroll, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        btnCancel = new JButton("Hủy") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(0xf1, 0xf5, 0xf9) : Color.WHITE);
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(UITheme.BORDER_LIGHT);
                g2.draw(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnCancel.setFont(UITheme.FONT_MEDIUM);
        btnCancel.setForeground(UITheme.TEXT_MUTED);
        btnCancel.setContentAreaFilled(false);
        btnCancel.setBorder(new EmptyBorder(6, 16, 6, 16));
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> dispose());

        btnSubmit = new JButton(existingGroup == null ? "Tạo nhóm" : "Thêm thành viên") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? UITheme.THEME_PRIMARY.darker() : (getModel().isRollover() ? new Color(0x25, 0x63, 0xeb) : UITheme.THEME_PRIMARY));
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnSubmit.setFont(UITheme.FONT_MEDIUM);
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setContentAreaFilled(false);
        btnSubmit.setBorder(new EmptyBorder(6, 18, 6, 18));
        btnSubmit.setFocusPainted(false);
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmit.addActionListener(e -> handleSubmit());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSubmit);
        content.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(content);
        loadFriends();
    }

    private void loadFriends() {
        Set<String> existingIds = new HashSet<>();
        if (existingGroup != null) {
            existingGroup.getMembers().forEach(member -> existingIds.add(member.getId()));
        }
        new SwingWorker<List<MemberCheckItem>, Void>() {
            @Override
            protected List<MemberCheckItem> doInBackground() throws Exception {
                JsonObject snapshot = JsonParser.parseString(ApiClient.getFriends()).getAsJsonObject();
                Set<String> acceptedIds = new HashSet<>();
                for (JsonElement relationship : snapshot.getAsJsonArray("accepted")) {
                    for (JsonElement member : relationship.getAsJsonArray()) {
                        acceptedIds.add(member.getAsString());
                    }
                }
                List<MemberCheckItem> eligibleFriends = new ArrayList<>();
                for (JsonElement element : snapshot.getAsJsonArray("contacts")) {
                    JsonObject contact = element.getAsJsonObject();
                    String id = contact.get("id").getAsString();
                    if (acceptedIds.contains(id) && !existingIds.contains(id)) {
                        JsonElement email = contact.get("email");
                        eligibleFriends.add(new MemberCheckItem(id, contact.get("name").getAsString(),
                                email == null || email.isJsonNull() ? "" : email.getAsString()));
                    }
                }
                return eligibleFriends;
            }

            @Override
            protected void done() {
                if (!isDisplayable()) return;
                try {
                    friends.addAll(get());
                    friendsLoaded = true;
                    filterMembers();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showMemberStatus("Không tải được danh sách bạn bè.");
                }
            }
        }.execute();
    }

    private void filterMembers() {
        if (!friendsLoaded) return;
        String query = searchMemberField.getText().strip().toLowerCase(Locale.ROOT);
        memberListModel.clear();
        for (MemberCheckItem friend : friends) {
            if (friend.name.toLowerCase(Locale.ROOT).contains(query)
                    || friend.email.toLowerCase(Locale.ROOT).contains(query)) {
                memberListModel.addElement(friend);
            }
        }
        if (!memberListModel.isEmpty()) {
            memberScroll.setViewportView(memberJList);
        } else if (friends.isEmpty()) {
            showMemberStatus(existingGroup == null ? "Bạn chưa có bạn bè để chọn."
                    : "Không còn bạn bè nào để thêm vào nhóm.");
        } else {
            showMemberStatus("Không tìm thấy bạn bè phù hợp.");
        }
    }

    private void showMemberStatus(String message) {
        JLabel status = new JLabel(message, SwingConstants.CENTER);
        status.setFont(UITheme.FONT_SMALL);
        status.setForeground(UITheme.TEXT_MUTED);
        memberScroll.setViewportView(status);
    }

    private void toggleMember(int index) {
        if (index < 0 || !btnSubmit.isEnabled()) return;
        MemberCheckItem item = memberListModel.get(index);
        item.selected = !item.selected;
        if (item.selected) selectedUserIds.add(item.id);
        else selectedUserIds.remove(item.id);
        selectedCountBadge.setText("Đã chọn: " + selectedUserIds.size());
        memberJList.repaint();
    }

    private void handleSubmit() {
        String name = groupNameField.getText().trim();
        if (existingGroup == null && name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên nhóm!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedUserIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chọn ít nhất một bạn bè.", getTitle(), JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> memberList = new ArrayList<>(selectedUserIds);
        btnSubmit.setEnabled(false);
        btnCancel.setEnabled(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                if (existingGroup != null) {
                    ApiClient.addMembersToGroup(existingGroup.getId(), memberList);
                    return existingGroup.getId();
                }
                ApiClient.ApiResponse response = ApiClient.createGroup(name, memberList);
                if (!response.success) {
                    throw new IOException(response.message != null ? response.message : "Không thể tạo nhóm.");
                }
                return "db-" + response.json.get("id").getAsString();
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                btnSubmit.setEnabled(true);
                btnCancel.setEnabled(true);
                setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                try {
                    String groupId = get();
                    if (onGroupSaved != null) onGroupSaved.accept(groupId);
                    JOptionPane.showMessageDialog(CreateGroupDialog.this,
                            existingGroup == null ? "Tạo nhóm thành công!" : "Đã thêm thành viên.",
                            "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    JOptionPane.showMessageDialog(CreateGroupDialog.this,
                            ex.getCause().getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public void setOnGroupSaved(Consumer<String> callback) { this.onGroupSaved = callback; }

    static class MemberCheckItem {
        String id;
        String name;
        String email;
        boolean selected;

        MemberCheckItem(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }

    static class MemberListRenderer extends JPanel implements ListCellRenderer<MemberCheckItem> {
        private final JCheckBox checkBox = new JCheckBox();
        private final JLabel nameLabel = new JLabel();
        private final JLabel emailLabel = new JLabel();

        MemberListRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(new EmptyBorder(6, 8, 6, 8));
            setOpaque(true);

            checkBox.setOpaque(false);
            add(checkBox, BorderLayout.WEST);

            JPanel textPanel = new JPanel(new GridLayout(2, 1));
            textPanel.setOpaque(false);
            nameLabel.setFont(UITheme.FONT_SUBHEADING);
            emailLabel.setFont(UITheme.FONT_XS);
            emailLabel.setForeground(UITheme.TEXT_MUTED);

            textPanel.add(nameLabel);
            textPanel.add(emailLabel);
            add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends MemberCheckItem> list,
                                                      MemberCheckItem item, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            if (item != null) {
                checkBox.setSelected(item.selected);
                nameLabel.setText(item.name);
                emailLabel.setText(item.email);
                setBackground(isSelected ? UITheme.SIDEBAR_ITEM_HOVER : Color.WHITE);
            }
            return this;
        }
    }
}

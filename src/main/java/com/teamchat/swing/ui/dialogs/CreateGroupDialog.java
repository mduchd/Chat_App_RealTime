package com.teamchat.swing.ui.dialogs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teamchat.swing.api.ApiClient;
import com.teamchat.swing.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Modal for creating a new group chat matching ChatSpace web design.
 */
public class CreateGroupDialog extends JDialog {

    private final JTextField groupNameField;
    private final JTextField avatarTextField;
    private final JTextField searchMemberField;
    private final DefaultListModel<MemberCheckItem> memberListModel = new DefaultListModel<>();
    private final JList<MemberCheckItem> memberJList = new JList<>(memberListModel);
    private final JLabel selectedCountBadge;

    private final Set<String> selectedUserIds = new HashSet<>();
    private Consumer<JsonObject> onGroupCreated;

    public CreateGroupDialog(Frame parent) {
        super(parent, "Tạo nhóm chat mới", true);
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
        form.add(searchMemberField);
        form.add(Box.createVerticalStrut(8));

        content.add(form, BorderLayout.NORTH);

        // Member list with checkboxes
        memberJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        memberJList.setCellRenderer(new MemberListRenderer());
        memberJList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int index = memberJList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    MemberCheckItem item = memberListModel.get(index);
                    item.selected = !item.selected;
                    if (item.selected) selectedUserIds.add(item.id);
                    else selectedUserIds.remove(item.id);
                    selectedCountBadge.setText("Đã chọn: " + selectedUserIds.size());
                    memberJList.repaint();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(memberJList);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        content.add(scroll, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = new JButton("Hủy") {
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

        JButton btnSubmit = new JButton("Tạo nhóm") {
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
        loadDiscoverUsers();
    }

    private void loadDiscoverUsers() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ApiClient.discoverUsers("");
            }

            @Override
            protected void done() {
                try {
                    String json = get();
                    JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                    for (JsonElement el : arr) {
                        JsonObject obj = el.getAsJsonObject();
                        String id = obj.get("id").getAsString();
                        String name = obj.get("name").getAsString();
                        String email = obj.has("email") && !obj.get("email").isJsonNull() ? obj.get("email").getAsString() : "";
                        memberListModel.addElement(new MemberCheckItem(id, name, email));
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void handleSubmit() {
        String name = groupNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên nhóm!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> memberList = new ArrayList<>(selectedUserIds);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<ApiClient.ApiResponse, Void>() {
            @Override
            protected ApiClient.ApiResponse doInBackground() throws Exception {
                return ApiClient.createGroup(name, memberList);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    ApiClient.ApiResponse res = get();
                    if (res.success) {
                        JOptionPane.showMessageDialog(CreateGroupDialog.this,
                                "Tạo nhóm thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        if (onGroupCreated != null) onGroupCreated.accept(res.json);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(CreateGroupDialog.this,
                                res.message != null ? res.message : "Không thể tạo nhóm!",
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CreateGroupDialog.this,
                            "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public void setOnGroupCreated(Consumer<JsonObject> callback) { this.onGroupCreated = callback; }

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

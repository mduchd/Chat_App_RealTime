package com.teamchat.swing.ui.dialogs;

import com.teamchat.swing.api.ApiClient;
import com.teamchat.swing.model.UserInfo;
import com.teamchat.swing.ui.UITheme;
import com.teamchat.swing.ui.components.AvatarCircle;
import com.teamchat.swing.util.SoundService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.function.Consumer;

/**
 * Hộp thoại thay đổi ảnh đại diện (Avatar) theo phong cách tối giản, hiện đại (Minimalist & Clean).
 * Không dùng icon emoji ký tự lạ tránh lỗi ô vuông font [□].
 */
public class ChangeAvatarDialog extends JDialog {

    private final UserInfo user;
    private final Consumer<String> onAvatarChanged;
    private final AvatarCircle previewAvatar;
    private final JLabel lblStatus;

    private String selectedUrl;
    private File selectedFile;
    private JTextField txtUrl;

    // Bộ sưu tập avatar mẫu hoạt hình đẹp mắt
    private static final String[] PRESET_AVATARS = {
            "https://api.dicebear.com/7.x/bottts/png?seed=Felix",
            "https://api.dicebear.com/7.x/bottts/png?seed=Aneka",
            "https://api.dicebear.com/7.x/bottts/png?seed=Cookie",
            "https://api.dicebear.com/7.x/bottts/png?seed=Midnight",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Oliver",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Sophia",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Lucas",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Mia"
    };

    private JPanel[] presetWrappers = new JPanel[PRESET_AVATARS.length];

    public ChangeAvatarDialog(Dialog owner, UserInfo user, Consumer<String> onAvatarChanged) {
        super(owner, "Thay đổi ảnh đại diện", true);
        this.user = user;
        this.onAvatarChanged = onAvatarChanged;
        this.selectedUrl = user != null ? user.getAvatar() : null;

        setSize(440, 520);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.WINDOW_BG);

        // ====================== 1. PHẦN TRÊN: PREVIEW AVATAR ======================
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.setBorder(new EmptyBorder(16, 20, 10, 20));

        JPanel avatarCenter = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        avatarCenter.setOpaque(false);
        previewAvatar = new AvatarCircle(user != null ? user.getInitials() : "CS", 80, UITheme.THEME_PRIMARY, Color.WHITE);
        if (selectedUrl != null && !selectedUrl.isBlank()) {
            previewAvatar.setImageUrl(selectedUrl);
        }
        avatarCenter.add(previewAvatar);
        topPanel.add(avatarCenter);
        topPanel.add(Box.createVerticalStrut(8));

        lblStatus = new JLabel("Chọn avatar mẫu hoặc tải ảnh từ máy tính", SwingConstants.CENTER);
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.TEXT_MUTED);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(lblStatus);

        add(topPanel, BorderLayout.NORTH);

        // ====================== 2. PHẦN GIỮA: TABS CHỌN ẢNH ======================
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_SUBHEADING);
        tabbedPane.setBorder(new EmptyBorder(0, 16, 0, 16));

        // --- TAB 1: Bộ sưu tập avatar mẫu ---
        JPanel presetTab = new JPanel();
        presetTab.setLayout(new BoxLayout(presetTab, BoxLayout.Y_AXIS));
        presetTab.setOpaque(false);
        presetTab.setBorder(new EmptyBorder(12, 10, 10, 10));

        JPanel grid = new JPanel(new GridLayout(2, 4, 10, 10));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(380, 160));

        for (int i = 0; i < PRESET_AVATARS.length; i++) {
            final int index = i;
            final String url = PRESET_AVATARS[i];

            JPanel itemCard = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean isSelected = url.equals(selectedUrl);
                    if (isSelected) {
                        g2.setColor(new Color(0xeb, 0xf5, 0xff));
                        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                        g2.setColor(UITheme.THEME_PRIMARY);
                        g2.setStroke(new BasicStroke(2f));
                        g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 10, 10));
                    } else {
                        g2.setColor(Color.WHITE);
                        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                        g2.setColor(UITheme.BORDER_LIGHT);
                        g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                    }
                    g2.dispose();
                }
            };
            itemCard.setOpaque(false);
            itemCard.setPreferredSize(new Dimension(64, 64));
            itemCard.setCursor(new Cursor(Cursor.HAND_CURSOR));

            AvatarCircle c = new AvatarCircle("AV", 46, new Color(0x38bdf8), Color.WHITE);
            c.setImageUrl(url);
            itemCard.add(c);

            presetWrappers[index] = itemCard;

            itemCard.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    selectedUrl = url;
                    selectedFile = null;
                    if (txtUrl != null) txtUrl.setText("");
                    previewAvatar.setImageUrl(url);
                    lblStatus.setText("Đã chọn avatar mẫu!");
                    lblStatus.setForeground(UITheme.THEME_PRIMARY);
                    repaintPresets();
                }
            });

            grid.add(itemCard);
        }
        presetTab.add(grid);
        tabbedPane.addTab("Avatar mẫu", presetTab);

        // --- TAB 2: Tải lên hoặc nhập link ---
        JPanel customTab = new JPanel();
        customTab.setLayout(new BoxLayout(customTab, BoxLayout.Y_AXIS));
        customTab.setOpaque(false);
        customTab.setBorder(new EmptyBorder(16, 12, 12, 12));

        // Nút chọn từ máy
        JButton btnFile = new JButton("Chọn tệp ảnh từ máy tính...") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(0xf8, 0xfa, 0xfc) : Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(UITheme.BORDER_LIGHT);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnFile.setFont(UITheme.FONT_MEDIUM);
        btnFile.setForeground(UITheme.TEXT_MAIN);
        btnFile.setContentAreaFilled(false);
        btnFile.setBorder(new EmptyBorder(8, 14, 8, 14));
        btnFile.setFocusPainted(false);
        btnFile.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFile.setMaximumSize(new Dimension(380, 36));
        btnFile.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnFile.addActionListener(e -> chooseLocalFile());
        customTab.add(btnFile);

        customTab.add(Box.createVerticalStrut(14));

        JLabel orLabel = new JLabel("Hoặc nhập đường dẫn ảnh trực tuyến (URL):");
        orLabel.setFont(UITheme.FONT_XS);
        orLabel.setForeground(UITheme.TEXT_MUTED);
        orLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        customTab.add(orLabel);
        customTab.add(Box.createVerticalStrut(6));

        JPanel urlRow = new JPanel(new BorderLayout(8, 0));
        urlRow.setOpaque(false);
        urlRow.setMaximumSize(new Dimension(380, 34));

        txtUrl = new JTextField();
        txtUrl.setFont(UITheme.FONT_REGULAR);
        txtUrl.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
        urlRow.add(txtUrl, BorderLayout.CENTER);

        JButton btnPreview = new JButton("Xem");
        btnPreview.setFont(UITheme.FONT_SMALL);
        btnPreview.setFocusPainted(false);
        btnPreview.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPreview.addActionListener(e -> {
            String u = txtUrl.getText().trim();
            if (!u.isEmpty()) {
                selectedUrl = u;
                selectedFile = null;
                previewAvatar.setImageUrl(u);
                lblStatus.setText("Đang xem trước ảnh từ link URL");
                lblStatus.setForeground(UITheme.THEME_PRIMARY);
                repaintPresets();
            }
        });
        urlRow.add(btnPreview, BorderLayout.EAST);
        customTab.add(urlRow);

        tabbedPane.addTab("Tải lên / Link ảnh", customTab);

        add(tabbedPane, BorderLayout.CENTER);

        // ====================== 3. PHẦN DƯỚI: NÚT THAO TÁC ======================
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(UITheme.SIDEBAR_CARD_BG);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_LIGHT),
                new EmptyBorder(10, 16, 10, 16)
        ));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton btnCancel = new JButton("Hủy bỏ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(0xf1, 0xf5, 0xf9) : Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(UITheme.BORDER_LIGHT);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnCancel.setFont(UITheme.FONT_MEDIUM);
        btnCancel.setForeground(UITheme.TEXT_MUTED);
        btnCancel.setContentAreaFilled(false);
        btnCancel.setBorder(new EmptyBorder(6, 14, 6, 14));
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> dispose());
        actions.add(btnCancel);

        JButton btnSave = new JButton("Lưu thay đổi") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? UITheme.THEME_PRIMARY.darker() : (getModel().isRollover() ? new Color(0x25, 0x63, 0xeb) : UITheme.THEME_PRIMARY));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnSave.setFont(UITheme.FONT_MEDIUM);
        btnSave.setForeground(Color.WHITE);
        btnSave.setContentAreaFilled(false);
        btnSave.setBorder(new EmptyBorder(6, 16, 6, 16));
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> saveAvatar());
        actions.add(btnSave);

        bottomBar.add(actions, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private void repaintPresets() {
        for (JPanel p : presetWrappers) {
            if (p != null) p.repaint();
        }
    }

    private void chooseLocalFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn ảnh đại diện");
        chooser.setFileFilter(new FileNameExtensionFilter("Tệp ảnh (*.png, *.jpg, *.jpeg, *.webp, *.gif)",
                "png", "jpg", "jpeg", "webp", "gif"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            this.selectedFile = f;
            this.selectedUrl = f.getAbsolutePath();
            if (txtUrl != null) txtUrl.setText(f.getName());
            previewAvatar.setImageFile(f);
            lblStatus.setText("Đã chọn file: " + f.getName());
            lblStatus.setForeground(UITheme.THEME_PRIMARY);
            repaintPresets();
        }
    }

    private void saveAvatar() {
        if (selectedFile == null && (selectedUrl == null || selectedUrl.trim().isEmpty())) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn hoặc tải lên một ảnh đại diện!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        lblStatus.setText("Đang lưu ảnh đại diện...");
        lblStatus.setForeground(UITheme.THEME_PRIMARY);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                if (selectedFile != null) {
                    ApiClient.ApiResponse res = ApiClient.uploadAvatar(selectedFile);
                    if (res.success && res.json != null && res.json.has("avatarUrl")) {
                        return res.json.get("avatarUrl").getAsString();
                    } else {
                        throw new Exception(res.message != null ? res.message : "Tải ảnh lên thất bại");
                    }
                } else {
                    ApiClient.ApiResponse res = ApiClient.updateAvatar(selectedUrl);
                    if (res.success) {
                        return selectedUrl;
                    } else {
                        throw new Exception(res.message != null ? res.message : "Cập nhật ảnh thất bại");
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    String finalUrl = get();
                    if (user != null) {
                        user.setAvatar(finalUrl);
                    }
                    if (onAvatarChanged != null) {
                        onAvatarChanged.accept(finalUrl);
                    }
                    SoundService.playNotificationSound();
                    JOptionPane.showMessageDialog(ChangeAvatarDialog.this, "Cập nhật ảnh đại diện thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception ex) {
                    lblStatus.setText("Lỗi: " + ex.getMessage());
                    lblStatus.setForeground(UITheme.DANGER_RED);
                    JOptionPane.showMessageDialog(ChangeAvatarDialog.this, "Lỗi khi lưu ảnh: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}

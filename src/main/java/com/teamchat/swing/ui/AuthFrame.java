package com.teamchat.swing.ui;

import com.teamchat.swing.api.ApiClient;
import com.teamchat.swing.model.UserInfo;
import com.teamchat.swing.ui.components.RoundedButton;
import com.teamchat.swing.ui.components.RoundedPanel;
import com.teamchat.swing.ui.components.RoundedPasswordField;
import com.teamchat.swing.ui.components.RoundedTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/** Màn hình đăng nhập và đăng ký ChatSpace. */
public class AuthFrame extends JFrame {

    private static final int FORM_WIDTH = 350;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel viewsPanel = new JPanel(cardLayout);

    // Tab buttons bo tròn
    private RoundedButton tabBtnLogin;
    private RoundedButton tabBtnRegister;

    // Login Form Fields
    private RoundedTextField loginEmailField;
    private RoundedPasswordField loginPasswordField;
    private RoundedButton loginSubmitButton;
    private boolean loginPasswordVisible;
    private RoundedTextField regEmailField;
    private RoundedPasswordField regPasswordField;
    private RoundedButton registerSubmitButton;
    private boolean regPasswordVisible;
    private boolean signingIn;

    public AuthFrame() {
        setTitle("CHATSPACE — Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 720);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(UITheme.PAGE_BG);

        initUI();
        getRootPane().setDefaultButton(loginSubmitButton);
    }

    private void initUI() {
        setLayout(new GridBagLayout());

        // Khung Card trắng bo góc 16px (chuẩn CSS .auth-card)
        RoundedPanel card = new RoundedPanel(16, UITheme.BORDER_LIGHT);
        card.setBackground(UITheme.WINDOW_BG);
        card.setPreferredSize(new Dimension(430, 650));
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(24, 35, 22, 35));

        // -------------------------------------------------------------
        // 1. BRAND HEADER (LOGO + TIÊU ĐỀ + KHUNG TABS)
        // -------------------------------------------------------------
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);

        JLabel brandIcon = new JLabel(new com.teamchat.swing.ui.components.VectorIcon(com.teamchat.swing.ui.components.VectorIcon.Type.CHAT, 42, UITheme.THEME_PRIMARY, 2.2f), SwingConstants.CENTER);
        brandIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("CHATSPACE", SwingConstants.CENTER);
        titleLabel.setFont(UITheme.FONT_TITLE); // 23pt BOLD
        titleLabel.setForeground(UITheme.THEME_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Hệ thống trò chuyện bảo mật thời gian thực", SwingConstants.CENTER);
        subtitleLabel.setFont(UITheme.FONT_SMALL); // 12pt
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        brandPanel.add(brandIcon);
        brandPanel.add(Box.createVerticalStrut(2));
        brandPanel.add(titleLabel);
        brandPanel.add(Box.createVerticalStrut(2));
        brandPanel.add(subtitleLabel);
        brandPanel.add(Box.createVerticalStrut(14));

        // Khung Tabs bo tròn 12px nền xám #f2f6fb (chuẩn CSS .auth-main-tabs)
        RoundedPanel tabsPanel = new RoundedPanel(12, UITheme.BORDER_LIGHT);
        tabsPanel.setBackground(UITheme.SIDEBAR_CARD_BG);
        tabsPanel.setLayout(new GridLayout(1, 2, 4, 0));
        tabsPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        tabsPanel.setMaximumSize(new Dimension(FORM_WIDTH, 44));
        tabsPanel.setPreferredSize(new Dimension(FORM_WIDTH, 44));
        tabsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        tabBtnLogin = new RoundedButton("Đăng nhập", 9, Color.WHITE, UITheme.THEME_PRIMARY);
        tabBtnLogin.setFont(UITheme.FONT_HEADING); // 16pt BOLD
        tabBtnLogin.addActionListener(e -> switchAuthTab("login"));

        tabBtnRegister = new RoundedButton("Đăng ký", 9, new Color(0, 0, 0, 0), UITheme.TEXT_MUTED);
        tabBtnRegister.setFont(UITheme.FONT_HEADING); // 16pt BOLD
        tabBtnRegister.addActionListener(e -> switchAuthTab("register"));

        tabsPanel.add(tabBtnLogin);
        tabsPanel.add(tabBtnRegister);
        brandPanel.add(tabsPanel);

        card.add(brandPanel, BorderLayout.NORTH);

        // -------------------------------------------------------------
        // 2. CÁC VIEW DÙNG CARDLAYOUT
        // -------------------------------------------------------------
        viewsPanel.setOpaque(false);
        viewsPanel.add(createLoginView(), "login");
        viewsPanel.add(createRegisterView(), "register");

        card.add(viewsPanel, BorderLayout.CENTER);
        add(card);
    }

    private void switchAuthTab(String tabName) {
        if (signingIn) return;
        cardLayout.show(viewsPanel, tabName);
        if ("login".equals(tabName)) {
            tabBtnLogin.setCustomBackground(Color.WHITE);
            tabBtnLogin.setForeground(UITheme.THEME_PRIMARY);
            tabBtnRegister.setCustomBackground(new Color(0, 0, 0, 0));
            tabBtnRegister.setForeground(UITheme.TEXT_MUTED);
            setTitle("CHATSPACE — Đăng nhập");
            getRootPane().setDefaultButton(loginSubmitButton);
            loginEmailField.requestFocusInWindow();
        } else if ("register".equals(tabName)) {
            tabBtnLogin.setCustomBackground(new Color(0, 0, 0, 0));
            tabBtnLogin.setForeground(UITheme.TEXT_MUTED);
            tabBtnRegister.setCustomBackground(Color.WHITE);
            tabBtnRegister.setForeground(UITheme.THEME_PRIMARY);
            setTitle("CHATSPACE — Đăng ký");
            getRootPane().setDefaultButton(registerSubmitButton);
            regEmailField.requestFocusInWindow();
        }
    }

    // =====================================================================
    // 1. MÀN HÌNH ĐĂNG NHẬP
    // =====================================================================
    private JPanel createLoginView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(6, 0, 0, 0));

        // auth-view-desc
        JLabel desc = new JLabel("<html><center>Đăng nhập bằng email đã đăng ký để tiếp tục trò chuyện</center></html>", SwingConstants.CENTER);
        desc.setFont(UITheme.FONT_SMALL); // 12pt
        desc.setForeground(UITheme.TEXT_MUTED);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        desc.setMaximumSize(new Dimension(FORM_WIDTH, 34));
        panel.add(desc);
        panel.add(Box.createVerticalStrut(10));

        loginEmailField = createRoundedInput("name@example.com");
        panel.add(createFieldLabel("Địa chỉ email", loginEmailField));
        panel.add(Box.createVerticalStrut(4));
        panel.add(loginEmailField);
        panel.add(Box.createVerticalStrut(10));

        loginPasswordField = createRoundedPassword();
        panel.add(createFieldLabel("Mật khẩu", loginPasswordField));
        panel.add(Box.createVerticalStrut(4));

        JPanel passWrap = createPasswordWrapper(loginPasswordField, () -> {
            loginPasswordVisible = !loginPasswordVisible;
            loginPasswordField.setEchoChar(loginPasswordVisible ? (char) 0 : '•');
        });
        panel.add(passWrap);
        panel.add(Box.createVerticalStrut(16));

        loginSubmitButton = createSubmitButton("Đăng nhập vào ChatSpace", this::handleLogin);
        panel.add(loginSubmitButton);
        panel.add(Box.createVerticalStrut(12));

        // Footer: Chưa có tài khoản? Đăng ký ngay
        JLabel switchLabel = new JLabel("<html>Chưa có tài khoản? <font color='#1d72b8'><b>Đăng ký ngay</b></font></html>", SwingConstants.CENTER);
        switchLabel.setFont(UITheme.FONT_SMALL); // 12pt
        switchLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        switchLabel.setMaximumSize(new Dimension(FORM_WIDTH, 22));
        switchLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                switchAuthTab("register");
            }
        });
        panel.add(switchLabel);

        return panel;
    }

    // =====================================================================
    // 2. MÀN HÌNH ĐĂNG KÝ
    // =====================================================================
    private JPanel createRegisterView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 0, 0, 0));

        // auth-view-desc
        JLabel desc = new JLabel("<html><center>Đăng ký tài khoản bằng email và mật khẩu</center></html>", SwingConstants.CENTER);
        desc.setFont(UITheme.FONT_SMALL);
        desc.setForeground(UITheme.TEXT_MUTED);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        desc.setMaximumSize(new Dimension(FORM_WIDTH, 20));
        panel.add(desc);
        panel.add(Box.createVerticalStrut(8));

        regEmailField = createRoundedInput("name@example.com");
        panel.add(createFieldLabel("Địa chỉ email", regEmailField));
        panel.add(Box.createVerticalStrut(2));
        panel.add(regEmailField);
        panel.add(Box.createVerticalStrut(6));

        // Mật khẩu
        regPasswordField = createRoundedPassword();
        panel.add(createFieldLabel("Mật khẩu", regPasswordField));
        panel.add(Box.createVerticalStrut(2));
        panel.add(createPasswordWrapper(regPasswordField, () -> {
            regPasswordVisible = !regPasswordVisible;
            regPasswordField.setEchoChar(regPasswordVisible ? (char) 0 : '•');
        }));
        panel.add(Box.createVerticalStrut(6));

        registerSubmitButton = createSubmitButton("Hoàn tất đăng ký & Đăng nhập", this::handleRegister);
        panel.add(registerSubmitButton);
        panel.add(Box.createVerticalStrut(10));

        // Switch
        JLabel switchLabel = new JLabel("<html>Đã có tài khoản? <font color='#1d72b8'><b>Đăng nhập ngay</b></font></html>", SwingConstants.CENTER);
        switchLabel.setFont(UITheme.FONT_SMALL);
        switchLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        switchLabel.setMaximumSize(new Dimension(FORM_WIDTH, 20));
        switchLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                switchAuthTab("login");
            }
        });
        panel.add(switchLabel);

        return panel;
    }

    private void handleLogin() {
        submitCredentials(loginEmailField, loginPasswordField, loginSubmitButton, false);
    }

    private void handleRegister() {
        submitCredentials(regEmailField, regPasswordField, registerSubmitButton, true);
    }

    private void submitCredentials(RoundedTextField emailField, RoundedPasswordField passwordField,
            RoundedButton submitButton, boolean register) {
        if (signingIn) return;
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (!email.matches("[^\\s@]+@[^\\s@]+")) {
            showError("Nhập email hợp lệ.");
            emailField.requestFocusInWindow();
            return;
        }
        if (password.isBlank() || (register && password.length() < 6)) {
            showError(register ? "Mật khẩu cần ít nhất 6 ký tự." : "Nhập mật khẩu.");
            passwordField.requestFocusInWindow();
            return;
        }

        signingIn = true;
        tabBtnLogin.setEnabled(false);
        tabBtnRegister.setEnabled(false);
        submitButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<ApiClient.ApiResponse, Void>() {
            @Override
            protected ApiClient.ApiResponse doInBackground() throws Exception {
                return register ? ApiClient.registerLocal(email, password) : ApiClient.login(email, password);
            }

            @Override
            protected void done() {
                signingIn = false;
                tabBtnLogin.setEnabled(true);
                tabBtnRegister.setEnabled(true);
                submitButton.setEnabled(true);
                setCursor(Cursor.getDefaultCursor());
                try {
                    ApiClient.ApiResponse result = get();
                    if (result.success && result.user != null) {
                        openMainChat(result.user);
                    } else if (!register && result.httpCode == 401) {
                        showError("Email hoặc mật khẩu chưa đúng.");
                        passwordField.requestFocusInWindow();
                    } else {
                        showError(result.message == null ? "Không thực hiện được. Hãy thử lại." : result.message);
                    }
                } catch (ExecutionException ex) {
                    showError("Không kết nối được. Hãy chạy máy chủ ChatSpace local.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void openMainChat(UserInfo user) {
        dispose();
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(user);
            mainFrame.setVisible(true);
            mainFrame.toFront();
        });
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cần kiểm tra", JOptionPane.WARNING_MESSAGE);
    }

    // =====================================================================
    // UI BUILDER HELPERS
    // =====================================================================

    private JPanel createFieldLabel(String text, JComponent field) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.setMaximumSize(new Dimension(FORM_WIDTH, 20));

        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        label.setFont(UITheme.FONT_BOLD_SMALL); // 12pt BOLD
        label.setForeground(UITheme.TEXT_MAIN);
        p.add(label);
        return p;
    }

    private RoundedTextField createRoundedInput(String placeholder) {
        RoundedTextField field = new RoundedTextField(10, UITheme.BORDER_LIGHT, UITheme.THEME_PRIMARY);
        field.setFont(UITheme.FONT_REGULAR); // 14pt
        field.setToolTipText(placeholder);
        field.setPreferredSize(new Dimension(FORM_WIDTH, 38));
        field.setMaximumSize(new Dimension(FORM_WIDTH, 38));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        return field;
    }

    private RoundedPasswordField createRoundedPassword() {
        RoundedPasswordField field = new RoundedPasswordField(10, UITheme.BORDER_LIGHT, UITheme.THEME_PRIMARY);
        field.setFont(UITheme.FONT_REGULAR); // 14pt
        field.setPreferredSize(new Dimension(FORM_WIDTH - 42, 38));
        field.setEchoChar('•');
        return field;
    }

    /**
     * Bọc ô password cùng nút mắt ẩn/hiện mật khẩu
     */
    private JPanel createPasswordWrapper(RoundedPasswordField field, Runnable toggleAction) {
        JPanel wrap = new JPanel(new BorderLayout(4, 0));
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(FORM_WIDTH, 38));
        wrap.setPreferredSize(new Dimension(FORM_WIDTH, 38));

        wrap.add(field, BorderLayout.CENTER);

        JButton btnEye = new JButton("👁");
        btnEye.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnEye.setPreferredSize(new Dimension(38, 38));
        btnEye.setFocusPainted(false);
        btnEye.setContentAreaFilled(false);
        btnEye.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        btnEye.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEye.setToolTipText("Hiện/Ẩn mật khẩu");
        btnEye.addActionListener(e -> toggleAction.run());
        wrap.add(btnEye, BorderLayout.EAST);

        return wrap;
    }

    private RoundedButton createSubmitButton(String text, Runnable action) {
        RoundedButton button = new RoundedButton(text, 9, UITheme.THEME_PRIMARY, Color.WHITE);
        button.setFont(UITheme.FONT_HEADING);
        button.setMaximumSize(new Dimension(FORM_WIDTH, 42));
        button.setPreferredSize(new Dimension(FORM_WIDTH, 42));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(e -> action.run());
        return button;
    }
}

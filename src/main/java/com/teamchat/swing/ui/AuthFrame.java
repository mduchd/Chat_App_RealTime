package com.teamchat.swing.ui;

import com.teamchat.swing.api.ApiClient;
import com.teamchat.swing.api.GoogleSignIn;
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

/**
 * =========================================================================
 * FILE: AuthFrame.java - MÀN HÌNH ĐĂNG NHẬP / ĐĂNG KÝ / QUÊN MẬT KHẨU
 * =========================================================================
 * 
 * [THIẾT KẾ CHUẨN 100% THEO FILE HTML/CSS]
 * - Cỡ chữ to thêm 1pt (UITheme font scale +1pt).
 * - Tất cả nhãn, văn bản mô tả, gợi ý, nút bấm và placeholder khớp 100% file index.html.
 * - Có nút mắt 👁 ẩn/hiện mật khẩu trên các ô nhập mật khẩu.
 * - Các góc bo tròn mềm mại: Card 16px, Tabs 12px, Button/Input 10px.
 */
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
    private RoundedButton googleLoginButton;
    private boolean signingIn;
    private boolean loginPasswordVisible = false;

    // Register Form Fields
    private RoundedTextField regPhoneField;
    private RoundedTextField regEmailField;
    private RoundedPasswordField regPasswordField;
    private boolean regPasswordVisible = false;
    private RoundedPasswordField regConfirmPasswordField;
    private boolean regConfirmVisible = false;
    private RoundedTextField regOtpField;
    private JLabel regOtpHint;

    // Forgot Password Form Fields
    private RoundedTextField forgotEmailField;
    private RoundedTextField forgotOtpField;
    private JLabel forgotOtpHint;
    private RoundedPasswordField forgotNewPasswordField;
    private boolean forgotPasswordVisible = false;
    private RoundedPasswordField forgotConfirmPasswordField;
    private boolean forgotConfirmVisible = false;

    public AuthFrame() {
        setTitle("CHATSPACE — Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 720);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(UITheme.PAGE_BG);

        initUI();
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
        viewsPanel.add(createForgotView(), "forgot");

        card.add(viewsPanel, BorderLayout.CENTER);
        add(card);
    }

    private void switchAuthTab(String tabName) {
        cardLayout.show(viewsPanel, tabName);
        if ("login".equals(tabName)) {
            tabBtnLogin.setCustomBackground(Color.WHITE);
            tabBtnLogin.setForeground(UITheme.THEME_PRIMARY);
            tabBtnRegister.setCustomBackground(new Color(0, 0, 0, 0));
            tabBtnRegister.setForeground(UITheme.TEXT_MUTED);
            setTitle("CHATSPACE — Đăng nhập");
        } else if ("register".equals(tabName)) {
            tabBtnLogin.setCustomBackground(new Color(0, 0, 0, 0));
            tabBtnLogin.setForeground(UITheme.TEXT_MUTED);
            tabBtnRegister.setCustomBackground(Color.WHITE);
            tabBtnRegister.setForeground(UITheme.THEME_PRIMARY);
            setTitle("CHATSPACE — Đăng ký");
        } else if ("forgot".equals(tabName)) {
            tabBtnLogin.setCustomBackground(new Color(0, 0, 0, 0));
            tabBtnLogin.setForeground(UITheme.TEXT_MUTED);
            tabBtnRegister.setCustomBackground(new Color(0, 0, 0, 0));
            tabBtnRegister.setForeground(UITheme.TEXT_MUTED);
            setTitle("CHATSPACE — Quên mật khẩu");
        }
    }

    // =====================================================================
    // 1. MÀN HÌNH ĐĂNG NHẬP (VIEW 1: LOGIN - KHỚP 100% HTML)
    // =====================================================================
    private JPanel createLoginView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(6, 0, 0, 0));

        // auth-view-desc
        JLabel desc = new JLabel("<html><center>Đăng nhập bằng tài khoản Gmail đã đăng ký để tiếp tục trò chuyện</center></html>", SwingConstants.CENTER);
        desc.setFont(UITheme.FONT_SMALL); // 12pt
        desc.setForeground(UITheme.TEXT_MUTED);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        desc.setMaximumSize(new Dimension(FORM_WIDTH, 34));
        panel.add(desc);
        panel.add(Box.createVerticalStrut(10));

        // Nhập Gmail
        panel.add(createFieldLabel("Địa chỉ Gmail"));
        panel.add(Box.createVerticalStrut(4));
        loginEmailField = createRoundedInput("name@gmail.com");
        panel.add(loginEmailField);
        panel.add(Box.createVerticalStrut(10));

        // Mật khẩu + Link Quên mật khẩu
        JPanel passLabelRow = new JPanel(new BorderLayout());
        passLabelRow.setOpaque(false);
        passLabelRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        passLabelRow.setMaximumSize(new Dimension(FORM_WIDTH, 20));

        JLabel passLabel = new JLabel("Mật khẩu");
        passLabel.setFont(UITheme.FONT_BOLD_SMALL); // 12pt BOLD
        passLabel.setForeground(UITheme.TEXT_MAIN);

        JLabel forgotLink = new JLabel("Quên mật khẩu?");
        forgotLink.setFont(UITheme.FONT_SMALL); // 12pt
        forgotLink.setForeground(UITheme.THEME_PRIMARY);
        forgotLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                switchAuthTab("forgot");
            }
        });

        passLabelRow.add(passLabel, BorderLayout.WEST);
        passLabelRow.add(forgotLink, BorderLayout.EAST);
        panel.add(passLabelRow);
        panel.add(Box.createVerticalStrut(4));

        // Cụm mật khẩu có nút mắt ẩn/hiện
        loginPasswordField = createRoundedPassword();
        JPanel passWrap = createPasswordWrapper(loginPasswordField, () -> {
            loginPasswordVisible = !loginPasswordVisible;
            loginPasswordField.setEchoChar(loginPasswordVisible ? (char) 0 : '•');
        });
        panel.add(passWrap);
        panel.add(Box.createVerticalStrut(16));

        // Nút Đăng nhập vào ChatSpace (btn-cs-primary)
        RoundedButton btnSubmit = new RoundedButton("Đăng nhập vào ChatSpace", 9, UITheme.THEME_PRIMARY, Color.WHITE);
        btnSubmit.setFont(UITheme.FONT_HEADING); // 16pt BOLD
        btnSubmit.setMaximumSize(new Dimension(FORM_WIDTH, 42));
        btnSubmit.setPreferredSize(new Dimension(FORM_WIDTH, 42));
        btnSubmit.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSubmit.addActionListener(e -> handleLogin());
        panel.add(btnSubmit);
        panel.add(Box.createVerticalStrut(12));

        // Divider: HOẶC
        panel.add(createDivider("HOẶC"));
        panel.add(Box.createVerticalStrut(12));

        // Nút Google (btn-google-oauth)
        googleLoginButton = new RoundedButton("Đăng nhập nhanh bằng Google", 9, Color.WHITE, UITheme.TEXT_MAIN, UITheme.BORDER_LIGHT);
        googleLoginButton.setFont(UITheme.FONT_REGULAR);
        googleLoginButton.setMaximumSize(new Dimension(FORM_WIDTH, 40));
        googleLoginButton.setPreferredSize(new Dimension(FORM_WIDTH, 40));
        googleLoginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        googleLoginButton.addActionListener(e -> startSignIn(GoogleSignIn::signIn, true));
        panel.add(googleLoginButton);
        panel.add(Box.createVerticalStrut(14));

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
    // 2. MÀN HÌNH ĐĂNG KÝ (VIEW 2: REGISTER - KHỚP 100% HTML)
    // =====================================================================
    private JPanel createRegisterView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 0, 0, 0));

        // auth-view-desc
        JLabel desc = new JLabel("<html><center>Đăng ký tài khoản mới bằng số điện thoại và Gmail</center></html>", SwingConstants.CENTER);
        desc.setFont(UITheme.FONT_SMALL);
        desc.setForeground(UITheme.TEXT_MUTED);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        desc.setMaximumSize(new Dimension(FORM_WIDTH, 20));
        panel.add(desc);
        panel.add(Box.createVerticalStrut(8));

        // Số điện thoại
        panel.add(createFieldLabel("Số điện thoại"));
        panel.add(Box.createVerticalStrut(2));
        regPhoneField = createRoundedInput("Ví dụ: 0987654321");
        panel.add(regPhoneField);
        panel.add(Box.createVerticalStrut(6));

        // Gmail
        panel.add(createFieldLabel("Địa chỉ Gmail"));
        panel.add(Box.createVerticalStrut(2));
        regEmailField = createRoundedInput("username@gmail.com");
        panel.add(regEmailField);
        panel.add(Box.createVerticalStrut(6));

        // Mật khẩu
        panel.add(createFieldLabel("Mật khẩu"));
        panel.add(Box.createVerticalStrut(2));
        regPasswordField = createRoundedPassword();
        panel.add(createPasswordWrapper(regPasswordField, () -> {
            regPasswordVisible = !regPasswordVisible;
            regPasswordField.setEchoChar(regPasswordVisible ? (char) 0 : '•');
        }));
        panel.add(Box.createVerticalStrut(6));

        // Xác nhận lại mật khẩu
        panel.add(createFieldLabel("Xác nhận lại mật khẩu"));
        panel.add(Box.createVerticalStrut(2));
        regConfirmPasswordField = createRoundedPassword();
        panel.add(createPasswordWrapper(regConfirmPasswordField, () -> {
            regConfirmVisible = !regConfirmVisible;
            regConfirmPasswordField.setEchoChar(regConfirmVisible ? (char) 0 : '•');
        }));
        panel.add(Box.createVerticalStrut(6));

        // Mã OTP
        panel.add(createFieldLabel("Mã xác thực OTP gửi qua Gmail"));
        panel.add(Box.createVerticalStrut(2));

        JPanel otpRow = new JPanel(new BorderLayout(6, 0));
        otpRow.setOpaque(false);
        otpRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        otpRow.setMaximumSize(new Dimension(FORM_WIDTH, 38));

        regOtpField = new RoundedTextField(10, UITheme.BORDER_LIGHT, UITheme.THEME_PRIMARY);
        regOtpField.setFont(new Font("Segoe UI", Font.BOLD, 15));
        regOtpField.setHorizontalAlignment(JTextField.CENTER);

        RoundedButton btnSendOtp = new RoundedButton("Gửi mã OTP", 9, UITheme.SIDEBAR_CARD_BG, UITheme.THEME_PRIMARY, UITheme.THEME_PRIMARY);
        btnSendOtp.setFont(UITheme.FONT_BOLD_SMALL);
        btnSendOtp.addActionListener(e -> handleSendOtp(regEmailField.getText().trim(), "REGISTER", btnSendOtp));

        otpRow.add(regOtpField, BorderLayout.CENTER);
        otpRow.add(btnSendOtp, BorderLayout.EAST);
        panel.add(otpRow);

        // Hint text đúng HTML: regOtpHint
        regOtpHint = new JLabel("<html><small>Nhấn \"Gửi mã OTP\", hệ thống sẽ gửi mã 6 số vào Gmail.</small></html>");
        regOtpHint.setFont(UITheme.FONT_XS); // 11pt
        regOtpHint.setForeground(UITheme.TEXT_MUTED);
        regOtpHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        regOtpHint.setMaximumSize(new Dimension(FORM_WIDTH, 20));
        panel.add(regOtpHint);
        panel.add(Box.createVerticalStrut(10));

        // Submit: Hoàn tất đăng ký & Đăng nhập
        RoundedButton btnSubmit = new RoundedButton("Hoàn tất đăng ký & Đăng nhập", 9, UITheme.THEME_PRIMARY, Color.WHITE);
        btnSubmit.setFont(UITheme.FONT_HEADING);
        btnSubmit.setMaximumSize(new Dimension(FORM_WIDTH, 42));
        btnSubmit.setPreferredSize(new Dimension(FORM_WIDTH, 42));
        btnSubmit.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSubmit.addActionListener(e -> handleRegister());
        panel.add(btnSubmit);
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

    // =====================================================================
    // 3. MÀN HÌNH QUÊN MẬT KHẨU (VIEW 3: FORGOT - KHỚP 100% HTML)
    // =====================================================================
    private JPanel createForgotView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(6, 0, 0, 0));

        // auth-view-desc
        JLabel desc = new JLabel("<html><center>Nhập địa chỉ Gmail để nhận mã OTP đặt lại mật khẩu mới</center></html>", SwingConstants.CENTER);
        desc.setFont(UITheme.FONT_SMALL);
        desc.setForeground(UITheme.TEXT_MUTED);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        desc.setMaximumSize(new Dimension(FORM_WIDTH, 20));
        panel.add(desc);
        panel.add(Box.createVerticalStrut(8));

        panel.add(createFieldLabel("Địa chỉ Gmail đã đăng ký"));
        panel.add(Box.createVerticalStrut(2));
        forgotEmailField = createRoundedInput("name@gmail.com");
        panel.add(forgotEmailField);
        panel.add(Box.createVerticalStrut(6));

        // OTP
        panel.add(createFieldLabel("Mã xác thực OTP gửi qua Gmail"));
        panel.add(Box.createVerticalStrut(2));

        JPanel otpRow = new JPanel(new BorderLayout(6, 0));
        otpRow.setOpaque(false);
        otpRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        otpRow.setMaximumSize(new Dimension(FORM_WIDTH, 38));

        forgotOtpField = new RoundedTextField(10, UITheme.BORDER_LIGHT, UITheme.THEME_PRIMARY);
        forgotOtpField.setFont(new Font("Segoe UI", Font.BOLD, 15));
        forgotOtpField.setHorizontalAlignment(JTextField.CENTER);

        RoundedButton btnSendOtp = new RoundedButton("Nhận mã OTP", 9, UITheme.SIDEBAR_CARD_BG, UITheme.THEME_PRIMARY, UITheme.THEME_PRIMARY);
        btnSendOtp.setFont(UITheme.FONT_BOLD_SMALL);
        btnSendOtp.addActionListener(e -> handleSendOtp(forgotEmailField.getText().trim(), "FORGOT_PASSWORD", btnSendOtp));

        otpRow.add(forgotOtpField, BorderLayout.CENTER);
        otpRow.add(btnSendOtp, BorderLayout.EAST);
        panel.add(otpRow);

        forgotOtpHint = new JLabel("<html><small>Mã xác nhận gồm 6 số sẽ được gửi thẳng vào hộp thư Gmail.</small></html>");
        forgotOtpHint.setFont(UITheme.FONT_XS);
        forgotOtpHint.setForeground(UITheme.TEXT_MUTED);
        forgotOtpHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        forgotOtpHint.setMaximumSize(new Dimension(FORM_WIDTH, 20));
        panel.add(forgotOtpHint);
        panel.add(Box.createVerticalStrut(8));

        panel.add(createFieldLabel("Mật khẩu mới"));
        panel.add(Box.createVerticalStrut(2));
        forgotNewPasswordField = createRoundedPassword();
        panel.add(createPasswordWrapper(forgotNewPasswordField, () -> {
            forgotPasswordVisible = !forgotPasswordVisible;
            forgotNewPasswordField.setEchoChar(forgotPasswordVisible ? (char) 0 : '•');
        }));
        panel.add(Box.createVerticalStrut(6));

        panel.add(createFieldLabel("Xác nhận mật khẩu mới"));
        panel.add(Box.createVerticalStrut(2));
        forgotConfirmPasswordField = createRoundedPassword();
        panel.add(createPasswordWrapper(forgotConfirmPasswordField, () -> {
            forgotConfirmVisible = !forgotConfirmVisible;
            forgotConfirmPasswordField.setEchoChar(forgotConfirmVisible ? (char) 0 : '•');
        }));
        panel.add(Box.createVerticalStrut(10));

        // Submit: Đặt lại mật khẩu & Đăng nhập
        RoundedButton btnSubmit = new RoundedButton("Đặt lại mật khẩu & Đăng nhập", 9, UITheme.THEME_PRIMARY, Color.WHITE);
        btnSubmit.setFont(UITheme.FONT_HEADING);
        btnSubmit.setMaximumSize(new Dimension(FORM_WIDTH, 42));
        btnSubmit.setPreferredSize(new Dimension(FORM_WIDTH, 42));
        btnSubmit.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSubmit.addActionListener(e -> handleForgotPassword());
        panel.add(btnSubmit);
        panel.add(Box.createVerticalStrut(10));

        JLabel backLink = new JLabel("Quay lại Đăng nhập", SwingConstants.CENTER);
        backLink.setFont(UITheme.FONT_SMALL);
        backLink.setForeground(UITheme.THEME_PRIMARY);
        backLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backLink.setAlignmentX(Component.CENTER_ALIGNMENT);
        backLink.setMaximumSize(new Dimension(FORM_WIDTH, 20));
        backLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                switchAuthTab("login");
            }
        });
        panel.add(backLink);

        return panel;
    }

    // =====================================================================
    // XỬ LÝ NGHIỆP VỤ (BUSINESS LOGIC)
    // =====================================================================

    private void handleLogin() {
        if (signingIn) return;
        String email = loginEmailField.getText().trim();
        String password = new String(loginPasswordField.getPassword()).trim();

        if (email.isEmpty() || !email.contains("@")) {
            showError("Vui lòng nhập địa chỉ Gmail hợp lệ!");
            return;
        }
        if (password.isEmpty()) {
            showError("Vui lòng nhập mật khẩu!");
            return;
        }

        startSignIn(() -> ApiClient.login(email, password), false);
    }

    private void startSignIn(Callable<ApiClient.ApiResponse> action, boolean google) {
        if (signingIn) return;
        signingIn = true;
        googleLoginButton.setEnabled(false);
        if (google) {
            googleLoginButton.setText("Đang chờ đăng nhập trên trình duyệt…");
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<ApiClient.ApiResponse, Void>() {
            @Override
            protected ApiClient.ApiResponse doInBackground() throws Exception {
                return action.call();
            }

            @Override
            protected void done() {
                signingIn = false;
                googleLoginButton.setEnabled(true);
                if (google) {
                    googleLoginButton.setText("Đăng nhập nhanh bằng Google");
                }
                setCursor(Cursor.getDefaultCursor());
                try {
                    ApiClient.ApiResponse res = get();
                    if (res.success && res.user != null) {
                        openMainChat(res.user);
                    } else {
                        showError(res.message != null ? res.message : "Đăng nhập thất bại!");
                    }
                } catch (ExecutionException ex) {
                    showError((google ? "Đăng nhập Google thất bại. " : "Lỗi kết nối máy chủ: ")
                            + ex.getCause().getMessage());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void handleSendOtp(String email, String purpose, JButton btn) {
        if (email.isEmpty() || !email.contains("@")) {
            showError("Vui lòng nhập địa chỉ Gmail hợp lệ trước khi gửi mã OTP!");
            return;
        }

        final String originalText = "FORGOT_PASSWORD".equalsIgnoreCase(purpose) ? "Nhận mã OTP" : "Gửi mã OTP";
        btn.setEnabled(false);
        btn.setText("Đang gửi...");

        new SwingWorker<ApiClient.ApiResponse, Void>() {
            @Override
            protected ApiClient.ApiResponse doInBackground() throws Exception {
                return ApiClient.sendOtp(email, purpose);
            }

            @Override
            protected void done() {
                try {
                    ApiClient.ApiResponse res = get();
                    if (res.success) {
                        // Tự động điền OTP nếu ở chế độ dev/local test
                        if (res.devOtp != null && !res.devOtp.isEmpty()) {
                            if ("REGISTER".equalsIgnoreCase(purpose)) {
                                regOtpField.setText(res.devOtp);
                                regOtpField.requestFocusInWindow();
                                if (regOtpHint != null) {
                                    regOtpHint.setText("<html><small><font color='#16a34a'>✓ Đã tạo mã OTP test: <b>" + res.devOtp + "</b> (đã tự điền).</font></small></html>");
                                }
                            } else {
                                forgotOtpField.setText(res.devOtp);
                                forgotOtpField.requestFocusInWindow();
                                if (forgotOtpHint != null) {
                                    forgotOtpHint.setText("<html><small><font color='#16a34a'>✓ Đã tạo mã OTP test: <b>" + res.devOtp + "</b> (đã tự điền).</font></small></html>");
                                }
                            }
                        } else {
                            if ("REGISTER".equalsIgnoreCase(purpose) && regOtpHint != null) {
                                regOtpHint.setText("<html><small><font color='#16a34a'>✓ Đã gửi mã OTP đến <b>" + email + "</b>.</font></small></html>");
                            } else if (forgotOtpHint != null) {
                                forgotOtpHint.setText("<html><small><font color='#16a34a'>✓ Đã gửi mã OTP đến <b>" + email + "</b>.</font></small></html>");
                            }
                        }

                        // Đếm ngược 60 giây như trên web app.js
                        startOtpCountdown(btn, originalText);

                        JOptionPane.showMessageDialog(AuthFrame.this,
                                res.message != null ? res.message : "Đã gửi mã OTP đến " + email,
                                "Thông báo OTP", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        btn.setEnabled(true);
                        btn.setText(originalText);
                        showError(res.message != null ? res.message : "Không thể gửi OTP!");
                    }
                } catch (Exception ex) {
                    btn.setEnabled(true);
                    btn.setText(originalText);
                    showError("Lỗi kết nối khi gửi OTP: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void startOtpCountdown(JButton btn, String originalText) {
        btn.setEnabled(false);
        final int[] countdown = {60};
        btn.setText("Gửi lại (60s)");

        javax.swing.Timer timer = new javax.swing.Timer(1000, null);
        timer.addActionListener(e -> {
            countdown[0]--;
            if (countdown[0] <= 0) {
                timer.stop();
                btn.setEnabled(true);
                btn.setText(originalText);
            } else {
                btn.setText("Gửi lại (" + countdown[0] + "s)");
            }
        });
        timer.start();
    }

    private void handleRegister() {
        String phone = regPhoneField.getText().trim();
        String email = regEmailField.getText().trim();
        String password = new String(regPasswordField.getPassword()).trim();
        String confirm = new String(regConfirmPasswordField.getPassword()).trim();
        String otp = regOtpField.getText().trim();

        if (phone.isEmpty()) { showError("Vui lòng nhập số điện thoại!"); return; }
        if (email.isEmpty() || !email.contains("@")) { showError("Vui lòng nhập địa chỉ Gmail hợp lệ!"); return; }
        if (password.length() < 6) { showError("Mật khẩu phải có ít nhất 6 ký tự!"); return; }
        if (!password.equals(confirm)) { showError("Xác nhận mật khẩu không khớp!"); return; }
        if (otp.isEmpty()) { showError("Vui lòng nhập mã xác thực OTP từ Gmail!"); return; }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<ApiClient.ApiResponse, Void>() {
            @Override
            protected ApiClient.ApiResponse doInBackground() throws Exception {
                return ApiClient.register(phone, email, password, otp);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    ApiClient.ApiResponse res = get();
                    if (res.success && res.user != null) {
                        JOptionPane.showMessageDialog(AuthFrame.this,
                                "Đăng ký tài khoản thành công! Đang chuyển vào ứng dụng...",
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        openMainChat(res.user);
                    } else {
                        showError(res.message != null ? res.message : "Đăng ký thất bại!");
                    }
                } catch (Exception ex) {
                    showError("Lỗi kết nối: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void handleForgotPassword() {
        String email = forgotEmailField.getText().trim();
        String otp = forgotOtpField.getText().trim();
        String password = new String(forgotNewPasswordField.getPassword()).trim();
        String confirm = new String(forgotConfirmPasswordField.getPassword()).trim();

        if (email.isEmpty() || !email.contains("@")) { showError("Vui lòng nhập Gmail hợp lệ!"); return; }
        if (otp.isEmpty()) { showError("Vui lòng nhập mã OTP xác thực!"); return; }
        if (password.length() < 6) { showError("Mật khẩu mới phải có ít nhất 6 ký tự!"); return; }
        if (!password.equals(confirm)) { showError("Xác nhận mật khẩu không khớp!"); return; }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<ApiClient.ApiResponse, Void>() {
            @Override
            protected ApiClient.ApiResponse doInBackground() throws Exception {
                return ApiClient.forgotPassword(email, otp, password);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    ApiClient.ApiResponse res = get();
                    if (res.success) {
                        JOptionPane.showMessageDialog(AuthFrame.this,
                                res.message != null ? res.message : "Đặt lại mật khẩu thành công! Bây giờ bạn có thể đăng nhập bằng mật khẩu mới.",
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        // Điền sẵn Gmail vào ô đăng nhập
                        loginEmailField.setText(email);
                        loginPasswordField.setText("");
                        loginPasswordField.requestFocusInWindow();
                        switchAuthTab("login");
                    } else {
                        showError(res.message != null ? res.message : "Đặt lại mật khẩu thất bại!");
                    }
                } catch (Exception ex) {
                    showError("Lỗi kết nối: " + ex.getMessage());
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

    private JPanel createFieldLabel(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.setMaximumSize(new Dimension(FORM_WIDTH, 20));

        JLabel label = new JLabel(text);
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

    private JPanel createDivider(String text) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setMaximumSize(new Dimension(FORM_WIDTH, 18));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(new JSeparator(), gbc);

        JLabel label = new JLabel("  " + text + "  ");
        label.setFont(UITheme.FONT_XS); // 11pt
        label.setForeground(UITheme.TEXT_MUTED);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(label, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(new JSeparator(), gbc);

        return panel;
    }
}

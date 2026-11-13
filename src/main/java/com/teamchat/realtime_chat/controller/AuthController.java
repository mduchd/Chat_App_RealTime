package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.repository.UserRepository;
import com.teamchat.realtime_chat.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, OtpService otpService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * API gửi mã OTP thẳng vào Gmail
     */
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String email = request.get("email");
        String purpose = request.get("purpose");

        if (email == null || email.isBlank() || !email.contains("@")) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập địa chỉ Gmail hợp lệ!");
            return ResponseEntity.badRequest().body(response);
        }

        String cleanEmail = email.trim().toLowerCase();
        String cleanPurpose = (purpose != null) ? purpose.trim().toUpperCase() : "REGISTER";

        // Kiểm tra logic nghiệp vụ theo mục đích
        if ("REGISTER".equals(cleanPurpose)) {
            if (userRepository.existsByEmail(cleanEmail)) {
                response.put("success", false);
                response.put("message", "Địa chỉ Gmail này đã được đăng ký! Vui lòng chọn Đăng nhập hoặc sử dụng Gmail khác.");
                return ResponseEntity.badRequest().body(response);
            }
        } else if ("FORGOT_PASSWORD".equals(cleanPurpose)) {
            if (!userRepository.existsByEmail(cleanEmail)) {
                response.put("success", false);
                response.put("message", "Không tìm thấy tài khoản liên kết với Gmail này! Vui lòng kiểm tra lại hoặc đăng ký mới.");
                return ResponseEntity.badRequest().body(response);
            }
        }

        boolean sent = otpService.generateAndSendOtp(cleanEmail, cleanPurpose);
        if (sent) {
            response.put("success", true);
            response.put("message", "Mã xác thực OTP đã được gửi thẳng vào hộp thư Gmail: " + cleanEmail + ". Vui lòng kiểm tra hộp thư đến (và mục Thư rác/Spam)!");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Không thể gửi email OTP qua máy chủ Gmail lúc này. Vui lòng kiểm tra cấu hình MAIL_USERNAME / MAIL_PASSWORD trên server hoặc thử lại sau!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * API Đăng ký tài khoản mới (phải có OTP gửi vào Gmail)
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String phone = request.get("phone");
        String email = request.get("email");
        String password = request.get("password");
        String otp = request.get("otp");

        if (phone == null || phone.isBlank()) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập số điện thoại!");
            return ResponseEntity.badRequest().body(response);
        }

        if (email == null || email.isBlank() || !email.contains("@")) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập địa chỉ Gmail hợp lệ!");
            return ResponseEntity.badRequest().body(response);
        }

        if (password == null || password.length() < 6) {
            response.put("success", false);
            response.put("message", "Mật khẩu phải có độ dài tối thiểu 6 ký tự!");
            return ResponseEntity.badRequest().body(response);
        }

        if (otp == null || otp.isBlank()) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập mã xác thực OTP đã được gửi đến Gmail!");
            return ResponseEntity.badRequest().body(response);
        }

        String cleanPhone = phone.trim().replaceAll("\\s+", "");
        String cleanEmail = email.trim().toLowerCase();
        String displayName = cleanPhone;
        String emailLocalPart = cleanEmail.contains("@") ? cleanEmail.substring(0, cleanEmail.indexOf('@')) : cleanEmail;
        if (!emailLocalPart.isBlank()) {
            displayName = emailLocalPart;
        }

        if (userRepository.existsByPhone(cleanPhone)) {
            response.put("success", false);
            response.put("message", "Số điện thoại này đã được đăng ký tài khoản!");
            return ResponseEntity.badRequest().body(response);
        }

        if (userRepository.existsByEmail(cleanEmail)) {
            response.put("success", false);
            response.put("message", "Gmail này đã được đăng ký tài khoản! Vui lòng chọn Đăng nhập.");
            return ResponseEntity.badRequest().body(response);
        }

        boolean otpValid = otpService.verifyOtp(cleanEmail, otp, "REGISTER");
        if (!otpValid) {
            response.put("success", false);
            response.put("message", "Mã xác thực OTP không chính xác hoặc đã hết hạn! Vui lòng bấm 'Gửi mã OTP' để nhận mã mới.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            User newUser = new User();
            newUser.setPhone(cleanPhone);
            newUser.setEmail(cleanEmail);
            newUser.setDisplayName(displayName.trim());
            newUser.setPasswordHash(passwordEncoder.encode(password));
            newUser.setAvatarUrl("https://ui-avatars.com/api/?name=" + URLEncoder.encode(displayName.trim(), StandardCharsets.UTF_8) + "&background=1d72b8&color=fff&bold=true");
            newUser.setLastLoginAt(LocalDateTime.now());

            User savedUser = userRepository.save(newUser);

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", savedUser.getId().toString());
            userData.put("name", savedUser.getDisplayName());
            userData.put("email", savedUser.getEmail());
            userData.put("phone", savedUser.getPhone());
            userData.put("avatar", savedUser.getAvatarUrl());

            response.put("success", true);
            response.put("message", "Đăng ký tài khoản thành công!");
            response.put("user", userData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi lưu người dùng mới: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra trong quá trình tạo tài khoản. Vui lòng thử lại!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * API Đăng nhập hệ thống (BẮT BUỘC ĐÃ ĐĂNG KÝ MỚI ĐƯỢC ĐĂNG NHẬP)
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String email = request.get("email");
        String password = request.get("password");

        if (email == null || email.isBlank()) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập địa chỉ Gmail / Email đăng nhập!");
            return ResponseEntity.badRequest().body(response);
        }

        if (password == null || password.isBlank()) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập mật khẩu!");
            return ResponseEntity.badRequest().body(response);
        }

        String cleanEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);

        // BẮT BUỘC: Người dùng phải đăng ký mới đăng nhập được, tài khoản chưa đăng ký sẽ bị chặn!
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Tài khoản với Gmail này chưa được đăng ký! Vui lòng đăng ký tài khoản trước khi đăng nhập.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User user = userOpt.get();

        // Kiểm tra mật khẩu
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            response.put("success", false);
            response.put("message", "Tài khoản này được đăng ký thông qua Google OAuth. Vui lòng sử dụng nút 'Đăng nhập nhanh bằng Google' hoặc dùng 'Quên mật khẩu' để tạo mật khẩu.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            response.put("success", false);
            response.put("message", "Mật khẩu không chính xác! Vui lòng thử lại hoặc chọn Quên mật khẩu.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Cập nhật thời điểm đăng nhập gần nhất
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId().toString());
        userData.put("name", user.getDisplayName());
        userData.put("email", user.getEmail());
        userData.put("avatar", user.getAvatarUrl());

        response.put("success", true);
        response.put("message", "Đăng nhập thành công! Chào mừng " + user.getDisplayName());
        response.put("user", userData);
        return ResponseEntity.ok(response);
    }

    /**
     * API Quên mật khẩu (Xác minh OTP gửi qua Gmail và cập nhật mật khẩu mới)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        if (email == null || email.isBlank() || !email.contains("@")) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập địa chỉ Gmail hợp lệ!");
            return ResponseEntity.badRequest().body(response);
        }

        if (otp == null || otp.isBlank()) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập mã OTP xác thực!");
            return ResponseEntity.badRequest().body(response);
        }

        if (newPassword == null || newPassword.length() < 6) {
            response.put("success", false);
            response.put("message", "Mật khẩu mới phải có ít nhất 6 ký tự!");
            return ResponseEntity.badRequest().body(response);
        }

        String cleanEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);

        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không tìm thấy tài khoản với địa chỉ Gmail này!");
            return ResponseEntity.badRequest().body(response);
        }

        boolean otpValid = otpService.verifyOtp(cleanEmail, otp, "FORGOT_PASSWORD");
        if (!otpValid) {
            response.put("success", false);
            response.put("message", "Mã OTP không chính xác hoặc đã hết hạn! Vui lòng bấm 'Nhận mã OTP' để lấy lại.");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        response.put("success", true);
        response.put("message", "Đặt lại mật khẩu thành công! Bây giờ bạn có thể đăng nhập bằng mật khẩu mới.");
        return ResponseEntity.ok(response);
    }
}

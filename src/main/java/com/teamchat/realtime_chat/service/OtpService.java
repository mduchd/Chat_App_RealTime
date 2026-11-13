package com.teamchat.realtime_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final long OTP_VALIDITY_SECONDS = 5 * 60; // 5 phút

    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();

    public OtpService(EmailService emailService) {
        this.emailService = emailService;
    }

    private static class OtpEntry {
        final String code;
        final Instant expiresAt;

        OtpEntry(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Tạo và gửi mã OTP qua Gmail.
     *
     * @param email Địa chỉ Gmail nhận mã
     * @param purpose Mục đích ("REGISTER", "LOGIN", "FORGOT_PASSWORD")
     * @return boolean Thành công hay thất bại khi gửi
     */
    public boolean generateAndSendOtp(String email, String purpose) {
        if (email == null || email.isBlank()) {
            return false;
        }

        String cleanEmail = email.trim().toLowerCase();
        String cleanPurpose = (purpose != null && !purpose.isBlank()) ? purpose.trim().toUpperCase() : "REGISTER";

        // Sinh mã 6 số ngẫu nhiên
        String otpCode = String.valueOf(100000 + random.nextInt(900000));
        Instant expiresAt = Instant.now().plusSeconds(OTP_VALIDITY_SECONDS);

        String storageKey = cleanEmail + ":" + cleanPurpose;
        otpStorage.put(storageKey, new OtpEntry(otpCode, expiresAt));

        String purposeDisplay = switch (cleanPurpose) {
            case "REGISTER" -> "Đăng ký tài khoản mới";
            case "FORGOT_PASSWORD" -> "Khôi phục mật khẩu";
            case "LOGIN" -> "Xác thực đăng nhập";
            default -> "Xác thực tài khoản";
        };

        logger.info("Đã tạo mã OTP cho [{}], mục đích: {}", cleanEmail, cleanPurpose);

        // Gửi qua EmailService
        boolean sent = emailService.sendOtpEmail(cleanEmail, otpCode, purposeDisplay);
        if (!sent) {
            logger.warn("Không gửi được email qua SMTP Gmail cho {}. (Lưu ý: kiểm tra mật khẩu ứng dụng Gmail MAIL_PASSWORD)", cleanEmail);
        }

        return sent;
    }

    /**
     * Kiểm tra mã OTP người dùng nhập.
     *
     * @param email Địa chỉ Gmail
     * @param code Mã OTP do người dùng nhập
     * @param purpose Mục đích
     * @return boolean Hợp lệ hay không
     */
    public boolean verifyOtp(String email, String code, String purpose) {
        if (email == null || code == null) {
            return false;
        }

        String cleanEmail = email.trim().toLowerCase();
        String cleanCode = code.trim();
        String cleanPurpose = (purpose != null && !purpose.isBlank()) ? purpose.trim().toUpperCase() : "REGISTER";

        String storageKey = cleanEmail + ":" + cleanPurpose;
        OtpEntry entry = otpStorage.get(storageKey);

        if (entry == null) {
            logger.warn("Không tìm thấy mã OTP cho [{}] hoặc mã chưa được gửi.", cleanEmail);
            return false;
        }

        if (entry.isExpired()) {
            otpStorage.remove(storageKey);
            logger.warn("Mã OTP cho [{}] đã hết hạn.", cleanEmail);
            return false;
        }

        if (entry.code.equals(cleanCode)) {
            // Đúng mã -> Xóa để không thể tái sử dụng
            otpStorage.remove(storageKey);
            logger.info("Xác thực OTP thành công cho [{}]", cleanEmail);
            return true;
        }

        logger.warn("Mã OTP không khớp cho [{}]", cleanEmail);
        return false;
    }
}


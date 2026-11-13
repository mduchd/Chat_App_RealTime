package com.teamchat.realtime_chat.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * Gửi mã xác minh OTP thẳng vào hộp thư Gmail của người dùng.
     *
     * @param toEmail Email người nhận
     * @param otpCode Mã OTP gồm 6 chữ số
     * @param purpose Mục đích gửi mã: "Đăng ký tài khoản", "Đăng nhập", "Đặt lại mật khẩu"
     * @return boolean Thành công hay thất bại
     */
    public boolean sendOtpEmail(String toEmail, String otpCode, String purpose) {
        if (mailSender == null) {
            logger.error("JavaMailSender chưa được cấu hình. Vui lòng kiểm tra spring.mail.* trong application.properties");
            return false;
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            logger.error("MAIL_USERNAME chưa được cấu hình. Cần set biến môi trường MAIL_USERNAME với tài khoản Gmail chính xác.");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "ChatSpace Realtime");
            helper.setTo(toEmail);
            helper.setSubject("ChatSpace — Mã xác thực OTP (" + otpCode + ")");

            String htmlContent = buildOtpHtmlTemplate(otpCode, purpose, toEmail);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Đã gửi mã OTP thành công đến Gmail: {}", toEmail);
            return true;
        } catch (Exception e) {
            logger.error("Lỗi khi gửi email OTP đến {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    private String buildOtpHtmlTemplate(String otpCode, String purpose, String recipientEmail) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 24px; color: #1e293b; }
                    .email-container { max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                    .header { background: #16263d; padding: 28px 24px; text-align: center; }
                    .header h1 { color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 1.5px; font-weight: 700; }
                    .header p { color: #94a3b8; margin: 6px 0 0 0; font-size: 13px; }
                    .body-content { padding: 32px 28px; text-align: center; }
                    .greeting { font-size: 16px; color: #334155; margin-bottom: 12px; }
                    .purpose-badge { display: inline-block; background: #e0f2fe; color: #0369a1; padding: 6px 14px; border-radius: 999px; font-size: 13px; font-weight: 600; margin-bottom: 20px; }
                    .otp-box { background: #f8fafc; border: 2px dashed #0284c7; border-radius: 12px; padding: 18px 24px; margin: 20px 0; display: inline-block; }
                    .otp-code { font-size: 34px; font-weight: 800; letter-spacing: 8px; color: #0284c7; margin: 0; font-family: 'Courier New', monospace; }
                    .note { font-size: 13px; color: #64748b; line-height: 1.6; margin-top: 18px; }
                    .warning { font-size: 12px; color: #ef4444; margin-top: 10px; font-weight: 500; }
                    .footer { background: #f8fafc; padding: 16px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <h1>CHATSPACE</h1>
                        <p>Hệ thống nhắn tin thời gian thực</p>
                    </div>
                    <div class="body-content">
                        <div class="greeting">Xin chào <strong>%s</strong>,</div>
                        <div class="purpose-badge">%s</div>
                        <p style="font-size: 14px; color: #475569; margin: 0;">Dưới đây là mã xác thực một lần (OTP) của bạn:</p>
                        
                        <div class="otp-box">
                            <div class="otp-code">%s</div>
                        </div>

                        <p class="note">Mã xác thực này có hiệu lực trong <strong>5 phút</strong>.<br>Vui lòng không chia sẻ mã này cho bất kỳ ai khác.</p>
                        <p class="warning">Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email.</p>
                    </div>
                    <div class="footer">
                        © 2026 ChatSpace — Ứng dụng nhắn tin bảo mật thời gian thực
                    </div>
                </div>
            </body>
            </html>
            """.formatted(recipientEmail, purpose, otpCode);
    }
}


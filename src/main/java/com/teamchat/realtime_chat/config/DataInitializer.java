package com.teamchat.realtime_chat.config;

import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * Tự động tạo sẵn tài khoản Quản trị viên (Admin) mặc định nếu chưa tồn tại
 * để bạn đăng nhập kiểm thử ngay mà không cần qua bước OTP đăng ký.
 */
@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    @Profile("!swing-local")
    public CommandLineRunner initAdminAccount(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            try {
                String adminEmail = "admin@gmail.com";
                if (userRepository.findByEmail(adminEmail).isEmpty()) {
                    User admin = new User();
                    admin.setDisplayName("Quản trị viên (Admin)");
                    admin.setEmail(adminEmail);
                    admin.setPhone("0988888888");
                    admin.setPasswordHash(passwordEncoder.encode("admin123"));
                    admin.setAvatarUrl("https://ui-avatars.com/api/?name=Admin&background=16263d&color=fff&bold=true");
                    admin.setLastLoginAt(LocalDateTime.now());

                    userRepository.save(admin);
                    logger.info("===============================================================");
                    logger.info(">>> ĐÃ KHỞI TẠO TÀI KHOẢN ADMIN MẶC ĐỊNH THÀNH CÔNG <<<");
                    logger.info(">>> Gmail:    {}                                  <<<", adminEmail);
                    logger.info(">>> Mật khẩu: admin123                                        <<<");
                    logger.info("===============================================================");
                }
            } catch (Exception ex) {
                logger.info("DataInitializer bỏ qua khởi tạo (môi trường test hoặc DB in-memory): {}", ex.getMessage());
            }
        };
    }
}

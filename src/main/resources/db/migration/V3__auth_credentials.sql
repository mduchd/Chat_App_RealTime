-- =====================================================
-- V3: THÊM HỖ TRỢ ĐĂNG KÝ VÀ MẬT KHẨU CHO NGƯỜI DÙNG
-- Cho phép tài khoản đăng ký qua Email/Password
-- =====================================================

-- Cho phép google_sub nhận NULL đối với tài khoản đăng ký email
ALTER TABLE users MODIFY google_sub VARCHAR(255) NULL;

-- Thêm cột password_hash để lưu mật khẩu đã băm (BCrypt)
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255) NULL AFTER avatar_url;

package com.teamchat.swing;

import com.formdev.flatlaf.FlatLightLaf;
import com.teamchat.swing.api.ApiClient;
import com.teamchat.swing.ui.AuthFrame;

import javax.swing.*;
import java.awt.*;

/**
 * =========================================================================
 * FILE: ChatSpaceApp.java - ĐIỂM BẮT ĐẦU CHẠY CỦA ỨNG DỤNG (MAIN ENTRY)
 * =========================================================================
 * 
 * [MỤC ĐÍCH & CHỨC NĂNG]
 * - Lớp khởi tạo chính (Main class) của toàn bộ ứng dụng Desktop ChatSpace.
 * - Cấu hình Look & Feel hiện đại với thư viện FlatLaf (bo tròn, phẳng, tối giản).
 * - Cấu hình địa chỉ máy chủ Backend (mặc định localhost:8080 hoặc qua biến môi trường).
 * - Bật khử răng cưa chữ (Anti-aliasing) để font chữ hiển thị mượt mà trên Windows.
 * - Khởi tạo và hiển thị màn hình Đăng nhập (AuthFrame).
 */
public class ChatSpaceApp {

    public static void main(String[] args) {
        // -------------------------------------------------------------
        // BƯỚC 0: BẬT KHỬ RĂNG CƯA SUBPIXEL CLEARTYPE CHO TOÀN BỘ FONT CHỮ
        // -------------------------------------------------------------
        // Phải đặt trước khi AWT/Swing khởi tạo để font chữ hiển thị sắc nét, mịn màng như web
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");

        // -------------------------------------------------------------
        // BƯỚC 1: CẤU HÌNH ĐỊA CHỈ BACKEND SERVER
        // -------------------------------------------------------------
        // - Ưu tiên 1: Đọc từ biến môi trường CHATSPACE_BACKEND_URL (nếu có).
        // - Ưu tiên 2: Đọc từ tham số dòng lệnh args[0] (khi chạy: java -jar app.jar http://...)
        // - Mặc định: Nếu không truyền gì, ApiClient tự dùng "http://localhost:8080".
        String backendUrl = System.getenv("CHATSPACE_BACKEND_URL");
        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            backendUrl = args[0];
        }
        if (backendUrl != null && !backendUrl.isBlank()) {
            // Cập nhật URL máy chủ vào ApiClient để toàn bộ ứng dụng dùng chung
            ApiClient.setBaseUrl(backendUrl);
        }

        // -------------------------------------------------------------
        // BƯỚC 2: CẤU HÌNH GIAO DIỆN FLATLAF (MODERN LOOK & FEEL)
        // -------------------------------------------------------------
        // Sử dụng thư viện FlatLaf (com.formdev:flatlaf) để giao diện Swing
        // trông hiện đại như ứng dụng Web/Electron thay vì giao diện cổ điển của Java.
        try {
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.setLookAndFeel(new FlatLightLaf());

            // Tùy biến độ bo tròn góc (border radius) cho các thành phần UI:
            UIManager.put("Button.arc", 10);          // Nút bấm bo tròn 10px
            UIManager.put("Component.arc", 10);       // Các component chung bo tròn 10px
            UIManager.put("TextComponent.arc", 10);    // Ô nhập liệu textfield bo tròn 10px
            UIManager.put("ScrollBar.thumbArc", 999);  // Thanh cuộn dạng pill mềm mại
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        } catch (Exception e) {
            System.err.println("Không thể khởi tạo FlatLaf Look and Feel: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // BƯỚC 3: KHỞI CHẠY GIAO DIỆN TRÊN LUỒNG EVENT DISPATCH THREAD (EDT)
        // -------------------------------------------------------------
        SwingUtilities.invokeLater(() -> {
            AuthFrame authFrame = new AuthFrame();
            authFrame.setVisible(true); // Hiển thị màn hình Đăng nhập
        });
    }
}

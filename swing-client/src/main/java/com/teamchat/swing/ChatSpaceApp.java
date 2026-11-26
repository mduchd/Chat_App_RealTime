package com.teamchat.swing;

import com.formdev.flatlaf.FlatLightLaf;
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
 * - Kết nối với máy chủ ChatSpace local.
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
        // CẤU HÌNH GIAO DIỆN FLATLAF
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
        // KHỞI CHẠY GIAO DIỆN TRÊN LUỒNG EVENT DISPATCH THREAD (EDT)
        // -------------------------------------------------------------
        SwingUtilities.invokeLater(() -> {
            AuthFrame authFrame = new AuthFrame();
            authFrame.setVisible(true); // Hiển thị màn hình Đăng nhập
        });
    }
}

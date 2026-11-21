package com.teamchat.swing.util;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Service quản lý thông báo đẩy (Desktop Notification) trên thanh tác vụ hệ thống (System Tray).
 */
public class NotificationService {

    private static TrayIcon trayIcon;
    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        if (!SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x1d, 0x72, 0xb8));
            g.fillOval(0, 0, 16, 16);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g.drawString("C", 4, 12);
            g.dispose();

            trayIcon = new TrayIcon(image, "ChatSpace");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            initialized = true;
        } catch (Exception ignored) {}
    }

    /**
     * Hiển thị thông báo đẩy lên màn hình desktop
     */
    public static void showNotification(String title, String message) {
        try {
            if (!initialized) init();
            if (trayIcon != null) {
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
            }
        } catch (Exception ignored) {}
    }
}

package com.teamchat.swing.ui;

import java.awt.*;

/**
 * Design tokens and colors matching ChatSpace web stylesheet (css/style.css).
 * Supports full Dark/Light theme switching matching body.dark-mode in style.css.
 */
public final class UITheme {

    private UITheme() {}

    public static boolean isDarkMode = false;

    // Dynamic Colors matching css/style.css :root and body.dark-mode
    public static Color PAGE_BG = new Color(0xe8, 0xf1, 0xf8);
    public static Color WINDOW_BG = Color.WHITE;
    public static Color TOPBAR_BG = new Color(0x16, 0x26, 0x3d);
    public static Color TOPBAR_TEXT = Color.WHITE;

    public static Color SIDEBAR_CARD_BG = new Color(0xf2, 0xf6, 0xfb);
    public static Color SIDEBAR_ITEM_HOVER = new Color(0xe6, 0xef, 0xfa);
    public static Color SIDEBAR_ITEM_ACTIVE = new Color(0xd9, 0xea, 0xfc);

    public static Color THEME_PRIMARY = new Color(0x1d, 0x72, 0xb8);
    public static Color THEME_HOVER = new Color(0x15, 0x5d, 0x98);
    public static Color AVATAR_INACTIVE = new Color(0x98, 0xa6, 0xb8);

    public static Color BUBBLE_RECEIVED_BG = new Color(0xed, 0xf2, 0xf7);
    public static Color BUBBLE_RECEIVED_TEXT = new Color(0x1e, 0x29, 0x3b);
    public static Color BUBBLE_SENT_BG = new Color(0x1d, 0x72, 0xb8);
    public static Color BUBBLE_SENT_TEXT = Color.WHITE;

    public static Color TEXT_MAIN = new Color(0x1e, 0x29, 0x3b);
    public static Color TEXT_MUTED = new Color(0x64, 0x74, 0x8b);
    public static Color BORDER_LIGHT = new Color(0xe2, 0xe8, 0xf0);
    public static Color ONLINE_GREEN = new Color(0x10, 0xb9, 0x81);
    public static Color DANGER_RED = new Color(0xef, 0x44, 0x44);

    // Window dots
    public static final Color WIN_DOT_BLUE = new Color(0x60, 0xa5, 0xfa);
    public static final Color WIN_DOT_YELLOW = new Color(0xf5, 0xb0, 0x41);
    public static final Color WIN_DOT_GREEN = new Color(0x2e, 0xcc, 0x71);

    // Standard Fonts (Tỷ lệ phân cấp chuẩn, thanh lịch, đồng đều, không bị chênh lệch to nhỏ)
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 17);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_SUBHEADING = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_MEDIUM = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BOLD_SMALL = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_XS = new Font("Segoe UI", Font.PLAIN, 12);

    /**
     * Chuyển đổi toàn bộ bảng màu theo đúng biến CSS body.dark-mode trong style.css
     */
    public static void setDarkMode(boolean dark) {
        isDarkMode = dark;
        if (dark) {
            // Dark Mode colors từ style.css dòng 30-42
            PAGE_BG = new Color(0x0b, 0x11, 0x20);
            WINDOW_BG = new Color(0x11, 0x18, 0x27);
            TOPBAR_BG = new Color(0x0f, 0x17, 0x2a);
            SIDEBAR_CARD_BG = new Color(0x1e, 0x29, 0x3b);
            SIDEBAR_ITEM_HOVER = new Color(0x27, 0x35, 0x49);
            SIDEBAR_ITEM_ACTIVE = new Color(0x1e, 0x3a, 0x5f);
            BUBBLE_RECEIVED_BG = new Color(0x1e, 0x29, 0x3b);
            BUBBLE_RECEIVED_TEXT = new Color(0xf1, 0xf5, 0xf9);
            TEXT_MAIN = new Color(0xf1, 0xf5, 0xf9);
            TEXT_MUTED = new Color(0x94, 0xa3, 0xb8);
            BORDER_LIGHT = new Color(0x33, 0x41, 0x55);
        } else {
            // Light Mode colors
            PAGE_BG = new Color(0xe8, 0xf1, 0xf8);
            WINDOW_BG = Color.WHITE;
            TOPBAR_BG = new Color(0x16, 0x26, 0x3d);
            SIDEBAR_CARD_BG = new Color(0xf2, 0xf6, 0xfb);
            SIDEBAR_ITEM_HOVER = new Color(0xe6, 0xef, 0xfa);
            SIDEBAR_ITEM_ACTIVE = new Color(0xd9, 0xea, 0xfc);
            BUBBLE_RECEIVED_BG = new Color(0xed, 0xf2, 0xf7);
            BUBBLE_RECEIVED_TEXT = new Color(0x1e, 0x29, 0x3b);
            TEXT_MAIN = new Color(0x1e, 0x29, 0x3b);
            TEXT_MUTED = new Color(0x64, 0x74, 0x8b);
            BORDER_LIGHT = new Color(0xe2, 0xe8, 0xf0);
        }
    }
}

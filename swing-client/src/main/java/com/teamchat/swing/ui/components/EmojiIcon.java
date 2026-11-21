package com.teamchat.swing.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * =========================================================================
 * FILE: EmojiIcon.java - BỘ VẼ EMOJI VECTOR ĐẦY ĐỦ MÀU SẮC (KHÔNG BỊ LỖI Ô VUÔNG □)
 * =========================================================================
 * Vẽ trực tiếp 8 biểu tượng cảm xúc chuẩn theo đúng danh sách trong HTML:
 * 👍 (Thích), ❤️ (Tim), 😂 (Cười), 😮 (Ngạc nhiên), 😢 (Buồn), 🔥 (Lửa), 🎉 (Ăn mừng), 🙌 (Vỗ tay)
 */
public class EmojiIcon implements Icon {

    public enum Type {
        LIKE,   // 👍
        HEART,  // ❤️
        LAUGH,  // 😂
        WOW,    // 😮
        SAD,    // 😢
        FIRE,   // 🔥
        PARTY,  // 🎉
        CLAP    // 🙌
    }

    private final Type type;
    private final int size;

    public EmojiIcon(Type type, int size) {
        this.type = type;
        this.size = size;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        double scale = size / 24.0;
        g2.translate(x, y);
        g2.scale(scale, scale);

        switch (type) {
            case LIKE -> drawLike(g2);
            case HEART -> drawHeart(g2);
            case LAUGH -> drawLaugh(g2);
            case WOW -> drawWow(g2);
            case SAD -> drawSad(g2);
            case FIRE -> drawFire(g2);
            case PARTY -> drawParty(g2);
            case CLAP -> drawClap(g2);
        }

        g2.dispose();
    }

    // 1. 👍 LIKE (Ngón cái giơ lên màu xanh/vàng)
    private void drawLike(Graphics2D g) {
        g.setColor(new Color(0x3b, 0x82, 0xf6)); // Blue
        // Cổ tay
        g.fillRoundRect(2, 11, 4, 10, 2, 2);
        // Bàn tay & ngón cái
        Path2D p = new Path2D.Float();
        p.moveTo(7, 12);
        p.lineTo(9, 6);
        p.curveTo(9, 4, 11, 3, 12.5f, 4.5f);
        p.curveTo(13.5f, 6, 12.5f, 9, 12, 10);
        p.lineTo(19, 10);
        p.curveTo(21, 10, 21.5f, 12, 20.5f, 13.5f);
        p.curveTo(21.5f, 14.5f, 21, 16, 19.5f, 16.5f);
        p.curveTo(20.5f, 17.5f, 19.5f, 19, 18, 19.5f);
        p.curveTo(18.5f, 20, 17.5f, 21, 16, 21);
        p.lineTo(7, 21);
        p.closePath();
        g.fill(p);
    }

    // 2. ❤️ HEART (Trái tim đỏ rực)
    private void drawHeart(Graphics2D g) {
        g.setColor(new Color(0xef, 0x44, 0x44)); // Vivid Red
        Path2D p = new Path2D.Float();
        p.moveTo(12, 19.5f);
        p.curveTo(4.5f, 13.5f, 2, 8, 6, 4.5f);
        p.curveTo(9, 2.2f, 11.2f, 4.2f, 12, 5.8f);
        p.curveTo(12.8f, 4.2f, 15, 2.2f, 18, 4.5f);
        p.curveTo(22, 8, 19.5f, 13.5f, 12, 19.5f);
        p.closePath();
        g.fill(p);
    }

    // 3. 😂 LAUGH (Cười tít mắt có giọt nước mắt)
    private void drawLaugh(Graphics2D g) {
        // Mặt vàng
        g.setColor(new Color(0xfb, 0xbf, 0x24));
        g.fillOval(2, 2, 20, 20);

        // Mắt cười ^ ^
        g.setColor(new Color(0x78, 0x35, 0x0f));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(5, 7, 5, 4, 0, 180);
        g.drawArc(14, 7, 5, 4, 0, 180);

        // Miệng cười mở rộng
        g.drawArc(6, 10, 12, 8, 180, 180);
        g.setColor(new Color(0xb9, 0x1c, 0x1c));
        g.fillArc(6, 10, 12, 8, 180, 180);

        // Giọt nước mắt 2 bên
        g.setColor(new Color(0x38, 0xbd, 0xf8));
        g.fillOval(3, 11, 4, 4);
        g.fillOval(17, 11, 4, 4);
    }

    // 4. 😮 WOW (Mắt tròn xoe, mồm chữ O)
    private void drawWow(Graphics2D g) {
        g.setColor(new Color(0xfb, 0xbf, 0x24));
        g.fillOval(2, 2, 20, 20);

        // Mắt tròn
        g.setColor(new Color(0x78, 0x35, 0x0f));
        g.fillOval(6, 7, 4, 5);
        g.fillOval(14, 7, 4, 5);

        // Miệng chữ O
        g.fillOval(9, 13, 6, 7);
        g.setColor(new Color(0x45, 0x1a, 0x03));
        g.fillOval(10, 14, 4, 5);
    }

    // 5. 😢 SAD (Mặt buồn mếu, có giọt nước mắt rơi)
    private void drawSad(Graphics2D g) {
        g.setColor(new Color(0xfb, 0xbf, 0x24));
        g.fillOval(2, 2, 20, 20);

        // Mắt
        g.setColor(new Color(0x78, 0x35, 0x0f));
        g.fillOval(6, 8, 3, 4);
        g.fillOval(15, 8, 3, 4);

        // Miệng mếu xuống
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(7, 15, 10, 6, 0, 180);

        // Giọt nước mắt rơi bên má
        g.setColor(new Color(0x38, 0xbd, 0xf8));
        Path2D drop = new Path2D.Float();
        drop.moveTo(18, 12);
        drop.curveTo(16.5f, 15, 16.5f, 17, 18, 17);
        drop.curveTo(19.5f, 17, 19.5f, 15, 18, 12);
        drop.closePath();
        g.fill(drop);
    }

    // 6. 🔥 FIRE (Ngọn lửa bốc cháy)
    private void drawFire(Graphics2D g) {
        // Lửa ngoài màu cam đỏ
        g.setColor(new Color(0xea, 0x58, 0x0c));
        Path2D p = new Path2D.Float();
        p.moveTo(12, 2);
        p.curveTo(14, 6, 17, 7, 18, 11);
        p.curveTo(19.5f, 15, 18, 19, 15, 21);
        p.curveTo(10, 22.5f, 4, 18, 6, 12);
        p.curveTo(7, 9, 9, 8, 9, 6);
        p.curveTo(10, 5, 11, 3, 12, 2);
        p.closePath();
        g.fill(p);

        // Lửa trong màu vàng rực
        g.setColor(new Color(0xfd, 0xba, 0x74));
        Path2D inner = new Path2D.Float();
        inner.moveTo(12, 9);
        inner.curveTo(13.5f, 12, 15, 13, 15, 16);
        inner.curveTo(15, 19, 13, 20.5f, 11, 20);
        inner.curveTo(8, 19, 7.5f, 15, 10, 13);
        inner.curveTo(11, 11, 11.5f, 10, 12, 9);
        inner.closePath();
        g.fill(inner);
    }

    // 7. 🎉 PARTY (Pháo hoa nổ ăn mừng)
    private void drawParty(Graphics2D g) {
        // Nón loa màu vàng
        g.setColor(new Color(0xf5, 0x9e, 0x0b));
        Path2D cone = new Path2D.Float();
        cone.moveTo(3, 21);
        cone.lineTo(13, 11);
        cone.lineTo(17, 15);
        cone.closePath();
        g.fill(cone);

        // Vạch sọc trên nón
        g.setColor(new Color(0xef, 0x44, 0x44));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(6, 18, 10, 14);

        // Các tia sáng & chấm kim tuyến bung tỏa
        g.setColor(new Color(0xef, 0x44, 0x44));
        g.fillOval(17, 6, 3, 3);

        g.setColor(new Color(0x3b, 0x82, 0xf6));
        g.fillOval(20, 11, 3, 3);

        g.setColor(new Color(0x10, 0xb9, 0x81));
        g.fillOval(13, 4, 3, 3);

        g.setColor(new Color(0xa8, 0x55, 0xf7));
        g.fillOval(18, 17, 2, 2);
    }

    // 8. 🙌 CLAP (Hai bàn tay giơ lên chúc mừng)
    private void drawClap(Graphics2D g) {
        g.setColor(new Color(0xfb, 0xbf, 0x24));
        // Bàn tay trái
        Path2D left = new Path2D.Float();
        left.moveTo(4, 18);
        left.lineTo(7, 8);
        left.curveTo(7.5f, 6, 9.5f, 6.5f, 9.5f, 8.5f);
        left.lineTo(9.5f, 14);
        left.lineTo(11, 14);
        left.lineTo(9, 21);
        left.closePath();
        g.fill(left);

        // Bàn tay phải
        Path2D right = new Path2D.Float();
        right.moveTo(20, 18);
        right.lineTo(17, 8);
        right.curveTo(16.5f, 6, 14.5f, 6.5f, 14.5f, 8.5f);
        right.lineTo(14.5f, 14);
        right.lineTo(13, 14);
        right.lineTo(15, 21);
        right.closePath();
        g.fill(right);

        // Tia chúc mừng trên đỉnh
        g.setColor(new Color(0xf5, 0x9e, 0x0b));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(12, 3, 12, 6);
        g.drawLine(8, 4, 9, 6);
        g.drawLine(16, 4, 15, 6);
    }
}

package com.teamchat.swing.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * =========================================================================
 * FILE: VectorIcon.java - BỘ ICON VECTOR OUTLINE CHUYÊN NGHIỆP (FEATHER / LUCIDE STYLE)
 * =========================================================================
 * 
 * [ĐẶC ĐIỂM]
 * - Vẽ 100% bằng Java Graphics2D Vector (Path2D, CAP_ROUND, JOIN_ROUND).
 * - Hiển thị sắc nét ở mọi độ phân giải màn hình (Retina / 4K / HD).
 * - Không bị lỗi font Unicode □, không cần tải thêm file ảnh bên ngoài.
 * - Cho phép tùy biến kích thước (size) và màu sắc (color) linh hoạt.
 */
public class VectorIcon implements Icon {

    public enum Type {
        CHAT,           // Bong bóng chat
        FRIENDS,        // Hai người bạn
        ADD_GROUP,      // Thêm nhóm / Bạn bè +
        SETTINGS,       // Bánh răng cài đặt
        SEARCH,         // Kính lúp tìm kiếm
        BELL,           // Chuông thông báo
        PIN,            // Ghim
        ARCHIVE,        // Lưu trữ / Hộp tài liệu
        MORE_VERTICAL,  // Menu 3 chấm dọc
        SEND,           // Máy bay gửi tin nhắn
        SMILE,          // Mặt cười emoji
        TRASH,          // Thùng rác xóa
        BLOCK,          // Vòng tròn cấm
        GRID,           // 4 ô vuông dashboard (như mẫu bạn gửi)
        LIST,           // Danh sách 3 dòng + chấm (như mẫu bạn gửi)
        CALENDAR,       // Lịch / Sự kiện (như mẫu bạn gửi)
        SCAN,           // Khung quét QR / Barcode (như mẫu bạn gửi)
        LOGOUT,         // Đăng xuất
        MOON,           // Mặt trăng khuyết (Chế độ tối / Dark Mode)
        SUN             // Mặt trời (Chế độ sáng / Light Mode)
    }

    private final Type type;
    private final int size;
    private Color color;
    private final float strokeWidth;

    public VectorIcon(Type type, int size, Color color) {
        this(type, size, color, 1.8f);
    }

    public VectorIcon(Type type, int size, Color color, float strokeWidth) {
        this.type = type;
        this.size = size;
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    public void setColor(Color color) {
        this.color = color;
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
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Chuẩn hóa tọa độ dựa trên kích thước chuẩn 24x24
        double scale = size / 24.0;
        g2.translate(x, y);
        g2.scale(scale, scale);

        g2.setColor(color != null ? color : (c != null ? c.getForeground() : Color.BLACK));
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (type) {
            case CHAT -> drawChat(g2);
            case FRIENDS -> drawFriends(g2);
            case ADD_GROUP -> drawAddGroup(g2);
            case SETTINGS -> drawSettings(g2);
            case SEARCH -> drawSearch(g2);
            case BELL -> drawBell(g2);
            case PIN -> drawPin(g2);
            case ARCHIVE -> drawArchive(g2);
            case MORE_VERTICAL -> drawMoreVertical(g2);
            case SEND -> drawSend(g2);
            case SMILE -> drawSmile(g2);
            case TRASH -> drawTrash(g2);
            case BLOCK -> drawBlock(g2);
            case GRID -> drawGrid(g2);
            case LIST -> drawList(g2);
            case CALENDAR -> drawCalendar(g2);
            case SCAN -> drawScan(g2);
            case LOGOUT -> drawLogout(g2);
            case MOON -> drawMoon(g2);
            case SUN -> drawSun(g2);
        }

        g2.dispose();
    }

    // 1. Bong bóng chat bo góc thanh thoát
    private void drawChat(Graphics2D g) {
        Path2D p = new Path2D.Float();
        p.moveTo(21, 14);
        p.curveTo(21, 17.5, 17.5, 20.5, 13, 20.5);
        p.curveTo(11.5, 20.5, 9.8, 20.1, 8.5, 19.5);
        p.lineTo(3.5, 20.5);
        p.lineTo(4.8, 16.5);
        p.curveTo(3.7, 15.2, 3, 13.7, 3, 12);
        p.curveTo(3, 7.5, 7.5, 4, 12.5, 4);
        p.curveTo(17.5, 4, 21, 7.5, 21, 12);
        g.draw(p);
    }

    // 2. Hai người bạn (Friends)
    private void drawFriends(Graphics2D g) {
        // Người trước
        g.drawOval(6, 4, 6, 6);
        Path2D body1 = new Path2D.Float();
        body1.moveTo(3, 19);
        body1.curveTo(3, 15, 6, 14, 9, 14);
        body1.curveTo(12, 14, 15, 15, 15, 19);
        g.draw(body1);

        // Người sau
        Path2D head2 = new Path2D.Float();
        head2.append(new Arc2D.Float(13, 5, 5, 5, -80, 190, Arc2D.OPEN), false);
        g.draw(head2);

        Path2D body2 = new Path2D.Float();
        body2.moveTo(15, 14.5);
        body2.curveTo(17.5, 14.8, 20, 16, 20, 19);
        g.draw(body2);
    }

    // 3. Thêm nhóm / Bạn bè +
    private void drawAddGroup(Graphics2D g) {
        g.drawOval(6, 4, 6, 6);
        Path2D body = new Path2D.Float();
        body.moveTo(3, 19);
        body.curveTo(3, 15, 6, 14, 9, 14);
        body.curveTo(12, 14, 15, 15, 15, 19);
        g.draw(body);

        // Dấu +
        g.drawLine(18, 9, 18, 15);
        g.drawLine(15, 12, 21, 12);
    }

    // 4. Bánh răng cài đặt (Settings / Gear cơ khí chuẩn 6 răng)
    private void drawSettings(Graphics2D g) {
        // Lỗ tròn trung tâm
        g.drawOval(9, 9, 6, 6);
        
        // Vành bánh răng ngoài với 6 răng nổi khối cơ khí rõ rệt
        Path2D gear = new Path2D.Float();
        int teeth = 6;
        for (int i = 0; i < teeth; i++) {
            double a1 = Math.toRadians(i * 60 - 16);
            double a2 = Math.toRadians(i * 60 - 8);
            double a3 = Math.toRadians(i * 60 + 8);
            double a4 = Math.toRadians(i * 60 + 16);
            
            float rInner = 7.0f;
            float rOuter = 10.2f;
            
            float x1 = (float) (12 + rInner * Math.cos(a1));
            float y1 = (float) (12 + rInner * Math.sin(a1));
            float x2 = (float) (12 + rOuter * Math.cos(a2));
            float y2 = (float) (12 + rOuter * Math.sin(a2));
            float x3 = (float) (12 + rOuter * Math.cos(a3));
            float y3 = (float) (12 + rOuter * Math.sin(a3));
            float x4 = (float) (12 + rInner * Math.cos(a4));
            float y4 = (float) (12 + rInner * Math.sin(a4));
            
            if (i == 0) {
                gear.moveTo(x1, y1);
            } else {
                gear.lineTo(x1, y1);
            }
            gear.lineTo(x2, y2);
            gear.lineTo(x3, y3);
            gear.lineTo(x4, y4);
        }
        gear.closePath();
        g.draw(gear);
    }

    // 5. Kính lúp (Search)
    private void drawSearch(Graphics2D g) {
        g.drawOval(4, 4, 11, 11);
        g.drawLine(13, 13, 20, 20);
    }

    // 6. Chuông (Bell)
    private void drawBell(Graphics2D g) {
        Path2D p = new Path2D.Float();
        p.moveTo(12, 3);
        p.curveTo(8.5, 3, 6, 6, 6, 11);
        p.curveTo(6, 15, 4, 17, 4, 17);
        p.lineTo(20, 17);
        p.curveTo(20, 17, 18, 15, 18, 11);
        p.curveTo(18, 6, 15.5, 3, 12, 3);
        p.closePath();
        g.draw(p);
        g.draw(new Arc2D.Float(10, 18, 4, 3, 0, -180, Arc2D.OPEN));
    }

    // 7. Ghim (Pin)
    private void drawPin(Graphics2D g) {
        g.drawRoundRect(8, 4, 8, 4, 2, 2);
        g.drawLine(10, 8, 10, 13);
        g.drawLine(14, 8, 14, 13);
        g.drawLine(6, 13, 18, 13);
        g.drawLine(12, 13, 12, 21);
    }

    // 8. Hộp lưu trữ (Archive)
    private void drawArchive(Graphics2D g) {
        g.drawRoundRect(4, 4, 16, 5, 2, 2);
        Path2D p = new Path2D.Float();
        p.moveTo(5, 9);
        p.lineTo(5, 19);
        p.curveTo(5, 20, 6, 20, 7, 20);
        p.lineTo(17, 20);
        p.curveTo(18, 20, 19, 20, 19, 19);
        p.lineTo(19, 9);
        g.draw(p);
        g.drawLine(10, 13, 14, 13);
    }

    // 9. Ba chấm dọc (More)
    private void drawMoreVertical(Graphics2D g) {
        g.fillOval(11, 5, 2, 2);
        g.fillOval(11, 11, 2, 2);
        g.fillOval(11, 17, 2, 2);
    }

    // 10. Gửi tin nhắn (Send)
    private void drawSend(Graphics2D g) {
        Path2D p = new Path2D.Float();
        p.moveTo(21, 3);
        p.lineTo(10, 14);
        p.moveTo(21, 3);
        p.lineTo(14, 21);
        p.lineTo(10, 14);
        p.lineTo(3, 10);
        p.closePath();
        g.draw(p);
    }

    // 11. Mặt cười (Smile)
    private void drawSmile(Graphics2D g) {
        g.drawOval(3, 3, 18, 18);
        g.fillOval(8, 9, 2, 2);
        g.fillOval(14, 9, 2, 2);
        g.draw(new Arc2D.Float(8, 10, 8, 7, 200, 140, Arc2D.OPEN));
    }

    // 12. Thùng rác (Trash)
    private void drawTrash(Graphics2D g) {
        g.drawLine(4, 6, 20, 6);
        g.drawLine(9, 6, 9, 4);
        g.drawLine(9, 4, 15, 4);
        g.drawLine(15, 4, 15, 6);
        Path2D p = new Path2D.Float();
        p.moveTo(6, 6);
        p.lineTo(7, 19);
        p.curveTo(7, 20, 8, 20, 9, 20);
        p.lineTo(15, 20);
        p.curveTo(16, 20, 17, 20, 17, 19);
        p.lineTo(18, 6);
        g.draw(p);
        g.drawLine(10, 10, 10, 16);
        g.drawLine(14, 10, 14, 16);
    }

    // 13. Chặn (Block)
    private void drawBlock(Graphics2D g) {
        g.drawOval(4, 4, 16, 16);
        g.drawLine(7, 7, 17, 17);
    }

    // 14. Grid 4 ô vuông dashboard (Khớp 100% mẫu bạn gửi)
    private void drawGrid(Graphics2D g) {
        g.drawRoundRect(4, 4, 6, 6, 2, 2);
        g.drawRoundRect(14, 4, 6, 6, 2, 2);
        g.drawRoundRect(4, 14, 6, 6, 2, 2);
        g.drawRoundRect(14, 14, 6, 6, 2, 2);
    }

    // 15. Danh sách 3 dòng kèm chấm tròn (Khớp 100% mẫu bạn gửi)
    private void drawList(Graphics2D g) {
        g.fillOval(4, 6, 2, 2);
        g.drawLine(9, 7, 20, 7);

        g.fillOval(4, 11, 2, 2);
        g.drawLine(9, 12, 20, 12);

        g.fillOval(4, 16, 2, 2);
        g.drawLine(9, 17, 20, 17);
    }

    // 16. Lịch / Calendar (Khớp 100% mẫu bạn gửi)
    private void drawCalendar(Graphics2D g) {
        g.drawRoundRect(4, 6, 16, 14, 4, 4);
        g.drawLine(4, 10, 20, 10);
        g.drawLine(8, 3, 8, 6);
        g.drawLine(16, 3, 16, 6);
        g.drawRoundRect(7, 13, 4, 3, 1, 1);
    }

    // 17. Quét mã / Scanner (Khớp 100% icon tab đang active trong ảnh mẫu)
    private void drawScan(Graphics2D g) {
        // 4 góc khung quét bo tròn
        g.draw(new Arc2D.Float(3, 4, 6, 6, 90, 90, Arc2D.OPEN));
        g.draw(new Arc2D.Float(15, 4, 6, 6, 0, 90, Arc2D.OPEN));
        g.draw(new Arc2D.Float(3, 14, 6, 6, 180, 90, Arc2D.OPEN));
        g.draw(new Arc2D.Float(15, 14, 6, 6, 270, 90, Arc2D.OPEN));

        // Thanh quét ngang
        g.drawLine(4, 12, 20, 12);

        // Các vạch barcode đứng
        g.drawLine(8, 9, 8, 11);
        g.drawLine(12, 8, 12, 11);
        g.drawLine(16, 9, 16, 11);
        g.drawLine(8, 13, 8, 15);
        g.drawLine(12, 13, 12, 16);
        g.drawLine(16, 13, 16, 15);
    }

    // 18. Đăng xuất
    private void drawLogout(Graphics2D g) {
        Path2D p = new Path2D.Float();
        p.moveTo(10, 4);
        p.lineTo(5, 4);
        p.curveTo(4, 4, 4, 5, 4, 6);
        p.lineTo(4, 18);
        p.curveTo(4, 19, 4, 20, 5, 20);
        p.lineTo(10, 20);
        g.draw(p);
        g.drawLine(9, 12, 20, 12);
        g.drawLine(16, 8, 20, 12);
        g.drawLine(16, 16, 20, 12);
    }

    // 19. Mặt trăng khuyết (Chế độ tối / Dark Mode)
    private void drawMoon(Graphics2D g) {
        Path2D p = new Path2D.Float();
        p.moveTo(21, 12.8f);
        p.curveTo(19.8f, 17.5f, 15.5f, 21f, 10.5f, 21f);
        p.curveTo(5.2f, 21f, 2.5f, 16.5f, 2.5f, 11.5f);
        p.curveTo(2.5f, 6.8f, 5.8f, 3.2f, 10.2f, 2.2f);
        p.curveTo(9.5f, 4.0f, 9.8f, 6.2f, 10.8f, 7.8f);
        p.curveTo(12.3f, 10.2f, 15.2f, 11.5f, 18.2f, 11.2f);
        p.curveTo(19.3f, 11.1f, 20.3f, 11.8f, 21f, 12.8f);
        p.closePath();
        g.draw(p);
    }

    // 20. Mặt trời (Chế độ sáng / Light Mode)
    private void drawSun(Graphics2D g) {
        g.drawOval(8, 8, 8, 8);
        g.drawLine(12, 2, 12, 5);
        g.drawLine(12, 19, 12, 22);
        g.drawLine(2, 12, 5, 12);
        g.drawLine(19, 12, 22, 12);
        g.drawLine(5, 5, 7, 7);
        g.drawLine(17, 17, 19, 19);
        g.drawLine(5, 19, 7, 17);
        g.drawLine(17, 7, 19, 5);
    }
}

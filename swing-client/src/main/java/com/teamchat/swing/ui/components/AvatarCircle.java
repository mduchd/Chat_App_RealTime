package com.teamchat.swing.ui.components;

import com.teamchat.swing.api.ApiClient;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Circular avatar component that displays user/group initials on a colored background
 * or a loaded avatar image (local file, uploaded URL, or web link),
 * matching ChatSpace web design.
 */
public class AvatarCircle extends JComponent {

    private static final Map<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService IMAGE_LOADER = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "AvatarImageLoader");
        t.setDaemon(true);
        return t;
    });

    private String text;
    private int size;
    private Color bgColor;
    private Color textColor;
    private boolean online;
    private String imageUrl;
    private Image cachedImage;

    public AvatarCircle(String text, int size) {
        this(text, size, new Color(0x98, 0xa6, 0xb8), Color.WHITE);
    }

    public AvatarCircle(String text, int size, Color bgColor, Color textColor) {
        this.text = text != null ? text.toUpperCase() : "?";
        this.size = size;
        this.bgColor = bgColor;
        this.textColor = textColor;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setOpaque(false);
    }

    public void setText(String text) {
        this.text = text != null ? text.toUpperCase() : "?";
        repaint();
    }

    public void setBgColor(Color bgColor) {
        this.bgColor = bgColor;
        repaint();
    }

    public void setOnline(boolean online) {
        this.online = online;
        repaint();
    }

    public void setImage(Image image) {
        this.cachedImage = image;
        repaint();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String url) {
        this.imageUrl = url;
        if (url == null || url.trim().isEmpty()) {
            this.cachedImage = null;
            repaint();
            return;
        }

        String finalUrl = url.trim();
        // Kiểm tra trong bộ nhớ đệm
        if (IMAGE_CACHE.containsKey(finalUrl)) {
            this.cachedImage = IMAGE_CACHE.get(finalUrl);
            repaint();
            return;
        }

        // Tải ảnh ngầm (Asynchronous) không làm đơ giao diện
        IMAGE_LOADER.submit(() -> {
            try {
                Image img = null;
                if (finalUrl.startsWith("http://") || finalUrl.startsWith("https://")) {
                    URL u = URI.create(finalUrl).toURL();
                    img = ImageIO.read(u);
                } else if (finalUrl.startsWith("/uploads/")) {
                    // Endpoint ảnh upload trên backend
                    String fullUrl = ApiClient.getBaseUrl() + finalUrl;
                    URL u = URI.create(fullUrl).toURL();
                    img = ImageIO.read(u);
                } else {
                    File f = new File(finalUrl);
                    if (f.exists()) {
                        img = ImageIO.read(f);
                    }
                }

                if (img != null) {
                    IMAGE_CACHE.put(finalUrl, img);
                    final Image loadedImg = img;
                    SwingUtilities.invokeLater(() -> {
                        if (finalUrl.equals(this.imageUrl)) {
                            this.cachedImage = loadedImg;
                            repaint();
                        }
                    });
                }
            } catch (Exception ignored) {
                // Nếu tải ảnh thất bại, component sẽ tự hiển thị chữ cái đại diện (initials)
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (cachedImage != null) {
            // Cắt khung tròn và vẽ ảnh đại diện
            Shape oldClip = g2.getClip();
            g2.setClip(new Ellipse2D.Float(0, 0, size, size));
            g2.drawImage(cachedImage, 0, 0, size, size, null);
            g2.setClip(oldClip);

            // Viền mỏng tinh tế quanh avatar
            g2.setColor(new Color(0, 0, 0, 25));
            g2.draw(new Ellipse2D.Float(0, 0, size - 1, size - 1));
        } else {
            // Draw circle background
            g2.setColor(bgColor);
            g2.fill(new Ellipse2D.Float(0, 0, size, size));

            // Draw initials centered
            g2.setColor(textColor);
            int fontSize = Math.max(10, size * 38 / 100);
            g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getAscent();
            int x = (size - textWidth) / 2;
            int y = (size + textHeight) / 2 - 2;
            g2.drawString(text, x, y);
        }

        // Optional online indicator badge
        if (online) {
            int dotSize = Math.max(8, size / 4);
            int dotX = size - dotSize;
            int dotY = size - dotSize;
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Float(dotX - 1, dotY - 1, dotSize + 2, dotSize + 2));
            g2.setColor(new Color(0x10, 0xb9, 0x81)); // online green
            g2.fill(new Ellipse2D.Float(dotX, dotY, dotSize, dotSize));
        }

        g2.dispose();
    }
}

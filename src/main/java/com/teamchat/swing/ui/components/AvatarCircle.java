package com.teamchat.swing.ui.components;

import com.teamchat.swing.api.ApiClient;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Circular avatar component that displays user/group initials on a colored background
 * or a loaded avatar image (local file, uploaded URL, or web link),
 * matching ChatSpace web design.
 */
public class AvatarCircle extends JComponent {

    private static final Map<String, CompletableFuture<Image>> IMAGE_CACHE = new ConcurrentHashMap<>();
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
    private String imageSource;
    private Image cachedImage;
    private final Set<String> pendingRepaints = new HashSet<>();

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

    public void setImageUrl(String url) {
        setImageUrl(url, this);
    }

    public void setImageUrl(String url, JComponent repaintTarget) {
        String source = url == null ? "" : url.strip();
        if (source.startsWith("/uploads/")) {
            source = ApiClient.getBaseUrl() + source;
        } else if (!source.regionMatches(true, 0, "https://", 0, 8)
                && !source.regionMatches(true, 0, "http://", 0, 7)) {
            source = null;
        }
        setSource(source, repaintTarget);
    }

    public void setImageFile(File file) {
        setSource(file.toURI().toString(), this);
    }

    private void setSource(String source, JComponent repaintTarget) {
        if (!Objects.equals(source, imageSource)) {
            imageSource = source;
            cachedImage = null;
            repaint();
        }
        if (source == null || cachedImage != null) return;

        CompletableFuture<Image> load = IMAGE_CACHE.computeIfAbsent(source, AvatarCircle::loadAsync);
        if (load.isDone()) {
            cachedImage = load.getNow(null);
            return;
        }
        if (pendingRepaints.add(source)) {
            load.thenAccept(image -> SwingUtilities.invokeLater(() -> {
                pendingRepaints.remove(source);
                if (source.equals(imageSource)) {
                    cachedImage = image;
                }
                // A reused list renderer must repaint the list once the image is available.
                repaintTarget.repaint();
            }));
        }
    }

    private static CompletableFuture<Image> loadAsync(String source) {
        CompletableFuture<Image> load = CompletableFuture.supplyAsync(() -> readImage(source), IMAGE_LOADER);
        load.thenAccept(image -> {
            if (image == null) {
                CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS)
                        .execute(() -> IMAGE_CACHE.remove(source, load));
            }
        });
        return load;
    }

    private static Image readImage(String source) {
        try {
            URI uri = URI.create(source);
            if ("file".equals(uri.getScheme())) {
                return ImageIO.read(new File(uri));
            }
            var connection = uri.toURL().openConnection();
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(5_000);
            connection.setRequestProperty("Accept", "image/png,image/jpeg,image/gif");
            try (var stream = connection.getInputStream()) {
                return ImageIO.read(stream);
            }
        } catch (java.io.IOException | IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int imageWidth = cachedImage == null ? 0 : cachedImage.getWidth(this);
        int imageHeight = cachedImage == null ? 0 : cachedImage.getHeight(this);
        if (imageWidth > 0 && imageHeight > 0) {
            // Cắt khung tròn và vẽ ảnh đại diện
            Shape oldClip = g2.getClip();
            g2.setClip(new Ellipse2D.Float(0, 0, size, size));
            double scale = Math.max((double) size / imageWidth, (double) size / imageHeight);
            int width = (int) Math.ceil(imageWidth * scale);
            int height = (int) Math.ceil(imageHeight * scale);
            g2.drawImage(cachedImage, (size - width) / 2, (size - height) / 2, width, height, this);
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

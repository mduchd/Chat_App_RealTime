package com.teamchat.swing.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Button with custom rounded corners and smooth antialiasing,
 * matching CSS border-radius rules from ChatSpace web stylesheet.
 */
public class RoundedButton extends JButton {

    private int radius;
    private Color normalBg;
    private Color hoverBg;
    private Color borderColor;
    private boolean isHover = false;

    public RoundedButton(String text, int radius, Color bg, Color fg) {
        this(text, radius, bg, fg, null);
    }

    public RoundedButton(String text, int radius, Color bg, Color fg, Color borderColor) {
        super(text);
        this.radius = radius;
        this.normalBg = bg;
        this.hoverBg = createHoverColor(bg);
        this.borderColor = borderColor;

        setForeground(fg);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                isHover = true;
                repaint();
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                isHover = false;
                repaint();
            }
        });
    }

    public void setCustomBackground(Color bg) {
        this.normalBg = bg;
        this.hoverBg = createHoverColor(bg);
        repaint();
    }

    public void setCustomBorderColor(Color border) {
        this.borderColor = border;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill rounded background
        g2.setColor(isHover && isEnabled() ? hoverBg : normalBg);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));

        // Draw border if set
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, radius, radius));
        }

        g2.dispose();
        super.paintComponent(g);
    }

    private Color createHoverColor(Color c) {
        if (c == null) return null;
        if (c.equals(Color.WHITE)) return new Color(0xf1, 0xf5, 0xf9);
        int r = Math.max(0, c.getRed() - 15);
        int g = Math.max(0, c.getGreen() - 15);
        int b = Math.max(0, c.getBlue() - 15);
        return new Color(r, g, b, c.getAlpha());
    }
}

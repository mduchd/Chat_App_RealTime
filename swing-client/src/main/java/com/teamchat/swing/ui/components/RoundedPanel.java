package com.teamchat.swing.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Panel with rounded corners, matching ChatSpace design.
 */
public class RoundedPanel extends JPanel {

    private int cornerRadius;
    private Color borderColor;

    public RoundedPanel(int radius) {
        this(radius, null);
    }

    public RoundedPanel(int radius, Color borderColor) {
        super();
        this.cornerRadius = radius;
        this.borderColor = borderColor;
        setOpaque(false);
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(getBackground());
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));

        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius));
        }

        g2.dispose();
    }
}

package com.teamchat.swing.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Password field with rounded border (radius: 10px), matching CSS .auth-input
 */
public class RoundedPasswordField extends JPasswordField {

    private int radius;
    private Color borderColor;
    private Color focusColor;

    public RoundedPasswordField(int radius, Color borderColor, Color focusColor) {
        this.radius = radius;
        this.borderColor = borderColor;
        this.focusColor = focusColor;
        setOpaque(false);
        setBorder(new EmptyBorder(6, 12, 6, 12));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // White background
        g2.setColor(getBackground());
        g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, radius, radius));

        // Rounded border
        g2.setColor(hasFocus() ? focusColor : borderColor);
        g2.setStroke(new BasicStroke(hasFocus() ? 1.5f : 1.0f));
        g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, radius, radius));

        g2.dispose();
        super.paintComponent(g);
    }
}

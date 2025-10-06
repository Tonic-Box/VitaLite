package com.tonic.services.hotswapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import net.runelite.client.util.ImageUtil;

public class CycleButton extends JButton {
    private boolean isHovered = false;
    private final BufferedImage normalImage;
    private final BufferedImage hoverImage;

    public CycleButton() {
        BufferedImage cycleImage = ImageUtil.loadImageResource(CycleButton.class, "cycle.png");
        this.normalImage = cycleImage;
        this.hoverImage = cycleImage != null ? ImageUtil.luminanceScale(cycleImage, 1.3f) : null;
        setPreferredSize(new Dimension(20, 20));
        setMinimumSize(new Dimension(20, 20));
        setMaximumSize(new Dimension(20, 20));
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setToolTipText("Reload plugin from disk.");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                setCursor(Cursor.getDefaultCursor());
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        BufferedImage imageToDraw = isHovered ? hoverImage : normalImage;
        if (imageToDraw != null) {
            int imgWidth = imageToDraw.getWidth();
            int imgHeight = imageToDraw.getHeight();
            int targetSize = Math.min(getWidth() - 4, getHeight() - 4);
            double scale = targetSize / (double) Math.max(imgWidth, imgHeight);
            int scaledWidth = (int) (imgWidth * scale);
            int scaledHeight = (int) (imgHeight * scale);
            int x = (getWidth() - scaledWidth) / 2;
            int y = (getHeight() - scaledHeight) / 2;
            g2.drawImage(imageToDraw, x, y, scaledWidth, scaledHeight, null);
        }

        g2.dispose();
    }

    @Override
    public void setContentAreaFilled(boolean b) {
        // Override to prevent default painting
    }
}
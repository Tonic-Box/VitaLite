package com.tonic.services;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Overlay to visualize where clicks are being sent in the client
 */
@Singleton
public class ClickVisualizationOverlay extends Overlay {

    private static final CopyOnWriteArrayList<ClickVisualization> recentClicks = new CopyOnWriteArrayList<>();
    private static final long CLICK_DISPLAY_DURATION_MS = 2000; // Show clicks for 2 seconds
    private static boolean enabled = true;

    @Inject
    private Client client;

    public ClickVisualizationOverlay() {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!enabled || client == null) {
            return null;
        }

        long currentTime = System.currentTimeMillis();

        recentClicks.removeIf(click -> currentTime - click.timestamp > CLICK_DISPLAY_DURATION_MS);

        for (ClickVisualization click : recentClicks) {
            renderClick(graphics, click, currentTime);
        }

        return null;
    }

    private void renderClick(Graphics2D graphics, ClickVisualization click, long currentTime) {
        long age = currentTime - click.timestamp;
        float progress = (float) age / CLICK_DISPLAY_DURATION_MS;

        int alpha = (int) (255 * (1.0f - progress));

        Color color = click.getColor();
        Color fadedColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);

        graphics.setColor(fadedColor);
        graphics.setStroke(new BasicStroke(2));

        int size = 10 + (int) (5 * progress);
        graphics.drawLine(click.x - size, click.y, click.x + size, click.y);
        graphics.drawLine(click.x, click.y - size, click.x, click.y + size);

        int circleSize = (int) (15 + 10 * progress);
        graphics.drawOval(click.x - circleSize / 2, click.y - circleSize / 2, circleSize, circleSize);

        if (click.label != null && !click.label.isEmpty()) {
            graphics.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = graphics.getFontMetrics();
            int textWidth = fm.stringWidth(click.label);
            int textX = click.x - textWidth / 2;
            int textY = click.y - circleSize / 2 - 5;

            graphics.setColor(new Color(0, 0, 0, alpha / 2));
            graphics.fillRect(textX - 2, textY - fm.getHeight() + 2, textWidth + 4, fm.getHeight());

            graphics.setColor(fadedColor);
            graphics.drawString(click.label, textX, textY);
        }
    }

    /**
     * Record a click at the specified location
     */
    public static void recordClick(int x, int y, ClickType type, String label) {
        if (!enabled) {
            return;
        }
        recentClicks.add(new ClickVisualization(x, y, type, label, System.currentTimeMillis()));
    }

    /**
     * Record a click at the specified Point
     */
    public static void recordClick(Point point, ClickType type, String label) {
        if (point != null) {
            recordClick(point.getX(), point.getY(), type, label);
        }
    }

    /**
     * Enable or disable click visualization
     */
    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    /**
     * Check if click visualization is enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Clear all click visualizations
     */
    public static void clearClicks() {
        recentClicks.clear();
    }

    /**
     * Represents a single click visualization
     */
    private static class ClickVisualization {
        final int x;
        final int y;
        final ClickType type;
        final String label;
        final long timestamp;

        ClickVisualization(int x, int y, ClickType type, String label, long timestamp) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.label = label;
            this.timestamp = timestamp;
        }

        Color getColor() {
            return type.getColor();
        }
    }

    /**
     * Types of clicks for different colors
     */
    public enum ClickType {
        MOVEMENT(new Color(0, 255, 0)),      // Green for walking
        NPC(new Color(255, 255, 0)),         // Yellow for NPCs
        OBJECT(new Color(0, 255, 255)),      // Cyan for objects
        ITEM(new Color(255, 165, 0)),        // Orange for items
        GROUND_ITEM(new Color(255, 0, 255)), // Magenta for ground items
        WIDGET(new Color(138, 43, 226)),     // Purple for widgets
        GENERIC(new Color(255, 255, 255));   // White for generic

        private final Color color;

        ClickType(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }
}

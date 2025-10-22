package com.tonic.util;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Utility for visualizing where clicks are being sent in the client
 * This helps debug and verify that clicks are going to the correct locations
 */
public class ClickVisualization {

    private static final CopyOnWriteArrayList<ClickRecord> recentClicks = new CopyOnWriteArrayList<>();
    private static final long CLICK_DISPLAY_DURATION_MS = 600; // Show clicks for 600ms (matching RS animation)
    private static boolean enabled = true;

    private static final int ANIMATION_FRAMES = 4;
    private static final long FRAME_DURATION = 150;

    /**
     * Record a click at the specified location for visualization
     * @param x Screen x coordinate
     * @param y Screen y coordinate
     * @param type The type of click (determines color)
     * @param label Optional label to display
     */
    public static void recordClick(int x, int y, ClickType type, String label) {
        if (!enabled) {
            return;
        }
        recentClicks.add(new ClickRecord(x, y, type, label, System.currentTimeMillis()));
    }

    /**
     * Record a click at the specified Point
     * @param point The screen point
     * @param type The type of click
     * @param label Optional label to display
     */
    public static void recordClick(Point point, ClickType type, String label) {
        if (point != null) {
            recordClick(point.getX(), point.getY(), type, label);
        }
    }

    /**
     * Enable or disable click visualization
     * @param enable True to enable, false to disable
     */
    public static void setEnabled(boolean enable) {
        enabled = enable;
        if (!enable) {
            clearClicks();
        }
    }

    /**
     * Check if visualization is enabled
     * @return True if enabled
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
     * Render all recent clicks (called by overlay)
     * @param graphics The Graphics2D context
     * @param client The game client
     */
    public static void render(Graphics2D graphics, Client client) {
        if (!enabled || graphics == null || client == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        recentClicks.removeIf(click -> currentTime - click.timestamp > CLICK_DISPLAY_DURATION_MS);

        for (ClickRecord click : recentClicks) {
            renderClick(graphics, click, currentTime);
        }
    }

    /**
     * Render a single click visualization mimicking RuneScape's red/yellow X animation
     */
    private static void renderClick(Graphics2D graphics, ClickRecord click, long currentTime) {
        long age = currentTime - click.timestamp;

        int currentFrame = (int) (age / FRAME_DURATION);
        if (currentFrame >= ANIMATION_FRAMES) {
            return;
        }

        float frameProgress = (float) (age % FRAME_DURATION) / FRAME_DURATION;
        int alpha = (int) (255 * (1.0f - frameProgress * 0.5f)); // Fade out each frame

        Color xColor = (currentFrame % 2 == 0) ? new Color(255, 0, 0, alpha) : new Color(255, 255, 0, alpha);

        graphics.setColor(xColor);
        graphics.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int baseSize = 8;
        int size = baseSize + (currentFrame * 3);

        graphics.drawLine(click.x - size, click.y - size, click.x + size, click.y + size);
        graphics.drawLine(click.x + size, click.y - size, click.x - size, click.y + size);

        // Optional: Draw label if provided (only on first frame)
        if (currentFrame == 0 && click.label != null && !click.label.isEmpty()) {
            graphics.setFont(new Font("Arial", Font.BOLD, 11));
            FontMetrics fm = graphics.getFontMetrics();
            int textWidth = fm.stringWidth(click.label);
            int textX = click.x - textWidth / 2;
            int textY = click.y - size - 8;

            graphics.setColor(new Color(0, 0, 0, alpha));
            graphics.drawString(click.label, textX + 1, textY + 1);

            graphics.setColor(new Color(255, 255, 255, alpha));
            graphics.drawString(click.label, textX, textY);
        }
    }

    /**
     * Internal record of a click for visualization
     */
    private static class ClickRecord {
        final int x;
        final int y;
        final ClickType type;
        final String label;
        final long timestamp;

        ClickRecord(int x, int y, ClickType type, String label, long timestamp) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.label = label;
            this.timestamp = timestamp;
        }
    }

    /**
     * Types of clicks with associated colors
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

    /**
     * Overlay class to render click visualizations
     * Register this with your plugin's overlay manager
     */
    public static class ClickVisualizationOverlay extends Overlay {
        private final Client client;

        public ClickVisualizationOverlay(Client client) {
            this.client = client;
            setPosition(OverlayPosition.DYNAMIC);
            setLayer(OverlayLayer.ALWAYS_ON_TOP);
            setPriority(OverlayPriority.HIGH);
        }

        @Override
        public Dimension render(Graphics2D graphics) {
            ClickVisualization.render(graphics, client);
            return null;
        }
    }
}

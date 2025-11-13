package com.tonic.services.mouserecorder;

import lombok.Getter;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Utility for visualizing generated mouse movement paths in real-time on the game canvas.
 * This helps debug and verify that generated movements look natural and realistic.
 */
public class MovementVisualization
{
    private static final CopyOnWriteArrayList<MovementRecord> recentMovements = new CopyOnWriteArrayList<>();
    private static final long MOVEMENT_DISPLAY_DURATION_MS = 2500; // Show movements for 2.5 seconds

    @Getter
    private static boolean enabled = false;

    /**
     * Record a movement path for visualization
     * @param points List of MouseDataPoints representing the path
     * @param source The source/type of movement
     */
    public static void recordMovement(List<MouseDataPoint> points, MovementSource source)
    {
        if (!enabled || points == null || points.isEmpty())
        {
            return;
        }
        recentMovements.add(new MovementRecord(points, source, System.currentTimeMillis()));
    }

    /**
     * Enable or disable movement visualization
     * @param enable True to enable, false to disable
     */
    public static void setEnabled(boolean enable)
    {
        enabled = enable;
        if (!enable)
        {
            clearMovements();
        }
    }

    /**
     * Clear all movement visualizations
     */
    public static void clearMovements()
    {
        recentMovements.clear();
    }

    /**
     * Render all recent movements (called by overlay)
     * @param graphics The Graphics2D context
     */
    public static void render(Graphics2D graphics)
    {
        if (!enabled || graphics == null)
        {
            return;
        }

        long currentTime = System.currentTimeMillis();

        recentMovements.removeIf(movement -> currentTime - movement.timestamp > MOVEMENT_DISPLAY_DURATION_MS);

        for (MovementRecord movement : recentMovements)
        {
            renderMovement(graphics, movement, currentTime);
        }
    }

    /**
     * Render a single movement path as a connected line with fade animation
     */
    private static void renderMovement(Graphics2D graphics, MovementRecord movement, long currentTime)
    {
        long age = currentTime - movement.timestamp;
        if (age > MOVEMENT_DISPLAY_DURATION_MS)
        {
            return;
        }

        float fadeRatio = 1.0f - ((float) age / MOVEMENT_DISPLAY_DURATION_MS);
        int alpha = (int) (255 * fadeRatio);

        if (alpha <= 0)
        {
            return;
        }

        Color baseColor = movement.source.getColor();
        Color fadedColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);

        graphics.setColor(fadedColor);

        float strokeWidth = 2.0f + fadeRatio;
        graphics.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        List<MouseDataPoint> points = movement.points;

        for (int i = 0; i < points.size() - 1; i++)
        {
            MouseDataPoint p1 = points.get(i);
            MouseDataPoint p2 = points.get(i + 1);

            graphics.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }

        if (!points.isEmpty())
        {
            MouseDataPoint start = points.get(0);
            int markerSize = (int) (6 * fadeRatio);
            graphics.fillOval(start.getX() - markerSize / 2, start.getY() - markerSize / 2, markerSize, markerSize);

            MouseDataPoint end = points.get(points.size() - 1);
            int endMarkerSize = (int) (8 * fadeRatio);
            graphics.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha / 2));
            graphics.drawOval(end.getX() - endMarkerSize / 2, end.getY() - endMarkerSize / 2,
                    endMarkerSize, endMarkerSize);
        }

        if (fadeRatio > 0.3f && !points.isEmpty())
        {
            MouseDataPoint mid = points.get(points.size() / 2);
            String label = String.format("%s (%d pts)", movement.source.getLabel(), points.size());

            graphics.setFont(new Font("Arial", Font.PLAIN, 10));
            FontMetrics fm = graphics.getFontMetrics();
            int textWidth = fm.stringWidth(label);
            int textX = mid.getX() - textWidth / 2;
            int textY = mid.getY() - 8;

            graphics.setColor(new Color(0, 0, 0, alpha));
            graphics.drawString(label, textX + 1, textY + 1);

            graphics.setColor(Color.CYAN);
            graphics.drawString(label, textX, textY);
        }
    }

    /**
     * Internal record of a movement path for visualization
     */
    private static class MovementRecord
    {
        final List<MouseDataPoint> points;
        final MovementSource source;
        final long timestamp;

        MovementRecord(List<MouseDataPoint> points, MovementSource source, long timestamp)
        {
            this.points = points;
            this.source = source;
            this.timestamp = timestamp;
        }
    }

    /**
     * Source/type of mouse movement with associated colors
     */
    @Getter
    public enum MovementSource
    {
        TRAJECTORY_GENERATED(new Color(0, 255, 100), "Generated"),   // Bright green for generated
        IDLE_JITTER(new Color(100, 150, 255), "Jitter"),             // Light blue for jitter
        MANUAL(new Color(255, 255, 255), "Manual"),                  // White for manual play
        LINEAR(new Color(255, 200, 0), "Linear"),                    // Yellow for linear fallback
        UNKNOWN(new Color(150, 150, 150), "Unknown");                // Gray for unknown

        private final Color color;
        private final String label;

        MovementSource(Color color, String label)
        {
            this.color = color;
            this.label = label;
        }
    }
}

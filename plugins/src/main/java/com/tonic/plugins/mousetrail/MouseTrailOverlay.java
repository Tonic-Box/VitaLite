package com.tonic.plugins.mousetrail;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Overlay that renders the mouse trail visualization
 */
public class MouseTrailOverlay extends Overlay {

    private final MouseTrailPlugin plugin;
    private final MouseTrailConfig config;

    @Inject
    public MouseTrailOverlay(MouseTrailPlugin plugin, MouseTrailConfig config) {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGHEST);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        Queue<MouseTrailPlugin.TrailPoint> trail = plugin.getTrail();

        if (trail.isEmpty()) {
            return null;
        }

        // Convert to list for indexed access
        List<MouseTrailPlugin.TrailPoint> points = new ArrayList<>(trail);

        if (points.size() < 2) {
            return null;
        }

        // Enable anti-aliasing if configured
        if (config.smoothTrail()) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }

        // Get current time for fade calculations
        long currentTime = System.currentTimeMillis();
        long fadeTimeMs = config.fadeTimeMs();

        // Draw trail segments
        for (int i = 0; i < points.size() - 1; i++) {
            MouseTrailPlugin.TrailPoint p1 = points.get(i);
            MouseTrailPlugin.TrailPoint p2 = points.get(i + 1);

            // Calculate position in trail (0.0 = oldest, 1.0 = newest)
            float trailPosition = (float) i / (points.size() - 1);

            // Calculate age-based fade
            long age = currentTime - p1.timestamp;
            float ageFade = 1.0f - Math.min(1.0f, (float) age / fadeTimeMs);

            // Calculate final alpha
            int baseAlpha = config.fadeAlpha();
            int alpha = (int) (baseAlpha + (255 - baseAlpha) * trailPosition * ageFade);
            alpha = Math.max(0, Math.min(255, alpha));

            // Calculate color with gradient if enabled
            Color color;
            if (config.useGradient()) {
                color = interpolateColor(config.trailColor(), config.headColor(),
                                        trailPosition, alpha);
            } else {
                Color base = config.trailColor();
                color = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
            }

            // Calculate line width with slight taper
            float widthMultiplier = 0.5f + (0.5f * trailPosition);
            float lineWidth = config.trailWidth() * widthMultiplier;

            graphics.setColor(color);
            graphics.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.drawLine(p1.point.x, p1.point.y, p2.point.x, p2.point.y);
        }

        return null;
    }

    /**
     * Interpolates between two colors with alpha
     */
    private Color interpolateColor(Color c1, Color c2, float ratio, int alpha) {
        int red = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * ratio);
        int green = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio);
        int blue = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio);

        return new Color(red, green, blue, alpha);
    }
}

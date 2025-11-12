package com.tonic.plugins.mousetrail;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Mouse Trail Visualization Plugin
 *
 * Creates a visual "comet trail" effect that follows the mouse cursor.
 * Safe, read-only visualization for analyzing mouse movement patterns.
 */
@PluginDescriptor(
    name = "Mouse Trail",
    description = "Visual comet trail for mouse cursor movement analysis",
    tags = {"mouse", "visualization", "trail", "overlay"}
)
public class MouseTrailPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private MouseManager mouseManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MouseTrailOverlay overlay;

    @Inject
    private MouseTrailConfig config;

    private final Queue<TrailPoint> trail = new LinkedList<>();
    private MouseListener mouseListener;

    /**
     * Represents a point in the mouse trail with timing information
     */
    static class TrailPoint {
        final Point point;
        final long timestamp;

        TrailPoint(Point point, long timestamp) {
            this.point = point;
            this.timestamp = timestamp;
        }
    }

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);

        // Register mouse listener to track movement
        mouseListener = new MouseListener() {
            @Override
            public MouseEvent mouseClicked(MouseEvent e) { return e; }

            @Override
            public MouseEvent mousePressed(MouseEvent e) { return e; }

            @Override
            public MouseEvent mouseReleased(MouseEvent e) { return e; }

            @Override
            public MouseEvent mouseEntered(MouseEvent e) { return e; }

            @Override
            public MouseEvent mouseExited(MouseEvent e) { return e; }

            @Override
            public MouseEvent mouseDragged(MouseEvent e) {
                handleMouseMove(e.getPoint());
                return e;
            }

            @Override
            public MouseEvent mouseMoved(MouseEvent e) {
                handleMouseMove(e.getPoint());
                return e;
            }
        };

        mouseManager.registerMouseListener(mouseListener);
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlay);
        mouseManager.unregisterMouseListener(mouseListener);
        trail.clear();
    }

    /**
     * Handles mouse movement events
     */
    private void handleMouseMove(Point point) {
        long currentTime = System.currentTimeMillis();

        // Add point to trail
        trail.add(new TrailPoint(new Point(point), currentTime));

        // Trim trail to configured length
        int maxLength = config.trailLength();
        while (trail.size() > maxLength) {
            trail.poll();
        }

        // Remove old points based on fade time
        long fadeTimeMs = config.fadeTimeMs();
        while (!trail.isEmpty() && (currentTime - trail.peek().timestamp) > fadeTimeMs) {
            trail.poll();
        }
    }

    /**
     * Gets the current trail for rendering
     */
    public Queue<TrailPoint> getTrail() {
        return trail;
    }

    /**
     * Clears the current trail
     */
    public void clearTrail() {
        trail.clear();
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        // Cleanup old points every tick to prevent memory leaks
        long currentTime = System.currentTimeMillis();
        long fadeTimeMs = config.fadeTimeMs();

        while (!trail.isEmpty() && (currentTime - trail.peek().timestamp) > fadeTimeMs * 2) {
            trail.poll();
        }
    }

    @Provides
    MouseTrailConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MouseTrailConfig.class);
    }
}

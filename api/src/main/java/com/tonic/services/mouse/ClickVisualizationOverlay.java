package com.tonic.services.mouse;

import com.tonic.Static;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.ClickPacket;
import com.tonic.services.ClickPacket.ClickType;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Overlay to visualize where clicks are being sent in the client
 */
@Singleton
public class ClickVisualizationOverlay extends Overlay {

    private static final CopyOnWriteArrayList<ClickVisualization> recentClicks = new CopyOnWriteArrayList<>();
    private static final long CLICK_DISPLAY_DURATION_MS = 2000; // Show clicks for 2 seconds

    @Inject
    private Client client;

    public ClickVisualizationOverlay() {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.HIGH);
        EventBus eventBus = Static.getInjector().getInstance(EventBus.class);
        eventBus.register(this);
    }

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        List<ClickPacket> packets = ClickManager.releaseClicks();
        for (ClickPacket packet : packets) {
            recordClick(packet.getX(), packet.getY(), packet.getPacketInteractionType(), packet.getDate().format(DateTimeFormatter.ISO_LOCAL_TIME));
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!Static.getVitaConfig().shouldVisualizeClicks() || client == null) {
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
        if (!Static.getVitaConfig().shouldVisualizeClicks()) {
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
     * Record a click visualization for walking to a destination
     * @param destination The destination WorldPoint
     */
    public static void recordWalkClick(WorldPoint destination) {
        if (destination == null) {
            return;
        }

        Client client = Static.getClient();

        try {
            LocalPoint localPoint = LocalPoint.fromWorld(client, destination);
            if (localPoint != null) {
                Point screenPoint = Perspective.localToCanvas(client, localPoint, client.getPlane());

                boolean isValidScreenPoint = false;
                if (screenPoint != null) {
                    java.awt.Rectangle viewport = client.getCanvas().getBounds();
                    isValidScreenPoint = screenPoint.getX() >= 0 &&
                            screenPoint.getX() <= viewport.width &&
                            screenPoint.getY() >= 0 &&
                            screenPoint.getY() <= viewport.height;
                }

                if (screenPoint != null && isValidScreenPoint) {
                    Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
                    if (tilePoly != null) {
                        ClickManager.queueClickBox(tilePoly);
                    }
                    else {
                        //generate 25 x 25 poly around point, setting point messes up static click
                        Polygon clickBox = new Polygon();
                        int boxWidth = 10;
                        int boxHeight = 10;
                        clickBox.addPoint(screenPoint.getX() - boxWidth / 2, screenPoint.getY() - boxHeight / 2);
                        clickBox.addPoint(screenPoint.getX() + boxWidth / 2, screenPoint.getY() - boxHeight / 2);
                        clickBox.addPoint(screenPoint.getX() + boxWidth / 2, screenPoint.getY() + boxHeight / 2);
                        clickBox.addPoint(screenPoint.getX() - boxWidth / 2, screenPoint.getY() + boxHeight / 2);
                        ClickManager.queueClickBox(clickBox);
                    }
                } else {
                    Point minimapPoint = Static.invoke(() -> Perspective.localToMinimap(client, localPoint));
                    if (minimapPoint != null) {
                        int radius = 3;
                        int[] xPoints = new int[8];
                        int[] yPoints = new int[8];
                        for (int i = 0; i < 8; i++) {
                            double angle = 2 * Math.PI * i / 8;
                            xPoints[i] = minimapPoint.getX() + (int)(radius * Math.cos(angle));
                            yPoints[i] = minimapPoint.getY() + (int)(radius * Math.sin(angle));
                        }
                        Polygon minimapClickBox = new Polygon(xPoints, yPoints, 8);

                        ClickManager.queueClickBox(minimapClickBox);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}

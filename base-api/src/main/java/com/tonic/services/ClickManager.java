package com.tonic.services;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.services.ClickPacket.ClickPacket;
import com.tonic.services.ClickPacket.PacketInteractionType;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages mouse click strategies and click packet generation.
 */
public class ClickManager
{
    @Setter
    @Getter
    private static volatile ClickStrategy strategy = ClickStrategy.STATIC;
    @Getter
    private static final AtomicReference<Point> point = new AtomicReference<>(new Point(-1, -1));
    private static volatile Shape shape = null;
    private static final List<ClickPacket> clickPackets = new ArrayList<>();

    /**
     * Sets the target point for static clicking.
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public static void setPoint(int x, int y)
    {
        point.set(new Point(x, y));
    }

    /**
     * Queues a shape for controlled clicking.
     * @param shape the shape to click within
     */
    public static void queueClickBox(Shape shape)
    {
        if(shape == null)
        {
            ClickManager.shape = null;
            return;
        }
        ClickManager.shape = shape;
    }

    /**
     * Clears the currently set click box.
     */
    public static void clearClickBox()
    {
        shape = null;
    }

    /**
     * Sends a click packet using the current strategy.
     */
    public static void click()
    {
        click(PacketInteractionType.UNBOUND_INTERACT);
    }

    /**
     * Sends a click packet using the current strategy and specified interaction type.
     * @param packetInteractionType the type of interaction for the click packet
     */
    public static void click(PacketInteractionType packetInteractionType)
    {
        Static.invoke(() -> {
            TClient client = Static.getClient();
            int px = point.get().x;
            int py = point.get().y;
            switch (strategy)
            {
                case STATIC:
                    defaultStaticClickPacket(packetInteractionType, client, px, py);
                    break;
                case RANDOM:
                    Rectangle r = Static.getRuneLite().getGameApplet().getViewportArea();
                    if(r == null)
                    {
                        Logger.warn("Viewport area is null, defaulting to STATIC.");
                        defaultStaticClickPacket(packetInteractionType, client, px, py);
                        break;
                    }
                    int rx = (int) (Math.random() * r.getWidth()) + r.x;
                    int ry = (int) (Math.random() * r.getHeight()) + r.y;
                    client.getPacketWriter().clickPacket(0, rx, ry);
                    clickPackets.add(new ClickPacket(packetInteractionType, rx, ry));
                    break;
                case CONTROLLED:
                    if(shape == null)
                    {
                        Logger.warn("Click box is null, defaulting to STATIC.");
                        defaultStaticClickPacket(packetInteractionType, client, px, py);
                        break;
                    }
                    Point p = getRandomPointInShape(shape);
                    client.getPacketWriter().clickPacket(0, p.x, p.y);
                    clickPackets.add(new ClickPacket(packetInteractionType, p.x, p.y));
                    break;
            }
        });
    }

    private static Point getRandomPointInShape(Shape shape) {
        Rectangle bounds = shape.getBounds(); // getBounds() returns Rectangle with ints

        while (true) {
            int x = (int) (Math.random() * bounds.width) + bounds.x;
            int y = (int) (Math.random() * bounds.height) + bounds.y;

            if (shape.contains(x, y)) {
                return new Point(x, y);
            }
        }
    }

    private static void defaultStaticClickPacket(PacketInteractionType packetInteractionType, TClient client, int x, int y) {
        client.getPacketWriter().clickPacket(0, x, y);
        clickPackets.add(new ClickPacket(packetInteractionType, x, y));
    }
}

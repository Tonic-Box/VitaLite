package com.tonic.services;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TClient;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;

public class ClickManager
{
    @Setter
    @Getter
    private static ClickStrategy strategy = ClickStrategy.STATIC;
    @Getter
    private static final Point point = new Point(-1, -1);
    private static Shape shape = null;

    public static void setPoint(int x, int y)
    {
        point.setLocation(x, y);
    }

    public static void queueClickBox(Shape shape)
    {
        if(shape == null)
        {
            ClickManager.shape = null;
            return;
        }
        ClickManager.shape = shape;
    }

    public static void clearClickBox()
    {
        shape = null;
    }

    public static void click()
    {
        Static.invoke(() -> {
            TClient client = Static.getClient();
            switch (strategy)
            {
                case STATIC:
                    client.getPacketWriter().clickPacket(0, point.x, point.y);
                    break;
                case RANDOM:
                    Rectangle r = Static.getRuneLite().getGameApplet().getViewportArea();
                    if(r == null)
                    {
                        Logger.warn("Viewport area is null, defaulting to STATIC.");
                        client.getPacketWriter().clickPacket(0, point.x, point.y);
                        break;
                    }
                    int rx = (int) (Math.random() * r.getWidth()) + r.x;
                    int ry = (int) (Math.random() * r.getHeight()) + r.y;
                    client.getPacketWriter().clickPacket(0, rx, ry);
                    break;
                case CONTROLLED:
                    if(shape == null)
                    {
                        Logger.warn("Click box is null, defaulting to STATIC.");
                        client.getPacketWriter().clickPacket(0, point.x, point.y);
                        break;
                    }
                    Point p = getRandomPointInShape(shape);
                    client.getPacketWriter().clickPacket(0, p.x, p.y);
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
}

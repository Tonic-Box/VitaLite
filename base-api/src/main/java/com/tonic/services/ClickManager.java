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
    private static Rectangle rect = null;

    public static void setPoint(int x, int y)
    {
        point.setLocation(x, y);
    }

    public static void queueClickBox(Rectangle rectangle)
    {
        rect = rectangle;
    }

    public static void clearClickBox()
    {
        rect = null;
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
                    int rx = (int) (Math.random() * r.getWidth()) + r.x;
                    int ry = (int) (Math.random() * r.getHeight()) + r.y;
                    client.getPacketWriter().clickPacket(0, rx, ry);
                    break;
                case CONTROLLED:
                    if(rect == null)
                    {
                        Logger.warn("Click box is null, defaulting to STATIC.");
                        client.getPacketWriter().clickPacket(0, point.x, point.y);
                        break;
                    }
                    int cx = (int) (Math.random() * rect.getWidth()) + rect.x;
                    int cy = (int) (Math.random() * rect.getHeight()) + rect.y;
                    client.getPacketWriter().clickPacket(0, cx, cy);
                    break;
            }
        });
    }
}

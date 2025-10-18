package com.tonic.util;

import com.tonic.Static;
import com.tonic.data.ItemEx;
import com.tonic.data.TileObjectEx;
import com.tonic.services.ClickManager;
import net.runelite.api.Actor;
import java.awt.*;

public class ClickManagerUtil
{
    public static void queueClickBox(TileObjectEx object)
    {
        Static.invoke(() -> {
            Shape shape = object.getTileObject().getClickbox();
            if(shape == null)
            {
                shape = Static.getRuneLite().getGameApplet().getWorldViewportArea();
            }
            ClickManager.queueClickBox(shape);
            return true;
        });
    }

    public static void queueClickBox(Actor actor)
    {
        Static.invoke(() -> {
            Shape shape = actor.getConvexHull();
            if(shape == null)
            {
                shape = Static.getRuneLite().getGameApplet().getWorldViewportArea();
            }
            ClickManager.queueClickBox(shape);
            return true;
        });

    }

    public static void queueClickBox(ItemEx item)
    {
        Static.invoke(() -> {
            Shape shape = item.getClickBox();
            if(shape == null)
            {
                shape = Static.getRuneLite().getGameApplet().getSideMenuArea();
            }
            ClickManager.queueClickBox(shape);
            return true;
        });
    }
}

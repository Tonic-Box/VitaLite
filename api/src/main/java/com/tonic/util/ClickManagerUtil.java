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
        Shape shape = object.getTileObject().getClickbox();
        if(shape == null)
        {
            shape = Static.getRuneLite().getGameApplet().getWorldViewportArea();
        }
        ClickManager.queueClickBox(shape);
    }

    public static void queueClickBox(Actor actor)
    {
        Shape shape = actor.getConvexHull();
        if(shape == null)
        {
            shape = Static.getRuneLite().getGameApplet().getWorldViewportArea();
        }
        ClickManager.queueClickBox(shape);
    }

    public static void queueClickBox(ItemEx item)
    {
        Shape shape = item.getClickBox();
        if(shape == null)
        {
            shape = Static.getRuneLite().getGameApplet().getSideMenuArea();
        }
        ClickManager.queueClickBox(shape);
    }
}

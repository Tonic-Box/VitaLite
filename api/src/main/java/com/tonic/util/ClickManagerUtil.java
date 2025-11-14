package com.tonic.util;

import com.tonic.Static;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.ActorEx;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.services.ClickManager;
import net.runelite.api.widgets.Widget;
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

    public static void queueClickBox(TileItemEx item)
    {
        Static.invoke(() -> {
            Shape shape = item.getShape();
            if(shape == null)
            {
                shape = Static.getRuneLite().getGameApplet().getWorldViewportArea();
            }
            ClickManager.queueClickBox(shape);
            return true;
        });
    }

    public static void queueClickBox(ActorEx<?> actor)
    {
        Static.invoke(() -> {
            Shape shape = actor.getShape();
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

    public static void queueClickBox(Widget widget)
    {
        Static.invoke(() -> {
            Shape shape = widget.getBounds();
            if(shape == null)
            {
                shape = Static.getRuneLite().getGameApplet().getSideMenuArea();
            }
            ClickManager.queueClickBox(shape);
            return true;
        });
    }

    public static void queueClickBoxInterface(int interfaceId)
    {
        Static.invoke(() -> {
            Widget widget = WidgetAPI.get(interfaceId);
            Shape shape = widget.getBounds();
            if(shape == null)
            {
                shape = Static.getRuneLite().getGameApplet().getSideMenuArea();
            }
            ClickManager.queueClickBox(shape);
            return true;
        });
    }
}

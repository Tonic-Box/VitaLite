package com.tonic.services.streamer;

import com.google.common.collect.MapMaker;
import com.tonic.Static;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxOverlay;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * InfoBoxes as the mirror shows them: an InfoBox panel holding any that are not stock is drawn a second time for the mirror with only the stock ones, so they close up as they would in RuneLite. Runs on the client thread only.
 */
public final class MirrorInfoBoxes
{
    private static final Map<Overlay, Dimension> mirrorSizes = new MapMaker().weakKeys().makeMap();

    private static Method pluginGetter;
    private static Field hoveredField;
    private static boolean broken;
    private static boolean filtering;

    private MirrorInfoBoxes()
    {
    }

    /**
     * Narrows an InfoBox panel's boxes to the stock ones while it is drawn for the mirror. Called by the InfoBox panel each time it reads its boxes while streamer mode is on.
     *
     * @param boxes the panel's boxes
     * @return the boxes to draw
     */
    public static List<InfoBox> boxes(List<InfoBox> boxes)
    {
        if (!filtering)
        {
            return boxes;
        }
        List<InfoBox> stock = new ArrayList<>(boxes.size());
        for (InfoBox box : boxes)
        {
            if (isStock(box))
            {
                stock.add(box);
            }
        }
        return stock;
    }

    /**
     * @param overlay an overlay
     * @return whether it is an InfoBox panel holding a box that is not stock
     */
    static boolean hidesAny(Overlay overlay)
    {
        if (!(overlay instanceof InfoBoxOverlay) || !resolve())
        {
            return false;
        }
        for (InfoBox box : ((InfoBoxOverlay) overlay).getInfoBoxes())
        {
            if (!isStock(box))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @param entry a right-click entry of an InfoBox panel
     * @return whether it belongs to an InfoBox that is not stock
     */
    static boolean ownsHidden(OverlayMenuEntry entry)
    {
        if (!resolve())
        {
            return false;
        }
        for (InfoBox box : Static.getInjector().getInstance(InfoBoxManager.class).getInfoBoxes())
        {
            for (OverlayMenuEntry boxEntry : box.getMenuEntries())
            {
                if (boxEntry == entry)
                {
                    return !isStock(box);
                }
            }
        }
        return false;
    }

    /**
     * @param overlay an InfoBox panel
     * @return the size the mirror showed it at last
     */
    static Dimension mirrorSize(Overlay overlay)
    {
        Dimension size = mirrorSizes.get(overlay);
        return size != null ? size : overlay.getBounds().getSize();
    }

    /**
     * Draws an InfoBox panel for the mirror with only its stock boxes, leaving the panel's own state as RuneLite's draw left it.
     *
     * @param overlay the panel
     * @param graphics graphics drawing for the mirror, at the panel's mirror location
     * @param at where the mirror shows the panel
     * @return the size drawn
     */
    static Dimension render(Overlay overlay, Graphics2D graphics, Point at)
    {
        Rectangle bounds = overlay.getBounds();
        Rectangle real = new Rectangle(bounds);
        Object hovered;
        try
        {
            hovered = hoveredField.get(overlay);
        }
        catch (ReflectiveOperationException e)
        {
            broken = true;
            mirrorSizes.put(overlay, new Dimension());
            return new Dimension();
        }
        Dimension drawn = null;
        try
        {
            bounds.setLocation(at);
            filtering = true;
            drawn = overlay.render(graphics);
        }
        catch (RuntimeException ignored)
        {
        }
        finally
        {
            filtering = false;
            bounds.setBounds(real);
            try
            {
                hoveredField.set(overlay, hovered);
            }
            catch (ReflectiveOperationException e)
            {
                broken = true;
            }
        }
        Dimension size = drawn == null ? new Dimension() : drawn;
        mirrorSizes.put(overlay, size);
        return size;
    }

    private static boolean isStock(InfoBox box)
    {
        if (!StockContent.isStock(box.getClass()))
        {
            return false;
        }
        try
        {
            Plugin plugin = (Plugin) pluginGetter.invoke(box);
            return plugin == null || StockContent.isStock(plugin);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            broken = true;
            return true;
        }
    }

    private static boolean resolve()
    {
        if (broken)
        {
            return false;
        }
        if (pluginGetter != null)
        {
            return true;
        }
        try
        {
            hoveredField = Members.field(InfoBoxOverlay.class, "hoveredComponent");
            pluginGetter = Members.method(InfoBox.class, "getPlugin");
            return true;
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            broken = true;
            return false;
        }
    }
}

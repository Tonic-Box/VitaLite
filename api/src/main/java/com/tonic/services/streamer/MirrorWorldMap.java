package com.tonic.services.streamer;

import com.google.common.collect.MapMaker;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.util.Collections;
import java.util.Set;

/**
 * World map points as the mirror shows them: a point is marked when it is added by content that is not stock, and its icon and tooltip are then drawn for the real view only. Runs on the client thread, apart from marking.
 */
public final class MirrorWorldMap
{
    private static final String WORLD_MAP_PACKAGE = "net.runelite.client.ui.overlay.worldmap.";

    private static final Set<WorldMapPoint> hidden = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    private static WorldMapPoint drawing;

    private MirrorWorldMap()
    {
    }

    /**
     * Marks a point being added when content that is not stock adds it. Called by the world map point manager.
     *
     * @param point the point
     */
    public static void added(WorldMapPoint point)
    {
        if (point != null && !(StockContent.isStock(point.getClass()) && StockContent.callerIsStock(WORLD_MAP_PACKAGE)))
        {
            hidden.add(point);
        }
    }

    /**
     * Notes the point the world map overlay is about to draw. Called by the overlay while streamer mode is on.
     *
     * @param point the point
     */
    public static void point(WorldMapPoint point)
    {
        drawing = point;
    }

    /**
     * Draws a point's icon, for the real view only when the point is hidden. Called by the world map overlay in place of its own draw while streamer mode is on.
     *
     * @param graphics the overlay's graphics
     * @param image the icon
     * @param x where to draw it
     * @param y where to draw it
     * @param observer the image observer
     * @return whether the image was drawn completely
     */
    public static boolean drawImage(Graphics2D graphics, Image image, int x, int y, ImageObserver observer)
    {
        if (drawing == null || !hidden.contains(drawing))
        {
            return graphics.drawImage(image, x, y, observer);
        }
        try
        {
            return MirrorHiding.hide(graphics).drawImage(image, x, y, observer);
        }
        finally
        {
            MirrorHiding.end();
        }
    }

    /**
     * Starts drawing a point's tooltip, for the real view only when the point is hidden. Called by the world map overlay while streamer mode is on.
     *
     * @param graphics the overlay's graphics
     * @param point the point whose tooltip is drawn
     * @return the graphics to draw the tooltip with
     */
    public static Graphics2D tooltip(Graphics2D graphics, WorldMapPoint point)
    {
        return hidden.contains(point) ? MirrorHiding.hide(graphics) : graphics;
    }

    /**
     * Forgets the point drawn last, as when streamer mode starts or stops. Called on the client thread.
     */
    static void reset()
    {
        drawing = null;
    }

    /**
     * @param point a world map point
     * @return whether content that is not stock added it
     */
    static boolean isHidden(WorldMapPoint point)
    {
        return hidden.contains(point);
    }
}

package com.tonic.api.widgets;

import com.tonic.Static;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayUtil;

import java.awt.*;
import java.util.List;

/**
 * MiniMap API
 */
public class MiniMapAPI {
    /**
     * Draws a path on the minimap.
     *
     * @param graphics The graphics context to draw on.
     * @param path     The list of WorldPoints representing the path.
     * @param color    The color to use for drawing the path.
     */
    public static void drawPath(Graphics2D graphics, List<WorldPoint> path, Color color)
    {
        if(graphics == null || path == null || path.isEmpty())
        {
            return;
        }

        for (WorldPoint point : path)
        {
            RenderNode(graphics, point, color);
        }
    }

    /**
     * Renders a single node on the minimap.
     *
     * @param graphics The graphics context to draw on.
     * @param point    The WorldPoint to render.
     * @param color    The color to use for rendering the node.
     */
    public static void RenderNode(Graphics2D graphics, WorldPoint point, Color color)
    {
        final Client client = Static.getClient();
        WorldPoint playerLocation = WorldPointUtil.getTopWorldViewLocation();
        if (point.distanceTo(playerLocation) >= 16)
        {
            return;
        }

        LocalPoint lp = LocalPoint.fromWorld(client, point);
        if (lp == null)
        {
            return;
        }

        net.runelite.api.Point posOnMinimap = Perspective.localToMinimap(client, lp);
        if (posOnMinimap == null)
        {
            return;
        }

        OverlayUtil.renderMinimapLocation(graphics, posOnMinimap, color);
    }
}

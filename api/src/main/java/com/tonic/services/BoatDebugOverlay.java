package com.tonic.services;

import com.tonic.Static;
import com.tonic.api.game.sailing.SailingAPI;
import com.tonic.api.game.sailing.SailingRenderUtil;
import com.tonic.util.ClientConfig;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * Sailing debug drawing while on a boat: hull, deck, sea cells, sea graph, route and predicted track, each behind its own setting.
 */
final class BoatDebugOverlay extends Overlay
{
    private static final int SEA_RADIUS = 16;
    private static final int TRACK_TICKS = 8;

    BoatDebugOverlay()
    {
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!SailingAPI.isOnBoat())
        {
            return null;
        }

        ClientConfig config = Static.getVitaConfig();
        if (config.getDrawBoatHull())
        {
            SailingRenderUtil.renderPlayerBoatHull(graphics);
        }
        if (config.getDrawBoatDeck())
        {
            SailingRenderUtil.renderPlayerBoatDeck(graphics);
        }
        if (config.getDrawSeaCells())
        {
            SailingRenderUtil.renderSeaCells(graphics, SEA_RADIUS);
        }
        if (config.getDrawSeaGraph())
        {
            SailingRenderUtil.renderSeaGraph(graphics);
        }
        if (config.getDrawBoatRoute())
        {
            SailingRenderUtil.renderRoute(graphics);
        }
        if (config.getDrawBoatTrack())
        {
            SailingRenderUtil.renderPredictedTrack(graphics, TRACK_TICKS);
        }
        return null;
    }
}

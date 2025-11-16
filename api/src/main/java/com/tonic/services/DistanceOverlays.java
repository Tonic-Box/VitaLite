package com.tonic.services;

import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.model.ui.DistanceDebugger;
import static com.tonic.model.ui.DistanceDebugger.DistanceMode;

import com.tonic.util.Distance;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayUtil;

import java.awt.*;

public class DistanceOverlays extends Overlay
{
    private DistanceMode mode;

    @Override
    public Dimension render(Graphics2D graphics) {
        if(!DistanceDebugger.isOn())
        {
            return null;
        }

        mode = DistanceDebugger.getDistanceMode();
        if(mode == null)
        {
            return null;
        }

        if(DistanceDebugger.getPlayersEnabled())
        {
            renderPlayers(graphics);
        }
        if(DistanceDebugger.getNpcsEnabled())
        {
            renderNpcs(graphics);
        }
        if(DistanceDebugger.getTileObjectsEnabled())
        {
            renderTileObjects(graphics);
        }
        if(DistanceDebugger.getTileItemsEnabled())
        {
            renderTileItems(graphics);
        }
        return null;
    }

    public void renderTileItems(Graphics2D graphics)
    {
        for(TileItemEx item : GameManager.tileItemList())
        {
            int distance = measureDistance(item.getInteractionPoint());
            String text = item.getName() + "(" + distance + ")";
            OverlayUtil.renderTileOverlay(graphics, item.getTile().getItemLayer(), text, Color.RED);
        }
    }

    private void renderTileObjects(Graphics2D graphics)
    {
        WorldPoint player = PlayerEx.getLocal().getWorldPoint();
        for(TileObjectEx tileObjectEx : GameManager.objectList())
        {
            if(Distance.chebyshev(player, tileObjectEx.getWorldPoint()) > 15)
                continue;

            if(!(tileObjectEx.getTileObject() instanceof GameObject))
                continue;

            if(!tileObjectEx.isInteractable())
                continue;

            int distance = measureDistance(tileObjectEx.getInteractionPoint());

            String text = tileObjectEx.getName() + "(" + distance + ")";
            OverlayUtil.renderTileOverlay(graphics, tileObjectEx.getTileObject(), text, Color.GREEN);
        }
    }

    private void renderPlayers(Graphics2D graphics)
    {
        for(PlayerEx player : GameManager.playerList())
        {
            if(player == PlayerEx.getLocal())
                continue;

            int distance = measureDistance(player.getInteractionPoint());
            String text = player.getName() + "(" + distance + ")";
            OverlayUtil.renderActorOverlay(graphics, player.getActor(), text, Color.CYAN);
        }
    }

    private void renderNpcs(Graphics2D graphics)
    {
        for(NpcEx npc : GameManager.npcList())
        {
            int distance = measureDistance(npc.getInteractionPoint());
            String text = npc.getName() + "(" + distance + ")";
            OverlayUtil.renderActorOverlay(graphics, npc.getActor(), text, Color.CYAN);
        }
    }

    private int measureDistance(WorldPoint to)
    {
        WorldPoint from = PlayerEx.getLocal().getWorldPoint();
        if(from == null || to == null)
        {
            return -1;
        }

        switch(mode)
        {
            case PATH_TO:
                return Distance.pathDistanceTo(from, to);
            case DIAGONAL:
                return Distance.diagonal(from, to);
            case CHEBYSHEV:
                return Distance.chebyshev(from, to);
            case MANHATTAN:
                return Distance.manhattan(from, to);
            case EUCLIDEAN:
                return Distance.euclidean(from, to);
            case EUCLIDEAN_SQUARED:
                return Distance.euclideanSquared(from, to);
        }

        return -1;
    }
}

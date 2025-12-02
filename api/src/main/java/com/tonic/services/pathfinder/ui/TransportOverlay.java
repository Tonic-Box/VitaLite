package com.tonic.services.pathfinder.ui;

import com.google.common.base.Strings;
import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.ClientScriptAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.services.pathfinder.model.TransportDto;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportLoader;
import com.tonic.util.WorldPointUtil;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransportOverlay extends Overlay
{
    private static final int MAX_DRAW_DISTANCE = 32;
    private static final Font FONT = FontManager.getRunescapeFont().deriveFont(Font.BOLD, 16);
    private final Map<WorldPoint,Integer> points = new HashMap<>();
    private static final Map<Long, List<TransportDto>> TRANSPORT_INDEX = new HashMap<>();
    @Setter
    private boolean active = false;

    public TransportOverlay()
    {
        ClientScriptAPI.closeNumericInputDialogue();
        Static.getRuneLite().getEventBus().register(this);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded entry) {
        if(!active)
            return;
        String color = "<col=00ff00>";
        int opcode = entry.getType();
        if(opcode == MenuAction.WALK.getId())
        {
            Client client = Static.getClient();
            WorldView worldView = client.getTopLevelWorldView();
            if(worldView.getSelectedSceneTile() == null)
                return;

            WorldPoint worldPoint = WorldPointUtil.get(worldView.getSelectedSceneTile().getWorldLocation());
            List<Transport> tr = TransportLoader.getTransports().get(WorldPointUtil.compress(worldPoint));
            if(tr != null && !tr.isEmpty())
            {
                for(Transport t : tr)
                {
                    if(t.getId() == -1)
                    {
                        client.createMenuEntry(1)
                                .setOption("Hardcoded Transport [-> " + WorldPointUtil.fromCompressed(t.getDestination()) + "]")
                                .setTarget(color + "Transport ")
                                .setType(MenuAction.RUNELITE);
                    }
                }
            }

            TransportDto transport;
            for (int i = 0; i < TransportEditorFrame.getTransports().size(); i++) {
                transport = TransportEditorFrame.getTransports().get(i);
                if(!transport.getSource().equals(worldPoint))
                    continue;

                TransportDto finalTransport = transport;
                int finalI = i;
                client.createMenuEntry(1)
                        .setOption("Edit Transport [-> " + transport.getDestination() + "]")
                        .setTarget(color + "Transport ")
                        .setType(MenuAction.RUNELITE)
                        .onClick(c -> TransportEditorFrame.INSTANCE.selectTransportByObjectAndSource(finalTransport.getObjectId(), finalI));
            }
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        graphics.setFont(FONT);
        getPoints();
        Stroke stroke = new BasicStroke((float) 1);
        for(var entry : points.entrySet())
        {
            drawTile(graphics, entry.getKey(), entry.getValue() + " transport(s)", stroke);
        }
        return null;
    }

    private void drawTile(Graphics2D graphics, WorldPoint point, @Nullable String label, Stroke borderStroke)
    {
        Client client = Static.getClient();
        WorldView wv = client.getTopLevelWorldView();
        WorldPoint playerLocation = PlayerEx.getLocal().getWorldPoint();

        if (point.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE)
        {
            return;
        }

        LocalPoint lp = LocalPoint.fromWorld(wv, point);
        if (lp == null)
        {
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly != null)
        {
            OverlayUtil.renderPolygon(graphics, poly, Color.CYAN, new Color(0, 0, 0, .01f), borderStroke);
        }

        if (!Strings.isNullOrEmpty(label))
        {
            Point canvasTextLocation = Perspective.getCanvasTextLocation(client, graphics, lp, label, 0);
            if (canvasTextLocation != null)
            {
                OverlayUtil.renderTextLocation(graphics, canvasTextLocation, label, Color.WHITE);
            }
        }
    }

    public void getPoints()
    {
        Client client = Static.getClient();
        points.clear();
        WorldView wv = client.getTopLevelWorldView();
        Scene scene = wv.getScene();
        if (scene == null)
        {
            return;
        }
        Tile[][][] tiles = scene.getTiles();

        int z = wv.getPlane();
        Tile tile;
        WorldPoint point;

        WorldPoint player = PlayerEx.getLocal().getWorldPoint();
        int px = player.getX() - wv.getBaseX();
        int py = player.getY() - wv.getBaseY();

        int radius = MAX_DRAW_DISTANCE;
        int startX = Math.max(0, px - radius);
        int endX   = Math.min(Constants.SCENE_SIZE - 1, px + radius);
        int startY = Math.max(0, py - radius);
        int endY   = Math.min(Constants.SCENE_SIZE - 1, py + radius);

        for (int x = startX; x <= endX; x++)
        {
            for (int y = startY; y <= endY; y++)

            {
                tile = tiles[z][x][y];

                if (tile == null)
                {
                    continue;
                }
                point = WorldPointUtil.get(tile.getWorldLocation()); // normalize
                long key = WorldPointUtil.compress(point);
                List<TransportDto> trIndexed = TRANSPORT_INDEX.get(key);
                if(trIndexed != null && !trIndexed.isEmpty())
                {
                    points.put(point, trIndexed.size());
                }

            }
        }
    }

    public static void rebuildTransportIndex(List<TransportDto> transports)
    {
        TRANSPORT_INDEX.clear();

        for (TransportDto t : transports)
        {
            WorldPoint normalized = WorldPointUtil.get(t.getSource()); // normalize to same region-base
            long key = WorldPointUtil.compress(normalized);
            TRANSPORT_INDEX.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }
    }
}

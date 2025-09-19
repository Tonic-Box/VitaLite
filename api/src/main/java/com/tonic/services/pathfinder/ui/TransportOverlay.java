package com.tonic.services.pathfinder.ui;

import com.google.common.base.Strings;
import com.tonic.Static;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportLoader;
import com.tonic.util.WorldPointUtil;
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
import java.util.Map;

public class TransportOverlay extends Overlay
{
    private static final int MAX_DRAW_DISTANCE = 32;
    private static final Font FONT = FontManager.getRunescapeFont().deriveFont(Font.BOLD, 16);
    private final Map<WorldPoint,Integer> points = new HashMap<>();

    public TransportOverlay()
    {
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded entry) {
        String color = "<col=00ff00>";
        int opcode = entry.getType();
        if(opcode == MenuAction.WALK.getId())
        {
            Client client = Static.getClient();
            WorldView worldView = client.getTopLevelWorldView();
            if(worldView.getSelectedSceneTile() == null)
                return;

            WorldPoint worldPoint = WorldPointUtil.get(worldView.getSelectedSceneTile().getWorldLocation());
            ArrayList<Transport> tr = TransportLoader.getTransports().get(WorldPointUtil.compress(worldPoint));
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
                        continue;
                    }
                    client.createMenuEntry(1)
                            .setOption("Edit Transport [-> " + WorldPointUtil.fromCompressed(t.getDestination()) + "]")
                            .setTarget(color + "Transport ")
                            .setType(MenuAction.RUNELITE)
                            .onClick(c -> TransportEditorFrame.INSTANCE.selectTransportByObjectAndSource(t.getId(), worldPoint));
                }
            }
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        graphics.setFont(FONT);
        getPoints();
        Client client = Static.getClient();
        WorldView wv = client.getTopLevelWorldView();
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
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

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
        ArrayList<Transport> tr;

        for (int x = 0; x < Constants.SCENE_SIZE; ++x)
        {
            for (int y = 0; y < Constants.SCENE_SIZE; ++y)
            {
                tile = tiles[z][x][y];

                if (tile == null)
                {
                    continue;
                }
                point = tile.getWorldLocation();
                tr = TransportLoader.getTransports().get(WorldPointUtil.compress(point));
                if(tr != null && !tr.isEmpty())
                {
                    points.put(tile.getWorldLocation(), tr.size());
                }
            }
        }
    }
}

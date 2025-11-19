package com.tonic.services.stratpath;

import com.tonic.Static;
import com.tonic.services.GameManager;
import com.tonic.services.pathfinder.StrategicPathing;
import com.tonic.util.WorldPointUtil;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class StratPathOverlay extends Overlay
{
    private static StratPathOverlay INSTANCE;

    public static StratPathOverlay get()
    {
        if(INSTANCE == null)
        {
            INSTANCE = new StratPathOverlay();
        }
        return INSTANCE;
    }
    private final CopyOnWriteArrayList<WorldPoint> warningTiles = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<WorldPoint> impossibleTiles = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<WorldPoint> path = new CopyOnWriteArrayList<>();
    private volatile WorldPoint destinationTile = null;
    StratPathOverlay()
    {
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
        EventBus eventBus = Static.getInjector().getInstance(EventBus.class);
        eventBus.register(this);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if(!Static.getVitaConfig().getDrawStratPath())
            return null;
        drawWorldTiles(graphics, warningTiles, Color.YELLOW);
        drawWorldTiles(graphics, impossibleTiles, Color.RED);
        if(destinationTile != null && GameManager.isReachable(destinationTile))
        {
            if(!path.isEmpty())
            {
                drawWorldTiles(graphics, path, Color.MAGENTA);
            }

            drawWorldTiles(graphics, List.of(destinationTile), Color.GREEN);
        }
        return null;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if(!Static.getVitaConfig().getDrawStratPath() || destinationTile == null || !GameManager.isReachable(destinationTile))
            return;

        path.clear();
        path.addAll(StrategicPathing.pathTo(destinationTile, new HashSet<>(warningTiles), new HashSet<>(impossibleTiles)));
    }

    private void drawWorldTiles(Graphics2D graphics, List<WorldPoint> points, Color color)
    {
        if(points == null || points.isEmpty())
            return;

        final Client client = Static.getClient();
        final WorldView worldView = client.getTopLevelWorldView();
        final WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        final int MAX_DRAW_DISTANCE = 32;

        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
        Stroke stroke = new BasicStroke(2.0f);

        for(WorldPoint point : points)
        {
            if(point.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE)
                continue;

            LocalPoint localPoint = LocalPoint.fromWorld(worldView, point);
            if(localPoint == null)
                continue;

            Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
            if(polygon == null)
                continue;

            OverlayUtil.renderPolygon(graphics, polygon, color, fillColor, stroke);
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded entry) {
        if(!Static.getVitaConfig().getDrawStratPath())
            return;

        int opcode = entry.getType();
        if(opcode != MenuAction.WALK.getId())
            return;

        Client client = Static.getClient();
        WorldView worldView = client.getTopLevelWorldView();
        if(worldView.getSelectedSceneTile() == null)
            return;

        WorldPoint worldPoint = WorldPointUtil.get(worldView.getSelectedSceneTile().getWorldLocation());
        String addColor = "<col=00ff00>";
        String removeColor = "<col=ff0000>";
        String clearColor = "<col=ffff00>";

        String addDestColor = "<col=00ffff>";
        String removeDestColor = "<col=ff00ff>";

        if(warningTiles.contains(worldPoint))
        {
            createOption("Warning", removeColor, () -> warningTiles.remove(worldPoint));
        }
        else
        {
            createOption("Warning", addColor, () -> warningTiles.add(worldPoint));
        }

        if(impossibleTiles.contains(worldPoint))
        {
            createOption("Impossible", removeColor, () -> impossibleTiles.remove(worldPoint));
        }
        else
        {
            createOption("Impossible", addColor, () -> impossibleTiles.add(worldPoint));
        }

        if(destinationTile != null && destinationTile.equals(worldPoint))
        {
            createOption("Destination", removeDestColor, () -> destinationTile = null);
        }
        else if(GameManager.isReachable(worldPoint))
        {
            createOption("Destination", addDestColor, () -> destinationTile = worldPoint);
        }

        createOption("Clear", clearColor, () -> {
            warningTiles.clear();
            impossibleTiles.clear();
        });
    }

    private void createOption(String type, String color, Runnable action)
    {
        Client client = Static.getClient();
        client.getMenu().createMenuEntry(1)
                .setOption("Mark " + type + " Tile")
                .setTarget(color + "Mark ")
                .setType(MenuAction.RUNELITE)
                .onClick(c -> action.run());
    }
}

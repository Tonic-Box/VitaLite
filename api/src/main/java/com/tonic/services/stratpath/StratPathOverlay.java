package com.tonic.services.stratpath;

import com.tonic.Static;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.services.GameManager;
import com.tonic.services.ground.GroundCanvas;
import com.tonic.services.ground.GroundOverlay;
import com.tonic.services.pathfinder.StrategicPathing;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class StratPathOverlay implements GroundOverlay
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
        EventBus eventBus = Static.getInjector().getInstance(EventBus.class);
        eventBus.register(this);
    }

    @Override
    public void paint(GroundCanvas canvas) {
        if(!Static.getVitaConfig().getDrawStratPath())
            return;
        drawWorldTiles(canvas, warningTiles, Color.YELLOW);
        drawWorldTiles(canvas, impossibleTiles, Color.RED);
        if(GameManager.isReachable(destinationTile))
        {
            if(!path.isEmpty())
            {
                drawWorldTiles(canvas, path, Color.MAGENTA);
            }

            drawWorldTiles(canvas, List.of(destinationTile), Color.GREEN);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if(!Static.getVitaConfig().getDrawStratPath() || !GameManager.isReachable(destinationTile))
            return;

        path.clear();
        path.addAll(StrategicPathing.pathTo(destinationTile, new HashSet<>(warningTiles), new HashSet<>(impossibleTiles)));
    }

    private void drawWorldTiles(GroundCanvas canvas, List<WorldPoint> points, Color color)
    {
        if(points == null || points.isEmpty() || PlayerEx.getLocal() == null)
            return;

        final WorldPoint playerLocation = PlayerEx.getLocal().getWorldPoint();
        final int MAX_DRAW_DISTANCE = 32;

        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
        for(WorldPoint point : points)
        {
            if(point.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE)
                continue;

            canvas.markTile(point.getX(), point.getY(), point.getPlane(), color, fillColor, GroundCanvas.THIN);
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

package com.tonic.services;

import com.tonic.Static;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.MiniMapAPI;
import com.tonic.api.widgets.WorldMapAPI;
import com.tonic.data.TileItemEx;
import com.tonic.data.TileObjectEx;
import com.tonic.services.hotswapper.PluginReloader;
import com.tonic.services.pathfinder.Pathfinder;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.model.Step;
import com.tonic.services.pathfinder.transports.TransportLoader;
import com.tonic.util.ThreadPool;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * GameManager
 */
public class GameManager extends Overlay {
    //static api
    public static int getTickCount()
    {
        return INSTANCE.tickCount;
    }

    public static Stream<Player> playerStream()
    {
        return  playerList().stream();
    }

    public static Stream<NPC> npcStream()
    {
        return npcList().stream();
    }

    public static ArrayList<Player> playerList()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getTopLevelWorldView().players().stream().collect(Collectors.toCollection(ArrayList::new)));
    }

    public static ArrayList<NPC> npcList()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getTopLevelWorldView().npcs().stream().collect(Collectors.toCollection(ArrayList::new)));
    }

    public static Stream<TileObjectEx> objectStream()
    {
        return objectList().stream();
    }

    public static ArrayList<TileObjectEx> objectList()
    {
        return Static.invoke(() -> {
            ArrayList<TileObjectEx> temp = new ArrayList<>();
            Client client = Static.getClient();
            Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();
            for (Tile[][] value : tiles) {
                for (Tile[] item : value) {
                    for (Tile tile : item) {
                        if (tile != null) {
                            if (tile.getGameObjects() != null) {
                                for (GameObject gameObject : tile.getGameObjects()) {
                                    if (gameObject != null && gameObject.getSceneMinLocation().equals(tile.getSceneLocation())) {
                                        temp.add(new TileObjectEx(gameObject));
                                    }
                                }
                            }
                            if (tile.getWallObject() != null) {
                                temp.add(new TileObjectEx(tile.getWallObject()));
                            }
                            if (tile.getDecorativeObject() != null) {
                                temp.add(new TileObjectEx(tile.getDecorativeObject()));
                            }
                            if (tile.getGroundObject() != null) {
                                temp.add(new TileObjectEx(tile.getGroundObject()));
                            }
                        }
                    }
                }
            }
            return temp;
        });
    }

    public static Stream<TileItemEx> tileItemStream()
    {
        return new ArrayList<>(INSTANCE.tileItemCache).stream();
    }

    public static ArrayList<TileItemEx> tileItemList()
    {
        return new ArrayList<>(INSTANCE.tileItemCache);
    }


    //singleton instance
    private final static GameManager INSTANCE = new GameManager();


    /**
     * For internal use only, a call to this is injected into RL on
     * startup to ensure static init of this class runs early on.
     */
    public static void init()
    {
    }

    private GameManager()
    {
        OverlayManager overlayManager = Static.getInjector().getInstance(OverlayManager.class);
        overlayManager.add(this);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_LOW);
        setLayer(OverlayLayer.ABOVE_WIDGETS);

        Static.getRuneLite()
                .getEventBus()
                .register(this);
        TransportLoader.init();
        BankCache.init();

        ThreadPool.submit(() -> {
            Client client = Static.getClient();
            while(client == null || client.getGameState() != GameState.LOGIN_SCREEN)
            {
                Delays.wait(1000);
                client = Static.getClient();
            }
            PluginReloader.init();
            PluginReloader.forceRebuildPluginList();
        });

        System.out.println("GameCache initialized!");
    }

    private final List<TileItemEx> tileItemCache = new CopyOnWriteArrayList<>();
    private int tickCount = 0;
    private volatile List<WorldPoint> pathPoints = null;
    private volatile List<WorldPoint> testPoints = null;

    public static void setPathPoints(List<WorldPoint> points)
    {
        INSTANCE.pathPoints = points;
    }

    public static void clearPathPoints()
    {
        INSTANCE.pathPoints = null;
    }

    @Subscribe
    protected void onGameTick(GameTick event)
    {
        tickCount++;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if(event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
            tickCount = 0;
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned event)
    {
        tileItemCache.add(
                new TileItemEx(
                        event.getItem(),
                        WorldPoint.fromLocal(Static.getClient(), event.getTile().getLocalLocation()),
                        event.getTile().getLocalLocation()
                )
        );
    }

    @Subscribe
    public void onItemDespawned(ItemDespawned event)
    {
        tileItemCache.removeIf(ex -> ex.getItem().equals(event.getItem()) &&
                ex.getWorldLocation().equals(WorldPoint.fromLocal(Static.getClient(), event.getTile().getLocalLocation())) &&
                ex.getLocalPoint().equals(event.getTile().getLocalLocation())
        );
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        final Client client = Static.getClient();
        final Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if(map == null)
            return;

        Point lastMenuOpenedPoint = client.getMouseCanvasPosition();
        final WorldPoint wp = WorldMapAPI.convertMapClickToWorldPoint(client, lastMenuOpenedPoint.getX(), lastMenuOpenedPoint.getY());

        if (wp != null) {
            addMenuEntry(event, wp);
        }
    }

    private void addMenuEntry(MenuEntryAdded event, WorldPoint wp) {
        final Client client = Static.getClient();
        List<MenuEntry> entries = new LinkedList<>(Arrays.asList(client.getMenu().getMenuEntries()));

        if (entries.stream().anyMatch(e -> e.getOption().equals("Walk ") || e.getOption().equals("Test Path ") || e.getOption().equals("Clear "))) {
            return;
        }

        String color = "<col=00ff00>";
        if(!Walker.isWalking())
        {
            client.getMenu().createMenuEntry(0)
                    .setOption("Walk ")
                    .setTarget(color + wp.toString() + " ")
                    .setParam0(event.getActionParam0())
                    .setParam1(event.getActionParam1())
                    .setIdentifier(event.getIdentifier())
                    .setType(MenuAction.RUNELITE)
                    .onClick(e -> ThreadPool.submit(() -> Walker.walkTo(wp)));
        }
        else
        {
            client.getMenu().createMenuEntry(0)
                    .setOption("Cancel ")
                    .setTarget(color + "Walker ")
                    .setParam0(event.getActionParam0())
                    .setParam1(event.getActionParam1())
                    .setIdentifier(event.getIdentifier())
                    .setType(MenuAction.RUNELITE)
                    .onClick(e -> Walker.cancelWalk());
        }


        color = "<col=9B59B6>";
        client.getMenu().createMenuEntry(0)
                .setOption("Test Path ")
                .setTarget(color + wp.toString() + " ")
                .setParam0(event.getActionParam0())
                .setParam1(event.getActionParam1())
                .setIdentifier(event.getIdentifier())
                .setType(MenuAction.RUNELITE)
                .onClick(e -> ThreadPool.submit(() -> {
                    Pathfinder engine = new Pathfinder(wp);
                    List<Step> path = engine.find();
                    if(path == null || path.isEmpty())
                        return;
                    testPoints = Step.toWorldPoints(path);
                }));
        color = "<col=FF0000>";
        if(testPoints != null)
        {
            client.getMenu().createMenuEntry(0)
                    .setOption("Clear ")
                    .setTarget(color + "Test Path ")
                    .setParam0(event.getActionParam0())
                    .setParam1(event.getActionParam1())
                    .setIdentifier(event.getIdentifier())
                    .setType(MenuAction.RUNELITE)
                    .onClick(e -> testPoints = null);
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if(testPoints != null && !testPoints.isEmpty())
        {
            WorldMapAPI.drawPath(graphics, testPoints, Color.MAGENTA);
            MiniMapAPI.drawPath(graphics, testPoints, Color.MAGENTA);
            WorldPoint last = testPoints.get(testPoints.size() - 1);
            WorldMapAPI.drawRedMapMarker(graphics, last);
        }

        if(!Static.getVitaConfig().shouldDrawWalkerPath())
            return null;

        if(pathPoints != null && !pathPoints.isEmpty())
        {
            WorldMapAPI.drawPath(graphics, pathPoints, Color.CYAN);
            MiniMapAPI.drawPath(graphics, pathPoints, Color.CYAN);
            WorldPoint last = pathPoints.get(pathPoints.size() - 1);
            WorldMapAPI.drawGreenMapMarker(graphics, last);
        }

        return null;
    }
}

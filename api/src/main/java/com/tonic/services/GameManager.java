package com.tonic.services;

import com.tonic.Static;
import com.tonic.api.widgets.MinimapAPI;
import com.tonic.data.TileItemEx;
import com.tonic.data.TileObjectEx;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.transports.TransportLoader;
import com.tonic.util.ThreadPool;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import org.lwjgl.system.linux.Stat;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GameManager
{
    //static api
    public static int getTickCount()
    {
        return INSTANCE.tickCount;
    }

    public static Actor getInteracting()
    {
        Client client = Static.getClient();
        Actor interacting = client.getLocalPlayer().getInteracting();
        if(interacting == null)
            interacting = INSTANCE.lastInteracting;
        return interacting;
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
                                    if (gameObject != null) {
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
        Static.getRuneLite()
                .getEventBus()
                .register(this);
        TransportLoader.init();
        System.out.println("GameCache initialized!");
    }

    private final List<TileItemEx> tileItemCache = new CopyOnWriteArrayList<>();
    private Actor lastInteracting = null;
    private int tickCount = 0;
    private Point lastMenuOpenedPoint;

    @Subscribe
    public void onGameTick(GameTick event)
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
    public void onInteractingChanged(InteractingChanged event)
    {
        Client client = Static.getClient();
        if(event.getSource() == client.getLocalPlayer() && event.getTarget() != null)
        {
            if(filter(event.getSource()))
                lastInteracting = event.getTarget();
        }
        else if(event.getTarget() == client.getLocalPlayer() && event.getSource() != null)
        {
            if(filter(event.getSource()))
                lastInteracting = event.getSource();
        }
    }

    private boolean filter(Actor actor)
    {
        return actor.getName() != null;
    }

    //tile items

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
    public void onMenuOpened(MenuOpened event) {
        final Client client = Static.getClient();
        lastMenuOpenedPoint = client.getMouseCanvasPosition();
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {


        final Client client = Static.getClient();
        final Widget map = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
        if(map == null)
            return;

        Point lastMenuOpenedPoint = client.getMouseCanvasPosition();
        final WorldPoint wp = MinimapAPI.convertMapClickToWorldPoint(client, lastMenuOpenedPoint.getX(), lastMenuOpenedPoint.getY());

        if (wp != null) {
            addMenuEntry(event, wp);
        }
    }

    private void addMenuEntry(MenuEntryAdded event, WorldPoint wp) {
        final Client client = Static.getClient();
        List<MenuEntry> entries = new LinkedList<>(Arrays.asList(client.getMenuEntries()));

        if (entries.stream().anyMatch(e -> e.getOption().equals("Pathfind") && e.getTarget().equals("Here"))) {
            return;
        }

        String color = "<col=00ff00>";
        client.createMenuEntry(0)
                .setOption("Walk ")
                .setTarget(color + wp.toString() + " ")
                .setParam0(event.getActionParam0())
                .setParam1(event.getActionParam1())
                .setIdentifier(event.getIdentifier())
                .setType(MenuAction.RUNELITE)
                .onClick(e -> ThreadPool.submit(() -> Walker.walkTo(wp)));
    }
}

package com.tonic.services;

import com.tonic.Static;
import com.tonic.data.TileItemEx;
import com.tonic.data.TileObjectEx;
import com.tonic.services.pathfinder.TransportLoader;
import com.tonic.util.ThreadPool;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class GameCache
{
    private static boolean INITIAL_LOGIN = true;
    private static boolean REFRESH_PATH = false;
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
        return new ArrayList<>(INSTANCE.playerCache).stream();
    }

    public static Stream<NPC> npcStream()
    {
        return new ArrayList<>(INSTANCE.npcCache).stream();
    }

    public static ArrayList<Player> playerList()
    {
        return new ArrayList<>(INSTANCE.playerCache);
    }

    public static ArrayList<NPC> npcList()
    {
        return new ArrayList<>(INSTANCE.npcCache);
    }

    public static Stream<TileObjectEx> objectStream()
    {
        return new ArrayList<>(INSTANCE.objectCache).stream();
    }

    public static ArrayList<TileObjectEx> objectList()
    {
        return new ArrayList<>(INSTANCE.objectCache);
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
    private final static GameCache INSTANCE = new GameCache();


    /**
     * For internal use only, a call to this is injected into RL on
     * startup to ensure static init of this class runs early on.
     */
    public static void init()
    {
    }

    private GameCache()
    {
        Static.getRuneLite()
                .getEventBus()
                .register(this);
        TransportLoader.init();
        System.out.println("GameCache initialized!");
    }

    private final List<TileObjectEx> objectCache = new CopyOnWriteArrayList<>();
    private final List<TileItemEx> tileItemCache = new CopyOnWriteArrayList<>();
    private final List<NPC> npcCache = new CopyOnWriteArrayList<>();
    private final List<Player> playerCache = new CopyOnWriteArrayList<>();
    private Actor lastInteracting = null;
    private int tickCount = 0;

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

//        switch (event.getGameState())
//        {
//            case UNKNOWN:
//            case STARTING:
//            case LOGIN_SCREEN:
//            case LOGIN_SCREEN_AUTHENTICATOR:
//            case CONNECTION_LOST:
//                INITIAL_LOGIN = true;
//                break;
//            case LOGGED_IN:
//                if (INITIAL_LOGIN)
//                {
//                    INITIAL_LOGIN = false;
//                    ThreadPool.schedule(() -> {
//                        REFRESH_PATH = true;
//                        TransportLoader.refreshTransports();
//                    }, 1000, TimeUnit.MILLISECONDS);
//                }
//        }
    }

    // ############## Actors ##############

    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event)
    {
        playerCache.add(event.getPlayer());
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        npcCache.add(event.getNpc());
    }

    @Subscribe
    public void onPlayerDespawned(PlayerDespawned event)
    {
        playerCache.remove(event.getPlayer());
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        npcCache.remove(event.getNpc());
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

    // ############## TileObjects ##############

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        addTileObject(event.getGameObject());
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        removeTileObject(event.getGameObject());
    }

    @Subscribe
    public void onWallObjectSpawned(WallObjectSpawned event)
    {
        addTileObject(event.getWallObject());
    }

    @Subscribe
    public void onWallObjectDespawned(WallObjectDespawned event)
    {
        removeTileObject(event.getWallObject());
    }

    @Subscribe
    public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
    {
        addTileObject(event.getDecorativeObject());
    }

    @Subscribe
    public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
    {
        removeTileObject(event.getDecorativeObject());
    }

    @Subscribe
    public void onGroundObjectSpawned(GroundObjectSpawned event)
    {
        addTileObject(event.getGroundObject());
    }

    @Subscribe
    public void onGroundObjectDespawned(GroundObjectDespawned event)
    {
        removeTileObject(event.getGroundObject());
    }

    private void addTileObject(TileObject tileObject)
    {
        objectCache.add(new TileObjectEx(tileObject));
    }

    private void removeTileObject(TileObject tileObject)
    {
        objectCache.removeIf(ex -> ex.getTileObject().equals(tileObject));
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
}

package com.tonic.services;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.MiniMapAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.api.widgets.WorldMapAPI;
import com.tonic.data.LoginResponse;
import com.tonic.data.TileItemEx;
import com.tonic.data.TileObjectEx;
import com.tonic.services.hotswapper.PluginReloader;
import com.tonic.services.mouse.ClickVisualizationOverlay;
import com.tonic.services.mouse.MovementVisualizationOverlay;
import com.tonic.services.pathfinder.abstractions.IPathfinder;
import com.tonic.services.pathfinder.abstractions.IStep;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.model.WalkerPath;
import com.tonic.services.pathfinder.transports.TransportLoader;
import com.tonic.util.RuneliteConfigUtil;
import com.tonic.util.ThreadPool;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collections;
import java.util.IdentityHashMap;

/**
 * GameManager
 */
public class GameManager extends Overlay {
    //static api
    public static int getTickCount()
    {
        return INSTANCE.tickCount;
    }
    private static int lastUpdateTileObjects = 0;
    private static int lastUpdatePlayers = 0;
    private static int lastUpdateNpcs = 0;
    private static final List<TileObjectEx> tileObjects = new ArrayList<>();
    private static final List<NPC> npcs = new ArrayList<>();
    private static final List<Player> players = new ArrayList<>();
    public static Stream<Player> playerStream()
    {
        return  playerList().stream();
    }

    public static Stream<NPC> npcStream()
    {
        return npcList().stream();
    }
    private static WalkerPath walkerPath;

    public static List<Player> playerList()
    {
        Client client = Static.getClient();

        if (lastUpdatePlayers < client.getTickCount())
        {
            players.clear();
            List<Player> playersList = Static.invoke(() -> client.getTopLevelWorldView().players().stream().collect(Collectors.toCollection(ArrayList::new)));
            players.addAll(playersList);
            lastUpdatePlayers = client.getTickCount();
        }

        return players;
    }

    public static List<NPC> npcList()
    {
        Client client = Static.getClient();

        if (lastUpdateNpcs < client.getTickCount())
        {
            npcs.clear();
            List<NPC> npcsList = Static.invoke(() -> client.getTopLevelWorldView().npcs().stream().collect(Collectors.toCollection(ArrayList::new)));
            npcs.addAll(npcsList);
            lastUpdateNpcs = client.getTickCount();
        }

        return npcs;
    }

    public static Stream<TileObjectEx> objectStream()
    {
        return objectList().stream();
    }

    public static List<TileObjectEx> objectList()
    {
        Client client = Static.getClient();

        if (lastUpdateTileObjects < client.getTickCount())
        {
            tileObjects.clear();

            ArrayList<TileObjectEx> objects = Static.invoke(() -> {
                ArrayList<TileObjectEx> temp = new ArrayList<>();
                Tile[][] value = client.getTopLevelWorldView().getScene().getTiles()[client.getTopLevelWorldView().getPlane()];
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
                return temp;
            });

            tileObjects.addAll(objects);
            lastUpdateTileObjects = client.getTickCount();
        }

        return GameManager.tileObjects;
    }

    public static Stream<TileItemEx> tileItemStream()
    {
        return tileItemList().stream();
    }

    public static ArrayList<TileItemEx> tileItemList()
    {
        Client client = Static.getClient();
        WorldView wv = client.getTopLevelWorldView();
        ArrayList<TileItemEx> copy = new ArrayList<>(INSTANCE.tileItemCache);
        copy.removeIf(i -> i.getWorldLocation().getPlane() != wv.getPlane());
        return copy;
    }

    public static Stream<Widget> widgetStream()
    {
        return widgetList().stream();
    }

    public static List<Widget> widgetList() {
        return Static.invoke(() -> {
            Widget[] roots = ((Client) Static.getClient()).getWidgetRoots();

            List<Widget> result = new ArrayList<>(256);
            Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            Deque<Widget> toProcess = new ArrayDeque<>();
            addNonNull(toProcess, roots);
            while (!toProcess.isEmpty()) {
                Widget widget = toProcess.pop();

                if (!visited.add(widget)) {
                    continue;
                }

                result.add(widget);
                addNonNull(toProcess, widget.getChildren());
                addNonNull(toProcess, widget.getStaticChildren());
                addNonNull(toProcess, widget.getDynamicChildren());
                addNonNull(toProcess, widget.getNestedChildren());
            }

            return result;
        });
    }

    public static List<Widget> widgetList(Widget... roots) {
        return Static.invoke(() -> {
            List<Widget> result = new ArrayList<>(256);
            Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            Deque<Widget> toProcess = new ArrayDeque<>();
            addNonNull(toProcess, roots);
            while (!toProcess.isEmpty()) {
                Widget widget = toProcess.pop();

                if (!visited.add(widget)) {
                    continue;
                }

                result.add(widget);
                addNonNull(toProcess, widget.getChildren());
                addNonNull(toProcess, widget.getStaticChildren());
                addNonNull(toProcess, widget.getDynamicChildren());
                addNonNull(toProcess, widget.getNestedChildren());
            }

            return result;
        });
    }

    public static List<Widget> widgetList(int... rootIds) {
        Widget[] roots = new Widget[rootIds.length];
        for(int i = 0; i < rootIds.length; i++)
        {
            roots[i] = WidgetAPI.get(rootIds[i]);
        }

        return Static.invoke(() -> {
            List<Widget> result = new ArrayList<>(256);
            Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            Deque<Widget> toProcess = new ArrayDeque<>();
            addNonNull(toProcess, roots);
            while (!toProcess.isEmpty()) {
                Widget widget = toProcess.pop();

                if (!visited.add(widget)) {
                    continue;
                }

                result.add(widget);
                addNonNull(toProcess, widget.getChildren());
                addNonNull(toProcess, widget.getStaticChildren());
                addNonNull(toProcess, widget.getDynamicChildren());
                addNonNull(toProcess, widget.getNestedChildren());
            }

            return result;
        });
    }

    private static void addNonNull(Deque<Widget> stack, Widget[] widgets) {
        if (widgets != null) {
            for (Widget w : widgets) {
                if (w != null) {
                    stack.push(w);
                }
            }
        }
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

        ClickVisualizationOverlay clickVizOverlay = Static.getInjector().getInstance(ClickVisualizationOverlay.class);
        overlayManager.add(clickVizOverlay);

        MovementVisualizationOverlay moveVizOverlay = Static.getInjector().getInstance(MovementVisualizationOverlay.class);
        overlayManager.add(moveVizOverlay);

        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_LOW);
        setLayer(OverlayLayer.ABOVE_WIDGETS);

        this.tileOverlays = new TileOverlays(this);
        overlayManager.add(tileOverlays);

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
            Walker.getObjectMap();
            PluginReloader.init();
            PluginReloader.forceRebuildPluginList();

            RuneliteConfigUtil.verifyCacheAndVersion(client.getRevision());

            if(AutoLogin.getCredentials() != null)
            {
                try
                {
                    String[] parts = AutoLogin.getCredentials().split(":");
                    AutoLogin.setCredentials(null);
                    if(parts.length == 2)
                    {
                        LoginService.login(parts[0], parts[1], true);
                    }
                    else if(parts.length == 3)
                    {
                        LoginService.login(parts[0], parts[1], parts[2], true);
                    }
                }
                catch (Exception e)
                {
                    Logger.error("AutoLogin failed: " + e.getMessage());
                }
            }
        });

        System.out.println("GameCache initialized!");
    }

    private final List<TileItemEx> tileItemCache = new CopyOnWriteArrayList<>();
    private int tickCount = 0;
    @Getter
    private volatile List<WorldPoint> pathPoints = null;
    @Getter
    private volatile List<WorldPoint> testPoints = null;

    private final TileOverlays tileOverlays;

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
        if(walkerPath != null && !walkerPath.step())
        {
            walkerPath = null;
        }
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
        if(walkerPath == null)
        {
            client.getMenu().createMenuEntry(0)
                    .setOption("Walk ")
                    .setTarget(color + wp.toString() + " ")
                    .setParam0(event.getActionParam0())
                    .setParam1(event.getActionParam1())
                    .setIdentifier(event.getIdentifier())
                    .setType(MenuAction.RUNELITE)
                    .onClick(e -> walkerPath = WalkerPath.get(wp));
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
                    .onClick(e -> walkerPath.cancel());
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
                    final IPathfinder engine = Static.getVitaConfig().getPathfinderImpl().newInstance();
                    List<? extends IStep> path = engine.find(wp);
                    if(path == null || path.isEmpty())
                        return;
                    testPoints = IStep.toWorldPoints(path);
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
    @Subscribe
    public void onLoginResponse(LoginResponse event)
    {
        if(event.isBanned())
        {
            Logger.error("LoginResponse: Account is banned!" );
        }
    }
}

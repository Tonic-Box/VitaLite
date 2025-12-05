package com.tonic.services;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.SceneAPI;
import com.tonic.api.game.sailing.BoatStatsAPI;
import com.tonic.services.pathfinder.sailing.BoatCollisionAPI;
import com.tonic.services.pathfinder.sailing.BoatPathing;
import com.tonic.api.game.sailing.SailingAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.MiniMapAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.api.widgets.WorldMapAPI;
import com.tonic.data.LayoutView;
import com.tonic.data.LoginResponse;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.headless.HeadlessMode;
import com.tonic.services.hotswapper.PluginReloader;
import com.tonic.services.mouse.ClickVisualizationOverlay;
import com.tonic.services.mouse.MovementVisualizationOverlay;
import com.tonic.services.pathfinder.abstractions.IPathfinder;
import com.tonic.services.pathfinder.abstractions.IStep;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.model.WalkerPath;
import com.tonic.services.pathfinder.transports.TransportLoader;
import com.tonic.services.stratpath.StratPathOverlay;
import com.tonic.ui.VitaOverlay;
import com.tonic.util.Profiler;
import com.tonic.util.RuneliteConfigUtil;
import com.tonic.util.ThreadPool;
import com.tonic.util.WorldPointUtil;
import com.tonic.util.handler.StepHandler;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.*;
import net.runelite.http.api.worlds.WorldResult;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
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
    private static int lastUpdateReachableTiles = 0;
    private static int lastUpdateTileItems = 0;
    private static final List<TileObjectEx> tileObjects = new ArrayList<>();
    private static final List<NpcEx> npcs = new ArrayList<>();
    private static final List<PlayerEx> players = new ArrayList<>();
    private static final List<TileItemEx> tileItemCache = new CopyOnWriteArrayList<>();
    @Setter
    @Getter
    private static WalkerPath walkerPath;
    private static volatile StepHandler sailingPath;
    private static final TIntSet reachableTiles = new TIntHashSet();
    private static final Set<Integer> worldViews = ConcurrentHashMap.newKeySet();

    public static Stream<PlayerEx> playerStream()
    {
        return  playerList().stream();
    }

    public static Stream<NpcEx> npcStream()
    {
        return npcList().stream();
    }

    public static List<PlayerEx> playerList()
    {
        Client client = Static.getClient();

        if (lastUpdatePlayers < client.getTickCount())
        {
            players.clear();
            for(int id : worldViews)
            {
                WorldView wv = client.getWorldView(id);
                if(wv == null)
                    continue;
                players.addAll(Static.invoke(() ->
                        wv.players().stream()
                                .map(PlayerEx::new)
                                .collect(Collectors.toList())
                ));
            }
            lastUpdatePlayers = client.getTickCount();
        }

        return players;
    }

    public static List<NpcEx> npcList()
    {
        Client client = Static.getClient();

        if (lastUpdateNpcs < client.getTickCount())
        {
            npcs.clear();
            for(int id : worldViews)
            {
                WorldView wv = client.getWorldView(id);
                if(wv == null)
                    continue;
                npcs.addAll(Static.invoke(() ->
                        wv.npcs().stream()
                                .map(NpcEx::new)
                                .collect(Collectors.toList())
                ));
            }
            lastUpdateNpcs = client.getTickCount();
        }

        return npcs;
    }

    public static boolean isReachable(WorldPoint worldPoint)
    {
        return isReachable(WorldPointUtil.compress(worldPoint));
    }

    public static boolean isReachable(int x, int y, int plane)
    {
        return isReachable(WorldPointUtil.compress(x, y, plane));
    }

    public static boolean isReachable(int compressed)
    {
        Client client = Static.getClient();
        if(lastUpdateReachableTiles < client.getTickCount())
        {
            reachableTiles.clear();
            reachableTiles.addAll(SceneAPI.reachableTilesCompressed(PlayerEx.getLocal().getWorldPoint()));
            lastUpdateReachableTiles = client.getTickCount();
        }
        return reachableTiles.contains(compressed);
    }

    public static List<Tile> getTiles()
    {
        Client client = Static.getClient();
        int totalSize = Constants.SCENE_SIZE * Constants.SCENE_SIZE * worldViews.size();
        List<Tile> out = new ArrayList<>(totalSize);
        for(int id : worldViews)
        {
            WorldView wv = client.getWorldView(id);
            if(wv == null)
                continue;

            Tile[][] planeTiles = wv.getScene().getTiles()[wv.getPlane()];

            for (Tile[] row : planeTiles) {
                Collections.addAll(out, row);
            }
        }
        return out;
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
                for(int wv : worldViews)
                {
                    WorldView worldView = client.getWorldView(wv);
                    if(worldView == null)
                        continue;

                    Tile[][] value = worldView.getScene().getTiles()[worldView.getPlane()];
                    for (Tile[] item : value) {
                        for (Tile tile : item) {
                            if (tile != null) {
                                if (tile.getGameObjects() != null) {
                                    for (GameObject gameObject : tile.getGameObjects()) {
                                        if (gameObject != null && gameObject.getSceneMinLocation().equals(tile.getSceneLocation())) {
                                            if((gameObject.getHash() >>> 16 & 0x7L) != 2)
                                                continue;
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

            tileObjects.addAll(objects);
            lastUpdateTileObjects = client.getTickCount();
        }

        return GameManager.tileObjects;
    }

    public static Stream<TileItemEx> tileItemStream()
    {
        return tileItemList().stream();
    }

    public static List<TileItemEx> tileItemList()
    {
        Client client = Static.getClient();
        if(lastUpdateTileItems < client.getTickCount())
        {
            tileItemCache.clear();
            tileItemCache.addAll(Static.invoke(() -> {
                ArrayList<TileItemEx> temp = new ArrayList<>();
                WorldView wv = client.getTopLevelWorldView();
                Tile[][] value = wv.getScene().getTiles()[wv.getPlane()];
                for(int x = 0; x < value.length; x++)
                {
                    for (int y = 0; y < value[x].length; y++)
                    {
                        Tile tile = value[x][y];
                        if (tile != null) {
                            if(tile.getGroundItems() != null)
                            {
                                for(TileItem tileItem : tile.getGroundItems())
                                {
                                    if(tileItem != null)
                                    {
                                        WorldPoint wp = WorldPoint.fromScene(wv, x, y, wv.getPlane());
                                        TileItemEx itemEx = new TileItemEx(tileItem, wp);
                                        temp.add(itemEx);
                                    }
                                }
                            }
                        }
                    }
                }
                return temp;
            }));

            lastUpdateTileItems = client.getTickCount();
        }

        return tileItemCache;
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

        StratPathOverlay stratPathOverlay = Static.getInjector().getInstance(StratPathOverlay.class);
        overlayManager.add(stratPathOverlay);

        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_LOW);
        setLayer(OverlayLayer.ABOVE_WIDGETS);

        TileOverlays tileOverlays = new TileOverlays(this);
        overlayManager.add(tileOverlays);

        DistanceOverlays distanceOverlays = new DistanceOverlays();
        overlayManager.add(distanceOverlays);

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

            if(WorldSetter.getWorld() != -1)
            {
                hop(WorldSetter.getWorld(), client);
            }

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

    public void hop(int worldNumber, Client client)
    {
        WorldResult worldResult = Static.getInjector().getInstance(WorldService.class).getWorlds();
        if(worldResult == null) return;;
        net.runelite.http.api.worlds.World world = worldResult.findWorld(worldNumber);
        if(world == null) return;
        final net.runelite.api.World rsWorld = client.createWorld();
        rsWorld.setActivity(world.getActivity());
        rsWorld.setAddress(world.getAddress());
        rsWorld.setId(world.getId());
        rsWorld.setPlayerCount(world.getPlayers());
        rsWorld.setLocation(world.getLocation());
        rsWorld.setTypes(net.runelite.client.util.WorldUtil.toWorldTypes(world.getTypes()));
        if(client.getGameState() == GameState.LOGIN_SCREEN)
        {
            client.changeWorld(rsWorld);
            return;
        }
        client.openWorldHopper();
        client.hopToWorld(rsWorld);
    }

    private int tickCount = 0;
    @Getter
    private volatile List<WorldPoint> pathPoints = null;
    @Getter
    private volatile List<WorldPoint> testPoints = null;
    @Getter
    private volatile List<WorldPoint> sailingPoints = null;
    private final BoatOverlay boatOverlay = new BoatOverlay();
    private boolean boatDebugShowing = false;

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

        if(sailingPath != null && !sailingPath.step())
        {
            sailingPath = null;
        }

        if(Static.getVitaConfig().getDrawBoatDebug() && !boatDebugShowing && SailingAPI.isOnBoat())
        {
            boatOverlay.show();
            boatDebugShowing = true;
        }
        else if((!Static.getVitaConfig().getDrawBoatDebug() && boatDebugShowing) || !SailingAPI.isOnBoat())
        {
            boatOverlay.hide();
            boatDebugShowing = false;
        }

        processSpeed();

        Widget gameframe = LayoutView.GAMEFRAME.getWidget();
        if(gameframe == null)
            return;

        if(WidgetAPI.isVisible(gameframe) && Static.isHeadless())
        {
            gameframe.setHidden(true);
        }
        else if(!WidgetAPI.isVisible(gameframe) && !Static.isHeadless())
        {
            gameframe.setHidden(false);
        }

        // Update headless map view if active
        if (Static.isHeadless() && Static.getVitaConfig().shouldShowHeadlessMap()) {
            // Initialize click handlers once when map panel becomes available
            if (!HeadlessMapInteraction.isInitialized()) {
                HeadlessMapInteraction.initialize();
            }

            WorldPoint pos = PlayerEx.getLocal().getWorldPoint();
            HeadlessMode.updateMap(pos.getX(), pos.getY(), pos.getPlane(),
                    (x, y, plane) -> Walker.getCollisionMap().all((short) x, (short) y, (byte) plane));
        }
    }

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        Client client = Static.getClient();
        if(client.getGameState() != GameState.LOGGED_IN)
            return;

        if(Static.getVitaConfig().getDrawBoatDebug() && !boatOverlay.isHidden() && SailingAPI.isOnBoat())
        {
            boatOverlay.update();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if(event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
            tickCount = 0;
    }

    @Subscribe
    public void onWorldViewLoaded(WorldViewLoaded event)
    {
        worldViews.add(event.getWorldView().getId());
    }

    @Subscribe
    public void onWorldViewUnloaded(WorldViewUnloaded event)
    {
        worldViews.remove(event.getWorldView().getId());
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

        if (SailingAPI.isNavigating())
        {
            if(sailingPath == null)
            {
                client.getMenu().createMenuEntry(0)
                        .setOption("Sail ")
                        .setTarget(color + wp.toString() + " ")
                        .setParam0(event.getActionParam0())
                        .setParam1(event.getActionParam1())
                        .setIdentifier(event.getIdentifier())
                        .setType(MenuAction.RUNELITE)
                        .onClick(e -> {
                            ThreadPool.submit(() -> {
                                sailingPath = BoatPathing.travelTo(wp);
                            });
                        });
            }
            else
            {
                client.getMenu().createMenuEntry(0)
                        .setOption("Cancel ")
                        .setTarget(color + "Sailer ")
                        .setParam0(event.getActionParam0())
                        .setParam1(event.getActionParam1())
                        .setIdentifier(event.getIdentifier())
                        .setType(MenuAction.RUNELITE)
                        .onClick(e -> {
                            sailingPath = null;
                            SailingAPI.unSetSails();
                            clearPathPoints();
                        });
            }

            color = "<col=9B59B6>";
            client.getMenu().createMenuEntry(0)
                    .setOption("Test Sail Path ")
                    .setTarget(color + wp.toString() + " ")
                    .setParam0(event.getActionParam0())
                    .setParam1(event.getActionParam1())
                    .setIdentifier(event.getIdentifier())
                    .setType(MenuAction.RUNELITE)
                    .onClick(e -> ThreadPool.submit(() -> {
                        Profiler.Start("FindSailPath");
                        testPoints = BoatPathing.findFullPath(BoatCollisionAPI.getPlayerBoatWorldPoint(), wp);
                        Profiler.StopMS();
                    }));
            color = "<col=FF0000>";
            if(testPoints != null)
            {
                client.getMenu().createMenuEntry(0)
                        .setOption("Clear ")
                        .setTarget(color + "Test Sail Path ")
                        .setParam0(event.getActionParam0())
                        .setParam1(event.getActionParam1())
                        .setIdentifier(event.getIdentifier())
                        .setType(MenuAction.RUNELITE)
                        .onClick(e -> testPoints = null);
            }

            return;
        }

        if(SailingAPI.isOnBoat())
            return;

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

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        BoatStatsAPI.update(event);
    }

    private LocalPoint lastPoint;
    private void processSpeed()
    {
        if(!SailingAPI.isOnBoat())
            return;
        WorldEntity we = BoatCollisionAPI.getPlayerBoat();
        LocalPoint lp = we.getTargetLocation();

        if (!lp.equals(lastPoint))
        {
            if (lastPoint != null)
            {
                double trueSpeed = (float) Math.hypot(
                        (lastPoint.getX() - lp.getX()),
                        (lastPoint.getY() - lp.getY())
                );
                int speed = roundToQuarterTile(trueSpeed) / 32;
                if(speed > 32)
                    return;
                SailingAPI.setSpeed(speed);
            }
            lastPoint = lp;
        }
        else
        {
            SailingAPI.setSpeed(0);
        }
    }

    private static int roundToQuarterTile(double trueSpeed)
    {
        int quarterTileFloor = ((int) trueSpeed) & ~0x1F;
        int quarterTileCeil = quarterTileFloor + 0x20;

        if (quarterTileCeil - trueSpeed < trueSpeed - quarterTileFloor)
        {
            return quarterTileCeil;
        }

        return quarterTileFloor;
    }
}

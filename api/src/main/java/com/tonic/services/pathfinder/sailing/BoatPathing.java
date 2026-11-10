package com.tonic.services.pathfinder.sailing;

import com.tonic.api.handlers.GenericHandlerBuilder;
import com.tonic.data.locatables.sailing.PortLocation;
import com.tonic.services.GameManager;
import com.tonic.services.pathfinder.sailing.sea.SeaRoute;
import com.tonic.util.WorldPointUtil;
import com.tonic.util.handler.StepHandler;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.runelite.api.coords.WorldPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Sails the player's boat: voyages as step handlers to run a tick at a time, and route planning
 */
public class BoatPathing
{
    /**
     * Sails to a tile and stops there
     * @param worldPoint the tile to sail to
     * @return the step handler that runs the voyage
     */
    public static StepHandler travelTo(WorldPoint worldPoint)
    {
        return voyage(sailing -> sailing.sailTo(worldPoint));
    }

    /**
     * Sails to a port and docks there
     * @param port the port to dock at
     * @return the step handler that runs the voyage
     */
    public static StepHandler travelTo(PortLocation port)
    {
        return voyage(sailing -> sailing.sailTo(port(sailing, port)));
    }

    /**
     * Walks to a port's gangplank and boards the player's boat, recovering it first when it lies elsewhere
     * @param port the port to board at
     * @return the step handler that runs the boarding
     */
    public static StepHandler board(PortLocation port)
    {
        return voyage(sailing -> sailing.board(port(sailing, port)));
    }

    /**
     * Docks at a port from within reach of its gangplank
     * @param port the port to dock at
     * @return the step handler that runs the docking
     */
    public static StepHandler dock(PortLocation port)
    {
        return voyage(sailing -> sailing.dock(port(sailing, port)));
    }

    /**
     * Plans the route from a tile to another on the calling thread
     * @param start the tile to start from
     * @param target the tile to reach
     * @return the route's tiles, empty when there is none
     */
    public static List<WorldPoint> findFullPath(WorldPoint start, WorldPoint target)
    {
        return findFullPath(start, target, null);
    }

    /**
     * Plans the route from a tile to another keeping off some tiles
     * @param start the tile to start from
     * @param target the tile to reach
     * @param avoidTiles tiles to keep off, packed as WorldPointUtil packs them, or null
     * @return the route's tiles, empty when there is none
     */
    public static List<WorldPoint> findFullPath(WorldPoint start, WorldPoint target, IntOpenHashSet avoidTiles)
    {
        Set<WorldPoint> avoid = new HashSet<>();
        if (avoidTiles != null)
        {
            for (IntIterator tiles = avoidTiles.iterator(); tiles.hasNext(); )
            {
                avoid.add(WorldPointUtil.fromCompressed(tiles.nextInt()));
            }
        }
        return Sailing.get().plan(start, target, avoid).tiles();
    }

    /**
     * Stops whatever voyage is under way
     */
    public static void cancel()
    {
        Sailing.get().cancel();
        GameManager.clearPathPoints();
    }

    /**
     * Checks whether a voyage is under way
     * @return true if a voyage is under way, false otherwise
     */
    public static boolean isSailing()
    {
        return Sailing.get().busy();
    }

    /**
     * Gets what the voyage is doing
     * @return the voyage state
     */
    public static Sailing.State getState()
    {
        return Sailing.get().state();
    }

    /**
     * Gets why the last voyage failed
     * @return the failure reason, empty otherwise
     */
    public static String getFailure()
    {
        return Sailing.get().failure();
    }

    private static StepHandler voyage(Consumer<Sailing> start)
    {
        return GenericHandlerBuilder.get()
                .add(() -> start.accept(Sailing.get()))
                .add(context ->
                {
                    Sailing sailing = Sailing.get();
                    SeaRoute route = sailing.currentRoute();
                    if (route != null)
                    {
                        GameManager.setPathPoints(route.tiles());
                    }
                    if (sailing.busy())
                    {
                        return 1;
                    }
                    GameManager.clearPathPoints();
                    return 2;
                })
                .build();
    }

    private static Port port(Sailing sailing, PortLocation location)
    {
        Ports ports = sailing.ports();
        int dock = location.getGangplankObject() - Ports.FIRST_GANGPLANK;
        Port port = location.getGangplankObject() >= 0 ? ports.byDock(dock) : null;
        if (port == null)
        {
            port = ports.byName(location.getName());
        }
        if (port == null)
        {
            throw new IllegalArgumentException(location.getName() + " is not a dock the sea map knows");
        }
        return port;
    }
}

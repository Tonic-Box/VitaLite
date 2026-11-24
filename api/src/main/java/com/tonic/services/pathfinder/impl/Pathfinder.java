package com.tonic.services.pathfinder.impl;

import com.tonic.Logger;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.abstractions.IPathfinder;
import com.tonic.services.pathfinder.collections.BFSCache;
import com.tonic.services.pathfinder.collections.HybridIntQueue;
import com.tonic.services.pathfinder.collision.Flags;
import com.tonic.services.pathfinder.teleports.Teleport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportAvailability;
import com.tonic.util.WorldPointUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Breadth-first route search in which every level is one step from the start: cardinal and diagonal neighbours join the next level, a transport's far end joins TRANSPORT_STEPS_PER_TICK levels on for each tick it takes, and a teleport's landing joins at TELEPORT_PENALTY, so each is weighed against walking in steps. Tiles of the loaded scene are planned over the scene's own collision and everything else over the static map; in an instance the scene's tiles find their transports by the world tile they copy, and a transport landing or a target on a copied tile is reached at its copies.
 */
public class Pathfinder implements IPathfinder
{
    /**
     * How many steps the search walks before the teleports join it, so a target within a short walk is walked to and a distant one is teleported to.
     */
    public static final int TELEPORT_PENALTY = 25;

    /**
     * How many steps of walking a transport weighs for each tick it takes: as far as running goes in a tick.
     */
    public static final int TRANSPORT_STEPS_PER_TICK = 2;

    /**
     * How many tiles a search visits before giving up, unless limitedTo says fewer.
     */
    public static final int MOST_TILES = 10_000_000;

    private final Int2ObjectOpenHashMap<ObjectArrayList<Transport>> transports;
    private final TransportAvailability availability;
    private final List<Teleport> teleports;
    private final int teleportPenalty;
    private final LiveLayer live;
    private int mostTiles = MOST_TILES;
    private IntOpenHashSet goals;
    private SearchWorkspace work;
    private Teleport teleport;
    private int tilesReached;

    /**
     * @param transports the transports by the packed tile they leave from
     * @param availability which of them may be taken now
     * @param teleports the teleports usable now
     * @param teleportPenalty how many steps the search walks before a teleport may be taken; 0 lets a teleport win outright
     * @param live the loaded scene, or LiveLayer.NONE to plan everything over the static map
     */
    public Pathfinder(Int2ObjectOpenHashMap<ObjectArrayList<Transport>> transports, TransportAvailability availability, List<Teleport> teleports, int teleportPenalty, LiveLayer live)
    {
        this.transports = transports == null ? new Int2ObjectOpenHashMap<>() : transports;
        this.availability = availability == null ? TransportAvailability.ALL : availability;
        this.teleports = teleports == null ? Collections.emptyList() : teleports;
        this.teleportPenalty = Math.max(0, teleportPenalty);
        this.live = live == null ? LiveLayer.NONE : live;
    }

    /**
     * Bounds the search, for targets known to be near: a target nobody can reach is otherwise only given up on once every reachable tile has been visited.
     *
     * @param tiles how many tiles to visit at most
     * @return this pathfinder
     * @throws IllegalArgumentException if tiles is less than 1
     */
    public Pathfinder limitedTo(int tiles)
    {
        if (tiles < 1)
        {
            throw new IllegalArgumentException("tiles cannot be less than 1");
        }
        this.mostTiles = tiles;
        return this;
    }

    @Override
    public Teleport getTeleport()
    {
        return teleport;
    }

    /**
     * @return how many tiles the last search reached
     */
    public int tilesReached()
    {
        return tilesReached;
    }

    @Override
    public List<Step> find(WorldPoint start, WorldPoint target)
    {
        this.goals = target == null ? new IntOpenHashSet() : goalsAt(new int[] {WorldPointUtil.compress(target)});
        if (!goals.isEmpty() && !anyWalkable(goals))
        {
            Logger.warn("[Pathfinder] Cannot plan a route to a blocked tile " + target);
            this.goals = new IntOpenHashSet();
        }
        return find(start);
    }

    @Override
    public List<Step> find(WorldPoint start, WorldArea... worldAreas)
    {
        this.goals = goalsAt(WorldPointUtil.toCompressedPoints(worldAreas));
        return find(start);
    }

    @Override
    public List<Step> find(WorldPoint start, List<WorldArea> worldAreas)
    {
        return find(start, worldAreas.toArray(new WorldArea[0]));
    }

    private IntOpenHashSet goalsAt(int[] targets)
    {
        IntOpenHashSet set = new IntOpenHashSet();
        for (int target : targets)
        {
            set.addAll(live.liveKeys(target));
            set.add(target);
        }
        return set;
    }

    private boolean anyWalkable(IntOpenHashSet tiles)
    {
        for (int tile : tiles)
        {
            if (live.walkable(tile))
            {
                return true;
            }
        }
        return false;
    }

    private List<Step> find(WorldPoint start)
    {
        this.teleport = null;
        if (Walker.getCollisionMap() == null)
        {
            Logger.error("[Pathfinder] Collision map is null, cannot perform pathfinding.");
            return new ArrayList<>();
        }
        if (start == null)
        {
            return new ArrayList<>();
        }
        try
        {
            int first = WorldPointUtil.compress(start);
            work = SearchWorkspaces.borrow();
            tilesReached = 0;
            List<Step> path = buildPath(first);
            this.teleport = teleportStarting(path, first);
            return path;
        }
        catch (Exception e)
        {
            Logger.error(e, "[Pathfinder] %e");
            return new ArrayList<>();
        }
        finally
        {
            if (work != null)
            {
                tilesReached = work.visited.size();
                SearchWorkspaces.release(work);
                work = null;
            }
        }
    }

    /**
     * The teleport whose landing the route begins on, or null when it starts from the start tile. A teleport landing on the start tile is never taken, as walking holds that tile from the first level.
     */
    private Teleport teleportStarting(List<Step> path, int start)
    {
        if (path == null || path.isEmpty() || teleports.isEmpty())
        {
            return null;
        }
        int first = path.get(0).getPackedPosition();
        if (first == start)
        {
            return null;
        }
        for (Teleport candidate : teleports)
        {
            if (WorldPointUtil.compress(candidate.getDestination()) == first)
            {
                return candidate;
            }
        }
        return null;
    }

    private List<Step> buildPath(int start)
    {
        BFSCache visited = work.visited;
        HybridIntQueue queue = work.queue;
        markTransportTiles();

        visited.put(start, -1);
        queue.enqueue(start);
        stepOffABlockedStart(start, queue, visited);
        seedTeleports(queue, visited);

        if (goals == null || goals.isEmpty())
        {
            return new ArrayList<>();
        }
        return findGoal(visited, queue);
    }

    /**
     * Lets a route leave a start tile the collision has blocked, which the player stands on all the same, as at the foot of a climb: every walkable tile beside it is reached in the first step.
     */
    private void stepOffABlockedStart(int start, HybridIntQueue queue, BFSCache visited)
    {
        if (live.walkable(start))
        {
            return;
        }
        for (int dx = -1; dx <= 1; dx++)
        {
            for (int dy = -1; dy <= 1; dy++)
            {
                int beside = WorldPointUtil.dxy(start, dx, dy);
                if ((dx != 0 || dy != 0) && live.walkable(beside) && visited.put(beside, start))
                {
                    queue.enqueue(beside);
                }
            }
        }
    }

    /**
     * Adds each teleport's landing as a further place the route may start from, held back until the walk is teleportPenalty steps out.
     */
    private void seedTeleports(HybridIntQueue queue, BFSCache visited)
    {
        for (Teleport candidate : teleports)
        {
            int destination = WorldPointUtil.compress(candidate.getDestination());
            if (!live.walkable(destination))
            {
                continue;
            }
            if (visited.put(destination, -1))
            {
                queue.enqueueAtLevel(destination, teleportPenalty);
            }
        }
    }

    private List<Step> findGoal(BFSCache visited, HybridIntQueue queue)
    {
        int current;
        while (!queue.isEmpty())
        {
            if (visited.size() > mostTiles)
            {
                return new ArrayList<>();
            }
            current = queue.dequeue();
            if (!arrive(current, visited))
            {
                continue;
            }
            if (goals.contains(current))
            {
                return visited.path(current, live);
            }
            addNeighbors(current, queue, visited);
        }
        return new ArrayList<>();
    }

    /**
     * Takes a tile the search has just reached into the route tree. A tile walked to is in the tree already; one a transport leads to is put there when its turn in the held lane comes, unless walking reached it first. A held tile whose arrival was taken already, by walking or by a transport that lands sooner, is a stale copy and is not searched from again.
     *
     * @return whether the search goes on from the tile
     */
    private boolean arrive(int tile, BFSCache visited)
    {
        if (!work.pendingTiles.contains(tile))
        {
            return true;
        }
        Arrival arrival = work.pending.remove(tile);
        if (arrival == null)
        {
            return false;
        }
        visited.put(tile, arrival.from, arrival.transport);
        return true;
    }

    /**
     * A transport arrival waiting in the held lane, with the level it lands at.
     */
    static final class Arrival
    {
        final int from;
        final Transport transport;
        final int level;

        Arrival(int from, Transport transport, int level)
        {
            this.from = from;
            this.transport = transport;
            this.level = level;
        }
    }

    private void addNeighbors(int node, HybridIntQueue queue, BFSCache visited)
    {
        short x = WorldPointUtil.getCompressedX(node);
        short y = WorldPointUtil.getCompressedY(node);
        byte plane = WorldPointUtil.getCompressedPlane(node);

        byte flags = live.covers(x, y, plane) ? live.all(x, y, plane) : Walker.getCollisionMap().all(x, y, plane);
        switch (flags)
        {
            case Flags.ALL:
                addNeighbor(node, WorldPointUtil.compress(x - 1, y, plane), queue, visited);
                addNeighbor(node, WorldPointUtil.compress(x + 1, y, plane), queue, visited);
                addNeighbor(node, WorldPointUtil.compress(x, y - 1, plane), queue, visited);
                addNeighbor(node, WorldPointUtil.compress(x, y + 1, plane), queue, visited);
                addNeighbor(node, WorldPointUtil.compress(x - 1, y - 1, plane), queue, visited);
                addNeighbor(node, WorldPointUtil.compress(x + 1, y - 1, plane), queue, visited);
                addNeighbor(node, WorldPointUtil.compress(x - 1, y + 1, plane), queue, visited);
                addNeighbor(node, WorldPointUtil.compress(x + 1, y + 1, plane), queue, visited);
                checkTransports(node, queue, visited);
                return;
            case Flags.NONE:
                checkTransports(node, queue, visited);
                return;
            default:
                break;
        }

        if ((flags & Flags.WEST) != 0)
        {
            addNeighbor(node, WorldPointUtil.compress(x - 1, y, plane), queue, visited);
        }
        if ((flags & Flags.EAST) != 0)
        {
            addNeighbor(node, WorldPointUtil.compress(x + 1, y, plane), queue, visited);
        }
        if ((flags & Flags.SOUTH) != 0)
        {
            addNeighbor(node, WorldPointUtil.compress(x, y - 1, plane), queue, visited);
        }
        if ((flags & Flags.NORTH) != 0)
        {
            addNeighbor(node, WorldPointUtil.compress(x, y + 1, plane), queue, visited);
        }
        if ((flags & Flags.SOUTHWEST) != 0)
        {
            addNeighbor(node, WorldPointUtil.compress(x - 1, y - 1, plane), queue, visited);
        }
        if ((flags & Flags.SOUTHEAST) != 0)
        {
            addNeighbor(node, WorldPointUtil.compress(x + 1, y - 1, plane), queue, visited);
        }
        if ((flags & Flags.NORTHWEST) != 0)
        {
            addNeighbor(node, WorldPointUtil.compress(x - 1, y + 1, plane), queue, visited);
        }
        if ((flags & Flags.NORTHEAST) != 0)
        {
            addNeighbor(node, WorldPointUtil.compress(x + 1, y + 1, plane), queue, visited);
        }

        checkTransports(node, queue, visited);
    }

    /**
     * Marks every tile a transport leaves from, so the search asks the transport table only about those.
     */
    private void markTransportTiles()
    {
        IntIterator keys = transports.keySet().iterator();
        while (keys.hasNext())
        {
            work.transportTiles.add(keys.nextInt());
        }
    }

    private void checkTransports(int node, HybridIntQueue queue, BFSCache visited)
    {
        int key = live.covers(node) ? live.templateKey(node) : node;
        if (key == -1 || !work.transportTiles.contains(key))
        {
            return;
        }
        ObjectArrayList<Transport> leaving = transports.get(key);
        if (leaving == null)
        {
            return;
        }
        for (Transport transport : leaving)
        {
            if (!availability.allows(transport))
            {
                continue;
            }
            int arrival = queue.level() + transportSteps(transport);
            for (int destination : live.liveKeys(transport.getDestination().getPacked()))
            {
                if (visited.contains(destination))
                {
                    continue;
                }
                Arrival held = work.pending.get(destination);
                if (held != null && held.level <= arrival)
                {
                    continue;
                }
                work.pending.put(destination, new Arrival(node, transport, arrival));
                work.pendingTiles.add(destination);
                queue.enqueueAtLevel(destination, arrival);
            }
        }
    }

    /**
     * @param transport a transport
     * @return how many steps of walking it weighs: TRANSPORT_STEPS_PER_TICK for each tick it takes, at least one
     */
    public static int transportSteps(Transport transport)
    {
        return Math.max(1, transport.getDuration() * TRANSPORT_STEPS_PER_TICK);
    }

    private void addNeighbor(int node, int neighbor, HybridIntQueue queue, BFSCache visited)
    {
        if (!heldBy(neighbor, queue.level() + 1) && visited.put(neighbor, node))
        {
            queue.enqueue(neighbor);
        }
    }

    /**
     * Whether a transport lands on a tile no later than walking would reach it, so walking leaves the tile to that arrival: held tiles join the end of their level, after the tiles walked to from that level have been claimed.
     */
    private boolean heldBy(int tile, int level)
    {
        if (!work.pendingTiles.contains(tile))
        {
            return false;
        }
        Arrival held = work.pending.get(tile);
        return held != null && held.level <= level;
    }
}

package com.tonic.services.pathfinder.implimentations.hybridbfs;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.SceneAPI;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.abstractions.IPathfinder;
import com.tonic.services.pathfinder.collections.BFSCache;
import com.tonic.services.pathfinder.collections.HybridIntQueue;
import com.tonic.services.pathfinder.collision.Flags;
import com.tonic.services.pathfinder.collision.Properties;
import com.tonic.services.pathfinder.local.LocalCollisionMap;
import com.tonic.services.pathfinder.teleports.Teleport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportLoader;
import com.tonic.util.Location;
import com.tonic.util.Profiler;
import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Pathfinder class to find paths between points in the game world.
 */
public class HybridBFSAlgo implements IPathfinder
{
    private LocalCollisionMap localMap;
    @Getter
    private Teleport teleport;
    private WorldPoint targetWorldPoint;

    private int[] worldAreaPoints;
    private boolean inInstance = false;
    private int transportsUsed;

    /**
     * Initializes the pathfinder with a target WorldPoint.
     *
     * @param target The destination WorldPoint to find a path to.
     */
    @Override
    public List<HybridBFSStep> find(final WorldPoint target) {
        TransportLoader.refreshTransports();
        this.targetWorldPoint = target;
        return find();
    }

    /**
     * Initializes the pathfinder with target WorldAreas.
     * @param worldAreas The destination WorldAreas to find a path to the closest area.
     */
    @Override
    public List<HybridBFSStep> find(WorldArea... worldAreas)
    {
        TransportLoader.refreshTransports();
        worldAreaPoints = WorldPointUtil.toCompressedPoints(worldAreas);
        return find();
    }

    /**
     * Initializes the pathfinder with target WorldAreas.
     * @param worldAreas The destination WorldAreas to find a path to the closest area.
     */
    @Override
    public List<HybridBFSStep> find(List<WorldArea> worldAreas)
    {
        TransportLoader.refreshTransports();
        worldAreaPoints = WorldPointUtil.toCompressedPoints(worldAreas.toArray(new WorldArea[0]));
        return find();
    }

    /**
     * Finds a path from the player's current location to the target WorldPoint or WorldArea.
     *
     * @return A list of Steps representing the path, or an empty list if no path is found.
     */
    private List<HybridBFSStep> find() {
        if(Walker.getCollisionMap() == null)
        {
            Logger.error("[Pathfinder] Collision map is null, cannot perform pathfinding.");
            return new ArrayList<>();
        }
        try {
            Client client = Static.getClient();
            this.inInstance = client.getTopLevelWorldView().isInstance();
            List<Teleport> teleports = Teleport.buildTeleportLinks();

            final List<Integer> startPoints = new ArrayList<>();

            if(inInstance)
            {
                localMap = new LocalCollisionMap();
            }

            startPoints.add(0, WorldPointUtil.compress(client.getLocalPlayer().getWorldLocation()));

            for(final Teleport teleport : teleports) {
                if(!filterTeleports(teleport.getDestination()))
                {
                    startPoints.add(WorldPointUtil.compress(teleport.getDestination()));
                }
            }

            Profiler.Start("Pathfinding");

            final List<HybridBFSStep> path = buildPath(startPoints);

            Profiler.StopMS();
            Logger.info("Path Length: " + path.size());

            if(path.isEmpty())
                return path;

            for (final Teleport tp : teleports)
            {
                if(WorldPointUtil.compress(tp.getDestination()) == path.get(0).getPackedPosition())
                {
                    teleport = tp.copy();
                }
            }

            return path;

        } catch (Exception e) {
            Logger.error(e, "[Pathfinder] %e");
            return null;
        }
    }

    private List<HybridBFSStep> buildPath(final List<Integer> starts)
    {
        final BFSCache visited = new BFSCache();

        //blacklist
        for(int i : Properties.getBlacklist())
        {
            visited.put(i, -1);
        }

        final HybridIntQueue queue = new HybridIntQueue(10_000_000);

        for(final int wp : starts)
        {
            visited.put(wp, -1);
            queue.enqueue(wp);
        }


        if(targetWorldPoint != null)
            return findWorldPoint(visited, queue);
        if(worldAreaPoints != null && worldAreaPoints.length > 0)
            return findAreaPoint(visited, queue);
        return new ArrayList<>();
    }

    private List<HybridBFSStep> findAreaPoint(final BFSCache visited, final HybridIntQueue queue) {
        int current;
        while(!queue.isEmpty())
        {
            if(visited.size() > 10_000_000)
            {
                return new ArrayList<>();
            }
            current = queue.dequeue();
            if(ArrayUtils.contains(worldAreaPoints, current))
            {
                //Logger.info("Nodes visited: " + visited.size());
                return visited.path(current);

            }
            addNeighbors(current, queue, visited);
        }
        return new ArrayList<>();
    }

    private List<HybridBFSStep> findWorldPoint(final BFSCache visited, final HybridIntQueue queue) {
        final int targetIndex = WorldPointUtil.compress(targetWorldPoint);

        //validate target
        if(!Walker.getCollisionMap().walkable(targetIndex)) {
            Logger.info("Could not generate path to a blocked tile");
            return new ArrayList<>();
        }

        int current;
        while(!queue.isEmpty())
        {
            if(visited.size() > 10_000_000)
            {
                return new ArrayList<>();
            }
            current = queue.dequeue();
            if(current == targetIndex)
            {
                //Logger.info("Nodes visited: " + visited.size());
                return visited.path(current);

            }
            addNeighbors(current, queue, visited);
        }
        return new ArrayList<>();
    }

    private void addNeighborsLocal(final int node, final HybridIntQueue queue, final BFSCache visited)
    {
        final short x = WorldPointUtil.getCompressedX(node);
        final short y = WorldPointUtil.getCompressedY(node);
        final byte plane = WorldPointUtil.getCompressedPlane(node);

        if(!localMap.w(x, y, plane))
        {
            addNeighbor(node, WorldPointUtil.compress(x - 1, y, plane), queue, visited);
        }

        if(!localMap.e(x, y, plane))
        {
            addNeighbor(node, WorldPointUtil.compress(x + 1, y, plane), queue, visited);
        }

        if(!localMap.n(x, y, plane))
        {
            addNeighbor(node, WorldPointUtil.compress(x, y + 1, plane), queue, visited);
        }

        if(!localMap.s(x, y, plane))
        {
            addNeighbor(node, WorldPointUtil.compress(x, y - 1, plane), queue, visited);
        }

        if(!localMap.nw(x, y, plane))
        {
            addNeighbor(node, WorldPointUtil.compress(x - 1, y + 1, plane), queue, visited);
        }

        if(!localMap.ne(x, y, plane))
        {
            addNeighbor(node, WorldPointUtil.compress(x + 1, y + 1, plane), queue, visited);
        }

        if(!localMap.sw(x, y, plane))
        {
            addNeighbor(node, WorldPointUtil.compress(x - 1, y - 1, plane), queue, visited);
        }

        if(!localMap.se(x, y, plane))
        {
            addNeighbor(node, WorldPointUtil.compress(x + 1, y - 1, plane), queue, visited);
        }
    }

    private void addNeighbors(final int node, final HybridIntQueue queue, final BFSCache visited) {
        final short x = WorldPointUtil.getCompressedX(node);
        final short y = WorldPointUtil.getCompressedY(node);
        final byte plane = WorldPointUtil.getCompressedPlane(node);

        if(x > 6000)
        {
            if(inInstance)
                addNeighborsLocal(node, queue, visited);
            return;
        }

        final byte flags = Walker.getCollisionMap().all(x, y, plane);
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
                return;
        }

        if ((flags & Flags.WEST) != 0) {
            addNeighbor(node, WorldPointUtil.compress(x - 1, y, plane), queue, visited);
        }

        if ((flags & Flags.EAST) != 0) {
            addNeighbor(node, WorldPointUtil.compress(x + 1, y, plane), queue, visited);
        }

        if ((flags & Flags.SOUTH) != 0) {
            addNeighbor(node, WorldPointUtil.compress(x, y - 1, plane), queue, visited);
        }

        if ((flags & Flags.NORTH) != 0) {
            addNeighbor(node, WorldPointUtil.compress(x, y + 1, plane), queue, visited);
        }

        if ((flags & Flags.SOUTHWEST) != 0) {
            addNeighbor(node, WorldPointUtil.compress(x - 1, y - 1, plane), queue, visited);
        }

        if ((flags & Flags.SOUTHEAST) != 0) {
            addNeighbor(node, WorldPointUtil.compress(x + 1, y - 1, plane), queue, visited);
        }

        if ((flags & Flags.NORTHWEST) != 0) {
            addNeighbor(node, WorldPointUtil.compress(x - 1, y + 1, plane), queue, visited);
        }

        if ((flags & Flags.NORTHEAST) != 0) {
            addNeighbor(node, WorldPointUtil.compress(x + 1, y + 1, plane), queue, visited);
        }

        checkTransports(node, queue, visited);
    }

    private void checkTransports(final int node, final HybridIntQueue queue, final BFSCache visited)
    {
        final ArrayList<Transport> tr = TransportLoader.getTransports().get(node);
        if(tr != null)
        {
            for (Transport t : tr) {
                transportsUsed++;
                addTransportNeighbor(node, t.getDestination(), calculateDelay(t.getDuration() * 2, queue.size()), queue, visited);
            }
        }
    }

    private int calculateDelay(int transportDelay, int queueSize) {
        if (transportDelay <= 0) {
            return 0;
        }
        // Recalculate incrementValue without floating point:
        // (5 + 5 * transportsUsed) * 1.2 = 6 * (1 + transportsUsed)
        int incrementValue = 6 * (1 + transportsUsed);
        int part1 = queueSize * transportDelay;
        int part2 = incrementValue * (transportDelay * (transportDelay + 1) / 2);
        int longCalculated = part1 + part2;
        return longCalculated < 0 ? Integer.MAX_VALUE : longCalculated;
    }

    private void addTransportNeighbor(final int node, final int neighbor, final int delay, final HybridIntQueue queue, final BFSCache visited) {
        if (visited.put(neighbor, node))
        {
            queue.enqueueTransport(neighbor, delay);
        }
    }

    private void addNeighbor(final int node, final int neighbor, final HybridIntQueue queue, final BFSCache visited) {
        if (visited.put(neighbor, node))
        {
            queue.enqueue(neighbor);
        }
    }

    private boolean filterTeleports(final WorldPoint dest)
    {
        return Static.invoke(() ->
        {
            Client client = Static.getClient();
            WorldPoint local = client.getLocalPlayer().getWorldLocation();
            List<WorldPoint> path = SceneAPI.pathTo(local, dest);
            return path != null && path.size() < 20 && SceneAPI.isReachable(local, dest);
        });
    }

}

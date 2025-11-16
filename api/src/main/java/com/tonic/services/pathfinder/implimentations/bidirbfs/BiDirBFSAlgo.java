package com.tonic.services.pathfinder.implimentations.bidirbfs;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.SceneAPI;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.abstractions.IPathfinder;
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
import java.util.Collections;
import java.util.List;

/**
 * Bidirectional BFS pathfinder - searches from both start and goal simultaneously.
 * More efficient for long paths as it reduces the search space significantly.
 */
public class BiDirBFSAlgo implements IPathfinder
{
    private LocalCollisionMap localMap;
    @Getter
    private Teleport teleport;
    private WorldPoint targetWorldPoint;

    private int[] worldAreaPoints;
    private boolean inInstance = false;
    private int forwardTransportsUsed;
    private int backwardTransportsUsed;

    /**
     * Initializes the pathfinder with a target WorldPoint.
     *
     * @param target The destination WorldPoint to find a path to.
     */
    @Override
    public List<BiDirBFSStep> find(final WorldPoint target) {
        TransportLoader.refreshTransports();
        this.targetWorldPoint = target;
        return find();
    }

    /**
     * Initializes the pathfinder with target WorldAreas.
     * @param worldAreas The destination WorldAreas to find a path to the closest area.
     */
    @Override
    public List<BiDirBFSStep> find(WorldArea... worldAreas)
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
    public List<BiDirBFSStep> find(List<WorldArea> worldAreas)
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
    private List<BiDirBFSStep> find() {
        if(Walker.getCollisionMap() == null)
        {
            Logger.error("[BiDirBFS] Collision map is null, cannot perform pathfinding.");
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

            Profiler.Start("BiDirBFS Pathfinding");

            final List<BiDirBFSStep> path = buildPath(startPoints);

            Profiler.StopMS();
            Logger.info("[BiDirBFS] Path Length: " + path.size());

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
            Logger.error(e, "[BiDirBFS] %e");
            return null;
        }
    }

    private List<BiDirBFSStep> buildPath(final List<Integer> starts)
    {
        final BiDirBFSCache forwardVisited = new BiDirBFSCache();
        final BiDirBFSCache backwardVisited = new BiDirBFSCache();

        //blacklist
        for(int i : Properties.getBlacklist())
        {
            forwardVisited.put(i, -1);
            backwardVisited.put(i, -1);
        }

        final HybridIntQueue forwardQueue = new HybridIntQueue(10_000_000);
        final HybridIntQueue backwardQueue = new HybridIntQueue(10_000_000);

        // Initialize forward search from starts
        for(final int wp : starts)
        {
            forwardVisited.put(wp, -1);
            forwardQueue.enqueue(wp);
        }

        if(targetWorldPoint != null)
            return findWorldPointBidirectional(forwardVisited, backwardVisited, forwardQueue, backwardQueue);
        if(worldAreaPoints != null && worldAreaPoints.length > 0)
            return findAreaPointBidirectional(forwardVisited, backwardVisited, forwardQueue, backwardQueue);
        return new ArrayList<>();
    }

    private List<BiDirBFSStep> findAreaPointBidirectional(final BiDirBFSCache forwardVisited, final BiDirBFSCache backwardVisited,
                                                           final HybridIntQueue forwardQueue, final HybridIntQueue backwardQueue) {
        // Initialize backward search from all goal area points
        for(int goalPoint : worldAreaPoints)
        {
            if(Walker.getCollisionMap().walkable(goalPoint))
            {
                backwardVisited.put(goalPoint, -1);
                backwardQueue.enqueue(goalPoint);
            }
        }

        int meetingPoint = -1;

        while(!forwardQueue.isEmpty() && !backwardQueue.isEmpty())
        {
            if(forwardVisited.size() + backwardVisited.size() > 10_000_000)
            {
                Logger.info("[BiDirBFS] Search limit reached");
                return new ArrayList<>();
            }

            // Expand forward frontier
            if(!forwardQueue.isEmpty())
            {
                int current = forwardQueue.dequeue();

                // Check if backward search reached this node (get returns 0 if not present)
                if(backwardVisited.get(current) != 0)
                {
                    meetingPoint = current;
                    break;
                }

                addNeighbors(current, forwardQueue, forwardVisited, true);
            }

            // Expand backward frontier
            if(!backwardQueue.isEmpty())
            {
                int current = backwardQueue.dequeue();

                // Check if forward search reached this node (get returns 0 if not present)
                if(forwardVisited.get(current) != 0)
                {
                    meetingPoint = current;
                    break;
                }

                addNeighbors(current, backwardQueue, backwardVisited, false);
            }
        }

        if(meetingPoint != -1)
        {
            //Logger.info("[BiDirBFS] Nodes visited: " + (forwardVisited.size() + backwardVisited.size()) + " (meeting at " + meetingPoint + ")");
            return reconstructPath(forwardVisited, backwardVisited, meetingPoint);
        }

        return new ArrayList<>();
    }

    private List<BiDirBFSStep> findWorldPointBidirectional(final BiDirBFSCache forwardVisited, final BiDirBFSCache backwardVisited,
                                                            final HybridIntQueue forwardQueue, final HybridIntQueue backwardQueue) {
        final int targetIndex = WorldPointUtil.compress(targetWorldPoint);

        //validate target
        if(!Walker.getCollisionMap().walkable(targetIndex)) {
            Logger.info("[BiDirBFS] Could not generate path to a blocked tile");
            return new ArrayList<>();
        }

        // Initialize backward search from goal
        backwardVisited.put(targetIndex, -1);
        backwardQueue.enqueue(targetIndex);

        int meetingPoint = -1;

        while(!forwardQueue.isEmpty() && !backwardQueue.isEmpty())
        {
            if(forwardVisited.size() + backwardVisited.size() > 10_000_000)
            {
                Logger.info("[BiDirBFS] Search limit reached");
                return new ArrayList<>();
            }

            // Expand forward frontier
            if(!forwardQueue.isEmpty())
            {
                int current = forwardQueue.dequeue();

                // Check if we reached the goal directly
                if(current == targetIndex)
                {
                    Logger.info("[BiDirBFS] Nodes visited (forward only): " + forwardVisited.size());
                    return forwardVisited.path(current);
                }

                // Check if backward search reached this node (get returns 0 if not present)
                if(backwardVisited.get(current) != 0)
                {
                    meetingPoint = current;
                    break;
                }

                addNeighbors(current, forwardQueue, forwardVisited, true);
            }

            // Expand backward frontier
            if(!backwardQueue.isEmpty())
            {
                int current = backwardQueue.dequeue();

                // Check if forward search reached this node (get returns 0 if not present)
                if(forwardVisited.get(current) != 0)
                {
                    meetingPoint = current;
                    break;
                }

                addNeighbors(current, backwardQueue, backwardVisited, false);
            }
        }

        if(meetingPoint != -1)
        {
            //Logger.info("[BiDirBFS] Nodes visited: " + (forwardVisited.size() + backwardVisited.size()) + " (meeting at " + meetingPoint + ")");
            return reconstructPath(forwardVisited, backwardVisited, meetingPoint);
        }

        return new ArrayList<>();
    }

    /**
     * Reconstructs the complete path by combining forward and backward search paths.
     */
    private List<BiDirBFSStep> reconstructPath(BiDirBFSCache forwardVisited, BiDirBFSCache backwardVisited, int meetingPoint)
    {
        // Get forward path (from start to meeting point)
        List<BiDirBFSStep> forwardPath = forwardVisited.path(meetingPoint);

        // Get backward path (from meeting point to goal)
        List<BiDirBFSStep> backwardPath = backwardVisited.path(meetingPoint);

        // Reverse backward path since it was built from goal to meeting point
        Collections.reverse(backwardPath);

        // Fix transports in reversed backward path - they now point in wrong direction
        reverseTransports(backwardPath);

        // Remove the duplicate meeting point from one of the paths
        if(!backwardPath.isEmpty())
        {
            backwardPath.remove(0);
        }

        // Combine paths
        forwardPath.addAll(backwardPath);

        return forwardPath;
    }

    /**
     * Reverses transports in a reversed path so they point in the correct direction.
     * After reversing a path, transports point backward - this fixes them to point forward.
     */
    private void reverseTransports(List<BiDirBFSStep> path)
    {
        // For each step with a transport, find its reverse
        for (int i = 0; i < path.size(); i++)
        {
            BiDirBFSStep step = path.get(i);
            Transport currentTransport = step.getTransport();

            if (currentTransport != null)
            {
                // Current transport goes from step position to some destination
                // We need the reverse: from destination back to step position
                int currentPos = step.getPackedPosition();
                int transportDest = currentTransport.getDestination();

                // Look at destination for transport going back to source
                Transport reverseTransport = findTransport(transportDest, currentPos);

                // Replace step with reversed transport
                path.set(i, new BiDirBFSStep(currentPos, reverseTransport));
            }
        }
    }

    /**
     * Finds a transport from source position to destination position.
     */
    private Transport findTransport(int source, int destination)
    {
        ArrayList<Transport> transports = TransportLoader.getTransports().get(source);
        if (transports != null)
        {
            for (Transport t : transports)
            {
                if (t.getDestination() == destination)
                {
                    return t;
                }
            }
        }
        return null;
    }

    private void addNeighborsLocal(final int node, final HybridIntQueue queue, final BiDirBFSCache visited, boolean isForward)
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

    private void addNeighbors(final int node, final HybridIntQueue queue, final BiDirBFSCache visited, boolean isForward) {
        final short x = WorldPointUtil.getCompressedX(node);
        final short y = WorldPointUtil.getCompressedY(node);
        final byte plane = WorldPointUtil.getCompressedPlane(node);

        if(x > 6000)
        {
            if(inInstance)
                addNeighborsLocal(node, queue, visited, isForward);
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
                checkTransports(node, queue, visited, isForward);
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

        checkTransports(node, queue, visited, isForward);
    }

    private void checkTransports(final int node, final HybridIntQueue queue, final BiDirBFSCache visited, boolean isForward)
    {
        final ArrayList<Transport> tr = TransportLoader.getTransports().get(node);
        if(tr != null)
        {
            for (Transport t : tr) {
                if(isForward)
                {
                    forwardTransportsUsed++;
                    addTransportNeighbor(node, t.getDestination(), calculateDelay(t.getDuration() * 2, queue.size(), forwardTransportsUsed), queue, visited);
                }
                else
                {
                    backwardTransportsUsed++;
                    addTransportNeighbor(node, t.getDestination(), calculateDelay(t.getDuration() * 2, queue.size(), backwardTransportsUsed), queue, visited);
                }
            }
        }
    }

    private int calculateDelay(int transportDelay, int queueSize, int transportsUsed) {
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

    private void addTransportNeighbor(final int node, final int neighbor, final int delay, final HybridIntQueue queue, final BiDirBFSCache visited) {
        if (visited.put(neighbor, node))
        {
            queue.enqueueTransport(neighbor, delay);
        }
    }

    private void addNeighbor(final int node, final int neighbor, final HybridIntQueue queue, final BiDirBFSCache visited) {
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

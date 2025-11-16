package com.tonic.services.pathfinder.implimentations.astar;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.SceneAPI;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.abstractions.IPathfinder;
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
 * Bidirectional A* pathfinding.
 * Searches from both start and goal simultaneously for improved performance.
 */
public class AStarAlgo implements IPathfinder
{
    private static final int MAX_NODES = 10_000_000;

    private LocalCollisionMap localMap;
    @Getter
    private Teleport teleport;

    // Target state cached as primitives
    private int targetCompressed;
    private short targetX;
    private short targetY;
    private byte targetPlane;
    private int[] worldAreaPoints;

    // Start state for backward heuristic
    private short startX;
    private short startY;
    private byte startPlane;

    private boolean inInstance = false;
    private int forwardTransportsUsed;
    private int backwardTransportsUsed;
    private int playerStartPos;

    @Override
    public List<AStarStep> find(WorldPoint target) {
        TransportLoader.refreshTransports();
        this.targetCompressed = WorldPointUtil.compress(target);
        this.targetX = (short) target.getX();
        this.targetY = (short) target.getY();
        this.targetPlane = (byte) target.getPlane();
        this.worldAreaPoints = null;
        return find();
    }

    @Override
    public List<AStarStep> find(WorldArea... worldAreas) {
        TransportLoader.refreshTransports();
        this.targetCompressed = -1;
        this.worldAreaPoints = WorldPointUtil.toCompressedPoints(worldAreas);

        // Use first area point as heuristic target approximation
        if (worldAreaPoints != null && worldAreaPoints.length > 0) {
            int firstPoint = worldAreaPoints[0];
            this.targetX = WorldPointUtil.getCompressedX(firstPoint);
            this.targetY = WorldPointUtil.getCompressedY(firstPoint);
            this.targetPlane = WorldPointUtil.getCompressedPlane(firstPoint);
        }

        return find();
    }

    @Override
    public List<AStarStep> find(List<WorldArea> worldAreas) {
        TransportLoader.refreshTransports();
        this.targetCompressed = -1;
        this.worldAreaPoints = WorldPointUtil.toCompressedPoints(worldAreas.toArray(new WorldArea[0]));

        // Use first area point as heuristic target approximation
        if (worldAreaPoints != null && worldAreaPoints.length > 0) {
            int firstPoint = worldAreaPoints[0];
            this.targetX = WorldPointUtil.getCompressedX(firstPoint);
            this.targetY = WorldPointUtil.getCompressedY(firstPoint);
            this.targetPlane = WorldPointUtil.getCompressedPlane(firstPoint);
        }

        return find();
    }

    private List<AStarStep> find() {
        if (Walker.getCollisionMap() == null) {
            Logger.error("[A*] Collision map is null");
            return new ArrayList<>();
        }

        try {
            Client client = Static.getClient();
            this.inInstance = client.getTopLevelWorldView().isInstance();
            this.forwardTransportsUsed = 0;
            this.backwardTransportsUsed = 0;

            if (inInstance) {
                localMap = new LocalCollisionMap();
            }

            playerStartPos = WorldPointUtil.compress(client.getLocalPlayer().getWorldLocation());
            this.startX = WorldPointUtil.getCompressedX(playerStartPos);
            this.startY = WorldPointUtil.getCompressedY(playerStartPos);
            this.startPlane = WorldPointUtil.getCompressedPlane(playerStartPos);

            List<Teleport> teleports = Teleport.buildTeleportLinks();
            List<Integer> startPoints = new ArrayList<>();
            startPoints.add(playerStartPos);

            for (Teleport tp : teleports) {
                if (!filterTeleports(tp.getDestination())) {
                    startPoints.add(WorldPointUtil.compress(tp.getDestination()));
                }
            }

            Profiler.Start("Bidirectional A* Pathfinding");
            List<AStarStep> path = buildPath(startPoints);
            Profiler.StopMS();

            Logger.info("[BiDir A*] Path Length: " + path.size());

            if (path.isEmpty())
                return path;

            // Set teleport if path starts with one
            for (Teleport tp : teleports) {
                if (WorldPointUtil.compress(tp.getDestination()) == path.get(0).getPackedPosition()) {
                    teleport = tp.copy();
                    break;
                }
            }

            return path;

        } catch (Exception e) {
            Logger.error(e, "[BiDir A*] %e");
            return new ArrayList<>();
        }
    }

    private List<AStarStep> buildPath(List<Integer> starts) {
        AStarCache forwardCache = new AStarCache(10_000);
        AStarCache backwardCache = new AStarCache(10_000);
        AStarPriorityQueue forwardOpenSet = new AStarPriorityQueue(10_000);
        AStarPriorityQueue backwardOpenSet = new AStarPriorityQueue(10_000);
        gnu.trove.set.hash.TIntHashSet forwardClosedSet = new gnu.trove.set.hash.TIntHashSet(10_000);
        gnu.trove.set.hash.TIntHashSet backwardClosedSet = new gnu.trove.set.hash.TIntHashSet(10_000);

        // Blacklist
        for (int i : Properties.getBlacklist()) {
            forwardCache.putIfBetter(i, Integer.MAX_VALUE - 1, -1);
            backwardCache.putIfBetter(i, Integer.MAX_VALUE - 1, -1);
            forwardClosedSet.add(i);
            backwardClosedSet.add(i);
        }

        // Initialize forward search (from starts)
        for (int start : starts) {
            forwardCache.putIfBetter(start, 0, -1);
            int h = calculateHeuristic(start, targetX, targetY, targetPlane, worldAreaPoints, targetCompressed);
            forwardOpenSet.enqueue(start, h);
        }

        // Initialize backward search (from target)
        if (targetCompressed != -1) {
            backwardCache.putIfBetter(targetCompressed, 0, -1);
            int h = calculateHeuristic(targetCompressed, startX, startY, startPlane, null, playerStartPos);
            backwardOpenSet.enqueue(targetCompressed, h);
        } else if (worldAreaPoints != null && worldAreaPoints.length > 0) {
            // For area targets, initialize backward from all area points
            for (int areaPoint : worldAreaPoints) {
                backwardCache.putIfBetter(areaPoint, 0, -1);
                int h = calculateHeuristic(areaPoint, startX, startY, startPlane, null, playerStartPos);
                backwardOpenSet.enqueue(areaPoint, h);
            }
        }

        if (targetCompressed != -1)
            return findWorldPointBidirectional(forwardCache, backwardCache, forwardOpenSet, backwardOpenSet, forwardClosedSet, backwardClosedSet);
        if (worldAreaPoints != null && worldAreaPoints.length > 0)
            return findAreaPointBidirectional(forwardCache, backwardCache, forwardOpenSet, backwardOpenSet, forwardClosedSet, backwardClosedSet);

        return new ArrayList<>();
    }

    private int calculateHeuristic(int pos, short targetX, short targetY, byte targetPlane, int[] areaPoints, int targetCompressed) {
        short sx = WorldPointUtil.getCompressedX(pos);
        short sy = WorldPointUtil.getCompressedY(pos);
        byte sp = WorldPointUtil.getCompressedPlane(pos);

        if (targetCompressed != -1 || areaPoints == null) {
            int dx = sx > targetX ? sx - targetX : targetX - sx;
            int dy = sy > targetY ? sy - targetY : targetY - sy;
            int dz = sp > targetPlane ? sp - targetPlane : targetPlane - sp;
            return dx + dy + (dz * 100);
        } else {
            // Area heuristic - find minimum distance to any area point
            int minDist = Integer.MAX_VALUE;
            for (int areaPoint : areaPoints) {
                short ax = WorldPointUtil.getCompressedX(areaPoint);
                short ay = WorldPointUtil.getCompressedY(areaPoint);
                byte ap = WorldPointUtil.getCompressedPlane(areaPoint);

                int dx = sx > ax ? sx - ax : ax - sx;
                int dy = sy > ay ? sy - ay : ay - sy;
                int dz = sp > ap ? sp - ap : ap - sp;

                int dist = dx + dy + (dz * 100);
                if (dist < minDist) {
                    minDist = dist;
                }
            }
            return minDist;
        }
    }

    private List<AStarStep> findWorldPointBidirectional(AStarCache forwardCache, AStarCache backwardCache,
                                                         AStarPriorityQueue forwardOpenSet, AStarPriorityQueue backwardOpenSet,
                                                         gnu.trove.set.hash.TIntHashSet forwardClosedSet, gnu.trove.set.hash.TIntHashSet backwardClosedSet) {
        if (!Walker.getCollisionMap().walkable(targetCompressed)) {
            Logger.info("[BiDir A*] Target blocked");
            return new ArrayList<>();
        }

        int nodesExplored = 0;
        int meetingPoint = -1;

        while (!forwardOpenSet.isEmpty() && !backwardOpenSet.isEmpty()) {
            if (forwardCache.size() + backwardCache.size() > MAX_NODES) {
                return new ArrayList<>();
            }

            // Expand forward
            if (!forwardOpenSet.isEmpty()) {
                int current = forwardOpenSet.dequeue();
                if (!forwardClosedSet.contains(current)) {
                    nodesExplored++;

                    // Check if backward search reached this node
                    if (backwardCache.contains(current)) {
                        meetingPoint = current;
                        break;
                    }

                    forwardClosedSet.add(current);
                    expandNodeForward(current, forwardCache, forwardOpenSet, forwardClosedSet);
                }
            }

            // Expand backward
            if (!backwardOpenSet.isEmpty() && meetingPoint == -1) {
                int current = backwardOpenSet.dequeue();
                if (!backwardClosedSet.contains(current)) {
                    nodesExplored++;

                    // Check if forward search reached this node
                    if (forwardCache.contains(current)) {
                        meetingPoint = current;
                        break;
                    }

                    backwardClosedSet.add(current);
                    expandNodeBackward(current, backwardCache, backwardOpenSet, backwardClosedSet);
                }
            }
        }

        if (meetingPoint != -1) {
            Logger.info("[BiDir A*] Nodes: " + nodesExplored + ", Meeting: " + meetingPoint);
            return reconstructBidirectionalPath(forwardCache, backwardCache, meetingPoint);
        }

        return new ArrayList<>();
    }

    private List<AStarStep> findAreaPointBidirectional(AStarCache forwardCache, AStarCache backwardCache,
                                                        AStarPriorityQueue forwardOpenSet, AStarPriorityQueue backwardOpenSet,
                                                        gnu.trove.set.hash.TIntHashSet forwardClosedSet, gnu.trove.set.hash.TIntHashSet backwardClosedSet) {
        int nodesExplored = 0;
        int meetingPoint = -1;

        while (!forwardOpenSet.isEmpty() && !backwardOpenSet.isEmpty()) {
            if (forwardCache.size() + backwardCache.size() > MAX_NODES) {
                return new ArrayList<>();
            }

            // Expand forward
            if (!forwardOpenSet.isEmpty()) {
                int current = forwardOpenSet.dequeue();
                if (!forwardClosedSet.contains(current)) {
                    nodesExplored++;

                    // Check if backward search reached this node
                    if (backwardCache.contains(current)) {
                        meetingPoint = current;
                        break;
                    }

                    forwardClosedSet.add(current);
                    expandNodeForward(current, forwardCache, forwardOpenSet, forwardClosedSet);
                }
            }

            // Expand backward
            if (!backwardOpenSet.isEmpty() && meetingPoint == -1) {
                int current = backwardOpenSet.dequeue();
                if (!backwardClosedSet.contains(current)) {
                    nodesExplored++;

                    // Check if forward search reached this node
                    if (forwardCache.contains(current)) {
                        meetingPoint = current;
                        break;
                    }

                    backwardClosedSet.add(current);
                    expandNodeBackward(current, backwardCache, backwardOpenSet, backwardClosedSet);
                }
            }
        }

        if (meetingPoint != -1) {
            Logger.info("[BiDir A*] Nodes: " + nodesExplored + ", Meeting: " + meetingPoint);
            return reconstructBidirectionalPath(forwardCache, backwardCache, meetingPoint);
        }

        return new ArrayList<>();
    }

    private List<AStarStep> reconstructBidirectionalPath(AStarCache forwardCache, AStarCache backwardCache, int meetingPoint) {
        // Get forward path (from start to meeting point)
        List<AStarStep> forwardPath = forwardCache.reconstructPartialPath(meetingPoint);

        // Get backward path (from meeting point to goal)
        List<AStarStep> backwardPath = backwardCache.reconstructPartialPath(meetingPoint);

        // Reverse backward path since it was built from goal to meeting point
        java.util.Collections.reverse(backwardPath);

        // Fix transports in reversed backward path - they now point in wrong direction
        reverseTransports(backwardPath);

        // Remove the duplicate meeting point from one of the paths
        if (!backwardPath.isEmpty()) {
            backwardPath.remove(0);
        }

        // Remove player's starting position if present
        if (!forwardPath.isEmpty() && forwardPath.get(0).getPackedPosition() == playerStartPos) {
            forwardPath.remove(0);
        }

        // Combine paths
        forwardPath.addAll(backwardPath);

        return forwardPath;
    }

    /**
     * Reverses transports in a reversed path so they point in the correct direction.
     * After reversing a path, transports point backward - this fixes them to point forward.
     *
     * In A*, transports are stored as "how we arrived at this position".
     * After reversing, we need to shift transports to represent "transport to reach next position".
     */
    private void reverseTransports(List<AStarStep> path) {
        if (path.size() <= 1) {
            return;
        }

        // Build new path with corrected transports
        // Transport at position X means "use this transport to reach next position"
        List<AStarStep> corrected = new ArrayList<>(path.size());

        // For each position except last, find transport to next position
        for (int i = 0; i < path.size() - 1; i++) {
            int currentPos = path.get(i).getPackedPosition();
            int nextPos = path.get(i + 1).getPackedPosition();

            // Find transport from current position to next position
            Transport forwardTransport = findTransport(currentPos, nextPos);

            corrected.add(new AStarStep(currentPos, forwardTransport));
        }

        // Last position has no transport (destination)
        corrected.add(new AStarStep(path.get(path.size() - 1).getPackedPosition(), null));

        // Replace path contents
        path.clear();
        path.addAll(corrected);
    }

    /**
     * Finds a transport from source position to destination position.
     */
    private Transport findTransport(int source, int destination) {
        ArrayList<Transport> transports = TransportLoader.getTransports().get(source);
        if (transports != null) {
            for (Transport t : transports) {
                if (t.getDestination() == destination) {
                    return t;
                }
            }
        }
        return null;
    }

    private void expandNodeForward(int current, AStarCache cache, AStarPriorityQueue openSet, gnu.trove.set.hash.TIntHashSet closedSet) {
        int currentG = cache.getGScore(current);
        int tentativeG = currentG + 1;

        short x = WorldPointUtil.getCompressedX(current);
        short y = WorldPointUtil.getCompressedY(current);
        byte plane = WorldPointUtil.getCompressedPlane(current);

        if (x > 6000) {
            if (inInstance) {
                expandLocal(current, currentG, x, y, plane, cache, openSet, closedSet);
            }
            return;
        }

        byte flags = Walker.getCollisionMap().all(x, y, plane);

        switch (flags) {
            case Flags.ALL:
                // West
                int neighbor = WorldPointUtil.compress(x - 1, y, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x - 1 > targetX ? x - 1 - targetX : targetX - x + 1;
                    int dy = y > targetY ? y - targetY : targetY - y;
                    int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                // East
                neighbor = WorldPointUtil.compress(x + 1, y, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x + 1 > targetX ? x + 1 - targetX : targetX - x - 1;
                    int dy = y > targetY ? y - targetY : targetY - y;
                    int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                // South
                neighbor = WorldPointUtil.compress(x, y - 1, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x > targetX ? x - targetX : targetX - x;
                    int dy = y - 1 > targetY ? y - 1 - targetY : targetY - y + 1;
                    int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                // North
                neighbor = WorldPointUtil.compress(x, y + 1, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x > targetX ? x - targetX : targetX - x;
                    int dy = y + 1 > targetY ? y + 1 - targetY : targetY - y - 1;
                    int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                // Southwest
                neighbor = WorldPointUtil.compress(x - 1, y - 1, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x - 1 > targetX ? x - 1 - targetX : targetX - x + 1;
                    int dy = y - 1 > targetY ? y - 1 - targetY : targetY - y + 1;
                    int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                // Southeast
                neighbor = WorldPointUtil.compress(x + 1, y - 1, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x + 1 > targetX ? x + 1 - targetX : targetX - x - 1;
                    int dy = y - 1 > targetY ? y - 1 - targetY : targetY - y + 1;
                    int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                // Northwest
                neighbor = WorldPointUtil.compress(x - 1, y + 1, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x - 1 > targetX ? x - 1 - targetX : targetX - x + 1;
                    int dy = y + 1 > targetY ? y + 1 - targetY : targetY - y - 1;
                    int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                // Northeast
                neighbor = WorldPointUtil.compress(x + 1, y + 1, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x + 1 > targetX ? x + 1 - targetX : targetX - x - 1;
                    int dy = y + 1 > targetY ? y + 1 - targetY : targetY - y - 1;
                    int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                checkTransportsForward(current, currentG, cache, openSet);
                return;
            case Flags.NONE:
                return;
        }

        // Bitwise checks
        if ((flags & Flags.WEST) != 0) {
            int neighbor = WorldPointUtil.compress(x - 1, y, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x - 1 > targetX ? x - 1 - targetX : targetX - x + 1;
                int dy = y > targetY ? y - targetY : targetY - y;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if ((flags & Flags.EAST) != 0) {
            int neighbor = WorldPointUtil.compress(x + 1, y, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x + 1 > targetX ? x + 1 - targetX : targetX - x - 1;
                int dy = y > targetY ? y - targetY : targetY - y;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if ((flags & Flags.SOUTH) != 0) {
            int neighbor = WorldPointUtil.compress(x, y - 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x > targetX ? x - targetX : targetX - x;
                int dy = y - 1 > targetY ? y - 1 - targetY : targetY - y + 1;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if ((flags & Flags.NORTH) != 0) {
            int neighbor = WorldPointUtil.compress(x, y + 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x > targetX ? x - targetX : targetX - x;
                int dy = y + 1 > targetY ? y + 1 - targetY : targetY - y - 1;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if ((flags & Flags.SOUTHWEST) != 0) {
            int neighbor = WorldPointUtil.compress(x - 1, y - 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x - 1 > targetX ? x - 1 - targetX : targetX - x + 1;
                int dy = y - 1 > targetY ? y - 1 - targetY : targetY - y + 1;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if ((flags & Flags.SOUTHEAST) != 0) {
            int neighbor = WorldPointUtil.compress(x + 1, y - 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x + 1 > targetX ? x + 1 - targetX : targetX - x - 1;
                int dy = y - 1 > targetY ? y - 1 - targetY : targetY - y + 1;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if ((flags & Flags.NORTHWEST) != 0) {
            int neighbor = WorldPointUtil.compress(x - 1, y + 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x - 1 > targetX ? x - 1 - targetX : targetX - x + 1;
                int dy = y + 1 > targetY ? y + 1 - targetY : targetY - y - 1;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if ((flags & Flags.NORTHEAST) != 0) {
            int neighbor = WorldPointUtil.compress(x + 1, y + 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x + 1 > targetX ? x + 1 - targetX : targetX - x - 1;
                int dy = y + 1 > targetY ? y + 1 - targetY : targetY - y - 1;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        checkTransportsForward(current, currentG, cache, openSet);
    }

    private void expandNodeBackward(int current, AStarCache cache, AStarPriorityQueue openSet, gnu.trove.set.hash.TIntHashSet closedSet) {
        int currentG = cache.getGScore(current);
        int tentativeG = currentG + 1;

        short x = WorldPointUtil.getCompressedX(current);
        short y = WorldPointUtil.getCompressedY(current);
        byte plane = WorldPointUtil.getCompressedPlane(current);

        if (x > 6000) {
            if (inInstance) {
                expandLocalBackward(current, currentG, x, y, plane, cache, openSet, closedSet);
            }
            return;
        }

        byte flags = Walker.getCollisionMap().all(x, y, plane);

        // Same neighbor expansion logic but with startX, startY, startPlane heuristic
        switch (flags) {
            case Flags.ALL:
                // West
                int neighbor = WorldPointUtil.compress(x - 1, y, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x - 1 > startX ? x - 1 - startX : startX - x + 1;
                    int dy = y > startY ? y - startY : startY - y;
                    int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                // East
                neighbor = WorldPointUtil.compress(x + 1, y, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x + 1 > startX ? x + 1 - startX : startX - x - 1;
                    int dy = y > startY ? y - startY : startY - y;
                    int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                // South
                neighbor = WorldPointUtil.compress(x, y - 1, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x > startX ? x - startX : startX - x;
                    int dy = y - 1 > startY ? y - 1 - startY : startY - y + 1;
                    int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                // North
                neighbor = WorldPointUtil.compress(x, y + 1, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x > startX ? x - startX : startX - x;
                    int dy = y + 1 > startY ? y + 1 - startY : startY - y - 1;
                    int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                // Southwest
                neighbor = WorldPointUtil.compress(x - 1, y - 1, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x - 1 > startX ? x - 1 - startX : startX - x + 1;
                    int dy = y - 1 > startY ? y - 1 - startY : startY - y + 1;
                    int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                // Southeast
                neighbor = WorldPointUtil.compress(x + 1, y - 1, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x + 1 > startX ? x + 1 - startX : startX - x - 1;
                    int dy = y - 1 > startY ? y - 1 - startY : startY - y + 1;
                    int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                // Northwest
                neighbor = WorldPointUtil.compress(x - 1, y + 1, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x - 1 > startX ? x - 1 - startX : startX - x + 1;
                    int dy = y + 1 > startY ? y + 1 - startY : startY - y - 1;
                    int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                // Northeast
                neighbor = WorldPointUtil.compress(x + 1, y + 1, plane);
                if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                    int dx = x + 1 > startX ? x + 1 - startX : startX - x - 1;
                    int dy = y + 1 > startY ? y + 1 - startY : startY - y - 1;
                    int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                    openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
                }
                checkTransportsBackward(current, currentG, cache, openSet);
                return;
            case Flags.NONE:
                return;
        }

        // Bitwise checks (same as forward but with startX, startY, startPlane)
        if ((flags & Flags.WEST) != 0) {
            int neighbor = WorldPointUtil.compress(x - 1, y, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x - 1 > startX ? x - 1 - startX : startX - x + 1;
                int dy = y > startY ? y - startY : startY - y;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if ((flags & Flags.EAST) != 0) {
            int neighbor = WorldPointUtil.compress(x + 1, y, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x + 1 > startX ? x + 1 - startX : startX - x - 1;
                int dy = y > startY ? y - startY : startY - y;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if ((flags & Flags.SOUTH) != 0) {
            int neighbor = WorldPointUtil.compress(x, y - 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x > startX ? x - startX : startX - x;
                int dy = y - 1 > startY ? y - 1 - startY : startY - y + 1;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if ((flags & Flags.NORTH) != 0) {
            int neighbor = WorldPointUtil.compress(x, y + 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x > startX ? x - startX : startX - x;
                int dy = y + 1 > startY ? y + 1 - startY : startY - y - 1;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if ((flags & Flags.SOUTHWEST) != 0) {
            int neighbor = WorldPointUtil.compress(x - 1, y - 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x - 1 > startX ? x - 1 - startX : startX - x + 1;
                int dy = y - 1 > startY ? y - 1 - startY : startY - y + 1;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if ((flags & Flags.SOUTHEAST) != 0) {
            int neighbor = WorldPointUtil.compress(x + 1, y - 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x + 1 > startX ? x + 1 - startX : startX - x - 1;
                int dy = y - 1 > startY ? y - 1 - startY : startY - y + 1;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if ((flags & Flags.NORTHWEST) != 0) {
            int neighbor = WorldPointUtil.compress(x - 1, y + 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x - 1 > startX ? x - 1 - startX : startX - x + 1;
                int dy = y + 1 > startY ? y + 1 - startY : startY - y - 1;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if ((flags & Flags.NORTHEAST) != 0) {
            int neighbor = WorldPointUtil.compress(x + 1, y + 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x + 1 > startX ? x + 1 - startX : startX - x - 1;
                int dy = y + 1 > startY ? y + 1 - startY : startY - y - 1;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        checkTransportsBackward(current, currentG, cache, openSet);
    }

    private void expandLocal(int current, int currentG, short x, short y, byte plane, AStarCache cache, AStarPriorityQueue openSet, gnu.trove.set.hash.TIntHashSet closedSet) {
        int tentativeG = currentG + 1;

        if (!localMap.w(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x - 1, y, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x - 1 > targetX ? x - 1 - targetX : targetX - x + 1;
                int dy = y > targetY ? y - targetY : targetY - y;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if (!localMap.e(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x + 1, y, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x + 1 > targetX ? x + 1 - targetX : targetX - x - 1;
                int dy = y > targetY ? y - targetY : targetY - y;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if (!localMap.n(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x, y + 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x > targetX ? x - targetX : targetX - x;
                int dy = y + 1 > targetY ? y + 1 - targetY : targetY - y - 1;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if (!localMap.s(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x, y - 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x > targetX ? x - targetX : targetX - x;
                int dy = y - 1 > targetY ? y - 1 - targetY : targetY - y + 1;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if (!localMap.nw(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x - 1, y + 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x - 1 > targetX ? x - 1 - targetX : targetX - x + 1;
                int dy = y + 1 > targetY ? y + 1 - targetY : targetY - y - 1;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if (!localMap.ne(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x + 1, y + 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x + 1 > targetX ? x + 1 - targetX : targetX - x - 1;
                int dy = y + 1 > targetY ? y + 1 - targetY : targetY - y - 1;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if (!localMap.sw(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x - 1, y - 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x - 1 > targetX ? x - 1 - targetX : targetX - x + 1;
                int dy = y - 1 > targetY ? y - 1 - targetY : targetY - y + 1;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if (!localMap.se(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x + 1, y - 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x + 1 > targetX ? x + 1 - targetX : targetX - x - 1;
                int dy = y - 1 > targetY ? y - 1 - targetY : targetY - y + 1;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }
    }

    private void checkTransportsForward(int current, int currentG, AStarCache cache, AStarPriorityQueue openSet) {
        ArrayList<Transport> transports = TransportLoader.getTransports().get(current);
        if (transports == null) return;

        for (int i = 0; i < transports.size(); i++) {
            Transport t = transports.get(i);
            forwardTransportsUsed++;

            int duration = t.getDuration();
            int cost = duration + 1;

            int dest = t.getDestination();
            int tentativeG = currentG + cost;

            // Note: Not checking closedSet for transports to allow re-exploration with better cost
            if (cache.putIfBetter(dest, tentativeG, current, t)) {
                short dx = WorldPointUtil.getCompressedX(dest);
                short dy = WorldPointUtil.getCompressedY(dest);
                byte dp = WorldPointUtil.getCompressedPlane(dest);

                int hx = dx > targetX ? dx - targetX : targetX - dx;
                int hy = dy > targetY ? dy - targetY : targetY - dy;
                int hz = dp > targetPlane ? dp - targetPlane : targetPlane - dp;

                openSet.enqueue(dest, tentativeG + hx + hy + (hz * 100));
            }
        }
    }

    private void checkTransportsBackward(int current, int currentG, AStarCache cache, AStarPriorityQueue openSet) {
        ArrayList<Transport> transports = TransportLoader.getTransports().get(current);
        if (transports == null) return;

        for (int i = 0; i < transports.size(); i++) {
            Transport t = transports.get(i);
            backwardTransportsUsed++;

            int duration = t.getDuration();
            int cost = duration + 1;

            int dest = t.getDestination();
            int tentativeG = currentG + cost;

            // Note: Not checking closedSet for transports to allow re-exploration with better cost
            if (cache.putIfBetter(dest, tentativeG, current, t)) {
                short dx = WorldPointUtil.getCompressedX(dest);
                short dy = WorldPointUtil.getCompressedY(dest);
                byte dp = WorldPointUtil.getCompressedPlane(dest);

                int hx = dx > startX ? dx - startX : startX - dx;
                int hy = dy > startY ? dy - startY : startY - dy;
                int hz = dp > startPlane ? dp - startPlane : startPlane - dp;

                openSet.enqueue(dest, tentativeG + hx + hy + (hz * 100));
            }
        }
    }

    private void expandLocalBackward(int current, int currentG, short x, short y, byte plane, AStarCache cache, AStarPriorityQueue openSet, gnu.trove.set.hash.TIntHashSet closedSet) {
        int tentativeG = currentG + 1;

        if (!localMap.w(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x - 1, y, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x - 1 > startX ? x - 1 - startX : startX - x + 1;
                int dy = y > startY ? y - startY : startY - y;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if (!localMap.e(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x + 1, y, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x + 1 > startX ? x + 1 - startX : startX - x - 1;
                int dy = y > startY ? y - startY : startY - y;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if (!localMap.n(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x, y + 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x > startX ? x - startX : startX - x;
                int dy = y + 1 > startY ? y + 1 - startY : startY - y - 1;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if (!localMap.s(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x, y - 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x > startX ? x - startX : startX - x;
                int dy = y - 1 > startY ? y - 1 - startY : startY - y + 1;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if (!localMap.nw(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x - 1, y + 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x - 1 > startX ? x - 1 - startX : startX - x + 1;
                int dy = y + 1 > startY ? y + 1 - startY : startY - y - 1;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if (!localMap.ne(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x + 1, y + 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x + 1 > startX ? x + 1 - startX : startX - x - 1;
                int dy = y + 1 > startY ? y + 1 - startY : startY - y - 1;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if (!localMap.sw(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x - 1, y - 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x - 1 > startX ? x - 1 - startX : startX - x + 1;
                int dy = y - 1 > startY ? y - 1 - startY : startY - y + 1;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }

        if (!localMap.se(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x + 1, y - 1, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x + 1 > startX ? x + 1 - startX : startX - x - 1;
                int dy = y - 1 > startY ? y - 1 - startY : startY - y + 1;
                int dz = plane > startPlane ? plane - startPlane : startPlane - plane;
                openSet.enqueue(neighbor, tentativeG + dx + dy + (dz * 100));
            }
        }
    }

    private boolean filterTeleports(WorldPoint dest) {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            WorldPoint local = client.getLocalPlayer().getWorldLocation();
            List<WorldPoint> path = SceneAPI.pathTo(local, dest);
            return path != null && path.size() < 20 && SceneAPI.isReachable(local, dest);
        });
    }
}

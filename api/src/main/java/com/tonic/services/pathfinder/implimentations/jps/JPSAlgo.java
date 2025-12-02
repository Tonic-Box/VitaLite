package com.tonic.services.pathfinder.implimentations.jps;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.abstractions.IPathfinder;
import com.tonic.services.pathfinder.collision.CollisionMap;
import com.tonic.services.pathfinder.collision.Flags;
import com.tonic.services.pathfinder.teleports.Teleport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportLoader;
import com.tonic.util.Profiler;
import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Canonical JPS pathfinder with Transport Graph Augmentation.
 * Performs jump point identification on-the-fly during search (no preprocessing).
 */
public class JPSAlgo implements IPathfinder
{
    @Getter
    private Teleport teleport;

    // Target state cached as primitives
    private int targetCompressed;
    private short targetX;
    private short targetY;
    private byte targetPlane;
    private int[] worldAreaPoints;
    private int playerStartPos;

    // Cached collision map to avoid repeated method calls
    private CollisionMap collisionMap;

    public JPSAlgo() {
    }

    @Override
    public List<JPSStep> find(WorldPoint target) {
        TransportLoader.refreshTransports();
        this.targetCompressed = WorldPointUtil.compress(target);
        this.targetX = (short) target.getX();
        this.targetY = (short) target.getY();
        this.targetPlane = (byte) target.getPlane();
        this.worldAreaPoints = null;
        return find();
    }

    @Override
    public List<JPSStep> find(WorldArea... worldAreas) {
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
    public List<JPSStep> find(List<WorldArea> worldAreas) {
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

    private List<JPSStep> find() {
        collisionMap = Walker.getCollisionMap();
        if (collisionMap == null) {
            Logger.error("[Canonical JPS] Collision map is null");
            return new ArrayList<>();
        }

        try {
            playerStartPos = WorldPointUtil.compress(PlayerEx.getLocal().getWorldPoint());

            List<Teleport> teleports = Teleport.buildTeleportLinks();
            List<Integer> startPoints = new ArrayList<>();
            startPoints.add(playerStartPos);

            for (Teleport tp : teleports) {
                startPoints.add(WorldPointUtil.compress(tp.getDestination()));
            }

            Profiler.Start("Canonical JPS Pathfinding");
            List<JPSStep> path = buildPath(startPoints);
            Profiler.StopMS();

            Logger.info("[Canonical JPS] Path Length: " + path.size());

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
            Logger.error(e, "[Canonical JPS] %e");
            return new ArrayList<>();
        }
    }

    private List<JPSStep> buildPath(List<Integer> starts) {
        JPSCache cache = new JPSCache(200_000);
        JPSPriorityQueue openSet = new JPSPriorityQueue(200_000);
        TIntHashSet closedSet = new TIntHashSet(200_000);

        // Initialize start nodes
        for (int start : starts) {
            cache.putIfBetter(start, 0, -1);
            int h = heuristic(start);
            openSet.enqueue(start, h);
        }

        while (!openSet.isEmpty()) {
            int current = openSet.dequeue();

            // Skip if already processed
            if (!closedSet.add(current)) {
                continue;
            }

            // Goal reached
            if (isGoal(current)) {
                return cache.reconstructPath(current, playerStartPos);
            }

            int currentG = cache.getGScore(current);

            // Expand jump point successors (inline - no allocations)
            expandJumpSuccessors(current, currentG, cache, openSet, closedSet);

            // Expand transport edges
            expandTransports(current, currentG, cache, openSet, closedSet);
        }

        // No path found
        return new ArrayList<>();
    }

    /**
     * Checks if position is the goal.
     */
    private boolean isGoal(int position) {
        if (targetCompressed != -1) {
            return position == targetCompressed;
        }
        if (worldAreaPoints != null) {
            for (int areaPoint : worldAreaPoints) {
                if (position == areaPoint) return true;
            }
        }
        return false;
    }

    /**
     * Expands all jump point successors using fully inlined collision flag checks.
     * Matches A* pattern for maximum performance.
     */
    private void expandJumpSuccessors(int current, int currentG, JPSCache cache,
                                      JPSPriorityQueue openSet, TIntHashSet closedSet) {
        // Decompress coordinates once
        short x = WorldPointUtil.getCompressedX(current);
        short y = WorldPointUtil.getCompressedY(current);
        byte plane = WorldPointUtil.getCompressedPlane(current);

        // Get all collision flags at once (using cached collision map)
        byte flags = collisionMap.all(x, y, plane);

        int tentativeG = currentG + 1;

        // Cardinal directions - bitwise flag checks
        if ((flags & Flags.NORTH) != 0) {
            int neighbor = WorldPointUtil.compress(x, (short)(y + 1), plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                // Inline heuristic calculation
                int dx = x > targetX ? x - targetX : targetX - x;
                int dy = (y + 1) > targetY ? (y + 1) - targetY : targetY - (y + 1);
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + Math.max(dx, dy) + (dz * 100));
            }
        }

        if ((flags & Flags.SOUTH) != 0) {
            int neighbor = WorldPointUtil.compress(x, (short)(y - 1), plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = x > targetX ? x - targetX : targetX - x;
                int dy = (y - 1) > targetY ? (y - 1) - targetY : targetY - (y - 1);
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + Math.max(dx, dy) + (dz * 100));
            }
        }

        if ((flags & Flags.EAST) != 0) {
            int neighbor = WorldPointUtil.compress((short)(x + 1), y, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = (x + 1) > targetX ? (x + 1) - targetX : targetX - (x + 1);
                int dy = y > targetY ? y - targetY : targetY - y;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + Math.max(dx, dy) + (dz * 100));
            }
        }

        if ((flags & Flags.WEST) != 0) {
            int neighbor = WorldPointUtil.compress((short)(x - 1), y, plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = (x - 1) > targetX ? (x - 1) - targetX : targetX - (x - 1);
                int dy = y > targetY ? y - targetY : targetY - y;
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + Math.max(dx, dy) + (dz * 100));
            }
        }

        // Diagonal directions
        if ((flags & Flags.NORTHEAST) != 0) {
            int neighbor = WorldPointUtil.compress((short)(x + 1), (short)(y + 1), plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = (x + 1) > targetX ? (x + 1) - targetX : targetX - (x + 1);
                int dy = (y + 1) > targetY ? (y + 1) - targetY : targetY - (y + 1);
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + Math.max(dx, dy) + (dz * 100));
            }
        }

        if ((flags & Flags.NORTHWEST) != 0) {
            int neighbor = WorldPointUtil.compress((short)(x - 1), (short)(y + 1), plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = (x - 1) > targetX ? (x - 1) - targetX : targetX - (x - 1);
                int dy = (y + 1) > targetY ? (y + 1) - targetY : targetY - (y + 1);
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + Math.max(dx, dy) + (dz * 100));
            }
        }

        if ((flags & Flags.SOUTHEAST) != 0) {
            int neighbor = WorldPointUtil.compress((short)(x + 1), (short)(y - 1), plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = (x + 1) > targetX ? (x + 1) - targetX : targetX - (x + 1);
                int dy = (y - 1) > targetY ? (y - 1) - targetY : targetY - (y - 1);
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + Math.max(dx, dy) + (dz * 100));
            }
        }

        if ((flags & Flags.SOUTHWEST) != 0) {
            int neighbor = WorldPointUtil.compress((short)(x - 1), (short)(y - 1), plane);
            if (!closedSet.contains(neighbor) && cache.putIfBetter(neighbor, tentativeG, current)) {
                int dx = (x - 1) > targetX ? (x - 1) - targetX : targetX - (x - 1);
                int dy = (y - 1) > targetY ? (y - 1) - targetY : targetY - (y - 1);
                int dz = plane > targetPlane ? plane - targetPlane : targetPlane - plane;
                openSet.enqueue(neighbor, tentativeG + Math.max(dx, dy) + (dz * 100));
            }
        }
    }

    /**
     * Expands all transport edges from current node.
     * Uses index-based iteration to avoid iterator allocation.
     */
    private void expandTransports(int current, int currentG, JPSCache cache,
                                  JPSPriorityQueue openSet, TIntHashSet closedSet) {
        ArrayList<Transport> transports = TransportLoader.getTransports().get(current);
        if (transports == null) return;

        // Index-based iteration for performance (no iterator allocation)
        for (int i = 0, size = transports.size(); i < size; i++) {
            Transport transport = transports.get(i);
            int destination = transport.getDestination();

            // Skip if already processed
            if (closedSet.contains(destination)) continue;

            // Transport cost = duration + 1, inline g-score calculation
            int tentativeG = currentG + transport.getDuration() + 1;

            // Update if better path, inline heuristic
            if (cache.putIfBetter(destination, tentativeG, current, transport)) {
                openSet.enqueue(destination, tentativeG + heuristic(destination));
            }
        }
    }

    /**
     * Octile distance heuristic (supports diagonal movement).
     */
    private int heuristic(int from) {
        short sx = WorldPointUtil.getCompressedX(from);
        short sy = WorldPointUtil.getCompressedY(from);
        byte sp = WorldPointUtil.getCompressedPlane(from);

        if (targetCompressed != -1 || worldAreaPoints == null) {
            int dx = sx > targetX ? sx - targetX : targetX - sx;
            int dy = sy > targetY ? sy - targetY : targetY - sy;
            int dz = sp > targetPlane ? sp - targetPlane : targetPlane - sp;
            return Math.max(dx, dy) + (dz * 100);
        } else {
            // Area heuristic - find minimum distance to any area point
            int minDist = Integer.MAX_VALUE;
            for (int areaPoint : worldAreaPoints) {
                short ax = WorldPointUtil.getCompressedX(areaPoint);
                short ay = WorldPointUtil.getCompressedY(areaPoint);
                byte ap = WorldPointUtil.getCompressedPlane(areaPoint);

                int dx = sx > ax ? sx - ax : ax - sx;
                int dy = sy > ay ? sy - ay : ay - sy;
                int dz = sp > ap ? sp - ap : ap - sp;

                int dist = Math.max(dx, dy) + (dz * 100);
                if (dist < minDist) {
                    minDist = dist;
                }
            }
            return minDist;
        }
    }

}

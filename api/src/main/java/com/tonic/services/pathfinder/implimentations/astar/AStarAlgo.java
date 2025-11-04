package com.tonic.services.pathfinder.implimentations.astar;

import com.tonic.Logger;
import com.tonic.Static;
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
 * Ultra-optimized A* pathfinding.
 * Zero allocations in hot path, all primitives, inline operations.
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

    private boolean inInstance = false;
    private int transportsUsed;
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
            this.transportsUsed = 0;

            if (inInstance) {
                localMap = new LocalCollisionMap();
            }

            playerStartPos = WorldPointUtil.compress(client.getLocalPlayer().getWorldLocation());

            List<Teleport> teleports = Teleport.buildTeleportLinks();
            List<Integer> startPoints = new ArrayList<>();
            startPoints.add(playerStartPos);

            for (Teleport tp : teleports) {
                if (!filterTeleports(tp.getDestination())) {
                    startPoints.add(WorldPointUtil.compress(tp.getDestination()));
                }
            }

            Profiler.Start("A* Pathfinding");
            List<AStarStep> path = buildPath(startPoints);
            Profiler.StopMS();

            Logger.info("[A*] Path Length: " + path.size());

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
            Logger.error(e, "[A*] %e");
            return new ArrayList<>();
        }
    }

    private List<AStarStep> buildPath(List<Integer> starts) {
        AStarCache cache = new AStarCache(10_000);
        AStarPriorityQueue openSet = new AStarPriorityQueue(10_000);
        gnu.trove.set.hash.TIntHashSet closedSet = new gnu.trove.set.hash.TIntHashSet(10_000);

        // Blacklist
        for (int i : Properties.getBlacklist()) {
            cache.putIfBetter(i, Integer.MAX_VALUE - 1, -1);
            closedSet.add(i);
        }

        // Initialize starts
        for (int start : starts) {
            cache.putIfBetter(start, 0, -1);

            // Inline heuristic calculation
            int h;
            if (targetCompressed != -1) {
                short sx = WorldPointUtil.getCompressedX(start);
                short sy = WorldPointUtil.getCompressedY(start);
                byte sp = WorldPointUtil.getCompressedPlane(start);

                int dx = sx > targetX ? sx - targetX : targetX - sx;
                int dy = sy > targetY ? sy - targetY : targetY - sy;
                int dz = sp > targetPlane ? sp - targetPlane : targetPlane - sp;
                h = dx + dy + (dz * 100);
            } else {
                // Area heuristic
                int minDist = Integer.MAX_VALUE;
                short sx = WorldPointUtil.getCompressedX(start);
                short sy = WorldPointUtil.getCompressedY(start);
                byte sp = WorldPointUtil.getCompressedPlane(start);

                for (int areaPoint : worldAreaPoints) {
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
                h = minDist;
            }

            openSet.enqueue(start, h);
        }

        if (targetCompressed != -1)
            return findWorldPoint(cache, openSet, closedSet);
        if (worldAreaPoints != null && worldAreaPoints.length > 0)
            return findAreaPoint(cache, openSet, closedSet);

        return new ArrayList<>();
    }

    private List<AStarStep> findWorldPoint(AStarCache cache, AStarPriorityQueue openSet, gnu.trove.set.hash.TIntHashSet closedSet) {
        if (!Walker.getCollisionMap().walkable(targetCompressed)) {
            Logger.info("[A*] Target blocked");
            return new ArrayList<>();
        }

        int nodesExplored = 0;

        while (!openSet.isEmpty()) {
            if (cache.size() > MAX_NODES) {
                return new ArrayList<>();
            }

            int current = openSet.dequeue();

            // Skip if already expanded
            if (closedSet.contains(current)) continue;

            nodesExplored++;

            if (current == targetCompressed) {
                Logger.info("[A*] Nodes: " + nodesExplored);
                return cache.reconstructPath(current, playerStartPos);
            }

            closedSet.add(current);
            expandNode(current, cache, openSet, closedSet);
        }

        return new ArrayList<>();
    }

    private List<AStarStep> findAreaPoint(AStarCache cache, AStarPriorityQueue openSet, gnu.trove.set.hash.TIntHashSet closedSet) {
        int nodesExplored = 0;

        while (!openSet.isEmpty()) {
            if (cache.size() > MAX_NODES) {
                return new ArrayList<>();
            }

            int current = openSet.dequeue();

            // Skip if already expanded
            if (closedSet.contains(current)) continue;

            nodesExplored++;

            if (ArrayUtils.contains(worldAreaPoints, current)) {
                Logger.info("[A*] Nodes: " + nodesExplored);
                return cache.reconstructPath(current, playerStartPos);
            }

            closedSet.add(current);
            expandNode(current, cache, openSet, closedSet);
        }

        return new ArrayList<>();
    }

    private void expandNode(int current, AStarCache cache, AStarPriorityQueue openSet, gnu.trove.set.hash.TIntHashSet closedSet) {
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
                checkTransports(current, currentG, cache, openSet);
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

        checkTransports(current, currentG, cache, openSet);
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

    private void checkTransports(int current, int currentG, AStarCache cache, AStarPriorityQueue openSet) {
        ArrayList<Transport> transports = TransportLoader.getTransports().get(current);
        if (transports == null) return;

        for (int i = 0; i < transports.size(); i++) {
            Transport t = transports.get(i);
            transportsUsed++;

            int duration = t.getDuration();
            int cost;
            if (duration <= 0) {
                cost = 2;
            } else {
                int incrementValue = 6 * (1 + transportsUsed);
                int part1 = openSet.size() * duration;
                int part2 = incrementValue * (duration * (duration + 1) >> 1);
                int calculated = part1 + part2;
                cost = calculated < 0 ? Integer.MAX_VALUE : (calculated < 1 ? 1 : calculated);
            }

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

    private boolean filterTeleports(WorldPoint dest) {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            WorldPoint local = client.getLocalPlayer().getWorldLocation();
            List<Tile> path = Location.pathTo(local, dest);
            return path != null && path.size() < 20 && Location.isReachable(local, dest);
        });
    }
}

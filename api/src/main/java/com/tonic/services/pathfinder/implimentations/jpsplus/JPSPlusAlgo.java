package com.tonic.services.pathfinder.implimentations.jpsplus;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.abstractions.IPathfinder;
import com.tonic.services.pathfinder.teleports.Teleport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportLoader;
import com.tonic.util.Profiler;
import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * JPS+ pathfinder with Transport Graph Augmentation.
 * Combines preprocessed jump point graph with dynamic transport edges.
 */
public class JPSPlusAlgo implements IPathfinder
{
    private final JPSPlusPreprocessor preprocessor;
    @Getter
    private Teleport teleport;

    // Direction constants matching preprocessor
    private static final int[][] DIRECTIONS = {
        {0, 1},   // N
        {0, -1},  // S
        {1, 0},   // E
        {-1, 0},  // W
        {1, 1},   // NE
        {-1, 1},  // NW
        {1, -1},  // SE
        {-1, -1}  // SW
    };

    // Target state cached as primitives
    private int targetCompressed;
    private short targetX;
    private short targetY;
    private byte targetPlane;
    private int[] worldAreaPoints;
    private int playerStartPos;

    public JPSPlusAlgo() {
        this.preprocessor = new JPSPlusPreprocessor();
    }

    @Override
    public List<JPSPlusStep> find(WorldPoint target) {
        TransportLoader.refreshTransports();
        this.targetCompressed = WorldPointUtil.compress(target);
        this.targetX = (short) target.getX();
        this.targetY = (short) target.getY();
        this.targetPlane = (byte) target.getPlane();
        this.worldAreaPoints = null;
        return find();
    }

    @Override
    public List<JPSPlusStep> find(WorldArea... worldAreas) {
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
    public List<JPSPlusStep> find(List<WorldArea> worldAreas) {
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

    private List<JPSPlusStep> find() {
        if (Walker.getCollisionMap() == null) {
            Logger.error("[JPS+] Collision map is null");
            return new ArrayList<>();
        }

        try {
            Client client = Static.getClient();
            playerStartPos = WorldPointUtil.compress(client.getLocalPlayer().getWorldLocation());

            List<Teleport> teleports = Teleport.buildTeleportLinks();
            List<Integer> startPoints = new ArrayList<>();
            startPoints.add(playerStartPos);

            for (Teleport tp : teleports) {
                startPoints.add(WorldPointUtil.compress(tp.getDestination()));
            }

            Profiler.Start("JPS+ Pathfinding");
            List<JPSPlusStep> path = buildPath(startPoints);
            Profiler.StopMS();

            Logger.info("[JPS+] Path Length: " + path.size());

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
            Logger.error(e, "[JPS+] %e");
            return new ArrayList<>();
        }
    }

    private List<JPSPlusStep> buildPath(List<Integer> starts) {
        JPSPlusCache cache = new JPSPlusCache(10_000);
        PriorityQueue<JPSPlusNode> openSet = new PriorityQueue<>((a, b) ->
            Integer.compare(a.fScore, b.fScore));

        // Initialize start nodes
        for (int start : starts) {
            cache.putIfBetter(start, 0, -1);
            int h = heuristic(start);
            openSet.add(new JPSPlusNode(start, 0, h, -1, null));
        }

        while (!openSet.isEmpty()) {
            JPSPlusNode current = openSet.poll();

            // Skip if we've found a better path to this node
            if (current.gScore > cache.getGScore(current.position)) {
                continue;
            }

            // Goal reached
            if (isGoal(current.position)) {
                return cache.reconstructPath(current.position, playerStartPos);
            }

            // Expand jump point successors
            expandJumpSuccessors(current, cache, openSet);

            // Expand transport edges
            expandTransports(current, cache, openSet);
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
     * Expands all jump point successors from current node.
     */
    private void expandJumpSuccessors(JPSPlusNode current, JPSPlusCache cache, PriorityQueue<JPSPlusNode> openSet) {
        int[] successors = preprocessor.getJumpSuccessors(current.position);

        for (int dir = 0; dir < 8; dir++) {
            int distance = successors[dir];
            if (distance == -1) continue; // No jump point in this direction

            // Compute successor position
            int dx = DIRECTIONS[dir][0];
            int dy = DIRECTIONS[dir][1];
            int successor = getPositionAt(current.position, dx * distance, dy * distance);

            if (successor == -1) continue;

            // Calculate costs
            int moveCost = distance; // Each tile = 1 cost
            int tentativeG = current.gScore + moveCost;

            // Update if better path
            if (cache.putIfBetter(successor, tentativeG, current.position)) {
                int h = heuristic(successor);
                openSet.add(new JPSPlusNode(successor, tentativeG, h, current.position, null));
            }
        }
    }

    /**
     * Expands all transport edges from current node.
     */
    private void expandTransports(JPSPlusNode current, JPSPlusCache cache, PriorityQueue<JPSPlusNode> openSet) {
        ArrayList<Transport> transports = TransportLoader.getTransports().get(current.position);
        if (transports == null) return;

        for (int i = 0; i < transports.size(); i++) {
            Transport transport = transports.get(i);
            int destination = transport.getDestination();
            int duration = transport.getDuration();

            // Transport cost = duration + 1
            int cost = duration + 1;
            int tentativeG = current.gScore + cost;

            // Update if better path
            if (cache.putIfBetter(destination, tentativeG, current.position, transport)) {
                int h = heuristic(destination);
                openSet.add(new JPSPlusNode(destination, tentativeG, h, current.position, transport));
            }
        }
    }

    /**
     * Gets position at offset from current position.
     */
    private int getPositionAt(int position, int dx, int dy) {
        short x = WorldPointUtil.getCompressedX(position);
        short y = WorldPointUtil.getCompressedY(position);
        byte plane = WorldPointUtil.getCompressedPlane(position);

        return WorldPointUtil.compress((short)(x + dx), (short)(y + dy), plane);
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

    /**
     * Internal node class for priority queue.
     */
    private static class JPSPlusNode {
        final int position;
        final int gScore;
        final int fScore;
        final int parent;
        final Transport transport;

        JPSPlusNode(int position, int gScore, int hScore, int parent, Transport transport) {
            this.position = position;
            this.gScore = gScore;
            this.fScore = gScore + hScore;
            this.parent = parent;
            this.transport = transport;
        }
    }

    /**
     * Clears preprocessed data (call if collision map changes).
     */
    public void clearPreprocessing() {
        preprocessor.clear();
    }
}

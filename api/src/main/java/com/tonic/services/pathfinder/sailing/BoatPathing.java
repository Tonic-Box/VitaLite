package com.tonic.services.pathfinder.sailing;

import com.tonic.Static;
import com.tonic.api.game.sailing.Heading;
import com.tonic.api.game.sailing.SailingAPI;
import com.tonic.api.handlers.GenericHandlerBuilder;
import com.tonic.services.GameManager;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.collision.CollisionMap;
import com.tonic.util.Distance;
import com.tonic.util.Profiler;
import com.tonic.util.WorldPointUtil;
import com.tonic.util.handler.StepHandler;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.Getter;
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

/**
 * Dijkstra-based boat pathfinding with proximity-weighted costs.
 * Generates tile-by-tile paths preferring 6-7 tile clearance from obstacles,
 * naturally centers in corridors, and falls back to tighter paths when needed.
 *
 * ALGORITHM: Weighted Dijkstra with cost = baseCost × proximityMultiplier
 * - Base costs: 10 (orthogonal), 14 (diagonal ≈ √2 × 10)
 * - Proximity multipliers: 50× at 1 tile, 25× at 2, 12× at 3, 6× at 4, 3× at 5, 1× at 6+
 * - Natural centering: equidistant from walls = lowest combined cost
 *
 * OPTIMIZATION: Highly optimized hot path with:
 * - Primitive min-heap (no PriorityQueue boxing)
 * - Direction-to-heading lookup table (eliminates trig operations)
 * - Pre-computed hull offsets (eliminates API calls)
 * - Primitive int maps (eliminates boxing/unboxing)
 * - Direction indices (eliminates coordinate math)
 * - Proximity cache (avoids re-scanning same tiles)
 * - Early termination spiral scan (cardinals first)
 */
public class BoatPathing
{
    // Direction indices for 8 neighbors
    private static final int DIR_WEST = 0;
    private static final int DIR_EAST = 1;
    private static final int DIR_SOUTH = 2;
    private static final int DIR_NORTH = 3;
    private static final int DIR_SOUTHWEST = 4;
    private static final int DIR_SOUTHEAST = 5;
    private static final int DIR_NORTHWEST = 6;
    private static final int DIR_NORTHEAST = 7;

    // Direction offsets (matching DIR_* constants order)
    private static final int[] DX = {-1, 1, 0, 0, -1, 1, -1, 1};
    private static final int[] DY = {0, 0, -1, 1, -1, -1, 1, 1};

    // Base movement costs (10 for orthogonal, 14 for diagonal ≈ √2 × 10)
    private static final int[] BASE_COSTS = {10, 10, 10, 10, 14, 14, 14, 14};

    // Proximity costs (index = distance to nearest collision)
    // 0=blocked, 1-5=penalized, 6-7=ideal buffer
    private static final int[] PROXIMITY_COSTS = {
            Integer.MAX_VALUE,  // 0: blocked (hull collision handled separately)
            50,                 // 1: very close - emergency only
            25,                 // 2: close - avoid if possible
            12,                 // 3: moderate penalty
            6,                  // 4: slight penalty
            3,                  // 5: minor penalty
            1,                  // 6: ideal - base cost
            1,                  // 7: ideal - base cost
            1                   // 8+: open water
    };

    // Maximum radius to scan for proximity calculation
    private static final int MAX_PROXIMITY_SCAN = 8;

    public static StepHandler travelTo(WorldPoint worldPoint)
    {
        return GenericHandlerBuilder.get()
                .addDelayUntil(context -> {
                    if(!context.contains("PATH"))
                    {
                        WorldEntity boat = BoatCollisionAPI.getPlayerBoat();
                        WorldPoint start = BoatCollisionAPI.getPlayerBoatWorldPoint();
                        List<WorldPoint> fullPath = findFullPath(start, worldPoint);
                        if(fullPath == null || fullPath.isEmpty())
                        {
                            System.out.println("BoatPathing: No path found to " + worldPoint);
                            return true;
                        }
                        GameManager.setPathPoints(fullPath);
                        List<Waypoint> waypoints = convertToWaypoints(fullPath);
                        context.put("PATH", waypoints);
                        context.put("POINTER", 0);
                        context.put("LAST_HEADING", null);
                    }
                    List<Waypoint> waypoints = context.get("PATH");
                    Waypoint first = waypoints.get(1);
                    WorldPoint start = BoatCollisionAPI.getPlayerBoatWorldPoint();
                    Heading heading = Heading.getOptimalHeading(start, first.getPosition());

                    boolean headingInitSet = context.getOrDefault("HEADING_INIT_SET", false);
                    if(headingInitSet)
                    {
                        int dif = heading.getValue() - SailingAPI.getHeading().getValue();
                        if(dif <= 2 && dif >= -2)
                        {
                            SailingAPI.setSails();
                            return true;
                        }
                        if (SailingAPI.isMovingForward()) {
                            SailingAPI.unSetSails();
                        }
                        return false;
                    }
                    SailingAPI.setHeading(heading);
                    context.put("HEADING_INIT_SET", true);
                    return false;
                })
                .addDelayUntil(context -> {
                    if(!context.contains("PATH"))
                    {
                        return true;
                    }
                    List<Waypoint> waypoints = context.get("PATH");
                    int pointer = context.get("POINTER");

                    if(waypoints == null || waypoints.isEmpty() || pointer >= waypoints.size())
                    {
                        context.remove("PATH");
                        context.remove("POINTER");
                        SailingAPI.unSetSails();
                        GameManager.clearPathPoints();
                        return true;
                    }

                    Waypoint waypoint = waypoints.get(pointer);
                    Waypoint end = waypoints.get(waypoints.size() - 1);

                    if(BoatCollisionAPI.playerBoatContainsPoint(end.getPosition()))
                    {
                        context.remove("PATH");
                        context.remove("POINTER");
                        SailingAPI.unSetSails();
                        GameManager.clearPathPoints();
                        return true;
                    }

                    WorldPoint start = BoatCollisionAPI.getPlayerBoatWorldPoint();

                    if((end != waypoint && Distance.chebyshev(start, waypoint.getPosition()) <= 4))
                    {
                        context.put("POINTER", pointer + 1);
                        return false;
                    }
                    if(SailingAPI.trimSails())
                    {
                        return false;
                    }
                    Heading optimalHeading = Heading.getOptimalHeading(waypoint.getPosition());
                    Heading lastHeading = context.get("LAST_HEADING");
                    if(optimalHeading != lastHeading)
                    {
                        SailingAPI.sailTo(waypoint.getPosition());
                        context.put("LAST_HEADING", optimalHeading);
                    }
                    return false;
                })
                .build();
    }

    public static StepHandler travelTo(List<Waypoint> path)
    {
        return GenericHandlerBuilder.get()
                .addDelayUntil(context -> {
                    if(!context.contains("PATH"))
                    {
                        WorldPoint start = BoatCollisionAPI.getPlayerBoatWorldPoint();
                        if(Distance.chebyshev(start, path.get(0).getPosition()) > 10)
                        {
                            System.out.println("BoatPathing: Start too far from first waypoint");
                            return true;
                        }
                        context.put("PATH", path);
                        context.put("POINTER", 0);
                        context.put("LAST_HEADING", null);
                    }
                    List<Waypoint> waypoints = context.get("PATH");
                    Waypoint first = waypoints.get(1);
                    WorldPoint start = BoatCollisionAPI.getPlayerBoatWorldPoint();
                    Heading heading = Heading.getOptimalHeading(start, first.getPosition());

                    boolean headingInitSet = context.getOrDefault("HEADING_INIT_SET", false);
                    if(headingInitSet)
                    {
                        int dif = heading.getValue() - SailingAPI.getHeading().getValue();
                        if(dif <= 2 && dif >= -2)
                        {
                            SailingAPI.setSails();
                            return true;
                        }
                        if (SailingAPI.isMovingForward()) {
                            SailingAPI.unSetSails();
                        }
                        return false;
                    }
                    SailingAPI.setHeading(heading);
                    context.put("HEADING_INIT_SET", true);
                    return false;
                })
                .addDelayUntil(context -> {
                    if(!context.contains("PATH"))
                    {
                        return true;
                    }
                    List<Waypoint> waypoints = context.get("PATH");
                    int pointer = context.get("POINTER");

                    if(waypoints == null || waypoints.isEmpty() || pointer >= waypoints.size())
                    {
                        context.remove("PATH");
                        context.remove("POINTER");
                        SailingAPI.unSetSails();
                        GameManager.clearPathPoints();
                        return true;
                    }

                    Waypoint waypoint = waypoints.get(pointer);
                    Waypoint end = waypoints.get(waypoints.size() - 1);

                    if(BoatCollisionAPI.playerBoatContainsPoint(end.getPosition()))
                    {
                        context.remove("PATH");
                        context.remove("POINTER");
                        SailingAPI.unSetSails();
                        GameManager.clearPathPoints();
                        return true;
                    }

                    WorldPoint start = BoatCollisionAPI.getPlayerBoatWorldPoint();

                    if((end != waypoint && Distance.chebyshev(start, waypoint.getPosition()) <= 4))
                    {
                        context.put("POINTER", pointer + 1);
                        return false;
                    }
                    if(SailingAPI.trimSails())
                    {
                        return false;
                    }
                    Heading optimalHeading = Heading.getOptimalHeading(waypoint.getPosition());
                    Heading lastHeading = context.get("LAST_HEADING");
                    if(optimalHeading != lastHeading)
                    {
                        SailingAPI.sailTo(waypoint.getPosition());
                        context.put("LAST_HEADING", optimalHeading);
                    }
                    return false;
                })
                .build();
    }

    @Getter
    public static class Waypoint
    {
        private final WorldPoint position;
        private final Heading heading;

        public Waypoint(WorldPoint position, Heading heading)
        {
            this.position = position;
            this.heading = heading;
        }

        @Override
        public String toString()
        {
            return "Waypoint{" + position + ", heading=" + heading + "}";
        }
    }

    /**
     * Finds a sailing path from start to target, returns waypoints at turning points.
     */
    public static List<Waypoint> pathTo(WorldPoint target)
    {
        return Static.invoke(() -> {
            WorldEntity boat = BoatCollisionAPI.getPlayerBoat();
            if (boat == null) {
                System.out.println("SailPathing: No boat found");
                return null;
            }

            WorldPoint start = BoatCollisionAPI.getPlayerBoatWorldPoint();
            if (start == null) {
                System.out.println("SailPathing: No start position");
                return null;
            }

            return pathTo(start, target);
        });
    }

    /**
     * Finds a sailing path from start to target for the given boat.
     */
    public static List<Waypoint> pathTo(WorldPoint start, WorldPoint target)
    {
        return Static.invoke(() -> {
            // Find full tile-by-tile path using BFS
            List<WorldPoint> fullPath = findFullPath(start, target);

            if (fullPath == null || fullPath.isEmpty()) {
                System.out.println("SailPathing: No path found from " + start + " to " + target);
                return null;
            }

            System.out.println("SailPathing: Found full path with " + fullPath.size() + " tiles");

            // Convert to waypoints at turning points
            List<Waypoint> waypoints = convertToWaypoints(fullPath);
            System.out.println("SailPathing: Converted to " + waypoints.size() + " waypoints");

            return waypoints;
        });
    }

    /**
     * Initializes boat hull cache from current boat state.
     * Extracts hull as primitive offsets to avoid allocations in hot path.
     * NOTE: Gets boat fresh from client since WorldEntity doesn't survive Static.invoke boundary
     */
    private static BoatHullCache initializeBoatCache(WorldPoint boatCenter, CollisionMap collisionMap)
    {
        // Get player boat fresh (don't pass as parameter - doesn't survive Static.invoke)
        WorldEntity boat = BoatCollisionAPI.getPlayerBoat();
        if (boat == null) {
            System.out.println("SailPathing: No player boat found");
            return null;
        }

        // Get current hull in world coordinates - use player boat method
        Collection<WorldPoint> hull = BoatCollisionAPI.getPlayerBoatCollision();

        if (hull == null || hull.isEmpty()) {
            System.out.println("SailPathing: Empty boat hull (" + (hull == null ? "null" : hull.size()) + " tiles)");
            return null;
        }

        // Get current heading
        int currentHeading = SailingAPI.getHeadingValue();
        if (currentHeading == -1) {
            System.out.println("SailPathing: Not on boat (headingValue=-1)");
            return null;
        }

        // Convert hull to offsets from boat center
        int[] xOffsets = new int[hull.size()];
        int[] yOffsets = new int[hull.size()];
        int i = 0;
        for (WorldPoint hullTile : hull) {
            xOffsets[i] = hullTile.getX() - boatCenter.getX();
            yOffsets[i] = hullTile.getY() - boatCenter.getY();
            i++;
        }

        System.out.println("SailPathing: Initialized hull cache with " + hull.size() + " tiles, heading=" + currentHeading);
        return new BoatHullCache(xOffsets, yOffsets, currentHeading, collisionMap);
    }

    /**
     * Inlined collision check using pre-computed rotation matrices.
     * Eliminates API call overhead and object allocations.
     */
    private static boolean canBoatFitAtDirection(BoatHullCache cache, short targetX, short targetY, int directionIndex)
    {
        double cos = cache.directionCos[directionIndex];
        double sin = cache.directionSin[directionIndex];

        // Check each hull tile with pre-computed rotation
        for (int i = 0; i < cache.xOffsets.length; i++) {
            // Rotate offset
            int rotatedDx = (int) Math.round(cache.xOffsets[i] * cos - cache.yOffsets[i] * sin);
            int rotatedDy = (int) Math.round(cache.xOffsets[i] * sin + cache.yOffsets[i] * cos);

            // Calculate world position
            short worldX = (short) (targetX + rotatedDx);
            short worldY = (short) (targetY + rotatedDy);

            // Check collision on plane 0
            if (!cache.collisionMap.walkable(worldX, worldY, (byte) 0)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Uses Dijkstra with proximity costs to find optimal path from start to target.
     * Prefers 4-5 tile clearance from obstacles, naturally centers in corridors,
     * and falls back to tighter paths when necessary.
     *
     * OPTIMIZED: Primitive min-heap + primitive int maps + pre-computed rotations.
     */
    public static List<WorldPoint> findFullPath(WorldPoint start, WorldPoint target)
    {
        Profiler.Start("BoatPathing");
        List<WorldPoint> path = Static.invoke(() -> {
            CollisionMap collisionMap = Walker.getCollisionMap();
            if (collisionMap == null) {
                System.out.println("SailPathing: CollisionMap is null");
                return null;
            }

            // Initialize cache with hull offsets and pre-computed rotations
            // Don't pass boat entity - it doesn't survive Static.invoke boundary
            BoatHullCache cache = initializeBoatCache(start, collisionMap);
            if (cache == null) {
                System.out.println("SailPathing: Failed to initialize boat cache");
                return null;
            }

            // Dijkstra data structures - primitive arrays for heap
            int[] heapNodes = new int[100_000];
            int[] heapCosts = new int[100_000];
            int heapSize = 0;

            // Primitive maps for costs and parent tracking
            Int2IntOpenHashMap gScores = new Int2IntOpenHashMap();
            Int2IntOpenHashMap parents = new Int2IntOpenHashMap();
            Int2IntOpenHashMap proximityCache = new Int2IntOpenHashMap();
            gScores.defaultReturnValue(Integer.MAX_VALUE);
            parents.defaultReturnValue(-2);  // -2 = not visited
            proximityCache.defaultReturnValue(-1);  // -1 = not cached

            int startPacked = WorldPointUtil.compress(start);
            int targetPacked = WorldPointUtil.compress(target);

            // Initialize start node
            gScores.put(startPacked, 0);
            parents.put(startPacked, -1);  // -1 = start node marker
            heapSize = heapPush(heapNodes, heapCosts, heapSize, startPacked, 0);

            int maxIterations = 1_000_000;
            int iterations = 0;

            // Dijkstra search
            while (heapSize > 0 && iterations++ < maxIterations) {
                // Pop minimum cost node
                heapSize = heapPop(heapNodes, heapCosts, heapSize);
                int current = heapNodes[heapSize];
                int currentG = heapCosts[heapSize];

                // Skip if we've already found a better path to this node
                if (currentG > gScores.get(current)) {
                    continue;
                }

                // Check if reached target
                if (current == targetPacked) {
                    System.out.println("SailPathing: Found target after " + iterations + " iterations (Dijkstra with proximity)");
                    return reconstructFullPath(parents, targetPacked);
                }

                // Expand neighbors with cost-based evaluation
                heapSize = expandNeighborsWithCost(cache, collisionMap, current, currentG,
                        gScores, parents, proximityCache, heapNodes, heapCosts, heapSize);
            }

            System.out.println("SailPathing: No path found after " + iterations + " iterations");
            return null;
        });

        Profiler.StopMS();
        return path;
    }

    /**
     * Expands neighbors in 8 directions with proximity-weighted costs.
     * OPTIMIZED: Uses primitive arrays and maps, pre-computed rotations.
     * All operations use primitive ints - no object allocations in hot path.
     *
     * Cost formula: baseCost × proximityMultiplier
     * - Base costs: 10 (orthogonal), 14 (diagonal)
     * - Proximity multipliers: 50× at 1 tile, 20× at 2, 8× at 3, 1× at 4+
     *
     * This naturally centers the path between walls (equidistant = equal low costs)
     * and prefers 4-5 tile buffer from obstacles.
     *
     * @return new heap size
     */
    private static int expandNeighborsWithCost(
            BoatHullCache cache, CollisionMap collisionMap,
            int current, int currentG,
            Int2IntOpenHashMap gScores, Int2IntOpenHashMap parents,
            Int2IntOpenHashMap proximityCache,
            int[] heapNodes, int[] heapCosts, int heapSize)
    {
        // Extract as ints to avoid repeated casts
        int x = WorldPointUtil.getCompressedX(current);
        int y = WorldPointUtil.getCompressedY(current);
        int plane = WorldPointUtil.getCompressedPlane(current);

        // Expand in all 8 directions
        for (int dir = 0; dir < 8; dir++) {
            int nx = x + DX[dir];
            int ny = y + DY[dir];

            // Use int overload - no object allocation
            int neighborPacked = WorldPointUtil.compress(nx, ny, plane);

            // Skip if boat doesn't fit at this position/direction
            if (!canBoatFitAtDirection(cache, (short) nx, (short) ny, dir)) {
                continue;
            }

            // Calculate edge cost with proximity penalty
            int baseCost = BASE_COSTS[dir];
            int proximity = getProximityCachedInt(collisionMap, proximityCache, nx, ny, plane);
            int proximityCost = PROXIMITY_COSTS[Math.min(proximity, PROXIMITY_COSTS.length - 1)];

            // Avoid overflow: if proximityCost is MAX_VALUE, skip this tile
            if (proximityCost == Integer.MAX_VALUE) {
                continue;
            }

            int edgeCost = baseCost * proximityCost;
            int tentativeG = currentG + edgeCost;

            // Only update if this path is better
            if (tentativeG < gScores.get(neighborPacked)) {
                gScores.put(neighborPacked, tentativeG);
                parents.put(neighborPacked, current);
                heapSize = heapPush(heapNodes, heapCosts, heapSize, neighborPacked, tentativeG);
            }
        }

        return heapSize;
    }

    /**
     * Reconstructs the full tile-by-tile path from parents map.
     * OPTIMIZED: Uses primitive int map.
     */
    private static List<WorldPoint> reconstructFullPath(Int2IntOpenHashMap parents, int target)
    {
        List<WorldPoint> path = new ArrayList<>();
        int current = target;

        // Walk backwards from target to start
        while (current != -1) {
            short x = WorldPointUtil.getCompressedX(current);
            short y = WorldPointUtil.getCompressedY(current);
            byte plane = WorldPointUtil.getCompressedPlane(current);

            path.add(new WorldPoint(x, y, plane));

            int parent = parents.get(current);  // Primitive get, no boxing
            if (parent == -2 || parent == -1) {  // -2 = not found, -1 = start node
                break;
            }
            current = parent;
        }

        Collections.reverse(path);
        return path;
    }

    // ==================== Primitive Min-Heap Operations ====================

    /**
     * Pushes a node onto the min-heap.
     * @return new heap size
     */
    private static int heapPush(int[] nodes, int[] costs, int size, int node, int cost)
    {
        nodes[size] = node;
        costs[size] = cost;

        // Bubble up
        int i = size;
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (costs[i] < costs[parent]) {
                // Swap
                int tn = nodes[i]; nodes[i] = nodes[parent]; nodes[parent] = tn;
                int tc = costs[i]; costs[i] = costs[parent]; costs[parent] = tc;
                i = parent;
            } else {
                break;
            }
        }
        return size + 1;
    }

    /**
     * Pops the minimum node from the heap.
     * After calling, the popped node is at nodes[size] and cost at costs[size].
     * @return new heap size
     */
    private static int heapPop(int[] nodes, int[] costs, int size)
    {
        size--;
        // Move last element to root
        int poppedNode = nodes[0];
        int poppedCost = costs[0];
        nodes[0] = nodes[size];
        costs[0] = costs[size];
        // Store popped values at end for caller to retrieve
        nodes[size] = poppedNode;
        costs[size] = poppedCost;

        // Bubble down
        int i = 0;
        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int smallest = i;

            if (left < size && costs[left] < costs[smallest]) {
                smallest = left;
            }
            if (right < size && costs[right] < costs[smallest]) {
                smallest = right;
            }
            if (smallest == i) {
                break;
            }

            // Swap
            int tn = nodes[i]; nodes[i] = nodes[smallest]; nodes[smallest] = tn;
            int tc = costs[i]; costs[i] = costs[smallest]; costs[smallest] = tc;
            i = smallest;
        }
        return size;
    }

    // ==================== Proximity Calculation ====================

    /**
     * Gets the cached distance to nearest collision, computing if not cached.
     * OPTIMIZED: Uses primitive ints throughout - no object allocations.
     */
    private static int getProximityCachedInt(CollisionMap collisionMap, Int2IntOpenHashMap cache, int x, int y, int plane)
    {
        int packed = WorldPointUtil.compress(x, y, plane);
        int cached = cache.get(packed);
        if (cached != -1) {
            return cached;
        }

        int dist = calculateMinDistanceToCollisionInt(collisionMap, x, y, plane, MAX_PROXIMITY_SCAN);
        cache.put(packed, dist);
        return dist;
    }

    /**
     * Calculates minimum Chebyshev distance to nearest collision/obstacle.
     * Uses spiral-out scan with early termination for efficiency.
     * Checks cardinals first (most likely collision direction).
     * OPTIMIZED: Uses primitive ints, casts to short only at collision check.
     */
    private static int calculateMinDistanceToCollisionInt(CollisionMap collisionMap, int x, int y, int plane, int maxRadius)
    {
        byte p = (byte) plane;

        // Spiral out from center, early termination on first collision
        for (int r = 1; r <= maxRadius; r++) {
            // Check cardinals first (most common collision direction)
            if (!collisionMap.walkable((short) x, (short)(y + r), p)) return r;  // N
            if (!collisionMap.walkable((short) x, (short)(y - r), p)) return r;  // S
            if (!collisionMap.walkable((short)(x + r), (short) y, p)) return r;  // E
            if (!collisionMap.walkable((short)(x - r), (short) y, p)) return r;  // W

            // Corners
            if (!collisionMap.walkable((short)(x + r), (short)(y + r), p)) return r;  // NE
            if (!collisionMap.walkable((short)(x + r), (short)(y - r), p)) return r;  // SE
            if (!collisionMap.walkable((short)(x - r), (short)(y + r), p)) return r;  // NW
            if (!collisionMap.walkable((short)(x - r), (short)(y - r), p)) return r;  // SW

            // Fill in the rest of the ring edges
            for (int i = 1; i < r; i++) {
                // Top and bottom edges
                if (!collisionMap.walkable((short)(x + i), (short)(y + r), p)) return r;
                if (!collisionMap.walkable((short)(x - i), (short)(y + r), p)) return r;
                if (!collisionMap.walkable((short)(x + i), (short)(y - r), p)) return r;
                if (!collisionMap.walkable((short)(x - i), (short)(y - r), p)) return r;
                // Left and right edges
                if (!collisionMap.walkable((short)(x + r), (short)(y + i), p)) return r;
                if (!collisionMap.walkable((short)(x + r), (short)(y - i), p)) return r;
                if (!collisionMap.walkable((short)(x - r), (short)(y + i), p)) return r;
                if (!collisionMap.walkable((short)(x - r), (short)(y - i), p)) return r;
            }
        }
        return maxRadius + 1;  // No collision within scan range (open water)
    }

    /**
     * Converts full tile path to waypoints.
     * TEMPORARILY returns waypoint for EVERY tile to debug path quality.
     */
    public static List<Waypoint> convertToWaypoints(List<WorldPoint> path)
    {
        if (path.size() < 2) {
            return new ArrayList<>();
        }

        List<Waypoint> waypoints = new ArrayList<>();

        WorldPoint currentPos = path.get(0);
        Heading currentHeading = Heading.getOptimalHeading(currentPos, path.get(1));
        waypoints.add(new Waypoint(currentPos, currentHeading));
        currentPos = path.get(1);
        for(int i = 2; i < path.size(); i++)
        {
            WorldPoint nextPos = path.get(i);
            Heading nextHeading = Heading.getOptimalHeading(currentPos, nextPos);
            if(nextHeading != currentHeading)
            {
                waypoints.add(new Waypoint(currentPos, currentHeading));
                currentHeading = nextHeading;
                currentPos = nextPos;
                continue;
            }

            if(i == path.size() - 1)
            {
                waypoints.add(new Waypoint(nextPos, nextHeading));
            }

            currentPos = nextPos;
        }

        return waypoints;
    }
}

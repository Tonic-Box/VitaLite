package com.tonic.services.pathfinder.sailing;

import com.tonic.Static;
import com.tonic.api.game.sailing.Heading;
import com.tonic.api.game.sailing.SailingAPI;
import com.tonic.api.handlers.GenericHandlerBuilder;
import com.tonic.services.GameManager;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.collision.CollisionMap;
import com.tonic.services.pathfinder.sailing.graph.GraphNode;
import com.tonic.services.pathfinder.sailing.graph.NavGraph;
import com.tonic.services.pathfinder.tiletype.TileType;
import com.tonic.services.profiler.recording.MethodProfiler;
import com.tonic.util.Distance;
import com.tonic.util.Profiler;
import com.tonic.util.WorldPointUtil;
import com.tonic.util.handler.StepHandler;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.Getter;
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

/**
 * A* boat pathfinding with proximity-weighted costs and turn penalties.
 * Generates tile-by-tile paths preferring 6-7 tile clearance from obstacles,
 * naturally centers in corridors, avoids sharp turns, and falls back to
 * tighter paths when needed.
 *
 * ALGORITHM: A* with Chebyshev heuristic + cost = (baseCost × proximityMultiplier) + turnCost
 * - Base costs: 10 (orthogonal), 14 (diagonal ≈ √2 × 10)
 * - Proximity multipliers: 50× at 1 tile, 25× at 2, 12× at 3, 6× at 4, 3× at 5, 1× at 6+
 * - Turn costs: 0 for straight, 40 for 90°, 400 for 180° (boats need curved arcs to turn)
 * - Heuristic: Chebyshev distance × 10 (admissible for 8-directional movement)
 * - Natural centering: equidistant from walls = lowest combined cost
 *
 * OPTIMIZATION: Highly optimized hot path with:
 * - A* heuristic reduces iterations by 60-70% vs Dijkstra
 * - Closed set prevents node re-expansion
 * - Turn cost via parent lookup (avoids 8x state space expansion)
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
    private static final int[] DX = {-1, 1, 0, 0, -1, 1, -1, 1};
    private static final int[] DY = {0, 0, -1, 1, -1, -1, 1, 1};
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

    // Maximum radius to scan for proximity calculation (balanced: 5 for quality/speed tradeoff)
    private static final int MAX_PROXIMITY_SCAN = 5;

    // Direction index to heading value mapping (matches BoatHullCache.DIRECTION_HEADINGS)
    // West=4, East=12, South=0, North=8, SW=2, SE=14, NW=6, NE=10
    private static final int[] DIRECTION_TO_HEADING = {4, 12, 0, 8, 2, 14, 6, 10};

    // Number of parents to look back for cumulative turn calculation
    // 4 steps catches 3-step split turns (e.g., 30° + 30° + 30° = 90°)
    private static final int TURN_LOOKBACK_DEPTH = 3;

    // Turn cost penalties indexed by heading difference (0-8)
    // Heading diff: 0=same, 2=45°, 4=90°, 6=135°, 8=180°
    // Boats can't make sharp turns while moving - they need curved arcs
    // 90° turn needs ~5 tiles, 180° turn needs ~7 tiles
    private static final int[] TURN_COSTS = {
            0,    // 0: straight (0°) - no penalty
            0,    // 1: 22.5° - negligible turn
            5,    // 2: 45° - minor turn
            15,   // 3: 67.5° - moderate turn
            40,   // 4: 90° - significant turn (needs 5-6 tile radius)
            80,   // 5: 112.5° - major turn
            150,  // 6: 135° - severe turn
            250,  // 7: 157.5° - near-reversal
            400   // 8: 180° - full reversal (needs 6+ tile swing)
    };

    // Tile type cost penalties - high cost to strongly avoid hazardous water types
    private static final int BAD_WATER_COST = 10000;
    private static byte[] BAD_TILE_TYPES;
    private static final int AVOID_COST = 100;

    // Bad water buffer zone - scan radius for nearby hazardous tiles
    private static final int BAD_WATER_BUFFER_RADIUS = 4;

    // Graph-based pathfinding: maximum deviation from node path corridor (tiles)
    private static final int CORRIDOR_DEVIATION = 25;
    private static final int CORRIDOR_DEVIATION_SQUARED = CORRIDOR_DEVIATION * CORRIDOR_DEVIATION;

    // Corridor centerline pull weight - penalizes tiles far from corridor center
    // Higher = stronger preference for centerline (reduces node exploration)
    // Penalty = (distSq / CORRIDOR_DEVIATION) * WEIGHT (quadratic, integer-only)
    // 1 = gentle guidance, 2-3 = moderate, 5+ = strong pull
    private static final int CORRIDOR_PULL_WEIGHT = 2;

    // Maximum radius to search for nearest graph node (nodes can be sparse)
    private static final int MAX_NODE_SEARCH_RADIUS = 100;

    // Direction lookup table: index = (dx+1)*3 + (dy+1), maps to direction index 0-7
    // Eliminates loop in getDirectionIndex() - O(1) instead of O(8)
    private static final int[] DIRECTION_LUT = {
            4,  // (-1,-1) = SW
            0,  // (-1, 0) = W
            6,  // (-1,+1) = NW
            2,  // ( 0,-1) = S
            -1, // ( 0, 0) = no movement
            3,  // ( 0,+1) = N
            5,  // (+1,-1) = SE
            1,  // (+1, 0) = E
            7   // (+1,+1) = NE
    };

    // Fast bad tile type lookup - boolean array indexed by tile type byte
    // Eliminates loop in isBadTileType() - O(1) instead of O(n)
    private static final boolean[] IS_BAD_TILE = new boolean[256];

    public static StepHandler travelTo(WorldPoint worldPoint)
    {
        WorldPoint start = BoatCollisionAPI.getPlayerBoatWorldPoint();
        List<WorldPoint> fullPath = findFullPath(start, worldPoint);
        if(fullPath == null || fullPath.isEmpty())
        {
            System.out.println("BoatPathing: No path found to " + worldPoint);
            return GenericHandlerBuilder.get().build();
        }
        GameManager.setPathPoints(fullPath);
        List<Waypoint> waypoints = convertToWaypoints(fullPath);
        return travelTo(waypoints);
    }

    public static StepHandler travelTo(List<Waypoint> path)
    {
        return GenericHandlerBuilder.get()
                .addDelayUntil(context -> {
                    if(!context.contains("PATH"))
                    {
                        context.put("PATH", path);
                        context.put("POINTER", 0);
                        context.put("LAST_HEADING", null);
                        context.put("FINAL_DESTINATION", path.get(path.size() - 1).getPosition());
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
                    if(!context.contains("PATH") || !SailingAPI.isOnBoat())
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
                    WorldPoint start = BoatCollisionAPI.getPlayerBoatWorldPoint();

                    if(Distance.chebyshev(start, end.getPosition()) <= 3)
                    {
                        context.remove("PATH");
                        context.remove("POINTER");
                        SailingAPI.unSetSails();
                        GameManager.clearPathPoints();
                        return true;
                    }

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
        MethodProfiler.begin("BoatPathing.pathTo(target)");
        try {
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
        } finally {
            MethodProfiler.end("BoatPathing.pathTo(target)");
        }
    }

    /**
     * Finds a sailing path from start to target for the given boat.
     */
    public static List<Waypoint> pathTo(WorldPoint start, WorldPoint target)
    {
        MethodProfiler.begin("BoatPathing.pathTo(start,target)");
        try {
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
        } finally {
            MethodProfiler.end("BoatPathing.pathTo(start,target)");
        }
    }

    /**
     * Initializes boat hull cache from current boat state.
     * Extracts hull as primitive offsets to avoid allocations in hot path.
     * NOTE: Gets boat fresh from client since WorldEntity doesn't survive Static.invoke boundary
     */
    private static BoatHullCache initializeBoatCache(WorldPoint boatCenter, CollisionMap collisionMap)
    {
        MethodProfiler.begin("BoatPathing.initializeBoatCache");
        try {
            // Get player boat fresh (don't pass as parameter - doesn't survive Static.invoke)
            WorldEntity boat = BoatCollisionAPI.getPlayerBoat();
            if (boat == null) {
                System.out.println("SailPathing: No player boat found");
                return null;
            }

            // Get current hull in world coordinates - use player boat method
            Collection<WorldPoint> hull = BoatCollisionAPI.getPlayerBoatCollision();

            if (hull == null || hull.isEmpty()) {
                System.out.println("SailPathing: Empty boat hull (" + (hull == null ? "null" : 0) + " tiles)");
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

            return new BoatHullCache(xOffsets, yOffsets, currentHeading, collisionMap);
        } finally {
            MethodProfiler.end("BoatPathing.initializeBoatCache");
        }
    }

    /**
     * Collision check using pre-computed rotated hull offsets.
     * OPTIMIZED: No floating-point math or Math.round() in hot path.
     * All rotations are pre-computed during BoatHullCache initialization.
     */
    private static boolean canBoatFitAtDirection(BoatHullCache cache, short targetX, short targetY, int directionIndex)
    {
        MethodProfiler.begin("BoatPathing.canBoatFitAtDirection");
        try {
            // Use pre-computed rotated offsets - no floating-point math in hot path
            int[] rotatedX = cache.rotatedXOffsets[directionIndex];
            int[] rotatedY = cache.rotatedYOffsets[directionIndex];
            int hullSize = rotatedX.length;

            // Check each hull tile
            for (int i = 0; i < hullSize; i++) {
                short worldX = (short) (targetX + rotatedX[i]);
                short worldY = (short) (targetY + rotatedY[i]);

                // Check collision on plane 0
                if (!cache.collisionMap.walkable(worldX, worldY, (byte) 0)) {
                    return false;
                }
            }

            return true;
        } finally {
            MethodProfiler.end("BoatPathing.canBoatFitAtDirection");
        }
    }

    /**
     * Uses A* with proximity costs to find optimal path from start to target.
     * Prefers 6-7 tile clearance from obstacles, naturally centers in corridors,
     * and falls back to tighter paths when necessary.
     *
     * OPTIMIZED: A* with Chebyshev heuristic + closed set + primitive data structures.
     * Expected 60-70% fewer iterations than Dijkstra due to goal-directed search.
     */
    public static List<WorldPoint> findFullPath(WorldPoint start, WorldPoint target)
    {
        return findFullPath(start, target, null);
    }

    /**
     * Uses graph-based pathfinding with A* tile refinement.
     * 1. Find nearest graph nodes to start and target via BFS
     * 2. A* on navigation graph respecting water types
     * 3. A* tile-by-tile within corridor of node path
     *
     * Falls back to original A* if graph not available or no valid path.
     */
    public static List<WorldPoint> findFullPath(WorldPoint start, WorldPoint target, IntOpenHashSet avoidTiles)
    {
        MethodProfiler.begin("BoatPathing.findFullPath");
        Profiler.Start("BoatPathing");
        try {
            BAD_TILE_TYPES = TileType.getAvoidTileTypes();
            // Populate fast lookup table for bad tile types
            java.util.Arrays.fill(IS_BAD_TILE, false);
            for (byte b : BAD_TILE_TYPES) {
                IS_BAD_TILE[b & 0xFF] = true;
            }

            // Try graph-based pathfinding first
            NavGraph graph = Walker.getNavGraph();
            if (graph != null) {
                List<WorldPoint> graphPath = findFullPathWithGraph(start, target, avoidTiles, graph);
                if (graphPath != null) {
                    Profiler.StopMS();
                    return graphPath;
                }
                // Fallback to original A* if graph path failed
                System.out.println("BoatPathing: Graph pathfinding failed, falling back to original A*");
            }
            List<WorldPoint> path = Static.invoke(() -> {
                CollisionMap collisionMap = Walker.getCollisionMap();
                if (collisionMap == null) {
                    return null;
                }

                // Validate target - if boat can't fit, find nearest valid position
                WorldPoint adjustedTarget = target;
                WorldPoint validTarget = BoatCollisionAPI.findNearestValidPlayerBoatPosition(target, 10);
                if (validTarget == null) {
                    return null;
                }
                if (!validTarget.equals(target)) {
                    adjustedTarget = validTarget;
                }
                // Initialize cache with hull offsets and pre-computed rotations
                // Don't pass boat entity - it doesn't survive Static.invoke boundary
                BoatHullCache cache = initializeBoatCache(start, collisionMap);
                if (cache == null) {
                    // System.out.println("  ERROR: Failed to initialize boat cache");
                    return null;
                }

                // A* data structures - primitive arrays for heap (stores f-scores)
                int[] heapNodes = new int[100_000];
                int[] heapCosts = new int[100_000];  // f-scores for A*
                int heapSize = 0;

                // Primitive maps for g-scores, parents, and caches
                Int2IntOpenHashMap gScores = new Int2IntOpenHashMap();  // actual cost from start
                Int2IntOpenHashMap parents = new Int2IntOpenHashMap();
                Int2IntOpenHashMap proximityCache = new Int2IntOpenHashMap();
                IntOpenHashSet closedSet = new IntOpenHashSet();  // prevents re-expansion
                gScores.defaultReturnValue(Integer.MAX_VALUE);
                parents.defaultReturnValue(-2);  // -2 = not visited
                proximityCache.defaultReturnValue(-1);  // -1 = not cached

                int startPacked = WorldPointUtil.compress(start);
                int targetPacked = WorldPointUtil.compress(adjustedTarget);
                int targetX = WorldPointUtil.getCompressedX(targetPacked);
                int targetY = WorldPointUtil.getCompressedY(targetPacked);
                int startX = WorldPointUtil.getCompressedX(startPacked);
                int startY = WorldPointUtil.getCompressedY(startPacked);

                // Initialize start node with f = g(0) + h
                gScores.put(startPacked, 0);
                parents.put(startPacked, -1);  // -1 = start node marker
                int startH = heuristic(startX, startY, targetX, targetY);
                heapSize = heapPush(heapNodes, heapCosts, heapSize, startPacked, startH);

                int maxIterations = 1_000_000;
                int iterations = 0;

                // A* search
                while (heapSize > 0 && iterations++ < maxIterations) {
                    // Pop minimum f-score node
                    heapSize = heapPop(heapNodes, heapCosts, heapSize);
                    int current = heapNodes[heapSize];

                    // Skip if already in closed set (already fully processed)
                    if (closedSet.contains(current)) {
                        continue;
                    }
                    closedSet.add(current);

                    // Check if reached target
                    if (current == targetPacked) {
                        return reconstructFullPath(parents, targetPacked);
                    }

                    int currentG = gScores.get(current);

                    // Expand neighbors with A* scoring (f = g + h)
                    heapSize = expandNeighborsAStar(cache, collisionMap, current, currentG,
                            targetX, targetY, gScores, parents, proximityCache, closedSet,
                            heapNodes, heapCosts, heapSize, avoidTiles, startPacked);
                }
                return null;
            });

            Profiler.StopMS();
            return path;
        } finally {
            MethodProfiler.end("BoatPathing.findFullPath");
        }
    }

    // ==================== Graph-Based Pathfinding ====================

    /**
     * Finds a path using the navigation graph for high-level routing,
     * then A* for tile-by-tile navigation within the corridor.
     */
    private static List<WorldPoint> findFullPathWithGraph(
            WorldPoint start, WorldPoint target, IntOpenHashSet avoidTiles, NavGraph graph)
    {
        MethodProfiler.begin("BoatPathing.findFullPathWithGraph");
        try {
            return Static.invoke(() -> {
                int plane = start.getPlane();

            // Debug: Print graph stats and search positions
            // Step 1: Find nearest graph nodes via BFS (use world coords, not packed - packing formats differ!)
            int startNode = findNearestNode(graph, start.getX(), start.getY(), plane, MAX_NODE_SEARCH_RADIUS);
            int endNode = findNearestNode(graph, target.getX(), target.getY(), plane, MAX_NODE_SEARCH_RADIUS);

            if (startNode == -1 || endNode == -1) {
                // Debug: Find the bounding box of all nodes in the graph
                int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
                int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
                for (int node = graph.getNextNode(0); node >= 0; node = graph.getNextNode(node + 1)) {
                    int nx = GraphNode.getX(node);
                    int ny = GraphNode.getY(node);
                    minX = Math.min(minX, nx);
                    maxX = Math.max(maxX, nx);
                    minY = Math.min(minY, ny);
                    maxY = Math.max(maxY, ny);
                }
                System.out.println("  Graph bounds: X=[" + minX + ".." + maxX + "], Y=[" + minY + ".." + maxY + "]");
                System.out.println("  Your position: X=" + start.getX() + ", Y=" + start.getY());
                return null;
            }

            // Step 2: A* on graph to find node path
            List<Integer> nodePath = findGraphPath(graph, startNode, endNode, BAD_TILE_TYPES);

            if (nodePath == null || nodePath.isEmpty()) {
                System.out.println("BoatPathing: No valid graph path found");
                return null;
            }

            // Step 3: A* tile-by-tile with corridor constraint
            return findFullPathWithCorridor(start, target, avoidTiles, nodePath);
            });
        } finally {
            MethodProfiler.end("BoatPathing.findFullPathWithGraph");
        }
    }

    /**
     * Finds the nearest graph node to the given world position via BFS spiral search.
     *
     * @param graph The navigation graph
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param plane Plane (0-3)
     * @param maxRadius Maximum search radius
     * @return Packed coordinates of nearest node (using GraphNode packing), or -1 if not found
     */
    private static int findNearestNode(NavGraph graph, int worldX, int worldY, int plane, int maxRadius)
    {
        MethodProfiler.begin("BoatPathing.findNearestNode");
        try {
            // Check center first (pack using GraphNode format which matches the graph)
            int centerPacked = GraphNode.pack(worldX, worldY, plane);
            if (graph.hasNode(centerPacked)) {
                return centerPacked;
            }

            // Spiral outward
            for (int r = 1; r <= maxRadius; r++) {
                // Check ring at radius r
                for (int dx = -r; dx <= r; dx++) {
                    for (int dy = -r; dy <= r; dy++) {
                        // Only check tiles on the ring perimeter
                        if (Math.abs(dx) != r && Math.abs(dy) != r) continue;

                        int packed = GraphNode.pack(worldX + dx, worldY + dy, plane);
                        if (graph.hasNode(packed)) {
                            return packed;
                        }
                    }
                }
            }

            return -1;
        } finally {
            MethodProfiler.end("BoatPathing.findNearestNode");
        }
    }

    /**
     * Finds a path through the navigation graph using A*.
     *
     * @param graph The navigation graph
     * @param startNode Packed coordinates of start node
     * @param endNode Packed coordinates of end node
     * @param avoidTypes Tile types to avoid
     * @return List of packed node coordinates forming the path, or null if no path
     */
    private static List<Integer> findGraphPath(NavGraph graph, int startNode, int endNode, byte[] avoidTypes)
    {
        MethodProfiler.begin("BoatPathing.findGraphPath");
        try {
            if (startNode == endNode) {
                List<Integer> path = new ArrayList<>();
                path.add(startNode);
                return path;
            }

            // A* data structures
            Int2IntOpenHashMap gScores = new Int2IntOpenHashMap();
            Int2IntOpenHashMap parents = new Int2IntOpenHashMap();
            IntOpenHashSet closedSet = new IntOpenHashSet();
            gScores.defaultReturnValue(Integer.MAX_VALUE);
            parents.defaultReturnValue(-1);

            // Priority queue (simple array-based for small graphs)
            int[] heapNodes = new int[10000];
            int[] heapCosts = new int[10000];
            int heapSize = 0;

            // Initialize
            gScores.put(startNode, 0);
            int h = GraphNode.chebyshevDistance(startNode, endNode);
            heapSize = heapPush(heapNodes, heapCosts, heapSize, startNode, h);

            int iterations = 0;
            int maxIterations = 100000;

            while (heapSize > 0 && iterations++ < maxIterations) {
                heapSize = heapPop(heapNodes, heapCosts, heapSize);
                int current = heapNodes[heapSize];

                if (closedSet.contains(current)) continue;
                closedSet.add(current);

                if (current == endNode) {
                    // Reconstruct path
                    List<Integer> path = new ArrayList<>();
                    int node = endNode;
                    while (node != -1) {
                        path.add(node);
                        node = parents.get(node);
                    }
                    Collections.reverse(path);
                    return path;
                }

                int currentG = gScores.get(current);
                IntList neighbors = graph.getNeighbors(current);

                for (int i = 0; i < neighbors.size(); i++) {
                    int neighbor = neighbors.getInt(i);

                    if (closedSet.contains(neighbor)) continue;

                    // Check if edge is traversable (doesn't cross bad water types)
                    if (!graph.isEdgeTraversable(current, neighbor, avoidTypes)) {
                        continue;
                    }

                    // Edge cost is Chebyshev distance between nodes
                    int edgeCost = GraphNode.chebyshevDistance(current, neighbor);
                    int tentativeG = currentG + edgeCost;

                    if (tentativeG < gScores.get(neighbor)) {
                        gScores.put(neighbor, tentativeG);
                        parents.put(neighbor, current);

                        int neighborH = GraphNode.chebyshevDistance(neighbor, endNode);
                        int f = tentativeG + neighborH;
                        heapSize = heapPush(heapNodes, heapCosts, heapSize, neighbor, f);
                    }
                }
            }

            return null; // No path found
        } finally {
            MethodProfiler.end("BoatPathing.findGraphPath");
        }
    }

    /**
     * Finds a tile-by-tile path constrained to the corridor around the node path.
     *
     * @param start Starting world point
     * @param target Target world point
     * @param avoidTiles Optional tiles to avoid
     * @param nodePath List of packed node coordinates defining the corridor
     * @return Full tile path, or null if not found
     */
    private static List<WorldPoint> findFullPathWithCorridor(
            WorldPoint start, WorldPoint target, IntOpenHashSet avoidTiles, List<Integer> nodePath)
    {
        MethodProfiler.begin("BoatPathing.findFullPathWithCorridor");
        try {
            CollisionMap collisionMap = Walker.getCollisionMap();
            if (collisionMap == null) {
                return null;
            }

            // Validate and adjust target
            WorldPoint adjustedTarget = target;
            WorldPoint validTarget = BoatCollisionAPI.findNearestValidPlayerBoatPosition(target, 10);
            if (validTarget == null) {
                return null;
            }
            if (!validTarget.equals(target)) {
                adjustedTarget = validTarget;
            }

            // Initialize boat cache
            BoatHullCache cache = initializeBoatCache(start, collisionMap);
            if (cache == null) {
                return null;
            }

            // A* with corridor constraint
            int[] heapNodes = new int[100_000];
            int[] heapCosts = new int[100_000];
            int heapSize = 0;

            Int2IntOpenHashMap gScores = new Int2IntOpenHashMap();
            Int2IntOpenHashMap parents = new Int2IntOpenHashMap();
            Int2IntOpenHashMap proximityCache = new Int2IntOpenHashMap();
            IntOpenHashSet closedSet = new IntOpenHashSet();
            Int2IntOpenHashMap corridorDistance = new Int2IntOpenHashMap();  // cached distance to centerline (-2 = outside)
            gScores.defaultReturnValue(Integer.MAX_VALUE);
            parents.defaultReturnValue(-2);
            proximityCache.defaultReturnValue(-1);
            corridorDistance.defaultReturnValue(-1);  // -1 = not computed yet

            int startPacked = WorldPointUtil.compress(start);
            int targetPacked = WorldPointUtil.compress(adjustedTarget);
            int targetX = WorldPointUtil.getCompressedX(targetPacked);
            int targetY = WorldPointUtil.getCompressedY(targetPacked);

            gScores.put(startPacked, 0);
            parents.put(startPacked, -1);
            int startH = heuristic(WorldPointUtil.getCompressedX(startPacked),
                    WorldPointUtil.getCompressedY(startPacked), targetX, targetY);
            heapSize = heapPush(heapNodes, heapCosts, heapSize, startPacked, startH);

            int maxIterations = 1_000_000;
            int iterations = 0;

            while (heapSize > 0 && iterations++ < maxIterations) {
                heapSize = heapPop(heapNodes, heapCosts, heapSize);
                int current = heapNodes[heapSize];

                if (closedSet.contains(current)) continue;
                closedSet.add(current);

                if (current == targetPacked) {
                    return reconstructFullPath(parents, targetPacked);
                }

                int currentG = gScores.get(current);
                heapSize = expandNeighborsAStarCorridor(cache, collisionMap, current, currentG,
                        targetX, targetY, gScores, parents, proximityCache, closedSet,
                        heapNodes, heapCosts, heapSize, avoidTiles, startPacked, nodePath,
                        start.getX(), start.getY(), adjustedTarget.getX(), adjustedTarget.getY(),
                        corridorDistance);
            }

            return null;
        } finally {
            MethodProfiler.end("BoatPathing.findFullPathWithCorridor");
        }
    }

    /**
     * Expands neighbors with corridor constraint and centerline heuristic bonus.
     * Similar to expandNeighborsAStar but skips tiles outside the corridor
     * and adds a penalty for tiles far from the corridor centerline.
     */
    private static int expandNeighborsAStarCorridor(
            BoatHullCache cache, CollisionMap collisionMap,
            int current, int currentG,
            int targetX, int targetY,
            Int2IntOpenHashMap gScores, Int2IntOpenHashMap parents,
            Int2IntOpenHashMap proximityCache, IntOpenHashSet closedSet,
            int[] heapNodes, int[] heapCosts, int heapSize,
            IntOpenHashSet avoidTiles, int startPacked,
            List<Integer> nodePath,
            int startWorldX, int startWorldY, int targetWorldX, int targetWorldY,
            Int2IntOpenHashMap corridorDistance)
    {
        MethodProfiler.begin("BoatPathing.expandNeighborsAStarCorridor");
        try {
            int x = WorldPointUtil.getCompressedX(current);
            int y = WorldPointUtil.getCompressedY(current);
            int plane = WorldPointUtil.getCompressedPlane(current);
            int sX = WorldPointUtil.getCompressedX(startPacked);
            int sY = WorldPointUtil.getCompressedY(startPacked);
            int targetPacked = WorldPointUtil.compress(targetX, targetY, plane);

            // Cache TileTypeMap reference - avoid repeated getter calls in hot loop
            var tileTypeMap = Walker.getTileTypeMap();

            for (int dir = 0; dir < 8; dir++) {
                int nx = x + DX[dir];
                int ny = y + DY[dir];
                int neighborPacked = WorldPointUtil.compress(nx, ny, plane);

                if (closedSet.contains(neighborPacked)) continue;

                // Corridor constraint: get distance to centerline (with caching)
                // -1 = not computed, -2 = outside corridor, >= 0 = distance squared
                int corridorDistSq = corridorDistance.get(neighborPacked);
                if (corridorDistSq == -1) {
                    // Not cached - compute and cache result
                    corridorDistSq = getCorridorDistanceSquared(nx, ny, nodePath,
                            startWorldX, startWorldY, targetWorldX, targetWorldY);
                    // Store -2 for outside corridor, otherwise store actual distance
                    corridorDistance.put(neighborPacked, corridorDistSq == -1 ? -2 : corridorDistSq);
                    if (corridorDistSq == -1) {
                        corridorDistSq = -2;  // Update local var for the check below
                    }
                }
                if (corridorDistSq == -2) {
                    continue;  // Outside corridor
                }

                // Hull collision check
                boolean nearStart = Math.abs(nx - sX) <= 3 && Math.abs(ny - sY) <= 3;
                boolean isTarget = (neighborPacked == targetPacked);

                if (!isTarget) {
                    int hullDir = nearStart ? 8 : dir;
                    if (!canBoatFitAtDirection(cache, (short) nx, (short) ny, hullDir)) {
                        continue;
                    }
                }

                // Cost calculation (same as original)
                int baseCost = BASE_COSTS[dir];
                int combined = getCombinedProximityCached(collisionMap, proximityCache, nx, ny, plane);
                int collisionDist = combined >>> 16;
                int badWaterDist = combined & 0xFFFF;

                int proximityCost = PROXIMITY_COSTS[Math.min(collisionDist, PROXIMITY_COSTS.length - 1)];
                if (proximityCost == Integer.MAX_VALUE) continue;

                // Combined turn + alternation costs (single parent chain walk)
                int combinedCosts = getCombinedTurnCosts(parents, current, dir);
                int turnCost = combinedCosts >>> 16;
                int alternationCost = combinedCosts & 0xFFFF;

                int tileTypeCost = 0;
                byte tileType = tileTypeMap.getTileType(nx, ny, plane);
                if (isBadTileType(tileType)) {
                    tileTypeCost = BAD_WATER_COST;
                } else if (badWaterDist > 0) {
                    tileTypeCost = BAD_WATER_COST >> badWaterDist;
                }

                int cloudCost = 0;
                if (avoidTiles != null && avoidTiles.contains(neighborPacked)) {
                    cloudCost = AVOID_COST;
                }

                int edgeCost = baseCost * proximityCost + turnCost + alternationCost + tileTypeCost + cloudCost;
                int tentativeG = currentG + edgeCost;

                if (tentativeG < gScores.get(neighborPacked)) {
                    gScores.put(neighborPacked, tentativeG);
                    parents.put(neighborPacked, current);

                    int h = heuristic(nx, ny, targetX, targetY);

                    // Corridor centerline pull: penalize tiles far from center (quadratic, integer-only)
                    // corridorDistSq 0 = on centerline = no penalty
                    // corridorDistSq 625 (25²) = at edge = penalty of 25 * WEIGHT
                    int corridorPenalty = (corridorDistSq / CORRIDOR_DEVIATION) * CORRIDOR_PULL_WEIGHT;

                    int f = tentativeG + (h * 3 / 2) + corridorPenalty;
                    heapSize = heapPush(heapNodes, heapCosts, heapSize, neighborPacked, f);
                }
            }

            return heapSize;
        } finally {
            MethodProfiler.end("BoatPathing.expandNeighborsAStarCorridor");
        }
    }

    /**
     * Checks if a tile is within the corridor defined by the node path.
     * Also considers the actual start/target positions (which may be far from graph nodes).
     *
     * @param tileX Tile X coordinate
     * @param tileY Tile Y coordinate
     * @param nodePath List of packed node coordinates
     * @param maxDeviation Maximum perpendicular distance from path segments
     * @param startX Actual start position X (boat position)
     * @param startY Actual start position Y
     * @param targetX Actual target position X
     * @param targetY Actual target position Y
     * @return true if within corridor
     */
    private static boolean isWithinCorridor(int tileX, int tileY, List<Integer> nodePath,
                                            int maxDeviation, int startX, int startY, int targetX, int targetY)
    {
        if (nodePath == null || nodePath.size() < 2) {
            return true; // No corridor constraint
        }

        // Allow tiles near actual start position (boat may be far from first graph node)
        int distToStart = Math.max(Math.abs(tileX - startX), Math.abs(tileY - startY));
        if (distToStart <= maxDeviation) {
            return true;
        }

        // Allow tiles near actual target position (target may be far from last graph node)
        int distToTarget = Math.max(Math.abs(tileX - targetX), Math.abs(tileY - targetY));
        if (distToTarget <= maxDeviation) {
            return true;
        }

        // Check distance to each segment between graph nodes (using squared distance)
        // Uses pre-computed CORRIDOR_DEVIATION_SQUARED constant
        for (int i = 0; i < nodePath.size() - 1; i++) {
            int n1 = nodePath.get(i);
            int n2 = nodePath.get(i + 1);

            int x1 = GraphNode.getX(n1);
            int y1 = GraphNode.getY(n1);
            int x2 = GraphNode.getX(n2);
            int y2 = GraphNode.getY(n2);

            int distSquared = pointToSegmentDistanceSquared(tileX, tileY, x1, y1, x2, y2);
            if (distSquared <= CORRIDOR_DEVIATION_SQUARED) {
                return true;
            }
        }

        // Also check distance to first/last graph nodes themselves
        int first = nodePath.get(0);
        int last = nodePath.get(nodePath.size() - 1);
        int distToFirst = Math.max(Math.abs(tileX - GraphNode.getX(first)),
                Math.abs(tileY - GraphNode.getY(first)));
        int distToLast = Math.max(Math.abs(tileX - GraphNode.getX(last)),
                Math.abs(tileY - GraphNode.getY(last)));

        return distToFirst <= maxDeviation || distToLast <= maxDeviation;
    }

    /**
     * Returns the minimum squared distance from a tile to the corridor centerline.
     * Returns -1 if the tile is outside the corridor (> CORRIDOR_DEVIATION from all segments).
     * Used for corridor centerline heuristic bonus - tiles closer to center get lower penalty.
     *
     * @param tileX Tile X coordinate
     * @param tileY Tile Y coordinate
     * @param nodePath List of packed graph node coordinates forming the corridor
     * @param startX Actual start position X
     * @param startY Actual start position Y
     * @param targetX Actual target position X
     * @param targetY Actual target position Y
     * @return Minimum squared distance to corridor centerline, or -1 if outside corridor
     */
    private static int getCorridorDistanceSquared(int tileX, int tileY, List<Integer> nodePath,
                                                   int startX, int startY, int targetX, int targetY)
    {
        if (nodePath == null || nodePath.size() < 2) {
            return 0; // No corridor = on centerline
        }

        int minDistSq = Integer.MAX_VALUE;

        // Distance to actual start position
        int dxStart = tileX - startX;
        int dyStart = tileY - startY;
        int distToStartSq = dxStart * dxStart + dyStart * dyStart;
        minDistSq = Math.min(minDistSq, distToStartSq);

        // Distance to actual target position
        int dxTarget = tileX - targetX;
        int dyTarget = tileY - targetY;
        int distToTargetSq = dxTarget * dxTarget + dyTarget * dyTarget;
        minDistSq = Math.min(minDistSq, distToTargetSq);

        // Distance to each path segment
        for (int i = 0; i < nodePath.size() - 1; i++) {
            int n1 = nodePath.get(i);
            int n2 = nodePath.get(i + 1);

            int distSq = pointToSegmentDistanceSquared(tileX, tileY,
                    GraphNode.getX(n1), GraphNode.getY(n1),
                    GraphNode.getX(n2), GraphNode.getY(n2));
            minDistSq = Math.min(minDistSq, distSq);
        }

        // Distance to first/last graph nodes
        int first = nodePath.get(0);
        int last = nodePath.get(nodePath.size() - 1);
        int dxFirst = tileX - GraphNode.getX(first);
        int dyFirst = tileY - GraphNode.getY(first);
        minDistSq = Math.min(minDistSq, dxFirst * dxFirst + dyFirst * dyFirst);

        int dxLast = tileX - GraphNode.getX(last);
        int dyLast = tileY - GraphNode.getY(last);
        minDistSq = Math.min(minDistSq, dxLast * dxLast + dyLast * dyLast);

        // Return -1 if outside corridor, otherwise the minimum distance squared
        return minDistSq <= CORRIDOR_DEVIATION_SQUARED ? minDistSq : -1;
    }

    /**
     * Calculates squared perpendicular distance from point to line segment.
     * OPTIMIZED: Uses integer math only, no Math.sqrt (compare squared values instead).
     */
    private static int pointToSegmentDistanceSquared(int px, int py, int x1, int y1, int x2, int y2)
    {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int lengthSquared = dx * dx + dy * dy;

        if (lengthSquared == 0) {
            return (px - x1) * (px - x1) + (py - y1) * (py - y1);
        }

        // Clamp t to [0, 1] using integer math
        int dot = (px - x1) * dx + (py - y1) * dy;
        int projX, projY;
        if (dot <= 0) {
            projX = x1;
            projY = y1;
        } else if (dot >= lengthSquared) {
            projX = x2;
            projY = y2;
        } else {
            // t = dot / lengthSquared, proj = start + t * (end - start)
            projX = x1 + (dot * dx) / lengthSquared;
            projY = y1 + (dot * dy) / lengthSquared;
        }

        return (px - projX) * (px - projX) + (py - projY) * (py - projY);
    }

    /**
     * Expands neighbors in 8 directions with A* scoring (f = g + h).
     * OPTIMIZED: Uses primitive arrays and maps, pre-computed rotations, closed set.
     * All operations use primitive ints - no object allocations in hot path.
     *
     * Cost formula: (baseCost × proximityMultiplier) + turnCost
     * - Base costs: 10 (orthogonal), 14 (diagonal)
     * - Proximity multipliers: 50× at 1 tile, 25× at 2, 12× at 3, etc.
     * - Turn costs: 0 for straight, 40 for 90°, 400 for 180° (discourages sharp turns)
     *
     * This naturally centers the path between walls (equidistant = equal low costs)
     * and prefers 6-7 tile buffer from obstacles, while avoiding sharp turns.
     *
     * @return new heap size
     */
    private static int expandNeighborsAStar(
            BoatHullCache cache, CollisionMap collisionMap,
            int current, int currentG,
            int targetX, int targetY,
            Int2IntOpenHashMap gScores, Int2IntOpenHashMap parents,
            Int2IntOpenHashMap proximityCache, IntOpenHashSet closedSet,
            int[] heapNodes, int[] heapCosts, int heapSize,
            IntOpenHashSet avoidTiles,
            int startPacked)
    {
        MethodProfiler.begin("BoatPathing.expandNeighborsAStar");
        try {
            // Extract as ints to avoid repeated casts
            int x = WorldPointUtil.getCompressedX(current);
            int y = WorldPointUtil.getCompressedY(current);
            int plane = WorldPointUtil.getCompressedPlane(current);
            int sX = WorldPointUtil.getCompressedX(startPacked);
            int sY = WorldPointUtil.getCompressedY(startPacked);
            int targetPacked = WorldPointUtil.compress(targetX, targetY, plane);

            // Expand in all 8 directions
            for (int dir = 0; dir < 8; dir++) {
                int nx = x + DX[dir];
                int ny = y + DY[dir];

                // Use int overload - no object allocation
                int neighborPacked = WorldPointUtil.compress(nx, ny, plane);

                // Skip if already in closed set (already fully processed)
                if (closedSet.contains(neighborPacked)) {
                    continue;
                }

                // Skip if boat doesn't fit at this position/direction
                // EXCEPTIONS:
                // 1. Near start - use unrotated hull (index 8) since boat is at actual current heading
                // 2. Target tile - already validated with all headings, don't reject based on approach direction
                boolean nearStart = Math.abs(nx - sX) <= 3 && Math.abs(ny - sY) <= 3;
                boolean isTarget = (neighborPacked == targetPacked);

                if (!isTarget) {
                    // Near start: use unrotated hull (index 8) to match boat's actual current heading
                    // Elsewhere: use direction-rotated hull (index 0-7)
                    int hullDir = nearStart ? 8 : dir;
                    if (!canBoatFitAtDirection(cache, (short) nx, (short) ny, hullDir)) {
                        continue;
                    }
                }

                // Calculate edge cost with combined proximity data (collision + bad water)
                int baseCost = BASE_COSTS[dir];
                int combined = getCombinedProximityCached(collisionMap, proximityCache, nx, ny, plane);
                int collisionDist = combined >>> 16;
                int badWaterDist = combined & 0xFFFF;

                int proximityCost = PROXIMITY_COSTS[Math.min(collisionDist, PROXIMITY_COSTS.length - 1)];

                // Avoid overflow: if proximityCost is MAX_VALUE, skip this tile
                if (proximityCost == Integer.MAX_VALUE) {
                    continue;
                }

                // Combined turn + alternation costs (single parent chain walk)
                int combinedCosts = getCombinedTurnCosts(parents, current, dir);
                int turnCost = combinedCosts >>> 16;
                int alternationCost = combinedCosts & 0xFFFF;

                // Tile type penalty: bad water buffer zone from unified scan
                // Also check if tile itself is bad water (not just buffer zone)
                int tileTypeCost = 0;
                byte tileType = Walker.getTileTypeMap().getTileType(nx, ny, plane);
                if (isBadTileType(tileType)) {
                    tileTypeCost = BAD_WATER_COST;
                } else if (badWaterDist > 0) {
                    // Buffer zone: graduated penalty based on distance
                    tileTypeCost = BAD_WATER_COST >> badWaterDist;
                }

                // Cloud avoidance cost: high penalty for tiles in cloud danger zones
                int cloudCost = 0;
                if (avoidTiles != null && avoidTiles.contains(neighborPacked)) {
                    cloudCost = AVOID_COST;
                }

                int edgeCost = baseCost * proximityCost + turnCost + alternationCost + tileTypeCost + cloudCost;
                int tentativeG = currentG + edgeCost;

                // Only update if this path is better
                if (tentativeG < gScores.get(neighborPacked)) {
                    gScores.put(neighborPacked, tentativeG);
                    parents.put(neighborPacked, current);

                    // Weighted A* priority: f = g + w*h (w=1.5 for faster search, slightly suboptimal paths)
                    // This reduces node exploration by 2-5x while paths remain near-optimal
                    int h = heuristic(nx, ny, targetX, targetY);
                    int f = tentativeG + (h * 3 / 2);  // w = 1.5
                    heapSize = heapPush(heapNodes, heapCosts, heapSize, neighborPacked, f);
                }
            }

            return heapSize;
        } finally {
            MethodProfiler.end("BoatPathing.expandNeighborsAStar");
        }
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

    // ==================== A* Heuristic ====================

    /**
     * Chebyshev distance heuristic for A* - admissible for 8-directional movement.
     * Returns minimum possible cost to reach goal (never overestimates).
     * Uses base movement cost of 10 (minimum possible step cost).
     */
    private static int heuristic(int fromX, int fromY, int goalX, int goalY)
    {
        int dx = Math.abs(goalX - fromX);
        int dy = Math.abs(goalY - fromY);
        // Chebyshev: max of dx/dy since diagonal costs same as cardinal (10)
        return Math.max(dx, dy) * 10;
    }

    // ==================== Proximity Calculation ====================

    /**
     * Gets cached combined proximity data (collision + bad water distances).
     * Returns packed int: (collisionDist << 16) | badWaterDist
     * OPTIMIZED: Single cache lookup for both values.
     */
    private static int getCombinedProximityCached(CollisionMap collisionMap, Int2IntOpenHashMap cache, int x, int y, int plane)
    {
        int packed = WorldPointUtil.compress(x, y, plane);
        int cached = cache.get(packed);
        if (cached != -1) {
            return cached;
        }

        int combined = calculateCombinedProximity(collisionMap, x, y, plane);
        cache.put(packed, combined);
        return combined;
    }

    /**
     * Unified proximity scan: finds BOTH collision distance AND bad water distance in one pass.
     * Returns packed int: (collisionDist << 16) | badWaterDist
     *
     * OPTIMIZED: Single spiral scan replaces two separate scans, ~50% fewer lookups.
     * Early termination when both collision and bad water are found.
     */
    private static int calculateCombinedProximity(CollisionMap collisionMap, int x, int y, int plane)
    {
        byte p = (byte) plane;
        int collisionDist = MAX_PROXIMITY_SCAN + 1;  // Default: no collision found
        int badWaterDist = 0;  // Default: no bad water found (0 = none in range)

        // Cache TileTypeMap reference - avoid repeated getter calls
        var tileTypeMap = Walker.getTileTypeMap();

        // Spiral out from center
        for (int r = 1; r <= MAX_PROXIMITY_SCAN; r++) {
            // Check collision (until found)
            if (collisionDist > MAX_PROXIMITY_SCAN) {
                // Cardinals - N, S, E, W
                if (!collisionMap.walkable((short) x, (short)(y + r), p)) collisionDist = r;
                else if (!collisionMap.walkable((short) x, (short)(y - r), p)) collisionDist = r;
                else if (!collisionMap.walkable((short)(x + r), (short) y, p)) collisionDist = r;
                else if (!collisionMap.walkable((short)(x - r), (short) y, p)) collisionDist = r;
                    // Corners - NE, SE, NW, SW
                else if (!collisionMap.walkable((short)(x + r), (short)(y + r), p)) collisionDist = r;
                else if (!collisionMap.walkable((short)(x + r), (short)(y - r), p)) collisionDist = r;
                else if (!collisionMap.walkable((short)(x - r), (short)(y + r), p)) collisionDist = r;
                else if (!collisionMap.walkable((short)(x - r), (short)(y - r), p)) collisionDist = r;
                else {
                    // Ring edges
                    for (int i = 1; i < r && collisionDist > MAX_PROXIMITY_SCAN; i++) {
                        if (!collisionMap.walkable((short)(x + i), (short)(y + r), p)) collisionDist = r;
                        else if (!collisionMap.walkable((short)(x - i), (short)(y + r), p)) collisionDist = r;
                        else if (!collisionMap.walkable((short)(x + i), (short)(y - r), p)) collisionDist = r;
                        else if (!collisionMap.walkable((short)(x - i), (short)(y - r), p)) collisionDist = r;
                        else if (!collisionMap.walkable((short)(x + r), (short)(y + i), p)) collisionDist = r;
                        else if (!collisionMap.walkable((short)(x + r), (short)(y - i), p)) collisionDist = r;
                        else if (!collisionMap.walkable((short)(x - r), (short)(y + i), p)) collisionDist = r;
                        else if (!collisionMap.walkable((short)(x - r), (short)(y - i), p)) collisionDist = r;
                    }
                }
            }

            // Check bad water (until found, only within buffer radius)
            if (badWaterDist == 0 && r <= BAD_WATER_BUFFER_RADIUS) {
                // Cardinals
                if (IS_BAD_TILE[tileTypeMap.getTileType(x, y + r, plane) & 0xFF]) badWaterDist = r;
                else if (IS_BAD_TILE[tileTypeMap.getTileType(x, y - r, plane) & 0xFF]) badWaterDist = r;
                else if (IS_BAD_TILE[tileTypeMap.getTileType(x + r, y, plane) & 0xFF]) badWaterDist = r;
                else if (IS_BAD_TILE[tileTypeMap.getTileType(x - r, y, plane) & 0xFF]) badWaterDist = r;
                    // Corners
                else if (IS_BAD_TILE[tileTypeMap.getTileType(x + r, y + r, plane) & 0xFF]) badWaterDist = r;
                else if (IS_BAD_TILE[tileTypeMap.getTileType(x + r, y - r, plane) & 0xFF]) badWaterDist = r;
                else if (IS_BAD_TILE[tileTypeMap.getTileType(x - r, y + r, plane) & 0xFF]) badWaterDist = r;
                else if (IS_BAD_TILE[tileTypeMap.getTileType(x - r, y - r, plane) & 0xFF]) badWaterDist = r;
                else {
                    // Ring edges
                    for (int i = 1; i < r && badWaterDist == 0; i++) {
                        if (IS_BAD_TILE[tileTypeMap.getTileType(x + i, y + r, plane) & 0xFF]) badWaterDist = r;
                        else if (IS_BAD_TILE[tileTypeMap.getTileType(x - i, y + r, plane) & 0xFF]) badWaterDist = r;
                        else if (IS_BAD_TILE[tileTypeMap.getTileType(x + i, y - r, plane) & 0xFF]) badWaterDist = r;
                        else if (IS_BAD_TILE[tileTypeMap.getTileType(x - i, y - r, plane) & 0xFF]) badWaterDist = r;
                        else if (IS_BAD_TILE[tileTypeMap.getTileType(x + r, y + i, plane) & 0xFF]) badWaterDist = r;
                        else if (IS_BAD_TILE[tileTypeMap.getTileType(x + r, y - i, plane) & 0xFF]) badWaterDist = r;
                        else if (IS_BAD_TILE[tileTypeMap.getTileType(x - r, y + i, plane) & 0xFF]) badWaterDist = r;
                        else if (IS_BAD_TILE[tileTypeMap.getTileType(x - r, y - i, plane) & 0xFF]) badWaterDist = r;
                    }
                }
            }

            // Early exit if both found
            if (collisionDist <= MAX_PROXIMITY_SCAN && badWaterDist > 0) break;
        }

        // Pack both results: high 16 bits = collision, low 16 = bad water
        return (collisionDist << 16) | badWaterDist;
    }

    // ==================== Turn Cost Calculation ====================

    /**
     * Gets direction index (0-7) from movement delta.
     * OPTIMIZED: O(1) lookup table instead of O(8) loop.
     * @return direction index, or -1 if no movement
     */
    private static int getDirectionIndex(int fromX, int fromY, int toX, int toY)
    {
        int dx = Integer.signum(toX - fromX);
        int dy = Integer.signum(toY - fromY);
        return DIRECTION_LUT[(dx + 1) * 3 + (dy + 1)];
    }

    /**
     * Calculates both turn cost and alternation cost in a single parent chain walk.
     * OPTIMIZED: Combines getTurnCost and getAlternationCost to avoid duplicate parent lookups.
     *
     * Returns packed int: (turnCost << 16) | alternationCost
     *
     * Turn cost: Based on cumulative heading change over TURN_LOOKBACK_DEPTH steps.
     * Alternation cost: Penalizes wobble patterns like EAST → SE → EAST.
     *
     * @param parents Parent map for path reconstruction
     * @param current Current node's packed coordinates
     * @param nextDir Direction index (0-7) for the next move
     * @return Packed costs: (turnCost << 16) | alternationCost
     */
    private static int getCombinedTurnCosts(Int2IntOpenHashMap parents, int current, int nextDir)
    {
        // Get parent chain - need current, parent, grandparent for alternation
        // and up to TURN_LOOKBACK_DEPTH ancestors for turn cost
        int parent = parents.get(current);
        if (parent == -1 || parent == -2) {
            return 0;  // No parent, no costs
        }

        int grandparent = parents.get(parent);

        // Extract coordinates for shared use
        int currentX = WorldPointUtil.getCompressedX(current);
        int currentY = WorldPointUtil.getCompressedY(current);
        int parentX = WorldPointUtil.getCompressedX(parent);
        int parentY = WorldPointUtil.getCompressedY(parent);

        // Direction from parent -> current (the move we just made)
        int currentDir = getDirectionIndex(parentX, parentY, currentX, currentY);

        // ===== ALTERNATION COST =====
        int alternationCost = 0;
        if (grandparent != -1 && grandparent != -2 && currentDir != -1) {
            int grandX = WorldPointUtil.getCompressedX(grandparent);
            int grandY = WorldPointUtil.getCompressedY(grandparent);
            int prevDir = getDirectionIndex(grandX, grandY, parentX, parentY);

            if (prevDir != -1 && prevDir != currentDir && nextDir == prevDir) {
                // Check if adjacent directions (within 45°)
                int prevHeading = DIRECTION_TO_HEADING[prevDir];
                int currentHeading = DIRECTION_TO_HEADING[currentDir];
                int headingDiff = Math.abs(prevHeading - currentHeading);
                if (headingDiff > 8) headingDiff = 16 - headingDiff;
                if (headingDiff <= 2) {
                    alternationCost = 25;
                }
            }
        }

        // ===== TURN COST =====
        // Continue walking back to find ancestor at TURN_LOOKBACK_DEPTH
        int ancestor = parent;
        int prevAncestor = current;

        // Already walked 1 step (current -> parent), need TURN_LOOKBACK_DEPTH - 1 more
        for (int i = 1; i < TURN_LOOKBACK_DEPTH; i++) {
            int next = parents.get(ancestor);
            if (next == -1 || next == -2) {
                break;
            }
            prevAncestor = ancestor;
            ancestor = next;
        }

        int turnCost = 0;
        if (ancestor != current) {
            int ancestorX = WorldPointUtil.getCompressedX(ancestor);
            int ancestorY = WorldPointUtil.getCompressedY(ancestor);
            int prevX = WorldPointUtil.getCompressedX(prevAncestor);
            int prevY = WorldPointUtil.getCompressedY(prevAncestor);

            int startDir = getDirectionIndex(ancestorX, ancestorY, prevX, prevY);
            if (startDir != -1) {
                int startHeading = DIRECTION_TO_HEADING[startDir];
                int endHeading = DIRECTION_TO_HEADING[nextDir];
                int headingDiff = Math.abs(endHeading - startHeading);
                if (headingDiff > 8) headingDiff = 16 - headingDiff;
                turnCost = TURN_COSTS[headingDiff];
            }
        }

        return (turnCost << 16) | alternationCost;
    }

    /**
     * @deprecated Use getCombinedTurnCosts for better performance
     */
    @Deprecated
    private static int getTurnCost(Int2IntOpenHashMap parents, int current, int nextDir)
    {
        return getCombinedTurnCosts(parents, current, nextDir) >>> 16;
    }

    /**
     * Calculates perpendicular distance from a point to the ideal straight line between start and target.
     * Used as a tie-breaker to prefer paths that stay close to the direct route.
     * OPTIMIZED: Uses integer math only (cross product magnitude).
     *
     * @return Approximate perpendicular distance (not exact, but sufficient for comparison)
     */
    private static int getCrossTrackDistance(int px, int py, int startX, int startY, int targetX, int targetY)
    {
        // Vector from start to target
        int dx = targetX - startX;
        int dy = targetY - startY;

        // Vector from start to point
        int dpx = px - startX;
        int dpy = py - startY;

        // Cross product magnitude = |dx*dpy - dy*dpx| gives area of parallelogram
        // Divided by line length gives perpendicular distance, but we skip the division
        // since we only need relative comparison (all paths use same start/target)
        int cross = Math.abs(dx * dpy - dy * dpx);

        // Normalize roughly by line length to keep penalty reasonable
        // Use max(|dx|,|dy|) as cheap approximation of length
        int lineLen = Math.max(Math.abs(dx), Math.abs(dy));
        if (lineLen == 0) return 0;

        return cross / lineLen;
    }

    /**
     * Checks if a tile type is a bad/hazardous water type.
     * OPTIMIZED: O(1) array lookup instead of O(n) loop.
     */
    private static boolean isBadTileType(byte tileType) {
        return IS_BAD_TILE[tileType & 0xFF];
    }

    /**
     * Converts full tile path to waypoints using sliding window heading detection.
     *
     * Uses a sliding window to calculate "local direction" over recent tiles. This:
     * - Smooths wobble (EAST/SE alternation averages to ESE over the window)
     * - Detects actual turns quickly (within window size tiles)
     *
     * Also checks path deviation - if any path tile strays too far from the straight line
     * between segment start and current position, forces a waypoint to keep boat on path.
     */
    public static List<Waypoint> convertToWaypoints(List<WorldPoint> path)
    {
        MethodProfiler.begin("BoatPathing.convertToWaypoints");
        try {
            if (path.size() < 2) {
                return new ArrayList<>();
            }

            List<Waypoint> waypoints = new ArrayList<>();

            // Window size for smoothing wobble while detecting turns quickly
            final int WINDOW_SIZE = 4;
            // Maximum distance any path tile can be from the straight line before forcing a waypoint
            final int MAX_DEVIATION = 3;
            // Maximum segment length before forcing a waypoint
            final int MAX_SEGMENT_LENGTH = 25;

            // Add starting waypoint so boat starts in correct direction
            Heading startHeading = Heading.getOptimalHeading(path.get(0), path.get(Math.min(WINDOW_SIZE, path.size() - 1)));
            waypoints.add(new Waypoint(path.get(0), startHeading));

            // Track segment start for heading calculation
            int segmentStartIndex = 0;
            Heading segmentStartHeading = startHeading;

            for (int i = WINDOW_SIZE; i < path.size(); i++) {
                // Calculate local heading over sliding window
                int windowStart = i - WINDOW_SIZE;
                Heading localHeading = Heading.getOptimalHeading(path.get(windowStart), path.get(i));

                // Compare current local heading to segment START heading
                int diff = getHeadingDifference(localHeading, segmentStartHeading);

                // Check if segment is too long
                int segmentLength = i - segmentStartIndex;

                // Check path deviation - does the actual path stray from straight line?
                boolean deviationExceeded = checkPathDeviation(path, segmentStartIndex, i);

                // Create waypoint if: heading changed significantly, segment too long, or path deviates
                if (diff > 1 || segmentLength >= MAX_SEGMENT_LENGTH || deviationExceeded) {
                    // Place waypoint at position where turn/deviation was detected
                    WorldPoint turnPoint = path.get(windowStart);
                    Heading segmentHeading = Heading.getOptimalHeading(path.get(segmentStartIndex), turnPoint);
                    waypoints.add(new Waypoint(turnPoint, segmentHeading));

                    // Start new segment
                    segmentStartIndex = windowStart;
                    segmentStartHeading = localHeading;
                }
            }

            // Final waypoint
            WorldPoint last = path.get(path.size() - 1);
            Heading finalHeading = Heading.getOptimalHeading(path.get(segmentStartIndex), last);
            waypoints.add(new Waypoint(last, finalHeading));

            return waypoints;
        } finally {
            MethodProfiler.end("BoatPathing.convertToWaypoints");
        }
    }

    /**
     * Checks if any path tile between start and end deviates more than maxDist
     * from the straight line between those two points.
     */
    private static boolean checkPathDeviation(List<WorldPoint> path, int startIdx, int endIdx)
    {
        WorldPoint start = path.get(startIdx);
        WorldPoint end = path.get(endIdx);

        // Line vector
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double lineLength = Math.sqrt(dx * dx + dy * dy);

        if (lineLength < 1) {
            return false;  // Start and end are same point
        }

        // Check each intermediate point
        for (int i = startIdx + 1; i < endIdx; i++) {
            WorldPoint p = path.get(i);

            // Calculate perpendicular distance from point to line
            // Using formula: |((y2-y1)*px - (x2-x1)*py + x2*y1 - y2*x1)| / sqrt((y2-y1)^2 + (x2-x1)^2)
            double dist = Math.abs(dy * p.getX() - dx * p.getY() + end.getX() * start.getY() - end.getY() * start.getX()) / lineLength;

            if (dist > 2) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calculates the minimum heading difference (0-8) accounting for wrap-around.
     * Heading values are 0-15 in a circle, so diff of 15 is actually 1 step.
     */
    private static int getHeadingDifference(Heading a, Heading b)
    {
        int diff = Math.abs(a.getValue() - b.getValue());
        if (diff > 8) {
            diff = 16 - diff;  // Wrap around the circle
        }
        return diff;
    }
}

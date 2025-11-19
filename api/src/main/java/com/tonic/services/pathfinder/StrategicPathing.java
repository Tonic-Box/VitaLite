package com.tonic.services.pathfinder;

import com.tonic.Static;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.handlers.GenericHandlerBuilder;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.services.pathfinder.local.LocalCollisionMap;
import com.tonic.util.handler.StepHandler;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

import java.util.*;
import java.util.function.Predicate;

/**
 * Strategic Pathfinding API - Provides advanced pathfinding capabilities with support for
 * avoiding and/or navigating around dangerous and impassible tiles.
 */
public class StrategicPathing {

    private static final HashSet<WorldPoint> EMPTY_SET = new HashSet<>();

    private static final int[][] DIRECTIONS_MAP = {
            {-2, 0},   // Far West
            {0, 2},    // Far North
            {2, 0},    // Far East
            {0, -2},   // Far South
            {1, 0},    // East
            {0, 1},    // North
            {-1, 0},   // West
            {0, -1},   // South
            {1, 1},    // NE diagonal
            {-1, -1},  // SW diagonal
            {-1, 1},   // NW diagonal
            {1, -1},   // SE diagonal
            {-2, 2},   // Far NW diagonal
            {-2, -2},  // Far SW diagonal
            {2, 2},    // Far NE diagonal
            {2, -2},   // Far SE diagonal
            {-2, -1},  // West-South L
            {-2, 1},   // West-North L
            {-1, -2},  // South-West L
            {-1, 2},   // North-West L
            {1, -2},   // South-East L
            {1, 2},    // North-East L
            {2, -1},   // East-South L
            {2, 1}     // East-North L
    };

    /**
     * Executes movement along the specified path (For threaded contexts)
     *
     * @param path The list of WorldPoints representing the path to follow.
     */
    public static void execute(List<WorldPoint> path) {
        StepHandler handler = getStepHandler(path);
        handler.execute();
    }

    /**
     * Creates a StepHandler for the specified path (For state machine non-threaded contexts)
     *
     * @param path The list of WorldPoints representing the path to follow.
     * @return A StepHandler that can be executed to follow the path.
     */
    public static StepHandler getStepHandler(List<WorldPoint> path) {
        return GenericHandlerBuilder.get()
                .addDelayUntil(context -> {
                    if(!MovementAPI.isRunEnabled()) {
                        MovementAPI.toggleRun();
                    }

                    int index = context.getOrDefault("stepIndex", -1) + 1;
                    if (index >= path.size()) {
                        return true;
                    }

                    MovementAPI.walkToWorldPoint(path.get(index));
                    context.put("stepIndex", index);
                    return false;
                })
                .build();
    }

    /**
     * Steps along the given path by one step.
     *
     * @param path The list of WorldPoints representing the path to follow.
     */
    public static void stepAlong(List<WorldPoint> path) {
        stepAlong(path, wp -> false);
    }

    /**
     * Steps along the given path by one step conditionally
     *
     * @param path          The list of WorldPoints representing the path to follow.
     * @param stopCondition A predicate that determines whether to stop before taking the next step.
     */
    public static void stepAlong(List<WorldPoint> path, Predicate<WorldPoint> stopCondition) {
        if(path == null || path.isEmpty()) {
            return;
        }
        if(!MovementAPI.isRunEnabled()) {
            MovementAPI.toggleRun();
        }
        WorldPoint playerPos = playerPosition();
        int index = 0;
        if(playerPos.equals(path.get(index)))
        {
            index = 1;
        }
        WorldPoint nextPoint = path.get(index);
        if(stopCondition.test(nextPoint)) {
            return;
        }
        MovementAPI.walkToWorldPoint(nextPoint);
    }

    // ============================================
    // Public API
    // ============================================

    /**
     * Finds a path to the specified goal point from the player's current position.
     *
     * @param goal The target WorldPoint to reach.
     * @return A list of WorldPoints representing the path, or null if no path is found.
     */
    public static List<WorldPoint> pathTo(WorldPoint goal) {
        return pathToGoalSet(new HashSet<>(Collections.singletonList(goal)), EMPTY_SET, EMPTY_SET, playerPosition());
    }

    /**
     * Finds a path to the specified goal point from the player's current position,
     * avoiding dangerous points.
     *
     * @param goal      The target WorldPoint to reach.
     * @param dangerous A set of WorldPoints to avoid.
     * @return A list of WorldPoints representing the path, or null if no path is found.
     */
    public static List<WorldPoint> pathTo(WorldPoint goal, HashSet<WorldPoint> dangerous) {
        return pathToGoalSet(new HashSet<>(Collections.singletonList(goal)), dangerous, EMPTY_SET, playerPosition());
    }

    /**
     * Finds a path to the specified goal point from the player's current position,
     * avoiding dangerous and impassible points.
     *
     * @param goal        The target WorldPoint to reach.
     * @param dangerous   A set of WorldPoints to avoid.
     * @param impassible  A set of WorldPoints that cannot be traversed.
     * @return A list of WorldPoints representing the path, or null if no path is found.
     */
    public static List<WorldPoint> pathTo(WorldPoint goal, HashSet<WorldPoint> dangerous, HashSet<WorldPoint> impassible) {
        return pathToGoalSet(new HashSet<>(Collections.singletonList(goal)), dangerous, impassible, playerPosition());
    }

    /**
     * Finds a path to any of the specified goal points from the player's current position.
     * @param goalSet A set of target WorldPoints to reach.
     * @return A list of WorldPoints representing the path, or null if no path is found.
     */
    public static List<WorldPoint> pathToSet(HashSet<WorldPoint> goalSet) {
        return pathToGoalSet(goalSet, EMPTY_SET, EMPTY_SET, playerPosition());
    }

    /**
     * Finds a path to any of the specified goal points from the player's current position,
     * avoiding dangerous points.
     *
     * @param goalSet   A set of target WorldPoints to reach.
     * @param dangerous A set of WorldPoints to avoid.
     * @param impassible A set of WorldPoints that cannot be traversed.
     * @return A list of WorldPoints representing the path, or null if no path is found.
     */
    public static List<WorldPoint> pathToSet(HashSet<WorldPoint> goalSet, HashSet<WorldPoint> dangerous, HashSet<WorldPoint> impassible) {
        return pathToGoalSet(goalSet, dangerous, impassible, playerPosition());
    }

    // ============================================
    // Main Pathfinding Algorithm (Optimized)
    // ============================================

    /**
     * Core pathfinding algorithm to find a path to any goal in the goal set,
     * avoiding dangerous and impassible tiles.
     *
     * @param goalSet     A set of target WorldPoints to reach.
     * @param dangerous   A set of WorldPoints to avoid.
     * @param impassible  A set of WorldPoints that cannot be traversed.
     * @param starting    The starting WorldPoint for pathfinding.
     * @return A list of WorldPoints representing the path, or null if no path is found.
     */
    public static List<WorldPoint> pathToGoalSet(
            HashSet<WorldPoint> goalSet,
            HashSet<WorldPoint> dangerous,
            HashSet<WorldPoint> impassible,
            WorldPoint starting) {

        // Sanity checks
        if (goalSet == null || goalSet.isEmpty()) {
            return null;
        }
        if (starting == null) {
            return null;
        }
        if (dangerous == null) {
            dangerous = EMPTY_SET;
        }
        if (impassible == null) {
            impassible = EMPTY_SET;
        }

        HashSet<WorldPoint> finalDangerous = dangerous;
        HashSet<WorldPoint> finalImpassible = impassible;
        return Static.invoke(() -> {

            // Get client and world view with null checks
            Client client = Static.getClient();
            if (client == null) {
                return null;
            }

            WorldView wv = client.getTopLevelWorldView();
            if (wv == null || wv.getCollisionMaps() == null) {
                return null;
            }

            int plane = starting.getPlane();
            if (plane < 0 || plane >= wv.getCollisionMaps().length || wv.getCollisionMaps()[plane] == null) {
                return null;
            }

            // Get scene bounds for boundary checking
            int baseX = wv.getBaseX();
            int baseY = wv.getBaseY();
            int[][] flags = wv.getCollisionMaps()[plane].getFlags();
            if (flags == null || flags.length == 0) {
                return null;
            }
            int sceneSize = flags.length; // typically 104

            // Convert WorldPoint sets to packed long coordinates for fast lookups
            LongOpenHashSet goalPacked = packWorldPoints(goalSet);
            LongOpenHashSet dangerousPacked = packWorldPoints(finalDangerous);
            LongOpenHashSet impassiblePacked = packWorldPoints(finalImpassible);

            // Create collision map instance for proper collision checking
            LocalCollisionMap collisionMap = new LocalCollisionMap(false);

            // Use boolean array for visited tracking (much faster than HashSet)
            boolean[][] visited = new boolean[sceneSize][sceneSize];

            // Queue using primitive coordinates
            ArrayDeque<PrimitiveNode> queue = new ArrayDeque<>();

            int startX = starting.getX();
            int startY = starting.getY();
            int startSceneX = startX - baseX;
            int startSceneY = startY - baseY;

            visited[startSceneX][startSceneY] = true;
            queue.add(new PrimitiveNode(startX, startY, null));

            while (!queue.isEmpty()) {
                PrimitiveNode current = queue.poll();
                int currentX = current.x;
                int currentY = current.y;

                // Check if we reached a goal (using packed coordinate)
                long currentPacked = packCoords(currentX, currentY);
                if (goalPacked.contains(currentPacked)) {
                    // Reconstruct path from primitive coordinates
                    List<WorldPoint> path = new ArrayList<>();
                    PrimitiveNode node = current;
                    while (node != null) {
                        path.add(new WorldPoint(node.x, node.y, plane));
                        node = node.previous;
                    }
                    Collections.reverse(path);
                    path.remove(0); // Remove starting position
                    return path;
                }

                // Explore all directions
                for (int[] direction : DIRECTIONS_MAP) {
                    int dx = direction[0];
                    int dy = direction[1];

                    if (dx == 0 && dy == 0) {
                        continue;
                    }

                    // Calculate next position with primitives (no WorldPoint creation yet)
                    int nextX = currentX + dx;
                    int nextY = currentY + dy;

                    // Bounds check on primitives BEFORE any object creation
                    if (nextX < baseX || nextX >= baseX + sceneSize ||
                            nextY < baseY || nextY >= baseY + sceneSize) {
                        continue;
                    }

                    // Scene-local coordinates for visited array
                    int nextSceneX = nextX - baseX;
                    int nextSceneY = nextY - baseY;

                    // Check visited using fast array lookup
                    if (visited[nextSceneX][nextSceneY]) {
                        continue;
                    }

                    // Pack coordinates for set lookups
                    long nextPacked = packCoords(nextX, nextY);

                    // Check dangerous/impassible using packed coordinates
                    if (impassiblePacked.contains(nextPacked) || dangerousPacked.contains(nextPacked)) {
                        continue;
                    }

                    // Check movement obstruction using primitive coordinates
                    boolean obstructed = false;

                    // Single-tile cardinal movements
                    if (dx == 1 && dy == 0) {
                        obstructed = collisionMap.e(currentX, currentY, plane);
                    } else if (dx == -1 && dy == 0) {
                        obstructed = collisionMap.w(currentX, currentY, plane);
                    } else if (dx == 0 && dy == 1) {
                        obstructed = collisionMap.n(currentX, currentY, plane);
                    } else if (dx == 0 && dy == -1) {
                        obstructed = collisionMap.s(currentX, currentY, plane);
                    }
                    // Far cardinal movements
                    else if (dx == -2 && dy == 0) {
                        obstructed = farWObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                    } else if (dx == 2 && dy == 0) {
                        obstructed = farEObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                    } else if (dx == 0 && dy == -2) {
                        obstructed = farSObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                    } else if (dx == 0 && dy == 2) {
                        obstructed = farNObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                    }
                    // L-shaped movements
                    else if (Math.abs(dx) + Math.abs(dy) == 3) {
                        if (dx == 1 && dy == 2) {
                            obstructed = northEastLObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        } else if (dx == 2 && dy == 1) {
                            obstructed = eastNorthLObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        } else if (dx == 2 && dy == -1) {
                            obstructed = eastSouthLObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        } else if (dx == 1 && dy == -2) {
                            obstructed = southEastLObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        } else if (dx == -1 && dy == -2) {
                            obstructed = southWestLObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        } else if (dx == -2 && dy == -1) {
                            obstructed = westSouthLObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        } else if (dx == -2 && dy == 1) {
                            obstructed = westNorthLObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        } else if (dx == -1 && dy == 2) {
                            obstructed = northWestLObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        }
                    }
                    // Diagonal and far diagonal movements
                    else {
                        if (dx == 1 && dy == -1) {
                            obstructed = seObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        } else if (dx == 1 && dy == 1) {
                            obstructed = neObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        } else if (dx == -1 && dy == 1) {
                            obstructed = nwObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        } else if (dx == -1 && dy == -1) {
                            obstructed = swObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        } else if (dx == -2 && dy == -2) {
                            obstructed = farSWObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        } else if (dx == -2 && dy == 2) {
                            obstructed = farNWObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        } else if (dx == 2 && dy == -2) {
                            obstructed = farSEObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        } else if (dx == 2 && dy == 2) {
                            obstructed = farNEObstructed(currentX, currentY, plane, impassiblePacked, collisionMap);
                        }
                    }

                    if (obstructed) {
                        continue;
                    }

                    // Mark as visited and add to queue
                    visited[nextSceneX][nextSceneY] = true;
                    queue.add(new PrimitiveNode(nextX, nextY, current));
                }
            }

            return null;
        });
    }

    // ============================================
    // Coordinate Packing Utilities
    // ============================================

    /**
     * Packs x,y coordinates into a single long for efficient storage
     */
    private static long packCoords(int x, int y) {
        return ((long) x << 32) | ((long) y & 0xFFFFFFFFL);
    }

    /**
     * Converts a HashSet of WorldPoints to packed long coordinates
     */
    private static LongOpenHashSet packWorldPoints(HashSet<WorldPoint> points) {
        LongOpenHashSet packed = new LongOpenHashSet(points.size());
        for (WorldPoint point : points) {
            packed.add(packCoords(point.getX(), point.getY()));
        }
        return packed;
    }

    // ============================================
    // Single Tile Diagonal Obstruction Checks (Optimized)
    // ============================================

    static boolean nwObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x - 1, y)) ||
            collision.w(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x, y + 1)) ||
            collision.n(x, y, plane);
    }

    static boolean neObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x + 1, y)) ||
            collision.e(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x, y + 1)) ||
            collision.n(x, y, plane);
    }

    static boolean seObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x + 1, y)) ||
            collision.e(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x, y - 1)) ||
            collision.s(x, y, plane);
    }

    static boolean swObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x - 1, y)) ||
            collision.w(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x, y - 1)) ||
            collision.s(x, y, plane);
    }

    // ============================================
    // Far Cardinal Direction Obstruction Checks (Optimized)
    // ============================================

    static boolean farNObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        // Check intermediate tile and both movement steps
        if (impassible.contains(packCoords(x, y + 1))) {
            return true;
        }
        // Check first step: current -> intermediate
        if (collision.n(x, y, plane)) {
            return true;
        }
        // Check second step: intermediate -> destination
        return collision.n(x, y + 1, plane);
    }

    static boolean farSObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        // Check intermediate tile and both movement steps
        if (impassible.contains(packCoords(x, y - 1))) {
            return true;
        }
        // Check first step: current -> intermediate
        if (collision.s(x, y, plane)) {
            return true;
        }
        // Check second step: intermediate -> destination
        return collision.s(x, y - 1, plane);
    }

    static boolean farEObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        // Check intermediate tile and both movement steps
        if (impassible.contains(packCoords(x + 1, y))) {
            return true;
        }
        // Check first step: current -> intermediate
        if (collision.e(x, y, plane)) {
            return true;
        }
        // Check second step: intermediate -> destination
        return collision.e(x + 1, y, plane);
    }

    static boolean farWObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        // Check intermediate tile and both movement steps
        if (impassible.contains(packCoords(x - 1, y))) {
            return true;
        }
        // Check first step: current -> intermediate
        if (collision.w(x, y, plane)) {
            return true;
        }
        // Check second step: intermediate -> destination
        return collision.w(x - 1, y, plane);
    }

    // ============================================
    // Far Diagonal Obstruction Checks (2 tiles) (Optimized)
    // ============================================

    static boolean farSWObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x - 1, y - 2))) {
            return true;
        }
        if (impassible.contains(packCoords(x - 2, y - 1))) {
            return true;
        }
        if (impassible.contains(packCoords(x, y - 1)) ||
            collision.s(x, y, plane)) {
            return true;
        }
        if (impassible.contains(packCoords(x - 1, y)) ||
            collision.w(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x - 1, y - 1)) ||
            collision.sw(x, y, plane);
    }

    static boolean farNWObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x - 1, y + 2))) {
            return true;
        }
        if (impassible.contains(packCoords(x - 2, y + 1))) {
            return true;
        }
        if (impassible.contains(packCoords(x, y + 1)) ||
            collision.n(x, y, plane)) {
            return true;
        }
        if (impassible.contains(packCoords(x - 1, y)) ||
            collision.w(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x - 1, y + 1)) ||
            collision.nw(x, y, plane);
    }

    static boolean farNEObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x + 1, y + 2))) {
            return true;
        }
        if (impassible.contains(packCoords(x + 2, y + 1))) {
            return true;
        }
        if (impassible.contains(packCoords(x, y + 1)) ||
            collision.n(x, y, plane)) {
            return true;
        }
        if (impassible.contains(packCoords(x + 1, y)) ||
            collision.e(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x + 1, y + 1)) ||
            collision.ne(x, y, plane);
    }

    static boolean farSEObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x + 1, y - 2))) {
            return true;
        }
        if (impassible.contains(packCoords(x + 2, y - 1))) {
            return true;
        }
        if (impassible.contains(packCoords(x, y - 1)) ||
            collision.s(x, y, plane)) {
            return true;
        }
        if (impassible.contains(packCoords(x + 1, y)) ||
            collision.e(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x + 1, y - 1)) ||
            collision.se(x, y, plane);
    }

    // ============================================
    // L-Shaped Movement Obstruction Checks (Optimized)
    // ============================================

    static boolean northEastLObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x + 1, y + 1)) ||
            collision.ne(x, y, plane)) {
            return true;
        }
        if (impassible.contains(packCoords(x, y + 1)) ||
            collision.n(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x, y + 2)) ||
            collision.n(x, y + 1, plane);
    }

    static boolean eastNorthLObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x + 1, y + 1)) ||
            collision.ne(x, y, plane)) {
            return true;
        }
        if (impassible.contains(packCoords(x + 1, y)) ||
            collision.e(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x + 2, y)) ||
            collision.e(x + 1, y, plane);
    }

    static boolean eastSouthLObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x + 1, y - 1)) ||
            collision.se(x, y, plane)) {
            return true;
        }
        if (impassible.contains(packCoords(x + 1, y)) ||
            collision.e(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x + 2, y)) ||
            collision.e(x + 1, y, plane);
    }

    static boolean southEastLObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x + 1, y - 1)) ||
            collision.se(x, y, plane)) {
            return true;
        }
        if (impassible.contains(packCoords(x, y - 1)) ||
            collision.s(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x, y - 2)) ||
            collision.s(x, y - 1, plane);
    }

    static boolean southWestLObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x - 1, y - 1)) ||
            collision.sw(x, y, plane)) {
            return true;
        }
        if (impassible.contains(packCoords(x, y - 1)) ||
            collision.s(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x, y - 2)) ||
            collision.s(x, y - 1, plane);
    }

    static boolean westSouthLObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x - 1, y - 1)) ||
            collision.sw(x, y, plane)) {
            return true;
        }
        if (impassible.contains(packCoords(x - 1, y)) ||
            collision.w(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x - 2, y)) ||
            collision.w(x - 1, y, plane);
    }

    static boolean westNorthLObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x - 1, y + 1)) ||
            collision.nw(x, y, plane)) {
            return true;
        }
        if (impassible.contains(packCoords(x - 1, y)) ||
            collision.w(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x - 2, y)) ||
            collision.w(x - 1, y, plane);
    }

    static boolean northWestLObstructed(int x, int y, int plane, LongOpenHashSet impassible, LocalCollisionMap collision) {
        if (impassible.contains(packCoords(x - 1, y + 1)) ||
            collision.nw(x, y, plane)) {
            return true;
        }
        if (impassible.contains(packCoords(x, y + 1)) ||
            collision.n(x, y, plane)) {
            return true;
        }
        return impassible.contains(packCoords(x, y + 2)) ||
            collision.n(x, y + 1, plane);
    }

    // ============================================
    // Utility Methods
    // ============================================

    private static WorldPoint playerPosition() {
        return PlayerEx.getLocal().getWorldPoint();
    }

    /**
     * Primitive coordinate node for pathfinding - avoids WorldPoint allocation in hot path
     */
    private static class PrimitiveNode {
        final int x;
        final int y;
        final PrimitiveNode previous;

        PrimitiveNode(int x, int y, PrimitiveNode previous) {
            this.x = x;
            this.y = y;
            this.previous = previous;
        }
    }
}

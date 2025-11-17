package com.tonic.services.pathfinder;

import com.tonic.Static;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.handlers.GenericHandlerBuilder;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.services.pathfinder.local.LocalCollisionMap;
import com.tonic.util.handler.StepHandler;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

/**
 * Strategic Pathfinding API
 */
public class StrategicPathing {

    private static final HashSet<WorldPoint> EMPTY_SET = new HashSet<>();

    public static final int[][] DIRECTIONS_MAP = {
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

    public static List<WorldPoint> pathToSet(HashSet<WorldPoint> goalSet) {
        return pathToGoalSet(goalSet, EMPTY_SET, EMPTY_SET, playerPosition());
    }

    public static List<WorldPoint> pathToSet(HashSet<WorldPoint> goalSet, HashSet<WorldPoint> dangerous, HashSet<WorldPoint> impassible) {
        return pathToGoalSet(goalSet, dangerous, impassible, playerPosition());
    }

    // ============================================
    // Main Pathfinding Algorithm
    // ============================================

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

        // Create collision map instance for proper collision checking
        LocalCollisionMap collisionMap = new LocalCollisionMap();

        ArrayDeque<Node> queue = new ArrayDeque<>();
        HashSet<WorldPoint> visited = new HashSet<>();
        visited.add(starting);
        queue.add(new Node(starting));

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            if (goalSet.contains(current.getData())) {
                List<WorldPoint> path = new ArrayList<>();
                while (current != null) {
                    path.add(current.getData());
                    current = current.getPrevious();
                }
                Collections.reverse(path);
                path.remove(0); // Remove starting position
                return path;
            }

            for (int[] direction : DIRECTIONS_MAP) {
                int x = direction[0];
                int y = direction[1];

                if (x == 0 && y == 0) {
                    continue;
                }

                WorldPoint currentPoint = current.getData();
                WorldPoint nextPoint = currentPoint.dx(x).dy(y);

                // Check scene bounds
                if (nextPoint.getX() < baseX || nextPoint.getX() >= baseX + sceneSize ||
                    nextPoint.getY() < baseY || nextPoint.getY() >= baseY + sceneSize ||
                    nextPoint.getPlane() != plane) {
                    continue;
                }

                if (impassible.contains(nextPoint) ||
                        dangerous.contains(nextPoint) ||
                        visited.contains(nextPoint)) {
                    continue;
                }

                // Check movement obstruction using LocalCollisionMap
                if (x == -2 && y == 0) {
                    if (farWObstructed(currentPoint, impassible, collisionMap)) continue;
                } else if (x == 2 && y == 0) {
                    if (farEObstructed(currentPoint, impassible, collisionMap)) continue;
                } else if (x == 0 && y == -2) {
                    if (farSObstructed(currentPoint, impassible, collisionMap)) continue;
                } else if (x == 0 && y == 2) {
                    if (farNObstructed(currentPoint, impassible, collisionMap)) continue;
                } else if (Math.abs(x) + Math.abs(y) == 3) {
                    // L-shaped movements
                    if (x == 1 && y == 2) {
                        if (northEastLObstructed(currentPoint, impassible, collisionMap)) continue;
                    } else if (x == 2 && y == 1) {
                        if (eastNorthLObstructed(currentPoint, impassible, collisionMap)) continue;
                    } else if (x == 2 && y == -1) {
                        if (eastSouthLObstructed(currentPoint, impassible, collisionMap)) continue;
                    } else if (x == 1 && y == -2) {
                        if (southEastLObstructed(currentPoint, impassible, collisionMap)) continue;
                    } else if (x == -1 && y == -2) {
                        if (southWestLObstructed(currentPoint, impassible, collisionMap)) continue;
                    } else if (x == -2 && y == -1) {
                        if (westSouthLObstructed(currentPoint, impassible, collisionMap)) continue;
                    } else if (x == -2 && y == 1) {
                        if (westNorthLObstructed(currentPoint, impassible, collisionMap)) continue;
                    } else if (x == -1 && y == 2) {
                        if (northWestLObstructed(currentPoint, impassible, collisionMap)) continue;
                    }
                } else {
                    // Single or two-tile movements
                    if (x == 1 && y == -1) {
                        if (seObstructed(currentPoint, impassible, collisionMap)) continue;
                    } else if (x == 1 && y == 1) {
                        if (neObstructed(currentPoint, impassible, collisionMap)) continue;
                    } else if (x == -1 && y == 1) {
                        if (nwObstructed(currentPoint, impassible, collisionMap)) continue;
                    } else if (x == -1 && y == -1) {
                        if (swObstructed(currentPoint, impassible, collisionMap)) continue;
                    } else if (x == -2 && y == -2) {
                        if (farSWObstructed(currentPoint, impassible, collisionMap)) continue;
                    } else if (x == -2 && y == 2) {
                        if (farNWObstructed(currentPoint, impassible, collisionMap)) continue;
                    } else if (x == 2 && y == -2) {
                        if (farSEObstructed(currentPoint, impassible, collisionMap)) continue;
                    } else if (x == 2 && y == 2) {
                        if (farNEObstructed(currentPoint, impassible, collisionMap)) continue;
                    }
                }

                visited.add(nextPoint);
                queue.add(new Node(nextPoint, current));
            }
        }

        return null;
    }

    // ============================================
    // Single Tile Diagonal Obstruction Checks
    // ============================================

    static boolean nwObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(-1)) ||
            collision.w(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dy(1)) ||
            collision.n(starting.getX(), starting.getY(), starting.getPlane());
    }

    static boolean neObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(1)) ||
            collision.e(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dy(1)) ||
            collision.n(starting.getX(), starting.getY(), starting.getPlane());
    }

    static boolean seObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(1)) ||
            collision.e(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dy(-1)) ||
            collision.s(starting.getX(), starting.getY(), starting.getPlane());
    }

    static boolean swObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(-1)) ||
            collision.w(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dy(-1)) ||
            collision.s(starting.getX(), starting.getY(), starting.getPlane());
    }

    // ============================================
    // Far Cardinal Direction Obstruction Checks
    // ============================================

    static boolean farNObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        return impassible.contains(starting.dy(1)) ||
            collision.n(starting.getX(), starting.getY(), starting.getPlane());
    }

    static boolean farSObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        return impassible.contains(starting.dy(-1)) ||
            collision.s(starting.getX(), starting.getY(), starting.getPlane());
    }

    static boolean farEObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        return impassible.contains(starting.dx(1)) ||
            collision.e(starting.getX(), starting.getY(), starting.getPlane());
    }

    static boolean farWObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        return impassible.contains(starting.dx(-1)) ||
            collision.w(starting.getX(), starting.getY(), starting.getPlane());
    }

    // ============================================
    // Far Diagonal Obstruction Checks (2 tiles)
    // ============================================

    static boolean farSWObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(-1).dy(-2))) {
            return true;
        }
        if (impassible.contains(starting.dx(-2).dy(-1))) {
            return true;
        }
        if (impassible.contains(starting.dy(-1)) ||
            collision.s(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        if (impassible.contains(starting.dx(-1)) ||
            collision.w(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dx(-1).dy(-1)) ||
            collision.sw(starting.getX(), starting.getY(), starting.getPlane());
    }

    static boolean farNWObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(-1).dy(2))) {
            return true;
        }
        if (impassible.contains(starting.dx(-2).dy(1))) {
            return true;
        }
        if (impassible.contains(starting.dy(1)) ||
            collision.n(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        if (impassible.contains(starting.dx(-1)) ||
            collision.w(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dx(-1).dy(1)) ||
            collision.nw(starting.getX(), starting.getY(), starting.getPlane());
    }

    static boolean farNEObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(1).dy(2))) {
            return true;
        }
        if (impassible.contains(starting.dx(2).dy(1))) {
            return true;
        }
        if (impassible.contains(starting.dy(1)) ||
            collision.n(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        if (impassible.contains(starting.dx(1)) ||
            collision.e(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dx(1).dy(1)) ||
            collision.ne(starting.getX(), starting.getY(), starting.getPlane());
    }

    static boolean farSEObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(1).dy(-2))) {
            return true;
        }
        if (impassible.contains(starting.dx(2).dy(-1))) {
            return true;
        }
        if (impassible.contains(starting.dy(-1)) ||
            collision.s(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        if (impassible.contains(starting.dx(1)) ||
            collision.e(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dx(1).dy(-1)) ||
            collision.se(starting.getX(), starting.getY(), starting.getPlane());
    }

    // ============================================
    // L-Shaped Movement Obstruction Checks
    // ============================================

    static boolean northEastLObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(1).dy(1)) ||
            collision.ne(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        if (impassible.contains(starting.dy(1)) ||
            collision.n(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dy(2)) ||
            collision.n(starting.getX(), starting.getY() + 1, starting.getPlane());
    }

    static boolean eastNorthLObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(1).dy(1)) ||
            collision.ne(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        if (impassible.contains(starting.dx(1)) ||
            collision.e(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dx(2)) ||
            collision.e(starting.getX() + 1, starting.getY(), starting.getPlane());
    }

    static boolean eastSouthLObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(1).dy(-1)) ||
            collision.se(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        if (impassible.contains(starting.dx(1)) ||
            collision.e(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dx(2)) ||
            collision.e(starting.getX() + 1, starting.getY(), starting.getPlane());
    }

    static boolean southEastLObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(1).dy(-1)) ||
            collision.se(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        if (impassible.contains(starting.dy(-1)) ||
            collision.s(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dy(-2)) ||
            collision.s(starting.getX(), starting.getY() - 1, starting.getPlane());
    }

    static boolean southWestLObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(-1).dy(-1)) ||
            collision.sw(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        if (impassible.contains(starting.dy(-1)) ||
            collision.s(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dy(-2)) ||
            collision.s(starting.getX(), starting.getY() - 1, starting.getPlane());
    }

    static boolean westSouthLObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(-1).dy(-1)) ||
            collision.sw(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        if (impassible.contains(starting.dx(-1)) ||
            collision.w(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dx(-2)) ||
            collision.w(starting.getX() - 1, starting.getY(), starting.getPlane());
    }

    static boolean westNorthLObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(-1).dy(1)) ||
            collision.nw(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        if (impassible.contains(starting.dx(-1)) ||
            collision.w(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dx(-2)) ||
            collision.w(starting.getX() - 1, starting.getY(), starting.getPlane());
    }

    static boolean northWestLObstructed(WorldPoint starting, HashSet<WorldPoint> impassible, LocalCollisionMap collision) {
        if (impassible.contains(starting.dx(-1).dy(1)) ||
            collision.nw(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        if (impassible.contains(starting.dy(1)) ||
            collision.n(starting.getX(), starting.getY(), starting.getPlane())) {
            return true;
        }
        return impassible.contains(starting.dy(2)) ||
            collision.n(starting.getX(), starting.getY() + 1, starting.getPlane());
    }

    // ============================================
    // Utility Methods
    // ============================================

    private static WorldPoint playerPosition() {
        return PlayerEx.getLocal().getWorldPoint();
    }

    @Getter
    private static class Node {
        private final WorldPoint data;
        private final Node previous;

        public Node(WorldPoint data) {
            this(data, null);
        }

        public Node(WorldPoint data, Node previous) {
            this.data = data;
            this.previous = previous;
        }
    }
}

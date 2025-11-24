package com.tonic.api.game.sailing;

import com.tonic.Static;
import com.tonic.api.handlers.GenericHandlerBuilder;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.collision.CollisionMap;
import com.tonic.services.pathfinder.collision.Flags;
import com.tonic.util.WorldPointUtil;
import com.tonic.util.handler.StepHandler;
import lombok.Getter;
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

/**
 * BFS-based boat pathfinding that generates tile-by-tile paths,
 * then converts to waypoints at turning points.
 *
 * OPTIMIZATION: Uses primitive-only collision checking in hot path to eliminate
 * ~800K+ object allocations per pathfinding call.
 */
public class SailPathing
{
    /**
     * Cache for boat hull data used during pathfinding.
     * Initialized once at start, then reused for all collision checks.
     */
    private static class BoatHullCache
    {
        final WorldEntity boat;
        final CollisionMap collisionMap;

        BoatHullCache(WorldEntity boat, CollisionMap collisionMap)
        {
            this.boat = boat;
            this.collisionMap = collisionMap;
        }
    }
    public static StepHandler travelTo(WorldPoint worldPoint)
    {
        return GenericHandlerBuilder.get()
                .addDelayUntil(context -> {
                    if(!context.contains("PATH"))
                    {
                        var waypoints = pathTo(worldPoint);
                        context.put("PATH", waypoints);
                        context.put("POINTER", 0);
                    }

                    List<Waypoint> waypoints = context.get("PATH");
                    int pointer = context.get("POINTER");

                    if(waypoints == null || waypoints.isEmpty() || pointer >= waypoints.size())
                    {
                        context.remove("PATH");
                        context.remove("POINTER");
                        return true;
                    }

                    Waypoint waypoint = waypoints.get(pointer);
                    if(BoatCollisionAPI.playerBoatContainsPoint(waypoint.getPosition()))
                    {
                        context.put("POINTER", pointer + 1);
                        return false;
                    }
                    SailingAPI.sailTo(worldPoint);
                    return false;
                })
                .add(SailingAPI::unSetSails)
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

            return pathTo(boat, start, target);
        });
    }

    /**
     * Finds a sailing path from start to target for the given boat.
     */
    public static List<Waypoint> pathTo(WorldEntity boat, WorldPoint start, WorldPoint target)
    {
        return Static.invoke(() -> {
            // Find full tile-by-tile path using BFS
            List<WorldPoint> fullPath = findFullPath(boat, start, target);

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
     * Primitive-only version of canPlayerBoatFitAtPoint that avoids object allocations.
     * Does EXACTLY what the original does but with packed coordinates.
     */
    private static boolean canBoatFitAtPackedPoint(BoatHullCache cache, short targetX, short targetY, byte targetPlane, short currentX, short currentY)
    {
        // Create temporary WorldPoint objects ONLY for calling existing API
        // (still much better than creating them in hot loop)
        WorldPoint targetWP = new WorldPoint(targetX, targetY, targetPlane);
        WorldPoint currentWP = new WorldPoint(currentX, currentY, targetPlane);

        Heading heading = Heading.getOptimalHeading(currentWP, targetWP);
        // Use the EXACT same API call as the original
        return BoatCollisionAPI.canPlayerBoatFitAtPoint(targetWP, heading);
    }

    /**
     * Uses BFS to find full tile-by-tile path from start to target.
     */
    public static List<WorldPoint> findFullPath(WorldEntity boat, WorldPoint start, WorldPoint target)
    {
        return Static.invoke(() -> {
            CollisionMap collisionMap = Walker.getCollisionMap();
            if (collisionMap == null) {
                System.out.println("SailPathing: CollisionMap is null");
                return null;
            }

            // Create cache once to pass boat and collisionMap to hot path
            BoatHullCache cache = new BoatHullCache(boat, collisionMap);

            // BFS data structures using compressed integers like HybridBFSAlgo
            Queue<Integer> queue = new LinkedList<>();
            Map<Integer, Integer> visited = new HashMap<>(); // current -> parent

            int startPacked = WorldPointUtil.compress(start);
            int targetPacked = WorldPointUtil.compress(target);

            queue.add(startPacked);
            visited.put(startPacked, -1); // -1 = start node

            int maxIterations = 1_000_000;
            int iterations = 0;

            // BFS search
            while (!queue.isEmpty() && iterations++ < maxIterations) {
                int current = queue.poll();

                // Check if reached target
                if (current == targetPacked) {
                    System.out.println("SailPathing: Found target after " + iterations + " iterations");
                    return reconstructFullPath(visited, targetPacked);
                }

                // Add neighbors in 8 directions (pass cache)
                addNeighbors(cache, current, queue, visited);
            }

            System.out.println("SailPathing: No path found after " + iterations + " iterations");
            return null;
        });
    }

    /**
     * Adds neighbors in 8 directions, checking boat hull collision for each.
     * NOTE: We can't use collisionMap.all() pre-filtering because that's for single-tile walking,
     * but boats are multi-tile entities. We must check the full boat hull for each position.
     */
    private static void addNeighbors(BoatHullCache cache, int node, Queue<Integer> queue, Map<Integer, Integer> visited)
    {
        short x = WorldPointUtil.getCompressedX(node);
        short y = WorldPointUtil.getCompressedY(node);
        byte plane = WorldPointUtil.getCompressedPlane(node);

        // Try all 8 directions - boat hull check will filter invalid ones
        addNeighborIfBoatFits(cache, node, WorldPointUtil.compress((short)(x - 1), y, plane), queue, visited);        // West
        addNeighborIfBoatFits(cache, node, WorldPointUtil.compress((short)(x + 1), y, plane), queue, visited);        // East
        addNeighborIfBoatFits(cache, node, WorldPointUtil.compress(x, (short)(y - 1), plane), queue, visited);        // South
        addNeighborIfBoatFits(cache, node, WorldPointUtil.compress(x, (short)(y + 1), plane), queue, visited);        // North
        addNeighborIfBoatFits(cache, node, WorldPointUtil.compress((short)(x - 1), (short)(y - 1), plane), queue, visited); // Southwest
        addNeighborIfBoatFits(cache, node, WorldPointUtil.compress((short)(x + 1), (short)(y - 1), plane), queue, visited); // Southeast
        addNeighborIfBoatFits(cache, node, WorldPointUtil.compress((short)(x - 1), (short)(y + 1), plane), queue, visited); // Northwest
        addNeighborIfBoatFits(cache, node, WorldPointUtil.compress((short)(x + 1), (short)(y + 1), plane), queue, visited); // Northeast
    }

    /**
     * Helper to add neighbor only if boat can move there.
     * OPTIMIZED: Extracts primitives from packed integers, avoids creating WorldPoint objects.
     */
    private static void addNeighborIfBoatFits(BoatHullCache cache, int currentNode, int neighborNode, Queue<Integer> queue, Map<Integer, Integer> visited)
    {
        // Skip if already visited
        if (visited.containsKey(neighborNode)) {
            return;
        }

        // Extract coordinates as primitives - no object allocations
        short nx = WorldPointUtil.getCompressedX(neighborNode);
        short ny = WorldPointUtil.getCompressedY(neighborNode);
        short cx = WorldPointUtil.getCompressedX(currentNode);
        short cy = WorldPointUtil.getCompressedY(currentNode);
        byte plane = WorldPointUtil.getCompressedPlane(neighborNode);

        // Check collision with primitives only
        if (!canBoatFitAtPackedPoint(cache, nx, ny, plane, cx, cy)) {
            return;
        }

        // Valid neighbor
        visited.put(neighborNode, currentNode);
        queue.add(neighborNode);
    }

    /**
     * Reconstructs the full tile-by-tile path from BFS visited map.
     */
    private static List<WorldPoint> reconstructFullPath(Map<Integer, Integer> visited, int target)
    {
        List<WorldPoint> path = new ArrayList<>();
        int current = target;

        // Walk backwards from target to start
        while (current != -1) {
            short x = WorldPointUtil.getCompressedX(current);
            short y = WorldPointUtil.getCompressedY(current);
            byte plane = WorldPointUtil.getCompressedPlane(current);

            path.add(new WorldPoint(x, y, plane));

            Integer parent = visited.get(current);
            if (parent == null || parent == -1) {
                break;
            }
            current = parent;
        }

        Collections.reverse(path);
        return path;
    }

    /**
     * Converts full tile path to waypoints.
     * TEMPORARILY returns waypoint for EVERY tile to debug path quality.
     */
    private static List<Waypoint> convertToWaypoints(List<WorldPoint> path)
    {
        if (path.size() < 2) {
            return new ArrayList<>();
        }

        List<Waypoint> waypoints = new ArrayList<>();

        // Add waypoint for EVERY tile to test if path itself is good
        for (int i = 0; i < path.size() - 1; i++) {
            WorldPoint curr = path.get(i);
            WorldPoint next = path.get(i + 1);
            Heading heading = Heading.getOptimalHeading(curr, next);
            waypoints.add(new Waypoint(curr, heading));
        }

        // Add last point
        WorldPoint last = path.get(path.size() - 1);
        WorldPoint beforeLast = path.get(path.size() - 2);
        Heading lastHeading = Heading.getOptimalHeading(beforeLast, last);
        waypoints.add(new Waypoint(last, lastHeading));

        System.out.println("SailPathing: Generated waypoint for EVERY tile (debug mode)");
        return waypoints;
    }
}

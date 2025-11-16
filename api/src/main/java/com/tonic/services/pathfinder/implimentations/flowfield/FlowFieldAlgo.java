package com.tonic.services.pathfinder.implimentations.flowfield;

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
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntByteHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Flow field pathfinder with sparse storage and intelligent caching.
 * Optimized for repeated paths to the same destination.
 *
 * Performance characteristics:
 * - First path to goal: ~50ms (build flow field)
 * - Subsequent paths: <1ms (cache lookup + path following)
 * - Memory: ~50KB per cached destination
 */
public class FlowFieldAlgo implements IPathfinder
{
    private static final int MAX_NODES_EXPANDED = 500_000;
    private static final FlowFieldCache cache = new FlowFieldCache();

    private LocalCollisionMap localMap;
    @Getter
    private Teleport teleport;

    private int targetCompressed;
    private int[] worldAreaPoints;
    private boolean inInstance = false;
    private int playerStartPos;

    @Override
    public List<FlowFieldStep> find(WorldPoint target) {
        TransportLoader.refreshTransports();
        this.targetCompressed = WorldPointUtil.compress(target);
        this.worldAreaPoints = null;
        return find();
    }

    @Override
    public List<FlowFieldStep> find(WorldArea... worldAreas) {
        TransportLoader.refreshTransports();
        this.targetCompressed = -1;
        this.worldAreaPoints = WorldPointUtil.toCompressedPoints(worldAreas);
        return find();
    }

    @Override
    public List<FlowFieldStep> find(List<WorldArea> worldAreas) {
        TransportLoader.refreshTransports();
        this.targetCompressed = -1;
        this.worldAreaPoints = WorldPointUtil.toCompressedPoints(worldAreas.toArray(new WorldArea[0]));
        return find();
    }

    private List<FlowFieldStep> find() {
        if (Walker.getCollisionMap() == null) {
            Logger.error("[FlowField] Collision map is null");
            return new ArrayList<>();
        }

        try {
            Client client = Static.getClient();
            this.inInstance = client.getTopLevelWorldView().isInstance();
            this.playerStartPos = WorldPointUtil.compress(client.getLocalPlayer().getWorldLocation());

            if (inInstance) {
                localMap = new LocalCollisionMap();
            }

            // Try cache first for single WorldPoint targets
            if (targetCompressed != -1) {
                FlowField field = cache.get(targetCompressed);
                if (field != null) {
                    List<FlowFieldStep> path = followField(field);
                    if (!path.isEmpty()) {
                        Logger.info("[FlowField] Cache hit, path length: " + path.size());
                        return path;
                    }
                }
            }

            // Build new flow field
            List<Teleport> teleports = Teleport.buildTeleportLinks();

            Profiler.Start("Flow Field Build");
            FlowField field = buildFlowField();
            Profiler.StopMS();

            // Cache if single target
            if (targetCompressed != -1 && field.getTilesReachable() > 0) {
                cache.put(targetCompressed, field);
            }

            int playerCost = field.getCost(playerStartPos);

            // Check which starting point (player or valid teleport) provides best path
            // Filter out nearby teleports (< 20 tiles from player)
            Teleport bestTeleport = null;
            int bestStartCost = playerCost;
            int bestStartPos = playerStartPos;

            for (Teleport tp : teleports) {
                // Skip teleports that are too close to player (< 20 tiles)
                if (filterTeleports(tp.getDestination())) {
                    continue;
                }

                int tpDest = WorldPointUtil.compress(tp.getDestination());
                int tpCost = field.getCost(tpDest);

                // Only use teleport if it saves at least 20 steps
                if (tpCost < bestStartCost && (bestStartCost - tpCost) >= 20) {
                    bestStartCost = tpCost;
                    bestStartPos = tpDest;
                    bestTeleport = tp;
                }
            }

            Profiler.Start("Flow Field Follow");
            List<FlowFieldStep> path;

            if (bestTeleport != null) {
                // Teleport provides best path
                teleport = bestTeleport.copy();
                path = followFieldFrom(field, bestStartPos);
                Logger.info("[FlowField] Using teleport (saves " + (playerCost - bestStartCost) + " tiles): " + bestTeleport);
            } else {
                // Walking is best
                path = followField(field);
            }

            Profiler.StopMS();

            Logger.info("[FlowField] Player cost: " + playerCost + ", Best start cost: " + bestStartCost + ", Path length: " + path.size() + ", Tiles reachable: " + field.getTilesReachable());

            return path;

        } catch (Exception e) {
            Logger.error(e, "[FlowField] %e");
            return new ArrayList<>();
        }
    }

    /**
     * Builds a sparse flow field using Dijkstra from goal backwards.
     */
    private FlowField buildFlowField() {
        TIntIntHashMap costs = new TIntIntHashMap(10_000, 0.5f, -1, Integer.MAX_VALUE);
        TIntIntHashMap parents = new TIntIntHashMap(10_000, 0.5f, -1, -1);

        // Build reverse transport map once (destination -> transports)
        TIntObjectHashMap<ArrayList<Transport>> reverseTransports = buildReverseTransportMap();

        // Use dynamic queue that can grow
        TIntArrayList queue = new TIntArrayList(10_000);
        int head = 0;

        // Blacklist
        for (int i : Properties.getBlacklist()) {
            costs.put(i, Integer.MAX_VALUE - 1);
        }

        // Initialize from goal(s)
        if (targetCompressed != -1) {
            if (!Walker.getCollisionMap().walkable(targetCompressed)) {
                Logger.info("[FlowField] Target blocked");
                return new FlowField(targetCompressed, new TIntByteHashMap(), costs);
            }
            costs.put(targetCompressed, 0);
            parents.put(targetCompressed, -1);
            queue.add(targetCompressed);
        } else if (worldAreaPoints != null) {
            for (int areaPos : worldAreaPoints) {
                costs.put(areaPos, 0);
                parents.put(areaPos, -1);
                queue.add(areaPos);
            }
        }

        // BFS expansion from goal backwards
        int nodesExpanded = 0;
        while (head < queue.size() && nodesExpanded < MAX_NODES_EXPANDED) {
            int current = queue.get(head++);
            int currentCost = costs.get(current);

            nodesExpanded++;

            // Check for transports that lead TO this tile
            // Treat transport sources as "neighbors" during backward expansion
            expandTransportsBackwards(current, currentCost, costs, parents, queue, reverseTransports);

            short x = WorldPointUtil.getCompressedX(current);
            short y = WorldPointUtil.getCompressedY(current);
            byte plane = WorldPointUtil.getCompressedPlane(current);

            if (x > 6000) {
                if (inInstance) {
                    expandLocalBackwards(current, currentCost, x, y, plane, costs, parents, queue);
                }
                continue;
            }

            byte flags = Walker.getCollisionMap().all(x, y, plane);

            // Expand backwards (from goal towards all reachable tiles)
            if (flags == Flags.ALL) {
                // West
                int neighbor = WorldPointUtil.compress(x - 1, y, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);

                // East
                neighbor = WorldPointUtil.compress(x + 1, y, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);

                // South
                neighbor = WorldPointUtil.compress(x, y - 1, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);

                // North
                neighbor = WorldPointUtil.compress(x, y + 1, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);

                // Southwest
                neighbor = WorldPointUtil.compress(x - 1, y - 1, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);

                // Southeast
                neighbor = WorldPointUtil.compress(x + 1, y - 1, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);

                // Northwest
                neighbor = WorldPointUtil.compress(x - 1, y + 1, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);

                // Northeast
                neighbor = WorldPointUtil.compress(x + 1, y + 1, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);

                continue;
            }

            if (flags == Flags.NONE) {
                continue;
            }

            // Bitwise checks
            if ((flags & Flags.WEST) != 0) {
                int neighbor = WorldPointUtil.compress(x - 1, y, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
            }
            if ((flags & Flags.EAST) != 0) {
                int neighbor = WorldPointUtil.compress(x + 1, y, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
            }
            if ((flags & Flags.SOUTH) != 0) {
                int neighbor = WorldPointUtil.compress(x, y - 1, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
            }
            if ((flags & Flags.NORTH) != 0) {
                int neighbor = WorldPointUtil.compress(x, y + 1, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
            }
            if ((flags & Flags.SOUTHWEST) != 0) {
                int neighbor = WorldPointUtil.compress(x - 1, y - 1, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
            }
            if ((flags & Flags.SOUTHEAST) != 0) {
                int neighbor = WorldPointUtil.compress(x + 1, y - 1, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
            }
            if ((flags & Flags.NORTHWEST) != 0) {
                int neighbor = WorldPointUtil.compress(x - 1, y + 1, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
            }
            if ((flags & Flags.NORTHEAST) != 0) {
                int neighbor = WorldPointUtil.compress(x + 1, y + 1, plane);
                tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
            }
        }

        // Build direction map from costs
        TIntByteHashMap directions = new TIntByteHashMap(costs.size());

        costs.forEachEntry((pos, cost) -> {
            if (cost == 0) {
                return true;  // Goal has no direction
            }

            byte bestDir = findBestDirection(pos, costs);
            if (bestDir != FlowField.DIR_NONE) {
                directions.put(pos, bestDir);
            }
            return true;
        });

        int goalPos = targetCompressed != -1 ? targetCompressed :
                     (worldAreaPoints != null && worldAreaPoints.length > 0 ? worldAreaPoints[0] : -1);

        return new FlowField(goalPos, directions, costs);
    }

    private void expandLocalBackwards(int current, int currentCost, short x, short y, byte plane,
                                       TIntIntHashMap costs, TIntIntHashMap parents, TIntArrayList queue) {
        if (!localMap.w(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x - 1, y, plane);
            tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
        }
        if (!localMap.e(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x + 1, y, plane);
            tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
        }
        if (!localMap.n(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x, y + 1, plane);
            tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
        }
        if (!localMap.s(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x, y - 1, plane);
            tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
        }
        if (!localMap.nw(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x - 1, y + 1, plane);
            tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
        }
        if (!localMap.ne(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x + 1, y + 1, plane);
            tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
        }
        if (!localMap.sw(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x - 1, y - 1, plane);
            tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
        }
        if (!localMap.se(x, y, plane)) {
            int neighbor = WorldPointUtil.compress(x + 1, y - 1, plane);
            tryAddNeighbor(neighbor, current, currentCost, costs, parents, queue);
        }
    }

    private void tryAddNeighbor(int neighbor, int parent, int parentCost,
                                 TIntIntHashMap costs, TIntIntHashMap parents, TIntArrayList queue) {
        int existingCost = costs.get(neighbor);
        int newCost = parentCost + 1;

        if (newCost < existingCost) {
            costs.put(neighbor, newCost);
            parents.put(neighbor, parent);
            queue.add(neighbor);
        }
    }

    /**
     * During backward expansion, find all transports that lead TO the current tile.
     * Treat transport sources as "neighbors" with transport cost.
     */
    private void expandTransportsBackwards(int current, int currentCost,
                                            TIntIntHashMap costs, TIntIntHashMap parents, TIntArrayList queue,
                                            TIntObjectHashMap<ArrayList<Transport>> reverseTransports) {
        // Direct lookup: what transports lead to current tile?
        ArrayList<Transport> transports = reverseTransports.get(current);
        if (transports == null) return;

        for (Transport transport : transports) {
            int source = transport.getSource();

            // Duration-based cost: duration + 1 penalty for durations > 3
            int duration = transport.getDuration();
            int transportCost = (duration > 3) ? duration + 1 : duration;

            int existingCost = costs.get(source);
            int newCost = currentCost + transportCost;

            if (newCost < existingCost) {
                costs.put(source, newCost);
                parents.put(source, current);  // Parent is the destination
                queue.add(source);
            }
        }
    }

    /**
     * Builds a reverse transport map: destination -> list of transports leading to it.
     * Built once per flow field for O(1) lookup during expansion.
     */
    private TIntObjectHashMap<ArrayList<Transport>> buildReverseTransportMap() {
        TIntObjectHashMap<ArrayList<Transport>> reverseMap = new TIntObjectHashMap<>();

        TransportLoader.getTransports().forEachEntry((source, transports) -> {
            if (transports != null) {
                for (Transport transport : transports) {
                    int destination = transport.getDestination();
                    ArrayList<Transport> transportList = reverseMap.get(destination);
                    if (transportList == null) {
                        transportList = new ArrayList<>(2);
                        reverseMap.put(destination, transportList);
                    }
                    transportList.add(transport);
                }
            }
            return true;
        });

        return reverseMap;
    }

    /**
     * Finds best direction from position by looking at neighbor costs.
     */
    private byte findBestDirection(int position, TIntIntHashMap costs) {
        short x = WorldPointUtil.getCompressedX(position);
        short y = WorldPointUtil.getCompressedY(position);
        byte plane = WorldPointUtil.getCompressedPlane(position);

        int currentCost = costs.get(position);
        int lowestCost = currentCost;
        byte bestDir = FlowField.DIR_NONE;

        // Check all 8 directions, find lowest cost neighbor
        int neighbor = WorldPointUtil.compress(x - 1, y, plane);
        int cost = costs.get(neighbor);
        if (cost < lowestCost) { lowestCost = cost; bestDir = FlowField.DIR_WEST; }

        neighbor = WorldPointUtil.compress(x + 1, y, plane);
        cost = costs.get(neighbor);
        if (cost < lowestCost) { lowestCost = cost; bestDir = FlowField.DIR_EAST; }

        neighbor = WorldPointUtil.compress(x, y - 1, plane);
        cost = costs.get(neighbor);
        if (cost < lowestCost) { lowestCost = cost; bestDir = FlowField.DIR_SOUTH; }

        neighbor = WorldPointUtil.compress(x, y + 1, plane);
        cost = costs.get(neighbor);
        if (cost < lowestCost) { lowestCost = cost; bestDir = FlowField.DIR_NORTH; }

        neighbor = WorldPointUtil.compress(x - 1, y - 1, plane);
        cost = costs.get(neighbor);
        if (cost < lowestCost) { lowestCost = cost; bestDir = FlowField.DIR_SOUTHWEST; }

        neighbor = WorldPointUtil.compress(x + 1, y - 1, plane);
        cost = costs.get(neighbor);
        if (cost < lowestCost) { lowestCost = cost; bestDir = FlowField.DIR_SOUTHEAST; }

        neighbor = WorldPointUtil.compress(x - 1, y + 1, plane);
        cost = costs.get(neighbor);
        if (cost < lowestCost) { lowestCost = cost; bestDir = FlowField.DIR_NORTHWEST; }

        neighbor = WorldPointUtil.compress(x + 1, y + 1, plane);
        cost = costs.get(neighbor);
        if (cost < lowestCost) { lowestCost = cost; bestDir = FlowField.DIR_NORTHEAST; }

        return bestDir;
    }

    /**
     * Follows flow field from player position to goal.
     * Checks for transports first, then follows flow field directions.
     */
    private List<FlowFieldStep> followField(FlowField field) {
        return followFieldFrom(field, playerStartPos);
    }

    /**
     * Follows flow field from a specific starting position (e.g. teleport destination).
     */
    private List<FlowFieldStep> followFieldFrom(FlowField field, int startPos) {
        if (!field.isReachable(startPos)) {
            return new ArrayList<>();
        }

        LinkedList<FlowFieldStep> path = new LinkedList<>();
        int current = startPos;
        int maxSteps = 1000;  // Prevent infinite loops
        int steps = 0;

        while (field.getCost(current) > 0 && steps < maxSteps) {
            int currentCost = field.getCost(current);
            int next = -1;
            Transport transport = null;

            // First, check if there's a beneficial transport from current position
            ArrayList<Transport> transports = TransportLoader.getTransports().get(current);
            if (transports != null) {
                int bestTransportCost = Integer.MAX_VALUE;
                Transport bestTransport = null;

                for (Transport t : transports) {
                    int destCost = field.getCost(t.getDestination());
                    if (destCost < bestTransportCost && destCost < currentCost) {
                        bestTransportCost = destCost;
                        bestTransport = t;
                    }
                }

                if (bestTransport != null) {
                    // Transport provides a better path
                    transport = bestTransport;
                    next = bestTransport.getDestination();
                }
            }

            // If no transport or transport not beneficial, follow flow field
            if (next == -1) {
                byte direction = field.getDirection(current);
                if (direction == FlowField.DIR_NONE) {
                    break;  // Unreachable
                }

                int[] offset = FlowField.getDirectionOffset(direction);
                short x = WorldPointUtil.getCompressedX(current);
                short y = WorldPointUtil.getCompressedY(current);
                byte plane = WorldPointUtil.getCompressedPlane(current);

                next = WorldPointUtil.compress(x + offset[0], y + offset[1], plane);
            }

            path.add(new FlowFieldStep(next, transport));
            current = next;
            steps++;
        }

        return new ArrayList<>(path);
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

package com.tonic.services.pathfinder.implimentations.jpsplus;

import com.tonic.services.pathfinder.CollisionMap;
import com.tonic.services.pathfinder.abstractions.IPathfinder;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * JPS+ pathfinder with Transport Graph Augmentation.
 * Combines preprocessed jump point graph with dynamic transport edges.
 */
public class JPSPlusAlgo implements IPathfinder
{
    private final CollisionMap collisionMap;
    private final JPSPlusPreprocessor preprocessor;

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

    public JPSPlusAlgo(CollisionMap collisionMap) {
        this.collisionMap = collisionMap;
        this.preprocessor = new JPSPlusPreprocessor(collisionMap);
    }

    @Override
    public List<JPSPlusStep> findPath(int playerStart, int targetEnd)
    {
        JPSPlusCache cache = new JPSPlusCache(10000);
        PriorityQueue<JPSPlusNode> openSet = new PriorityQueue<>((a, b) ->
            Integer.compare(a.fScore, b.fScore));

        // Initialize start node
        int startH = heuristic(playerStart, targetEnd);
        openSet.add(new JPSPlusNode(playerStart, 0, startH, -1, null));
        cache.putIfBetter(playerStart, 0, -1);

        while (!openSet.isEmpty()) {
            JPSPlusNode current = openSet.poll();

            // Goal reached
            if (current.position == targetEnd) {
                return cache.reconstructPath(targetEnd, playerStart);
            }

            // Skip if we've found a better path to this node
            if (current.gScore > cache.getGScore(current.position)) {
                continue;
            }

            // Expand jump point successors
            expandJumpSuccessors(current, targetEnd, cache, openSet);

            // Expand transport edges
            expandTransports(current, targetEnd, cache, openSet);
        }

        // No path found
        return new ArrayList<>();
    }

    /**
     * Expands all jump point successors from current node.
     */
    private void expandJumpSuccessors(JPSPlusNode current, int target,
                                     JPSPlusCache cache, PriorityQueue<JPSPlusNode> openSet)
    {
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
                int h = heuristic(successor, target);
                openSet.add(new JPSPlusNode(successor, tentativeG, h, current.position, null));
            }
        }
    }

    /**
     * Expands all transport edges from current node.
     */
    private void expandTransports(JPSPlusNode current, int target,
                                  JPSPlusCache cache, PriorityQueue<JPSPlusNode> openSet)
    {
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
                int h = heuristic(destination, target);
                openSet.add(new JPSPlusNode(destination, tentativeG, h, current.position, transport));
            }
        }
    }

    /**
     * Gets position at offset from current position.
     */
    private int getPositionAt(int position, int dx, int dy) {
        int current = position;
        int absDx = Math.abs(dx);
        int absDy = Math.abs(dy);
        int signX = Integer.signum(dx);
        int signY = Integer.signum(dy);

        // Move in steps to reach target offset
        for (int i = 0; i < Math.max(absDx, absDy); i++) {
            int stepX = (i < absDx) ? signX : 0;
            int stepY = (i < absDy) ? signY : 0;
            current = collisionMap.getNeighbor(current, stepX, stepY);
            if (current == -1) return -1;
        }

        return current;
    }

    /**
     * Octile distance heuristic (supports diagonal movement).
     */
    private int heuristic(int from, int to) {
        int[] fromCoords = collisionMap.unpack(from);
        int[] toCoords = collisionMap.unpack(to);

        int dx = Math.abs(fromCoords[0] - toCoords[0]);
        int dy = Math.abs(fromCoords[1] - toCoords[1]);

        // Octile: D * (dx + dy) + (D2 - 2*D) * min(dx, dy)
        // Where D=1 for cardinal, D2=1 for diagonal (since we allow diagonal)
        return Math.max(dx, dy);
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

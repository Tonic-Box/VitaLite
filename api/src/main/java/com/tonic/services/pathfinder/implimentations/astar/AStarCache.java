package com.tonic.services.pathfinder.implimentations.astar;

import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportLoader;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Optimized cache using Trove for maximum performance.
 * Matches BFS cache pattern but with g-score tracking.
 */
public class AStarCache
{
    private final TIntIntHashMap parents;
    private final TIntIntHashMap gScores;
    private final TIntObjectHashMap<Transport> transports;

    public AStarCache(int expectedSize) {
        this.parents = new TIntIntHashMap(expectedSize, 0.5f, -1, -1);
        this.gScores = new TIntIntHashMap(expectedSize, 0.5f, -1, Integer.MAX_VALUE);
        this.transports = new TIntObjectHashMap<>(expectedSize / 10);
    }

    /**
     * Attempts to add a new position with its g-score and parent.
     * @return true if added (better path), false if already has better path
     */
    public boolean putIfBetter(int position, int gScore, int parent) {
        int existingGScore = gScores.get(position);
        if (gScore < existingGScore) {
            gScores.put(position, gScore);
            parents.put(position, parent);
            return true;
        }
        return false;
    }

    /**
     * Adds position with g-score, parent, and transport.
     */
    public boolean putIfBetter(int position, int gScore, int parent, Transport transport) {
        int existingGScore = gScores.get(position);
        if (gScore < existingGScore) {
            gScores.put(position, gScore);
            parents.put(position, parent);
            if (transport != null) {
                transports.put(position, transport);
            }
            return true;
        }
        return false;
    }

    /**
     * Gets current g-score for position.
     */
    public int getGScore(int position) {
        return gScores.get(position);
    }

    /**
     * Checks if position has been visited.
     */
    public boolean contains(int position) {
        return gScores.contains(position);
    }

    public int size() {
        return parents.size();
    }

    /**
     * Reconstructs path from target back to start.
     */
    public List<AStarStep> reconstructPath(int target, int playerStart) {
        LinkedList<AStarStep> path = new LinkedList<>();
        int current = target;

        while (current != -1) {
            Transport transport = transports.get(current);
            path.addFirst(new AStarStep(current, transport));
            current = parents.get(current);
        }

        // Remove player's starting position (keep teleport destinations)
        if (!path.isEmpty() && path.getFirst().getPackedPosition() == playerStart) {
            path.removeFirst();
        }

        return new ArrayList<>(path);
    }

    /**
     * Reconstructs partial path from node back to start (for bidirectional).
     */
    public List<AStarStep> reconstructPartialPath(int node) {
        LinkedList<AStarStep> path = new LinkedList<>();
        int current = node;

        while (current != -1) {
            Transport transport = transports.get(current);
            path.addFirst(new AStarStep(current, transport));
            current = parents.get(current);
        }

        return new ArrayList<>(path);
    }
}

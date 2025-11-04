package com.tonic.services.pathfinder.implimentations.flowfield;

import gnu.trove.map.hash.TIntByteHashMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * Sparse flow field storing optimal directions from any tile to a goal.
 * Uses primitive collections for maximum memory efficiency.
 * Only stores tiles that are actually reachable from the goal.
 */
public class FlowField
{
    // Direction constants (bit-packed for efficiency)
    public static final byte DIR_NONE = -1;
    public static final byte DIR_WEST = 0;
    public static final byte DIR_EAST = 1;
    public static final byte DIR_SOUTH = 2;
    public static final byte DIR_NORTH = 3;
    public static final byte DIR_SOUTHWEST = 4;
    public static final byte DIR_SOUTHEAST = 5;
    public static final byte DIR_NORTHWEST = 6;
    public static final byte DIR_NORTHEAST = 7;

    private final int goalPosition;
    private final TIntByteHashMap directions;  // position -> direction
    private final TIntIntHashMap costs;        // position -> cost to goal
    private final long timestamp;
    private final int tilesReachable;

    public FlowField(int goalPosition, TIntByteHashMap directions, TIntIntHashMap costs) {
        this.goalPosition = goalPosition;
        this.directions = directions;
        this.costs = costs;
        this.timestamp = System.currentTimeMillis();
        this.tilesReachable = directions.size();
    }

    /**
     * Gets the optimal direction to move from this position.
     * @return Direction constant or DIR_NONE if unreachable
     */
    public byte getDirection(int position) {
        if (!directions.containsKey(position)) {
            return DIR_NONE;
        }
        return directions.get(position);
    }

    /**
     * Gets the cost (distance) from this position to goal.
     * @return Cost or Integer.MAX_VALUE if unreachable
     */
    public int getCost(int position) {
        if (!costs.containsKey(position)) {
            return Integer.MAX_VALUE;
        }
        return costs.get(position);
    }

    /**
     * Checks if position is reachable from goal.
     */
    public boolean isReachable(int position) {
        return directions.containsKey(position);
    }

    public int getGoalPosition() {
        return goalPosition;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getTilesReachable() {
        return tilesReachable;
    }

    /**
     * Estimates memory usage in bytes.
     */
    public int estimateMemoryBytes() {
        // Each entry: 4 bytes (key) + 1 byte (direction) + 4 bytes (cost) ≈ 9 bytes
        return tilesReachable * 9;
    }

    /**
     * Applies coordinate offset based on direction.
     * @return Array [deltaX, deltaY]
     */
    public static int[] getDirectionOffset(byte direction) {
        switch (direction) {
            case DIR_WEST:      return new int[]{-1,  0};
            case DIR_EAST:      return new int[]{ 1,  0};
            case DIR_SOUTH:     return new int[]{ 0, -1};
            case DIR_NORTH:     return new int[]{ 0,  1};
            case DIR_SOUTHWEST: return new int[]{-1, -1};
            case DIR_SOUTHEAST: return new int[]{ 1, -1};
            case DIR_NORTHWEST: return new int[]{-1,  1};
            case DIR_NORTHEAST: return new int[]{ 1,  1};
            default:            return new int[]{ 0,  0};
        }
    }
}

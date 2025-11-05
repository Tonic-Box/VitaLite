package com.tonic.services.pathfinder.implimentations.jpsplus;

import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.collision.Flags;
import com.tonic.util.WorldPointUtil;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Preprocesses the static grid to build jump point successor database.
 * One-time preprocessing cost for massive query speedup (~10x).
 */
public class JPSPlusPreprocessor
{
    private final TIntObjectHashMap<int[]> jumpSuccessors;

    // Direction constants: N, S, E, W, NE, NW, SE, SW
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

    public JPSPlusPreprocessor() {
        this.jumpSuccessors = new TIntObjectHashMap<>(100000);
    }

    /**
     * Gets or computes jump successors for a position.
     * Returns array of 8 jump distances (one per direction).
     * -1 means no jump point in that direction, otherwise distance to jump point.
     */
    public int[] getJumpSuccessors(int position) {
        int[] successors = jumpSuccessors.get(position);
        if (successors == null) {
            successors = computeJumpSuccessors(position);
            jumpSuccessors.put(position, successors);
        }
        return successors;
    }

    /**
     * Computes jump point successors for a single position in all 8 directions.
     */
    private int[] computeJumpSuccessors(int position) {
        int[] successors = new int[8];

        for (int dir = 0; dir < 8; dir++) {
            successors[dir] = computeJumpDistance(position, dir);
        }

        return successors;
    }

    /**
     * Computes the distance to the next jump point in a given direction.
     * Returns -1 if no jump point exists, otherwise the distance.
     */
    private int computeJumpDistance(int position, int direction) {
        int dx = DIRECTIONS[direction][0];
        int dy = DIRECTIONS[direction][1];

        int distance = 0;
        int current = position;

        while (distance < 1000) {
            distance++;

            // Try to move in the direction
            short x = WorldPointUtil.getCompressedX(current);
            short y = WorldPointUtil.getCompressedY(current);
            byte plane = WorldPointUtil.getCompressedPlane(current);

            short newX = (short)(x + dx);
            short newY = (short)(y + dy);
            int next = WorldPointUtil.compress(newX, newY, plane);

            // Check if walkable using collision map
            if (!Walker.getCollisionMap().walkable(next)) {
                return -1;
            }

            current = next;

            // Check if this is a jump point
            if (isJumpPoint(current, dx, dy)) {
                return distance;
            }
        }

        return -1;
    }

    /**
     * Determines if a position is a jump point when approached from a direction.
     */
    private boolean isJumpPoint(int position, int dx, int dy) {
        short x = WorldPointUtil.getCompressedX(position);
        short y = WorldPointUtil.getCompressedY(position);
        byte plane = WorldPointUtil.getCompressedPlane(position);

        // Cardinal directions
        if (dx == 0 || dy == 0) {
            return hasCardinalForcedNeighbor(x, y, plane, dx, dy);
        }

        // Diagonal directions
        return hasDiagonalForcedNeighbor(x, y, plane, dx, dy) ||
               hasCardinalJumpPoint(position, dx, dy);
    }

    /**
     * Checks for forced neighbors in cardinal direction movement.
     */
    private boolean hasCardinalForcedNeighbor(short x, short y, byte plane, int dx, int dy) {
        if (dx != 0) { // Horizontal movement
            // Check above and below
            int above = WorldPointUtil.compress(x, (short)(y + 1), plane);
            int below = WorldPointUtil.compress(x, (short)(y - 1), plane);
            int aboveBlocked = WorldPointUtil.compress((short)(x - dx), (short)(y + 1), plane);
            int belowBlocked = WorldPointUtil.compress((short)(x - dx), (short)(y - 1), plane);

            return (Walker.getCollisionMap().walkable(above) && !Walker.getCollisionMap().walkable(aboveBlocked)) ||
                   (Walker.getCollisionMap().walkable(below) && !Walker.getCollisionMap().walkable(belowBlocked));
        } else { // Vertical movement
            // Check left and right
            int left = WorldPointUtil.compress((short)(x - 1), y, plane);
            int right = WorldPointUtil.compress((short)(x + 1), y, plane);
            int leftBlocked = WorldPointUtil.compress((short)(x - 1), (short)(y - dy), plane);
            int rightBlocked = WorldPointUtil.compress((short)(x + 1), (short)(y - dy), plane);

            return (Walker.getCollisionMap().walkable(left) && !Walker.getCollisionMap().walkable(leftBlocked)) ||
                   (Walker.getCollisionMap().walkable(right) && !Walker.getCollisionMap().walkable(rightBlocked));
        }
    }

    /**
     * Checks for forced neighbors in diagonal direction movement.
     */
    private boolean hasDiagonalForcedNeighbor(short x, short y, byte plane, int dx, int dy) {
        // Check horizontal and vertical blocked neighbors
        int hBlocked = WorldPointUtil.compress((short)(x - dx), y, plane);
        int vBlocked = WorldPointUtil.compress(x, (short)(y - dy), plane);
        int hNext = WorldPointUtil.compress((short)(x - dx), (short)(y + dy), plane);
        int vNext = WorldPointUtil.compress((short)(x + dx), (short)(y - dy), plane);

        return (!Walker.getCollisionMap().walkable(hBlocked) && Walker.getCollisionMap().walkable(hNext)) ||
               (!Walker.getCollisionMap().walkable(vBlocked) && Walker.getCollisionMap().walkable(vNext));
    }

    /**
     * Checks if there's a jump point in the cardinal components of diagonal movement.
     */
    private boolean hasCardinalJumpPoint(int position, int dx, int dy) {
        // Check horizontal direction
        int hJump = computeJumpDistance(position, dx > 0 ? 2 : 3); // E or W
        if (hJump != -1) return true;

        // Check vertical direction
        int vJump = computeJumpDistance(position, dy > 0 ? 0 : 1); // N or S
        return vJump != -1;
    }

    /**
     * Clears the preprocessed data (for dynamic map changes).
     */
    public void clear() {
        jumpSuccessors.clear();
    }
}

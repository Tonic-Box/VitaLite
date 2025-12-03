package com.tonic.services.pathfinder.sailing.graph;

/**
 * Coordinate packing utilities for navigation graph nodes.
 * Nodes are identified by packed coordinates (one node per tile).
 *
 * Packing format: x: 13 bits (0-8191), y: 15 bits (0-32767), plane: 4 bits (0-15)
 */
public final class GraphNode {

    private GraphNode() {
        // Utility class
    }

    /**
     * Packs x, y, plane into a single 32-bit integer.
     */
    public static int pack(int x, int y, int plane) {
        return (x & 8191) | ((y & 32767) << 13) | ((plane & 15) << 28);
    }

    /**
     * Unpacks a 32-bit packed coordinate into [x, y, plane].
     */
    public static int[] unpack(int packed) {
        int x = packed & 8191;
        int y = (packed >> 13) & 32767;
        int plane = (packed >> 28) & 15;
        return new int[]{x, y, plane};
    }

    /**
     * Extracts X coordinate from packed value.
     */
    public static int getX(int packed) {
        return packed & 8191;
    }

    /**
     * Extracts Y coordinate from packed value.
     */
    public static int getY(int packed) {
        return (packed >> 13) & 32767;
    }

    /**
     * Extracts plane from packed value.
     */
    public static int getPlane(int packed) {
        return (packed >> 28) & 15;
    }

    /**
     * Creates a unique 64-bit key for an edge (for deduplication).
     * Always stores lower packed coord first.
     */
    public static long edgeKey(int source, int target) {
        if (source <= target) {
            return ((long) source << 32) | (target & 0xFFFFFFFFL);
        } else {
            return ((long) target << 32) | (source & 0xFFFFFFFFL);
        }
    }

    /**
     * Calculates Chebyshev distance between two packed coordinates.
     */
    public static int chebyshevDistance(int packed1, int packed2) {
        int dx = Math.abs(getX(packed1) - getX(packed2));
        int dy = Math.abs(getY(packed1) - getY(packed2));
        return Math.max(dx, dy);
    }
}

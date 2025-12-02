package com.tonic.services.pathfinder.sailing;

import com.tonic.services.pathfinder.collision.CollisionMap;

/**
 * Cache for boat hull data used during pathfinding.
 * Pre-computes hull offsets and rotated hull positions to eliminate
 * repeated API calls and floating-point math from the hot path.
 *
 * OPTIMIZATION: Pre-computes rotated hull offsets for all 8 directions
 * during initialization, eliminating ~160,000 Math.round() calls per pathfind.
 */
public class BoatHullCache
{
    // Pre-computed heading values for each direction (0-15 heading scale)
    // Eliminates expensive atan2/toDegrees/normalization operations
    private static final int[] DIRECTION_HEADINGS = {
            4,  // West (-1, 0)
            12, // East (1, 0)
            0,  // South (0, -1)
            8,  // North (0, 1)
            2,  // Southwest (-1, -1)
            14, // Southeast (1, -1)
            6,  // Northwest (-1, 1)
            10  // Northeast (1, 1)
    };

    final int[] xOffsets;
    final int[] yOffsets;
    final int currentHeadingValue;
    final CollisionMap collisionMap;

    // Pre-computed rotation matrices for each direction (kept for reference/debugging)
    final double[] directionCos;
    final double[] directionSin;

    // Pre-computed rotated hull offsets for all 8 directions + 1 unrotated (index 8)
    // Eliminates Math.round() from hot path - computed once at initialization
    // Index 8 = unrotated hull (current heading) for use near start position
    final int[][] rotatedXOffsets;  // [direction][hullTile]
    final int[][] rotatedYOffsets;  // [direction][hullTile]

    BoatHullCache(int[] xOffsets, int[] yOffsets, int currentHeadingValue, CollisionMap collisionMap)
    {
        this.xOffsets = xOffsets;
        this.yOffsets = yOffsets;
        this.currentHeadingValue = currentHeadingValue;
        this.collisionMap = collisionMap;

        // Pre-compute rotation matrices for all 8 directions
        this.directionCos = new double[8];
        this.directionSin = new double[8];

        // Pre-compute rotated hull offsets for all 8 directions + 1 unrotated
        int hullSize = xOffsets.length;
        this.rotatedXOffsets = new int[9][hullSize];  // 9 = 8 directions + 1 unrotated
        this.rotatedYOffsets = new int[9][hullSize];

        for (int dir = 0; dir < 8; dir++) {
            int targetHeadingValue = DIRECTION_HEADINGS[dir];
            int headingDiff = targetHeadingValue - currentHeadingValue;

            // Normalize to -8 to 7 range
            while (headingDiff > 8) headingDiff -= 16;
            while (headingDiff < -8) headingDiff += 16;

            double rotationRadians = headingDiff * Math.PI / 8.0;
            double cos = Math.cos(rotationRadians);
            double sin = Math.sin(rotationRadians);

            directionCos[dir] = cos;
            directionSin[dir] = sin;

            // Pre-compute rotated offsets for this direction
            // Math.round() happens HERE during init, not in hot path
            for (int i = 0; i < hullSize; i++) {
                rotatedXOffsets[dir][i] = (int) Math.round(xOffsets[i] * cos - yOffsets[i] * sin);
                rotatedYOffsets[dir][i] = (int) Math.round(xOffsets[i] * sin + yOffsets[i] * cos);
            }
        }

        // Index 8 = unrotated hull (current heading)
        // Used for checking positions near start where boat is at its actual current heading
        for (int i = 0; i < hullSize; i++) {
            rotatedXOffsets[8][i] = xOffsets[i];
            rotatedYOffsets[8][i] = yOffsets[i];
        }
    }
}

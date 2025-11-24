package com.tonic.services.pathfinder.sailing;

import com.tonic.services.pathfinder.collision.CollisionMap;

/**
 * Cache for boat hull data used during pathfinding.
 * Pre-computes hull offsets to eliminate repeated API calls.
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

    // Pre-computed rotation matrices for each direction
    final double[] directionCos;
    final double[] directionSin;

    BoatHullCache(int[] xOffsets, int[] yOffsets, int currentHeadingValue, CollisionMap collisionMap)
    {
        this.xOffsets = xOffsets;
        this.yOffsets = yOffsets;
        this.currentHeadingValue = currentHeadingValue;
        this.collisionMap = collisionMap;

        // Pre-compute rotation matrices for all 8 directions
        this.directionCos = new double[8];
        this.directionSin = new double[8];
        for (int dir = 0; dir < 8; dir++) {
            int targetHeadingValue = DIRECTION_HEADINGS[dir];
            int headingDiff = targetHeadingValue - currentHeadingValue;

            // Normalize to -8 to 7 range
            while (headingDiff > 8) headingDiff -= 16;
            while (headingDiff < -8) headingDiff += 16;

            double rotationRadians = headingDiff * Math.PI / 8.0;
            directionCos[dir] = Math.cos(rotationRadians);
            directionSin[dir] = Math.sin(rotationRadians);
        }
    }
}

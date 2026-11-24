package com.tonic.services.pathfinder.recorder;

import net.runelite.api.coords.WorldPoint;

/**
 * The tile faces a wall-type object closes by shape and rotation: one face for a straight wall, the two meeting at its corner for an L-corner, none for diagonals.
 */
final class WallCrossing
{
    private static final int SHAPE_WALL_STRAIGHT = 0;
    private static final int SHAPE_WALL_L_CORNER = 2;
    private static final int[] DX = {-1, 0, 1, 0};
    private static final int[] DY = {0, 1, 0, -1};

    private WallCrossing()
    {
    }

    static boolean crosses(WorldPoint wall, int shape, int rotation, WorldPoint from, WorldPoint to)
    {
        if (from.getPlane() != wall.getPlane() || to.getPlane() != wall.getPlane())
        {
            return false;
        }

        for (int face : faces(shape, rotation))
        {
            WorldPoint beyond = new WorldPoint(wall.getX() + DX[face], wall.getY() + DY[face], wall.getPlane());
            if ((from.equals(wall) && to.equals(beyond)) || (from.equals(beyond) && to.equals(wall)))
            {
                return true;
            }
        }
        return false;
    }

    static int[] faces(int shape, int rotation)
    {
        int face = rotation & 3;
        if (shape == SHAPE_WALL_STRAIGHT)
        {
            return new int[]{face};
        }
        if (shape == SHAPE_WALL_L_CORNER)
        {
            switch (face)
            {
                case 0:
                    return new int[]{1, 0};
                case 1:
                    return new int[]{1, 2};
                case 2:
                    return new int[]{3, 2};
                default:
                    return new int[]{3, 0};
            }
        }
        return new int[0];
    }
}

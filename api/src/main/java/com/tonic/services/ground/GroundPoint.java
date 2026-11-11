package com.tonic.services.ground;

/**
 * A point on the ground a canvas point was projected from: its world view, local position and the level its height was read from, chained to the next point projected to the same canvas point.
 */
final class GroundPoint
{
    final int worldView;
    final int x;
    final int y;
    final int plane;
    GroundPoint next;

    GroundPoint(int worldView, int x, int y, int plane)
    {
        this.worldView = worldView;
        this.x = x;
        this.y = y;
        this.plane = plane;
    }

    boolean samePlace(GroundPoint other)
    {
        return isAt(other.worldView, other.x, other.y, other.plane);
    }

    boolean isAt(int worldView, int x, int y, int plane)
    {
        return this.worldView == worldView && this.x == x && this.y == y && this.plane == plane;
    }

    boolean sameSurface(GroundPoint other)
    {
        return other.worldView == worldView && other.plane == plane;
    }

    long distanceSquared(GroundPoint other)
    {
        long dx = other.x - x;
        long dy = other.y - y;
        return dx * dx + dy * dy;
    }
}

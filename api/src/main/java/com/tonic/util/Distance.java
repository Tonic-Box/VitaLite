package com.tonic.util;

import com.tonic.Static;
import com.tonic.api.game.SceneAPI;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

/**
 * Utility class for calculating various distance metrics between points.
 */
public class Distance
{
    //higher
    public static int pathDistanceTo(Tile from, Tile to)
    {
        if(from == null || to == null)
            return Integer.MAX_VALUE;
        return pathDistanceTo(from.getWorldLocation(), to.getWorldLocation());
    }

    public static int pathDistanceTo(WorldPoint from, WorldPoint to)
    {
        if(from.equals(to))
            return 0;
        return Static.invoke(() -> {
            List<WorldPoint> path = SceneAPI.pathTo(from, to);
            if (path == null || path.isEmpty())
            {
                return Integer.MAX_VALUE;
            }
            return path.size() - 1;
        });
    }

    //base

    /**
     * Calculates the Euclidean (straight-line) distance between two points.
     * Formula: sqrt((x2-x1)² + (y2-y1)²)
     *
     * @param x1 X coordinate of first point
     * @param y1 Y coordinate of first point
     * @param x2 X coordinate of second point
     * @param y2 Y coordinate of second point
     * @return Euclidean distance between the points (truncated to int)
     */
    public static int euclidean(int x1, int y1, int x2, int y2)
    {
        int dx = x2 - x1;
        int dy = y2 - y1;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculates the Euclidean (straight-line) distance between two WorldPoints.
     * Returns Integer.MAX_VALUE if the points are on different planes.
     *
     * @param p1 First WorldPoint
     * @param p2 Second WorldPoint
     * @return Euclidean distance, or Integer.MAX_VALUE if on different planes
     */
    public static int euclidean(WorldPoint p1, WorldPoint p2)
    {
        if (p1.getPlane() != p2.getPlane())
        {
            return Integer.MAX_VALUE;
        }
        return euclidean(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Calculates the Manhattan (taxicab) distance between two points.
     * Formula: |x2-x1| + |y2-y1|
     *
     * @param x1 X coordinate of first point
     * @param y1 Y coordinate of first point
     * @param x2 X coordinate of second point
     * @param y2 Y coordinate of second point
     * @return Manhattan distance between the points
     */
    public static int manhattan(int x1, int y1, int x2, int y2)
    {
        return Math.abs(x2 - x1) + Math.abs(y2 - y1);
    }

    /**
     * Calculates the Manhattan (taxicab) distance between two WorldPoints.
     * Returns Integer.MAX_VALUE if the points are on different planes.
     *
     * @param p1 First WorldPoint
     * @param p2 Second WorldPoint
     * @return Manhattan distance, or Integer.MAX_VALUE if on different planes
     */
    public static int manhattan(WorldPoint p1, WorldPoint p2)
    {
        if (p1.getPlane() != p2.getPlane())
        {
            return Integer.MAX_VALUE;
        }
        return manhattan(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Calculates the Chebyshev (chessboard) distance between two points.
     * Formula: max(|x2-x1|, |y2-y1|)
     *
     * @param x1 X coordinate of first point
     * @param y1 Y coordinate of first point
     * @param x2 X coordinate of second point
     * @param y2 Y coordinate of second point
     * @return Chebyshev distance between the points
     */
    public static int chebyshev(int x1, int y1, int x2, int y2)
    {
        return Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
    }

    /**
     * Calculates the Chebyshev (chessboard) distance between two WorldPoints.
     * Returns Integer.MAX_VALUE if the points are on different planes.
     *
     * @param p1 First WorldPoint
     * @param p2 Second WorldPoint
     * @return Chebyshev distance, or Integer.MAX_VALUE if on different planes
     */
    public static int chebyshev(WorldPoint p1, WorldPoint p2)
    {
        if (p1.getPlane() != p2.getPlane())
        {
            return Integer.MAX_VALUE;
        }
        return chebyshev(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Calculates the Diagonal distance between two points.
     * Accounts for diagonal movement (sqrt(2) cost) and orthogonal movement (1 cost).
     * Formula: D * (dx + dy) + (D2 - 2 * D) * min(dx, dy)
     * where D = 1 (orthogonal cost), D2 = sqrt(2) (diagonal cost)
     *
     * @param x1 X coordinate of first point
     * @param y1 Y coordinate of first point
     * @param x2 X coordinate of second point
     * @param y2 Y coordinate of second point
     * @return Diagonal distance between the points (truncated to int)
     */
    public static int diagonal(int x1, int y1, int x2, int y2)
    {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        double D = 1.0; // Orthogonal cost
        double D2 = Math.sqrt(2); // Diagonal cost
        return (int) (D * (dx + dy) + (D2 - 2 * D) * Math.min(dx, dy));
    }

    /**
     * Calculates the Diagonal distance between two WorldPoints.
     * Returns Integer.MAX_VALUE if the points are on different planes.
     *
     * @param p1 First WorldPoint
     * @param p2 Second WorldPoint
     * @return Diagonal distance, or Integer.MAX_VALUE if on different planes
     */
    public static int diagonal(WorldPoint p1, WorldPoint p2)
    {
        if (p1.getPlane() != p2.getPlane())
        {
            return Integer.MAX_VALUE;
        }
        return diagonal(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Calculates the squared Euclidean distance between two points.
     * Useful when you only need to compare distances and want to avoid the sqrt calculation.
     * Formula: (x2-x1)² + (y2-y1)²
     *
     * @param x1 X coordinate of first point
     * @param y1 Y coordinate of first point
     * @param x2 X coordinate of second point
     * @param y2 Y coordinate of second point
     * @return Squared Euclidean distance between the points
     */
    public static int euclideanSquared(int x1, int y1, int x2, int y2)
    {
        int dx = x2 - x1;
        int dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    /**
     * Calculates the squared Euclidean distance between two WorldPoints.
     * Returns Integer.MAX_VALUE if the points are on different planes.
     *
     * @param p1 First WorldPoint
     * @param p2 Second WorldPoint
     * @return Squared Euclidean distance, or Integer.MAX_VALUE if on different planes
     */
    public static int euclideanSquared(WorldPoint p1, WorldPoint p2)
    {
        if (p1.getPlane() != p2.getPlane())
        {
            return Integer.MAX_VALUE;
        }
        return euclideanSquared(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Calculates the Euclidean (straight-line) distance between two Tiles.
     * Returns Integer.MAX_VALUE if either tile is null or they are on different planes.
     *
     * @param source Source Tile
     * @param dest Destination Tile
     * @return Euclidean distance, or Integer.MAX_VALUE if null or on different planes
     */
    public static int euclidean(Tile source, Tile dest)
    {
        if (source == null || dest == null)
            return Integer.MAX_VALUE;
        if (source.getPlane() != dest.getPlane())
            return Integer.MAX_VALUE;
        WorldPoint p1 = source.getWorldLocation();
        WorldPoint p2 = dest.getWorldLocation();
        return euclidean(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Calculates the Manhattan (taxicab) distance between two Tiles.
     * Returns Integer.MAX_VALUE if either tile is null or they are on different planes.
     *
     * @param source Source Tile
     * @param dest Destination Tile
     * @return Manhattan distance, or Integer.MAX_VALUE if null or on different planes
     */
    public static int manhattan(Tile source, Tile dest)
    {
        if (source == null || dest == null)
            return Integer.MAX_VALUE;
        if (source.getPlane() != dest.getPlane())
            return Integer.MAX_VALUE;
        WorldPoint p1 = source.getWorldLocation();
        WorldPoint p2 = dest.getWorldLocation();
        return manhattan(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Calculates the Chebyshev (chessboard) distance between two Tiles.
     * Returns Integer.MAX_VALUE if either tile is null or they are on different planes.
     *
     * @param source Source Tile
     * @param dest Destination Tile
     * @return Chebyshev distance, or Integer.MAX_VALUE if null or on different planes
     */
    public static int chebyshev(Tile source, Tile dest)
    {
        if (source == null || dest == null)
            return Integer.MAX_VALUE;
        if (source.getPlane() != dest.getPlane())
            return Integer.MAX_VALUE;
        WorldPoint p1 = source.getWorldLocation();
        WorldPoint p2 = dest.getWorldLocation();
        return chebyshev(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Calculates the Diagonal distance between two Tiles.
     * Returns Integer.MAX_VALUE if either tile is null or they are on different planes.
     *
     * @param source Source Tile
     * @param dest Destination Tile
     * @return Diagonal distance, or Integer.MAX_VALUE if null or on different planes
     */
    public static int diagonal(Tile source, Tile dest)
    {
        if (source == null || dest == null)
            return Integer.MAX_VALUE;
        if (source.getPlane() != dest.getPlane())
            return Integer.MAX_VALUE;
        WorldPoint p1 = source.getWorldLocation();
        WorldPoint p2 = dest.getWorldLocation();
        return diagonal(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Calculates the squared Euclidean distance between two Tiles.
     * Returns Integer.MAX_VALUE if either tile is null or they are on different planes.
     *
     * @param source Source Tile
     * @param dest Destination Tile
     * @return Squared Euclidean distance, or Integer.MAX_VALUE if null or on different planes
     */
    public static int euclideanSquared(Tile source, Tile dest)
    {
        if (source == null || dest == null)
            return Integer.MAX_VALUE;
        if (source.getPlane() != dest.getPlane())
            return Integer.MAX_VALUE;
        WorldPoint p1 = source.getWorldLocation();
        WorldPoint p2 = dest.getWorldLocation();
        return euclideanSquared(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }
}

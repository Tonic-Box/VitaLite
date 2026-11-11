package com.tonic.services.ground;

import java.util.Arrays;

/**
 * Lays flat shapes on a world view's ground. A tile is drawn as two triangles split along its south-east to north-west diagonal, so a shape is cut into the pieces that fall on each and every piece takes that triangle's own heights, lifted a little above it.
 */
final class GroundDrape
{
    static final double TILE = 128.0;

    private static final int AXES = 3;
    private static final double EPSILON = 1e-9;

    private final GroundSurface surface;
    private final double lift;
    private final GroundTriangles into;

    private double[] clippedX = new double[16];
    private double[] clippedY = new double[16];
    private double[] swapX = new double[16];
    private double[] swapY = new double[16];
    private double[] points = new double[16 * AXES];

    GroundDrape(GroundSurface surface, double lift, GroundTriangles into)
    {
        this.surface = surface;
        this.lift = lift;
        this.into = into;
    }

    GroundSurface surface()
    {
        return surface;
    }

    void fillConvex(double[] xs, double[] ys, int count, int plane, int argb)
    {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int at = 0; at < count; at++)
        {
            minX = Math.min(minX, xs[at]);
            maxX = Math.max(maxX, xs[at]);
            minY = Math.min(minY, ys[at]);
            maxY = Math.max(maxY, ys[at]);
        }

        int fromX = Math.max(surface.minTile(), (int) Math.floor(minX));
        int fromY = Math.max(surface.minTile(), (int) Math.floor(minY));
        int toX = Math.min(surface.maxTileX() - 1, (int) Math.ceil(maxX) - 1);
        int toY = Math.min(surface.maxTileY() - 1, (int) Math.ceil(maxY) - 1);
        if (fromX > toX || fromY > toY)
        {
            return;
        }

        int level = surface.surfacePlane(plane, fromX, fromY);
        if (flat(level, plane, fromX, fromY, toX, toY))
        {
            layFlat(xs, ys, count, level, fromX, fromY, argb);
            return;
        }

        for (int tileY = fromY; tileY <= toY; tileY++)
        {
            for (int tileX = fromX; tileX <= toX; tileX++)
            {
                int ground = surface.surfacePlane(plane, tileX, tileY);
                layOnHalf(xs, ys, count, tileX, tileY, ground, true, argb);
                layOnHalf(xs, ys, count, tileX, tileY, ground, false, argb);
            }
        }
    }

    private boolean flat(int level, int plane, int fromX, int fromY, int toX, int toY)
    {
        int height = surface.height(level, fromX, fromY);
        for (int tileY = fromY; tileY <= toY; tileY++)
        {
            for (int tileX = fromX; tileX <= toX; tileX++)
            {
                if (surface.surfacePlane(plane, tileX, tileY) != level)
                {
                    return false;
                }
            }
        }

        for (int cornerY = fromY; cornerY <= toY + 1; cornerY++)
        {
            for (int cornerX = fromX; cornerX <= toX + 1; cornerX++)
            {
                if (surface.height(level, cornerX, cornerY) != height)
                {
                    return false;
                }
            }
        }
        return true;
    }

    private void layFlat(double[] xs, double[] ys, int count, int level, int anyTileX, int anyTileY, int argb)
    {
        double min = surface.minTile();
        double maxX = surface.maxTileX();
        double maxY = surface.maxTileY();
        int kept = copy(xs, ys, count);
        kept = clip(kept, min, min, maxX, min);
        kept = clip(kept, maxX, min, maxX, maxY);
        kept = clip(kept, maxX, maxY, min, maxY);
        kept = clip(kept, min, maxY, min, min);
        if (kept < 3 || area(kept) < EPSILON)
        {
            return;
        }

        double ground = surface.height(level, anyTileX, anyTileY);
        ensurePoints(kept);
        for (int at = 0; at < kept; at++)
        {
            place(at, clippedX[at] * TILE, ground - lift, clippedY[at] * TILE);
        }
        for (int at = 1; at + 1 < kept; at++)
        {
            into.add(points, 0, at, at + 1, argb);
        }
    }

    private void layOnHalf(double[] xs, double[] ys, int count, int tileX, int tileY, int plane, boolean low, int argb)
    {
        int kept = copy(xs, ys, count);
        if (low)
        {
            kept = clip(kept, tileX, tileY, tileX + 1, tileY);
            kept = clip(kept, tileX + 1, tileY, tileX, tileY + 1);
            kept = clip(kept, tileX, tileY + 1, tileX, tileY);
        }
        else
        {
            kept = clip(kept, tileX + 1, tileY, tileX + 1, tileY + 1);
            kept = clip(kept, tileX + 1, tileY + 1, tileX, tileY + 1);
            kept = clip(kept, tileX, tileY + 1, tileX + 1, tileY);
        }
        if (kept < 3 || area(kept) < EPSILON)
        {
            return;
        }
        emit(kept, tileX, tileY, plane, low, argb);
    }

    private int copy(double[] xs, double[] ys, int count)
    {
        ensure(count);
        System.arraycopy(xs, 0, clippedX, 0, count);
        System.arraycopy(ys, 0, clippedY, 0, count);
        return count;
    }

    private int clip(int count, double fromX, double fromY, double toX, double toY)
    {
        if (count == 0)
        {
            return 0;
        }

        ensureSwap(count * 2);
        int kept = 0;
        for (int at = 0; at < count; at++)
        {
            double x = clippedX[at];
            double y = clippedY[at];
            double nextX = clippedX[(at + 1) % count];
            double nextY = clippedY[(at + 1) % count];
            double side = side(fromX, fromY, toX, toY, x, y);
            double nextSide = side(fromX, fromY, toX, toY, nextX, nextY);
            if (side >= 0)
            {
                swapX[kept] = x;
                swapY[kept++] = y;
            }
            if ((side >= 0) != (nextSide >= 0))
            {
                double t = side / (side - nextSide);
                swapX[kept] = x + (nextX - x) * t;
                swapY[kept++] = y + (nextY - y) * t;
            }
        }

        double[] held = clippedX;
        clippedX = swapX;
        swapX = held;
        held = clippedY;
        clippedY = swapY;
        swapY = held;
        return kept;
    }

    private void emit(int count, int tileX, int tileY, int plane, boolean low, int argb)
    {
        ensurePoints(count);
        for (int at = 0; at < count; at++)
        {
            double x = clippedX[at];
            double y = clippedY[at];
            double ground = height(plane, tileX, tileY, low, x - tileX, y - tileY);
            place(at, x * TILE, ground - lift, y * TILE);
        }
        for (int at = 1; at + 1 < count; at++)
        {
            into.add(points, 0, at, at + 1, argb);
        }
    }

    private void place(int at, double x, double height, double y)
    {
        points[at * AXES] = x;
        points[at * AXES + 1] = height;
        points[at * AXES + 2] = y;
    }

    private double height(int plane, int tileX, int tileY, boolean low, double across, double along)
    {
        double southEast = surface.height(plane, tileX + 1, tileY);
        double northWest = surface.height(plane, tileX, tileY + 1);
        if (low)
        {
            double southWest = surface.height(plane, tileX, tileY);
            return southWest + across * (southEast - southWest) + along * (northWest - southWest);
        }
        double northEast = surface.height(plane, tileX + 1, tileY + 1);
        return northEast + (1 - across) * (northWest - northEast) + (1 - along) * (southEast - northEast);
    }

    private void ensurePoints(int count)
    {
        if (points.length < count * AXES)
        {
            points = new double[count * 2 * AXES];
        }
    }

    private double area(int count)
    {
        double twice = 0;
        for (int at = 0; at < count; at++)
        {
            int next = (at + 1) % count;
            twice += clippedX[at] * clippedY[next] - clippedX[next] * clippedY[at];
        }
        return Math.abs(twice) / 2;
    }

    private static double side(double fromX, double fromY, double toX, double toY, double x, double y)
    {
        return (toX - fromX) * (y - fromY) - (toY - fromY) * (x - fromX);
    }

    private void ensure(int count)
    {
        if (clippedX.length < count)
        {
            clippedX = new double[count * 2];
            clippedY = new double[count * 2];
        }
    }

    private void ensureSwap(int count)
    {
        if (swapX.length < count)
        {
            swapX = new double[count * 2];
            swapY = new double[count * 2];
        }
        if (clippedX.length < count)
        {
            clippedX = Arrays.copyOf(clippedX, count * 2);
            clippedY = Arrays.copyOf(clippedY, count * 2);
        }
    }
}

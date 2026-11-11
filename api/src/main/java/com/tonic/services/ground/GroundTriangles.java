package com.tonic.services.ground;

import java.util.Arrays;

/**
 * Ground triangles in one world view's local space, each with one colour whose alpha is its opacity, with bounds kept per block of triangles and a revision that changes whenever they do.
 */
final class GroundTriangles
{
    static final int BLOCK = 64;
    static final int MIN_X = 0;
    static final int MAX_X = 1;
    static final int MIN_HEIGHT = 2;
    static final int MAX_HEIGHT = 3;
    static final int MIN_Y = 4;
    static final int MAX_Y = 5;

    private static final int INITIAL_TRIANGLES = 256;
    private static final int CORNERS = 3;
    private static final int AXES = 3;
    private static final int BOUNDS = 6;

    private final int worldViewId;
    private float[] corners = new float[INITIAL_TRIANGLES * CORNERS * AXES];
    private int[] colours = new int[INITIAL_TRIANGLES];
    private float[] blocks = new float[INITIAL_TRIANGLES / BLOCK * BOUNDS];
    private int count;
    private int revision;

    GroundTriangles(int worldViewId)
    {
        this.worldViewId = worldViewId;
    }

    int worldViewId()
    {
        return worldViewId;
    }

    void clear()
    {
        count = 0;
        revision++;
    }

    int count()
    {
        return count;
    }

    int revision()
    {
        return revision;
    }

    int blockCount()
    {
        return (count + BLOCK - 1) / BLOCK;
    }

    float bound(int block, int which)
    {
        return blocks[block * BOUNDS + which];
    }

    float x(int triangle, int corner)
    {
        return corners[(triangle * CORNERS + corner) * AXES];
    }

    float height(int triangle, int corner)
    {
        return corners[(triangle * CORNERS + corner) * AXES + 1];
    }

    float y(int triangle, int corner)
    {
        return corners[(triangle * CORNERS + corner) * AXES + 2];
    }

    int colour(int triangle)
    {
        return colours[triangle];
    }

    void add(double[] points, int a, int b, int c, int argb)
    {
        if (count == colours.length)
        {
            colours = Arrays.copyOf(colours, count * 2);
            corners = Arrays.copyOf(corners, count * 2 * CORNERS * AXES);
            blocks = Arrays.copyOf(blocks, count * 2 / BLOCK * BOUNDS);
        }
        int at = count * CORNERS * AXES;
        put(points, a, at);
        put(points, b, at + AXES);
        put(points, c, at + 2 * AXES);
        colours[count] = argb;
        widen(count / BLOCK, at, count % BLOCK == 0);
        count++;
        revision++;
    }

    private void put(double[] points, int index, int at)
    {
        corners[at] = (float) points[index * AXES];
        corners[at + 1] = (float) points[index * AXES + 1];
        corners[at + 2] = (float) points[index * AXES + 2];
    }

    private void widen(int block, int at, boolean first)
    {
        int bounds = block * BOUNDS;
        if (first)
        {
            blocks[bounds + MIN_X] = Float.MAX_VALUE;
            blocks[bounds + MAX_X] = -Float.MAX_VALUE;
            blocks[bounds + MIN_HEIGHT] = Float.MAX_VALUE;
            blocks[bounds + MAX_HEIGHT] = -Float.MAX_VALUE;
            blocks[bounds + MIN_Y] = Float.MAX_VALUE;
            blocks[bounds + MAX_Y] = -Float.MAX_VALUE;
        }
        for (int corner = 0; corner < CORNERS; corner++)
        {
            int offset = at + corner * AXES;
            blocks[bounds + MIN_X] = Math.min(blocks[bounds + MIN_X], corners[offset]);
            blocks[bounds + MAX_X] = Math.max(blocks[bounds + MAX_X], corners[offset]);
            blocks[bounds + MIN_HEIGHT] = Math.min(blocks[bounds + MIN_HEIGHT], corners[offset + 1]);
            blocks[bounds + MAX_HEIGHT] = Math.max(blocks[bounds + MAX_HEIGHT], corners[offset + 1]);
            blocks[bounds + MIN_Y] = Math.min(blocks[bounds + MIN_Y], corners[offset + 2]);
            blocks[bounds + MAX_Y] = Math.max(blocks[bounds + MAX_Y], corners[offset + 2]);
        }
    }
}

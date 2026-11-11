package com.tonic.services.ground;

/**
 * A shape an overlay drew through ground points: its corners on the ground and how it was painted.
 */
final class GroundShape
{
    final GroundPoint[] points;
    final boolean closed;
    final boolean fill;
    final int argb;
    final float strokeWidth;
    final float[] dash;
    final float dashPhase;

    GroundShape(GroundPoint[] points, boolean closed, boolean fill, int argb, float strokeWidth, float[] dash, float dashPhase)
    {
        this.points = points;
        this.closed = closed;
        this.fill = fill;
        this.argb = argb;
        this.strokeWidth = strokeWidth;
        this.dash = dash;
        this.dashPhase = dashPhase;
    }

    boolean samePlace(GroundShape other)
    {
        if (other.points.length != points.length)
        {
            return false;
        }
        for (int i = 0; i < points.length; i++)
        {
            if (!points[i].samePlace(other.points[i]))
            {
                return false;
            }
        }
        return true;
    }

    static GroundPoint[] match(int[] canvasX, int[] canvasY, int count)
    {
        GroundPoint[] candidates = new GroundPoint[count];
        for (int i = 0; i < count; i++)
        {
            candidates[i] = GroundPoints.lookup(canvasX[i], canvasY[i]);
            if (candidates[i] == null)
            {
                return null;
            }
        }

        for (GroundPoint first = candidates[0]; first != null; first = first.next)
        {
            GroundPoint[] chosen = new GroundPoint[count];
            chosen[0] = first;
            boolean complete = true;
            for (int i = 1; i < count && complete; i++)
            {
                chosen[i] = nearest(candidates[i], chosen[i - 1]);
                complete = chosen[i] != null;
            }
            if (complete)
            {
                return chosen;
            }
        }
        return null;
    }

    private static GroundPoint nearest(GroundPoint options, GroundPoint previous)
    {
        GroundPoint best = null;
        long bestDistance = Long.MAX_VALUE;
        for (GroundPoint option = options; option != null; option = option.next)
        {
            if (!option.sameSurface(previous))
            {
                continue;
            }
            long distance = option.distanceSquared(previous);
            if (distance < bestDistance)
            {
                best = option;
                bestDistance = distance;
            }
        }
        return best;
    }
}

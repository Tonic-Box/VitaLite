package com.tonic.services.mouserecorder;

import lombok.Getter;
import java.io.Serializable;

/**
 * Represents a single mouse movement sample with coordinates and timestamp.
 */
@Getter
public class MouseDataPoint implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final int x;
    private final int y;
    private final long timestampMillis;

    public MouseDataPoint(int x, int y, long timestampMillis)
    {
        this.x = x;
        this.y = y;
        this.timestampMillis = timestampMillis;
    }

    /**
     * Creates a MouseDataPoint with current system time.
     */
    public static MouseDataPoint now(int x, int y)
    {
        return new MouseDataPoint(x, y, System.currentTimeMillis());
    }

    /**
     * Creates a MouseDataPoint representing the mouse outside the viewport.
     */
    public static MouseDataPoint outsideViewport(long timestampMillis)
    {
        return new MouseDataPoint(-1, -1, timestampMillis);
    }

    /**
     * Returns true if this point is outside the viewport
     */
    public boolean isOutsideViewport()
    {
        return x == -1 && y == -1;
    }

    /**
     * Clamps coordinates to valid range (-1 to 65534)
     */
    public MouseDataPoint clamped()
    {
        int clampedX = Math.max(-1, Math.min(65534, x));
        int clampedY = Math.max(-1, Math.min(65534, y));
        return new MouseDataPoint(clampedX, clampedY, timestampMillis);
    }

    @Override
    public String toString()
    {
        return String.format("MouseDataPoint{x=%d, y=%d, time=%d}", x, y, timestampMillis);
    }
}

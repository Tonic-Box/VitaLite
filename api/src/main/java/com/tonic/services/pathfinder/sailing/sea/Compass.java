package com.tonic.services.pathfinder.sailing.sea;

/**
 * Boat angles in the client's 0..2047 units (0 south, 512 west, 1024 north, 1536 east) and the sixteen compass points
 */
public final class Compass
{
    public static final int POINTS = 16;
    public static final int TURN = 2048;
    public static final int STEP = TURN / POINTS;
    public static final int ANGLE_MASK = TURN - 1;
    public static final int FINE_SHIFT = 7;
    private static final double UNITS = TURN / (2 * Math.PI);
    private static final String[] NAMES = {"S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW", "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE"};

    private Compass()
    {
    }

    /**
     * Gets the bearing from one point to another, x east and y north
     * @param fromX Start x
     * @param fromY Start y
     * @param toX End x
     * @param toY End y
     * @return the bearing in angle units
     */
    public static int bearing(double fromX, double fromY, double toX, double toY)
    {
        return ((int) Math.round(Math.atan2(-(toX - fromX), -(toY - fromY)) * UNITS)) & ANGLE_MASK;
    }

    /**
     * Gets the compass point nearest a bearing
     * @param bearing Bearing in angle units
     * @return the compass point (0-15)
     */
    public static int of(int bearing)
    {
        return ((bearing + STEP / 2) & ANGLE_MASK) / STEP;
    }

    /**
     * Gets the bearing at the centre of a compass point
     * @param heading Compass point (0-15)
     * @return the bearing in angle units
     */
    public static int bearingOf(int heading)
    {
        return (heading * STEP) & ANGLE_MASK;
    }

    /**
     * Gets the signed shortest turn from one angle to another
     * @param from Angle turned from
     * @param to Angle turned to
     * @return the turn, in -1023..1024
     */
    public static int shortestArc(int from, int to)
    {
        int arc = (to - from) & ANGLE_MASK;
        if (arc > TURN / 2)
        {
            arc = -(TURN - arc);
        }
        return arc;
    }

    /**
     * Gets the name of a compass point, S at 0 round through W, N and E
     * @param heading Compass point (0-15)
     * @return the point's name
     */
    public static String name(int heading)
    {
        if (heading < 0 || heading >= POINTS)
        {
            throw new IndexOutOfBoundsException("heading=" + heading);
        }
        return NAMES[heading];
    }
}

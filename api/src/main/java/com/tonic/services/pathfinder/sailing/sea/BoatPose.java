package com.tonic.services.pathfinder.sailing.sea;

import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/**
 * Where a boat's pivot is on the sea and which way it faces
 */
public final class BoatPose
{
    private final int fineX;
    private final int fineY;
    private final int angle;

    private static final int FINE_SHIFT = Compass.FINE_SHIFT;
    private static final int HALF_TILE = 64;

    public BoatPose(int fineX, int fineY, int angle)
    {
        angle &= Compass.ANGLE_MASK;
        this.fineX = fineX;
        this.fineY = fineY;
        this.angle = angle;
    }

    /**
     * Gets the pose at a tile's centre facing a compass point
     * @param x Tile x
     * @param y Tile y
     * @param heading Compass point (0-15)
     * @return the pose
     */
    public static BoatPose atTile(int x, int y, int heading)
    {
        if (heading < 0 || heading >= Compass.POINTS)
        {
            throw new IllegalArgumentException("heading must be 0..15");
        }
        return new BoatPose(x << FINE_SHIFT | HALF_TILE, y << FINE_SHIFT | HALF_TILE, Compass.bearingOf(heading));
    }

    /**
     * Gets the ground tile the pivot stands on
     * @return the pivot's tile
     */
    public WorldPoint tile()
    {
        return new WorldPoint(tileX(), tileY(), 0);
    }

    public int tileX()
    {
        return fineX >> FINE_SHIFT;
    }

    public int tileY()
    {
        return fineY >> FINE_SHIFT;
    }

    /**
     * Gets the compass point nearest the angle, 0 south, 4 west, 8 north, 12 east
     * @return the compass point (0-15)
     */
    public int heading()
    {
        return Compass.of(angle);
    }

    /**
     * Gets the pose a tick of movement at a speed takes this one to, as the client rounds it
     * @param speed Speed in fine units a tick
     * @return the pose after the tick
     */
    public BoatPose advanced(int speed)
    {
        double theta = angle * (2 * Math.PI / Compass.TURN);
        return new BoatPose(fineX + (int) Math.round(-speed * Math.sin(theta)), fineY + (int) Math.round(-speed * Math.cos(theta)), angle);
    }

    @Override
    public String toString()
    {
        return "(" + fineX + ", " + fineY + ") angle " + angle + " on " + tile();
    }

    public int fineX()
    {
        return fineX;
    }

    public int fineY()
    {
        return fineY;
    }

    public int angle()
    {
        return angle;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof BoatPose))
        {
            return false;
        }
        BoatPose other = (BoatPose) o;
        return fineX == other.fineX && fineY == other.fineY && angle == other.angle;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(fineX, fineY, angle);
    }
}

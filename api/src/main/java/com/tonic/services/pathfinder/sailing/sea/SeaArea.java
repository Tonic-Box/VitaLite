package com.tonic.services.pathfinder.sailing.sea;

import java.util.Objects;

/**
 * A box of the sea, in tiles, edges included
 */
public final class SeaArea
{
    private final int minX;
    private final int minY;
    private final int maxX;
    private final int maxY;

    public SeaArea(int minX, int minY, int maxX, int maxY)
    {
        if (minX > maxX || minY > maxY)
        {
            throw new IllegalArgumentException("the edges must not cross, were x " + minX + " to " + maxX + " and y " + minY + " to " + maxY);
        }
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    /**
     * Checks whether the tile lies in the box
     * @param x Tile x
     * @param y Tile y
     * @return true if the tile is inside the box
     */
    public boolean contains(int x, int y)
    {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    public int minX()
    {
        return minX;
    }

    public int minY()
    {
        return minY;
    }

    public int maxX()
    {
        return maxX;
    }

    public int maxY()
    {
        return maxY;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof SeaArea))
        {
            return false;
        }
        SeaArea other = (SeaArea) o;
        return minX == other.minX && minY == other.minY && maxX == other.maxX && maxY == other.maxY;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(minX, minY, maxX, maxY);
    }

    @Override
    public String toString()
    {
        return "SeaArea[" + "minX=" + minX + ", " + "minY=" + minY + ", " + "maxX=" + maxX + ", " + "maxY=" + maxY + "]";
    }
}

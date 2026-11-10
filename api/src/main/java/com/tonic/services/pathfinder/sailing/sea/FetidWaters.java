package com.tonic.services.pathfinder.sailing.sea;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.runelite.api.coords.WorldPoint;

import java.util.Collection;
import java.util.List;

/**
 * The fetid pools' tiles, which slow a boat at full sail to a crawl while it lies on them (Jubbly Jive's pools, locs 60359 to 60363)
 */
public final class FetidWaters
{
    /**
     * No pools
     */
    public static final FetidWaters NONE = new FetidWaters(List.of());

    private static final int AHEAD = 128;
    private static final int[] AHEAD_X = new int[Compass.TURN];
    private static final int[] AHEAD_Y = new int[Compass.TURN];

    static
    {
        for (int angle = 0; angle < Compass.TURN; angle++)
        {
            double radians = angle * (2 * Math.PI / Compass.TURN);
            AHEAD_X[angle] = (int) Math.round(-Math.sin(radians) * AHEAD);
            AHEAD_Y[angle] = (int) Math.round(-Math.cos(radians) * AHEAD);
        }
    }

    private final LongOpenHashSet tiles = new LongOpenHashSet();

    public FetidWaters(Collection<WorldPoint> tiles)
    {
        if (tiles == null)
        {
            throw new IllegalArgumentException("tiles cannot be null");
        }
        for (WorldPoint tile : tiles)
        {
            this.tiles.add(key(tile.getX(), tile.getY()));
        }
    }

    /**
     * Checks whether they cover the same tiles
     * @param other Pools to compare with
     * @return true if both cover the same tiles
     */
    public boolean sameAs(FetidWaters other)
    {
        return other != null && tiles.equals(other.tiles);
    }

    /**
     * Checks whether there are no pools
     * @return true if there are no pools
     */
    public boolean isEmpty()
    {
        return tiles.isEmpty();
    }

    /**
     * Checks whether the tile is a pool's
     * @param x Tile x
     * @param y Tile y
     * @return true if the tile is a pool's
     */
    public boolean at(int x, int y)
    {
        return tiles.contains(key(x, y));
    }

    /**
     * Checks whether the pools hold a boat lying so
     * @param fineX Fine x of the boat
     * @param fineY Fine y of the boat
     * @param angle Angle the boat faces
     * @return true if the boat's pivot or the point ahead of it is on a pool
     */
    public boolean holds(int fineX, int fineY, int angle)
    {
        if (tiles.isEmpty())
        {
            return false;
        }
        int turned = angle & (Compass.TURN - 1);
        return at(fineX >> 7, fineY >> 7) || at((fineX + AHEAD_X[turned]) >> 7, (fineY + AHEAD_Y[turned]) >> 7);
    }

    private static long key(int x, int y)
    {
        return ((long) x << 32) | (y & 0xffffffffL);
    }
}

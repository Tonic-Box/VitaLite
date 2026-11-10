package com.tonic.services.pathfinder.sailing.sea;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.util.List;
import java.util.Objects;

/**
 * The rapids a boat may sail over, by tile
 */
public final class Rapids
{
    /**
     * No rapids
     */
    public static final Rapids NONE = new Rapids(List.of());

    private static final int NOT_ON = 0;
    private static final int STACK_BITS = 2;
    private static final int STACK_MASK = (1 << STACK_BITS) - 1;
    private static final int MOST_ENTRY_STACKS = STACK_MASK;

    private final Long2IntOpenHashMap tiles = new Long2IntOpenHashMap();

    /**
     * A patch of rapids
     */
    public static final class Patch
    {
        private final int x;
        private final int y;
        private final int sizeX;
        private final int sizeY;
        private final int stacks;

        public Patch(int x, int y, int sizeX, int sizeY, int stacks)
        {
            if (sizeX <= 0 || sizeY <= 0 || stacks < 1 || stacks > MOST_ENTRY_STACKS)
            {
                throw new IllegalArgumentException("sizes must be positive and stacks 1 to " + MOST_ENTRY_STACKS + ", were " + sizeX + "x" + sizeY + " and " + stacks);
            }
            this.x = x;
            this.y = y;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.stacks = stacks;
        }

        public int x()
        {
            return x;
        }

        public int y()
        {
            return y;
        }

        public int sizeX()
        {
            return sizeX;
        }

        public int sizeY()
        {
            return sizeY;
        }

        public int stacks()
        {
            return stacks;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Patch))
            {
                return false;
            }
            Patch other = (Patch) o;
            return x == other.x && y == other.y && sizeX == other.sizeX && sizeY == other.sizeY && stacks == other.stacks;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(x, y, sizeX, sizeY, stacks);
        }

        @Override
        public String toString()
        {
            return "Patch[" + "x=" + x + ", " + "y=" + y + ", " + "sizeX=" + sizeX + ", " + "sizeY=" + sizeY + ", " + "stacks=" + stacks + "]";
        }
    }

    public Rapids(List<Patch> patches)
    {
        Objects.requireNonNull(patches, "patches");
        tiles.defaultReturnValue(NOT_ON);
        for (int index = 0; index < patches.size(); index++)
        {
            Patch patch = Objects.requireNonNull(patches.get(index), "patch");
            for (int dx = 0; dx < patch.sizeX(); dx++)
            {
                for (int dy = 0; dy < patch.sizeY(); dy++)
                {
                    tiles.put(key(patch.x() + dx, patch.y() + dy), (index + 1) << STACK_BITS | patch.stacks());
                }
            }
        }
    }

    /**
     * Checks whether there are no rapids
     * @return true if there are no rapids
     */
    public boolean isEmpty()
    {
        return tiles.isEmpty();
    }

    /**
     * Gets the patch on the tile, packed with the stacks it gives
     * @param x Tile x
     * @param y Tile y
     * @return the packed patch, 0 when the tile has none
     */
    public int at(int x, int y)
    {
        return tiles.get(key(x, y));
    }

    /**
     * Gets which patch a packed value is
     * @param packed Packed patch from at
     * @return the patch's number
     */
    public static int patchOf(int packed)
    {
        return packed >>> STACK_BITS;
    }

    /**
     * Gets the stacks the patch gives a boat entering it unboosted
     * @param packed Packed patch from at
     * @return the stacks, 0 for none
     */
    public static int stacksOf(int packed)
    {
        return packed & STACK_MASK;
    }

    /**
     * Gets the stacks the patch it entered gives
     * @param here Packed patch the boat is on now
     * @param before Packed patch the boat was on before
     * @return the stacks, 0 when it entered none
     */
    public static int entered(int here, int before)
    {
        return here != NOT_ON && patchOf(here) != patchOf(before) ? stacksOf(here) : 0;
    }

    private static long key(int x, int y)
    {
        return (long) x << Integer.SIZE | (y & 0xffffffffL);
    }
}

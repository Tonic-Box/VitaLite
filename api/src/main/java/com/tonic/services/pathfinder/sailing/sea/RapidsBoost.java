package com.tonic.services.pathfinder.sailing.sea;

import java.util.Objects;

/**
 * A boat's speed from rapids, as the server carries it
 */
public final class RapidsBoost
{
    private final int stacks;
    private final int clock;

    /**
     * No boost
     */
    public static final RapidsBoost NONE = new RapidsBoost(0, 0);
    /**
     * How much a stack adds to the speed at full sail, in fine units a tick
     */
    public static final int GAIN = 64;
    /**
     * The most stacks a boat holds
     */
    public static final int MOST_STACKS = 15;
    /**
     * How many ticks after the patch last entered gave its stacks they hold
     */
    public static final int HELD_TICKS = 15;
    /**
     * How many ticks apart stacks are lost once they no longer hold
     */
    public static final int DECAY_TICKS = 4;
    /**
     * How many stacks go each time
     */
    public static final int DECAY_STACKS = 2;
    /**
     * One past the largest clock
     */
    public static final int CLOCK_LIMIT = HELD_TICKS;

    public RapidsBoost(int stacks, int clock)
    {
        if (stacks < 0 || stacks > MOST_STACKS || clock < 0 || clock >= CLOCK_LIMIT)
        {
            throw new IllegalArgumentException("stacks must be 0 to " + MOST_STACKS + " and clock 0 to " + (CLOCK_LIMIT - 1) + ", were " + stacks + " and " + clock);
        }
        this.stacks = stacks;
        this.clock = clock;
    }

    /**
     * Gets the boost a tick on, for the move after the next
     * @param entered Stacks the patch entered this tick gives, 0 for none
     * @return the next boost
     */
    public RapidsBoost after(int entered)
    {
        int next = next(stacks, clock, entered);
        return next == 0 ? NONE : new RapidsBoost(stacksOf(next), clockOf(next));
    }

    /**
     * Gets the boost a tick on, packed as stacksOf and clockOf read it, for a search that keeps it in a key
     * @param stacks Stacks held now
     * @param clock Clock now
     * @param entered Stacks the patch entered this tick gives, 0 for none
     * @return the packed next boost, 0 for none
     */
    static int next(int stacks, int clock, int entered)
    {
        if (entered > 0)
        {
            return pack(Math.min(MOST_STACKS, stacks + (stacks == 0 ? entered : 1)), 0);
        }
        if (stacks == 0)
        {
            return 0;
        }
        if (clock + 1 < CLOCK_LIMIT)
        {
            return pack(stacks, clock + 1);
        }
        int left = Math.max(0, stacks - DECAY_STACKS);
        return left == 0 ? 0 : pack(left, HELD_TICKS - DECAY_TICKS);
    }

    static int stacksOf(int packed)
    {
        return packed >>> Byte.SIZE;
    }

    static int clockOf(int packed)
    {
        return packed & 0xff;
    }

    private static int pack(int stacks, int clock)
    {
        return stacks << Byte.SIZE | clock;
    }

    public int stacks()
    {
        return stacks;
    }

    public int clock()
    {
        return clock;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof RapidsBoost))
        {
            return false;
        }
        RapidsBoost other = (RapidsBoost) o;
        return stacks == other.stacks && clock == other.clock;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(stacks, clock);
    }

    @Override
    public String toString()
    {
        return "RapidsBoost[" + "stacks=" + stacks + ", " + "clock=" + clock + "]";
    }
}

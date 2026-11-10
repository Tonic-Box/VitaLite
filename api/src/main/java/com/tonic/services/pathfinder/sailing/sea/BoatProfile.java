package com.tonic.services.pathfinder.sailing.sea;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * How a kind of boat moves, in fine units (1/128 tile) a tick
 */
public final class BoatProfile
{
    private final int fullSpeed;
    private final int slowSpeed;
    private final int ramp;
    private final int speedCap;

    /**
     * The raft
     */
    public static final BoatProfile RAFT = new BoatProfile(BoatKinematics.FULL_SPEED, BoatKinematics.SLOW_SPEED, BoatKinematics.RAMP);
    private static final int MOST_SPEEDS = 16;

    public BoatProfile(int fullSpeed, int slowSpeed, int ramp, int speedCap)
    {
        if (fullSpeed <= 0 || slowSpeed <= 0 || ramp <= 0)
        {
            throw new IllegalArgumentException("speeds must be positive, were " + fullSpeed + ", " + slowSpeed + ", " + ramp);
        }
        if (fullSpeed % ramp != 0 || slowSpeed % ramp != 0 || slowSpeed > fullSpeed)
        {
            throw new IllegalArgumentException("speeds must be whole ramps with slow at most full, were " + fullSpeed + ", " + slowSpeed + ", " + ramp);
        }
        if (speedCap < fullSpeed)
        {
            throw new IllegalArgumentException("speedCap cannot be below full speed " + fullSpeed + ", was " + speedCap);
        }
        if (speeds(fullSpeed, slowSpeed, ramp, speedCap).length > MOST_SPEEDS)
        {
            throw new IllegalArgumentException("a boat can move at most " + MOST_SPEEDS + " speeds, full " + fullSpeed + " by " + ramp + " capped at " + speedCap + " is more");
        }
        this.fullSpeed = fullSpeed;
        this.slowSpeed = slowSpeed;
        this.ramp = ramp;
        this.speedCap = speedCap;
    }

    /**
     * Creates a boat rapids carry no faster, only a gust
     * @param fullSpeed Speed at full sail
     * @param slowSpeed Speed at slow
     * @param ramp Speed gained or lost a tick
     */
    public BoatProfile(int fullSpeed, int slowSpeed, int ramp)
    {
        this(fullSpeed, slowSpeed, ramp, fullSpeed);
    }

    /**
     * Gets the profile of a boarded boat from its sidepanel's base speed and acceleration, the raft's for any it lacks
     * @param baseSpeed Sidepanel base speed
     * @param acceleration Sidepanel acceleration
     * @return the boat's profile
     */
    public static BoatProfile of(int baseSpeed, int acceleration)
    {
        return of(baseSpeed, acceleration, 0);
    }

    /**
     * Gets the profile of a boarded boat from its sidepanel's base speed, acceleration and speed cap, the raft's for any it lacks
     * @param baseSpeed Sidepanel base speed
     * @param acceleration Sidepanel acceleration
     * @param speedCap Sidepanel speed cap
     * @return the boat's profile
     */
    public static BoatProfile of(int baseSpeed, int acceleration, int speedCap)
    {
        int full = baseSpeed > 0 ? baseSpeed : RAFT.fullSpeed;
        int ramp = acceleration > 0 ? acceleration : RAFT.ramp;
        int slow = Math.max(ramp, RAFT.slowSpeed / ramp * ramp);
        int cap = Math.max(full, speedCap);
        if (full % ramp != 0 || slow > full || speeds(full, slow, ramp, cap).length > MOST_SPEEDS)
        {
            return RAFT;
        }
        return new BoatProfile(full, slow, ramp, cap);
    }

    /**
     * Gets the speed of a trimmed gust, a gain past full
     * @return the gust speed
     */
    public int gustSpeed()
    {
        return fullSpeed + ramp;
    }

    /**
     * Gets the speed at full sail so boosted, the rapids' up to the cap and a gust's step over it
     * @param gust Whether a gust is trimmed
     * @param stacks Rapids stacks held
     * @return the boosted speed
     */
    public int boostedSpeed(boolean gust, int stacks)
    {
        return Math.min(speedCap, fullSpeed + RapidsBoost.GAIN * stacks) + (gust ? ramp : 0);
    }

    /**
     * Gets the fastest the boat sails
     * @return the top speed
     */
    public int topSpeed()
    {
        return boostedSpeed(true, RapidsBoost.MOST_STACKS);
    }

    /**
     * Gets every speed the boat moves at
     * @return the speeds, ascending
     */
    public int[] speeds()
    {
        return speeds(fullSpeed, slowSpeed, ramp, speedCap);
    }

    private static int[] speeds(int fullSpeed, int slowSpeed, int ramp, int speedCap)
    {
        TreeSet<Integer> targets = new TreeSet<>(List.of(-slowSpeed, 0, slowSpeed, fullSpeed));
        for (int gust = 0; gust <= ramp; gust += ramp)
        {
            for (int stacks = 0; stacks <= RapidsBoost.MOST_STACKS; stacks++)
            {
                targets.add(Math.min(speedCap, fullSpeed + RapidsBoost.GAIN * stacks) + gust);
            }
        }
        TreeSet<Integer> speeds = new TreeSet<>();
        ArrayDeque<Integer> open = new ArrayDeque<>(List.of(0));
        while (!open.isEmpty() && speeds.size() <= MOST_SPEEDS)
        {
            int speed = open.poll();
            if (!speeds.add(speed))
            {
                continue;
            }
            for (int target : targets)
            {
                open.add(speed < target ? Math.min(target, speed + ramp) : Math.max(target, speed - ramp));
            }
        }
        int[] out = new int[speeds.size()];
        int i = 0;
        for (int speed : speeds)
        {
            out[i++] = speed;
        }
        return out;
    }

    public int fullSpeed()
    {
        return fullSpeed;
    }

    public int slowSpeed()
    {
        return slowSpeed;
    }

    public int ramp()
    {
        return ramp;
    }

    public int speedCap()
    {
        return speedCap;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof BoatProfile))
        {
            return false;
        }
        BoatProfile other = (BoatProfile) o;
        return fullSpeed == other.fullSpeed && slowSpeed == other.slowSpeed && ramp == other.ramp && speedCap == other.speedCap;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(fullSpeed, slowSpeed, ramp, speedCap);
    }

    @Override
    public String toString()
    {
        return "BoatProfile[" + "fullSpeed=" + fullSpeed + ", " + "slowSpeed=" + slowSpeed + ", " + "ramp=" + ramp + ", " + "speedCap=" + speedCap + "]";
    }
}

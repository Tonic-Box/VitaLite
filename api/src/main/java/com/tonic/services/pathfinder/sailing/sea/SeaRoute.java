package com.tonic.services.pathfinder.sailing.sea;

import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A planned way across the sea
 */
public final class SeaRoute
{
    /**
     * No way, and no plan made
     */
    public static final SeaRoute EMPTY = new SeaRoute(List.of(), List.of(), List.of(), null, Stats.NONE);

    private final List<BoatPose> poses;
    private final List<Integer> throttles;
    private final List<Integer> speeds;
    private final List<Leg> legs;
    private final WorldPoint goal;
    private final Stats stats;

    SeaRoute(List<BoatPose> poses, List<Integer> throttles, List<Integer> speeds, WorldPoint goal, Stats stats)
    {
        if (poses.size() != throttles.size() || poses.size() != speeds.size())
        {
            throw new IllegalArgumentException("every pose needs its throttle and speed");
        }
        if (stats == null)
        {
            throw new IllegalArgumentException("stats cannot be null");
        }
        this.poses = Collections.unmodifiableList(new ArrayList<>(poses));
        this.throttles = Collections.unmodifiableList(new ArrayList<>(throttles));
        this.speeds = Collections.unmodifiableList(new ArrayList<>(speeds));
        this.legs = Collections.unmodifiableList(legsOf(this.poses));
        this.goal = goal;
        this.stats = stats;
    }

    /**
     * Creates the route of no way to a goal, after a plan that found none
     * @param goal Tile the plan was made to
     * @param stats What the plan cost to make
     * @return the empty route
     */
    static SeaRoute none(WorldPoint goal, Stats stats)
    {
        return new SeaRoute(List.of(), List.of(), List.of(), goal, stats);
    }

    /**
     * Gets what the plan cost to make
     * @return the stats, NONE for a route never planned
     */
    public Stats stats()
    {
        return stats;
    }

    /**
     * Gets the throttle mode the boat is in at the pose
     * @param index Pose index
     * @return the throttle mode
     */
    public int throttleTo(int index)
    {
        return throttles.get(requireIndex(index));
    }

    /**
     * Gets the speed the boat moves at into the pose
     * @param index Pose index
     * @return the speed in fine units a tick, negative astern
     */
    public int speedAt(int index)
    {
        return speeds.get(requireIndex(index));
    }

    /**
     * Counts how many poses the boat is at slow for
     * @return the slow pose count
     */
    public int slowPoses()
    {
        return count(BoatKinematics.SLOW);
    }

    /**
     * Counts how many poses the boat is astern for
     * @return the astern pose count
     */
    public int reversePoses()
    {
        return count(BoatKinematics.REVERSE);
    }

    /**
     * Gets the poses from the start to the goal, the start first
     * @return the poses
     */
    public List<BoatPose> poses()
    {
        return poses;
    }

    /**
     * Gets the straight legs the poses fall into, in order
     * @return the legs
     */
    public List<Leg> legs()
    {
        return legs;
    }

    /**
     * Gets the ground tile under each pose, in order
     * @return the tiles
     */
    public List<WorldPoint> tiles()
    {
        List<WorldPoint> tiles = new ArrayList<>(poses.size());
        for (BoatPose pose : poses)
        {
            tiles.add(pose.tile());
        }
        return tiles;
    }

    /**
     * Gets the tile the route was planned to
     * @return the goal, null for the empty route
     */
    public WorldPoint goal()
    {
        return goal;
    }

    /**
     * Gets the last pose
     * @return the last pose, null for the empty route
     */
    public BoatPose end()
    {
        return poses.isEmpty() ? null : poses.get(poses.size() - 1);
    }

    public boolean isEmpty()
    {
        return poses.isEmpty();
    }

    public int size()
    {
        return poses.size();
    }

    private int count(int throttle)
    {
        int found = 0;
        for (int reached : throttles)
        {
            if (reached == throttle)
            {
                found++;
            }
        }
        return found;
    }

    private int requireIndex(int index)
    {
        if (index < 0 || index >= poses.size())
        {
            throw new IndexOutOfBoundsException("index=" + index);
        }
        return index;
    }

    private static List<Leg> legsOf(List<BoatPose> poses)
    {
        List<Leg> legs = new ArrayList<>();
        int start = 0;
        for (int i = 1; i <= poses.size(); i++)
        {
            if (i == poses.size() || poses.get(i).heading() != poses.get(start).heading())
            {
                if (i - 1 > start)
                {
                    legs.add(new Leg(poses.get(start), poses.get(i - 1), poses.get(start).heading()));
                }
                start = i;
            }
        }
        return legs;
    }

    @Override
    public String toString()
    {
        return isEmpty() ? "no route" : "route of " + poses.size() + " ticks in " + legs.size() + " legs to " + goal;
    }

    /**
     * What a plan cost to make
     */
    public static final class Stats
    {
        private final long millis;
        private final int polls;
        private final int expansions;
        private final int hullFits;
        private final int pushes;
        private final int peakOpen;
        private final int tiles;
        private final int cost;
        private final int heuristic;

        /**
         * No plan made
         */
        public static final Stats NONE = new Stats(0, 0, 0, 0, 0, 0, 0, 0, 0);

        public Stats(long millis, int polls, int expansions, int hullFits, int pushes, int peakOpen, int tiles, int cost, int heuristic)
        {
            this.millis = millis;
            this.polls = polls;
            this.expansions = expansions;
            this.hullFits = hullFits;
            this.pushes = pushes;
            this.peakOpen = peakOpen;
            this.tiles = tiles;
            this.cost = cost;
            this.heuristic = heuristic;
        }

        public long millis()
        {
            return millis;
        }

        public int polls()
        {
            return polls;
        }

        public int expansions()
        {
            return expansions;
        }

        public int hullFits()
        {
            return hullFits;
        }

        public int pushes()
        {
            return pushes;
        }

        public int peakOpen()
        {
            return peakOpen;
        }

        public int tiles()
        {
            return tiles;
        }

        public int cost()
        {
            return cost;
        }

        public int heuristic()
        {
            return heuristic;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Stats))
            {
                return false;
            }
            Stats other = (Stats) o;
            return millis == other.millis && polls == other.polls && expansions == other.expansions && hullFits == other.hullFits && pushes == other.pushes && peakOpen == other.peakOpen && tiles == other.tiles && cost == other.cost && heuristic == other.heuristic;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(millis, polls, expansions, hullFits, pushes, peakOpen, tiles, cost, heuristic);
        }

        @Override
        public String toString()
        {
            return "Stats[" + "millis=" + millis + ", " + "polls=" + polls + ", " + "expansions=" + expansions + ", " + "hullFits=" + hullFits + ", " + "pushes=" + pushes + ", " + "peakOpen=" + peakOpen + ", " + "tiles=" + tiles + ", " + "cost=" + cost + ", " + "heuristic=" + heuristic + "]";
        }
    }

    /**
     * A straight run of the route
     */
    public static final class Leg
    {
        private final BoatPose from;
        private final BoatPose to;
        private final int heading;

        public Leg(BoatPose from, BoatPose to, int heading)
        {
            this.from = from;
            this.to = to;
            this.heading = heading;
        }

        public BoatPose from()
        {
            return from;
        }

        public BoatPose to()
        {
            return to;
        }

        public int heading()
        {
            return heading;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Leg))
            {
                return false;
            }
            Leg other = (Leg) o;
            return Objects.equals(from, other.from) && Objects.equals(to, other.to) && heading == other.heading;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(from, to, heading);
        }

        @Override
        public String toString()
        {
            return "Leg[" + "from=" + from + ", " + "to=" + to + ", " + "heading=" + heading + "]";
        }
    }
}

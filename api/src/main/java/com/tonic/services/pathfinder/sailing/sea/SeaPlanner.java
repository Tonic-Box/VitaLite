package com.tonic.services.pathfinder.sailing.sea;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Plans a boat's way across the sea as the server will move it
 */
public final class SeaPlanner
{
    /**
     * How far either side of the web's line the local search may stray, in tiles
     */
    public static final int CORRIDOR = 20;
    /**
     * How far either side of a line found over the sea the search may stray, as a boat needs room the line does not
     */
    public static final int LINE_CORRIDOR = 40;
    /**
     * How far from a tile the nearest web node may be
     */
    public static final int NODE_RADIUS = 100;
    private static final int MOST_ITERATIONS = 2_000_000;
    private static final int MOST_AHEAD_ITERATIONS = 200_000;
    private static final int FINISH_WITHIN = 8;
    private static final int TIGHT_CLEARANCE = 2;
    private static final double WEIGHT = 1.25;
    /**
     * The weight with fetid pools in view
     */
    private static final double POOLED_WEIGHT = 1.0;
    private static final int MOST_AXES_PER_TICK = 256;
    private static final int TICK_COST = 10;
    private static final int CLICK_COST = 1;
    private static final int TURN_COST = 5;
    private static final int HELM_CLICK_COST = 40;
    private static final int MOST_GROUNDED_RUN = 4;
    private static final int GROUNDED_CELL_COST = 100;
    private static final int RACING_COST = 30;
    private static final int OUT_OF_BOUNDS_COST = 400;
    private static final int ASTERN_COST = 20;
    private static final int STOPPED_COST = 5;
    private static final int HAZARD_COST = 400;
    private static final int HAZARD_REACH = 4;
    private static final int AVOIDED_TILE_COST = 100;
    /**
     * What a tick off a run's lane costs
     */
    private static final int OFF_LANE_COST = 1;
    private static final int IMPASSABLE = -1;
    private static final int GOAL_REACH = 1;
    private static final int[] CLEARANCE_MULTIPLIER = {50, 25, 12, 6, 3, 1, 1, 1};
    private static final int FIELD_SCALE = 3;
    /**
     * What a fetid pool's tile adds to the field the search is guided by, three ticks
     */
    private static final int POOL_FIELD_COST = 30;
    /**
     * What a move out of a pose fetid pools hold the boat at costs over the slowing it gets, a helm click's worth
     */
    private static final int POOL_CONTACT_COST = HELM_CLICK_COST;
    private static final int[][] FIELD_MOVES = {{1, 0, 2}, {0, 1, 2}, {-1, 0, 2}, {0, -1, 2}, {1, 1, 3}, {1, -1, 3}, {-1, 1, 3}, {-1, -1, 3}, {1, 3, 6}, {1, -3, 6}, {-1, 3, 6}, {-1, -3, 6}, {3, 1, 6}, {3, -1, 6}, {-3, 1, 6}, {-3, -1, 6}};
    private static final int ASTERN_SLACK = 3;
    private static final int SLOW_SLACK = 2;
    private static final int RAMPING_SLACK = 1;
    private static final int[] MODES = {BoatKinematics.REVERSE, BoatKinematics.STOPPED, BoatKinematics.SLOW, BoatKinematics.FULL, BoatKinematics.READY};
    private static final int[][] REACHABLE = reachables();
    private static final int[][] TOWARD_FULL_ONLY = towardFulls();
    private static final int GRID_SHIFT = 5;
    private static final int TILE_SHIFT = 7;
    private static final int HALF_TILE = 64;
    private static final int CELL_BITS = 17;
    private static final long CELL_MASK = (1L << CELL_BITS) - 1;
    private static final int HEADING_BITS = 4;
    private static final int SPEED_BITS = 4;
    private static final int MODE_BITS = 3;
    private static final int STACK_BITS = 4;
    private static final int CLOCK_BITS = 5;
    private static final int MODE_SHIFT = 2 * CELL_BITS + HEADING_BITS + SPEED_BITS;
    private static final int STACK_SHIFT = MODE_SHIFT + MODE_BITS;
    private static final int CLOCK_SHIFT = STACK_SHIFT + STACK_BITS;
    private static final int OBJECTIVE_BITS = 2;
    private static final int OBJECTIVE_SHIFT = CLOCK_SHIFT + CLOCK_BITS;
    private static final int FOUL_BITS = 3;
    private static final int FOUL_SHIFT = OBJECTIVE_SHIFT + OBJECTIVE_BITS;
    private static final int CONTACT_SHIFT = FOUL_SHIFT + FOUL_BITS;
    private static final int WITHIN_LAST_SHIFT = CONTACT_SHIFT + 1;
    private static final int COORDINATE_BITS = 16;
    private static final int COORDINATE_MASK = (1 << COORDINATE_BITS) - 1;
    private static final int NO_NODE = -1;
    private static final int FIRST_NODES = 1 << 16;
    private static final int FIRST_TILES = 1 << 12;
    private static final int POSES_PER_TILE = (1 << (TILE_SHIFT - GRID_SHIFT)) * (1 << (TILE_SHIFT - GRID_SHIFT)) * Compass.POINTS;
    private static final byte FIT_UNKNOWN = 0;
    private static final byte FIT_YES = 1;
    private static final byte FIT_NO = 2;
    private static final int TICK_REACH = 2;
    private static final int SURE_FLOOD_AFTER_POLLS = 1_000_000;
    private static final int PLAIN_SLACK = 1;
    private static final int WIDER_SLACK = 3;
    private static final int LINE_STRAIGHT = 10;
    private static final int LINE_DIAGONAL = 14;
    private static final int LINE_SPACING = 16;
    private static final int LINE_NODE_BITS = 22;
    private static final int LINE_HEURISTIC_BITS = 20;
    private static final long LINE_NODE_MASK = (1L << LINE_NODE_BITS) - 1;
    private static final int LINE_MOST_NODES = 1 << LINE_NODE_BITS;
    private static final int[][] STEP_HEADINGS = stepHeadings();
    private static final int CELLS_PER_TILE = 1 << (TILE_SHIFT - GRID_SHIFT);

    private final SeaMap sea;
    private final SeaGraph graph;
    private final HullRule rule;
    private final BoatProfile profile;
    private final int[] speeds;
    private final int[][] advanceXs;
    private final int[][] advanceYs;
    private final int[] slackTicks;
    private final int mostAxesPerTick;
    private final boolean racing;
    private final SeaArea bounds;
    private final LongOpenHashSet lane;
    private final FetidWaters fetid;
    private final AtomicReference<KeptLine> keptLine;
    private final AtomicInteger seaLinesSearched;

    /**
     * Creates a planner under the calibrated hull rule
     * @param sea sea to plan over
     * @param graph web of sea routes that guides the search
     */
    public SeaPlanner(SeaMap sea, SeaGraph graph)
    {
        this(sea, graph, HullRule.CALIBRATED);
    }

    public SeaPlanner(SeaMap sea, SeaGraph graph, HullRule rule)
    {
        this(sea, graph, rule, BoatProfile.RAFT);
    }

    public SeaPlanner(SeaMap sea, SeaGraph graph, HullRule rule, BoatProfile profile)
    {
        this(sea, graph, rule, profile, false, null, new LongOpenHashSet(), FetidWaters.NONE, new AtomicReference<>(), new AtomicInteger());
    }

    private SeaPlanner(SeaMap sea, SeaGraph graph, HullRule rule, BoatProfile profile, boolean racing, SeaArea bounds, LongOpenHashSet lane, FetidWaters fetid, AtomicReference<KeptLine> keptLine, AtomicInteger seaLinesSearched)
    {
        this.racing = racing;
        this.bounds = bounds;
        this.lane = lane;
        this.fetid = fetid;
        this.keptLine = keptLine;
        this.seaLinesSearched = seaLinesSearched;
        this.sea = requireNonNull(sea, "sea");
        this.graph = requireNonNull(graph, "graph");
        this.rule = requireNonNull(rule, "rule");
        this.profile = requireNonNull(profile, "profile");
        this.speeds = profile.speeds();
        this.advanceXs = advances(speeds, true);
        this.advanceYs = advances(speeds, false);
        this.slackTicks = slacks(speeds, profile);
        this.mostAxesPerTick = MOST_AXES_PER_TICK * profile.fullSpeed() / BoatKinematics.FULL_SPEED;
    }

    /**
     * Gets a planner over the same sea and web for a boat that moves so, this one when it already does
     * @param profile how the boat moves
     * @return the planner for that profile
     */
    public SeaPlanner forProfile(BoatProfile profile)
    {
        return requireNonNull(profile, "profile").equals(this.profile) ? this : new SeaPlanner(sea, graph, rule, profile, racing, bounds, lane, fetid, keptLine, seaLinesSearched);
    }

    /**
     * Gets a planner for a timed run within an area, as a Barracuda Trial is
     * @param bounds area the run stays within
     * @return the planner for the trial
     */
    public SeaPlanner forTrial(SeaArea bounds)
    {
        return racing && requireNonNull(bounds, "bounds").equals(this.bounds) ? this : new SeaPlanner(sea, graph, rule, profile, true, requireNonNull(bounds, "bounds"), lane, fetid, keptLine, seaLinesSearched);
    }

    /**
     * Gets a planner that knows the fetid pools, which slow a boat at full sail that lies on them unless a trim's immunity keeps them off
     * @param pools fetid pools on the sea
     * @return the planner that knows the pools
     */
    public SeaPlanner withFetid(FetidWaters pools)
    {
        return requireNonNull(pools, "pools") == fetid ? this : new SeaPlanner(sea, graph, rule, profile, racing, bounds, lane, pools, keptLine, seaLinesSearched);
    }

    /**
     * Gets a planner that draws routes along a lane, as a trial's pathfinding hints mark the way its runs are sailed
     * @param tiles tiles of the lane
     * @return the planner that keeps to the lane
     */
    public SeaPlanner withLane(Set<WorldPoint> tiles)
    {
        LongOpenHashSet keys = new LongOpenHashSet(requireNonNull(tiles, "tiles").size());
        for (WorldPoint tile : tiles)
        {
            keys.add(tileKey(tile.getX(), tile.getY()));
        }
        return keys.equals(lane) ? this : new SeaPlanner(sea, graph, rule, profile, racing, bounds, keys, fetid, keptLine, seaLinesSearched);
    }

    /**
     * Gets how the boat it plans for moves
     * @return the boat profile
     */
    public BoatProfile profile()
    {
        return profile;
    }

    public HullRule rule()
    {
        return rule;
    }

    /**
     * Plans from a pose at rest to a stop within a tile of a goal
     * @param start pose the boat starts from
     * @param goal tile to reach
     * @param hull hull of the boat
     * @param avoided water families to keep out of
     * @param avoidedTiles tiles that cost extra to cross
     * @return the route, or an empty route when none was found
     */
    public SeaRoute plan(BoatPose start, WorldPoint goal, BoatHull hull, Set<WaterFamily> avoided, Set<WorldPoint> avoidedTiles)
    {
        return plan(start, 0, BoatKinematics.STOPPED, 0, goal, hull, avoided, avoidedTiles);
    }

    /**
     * Plans from a pose under way to a stop within a tile of a goal
     * @param start pose the boat starts from
     * @param speed speed the boat is moving at
     * @param mode throttle mode the boat is in
     * @param holdTicks ticks the boat keeps its throttle and heading before the plan may change them
     * @param goal tile to reach
     * @param hull hull of the boat
     * @param avoided water families to keep out of
     * @param avoidedTiles tiles that cost extra to cross
     * @return the route, or an empty route when none was found
     */
    public SeaRoute plan(BoatPose start, int speed, int mode, int holdTicks, WorldPoint goal, BoatHull hull, Set<WaterFamily> avoided, Set<WorldPoint> avoidedTiles)
    {
        return plan(start, speed, mode, holdTicks, Gust.NONE, Rapids.NONE, RapidsBoost.NONE, 0, goal, GOAL_REACH, true, hull, avoided, avoidedTiles);
    }

    /**
     * Plans from a pose under way to a stop within a tile of a goal, with a trimmed gust boosting the first moves
     * @param start pose the boat starts from
     * @param speed speed the boat is moving at
     * @param mode throttle mode the boat is in
     * @param holdTicks ticks the boat keeps its throttle and heading before the plan may change them
     * @param boostMoves how many moves from the start the trimmed gust boosts
     * @param goal tile to reach
     * @param hull hull of the boat
     * @param avoided water families to keep out of
     * @param avoidedTiles tiles that cost extra to cross
     * @return the route, or an empty route when none was found
     */
    public SeaRoute plan(BoatPose start, int speed, int mode, int holdTicks, int boostMoves, WorldPoint goal, BoatHull hull, Set<WaterFamily> avoided, Set<WorldPoint> avoidedTiles)
    {
        return plan(start, speed, mode, holdTicks, new Gust(0, boostMoves), Rapids.NONE, RapidsBoost.NONE, 0, goal, GOAL_REACH, true, hull, avoided, avoidedTiles);
    }

    /**
     * Plans from a pose under way to a stop within a tile of a goal, with a trimmed gust boosting the first moves and rapids boosting the boat wherever the route crosses them
     * @param start pose the boat starts from
     * @param speed speed the boat is moving at
     * @param mode throttle mode the boat is in
     * @param holdTicks ticks the boat keeps its throttle and heading before the plan may change them
     * @param boostMoves how many moves from the start the trimmed gust boosts
     * @param rapids rapids on the sea
     * @param rapidsBoost rapids boost the boat starts with
     * @param entering stacks the rapids the boat is entering at the start give
     * @param goal tile to reach
     * @param hull hull of the boat
     * @param avoided water families to keep out of
     * @param avoidedTiles tiles that cost extra to cross
     * @return the route, or an empty route when none was found
     */
    public SeaRoute plan(BoatPose start, int speed, int mode, int holdTicks, int boostMoves, Rapids rapids, RapidsBoost rapidsBoost, int entering, WorldPoint goal, BoatHull hull, Set<WaterFamily> avoided, Set<WorldPoint> avoidedTiles)
    {
        return plan(start, speed, mode, holdTicks, new Gust(0, boostMoves), rapids, rapidsBoost, entering, goal, GOAL_REACH, true, hull, avoided, avoidedTiles);
    }

    /**
     * Plans from a pose under way to pass within reach of a tile at whatever speed, for a run of waypoints sailed through rather than stopped at, as a trial's crates are
     * @param start pose the boat starts from
     * @param speed speed the boat is moving at
     * @param mode throttle mode the boat is in
     * @param holdTicks ticks the boat keeps its throttle and heading before the plan may change them
     * @param goal tile to reach
     * @param reach how many tiles from the goal count as reached
     * @param hull hull of the boat
     * @param avoided water families to keep out of
     * @param avoidedTiles tiles that cost extra to cross
     * @return the route, or an empty route when none was found
     */
    public SeaRoute planThrough(BoatPose start, int speed, int mode, int holdTicks, WorldPoint goal, int reach, BoatHull hull, Set<WaterFamily> avoided, Set<WorldPoint> avoidedTiles)
    {
        return planThrough(start, speed, mode, holdTicks, 0, goal, reach, hull, avoided, avoidedTiles);
    }

    /**
     * Plans from a pose under way to pass within reach of a tile, with a trimmed gust boosting the first moves
     * @param start pose the boat starts from
     * @param speed speed the boat is moving at
     * @param mode throttle mode the boat is in
     * @param holdTicks ticks the boat keeps its throttle and heading before the plan may change them
     * @param boostMoves how many moves from the start the trimmed gust boosts
     * @param goal tile to reach
     * @param reach how many tiles from the goal count as reached
     * @param hull hull of the boat
     * @param avoided water families to keep out of
     * @param avoidedTiles tiles that cost extra to cross
     * @return the route, or an empty route when none was found
     */
    public SeaRoute planThrough(BoatPose start, int speed, int mode, int holdTicks, int boostMoves, WorldPoint goal, int reach, BoatHull hull, Set<WaterFamily> avoided, Set<WorldPoint> avoidedTiles)
    {
        return planThrough(start, speed, mode, holdTicks, boostMoves, Rapids.NONE, RapidsBoost.NONE, 0, goal, reach, hull, avoided, avoidedTiles);
    }

    /**
     * Plans from a pose under way to pass within reach of a tile, with a trimmed gust boosting the first moves and rapids boosting the boat wherever the route crosses them
     * @param start pose the boat starts from
     * @param speed speed the boat is moving at
     * @param mode throttle mode the boat is in
     * @param holdTicks ticks the boat keeps its throttle and heading before the plan may change them
     * @param boostMoves how many moves from the start the trimmed gust boosts
     * @param rapids rapids on the sea
     * @param rapidsBoost rapids boost the boat starts with
     * @param entering stacks the rapids the boat is entering at the start give
     * @param goal tile to reach
     * @param reach how many tiles from the goal count as reached
     * @param hull hull of the boat
     * @param avoided water families to keep out of
     * @param avoidedTiles tiles that cost extra to cross
     * @return the route, or an empty route when none was found
     */
    public SeaRoute planThrough(BoatPose start, int speed, int mode, int holdTicks, int boostMoves, Rapids rapids, RapidsBoost rapidsBoost, int entering, WorldPoint goal, int reach, BoatHull hull, Set<WaterFamily> avoided, Set<WorldPoint> avoidedTiles)
    {
        if (reach < 1)
        {
            throw new IllegalArgumentException("reach must be at least 1, was " + reach);
        }
        return plan(start, speed, mode, holdTicks, new Gust(0, boostMoves), rapids, rapidsBoost, entering, goal, reach, false, hull, avoided, avoidedTiles);
    }

    /**
     * Plans from a pose under way to a stop anywhere within reach of a tile, as for work done from the boat beside something
     * @param start pose the boat starts from
     * @param speed speed the boat is moving at
     * @param mode throttle mode the boat is in
     * @param holdTicks ticks the boat keeps its throttle and heading before the plan may change them
     * @param boostMoves how many moves from the start the trimmed gust boosts
     * @param rapids rapids on the sea
     * @param rapidsBoost rapids boost the boat starts with
     * @param entering stacks the rapids the boat is entering at the start give
     * @param goal tile to reach
     * @param reach how many tiles from the goal count as reached
     * @param hull hull of the boat
     * @param avoided water families to keep out of
     * @param avoidedTiles tiles that cost extra to cross
     * @return the route, or an empty route when none was found
     */
    public SeaRoute planWithin(BoatPose start, int speed, int mode, int holdTicks, int boostMoves, Rapids rapids, RapidsBoost rapidsBoost, int entering, WorldPoint goal, int reach, BoatHull hull, Set<WaterFamily> avoided, Set<WorldPoint> avoidedTiles)
    {
        if (reach < 1)
        {
            throw new IllegalArgumentException("reach must be at least 1, was " + reach);
        }
        return plan(start, speed, mode, holdTicks, new Gust(0, boostMoves), rapids, rapidsBoost, entering, goal, reach, true, hull, avoided, avoidedTiles);
    }

    /**
     * A tile a route passes within reach of on its way, as a trial's crate is
     */
    public static final class Objective
    {
        private final WorldPoint tile;
        private final int reach;
        private final boolean byHull;
        private final boolean swept;

        public Objective(WorldPoint tile, int reach, boolean byHull, boolean swept)
        {
            requireNonNull(tile, "tile");
            if (reach < 1)
            {
                throw new IllegalArgumentException("reach must be at least 1, was " + reach);
            }
            if (swept && byHull)
            {
                throw new IllegalArgumentException("a swept objective is reckoned from the pivot");
            }
            this.tile = tile;
            this.reach = reach;
            this.byHull = byHull;
            this.swept = swept;
        }

        /**
         * Creates an objective passed only where the boat lies within reach at a tick
         * @param tile tile to pass
         * @param reach how many tiles from the tile count as passed
         * @param byHull whether reach is measured from the hull rather than the pivot's tile
         */
        public Objective(WorldPoint tile, int reach, boolean byHull)
        {
            this(tile, reach, byHull, false);
        }

        /**
         * Creates an objective reached when the pivot's tile comes within reach of it
         * @param tile tile to pass
         * @param reach how many tiles from the tile count as passed
         */
        public Objective(WorldPoint tile, int reach)
        {
            this(tile, reach, false);
        }

        /**
         * Checks whether the boat at that pose has passed within reach
         * @param pose pose of the boat
         * @param hull hull of the boat
         * @return true if the boat is within reach
         */
        public boolean passedBy(BoatPose pose, BoatHull hull)
        {
            return passedBy(pose.fineX(), pose.fineY(), pose.angle(), hull);
        }

        /**
         * Checks whether the boat has passed within reach at the pose now, the hull turned as it lay before
         * @param from pose the boat left, or null at the start
         * @param to pose the boat is at now
         * @param hull hull of the boat
         * @return true if the boat has passed within reach
         */
        public boolean passedBetween(BoatPose from, BoatPose to, BoatHull hull)
        {
            return from == null ? passedBy(to, hull) : passedBetween(from.fineX(), from.fineY(), to.fineX(), to.fineY(), from.angle(), hull);
        }

        /**
         * Checks whether the boat has passed within reach where it lies now
         * @param fromX fine x the pivot left
         * @param fromY fine y the pivot left
         * @param toX fine x the pivot is at now
         * @param toY fine y the pivot is at now
         * @param angle angle the hull lies at
         * @param hull hull of the boat
         * @return true if the boat has passed within reach
         */
        public boolean passedBetween(int fromX, int fromY, int toX, int toY, int angle, BoatHull hull)
        {
            return passedBy(toX, toY, angle, hull) || swept && crosses(fromX, fromY, toX, toY);
        }

        /**
         * Checks whether the pivot's line from one point to another crosses the tiles within reach
         * @param fromX fine x the line starts at
         * @param fromY fine y the line starts at
         * @param toX fine x the line ends at
         * @param toY fine y the line ends at
         * @return true if the line crosses the tiles within reach
         */
        private boolean crosses(int fromX, int fromY, int toX, int toY)
        {
            int minX = (tile.getX() - reach) << TILE_SHIFT;
            int maxX = ((tile.getX() + reach + 1) << TILE_SHIFT) - 1;
            int minY = (tile.getY() - reach) << TILE_SHIFT;
            int maxY = ((tile.getY() + reach + 1) << TILE_SHIFT) - 1;
            double enter = Math.max(entered(fromX, toX, minX, maxX), entered(fromY, toY, minY, maxY));
            double leave = Math.min(left(fromX, toX, minX, maxX), left(fromY, toY, minY, maxY));
            return enter <= leave;
        }

        /**
         * Finds the share of the way along one axis at which the line comes within the range, past 1 if it never does
         * @param from where the line starts on the axis
         * @param to where the line ends on the axis
         * @param min lowest point of the range
         * @param max highest point of the range
         * @return the share of the way at which the line enters the range
         */
        private static double entered(int from, int to, int min, int max)
        {
            if (from == to)
            {
                return from >= min && from <= max ? 0 : Double.POSITIVE_INFINITY;
            }
            double step = to - from;
            return Math.max(0, Math.min((min - from) / step, (max - from) / step));
        }

        /**
         * Finds the share of the way along one axis at which the line leaves the range, below 0 if it never was in it
         * @param from where the line starts on the axis
         * @param to where the line ends on the axis
         * @param min lowest point of the range
         * @param max highest point of the range
         * @return the share of the way at which the line leaves the range
         */
        private static double left(int from, int to, int min, int max)
        {
            if (from == to)
            {
                return from >= min && from <= max ? 1 : Double.NEGATIVE_INFINITY;
            }
            double step = to - from;
            return Math.min(1, Math.max((min - from) / step, (max - from) / step));
        }

        /**
         * Checks whether the boat there has passed within reach
         * @param fineX fine x of the pivot
         * @param fineY fine y of the pivot
         * @param angle angle the hull lies at
         * @param hull hull of the boat
         * @return true if the boat is within reach
         */
        public boolean passedBy(int fineX, int fineY, int angle, BoatHull hull)
        {
            if (!byHull)
            {
                return Math.max(Math.abs((fineX >> TILE_SHIFT) - tile.getX()), Math.abs((fineY >> TILE_SHIFT) - tile.getY())) <= reach;
            }
            double radians = angle * (2 * Math.PI / Compass.TURN);
            double rx = (tile.getX() << TILE_SHIFT) + HALF_TILE - fineX;
            double ry = (tile.getY() << TILE_SHIFT) + HALF_TILE - fineY;
            double along = -rx * Math.sin(radians) - ry * Math.cos(radians);
            double across = rx * Math.cos(radians) - ry * Math.sin(radians);
            return Math.abs(along) <= hull.halfAlong() + (reach << TILE_SHIFT) && Math.abs(across) <= hull.halfAcross() + (reach << TILE_SHIFT);
        }

        /**
         * Gets how far the pivot may lie from the tile's middle along either axis and still pass, in fine units
         * @param hull hull of the boat
         * @return the pivot's reach in fine units
         */
        int pivotReach(BoatHull hull)
        {
            if (!byHull)
            {
                return (reach << TILE_SHIFT) + HALF_TILE;
            }
            return (int) Math.ceil(Math.hypot(hull.halfAlong() + (reach << TILE_SHIFT), hull.halfAcross() + (reach << TILE_SHIFT)));
        }

        /**
         * Gets how many tiles about the tile a pivot passes it from whichever way the boat faces
         * @param hull hull of the boat
         * @return the tiles about the tile that surely pass it
         */
        int surelyWithin(BoatHull hull)
        {
            return byHull ? reach + (Math.min(hull.halfAlong(), hull.halfAcross()) >> TILE_SHIFT) : 1;
        }

        public WorldPoint tile()
        {
            return tile;
        }

        public int reach()
        {
            return reach;
        }

        public boolean byHull()
        {
            return byHull;
        }

        public boolean swept()
        {
            return swept;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Objective))
            {
                return false;
            }
            Objective other = (Objective) o;
            return Objects.equals(tile, other.tile) && reach == other.reach && byHull == other.byHull && swept == other.swept;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(tile, reach, byHull, swept);
        }

        @Override
        public String toString()
        {
            return "Objective[" + "tile=" + tile + ", " + "reach=" + reach + ", " + "byHull=" + byHull + ", " + "swept=" + swept + "]";
        }
    }

    /**
     * Plans from a pose under way to pass within reach of several tiles in turn without stopping, the last of them the route's end, in one search
     * @param start pose the boat starts from
     * @param speed speed the boat is moving at
     * @param mode throttle mode the boat is in
     * @param holdTicks ticks the boat keeps its throttle and heading before the plan may change them
     * @param boostMoves how many moves from the start the trimmed gust boosts
     * @param rapids rapids on the sea
     * @param rapidsBoost rapids boost the boat starts with
     * @param entering stacks the rapids the boat is entering at the start give
     * @param objectives tiles to pass within reach of in turn
     * @param hull hull of the boat
     * @param avoided water families to keep out of
     * @param avoidedTiles tiles that cost extra to cross
     * @return the route, or an empty route when none was found
     */
    public SeaRoute planThroughAll(BoatPose start, int speed, int mode, int holdTicks, int boostMoves, Rapids rapids, RapidsBoost rapidsBoost, int entering, List<Objective> objectives, BoatHull hull, Set<WaterFamily> avoided, Set<WorldPoint> avoidedTiles)
    {
        requireNonNull(objectives, "objectives");
        if (objectives.isEmpty() || objectives.size() > SeaGrid.MOST_OBJECTIVES)
        {
            throw new IllegalArgumentException("there must be one to " + SeaGrid.MOST_OBJECTIVES + " objectives, were " + objectives.size());
        }
        return plan(start, speed, mode, holdTicks, new Gust(0, boostMoves), rapids, rapidsBoost, entering, List.copyOf(objectives), false, hull, avoided, avoidedTiles);
    }

    /**
     * Plans ahead from where the boat is through up to four objectives in turn, the last passed or stopped within, as the pilot sails a race or a voyage
     * @param start pose the boat starts from
     * @param speed speed the boat is moving at
     * @param mode throttle mode the boat is in
     * @param gust trimmed gust boosting the boat
     * @param rapids rapids on the sea
     * @param rapidsBoost rapids boost the boat starts with
     * @param entering stacks the rapids the boat is entering at the start give
     * @param objectives tiles to pass within reach of in turn
     * @param stop whether the boat must stop within the last objective
     * @param hull hull of the boat
     * @param avoided water families to keep out of
     * @param avoidedTiles tiles that cost extra to cross
     * @param obstacles what the hull must not touch
     * @return the route, the best partial one when no full route was found, or an empty route
     */
    public SeaRoute planAhead(BoatPose start, int speed, int mode, Gust gust, Rapids rapids, RapidsBoost rapidsBoost, int entering, List<Objective> objectives, boolean stop, BoatHull hull, Set<WaterFamily> avoided, Set<WorldPoint> avoidedTiles, Obstacles obstacles)
    {
        return planAhead(start, speed, mode, gust, rapids, rapidsBoost, entering, BoatKinematics.Fouling.NONE, objectives, stop, hull, avoided, avoidedTiles, obstacles);
    }

    /**
     * Plans ahead as the overload without fouling does, from a boat the fetid pools have left something on
     * @param start pose the boat starts from
     * @param speed speed the boat is moving at
     * @param mode throttle mode the boat is in
     * @param gust trimmed gust boosting the boat
     * @param rapids rapids on the sea
     * @param rapidsBoost rapids boost the boat starts with
     * @param entering stacks the rapids the boat is entering at the start give
     * @param fouling what the fetid pools have left on the boat
     * @param objectives tiles to pass within reach of in turn
     * @param stop whether the boat must stop within the last objective
     * @param hull hull of the boat
     * @param avoided water families to keep out of
     * @param avoidedTiles tiles that cost extra to cross
     * @param obstacles what the hull must not touch
     * @return the route, the best partial one when no full route was found, or an empty route
     */
    public SeaRoute planAhead(BoatPose start, int speed, int mode, Gust gust, Rapids rapids, RapidsBoost rapidsBoost, int entering, BoatKinematics.Fouling fouling, List<Objective> objectives, boolean stop, BoatHull hull, Set<WaterFamily> avoided, Set<WorldPoint> avoidedTiles, Obstacles obstacles)
    {
        requireNonNull(gust, "gust");
        requireNonNull(fouling, "fouling");
        requireNonNull(objectives, "objectives");
        requireNonNull(obstacles, "obstacles");
        if (objectives.isEmpty() || objectives.size() > SeaGrid.MOST_OBJECTIVES)
        {
            throw new IllegalArgumentException("there must be one to " + SeaGrid.MOST_OBJECTIVES + " objectives, were " + objectives.size());
        }
        return plan(start, speed, mode, 0, gust, rapids, rapidsBoost, entering, fouling, List.copyOf(objectives), stop, hull, avoided, avoidedTiles, obstacles, true);
    }

    /**
     * Counts how many times a line over the whole sea was searched, as a route through the web's corridor was not found
     * @return the number of sea lines searched
     */
    int seaLinesSearched()
    {
        return seaLinesSearched.get();
    }

    /**
     * The last line searched over the whole sea, kept for the plans made again on the way to the same objectives
     */
    private static final class KeptLine
    {
        private final List<WorldPoint> goals;
        private final int halfAcross;
        private final int halfAlong;
        private final Set<WaterFamily> hazards;
        private final List<WorldPoint> line;

        KeptLine(List<WorldPoint> goals, int halfAcross, int halfAlong, Set<WaterFamily> hazards, List<WorldPoint> line)
        {
            this.goals = goals;
            this.halfAcross = halfAcross;
            this.halfAlong = halfAlong;
            this.hazards = hazards;
            this.line = line;
        }

        /**
         * Gets the rest of the line from its tile nearest a start within its corridor, the start first, for the same objectives, hull and waters
         * @param start tile to start from
         * @param objectives objectives the plan sails for
         * @param hull hull of the boat
         * @param hazards water families to keep out of
         * @return the rest of the line, or an empty list when it does not fit the plan
         */
        List<WorldPoint> from(WorldPoint start, List<Objective> objectives, BoatHull hull, Set<WaterFamily> hazards)
        {
            if (halfAcross != hull.halfAcross() || halfAlong != hull.halfAlong() || !this.hazards.equals(hazards) || !goals.equals(objectives.stream().map(Objective::tile).collect(Collectors.toList())))
            {
                return List.of();
            }
            int nearest = -1;
            int nearestDistance = Integer.MAX_VALUE;
            for (int index = 0; index < line.size(); index++)
            {
                WorldPoint tile = line.get(index);
                int distance = Math.max(Math.abs(tile.getX() - start.getX()), Math.abs(tile.getY() - start.getY()));
                if (distance < nearestDistance)
                {
                    nearest = index;
                    nearestDistance = distance;
                }
            }
            if (nearestDistance > LINE_CORRIDOR)
            {
                return List.of();
            }
            List<WorldPoint> rest = new ArrayList<>(line.size() - nearest + 1);
            rest.add(start);
            rest.addAll(line.subList(Math.min(line.size() - 1, nearest + 1), line.size()));
            return rest;
        }

        public List<WorldPoint> goals()
        {
            return goals;
        }

        public int halfAcross()
        {
            return halfAcross;
        }

        public int halfAlong()
        {
            return halfAlong;
        }

        public Set<WaterFamily> hazards()
        {
            return hazards;
        }

        public List<WorldPoint> line()
        {
            return line;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof KeptLine))
            {
                return false;
            }
            KeptLine other = (KeptLine) o;
            return Objects.equals(goals, other.goals) && halfAcross == other.halfAcross && halfAlong == other.halfAlong && Objects.equals(hazards, other.hazards) && Objects.equals(line, other.line);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(goals, halfAcross, halfAlong, hazards, line);
        }

        @Override
        public String toString()
        {
            return "KeptLine[" + "goals=" + goals + ", " + "halfAcross=" + halfAcross + ", " + "halfAlong=" + halfAlong + ", " + "hazards=" + hazards + ", " + "line=" + line + "]";
        }
    }

    /**
     * Checks whether a hull sailing poses a tick apart keeps off what lies in the way, tick by tick from one of them, as a plan's search keeps it
     * @param poses poses a tick apart
     * @param from index of the pose to check from
     * @param hull hull of the boat
     * @param obstacles what the hull must not touch
     * @return true if the hull keeps clear
     */
    public static boolean clearOf(List<BoatPose> poses, int from, BoatHull hull, Obstacles obstacles)
    {
        requireNonNull(poses, "poses");
        requireNonNull(hull, "hull");
        requireNonNull(obstacles, "obstacles");
        if (from < 0 || from >= poses.size())
        {
            throw new IllegalArgumentException("from must be the index of a pose, was " + from + " of " + poses.size());
        }
        if (obstacles.isEmpty())
        {
            return true;
        }
        Barriers barriers = new Barriers(obstacles, hull, hull.tiles(poses.get(from)));
        for (int index = from + 1; index < poses.size(); index++)
        {
            BoatPose pose = poses.get(index);
            if (barriers.struck(pose.fineX(), pose.fineY(), pose.angle(), index - from))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * The moves a trimmed gust boosts at full sail, counted from the start
     */
    public static final class Gust
    {
        private final int from;
        private final int moves;
        private final int immune;

        /**
         * No gust
         */
        public static final Gust NONE = new Gust(0, 0, 0);

        public Gust(int from, int moves, int immune)
        {
            if (from < 0 || moves < 0 || immune < 0)
            {
                throw new IllegalArgumentException("from, moves and immune cannot be negative, were " + from + ", " + moves + " and " + immune);
            }
            this.from = from;
            this.moves = moves;
            this.immune = immune;
        }

        /**
         * Creates a gust with no immunity from the pools
         * @param from move the boost starts at
         * @param moves how many moves the boost lasts
         */
        public Gust(int from, int moves)
        {
            this(from, moves, 0);
        }

        /**
         * Checks whether the move out of the pose some moves from the start is one the gust boosts
         * @param depth moves from the start
         * @return true if the gust boosts that move
         */
        boolean boosts(int depth)
        {
            return depth >= from && depth < from + moves;
        }

        /**
         * Checks whether the pools leave the boat be on the move out of the pose some moves from the start
         * @param depth moves from the start
         * @return true if the boat is immune to the pools on that move
         */
        boolean shields(int depth)
        {
            int first = Math.max(0, from - 1);
            return depth >= first && depth < first + immune;
        }

        public int from()
        {
            return from;
        }

        public int moves()
        {
            return moves;
        }

        public int immune()
        {
            return immune;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Gust))
            {
                return false;
            }
            Gust other = (Gust) o;
            return from == other.from && moves == other.moves && immune == other.immune;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(from, moves, immune);
        }

        @Override
        public String toString()
        {
            return "Gust[" + "from=" + from + ", " + "moves=" + moves + ", " + "immune=" + immune + "]";
        }
    }

    /**
     * What a hull must not touch
     */
    public static final class Obstacles
    {
        private final Set<WorldPoint> always;
        private final List<Set<WorldPoint>> byTick;

        /**
         * Nothing in the way
         */
        public static final Obstacles NONE = new Obstacles(Set.of(), List.of());

        public Obstacles(Set<WorldPoint> always, List<Set<WorldPoint>> byTick)
        {
            requireNonNull(always, "always");
            requireNonNull(byTick, "byTick");
            always = Set.copyOf(always);
            byTick = byTick.stream().map(Set::copyOf).collect(Collectors.toList());
            this.always = always;
            this.byTick = byTick;
        }

        /**
         * Checks whether nothing is in the way
         * @return true if there are no obstacles
         */
        public boolean isEmpty()
        {
            return always.isEmpty() && byTick.stream().allMatch(Set::isEmpty);
        }

        public Set<WorldPoint> always()
        {
            return always;
        }

        public List<Set<WorldPoint>> byTick()
        {
            return byTick;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Obstacles))
            {
                return false;
            }
            Obstacles other = (Obstacles) o;
            return Objects.equals(always, other.always) && Objects.equals(byTick, other.byTick);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(always, byTick);
        }

        @Override
        public String toString()
        {
            return "Obstacles[" + "always=" + always + ", " + "byTick=" + byTick + "]";
        }
    }

    private SeaRoute plan(BoatPose start, int speed, int mode, int holdTicks, Gust gust, Rapids rapids, RapidsBoost rapidsBoost, int entering, WorldPoint goal, int reach, boolean stop, BoatHull hull, Set<WaterFamily> avoided, Set<WorldPoint> avoidedTiles)
    {
        requireNonNull(goal, "goal");
        return plan(start, speed, mode, holdTicks, gust, rapids, rapidsBoost, entering, List.of(new Objective(goal, reach)), stop, hull, avoided, avoidedTiles);
    }

    private SeaRoute plan(BoatPose start, int speed, int mode, int holdTicks, Gust gust, Rapids rapids, RapidsBoost rapidsBoost, int entering, List<Objective> objectives, boolean stop, BoatHull hull, Set<WaterFamily> avoided, Set<WorldPoint> avoidedTiles)
    {
        return plan(start, speed, mode, holdTicks, gust, rapids, rapidsBoost, entering, BoatKinematics.Fouling.NONE, objectives, stop, hull, avoided, avoidedTiles, Obstacles.NONE, false);
    }

    private SeaRoute plan(BoatPose start, int speed, int mode, int holdTicks, Gust gust, Rapids rapids, RapidsBoost rapidsBoost, int entering, BoatKinematics.Fouling fouling, List<Objective> objectives, boolean stop, BoatHull hull, Set<WaterFamily> avoided, Set<WorldPoint> avoidedTiles, Obstacles obstacles, boolean anytime)
    {
        requireNonNull(start, "start");
        requireNonNull(rapids, "rapids");
        requireNonNull(rapidsBoost, "rapidsBoost");
        WorldPoint goal = objectives.get(objectives.size() - 1).tile();
        requireNonNull(hull, "hull");
        requireNonNull(avoided, "avoided");
        requireNonNull(avoidedTiles, "avoidedTiles");
        if (holdTicks < 0 || entering < 0)
        {
            throw new IllegalArgumentException("holdTicks and entering cannot be negative, were " + holdTicks + " and " + entering);
        }
        int speedIndex = speedIndex(speed);
        int modeIndex = modeIndex(mode);
        long started = System.nanoTime();
        for (Objective objective : objectives)
        {
            WorldPoint tile = objective.tile();
            if (!sea.isWater(tile.getX(), tile.getY()) || avoided.contains(sea.family(tile.getX(), tile.getY())))
            {
                return SeaRoute.none(goal, new SeaRoute.Stats(millisSince(started), 0, 0, 0, 0, 0, 0, 0, 0));
            }
        }
        Set<WaterFamily> hazards = hazards(avoided);
        KeptLine kept = keptLine.get();
        List<WorldPoint> keptFromHere = kept == null ? List.of() : kept.from(start.tile(), objectives, hull, hazards);
        Search search = null;
        Way way = null;
        if (!keptFromHere.isEmpty())
        {
            search = new Search(hull, hazards, avoidedTiles, keptFromHere, LINE_CORRIDOR, objectives, stop, holdTicks, gust, rapids, entering, PLAIN_SLACK, obstacles, anytime);
            way = search.run(start, speedIndex, modeIndex, rapidsBoost, fouling);
        }
        List<WorldPoint> line = keptFromHere;
        int corridor = LINE_CORRIDOR;
        if (way == null && (search == null || search.unreachable || search.exhausted))
        {
            line = legLines(start.tile(), objectives, (from, to) -> webLine(from, to, avoided));
            corridor = CORRIDOR;
            search = new Search(hull, hazards, avoidedTiles, line, corridor, objectives, stop, holdTicks, gust, rapids, entering, PLAIN_SLACK, obstacles, anytime);
            way = search.run(start, speedIndex, modeIndex, rapidsBoost, fouling);
        }
        Search first = search;
        if (way == null && (search.unreachable || search.exhausted) && !line.isEmpty())
        {
            seaLinesSearched.incrementAndGet();
            List<WorldPoint> seaLine = legLines(start.tile(), objectives, (from, to) -> new SeaLine(hull, hazards, avoidedTiles, to).find(from));
            if (!seaLine.isEmpty())
            {
                keptLine.set(new KeptLine(objectives.stream().map(Objective::tile).collect(Collectors.toList()), hull.halfAcross(), hull.halfAlong(), hazards, seaLine));
                line = seaLine;
                corridor = LINE_CORRIDOR;
                search = new Search(hull, hazards, avoidedTiles, line, corridor, objectives, stop, holdTicks, gust, rapids, entering, PLAIN_SLACK, obstacles, anytime);
                way = search.run(start, speedIndex, modeIndex, rapidsBoost, fouling);
            }
        }
        if (way == null && search.exhausted && !anytime)
        {
            search = new Search(hull, hazards, avoidedTiles, line, corridor, objectives, stop, holdTicks, gust, rapids, entering, WIDER_SLACK, obstacles, anytime);
            way = search.run(start, speedIndex, modeIndex, rapidsBoost, fouling);
        }
        if (way == null && anytime)
        {
            way = search.partial(start);
            if (way == null && search != first)
            {
                way = first.partial(start);
            }
        }
        SeaRoute.Stats stats = search.stats(millisSince(started));
        return way == null ? SeaRoute.none(goal, stats) : new SeaRoute(way.poses(), way.throttles(), way.speeds(), goal, stats);
    }

    /**
     * Joins the line through every leg, start to the first objective and each to the next, each leg's line from the way given
     * @param start tile to start from
     * @param objectives objectives in turn
     * @param leg finds a leg's line between two tiles
     * @return the joined line, or an empty list when any leg has none
     */
    private static List<WorldPoint> legLines(WorldPoint start, List<Objective> objectives, BiFunction<WorldPoint, WorldPoint, List<WorldPoint>> leg)
    {
        List<WorldPoint> line = new ArrayList<>();
        WorldPoint from = start;
        for (Objective objective : objectives)
        {
            List<WorldPoint> part = leg.apply(from, objective.tile());
            if (part.isEmpty())
            {
                return List.of();
            }
            line.addAll(line.isEmpty() ? part : part.subList(1, part.size()));
            from = objective.tile();
        }
        return line;
    }

    private static long millisSince(long startedNanos)
    {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }

    private int speedIndex(int speed)
    {
        for (int i = 0; i < speeds.length; i++)
        {
            if (speeds[i] == speed)
            {
                return i;
            }
        }
        throw new IllegalArgumentException("speed " + speed + " is not one the boat moves at");
    }

    private static int modeIndex(int mode)
    {
        for (int i = 0; i < MODES.length; i++)
        {
            if (MODES[i] == mode)
            {
                return i;
            }
        }
        throw new IllegalArgumentException("mode " + mode + " is not a throttle mode");
    }

    private List<WorldPoint> webLine(WorldPoint from, WorldPoint to, Set<WaterFamily> avoided)
    {
        int first = graph.nearestNode(from.getX(), from.getY(), 0, NODE_RADIUS);
        int last = graph.nearestNode(to.getX(), to.getY(), 0, NODE_RADIUS);
        if (first == -1 || last == -1)
        {
            return Collections.emptyList();
        }
        List<WorldPoint> nodes = graph.path(first, last, avoided);
        if (nodes.isEmpty())
        {
            return Collections.emptyList();
        }
        List<WorldPoint> line = new ArrayList<>(nodes.size() + 2);
        line.add(from);
        line.addAll(nodes);
        line.add(to);
        return line;
    }

    private static Set<WaterFamily> hazards(Set<WaterFamily> avoided)
    {
        Set<WaterFamily> hazards = EnumSet.noneOf(WaterFamily.class);
        for (WaterFamily family : avoided)
        {
            if (family != WaterFamily.NONE && family != WaterFamily.INLAND)
            {
                hazards.add(family);
            }
        }
        return hazards;
    }

    private static long tileKey(int x, int y)
    {
        return (x & COORDINATE_MASK) | (long) (y & COORDINATE_MASK) << COORDINATE_BITS;
    }

    /**
     * Builds the compass point a step of up to two tiles along each axis faces, by the step's x and y offsets plus two
     * @return the table of headings
     */
    private static int[][] stepHeadings()
    {
        int[][] table = new int[2 * TICK_REACH + 1][2 * TICK_REACH + 1];
        for (int dx = -TICK_REACH; dx <= TICK_REACH; dx++)
        {
            for (int dy = -TICK_REACH; dy <= TICK_REACH; dy++)
            {
                table[dx + TICK_REACH][dy + TICK_REACH] = Compass.of(Compass.bearing(0, 0, dx, dy));
            }
        }
        return table;
    }

    /**
     * Builds how far a tick moves the boat along an axis at each speed and heading, as the kinematics round it
     * @param speeds speeds the boat moves at
     * @param alongX whether the axis is x rather than y
     * @return the table of advances by speed and heading
     */
    private static int[][] advances(int[] speeds, boolean alongX)
    {
        int[][] table = new int[speeds.length][Compass.POINTS];
        for (int speed = 0; speed < speeds.length; speed++)
        {
            for (int heading = 0; heading < Compass.POINTS; heading++)
            {
                int angle = Compass.bearingOf(heading);
                table[speed][heading] = alongX ? BoatKinematics.advanceX(speeds[speed], angle) : BoatKinematics.advanceY(speeds[speed], angle);
            }
        }
        return table;
    }

    /**
     * Builds how many ticks' cost a state may lose to the best one at its tile, heading and speed and still be searched, by speed
     * @param speeds speeds the boat moves at
     * @param profile how the boat moves
     * @return the slack ticks by speed
     */
    private static int[] slacks(int[] speeds, BoatProfile profile)
    {
        int[] table = new int[speeds.length];
        for (int i = 0; i < speeds.length; i++)
        {
            int speed = speeds[i];
            table[i] = speed < 0 ? ASTERN_SLACK : speed == 0 || speed >= profile.fullSpeed() ? 0 : speed == profile.slowSpeed() ? SLOW_SLACK : RAMPING_SLACK;
        }
        return table;
    }

    int advanceX(int speedIndex, int heading)
    {
        return advanceXs[speedIndex][heading];
    }

    int advanceY(int speedIndex, int heading)
    {
        return advanceYs[speedIndex][heading];
    }

    int speedAt(int speedIndex)
    {
        return speeds[speedIndex];
    }

    int speedCount()
    {
        return speeds.length;
    }

    private static int[][] reachables()
    {
        int[][] table = new int[MODES.length][];
        for (int mode = 0; mode < MODES.length; mode++)
        {
            table[mode] = reachable(mode);
        }
        return table;
    }

    private static int[][] towardFulls()
    {
        int[][] table = new int[MODES.length][];
        for (int mode = 0; mode < MODES.length; mode++)
        {
            table[mode] = new int[] {towardFull(mode)};
        }
        return table;
    }

    private static long poseKey(int fineX, int fineY, int heading)
    {
        return ((long) (fineX >> GRID_SHIFT) & CELL_MASK) | ((long) (fineY >> GRID_SHIFT) & CELL_MASK) << CELL_BITS | (long) heading << (2 * CELL_BITS);
    }

    private static long stateKey(int fineX, int fineY, int heading, int speedIndex, int modeIndex, int boost, int objective, int fouling, boolean withinLast)
    {
        return poseKey(fineX, fineY, heading) | (long) speedIndex << (2 * CELL_BITS + HEADING_BITS) | (long) modeIndex << MODE_SHIFT | (long) RapidsBoost.stacksOf(boost) << STACK_SHIFT | (long) RapidsBoost.clockOf(boost) << CLOCK_SHIFT | (long) objective << OBJECTIVE_SHIFT | (long) fouling << FOUL_SHIFT | (withinLast ? 1L << WITHIN_LAST_SHIFT : 0);
    }

    /**
     * Checks whether the move into a state left the boat within the last objective's reach, the hull turned as it lay the move before
     * @param key packed state
     * @return true if the boat lies within the last objective's reach
     */
    private static boolean stateWithinLast(long key)
    {
        return (key >>> WITHIN_LAST_SHIFT & 1) != 0;
    }

    /**
     * Packs a state's fouling as the key holds it
     * @param moves moves of fouling left
     * @param contact whether the boat lies on the pools
     * @return the packed fouling
     */
    private static int fouling(int moves, boolean contact)
    {
        return moves | (contact ? 1 << FOUL_BITS : 0);
    }

    private static int stateFoulMoves(long key)
    {
        return (int) (key >>> FOUL_SHIFT) & ((1 << FOUL_BITS) - 1);
    }

    private static boolean stateContact(long key)
    {
        return (key >>> CONTACT_SHIFT & 1) != 0;
    }

    private static int stateFineX(long key)
    {
        return (int) (key & CELL_MASK) << GRID_SHIFT;
    }

    private static int stateFineY(long key)
    {
        return (int) ((key >> CELL_BITS) & CELL_MASK) << GRID_SHIFT;
    }

    private static int stateHeading(long key)
    {
        return (int) (key >> (2 * CELL_BITS)) & ((1 << HEADING_BITS) - 1);
    }

    private static int stateSpeed(long key)
    {
        return (int) (key >> (2 * CELL_BITS + HEADING_BITS)) & ((1 << SPEED_BITS) - 1);
    }

    private static int stateMode(long key)
    {
        return (int) (key >> MODE_SHIFT) & ((1 << MODE_BITS) - 1);
    }

    private static int stateStacks(long key)
    {
        return (int) (key >> STACK_SHIFT) & ((1 << STACK_BITS) - 1);
    }

    private static int stateClock(long key)
    {
        return (int) (key >> CLOCK_SHIFT) & ((1 << CLOCK_BITS) - 1);
    }

    private static int stateObjective(long key)
    {
        return (int) (key >> OBJECTIVE_SHIFT) & ((1 << OBJECTIVE_BITS) - 1);
    }

    /**
     * Gets the throttle modes one click from a mode
     * @param modeIndex index of the mode
     * @return the indices of the modes reachable
     */
    private static int[] reachable(int modeIndex)
    {
        switch (MODES[modeIndex])
        {
            case BoatKinematics.READY:
                return new int[] {modeIndex(BoatKinematics.FULL), modeIndex(BoatKinematics.REVERSE)};
            case BoatKinematics.REVERSE:
                return new int[] {modeIndex, modeIndex(BoatKinematics.STOPPED)};
            case BoatKinematics.STOPPED:
                return new int[] {modeIndex, modeIndex(BoatKinematics.SLOW), modeIndex(BoatKinematics.REVERSE)};
            case BoatKinematics.SLOW:
                return new int[] {modeIndex, modeIndex(BoatKinematics.FULL), modeIndex(BoatKinematics.STOPPED)};
            default:
                return new int[] {modeIndex, modeIndex(BoatKinematics.SLOW), modeIndex(BoatKinematics.STOPPED)};
        }
    }

    /**
     * Checks whether a mode drives the boat ahead under sail
     * @param modeIndex index of the mode
     * @return true if the sails are set
     */
    private static boolean sailsSet(int modeIndex)
    {
        return MODES[modeIndex] == BoatKinematics.SLOW || MODES[modeIndex] == BoatKinematics.FULL;
    }

    /**
     * Gets the one throttle click that takes a mode nearer full sail
     * @param modeIndex index of the mode
     * @return the index of the mode one click nearer full sail
     */
    private static int towardFull(int modeIndex)
    {
        switch (MODES[modeIndex])
        {
            case BoatKinematics.REVERSE:
                return modeIndex(BoatKinematics.STOPPED);
            case BoatKinematics.STOPPED:
                return modeIndex(BoatKinematics.SLOW);
            default:
                return modeIndex(BoatKinematics.FULL);
        }
    }

    private static <T> T requireNonNull(T value, String name)
    {
        if (value == null)
        {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        return value;
    }

    /**
     * What the hull may cross apart from any corridor
     */
    private final class Passage
    {
        private final SeaGrid grid;
        private final BoatHull hull;
        private final Set<WaterFamily> hazards;
        private final LongOpenHashSet avoidedKeys = new LongOpenHashSet();
        private final Long2IntOpenHashMap fitsByHeading = new Long2IntOpenHashMap(FIRST_TILES);
        private final int sureClearance;
        private final int inscribed;

        Passage(SeaGrid grid, BoatHull hull, Set<WaterFamily> hazards, Set<WorldPoint> avoidedTiles)
        {
            this.grid = grid;
            this.hull = hull;
            this.hazards = hazards;
            for (WorldPoint tile : avoidedTiles)
            {
                if (tile.getPlane() == 0)
                {
                    avoidedKeys.add(tileKey(tile.getX(), tile.getY()));
                }
            }
            this.sureClearance = hull.clearanceToFit();
            this.inscribed = Math.min(hull.halfAcross(), hull.halfAlong()) - rule.inset() - BoatHull.CELL;
        }

        /**
         * Gets what a water tile costs to cross apart from its clearance, impassable next to a hazard
         * @param x tile x
         * @param y tile y
         * @return the tile's cost
         */
        int waterCost(int x, int y)
        {
            boolean inBounds = bounds == null || bounds.contains(x, y);
            int cost = (avoidedKeys.contains(tileKey(x, y)) ? AVOIDED_TILE_COST : 0) + (inBounds ? 0 : OUT_OF_BOUNDS_COST) + (inBounds && !lane.isEmpty() && !lane.contains(tileKey(x, y)) ? OFF_LANE_COST : 0);
            if (hazards.isEmpty())
            {
                return cost;
            }
            int distance = hazardDistance(x, y);
            if (distance == 0)
            {
                return IMPASSABLE;
            }
            return distance > HAZARD_REACH ? cost : cost + (HAZARD_COST >> distance);
        }

        private int hazardDistance(int x, int y)
        {
            for (int ring = 0; ring <= HAZARD_REACH; ring++)
            {
                for (int dx = -ring; dx <= ring; dx++)
                {
                    for (int dy = -ring; dy <= ring; dy++)
                    {
                        if ((Math.abs(dx) == ring || Math.abs(dy) == ring) && hazards.contains(grid.family(x + dx, y + dy)))
                        {
                            return ring;
                        }
                    }
                }
            }
            return HAZARD_REACH + 1;
        }

        /**
         * Checks whether the hull fits at a tile's centre facing a compass point
         * @param tileX tile x
         * @param tileY tile y
         * @param heading compass point the hull faces
         * @return true if the hull fits
         */
        boolean fitsFacing(int tileX, int tileY, int heading)
        {
            if (grid.clearance(tileX, tileY) >= sureClearance)
            {
                return true;
            }
            long key = tileKey(tileX, tileY);
            int known = fitsByHeading.get(key);
            int asked = 1 << heading;
            if ((known & asked) == 0)
            {
                boolean fits = hull.fits(grid, (tileX << TILE_SHIFT) + HALF_TILE, (tileY << TILE_SHIFT) + HALF_TILE, Compass.bearingOf(heading), rule);
                known |= asked | (fits ? asked << Compass.POINTS : 0);
                fitsByHeading.put(key, known);
            }
            return (known & asked << Compass.POINTS) != 0;
        }

        /**
         * Checks whether a pivot cell has water at itself and a cell short of the hull's disc out along both axes
         * @param fineX fine x of the pivot
         * @param fineY fine y of the pivot
         * @return true if the hull might fit there
         */
        boolean mightFit(int fineX, int fineY)
        {
            return inscribed <= 0 || grid.waterAt(fineX, fineY) && grid.waterAt(fineX - inscribed, fineY) && grid.waterAt(fineX + inscribed, fineY) && grid.waterAt(fineX, fineY - inscribed) && grid.waterAt(fineX, fineY + inscribed);
        }
    }

    /**
     * A line for the corridor found over the whole sea when the web's leads through water the hull cannot pass
     */
    private final class SeaLine
    {
        private final SeaGrid grid = new SeaGrid(sea);
        private final Passage passage;
        private final Nodes nodes = new Nodes(FIRST_NODES);
        private final Heap open = Heap.whole();
        private final WorldPoint goal;

        SeaLine(BoatHull hull, Set<WaterFamily> hazards, Set<WorldPoint> avoidedTiles, WorldPoint goal)
        {
            this.passage = new Passage(grid, hull, hazards, avoidedTiles);
            this.goal = goal;
        }

        /**
         * Finds the line from a tile to within a tile of the goal, empty when the sea holds none or is too wide to search
         * @param from tile to start from
         * @return the line, or an empty list when none was found
         */
        List<WorldPoint> find(WorldPoint from)
        {
            int first = node(from.getX(), from.getY());
            nodes.costs[first] = 0;
            open.enqueue(lineEntry(remaining(from.getX(), from.getY()), remaining(from.getX(), from.getY()), first));
            while (!open.isEmpty() && nodes.count < LINE_MOST_NODES)
            {
                int node = (int) (open.dequeueLong() & LINE_NODE_MASK);
                if (nodes.closed[node])
                {
                    continue;
                }
                nodes.closed[node] = true;
                long key = nodes.keys[node];
                int x = (int) (key & COORDINATE_MASK);
                int y = (int) ((key >>> COORDINATE_BITS) & COORDINATE_MASK);
                if (Math.max(Math.abs(x - goal.getX()), Math.abs(y - goal.getY())) <= 1)
                {
                    return unwind(from, node);
                }
                int cost = nodes.costs[node];
                for (int dx = -1; dx <= 1; dx++)
                {
                    for (int dy = -1; dy <= 1; dy++)
                    {
                        int nextX = x + dx;
                        int nextY = y + dy;
                        int tileCost = (dx | dy) == 0 ? IMPASSABLE : tileCost(nextX, nextY);
                        if (tileCost == IMPASSABLE || !passage.fitsFacing(nextX, nextY, STEP_HEADINGS[dx + TICK_REACH][dy + TICK_REACH]))
                        {
                            continue;
                        }
                        int next = node(nextX, nextY);
                        if (nodes.closed[next])
                        {
                            continue;
                        }
                        int tentative = cost + (dx != 0 && dy != 0 ? LINE_DIAGONAL : LINE_STRAIGHT) * CLEARANCE_MULTIPLIER[grid.clearance(nextX, nextY)] + tileCost;
                        if (tentative >= nodes.costs[next])
                        {
                            continue;
                        }
                        nodes.costs[next] = tentative;
                        nodes.parents[next] = node;
                        int remaining = remaining(nextX, nextY);
                        open.enqueue(lineEntry(tentative + remaining, remaining, next));
                    }
                }
            }
            return Collections.emptyList();
        }

        private int tileCost(int x, int y)
        {
            int cost = grid.tileCost(x, y);
            if (cost == SeaGrid.UNKNOWN_COST)
            {
                cost = grid.isWater(x, y) ? passage.waterCost(x, y) : IMPASSABLE;
                grid.tileCost(x, y, cost);
            }
            return cost;
        }

        /**
         * Gets the least a way to the goal can still cost
         * @param x tile x
         * @param y tile y
         * @return the least remaining cost
         */
        private int remaining(int x, int y)
        {
            int dx = Math.abs(x - goal.getX());
            int dy = Math.abs(y - goal.getY());
            return LINE_STRAIGHT * Math.max(dx, dy) + (LINE_DIAGONAL - LINE_STRAIGHT) * Math.min(dx, dy);
        }

        private int node(int x, int y)
        {
            int node = grid.slot(x, y);
            if (node == SeaGrid.NO_SLOT)
            {
                node = nodes.add(tileKey(x, y));
                grid.slot(x, y, node);
            }
            return node;
        }

        /**
         * Gets the way's tiles from the start, every few along, the goal last, as the search's corridor is drawn
         * @param from tile the way starts at
         * @param last node the way ends at
         * @return the tiles of the line
         */
        private List<WorldPoint> unwind(WorldPoint from, int last)
        {
            List<WorldPoint> tiles = new ArrayList<>();
            for (int node = last; node != NO_NODE; node = nodes.parents[node])
            {
                long key = nodes.keys[node];
                tiles.add(new WorldPoint((int) (key & COORDINATE_MASK), (int) ((key >>> COORDINATE_BITS) & COORDINATE_MASK), 0));
            }
            Collections.reverse(tiles);
            List<WorldPoint> line = new ArrayList<>(tiles.size() / LINE_SPACING + 2);
            line.add(from);
            for (int at = LINE_SPACING; at < tiles.size(); at += LINE_SPACING)
            {
                line.add(tiles.get(at));
            }
            line.add(goal);
            return line;
        }

        private long lineEntry(long f, int remaining, int node)
        {
            return (f << LINE_HEURISTIC_BITS | remaining) << LINE_NODE_BITS | node;
        }
    }

    /**
     * A binary min-heap of longs ordered by their whole value or their high half, ties broken as fastutil's heap queue breaks them
     */
    private static final class Heap
    {
        private long[] entries;
        private final int shift;
        private int size;

        private Heap(int capacity, int shift)
        {
            this.entries = new long[capacity];
            this.shift = shift;
        }

        static Heap whole()
        {
            return new Heap(SeaPlanner.FIRST_NODES, 0);
        }

        static Heap byHighHalf(int capacity)
        {
            return new Heap(capacity, Integer.SIZE);
        }

        boolean isEmpty()
        {
            return size == 0;
        }

        int size()
        {
            return size;
        }

        void enqueue(long entry)
        {
            if (size == entries.length)
            {
                entries = Arrays.copyOf(entries, entries.length * 2);
            }
            int at = size++;
            long order = entry >>> shift;
            while (at != 0)
            {
                int parent = (at - 1) >>> 1;
                long above = entries[parent];
                if (above >>> shift <= order)
                {
                    break;
                }
                entries[at] = above;
                at = parent;
            }
            entries[at] = entry;
        }

        long dequeueLong()
        {
            long first = entries[0];
            long entry = entries[--size];
            entries[0] = entry;
            if (size != 0)
            {
                long order = entry >>> shift;
                int at = 0;
                int child;
                while ((child = (at << 1) + 1) < size)
                {
                    long below = entries[child];
                    int right = child + 1;
                    if (right < size && entries[right] >>> shift < below >>> shift)
                    {
                        below = entries[child = right];
                    }
                    if (order <= below >>> shift)
                    {
                        break;
                    }
                    entries[at] = below;
                    at = child;
                }
                entries[at] = entry;
            }
            return first;
        }
    }

    /**
     * The tiles within a width of a line, its segments filed by the squares of the sea they come near so a tile tests only those; every tile when the line is empty
     */
    private static final class Corridor
    {
        private static final int SQUARE_SHIFT = 5;

        private final int[] xs;
        private final int[] ys;
        private final int width;
        private final int firstSquareX;
        private final int firstSquareY;
        private final int squaresX;
        private final int squaresY;
        private final int[][] segments;

        Corridor(List<WorldPoint> line, int width)
        {
            this.width = width;
            this.xs = new int[line.size()];
            this.ys = new int[line.size()];
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (int i = 0; i < xs.length; i++)
            {
                xs[i] = line.get(i).getX();
                ys[i] = line.get(i).getY();
                minX = Math.min(minX, xs[i]);
                minY = Math.min(minY, ys[i]);
                maxX = Math.max(maxX, xs[i]);
                maxY = Math.max(maxY, ys[i]);
            }
            if (xs.length == 0)
            {
                this.firstSquareX = 0;
                this.firstSquareY = 0;
                this.squaresX = 0;
                this.squaresY = 0;
                this.segments = new int[0][];
                return;
            }

            int reach = width + 1;
            this.firstSquareX = (minX - reach) >> SQUARE_SHIFT;
            this.firstSquareY = (minY - reach) >> SQUARE_SHIFT;
            this.squaresX = ((maxX + reach) >> SQUARE_SHIFT) - firstSquareX + 1;
            this.squaresY = ((maxY + reach) >> SQUARE_SHIFT) - firstSquareY + 1;
            IntArrayList[] filed = new IntArrayList[squaresX * squaresY];
            for (int segment = 1; segment < xs.length; segment++)
            {
                int fromSquareX = (Math.min(xs[segment - 1], xs[segment]) - reach) >> SQUARE_SHIFT;
                int toSquareX = (Math.max(xs[segment - 1], xs[segment]) + reach) >> SQUARE_SHIFT;
                int fromSquareY = (Math.min(ys[segment - 1], ys[segment]) - reach) >> SQUARE_SHIFT;
                int toSquareY = (Math.max(ys[segment - 1], ys[segment]) + reach) >> SQUARE_SHIFT;
                for (int squareX = fromSquareX; squareX <= toSquareX; squareX++)
                {
                    for (int squareY = fromSquareY; squareY <= toSquareY; squareY++)
                    {
                        int square = (squareX - firstSquareX) * squaresY + squareY - firstSquareY;
                        if (filed[square] == null)
                        {
                            filed[square] = new IntArrayList();
                        }
                        filed[square].add(segment);
                    }
                }
            }

            this.segments = new int[filed.length][];
            for (int square = 0; square < filed.length; square++)
            {
                segments[square] = filed[square] == null ? IntArrays.EMPTY_ARRAY : filed[square].toIntArray();
            }
        }

        boolean contains(int x, int y)
        {
            if (xs.length == 0)
            {
                return true;
            }
            int squareX = (x >> SQUARE_SHIFT) - firstSquareX;
            int squareY = (y >> SQUARE_SHIFT) - firstSquareY;
            if (squareX < 0 || squareY < 0 || squareX >= squaresX || squareY >= squaresY)
            {
                return false;
            }
            for (int segment : segments[squareX * squaresY + squareY])
            {
                if (distance(x, y, segment) <= width)
                {
                    return true;
                }
            }
            return false;
        }

        private double distance(int x, int y, int segment)
        {
            double abx = xs[segment] - xs[segment - 1];
            double aby = ys[segment] - ys[segment - 1];
            double apx = x - xs[segment - 1];
            double apy = y - ys[segment - 1];
            double lengthSquared = abx * abx + aby * aby;
            double t = lengthSquared == 0 ? 0 : Math.max(0, Math.min(1, (apx * abx + apy * aby) / lengthSquared));
            return Math.hypot(apx - t * abx, apy - t * aby);
        }
    }

    /**
     * What a hull must not touch, keyed by tile, tested against the hull at a pose some ticks from the start
     */
    private static final class Barriers
    {
        private final BoatHull hull;
        private final LongOpenHashSet alwaysBarred;
        private final LongOpenHashSet[] barredByTick;
        private final LongOpenHashSet startCovered;
        private final int obstacleReach;

        Barriers(Obstacles obstacles, BoatHull hull, Collection<WorldPoint> startCovered)
        {
            this.hull = hull;
            this.alwaysBarred = keysOf(obstacles.always());
            this.barredByTick = new LongOpenHashSet[obstacles.byTick().size()];
            for (int tick = 0; tick < barredByTick.length; tick++)
            {
                barredByTick[tick] = keysOf(obstacles.byTick().get(tick));
            }
            this.startCovered = keysOf(startCovered);
            this.obstacleReach = (Math.max(hull.halfAcross(), hull.halfAlong()) + HALF_TILE + (1 << TILE_SHIFT) - 1) >> TILE_SHIFT;
        }

        private static LongOpenHashSet keysOf(Collection<WorldPoint> tiles)
        {
            LongOpenHashSet keys = new LongOpenHashSet();
            for (WorldPoint tile : tiles)
            {
                if (tile.getPlane() == 0)
                {
                    keys.add(tileKey(tile.getX(), tile.getY()));
                }
            }
            return keys;
        }

        /**
         * Checks whether the hull at a pose a number of ticks from the start would touch something in the way then
         * @param fineX fine x of the pivot
         * @param fineY fine y of the pivot
         * @param angle angle the hull lies at
         * @param tick ticks from the start
         * @return true if the hull touches an obstacle
         */
        private boolean struck(int fineX, int fineY, int angle, int tick)
        {
            LongOpenHashSet then = tick < barredByTick.length ? barredByTick[tick] : null;
            boolean none = then == null || then.isEmpty();
            if (none && alwaysBarred.isEmpty())
            {
                return false;
            }
            double radians = angle * (2 * Math.PI / Compass.TURN);
            double sin = Math.sin(radians);
            double cos = Math.cos(radians);
            int pivotX = fineX >> TILE_SHIFT;
            int pivotY = fineY >> TILE_SHIFT;
            for (int x = pivotX - obstacleReach; x <= pivotX + obstacleReach; x++)
            {
                for (int y = pivotY - obstacleReach; y <= pivotY + obstacleReach; y++)
                {
                    long key = tileKey(x, y);
                    if (startCovered.contains(key) || !alwaysBarred.contains(key) && (none || !then.contains(key)))
                    {
                        continue;
                    }
                    if (coversTile(x, y, fineX, fineY, sin, cos))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Checks whether any cell of the hull at a pose lies on a tile, cell by cell as the hull's own tiles are reckoned
         * @param tileX tile x
         * @param tileY tile y
         * @param fineX fine x of the pivot
         * @param fineY fine y of the pivot
         * @param sin sine of the hull's angle
         * @param cos cosine of the hull's angle
         * @return true if the hull covers the tile
         */
        private boolean coversTile(int tileX, int tileY, int fineX, int fineY, double sin, double cos)
        {
            for (int cellX = 0; cellX < CELLS_PER_TILE; cellX++)
            {
                for (int cellY = 0; cellY < CELLS_PER_TILE; cellY++)
                {
                    double rx = (tileX << TILE_SHIFT) + cellX * BoatHull.CELL + BoatHull.HALF_CELL - fineX;
                    double ry = (tileY << TILE_SHIFT) + cellY * BoatHull.CELL + BoatHull.HALF_CELL - fineY;
                    if (Math.abs(-rx * sin - ry * cos) <= hull.halfAlong() && Math.abs(rx * cos - ry * sin) <= hull.halfAcross())
                    {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * One plan's A*, with its tile costs and hull fits memoised
     */
    private final class Search
    {
        private final SeaGrid grid = new SeaGrid(sea);
        private final BoatHull hull;
        private final Passage passage;
        private final Corridor corridor;
        private final WorldPoint goal;
        private final List<Objective> objectives;
        private final int last;
        private final int[] objectiveFineX;
        private final int[] objectiveFineY;
        private final int[] objectiveSlack;
        private final long[] legsAfter;
        private final int bestsPerTile;
        private final Long2IntOpenHashMap index = new Long2IntOpenHashMap(FIRST_NODES);
        private final Int2IntOpenHashMap grounded = new Int2IntOpenHashMap();
        private final Int2IntOpenHashMap groundedRun = new Int2IntOpenHashMap();
        private int[] bests;
        private byte[] fitsKnown = new byte[FIRST_TILES * POSES_PER_TILE];
        private boolean[] flooded = new boolean[FIRST_TILES];
        private int slots;
        private final Nodes nodes = new Nodes(FIRST_NODES);
        private final Heap open = Heap.byHighHalf(FIRST_NODES);
        private final int holdTicks;
        private final Gust gust;
        private final Rapids rapids;
        private final int entering;
        private final boolean stop;
        private final int slackScale;
        private final boolean anytime;
        private final Obstacles obstacles;
        private Barriers barriers;
        private int firstNode = NO_NODE;
        private int bestNode = NO_NODE;
        private long bestLeft = Long.MAX_VALUE;
        private boolean exhausted;
        private boolean unreachable;
        private boolean pinned;
        private int startX;
        private int startY;
        private int polls;
        private int expansions;
        private int hullFits;
        private int pushes;
        private int peakOpen;
        private int tiles;
        private int cost;
        private int startHeuristic;
        private final double weight = fetid.isEmpty() ? WEIGHT : POOLED_WEIGHT;

        Search(BoatHull hull, Set<WaterFamily> hazards, Set<WorldPoint> avoidedTiles, List<WorldPoint> line, int corridor, List<Objective> objectives, boolean stop, int holdTicks, Gust gust, Rapids rapids, int entering, int slackScale, Obstacles obstacles, boolean anytime)
        {
            this.anytime = anytime;
            this.obstacles = obstacles;
            this.objectives = objectives;
            this.last = objectives.size() - 1;
            this.stop = stop;
            this.slackScale = slackScale;
            this.hull = hull;
            this.passage = new Passage(grid, hull, hazards, avoidedTiles);
            this.corridor = new Corridor(line, corridor);
            this.goal = objectives.get(last).tile();
            this.holdTicks = holdTicks;
            this.gust = gust;
            this.rapids = rapids;
            this.entering = entering;
            this.objectiveFineX = new int[objectives.size()];
            this.objectiveFineY = new int[objectives.size()];
            this.objectiveSlack = new int[objectives.size()];
            for (int objective = 0; objective <= last; objective++)
            {
                Objective target = objectives.get(objective);
                objectiveFineX[objective] = (target.tile().getX() << TILE_SHIFT) + HALF_TILE;
                objectiveFineY[objective] = (target.tile().getY() << TILE_SHIFT) + HALF_TILE;
                objectiveSlack[objective] = target.byHull() ? target.pivotReach(hull) : 0;
            }
            this.bestsPerTile = Compass.POINTS * speeds.length * objectives.size();
            this.bests = filledBests(FIRST_TILES * bestsPerTile);
            index.defaultReturnValue(NO_NODE);
            for (int objective = 0; objective <= last; objective++)
            {
                buildField(objective);
            }
            this.legsAfter = new long[objectives.size()];
            for (int objective = last - 1; objective >= 0; objective--)
            {
                legsAfter[objective] = legsAfter[objective + 1] + toObjective(objectiveFineX[objective], objectiveFineY[objective], objective + 1);
            }
        }

        /**
         * Gets the way to the state the search found nearest the objectives
         * @param start pose the boat starts from
         * @return the partial way, or null when the search found no state
         */
        Way partial(BoatPose start)
        {
            return bestNode == NO_NODE ? null : unwind(firstNode, bestNode, start);
        }

        SeaRoute.Stats stats(long millis)
        {
            return new SeaRoute.Stats(millis, polls, expansions, hullFits, pushes, peakOpen, tiles, cost, startHeuristic);
        }

        /**
         * Reckons back from the tiles the route may end on what every corridor tile pays to reach them, in thirds of the tick cost
         * @param objective index of the objective to build the field for
         */
        private void buildField(int objective)
        {
            WorldPoint target = objectives.get(objective).tile();
            int seeded = objectives.get(objective).surelyWithin(hull);
            Heap queue = Heap.byHighHalf(FIRST_NODES);
            for (int dx = -seeded; dx <= seeded; dx++)
            {
                for (int dy = -seeded; dy <= seeded; dy++)
                {
                    int x = target.getX() + dx;
                    int y = target.getY() + dy;
                    if (tileCost(x, y) != IMPASSABLE)
                    {
                        grid.field(objective, x, y, 0);
                        queue.enqueue(entry(0, (int) tileKey(x, y)));
                    }
                }
            }
            while (!queue.isEmpty())
            {
                long head = queue.dequeueLong();
                int cost = (int) (head >>> Integer.SIZE);
                int key = (int) head;
                int x = key & COORDINATE_MASK;
                int y = (key >>> COORDINATE_BITS) & COORDINATE_MASK;
                if (cost > grid.field(objective, x, y))
                {
                    continue;
                }
                for (int[] move : FIELD_MOVES)
                {
                    int nextX = x + move[0];
                    int nextY = y + move[1];
                    int tileCost = tileCost(nextX, nextY);
                    if (tileCost == IMPASSABLE)
                    {
                        continue;
                    }
                    int next = cost + move[2] * TICK_COST * CLEARANCE_MULTIPLIER[grid.clearance(nextX, nextY)] + FIELD_SCALE * (tileCost + (fetid.at(nextX, nextY) ? POOL_FIELD_COST : 0));
                    int known = grid.field(objective, nextX, nextY);
                    if (known == SeaGrid.UNKNOWN_COST || next < known)
                    {
                        grid.field(objective, nextX, nextY, next);
                        queue.enqueue(entry(next, (int) tileKey(nextX, nextY)));
                    }
                }
            }
        }

        Way run(BoatPose start, int speedIndex, int modeIndex, RapidsBoost rapidsBoost, BoatKinematics.Fouling startFouling)
        {
            startX = start.tileX();
            startY = start.tileY();
            int firstObjective = passing(0, start.fineX(), start.fineY(), start.fineX(), start.fineY(), start.angle());
            boolean startsWithinLast = firstObjective == last && objectives.get(last).passedBy(start, hull);
            long firstKey = stateKey(start.fineX(), start.fineY(), start.heading(), speedIndex, modeIndex, rapidsBoost.stacks() << Byte.SIZE | rapidsBoost.clock(), firstObjective, fouling(startFouling.moves(), startFouling.contact()), startsWithinLast);
            if (atGoal(firstKey))
            {
                return new Way(List.of(start), List.of(MODES[modeIndex]), List.of(speeds[speedIndex]));
            }
            barriers = new Barriers(obstacles, hull, hull.tiles(start));
            int startOffences = hull.offences(grid, start.fineX(), start.fineY(), start.angle(), rule);
            pinned = startOffences > 0;
            if (!goalReachable(false))
            {
                unreachable = true;
                if (!anytime)
                {
                    return null;
                }
            }
            int first = node(firstKey);
            this.firstNode = first;
            nodes.costs[first] = 0;
            if (startOffences > 0)
            {
                grounded.put(first, startOffences);
            }
            startHeuristic = (int) heuristic(start.fineX(), start.fineY(), firstObjective);
            open.enqueue(entry(startHeuristic, first));
            pushes++;
            peakOpen = 1;
            int iterations = 0;
            int most = anytime && speeds[speedIndex] != 0 ? MOST_AHEAD_ITERATIONS : MOST_ITERATIONS;
            while (!open.isEmpty() && iterations++ < most)
            {
                int node = (int) open.dequeueLong();
                polls++;
                if (polls == SURE_FLOOD_AFTER_POLLS && !anytime && !goalReachable(true))
                {
                    return null;
                }
                if (nodes.closed[node])
                {
                    continue;
                }
                nodes.closed[node] = true;
                long current = nodes.keys[node];
                if (atGoal(current))
                {
                    this.cost = nodes.costs[node];
                    return unwind(first, node, start);
                }
                if (anytime && node != first)
                {
                    long left = heuristic(stateFineX(current), stateFineY(current), stateObjective(current));
                    if (left < bestLeft)
                    {
                        bestLeft = left;
                        bestNode = node;
                    }
                }
                int cost = nodes.costs[node];
                int heading = stateHeading(current);
                int speed = stateSpeed(current);
                int mode = stateMode(current);
                boolean full = MODES[mode] == BoatKinematics.FULL;
                boolean boosted = gust.boosts(nodes.depths[node]);
                boolean pooled = fetid.holds(stateFineX(current), stateFineY(current), heading * Compass.STEP) && !gust.shields(nodes.depths[node]);
                int foulMoves = stateFoulMoves(current);
                boolean fouled = !pooled && foulMoves > 0;
                int fouling = fouling(BoatKinematics.Fouling.movesAfter(foulMoves, stateContact(current), pooled), pooled);
                int contactCost = pooled ? POOL_CONTACT_COST : 0;
                int stacks = stateStacks(current);
                int boost = boostAfter(node, current, stacks);
                boolean held = nodes.depths[node] < holdTicks && MODES[mode] != BoatKinematics.READY;
                boolean manoeuvring = !held && (node == first || nearStart(current) || nearGoal(current) || tight(current));
                boolean boxedIn = false;
                if (full || held || manoeuvring && MODES[mode] != BoatKinematics.READY)
                {
                    int heldSpeed = speedIndex(moved(speeds[speed], MODES[mode], boosted, stacks, pooled, fouled));
                    int turns = speeds[heldSpeed] > 0 && !held ? 1 : 0;
                    for (int turn = -turns; turn <= turns; turn++)
                    {
                        boxedIn |= !expand(node, current, cost, (heading + turn) & (Compass.POINTS - 1), heldSpeed, mode, boost, fouling, turnCost(node, current, turn) + contactCost);
                    }
                }
                if (grounded.get(node) > 0)
                {
                    holdOnTheGround(node, current, cost, heading, mode, boost, fouling);
                }
                for (int nextMode : held || full && !boxedIn && !manoeuvring ? IntArrays.EMPTY_ARRAY :manoeuvring || boxedIn ? REACHABLE[mode] : TOWARD_FULL_ONLY[mode])
                {
                    if (nextMode == mode)
                    {
                        continue;
                    }
                    int nextSpeed = speedIndex(moved(speeds[speed], MODES[nextMode], boosted, stacks, pooled, fouled));
                    int turns = speeds[nextSpeed] > 0 ? 1 : 0;
                    for (int turn = -turns; turn <= turns; turn++)
                    {
                        expand(node, current, cost, (heading + turn) & (Compass.POINTS - 1), nextSpeed, nextMode, boost, fouling, turnCost(node, current, turn) + CLICK_COST + contactCost);
                    }
                }
            }
            exhausted = open.isEmpty();
            return null;
        }

        /**
         * Checks whether any tile the route may end on can be reached
         * @param surely whether to try every pose on a tile rather than the step's heading
         * @return true if the goal can be reached
         */
        private boolean goalReachable(boolean surely)
        {
            if (surely)
            {
                Arrays.fill(flooded, false);
            }
            Heap queue = Heap.byHighHalf(FIRST_TILES);
            queue.enqueue(entry(goalDistance(startX, startY), (int) tileKey(startX, startY)));
            flooded[slot(startX, startY)] = true;
            while (!queue.isEmpty())
            {
                int key = (int) queue.dequeueLong();
                int x = key & COORDINATE_MASK;
                int y = (key >>> COORDINATE_BITS) & COORDINATE_MASK;
                if (Math.max(Math.abs(x - goal.getX()), Math.abs(y - goal.getY())) <= Math.max(1, (objectives.get(last).pivotReach(hull) >> TILE_SHIFT)))
                {
                    return true;
                }
                for (int dx = -TICK_REACH; dx <= TICK_REACH; dx++)
                {
                    for (int dy = -TICK_REACH; dy <= TICK_REACH; dy++)
                    {
                        int nextX = x + dx;
                        int nextY = y + dy;
                        if (tileCost(nextX, nextY) == IMPASSABLE)
                        {
                            continue;
                        }
                        int slot = slot(nextX, nextY);
                        if (flooded[slot])
                        {
                            continue;
                        }
                        flooded[slot] = true;
                        if (offThePinnedStart(nextX, nextY) || (surely ? anyPoseFits(slot, nextX, nextY) : passage.fitsFacing(nextX, nextY, STEP_HEADINGS[dx + TICK_REACH][dy + TICK_REACH])))
                        {
                            queue.enqueue(entry(goalDistance(nextX, nextY), (int) tileKey(nextX, nextY)));
                        }
                    }
                }
            }
            return false;
        }

        /**
         * Checks whether a tile lies within the hull's reach of a start the hull does not fit at, where the flood may pass as a pinned boat's moves off may
         * @param tileX tile x
         * @param tileY tile y
         * @return true if the tile is off the pinned start
         */
        private boolean offThePinnedStart(int tileX, int tileY)
        {
            int reach = (Math.max(hull.halfAcross(), hull.halfAlong()) + (1 << TILE_SHIFT) - 1) >> TILE_SHIFT;
            return pinned && Math.max(Math.abs(tileX - startX), Math.abs(tileY - startY)) <= reach;
        }

        /**
         * Checks whether the hull fits at some pose with its pivot on a tile, tried cell by cell where it might
         * @param slot slot of the tile's tables
         * @param tileX tile x
         * @param tileY tile y
         * @return true if some pose fits
         */
        private boolean anyPoseFits(int slot, int tileX, int tileY)
        {
            if (grid.clearance(tileX, tileY) >= passage.sureClearance)
            {
                return true;
            }
            for (int cell = 0; cell < CELLS_PER_TILE * CELLS_PER_TILE; cell++)
            {
                int fineX = (tileX << TILE_SHIFT) + (cell & (CELLS_PER_TILE - 1)) * (1 << GRID_SHIFT);
                int fineY = (tileY << TILE_SHIFT) + (cell >> (TILE_SHIFT - GRID_SHIFT)) * (1 << GRID_SHIFT);
                if (!passage.mightFit(fineX, fineY))
                {
                    continue;
                }
                for (int heading = 0; heading < Compass.POINTS; heading++)
                {
                    if (fits(slot, fineX, fineY, heading, Compass.bearingOf(heading)))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        private int goalDistance(int tileX, int tileY)
        {
            return Math.max(Math.abs(tileX - goal.getX()), Math.abs(tileY - goal.getY()));
        }

        /**
         * Gets the slot of a tile's own tables, given when none is yet
         * @param tileX tile x
         * @param tileY tile y
         * @return the tile's slot
         */
        private int slot(int tileX, int tileY)
        {
            int slot = grid.slot(tileX, tileY);
            if (slot == SeaGrid.NO_SLOT)
            {
                slot = slots++;
                grid.slot(tileX, tileY, slot);
                if (slot * bestsPerTile >= bests.length)
                {
                    int[] grown = filledBests(bests.length * 2);
                    System.arraycopy(bests, 0, grown, 0, bests.length);
                    bests = grown;
                    fitsKnown = Arrays.copyOf(fitsKnown, fitsKnown.length * 2);
                    flooded = Arrays.copyOf(flooded, flooded.length * 2);
                }
            }
            return slot;
        }

        /**
         * Gets the node for a state, made when none is yet
         * @param key packed state
         * @return the state's node
         */
        private int node(long key)
        {
            int node = index.get(key);
            if (node == NO_NODE)
            {
                node = nodes.add(key);
                index.put(key, node);
            }
            return node;
        }

        private long entry(long f, int node)
        {
            return f << Integer.SIZE | node;
        }

        /**
         * Tries one tick out of a state and files the state it reaches when it is the cheapest way there yet
         * @param from node of the state
         * @param current packed state
         * @param cost cost of the state
         * @param heading heading of the move
         * @param speedIndex index of the move's speed
         * @param modeIndex index of the move's throttle mode
         * @param boost rapids boost of the move
         * @param fouling packed fouling of the move
         * @param actionCost cost of the turn and clicks taken
         * @return false if the move is blocked, true otherwise
         */
        private boolean expand(int from, long current, int cost, int heading, int speedIndex, int modeIndex, int boost, int fouling, int actionCost)
        {
            expansions++;
            int speed = speeds[speedIndex];
            int angle = Compass.bearingOf(heading);
            int fineX = stateFineX(current) + advanceXs[speedIndex][heading];
            int fineY = stateFineY(current) + advanceYs[speedIndex][heading];
            int before = Compass.bearingOf(stateHeading(current));
            int objective = passing(stateObjective(current), stateFineX(current), stateFineY(current), fineX, fineY, before);
            boolean withinLast = objective == last && objectives.get(last).passedBetween(stateFineX(current), stateFineY(current), fineX, fineY, before, hull);
            long key = stateKey(fineX, fineY, heading, speedIndex, modeIndex, boost, objective, fouling, withinLast);
            if (key == current)
            {
                return true;
            }
            int tileX = fineX >> TILE_SHIFT;
            int tileY = fineY >> TILE_SHIFT;
            int tileCost = tileCost(tileX, tileY);
            if (tileCost == IMPASSABLE)
            {
                int closed = index.get(key);
                return closed != NO_NODE && nodes.closed[closed];
            }
            int perTick = TICK_COST * CLEARANCE_MULTIPLIER[grid.clearance(tileX, tileY)] + (speed < 0 ? ASTERN_COST : speed == 0 ? STOPPED_COST : 0) + (racing && MODES[modeIndex] != BoatKinematics.FULL ? RACING_COST : 0);
            int tentative = cost + perTick + tileCost + actionCost;
            int slot = slot(tileX, tileY);
            int bestIndex = slot * bestsPerTile + (objective * Compass.POINTS + heading) * speeds.length + speedIndex;
            int best = bests[bestIndex];
            if (best != Integer.MAX_VALUE && tentative > best + slackTicks[speedIndex] * perTick * slackScale)
            {
                return true;
            }
            int known = index.get(key);
            if (known != NO_NODE && (nodes.closed[known] || tentative >= nodes.costs[known]))
            {
                return true;
            }
            if (barriers.struck(fineX, fineY, angle, nodes.depths[from] + 1))
            {
                return false;
            }
            int offences = 0;
            if (!fits(slot, fineX, fineY, heading, angle))
            {
                offences = offencesOffTheGround(from, fineX, fineY, angle, modeIndex);
                if (offences < 0)
                {
                    return false;
                }
                tentative += offences * GROUNDED_CELL_COST;
                if (known != NO_NODE && tentative >= nodes.costs[known])
                {
                    return true;
                }
            }
            int node = known == NO_NODE ? node(key) : known;
            if (offences > 0)
            {
                grounded.put(node, offences);
                groundedRun.put(node, offences == grounded.get(from) ? groundedRun.get(from) + 1 : 0);
            }
            nodes.costs[node] = tentative;
            nodes.parents[node] = from;
            nodes.depths[node] = nodes.depths[from] + 1;
            if (tentative < best)
            {
                bests[bestIndex] = tentative;
            }
            open.enqueue(entry((long) (tentative + weight * heuristic(fineX, fineY, objective)), node));
            pushes++;
            peakOpen = Math.max(peakOpen, open.size());
            return true;
        }

        /**
         * Tries the moves of a boat held on the ground under sail
         * @param node node of the state
         * @param current packed state
         * @param cost cost of the state
         * @param heading heading of the state
         * @param mode index of the state's throttle mode
         * @param boost rapids boost of the moves
         * @param fouling packed fouling of the moves
         */
        private void holdOnTheGround(int node, long current, int cost, int heading, int mode, int boost, int fouling)
        {
            for (int nextMode : REACHABLE[mode])
            {
                if (!sailsSet(nextMode))
                {
                    continue;
                }
                for (int turn = -1; turn <= 1; turn++)
                {
                    if (turn != 0 || nextMode != mode)
                    {
                        expand(node, current, cost, (heading + turn) & (Compass.POINTS - 1), speedIndex(0), nextMode, boost, fouling, turnCost(node, current, turn) + (nextMode == mode ? 0 : CLICK_COST));
                    }
                }
            }
        }

        /**
         * Counts how many hull cells lie in the way at a pose that does not fit, where the move to it takes a boat lying against something further off it than the pose it left
         * @param from node of the state moved from
         * @param fineX fine x of the pivot
         * @param fineY fine y of the pivot
         * @param angle angle the hull lies at
         * @param modeIndex index of the move's throttle mode
         * @return the cells in the way, or -1 when the move is not allowed
         */
        private int offencesOffTheGround(int from, int fineX, int fineY, int angle, int modeIndex)
        {
            int before = grounded.get(from);
            if (before == 0)
            {
                return -1;
            }
            int after = hull.offences(grid, fineX, fineY, angle, rule);
            long left = nodes.keys[from];
            boolean stayed = stateFineX(left) == fineX && stateFineY(left) == fineY;
            boolean inPlace = stayed && Compass.bearingOf(stateHeading(left)) == angle;
            if (after < before || inPlace && after == before || stayed && sailsSet(modeIndex))
            {
                return after;
            }
            if (after == before && groundedRun.get(from) < MOST_GROUNDED_RUN && new HashSet<>(hull.offendingCells(grid, stateFineX(left), stateFineY(left), Compass.bearingOf(stateHeading(left)), rule)).containsAll(hull.offendingCells(grid, fineX, fineY, angle, rule)))
            {
                return after;
            }
            return -1;
        }

        /**
         * Checks whether the state lies near the objective it is sailing for, where crawling and backing are worth their cost
         * @param state packed state
         * @return true if the state is near its objective
         */
        private boolean nearGoal(long state)
        {
            WorldPoint target = objectives.get(stateObjective(state)).tile();
            return Math.max(Math.abs((stateFineX(state) >> TILE_SHIFT) - target.getX()), Math.abs((stateFineY(state) >> TILE_SHIFT) - target.getY())) <= FINISH_WITHIN;
        }

        /**
         * Finds the objective a pose reached from a point sails for next
         * @param objective objective sailed for before the move
         * @param fromX fine x the pivot left
         * @param fromY fine y the pivot left
         * @param fineX fine x of the pivot now
         * @param fineY fine y of the pivot now
         * @param angle angle the hull lay at
         * @return the index of the objective sailed for next
         */
        private int passing(int objective, int fromX, int fromY, int fineX, int fineY, int angle)
        {
            int next = objective;
            while (next < last && objectives.get(next).passedBetween(fromX, fromY, fineX, fineY, angle, hull))
            {
                next++;
            }
            return next;
        }

        private boolean nearStart(long state)
        {
            return Math.max(Math.abs((stateFineX(state) >> TILE_SHIFT) - startX), Math.abs((stateFineY(state) >> TILE_SHIFT) - startY)) <= FINISH_WITHIN;
        }

        /**
         * Gets what a move's turn costs
         * @param node node of the state
         * @param current packed state
         * @param turn compass points turned
         * @return the cost of the turn
         */
        private int turnCost(int node, long current, int turn)
        {
            if (turn == 0)
            {
                return 0;
            }
            int parent = nodes.parents[node];
            int turned = parent == NO_NODE ? 0 : Math.floorMod(stateHeading(current) - stateHeading(nodes.keys[parent]) + Compass.POINTS / 2, Compass.POINTS) - Compass.POINTS / 2;
            return Math.abs(turn) * TURN_COST + (turned == turn ? 0 : HELM_CLICK_COST);
        }

        /**
         * Gets the speed of the move out of a pose
         * @param speed speed the boat is moving at
         * @param mode throttle mode of the move
         * @param boosted whether a gust boosts the move
         * @param stacks rapids stacks the boat holds
         * @param pooled whether the fetid pools hold the boat
         * @param fouled whether the pools have left fouling on the boat
         * @return the speed of the move
         */
        private int moved(int speed, int mode, boolean boosted, int stacks, boolean pooled, boolean fouled)
        {
            int ramped = BoatKinematics.rampedSpeed(speed, mode, profile, boosted, stacks);
            if (pooled)
            {
                return BoatKinematics.pooledSpeed(speed, ramped, boosted, profile);
            }
            return fouled ? BoatKinematics.fouledSpeed(speed, ramped, profile) : ramped;
        }

        /**
         * Gets the rapids boost the moves out of a state's children run on
         * @param node node of the state
         * @param current packed state
         * @param stacks rapids stacks the state holds
         * @return the packed rapids boost
         */
        private int boostAfter(int node, long current, int stacks)
        {
            if (rapids.isEmpty() && stacks == 0)
            {
                return 0;
            }
            int here = rapids.at(stateFineX(current) >> TILE_SHIFT, stateFineY(current) >> TILE_SHIFT);
            int parent = nodes.parents[node];
            int entered = parent == NO_NODE ? entering : Rapids.entered(here, rapids.at(stateFineX(nodes.keys[parent]) >> TILE_SHIFT, stateFineY(nodes.keys[parent]) >> TILE_SHIFT));
            return RapidsBoost.next(stacks, stateClock(current), entered);
        }

        /**
         * Checks whether the state's tile has an obstacle within two tiles, where crawling and backing are worth their cost
         * @param state packed state
         * @return true if the state is in tight water
         */
        private boolean tight(long state)
        {
            return grid.clearance(stateFineX(state) >> TILE_SHIFT, stateFineY(state) >> TILE_SHIFT) <= TIGHT_CLEARANCE;
        }

        /**
         * Checks whether the hull fits at a pose
         * @param slot slot of the tile's tables
         * @param fineX fine x of the pivot
         * @param fineY fine y of the pivot
         * @param heading compass point the hull faces
         * @param angle angle the hull lies at
         * @return true if the hull fits
         */
        private boolean fits(int slot, int fineX, int fineY, int heading, int angle)
        {
            if (grid.clearance(fineX >> TILE_SHIFT, fineY >> TILE_SHIFT) >= passage.sureClearance)
            {
                return true;
            }
            int cell = ((fineX >> GRID_SHIFT) & 3) | ((fineY >> GRID_SHIFT) & 3) << 2;
            int known = slot * POSES_PER_TILE + (cell << HEADING_BITS | heading);
            if (fitsKnown[known] == FIT_UNKNOWN)
            {
                hullFits++;
                fitsKnown[known] = hull.fits(grid, fineX, fineY, angle, rule) ? FIT_YES : FIT_NO;
            }
            return fitsKnown[known] == FIT_YES;
        }

        private boolean atGoal(long state)
        {
            return stateObjective(state) == last && stateWithinLast(state) && (!stop || speeds[stateSpeed(state)] == 0);
        }

        /**
         * Gets the least the boat must still pay
         * @param fineX fine x of the pivot
         * @param fineY fine y of the pivot
         * @param objective index of the objective sailed for
         * @return the least remaining cost
         */
        private long heuristic(int fineX, int fineY, int objective)
        {
            return toObjective(fineX, fineY, objective) + legsAfter[objective];
        }

        /**
         * Gets the least the boat must pay from a point to an objective, as the heuristic reckons it to the goal
         * @param fineX fine x of the pivot
         * @param fineY fine y of the pivot
         * @param objective index of the objective
         * @return the least cost to the objective
         */
        private long toObjective(int fineX, int fineY, int objective)
        {
            int dx = Math.max(0, Math.abs(fineX - objectiveFineX[objective]) - objectiveSlack[objective]);
            int dy = Math.max(0, Math.abs(fineY - objectiveFineY[objective]) - objectiveSlack[objective]);
            int ticks = Math.max((Math.max(dx, dy) + profile.fullSpeed() - 1) / profile.fullSpeed(), (dx + dy + mostAxesPerTick - 1) / mostAxesPerTick);
            int field = grid.field(objective, fineX >> TILE_SHIFT, fineY >> TILE_SHIFT);
            return Math.max((long) ticks * TICK_COST, field == SeaGrid.UNKNOWN_COST ? 0 : field / FIELD_SCALE);
        }

        private int tileCost(int x, int y)
        {
            int cost = grid.tileCost(x, y);
            if (cost == SeaGrid.UNKNOWN_COST)
            {
                tiles++;
                cost = computeTileCost(x, y);
                grid.tileCost(x, y, cost);
            }
            return cost;
        }

        private int computeTileCost(int x, int y)
        {
            return grid.isWater(x, y) && corridor.contains(x, y) ? passage.waterCost(x, y) : IMPASSABLE;
        }

        private Way unwind(int first, int last, BoatPose start)
        {
            List<BoatPose> poses = new ArrayList<>();
            List<Integer> throttles = new ArrayList<>();
            List<Integer> sailed = new ArrayList<>();
            for (int node = last; node != NO_NODE; node = nodes.parents[node])
            {
                long key = nodes.keys[node];
                poses.add(node == first ? start : new BoatPose(stateFineX(key), stateFineY(key), Compass.bearingOf(stateHeading(key))));
                throttles.add(MODES[stateMode(key)]);
                sailed.add(speeds[stateSpeed(key)]);
            }
            Collections.reverse(poses);
            Collections.reverse(throttles);
            Collections.reverse(sailed);
            return new Way(poses, throttles, sailed);
        }
    }

    private static int[] filledBests(int size)
    {
        int[] bests = new int[size];
        Arrays.fill(bests, Integer.MAX_VALUE);
        return bests;
    }

    /**
     * The states a search has met, by node number
     */
    private static final class Nodes
    {
        private long[] keys;
        private int[] costs;
        private int[] parents;
        private int[] depths;
        private boolean[] closed;
        private int count;

        Nodes(int capacity)
        {
            keys = new long[capacity];
            costs = new int[capacity];
            parents = new int[capacity];
            depths = new int[capacity];
            closed = new boolean[capacity];
        }

        int add(long key)
        {
            if (count == keys.length)
            {
                int capacity = keys.length * 2;
                keys = Arrays.copyOf(keys, capacity);
                costs = Arrays.copyOf(costs, capacity);
                parents = Arrays.copyOf(parents, capacity);
                depths = Arrays.copyOf(depths, capacity);
                closed = Arrays.copyOf(closed, capacity);
            }
            keys[count] = key;
            costs[count] = Integer.MAX_VALUE;
            parents[count] = NO_NODE;
            depths[count] = 0;
            return count++;
        }
    }

    /**
     * A way found: its poses a tick apart
     */
    private static final class Way
    {
        private final List<BoatPose> poses;
        private final List<Integer> throttles;
        private final List<Integer> speeds;

        Way(List<BoatPose> poses, List<Integer> throttles, List<Integer> speeds)
        {
            this.poses = poses;
            this.throttles = throttles;
            this.speeds = speeds;
        }

        public List<BoatPose> poses()
        {
            return poses;
        }

        public List<Integer> throttles()
        {
            return throttles;
        }

        public List<Integer> speeds()
        {
            return speeds;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Way))
            {
                return false;
            }
            Way other = (Way) o;
            return Objects.equals(poses, other.poses) && Objects.equals(throttles, other.throttles) && Objects.equals(speeds, other.speeds);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(poses, throttles, speeds);
        }

        @Override
        public String toString()
        {
            return "Way[" + "poses=" + poses + ", " + "throttles=" + throttles + ", " + "speeds=" + speeds + "]";
        }
    }
}

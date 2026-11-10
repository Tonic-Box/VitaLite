package com.tonic.services.pathfinder.sailing;

import com.tonic.services.pathfinder.sailing.sea.BoatHull;
import com.tonic.services.pathfinder.sailing.sea.BoatKinematics;
import com.tonic.services.pathfinder.sailing.sea.BoatPose;
import com.tonic.services.pathfinder.sailing.sea.BoatProfile;
import com.tonic.services.pathfinder.sailing.sea.Compass;
import com.tonic.services.pathfinder.sailing.sea.FetidWaters;
import com.tonic.services.pathfinder.sailing.sea.HullRule;
import com.tonic.services.pathfinder.sailing.sea.Rapids;
import com.tonic.services.pathfinder.sailing.sea.RapidsBoost;
import com.tonic.services.pathfinder.sailing.sea.SeaArea;
import com.tonic.services.pathfinder.sailing.sea.SeaPlanner;
import com.tonic.services.pathfinder.sailing.sea.SeaRoute;
import com.tonic.services.pathfinder.sailing.sea.WaterFamily;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.IntFunction;

/**
 * Sails a boat through objectives on a race or a voyage, holding a route while the boat is on it and planning again in the tick it is not
 */
final class SeaPilot
{
    private static final int HORIZON = 16;
    /**
     * How many moves a trim keeps the fetid pools off the boat, its own tick's included
     */
    private static final int IMMUNE_TICKS = 6;
    /**
     * How many ticks a gust stays on offer untrimmed
     */
    private static final int OFFER_TICKS = 5;
    /**
     * The ticks short of the offer's assumed end a trim waited for is made by
     */
    private static final int TRIM_MARGIN = 1;
    private static final int NOT_OFFERED = Integer.MIN_VALUE;
    private static final int MOST_OBJECTIVES = 4;
    private static final long LATE_NANOS = 400_000_000L;
    private static final int GUST_MOVES = 20;
    private static final int OFF_ROUTE = -1;
    private static final int FINE_TILE = 128;
    private static final int STOP = 0;
    private static final int SLOWER = 1;
    private static final int FASTER = 2;
    private static final int[] LADDER = {BoatKinematics.REVERSE, BoatKinematics.STOPPED, BoatKinematics.SLOW, BoatKinematics.FULL};

    private final SeaWorld world;
    private final Sailing sailing;
    private final Helm helm;
    private final Executor planning;
    private final CloudStrikes clouds = new CloudStrikes();
    private final SeaHazards hazards;
    private List<SeaPlanner.Objective> objectives = List.of();
    private boolean stopping;
    private SeaArea bounds;
    private Set<WorldPoint> lane = Set.of();
    private int stopClickedTick = Integer.MIN_VALUE / 2;
    private int offeredSince = NOT_OFFERED;
    private SeaRoute route;
    private int routeTick;
    private boolean complete;
    private List<SeaPlanner.Objective> plannedFor = List.of();
    private boolean plannedStopping;
    private Set<WorldPoint> avoidedWhenPlanned = Set.of();
    private FetidWaters poolsWhenPlanned = FetidWaters.NONE;
    private CompletableFuture<Choice> pending;
    private int pendingTick;
    private BoatPose pendingStillAt;
    private int pendingMode;
    private List<SeaPlanner.Objective> pendingFor = List.of();
    private boolean pendingStopping;
    private long tickStarted;
    private int movedTick = Integer.MIN_VALUE;
    private int headingSet = BoatKinematics.NO_HEADING;
    private int trimmedTick = Integer.MIN_VALUE;
    private RapidsBoost rapidsBoost = RapidsBoost.NONE;
    private int rapidsHere;
    private int rapidsEntered;
    private BoatKinematics.Fouling fouling = BoatKinematics.Fouling.NONE;
    private boolean pooledLastTick;
    private int plans;
    private int shortPlans;
    private int offPoses;
    private int movesNotFound;
    private boolean pointByPoint;
    private BoatPose tickPose;
    private int tickPoseTick = Integer.MIN_VALUE;
    private String dangersNoted = "";
    private String noted;

    /**
     * What a tick's planning chose
     */
    private static final class Choice
    {
        private final SeaRoute route;
        private final int from;
        private final boolean held;
        private final boolean trim;
        private final Set<WorldPoint> avoided;
        private final FetidWaters pools;

        Choice(SeaRoute route, int from, boolean held, boolean trim, Set<WorldPoint> avoided, FetidWaters pools)
        {
            this.route = route;
            this.from = from;
            this.held = held;
            this.trim = trim;
            this.avoided = avoided;
            this.pools = pools;
        }

        public SeaRoute route()
        {
            return route;
        }

        public int from()
        {
            return from;
        }

        public boolean held()
        {
            return held;
        }

        public boolean trim()
        {
            return trim;
        }

        public Set<WorldPoint> avoided()
        {
            return avoided;
        }

        public FetidWaters pools()
        {
            return pools;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Choice))
            {
                return false;
            }
            Choice other = (Choice) o;
            return Objects.equals(route, other.route) && from == other.from && held == other.held && trim == other.trim && Objects.equals(avoided, other.avoided) && Objects.equals(pools, other.pools);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(route, from, held, trim, avoided, pools);
        }

        @Override
        public String toString()
        {
            return "Choice[" + "route=" + route + ", " + "from=" + from + ", " + "held=" + held + ", " + "trim=" + trim + ", " + "avoided=" + avoided + ", " + "pools=" + pools + "]";
        }
    }

    SeaPilot(SeaWorld world, Sailing sailing, Helm helm, Executor planning)
    {
        this.world = world;
        this.sailing = sailing;
        this.helm = helm;
        this.planning = planning;
        this.hazards = new SeaHazards(world, clouds, HORIZON);
    }

    /**
     * Sails on through objectives from the next tick
     * @param through the objectives to pass, one to four
     * @param stop whether to stop at the last objective
     * @param within the trial's bounds, or null off a trial
     */
    public void sail(List<SeaPlanner.Objective> through, boolean stop, SeaArea within)
    {
        sail(through, stop, within, Set.of());
    }

    /**
     * Sails on through objectives from the next tick, drawn along a lane where the route may wander from the objectives, as a trial's pathfinding hints mark it
     * @param through the objectives to pass, one to four
     * @param stop whether to stop at the last objective
     * @param within the trial's bounds, or null off a trial
     * @param along the lane tiles the route may wander along
     */
    public void sail(List<SeaPlanner.Objective> through, boolean stop, SeaArea within, Set<WorldPoint> along)
    {
        if (along == null)
        {
            throw new IllegalArgumentException("along cannot be null");
        }
        if (through == null || through.isEmpty() || through.size() > MOST_OBJECTIVES)
        {
            throw new IllegalArgumentException("one to four objectives are needed");
        }
        if (objectives.isEmpty())
        {
            headingSet = BoatKinematics.NO_HEADING;
            clouds.clear();
            pointByPoint = false;
            movesNotFound = 0;
        }
        objectives = List.copyOf(through);
        stopping = stop;
        bounds = within;
        lane = along;
    }

    /**
     * Notes a burst of speed that keeps the fetid pools off the boat from this tick, as a wind mote released is taken to do as a trim does (the NathanVegetable plugin keeps the pools off for seven ticks on the mote's message)
     */
    public void shieldedFromNow()
    {
        trimmedTick = world.tick();
    }

    /**
     * Lets go of the route held, so the tick's cycle plans again from where the boat lies
     */
    public void planAgain()
    {
        route = null;
    }

    /**
     * Checks whether the pilot clicked stop this tick or the last, before the throttle reads it
     * @return true if stop was clicked this tick or the last
     */
    public boolean stoppedLately()
    {
        return world.tick() - stopClickedTick <= 1;
    }

    /**
     * Stops piloting
     */
    public void stop()
    {
        objectives = List.of();
        route = null;
        pending = null;
    }

    /**
     * Checks whether a run is being sailed
     * @return true while a run is being sailed
     */
    public boolean busy()
    {
        return !objectives.isEmpty();
    }

    /**
     * Gets the route the boat sails now
     * @return the route, or null when none is held
     */
    public SeaRoute route()
    {
        return route;
    }

    /**
     * Counts the routes this pilot has planned and taken up
     * @return the number of routes taken up
     */
    public int plans()
    {
        return plans;
    }

    /**
     * Counts the routes that came short of the last objective
     * @return the number of short routes
     */
    public int shortPlans()
    {
        return shortPlans;
    }

    /**
     * Counts the times the boat was found off the pose its route held for the tick
     * @return the number of times off the pose
     */
    public int offPoses()
    {
        return offPoses;
    }

    /**
     * Counts the plans in a row that found no move from where the boat lay
     * @return the number of plans in a row
     */
    public int movesNotFound()
    {
        return movesNotFound;
    }

    /**
     * Gets the index of the route's pose for this tick
     * @return the pose index, or -1 with no route
     */
    int routeIndex()
    {
        return route == null ? -1 : world.tick() - routeTick;
    }

    /**
     * Gets the tile the route takes the boat to next
     * @return the next tile, or null with no route
     */
    public WorldPoint lookahead()
    {
        if (route == null)
        {
            return null;
        }
        return route.poses().get(Math.max(0, Math.min(route.size() - 1, world.tick() - routeTick + 1))).tile();
    }

    /**
     * Checks whether the boat was seen to turn faster than a point a tick, so it is steered a point at a time
     * @return true if the boat is steered a point at a time
     */
    public boolean steersPointByPoint()
    {
        return pointByPoint;
    }

    /**
     * Runs one cycle: a new tick keeps the route held or starts the tick's plan
     * @param newTick whether the cycle starts a new tick
     */
    void cycle(boolean newTick)
    {
        SeaWorld.Boat boat = world.boat();
        if (boat == null)
        {
            return;
        }
        BoatPose pose = boat.pose();
        if (newTick)
        {
            watch(pose);
        }
        if (objectives.isEmpty() || !sailing.atHelm())
        {
            return;
        }
        if (newTick)
        {
            tickStarted = System.nanoTime();
            BoatHull hull = boat.hull();
            Set<WorldPoint> avoidedTiles = avoidedTiles(pose, hull);
            SeaPlanner.Obstacles obstacles = hazards.now();
            int at = heldAt(pose, hull, avoidedTiles, obstacles);
            boolean offered = gustNow().moves() == 0 && helm.canTrim();
            offeredSince = !offered ? NOT_OFFERED : offeredSince == NOT_OFFERED ? world.tick() : offeredSince;
            if (at != OFF_ROUTE && !offered)
            {
                move(route, at, pose);
                return;
            }
            if (!waitingStill(pose))
            {
                pending = plan(pose, hull, avoidedTiles, obstacles, at);
                pendingTick = world.tick();
                pendingStillAt = sailing.speed() == 0 ? pose : null;
                pendingMode = sailing.throttle();
                pendingFor = objectives;
                pendingStopping = stopping;
            }
        }
        if (movedTick == world.tick())
        {
            return;
        }
        if (pending != null && pending.isDone() && (pendingTick == world.tick() || waitingStill(pose)))
        {
            Choice choice = pending.join();
            pending = null;
            take(choice, pose);
            return;
        }
        if (System.nanoTime() - tickStarted > LATE_NANOS && route != null)
        {
            int at = world.tick() - routeTick;
            if (at >= 0 && at + 1 < route.size() && route.poses().get(at).equals(pose))
            {
                move(route, at, pose);
            }
        }
    }

    /**
     * Checks whether the plan under way was made for a boat lying still where this one still lies, as it was set and bound, so it holds however late it comes
     * @param pose the boat's pose now
     * @return true if the plan under way still holds
     */
    private boolean waitingStill(BoatPose pose)
    {
        return pending != null && pose.equals(pendingStillAt) && sailing.speed() == 0 && sailing.throttle() == pendingMode && objectives.equals(pendingFor) && stopping == pendingStopping;
    }

    /**
     * Watches the boat once a tick while aboard, sailing or not, so a voyage set off under way knows what it carries
     * @param pose the boat's pose this tick
     */
    private void watch(BoatPose pose)
    {
        clouds.watch(world, world.tick());
        int here = sailing.rapids().rapids().at(pose.tileX(), pose.tileY());
        rapidsBoost = rapidsBoost.after(rapidsEntered);
        rapidsEntered = Rapids.entered(here, rapidsHere);
        rapidsHere = here;
        watchPools(pose);
        watchTurnRate(pose);
    }

    /**
     * Carries what the fetid pools left on the boat from the last tick's pose to this one, as the planner carries it from pose to pose
     * @param pose the boat's pose this tick
     */
    private void watchPools(BoatPose pose)
    {
        boolean aTickApart = tickPose != null && tickPoseTick == world.tick() - 1;
        fouling = aTickApart ? fouling.next(pooledLastTick) : BoatKinematics.Fouling.NONE;
        pooledLastTick = sailing.fetidPools().holds(pose.fineX(), pose.fineY(), pose.angle()) && immunityLeft() == 0;
    }

    /**
     * Falls back to a click a point when the boat turns more than a point from one tick to the next, as no boat is yet known to
     * @param pose the boat's pose this tick
     */
    private void watchTurnRate(BoatPose pose)
    {
        boolean aTickApart = tickPose != null && tickPoseTick == world.tick() - 1;
        if (!pointByPoint && busy() && aTickApart && Math.abs(Compass.shortestArc(tickPose.angle(), pose.angle())) > BoatKinematics.TURN)
        {
            pointByPoint = true;
            world.note("[sailing] the " + sailing.stats().type() + " turned " + Compass.shortestArc(tickPose.angle(), pose.angle()) + " units in a tick, from " + tickPose + " to " + pose + "; steering a point at a time this voyage");
        }
        tickPose = pose;
        tickPoseTick = world.tick();
    }

    /**
     * Gets the tiles a plan keeps the pivot off where it can
     * @param pose the boat's pose
     * @param hull the boat's hull
     * @return the avoided tiles
     */
    private Set<WorldPoint> avoidedTiles(BoatPose pose, BoatHull hull)
    {
        Set<WorldPoint> tiles = new HashSet<>(sailing.rapids().harmful());
        tiles.addAll(sailing.avoidedTiles());
        if (bounds != null)
        {
            return tiles;
        }
        int hullReach = (Math.max(hull.halfAcross(), hull.halfAlong()) + FINE_TILE - 1) / FINE_TILE;
        StringBuilder names = new StringBuilder();
        Set<WorldPoint> dangers = new HashSet<>();
        for (SeaDangers.Danger danger : SeaDangers.inView(world))
        {
            int reach = Math.min(danger.reach(), danger.distanceTo(pose.tile()) - hullReach - 1);
            if (reach < 1)
            {
                continue;
            }
            names.append(names.length() == 0 ? "" : ", ").append(danger.name()).append(" at (").append(danger.centreX()).append(", ").append(danger.centreY()).append(')');
            for (int dx = -reach; dx <= reach; dx++)
            {
                for (int dy = -reach; dy <= reach; dy++)
                {
                    dangers.add(new WorldPoint(danger.centreX() + dx, danger.centreY() + dy, 0));
                }
            }
        }
        hull.tiles(pose).forEach(dangers::remove);
        tiles.addAll(dangers);
        if (names.length() > 0 && !names.toString().equals(dangersNoted))
        {
            world.note("[sailing] planning around " + names);
        }
        dangersNoted = names.toString();
        return tiles;
    }

    /**
     * Finds the pose of the held route the boat lies at, while the route still holds
     * @param pose the boat's pose
     * @param hull the boat's hull
     * @param avoidedTiles the tiles a plan keeps the pivot off
     * @param obstacles the obstacles now
     * @return the pose index, or OFF_ROUTE
     */
    private int heldAt(BoatPose pose, BoatHull hull, Set<WorldPoint> avoidedTiles, SeaPlanner.Obstacles obstacles)
    {
        if (route == null || !objectives.equals(plannedFor) || stopping != plannedStopping)
        {
            return OFF_ROUTE;
        }
        int at = world.tick() - routeTick;
        if (at < 0 || at + 1 >= route.size())
        {
            return OFF_ROUTE;
        }
        if (!route.poses().get(at).equals(pose))
        {
            offPoses++;
            note("[sailing] off the planned pose " + at + " ticks on: expected " + route.poses().get(at) + ", was " + pose + "; planning again from there");
            return OFF_ROUTE;
        }
        if (!complete && route.size() - 1 - at <= HORIZON)
        {
            return OFF_ROUTE;
        }
        if (!SeaPlanner.clearOf(route.poses(), at, hull, obstacles) || newlyAvoidedAhead(at, avoidedTiles) || newPoolsAhead(at))
        {
            return OFF_ROUTE;
        }
        return at;
    }

    /**
     * Checks whether fetid pools seen since the held route was planned hold it from the pose at on, over the next HORIZON poses
     * @param at the route index to look from
     * @return true if newly seen pools lie under the route ahead
     */
    private boolean newPoolsAhead(int at)
    {
        FetidWaters pools = sailing.fetidPools();
        if (pools == poolsWhenPlanned)
        {
            return false;
        }
        for (int ahead = at; ahead < route.size() && ahead <= at + HORIZON; ahead++)
        {
            BoatPose pose = route.poses().get(ahead);
            if (pools.holds(pose.fineX(), pose.fineY(), pose.angle()) && !poolsWhenPlanned.holds(pose.fineX(), pose.fineY(), pose.angle()))
            {
                note("[sailing] fetid pools newly seen under the route at " + pose.tile() + "; planning again");
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a tile avoided since the held route was planned lies under its pivot from the pose at on
     * @param at the route index to look from
     * @param avoidedTiles the tiles avoided now
     * @return true if a newly avoided tile lies under the route ahead
     */
    private boolean newlyAvoidedAhead(int at, Set<WorldPoint> avoidedTiles)
    {
        if (avoidedTiles.equals(avoidedWhenPlanned))
        {
            return false;
        }
        for (int index = at + 1; index < route.size(); index++)
        {
            WorldPoint tile = route.poses().get(index).tile();
            if (avoidedTiles.contains(tile) && !avoidedWhenPlanned.contains(tile))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Plans on the planning thread
     * @param pose the boat's pose
     * @param hull the boat's hull
     * @param avoidedTiles the tiles a plan keeps the pivot off
     * @param obstacles the obstacles now
     * @param at the index of the held route's pose the boat lies at, or OFF_ROUTE
     * @return the planning's choice, once made
     */
    private CompletableFuture<Choice> plan(BoatPose pose, BoatHull hull, Set<WorldPoint> avoidedTiles, SeaPlanner.Obstacles obstacles, int at)
    {
        BoatProfile profile = sailing.profile();
        FetidWaters pools = sailing.fetidPools();
        SeaPlanner planner = (bounds == null ? sailing.planner().forProfile(profile) : sailing.planner().forProfile(profile).forTrial(bounds).withLane(lane)).withFetid(pools);
        int speed = measuredSpeed(profile);
        int mode = sailing.throttle();
        SeaPlanner.Gust boost = gustNow();
        boolean offered = boost.moves() == 0 && helm.canTrim();
        int moves = gustMoves();
        int latest = offered && !pools.isEmpty() ? trimDelayLeft() : 0;
        Rapids rapids = sailing.rapids().rapids();
        Set<WaterFamily> avoided = WaterFamily.avoided(world::varbit);
        RapidsBoost boostNow = rapidsBoost;
        int entering = rapidsEntered;
        BoatKinematics.Fouling foulingNow = fouling;
        List<SeaPlanner.Objective> through = objectives;
        boolean stop = stopping;
        SeaRoute held = at == OFF_ROUTE ? null : route;
        Set<WorldPoint> heldAvoided = avoidedWhenPlanned;
        FetidWaters heldPools = poolsWhenPlanned;
        return CompletableFuture.supplyAsync(() ->
        {
            IntFunction<SeaRoute> trimmedAfter = delay -> planner.planAhead(pose, speed, mode, new SeaPlanner.Gust(delay + 1, moves, IMMUNE_TICKS), rapids, boostNow, entering, foulingNow, through, stop, hull, avoided, avoidedTiles, obstacles);
            if (held != null)
            {
                SeaRoute gusted = trimmedAfter.apply(0);
                SeaRoute without = planner.planAhead(pose, speed, mode, boost, rapids, boostNow, entering, foulingNow, through, stop, hull, avoided, avoidedTiles, obstacles);
                boolean trimNow = trims(gusted, without, through, stop, hull) && !waitsToTrim(trimmedAfter, gusted, latest, through, stop, hull);
                return trimNow ? new Choice(gusted, 0, false, true, avoidedTiles, pools) : new Choice(held, at, true, false, heldAvoided, heldPools);
            }
            SeaRoute plain = planner.planAhead(pose, speed, mode, boost, rapids, boostNow, entering, foulingNow, through, stop, hull, avoided, avoidedTiles, obstacles);
            if (plain.size() == 1 && !stop)
            {
                plain = planner.planAhead(pose, speed, mode, boost, rapids, boostNow, entering, foulingNow, through, true, hull, avoided, avoidedTiles, obstacles);
            }
            if (plain.size() < 2)
            {
                plain = planner.planAhead(pose, speed, mode, boost, rapids, boostNow, entering, foulingNow, through, stop, hull, avoided, avoidedTiles, new SeaPlanner.Obstacles(obstacles.always(), List.of()));
            }
            if (plain.size() < 2)
            {
                plain = planner.planAhead(pose, speed, mode, boost, rapids, boostNow, entering, foulingNow, through, stop, hull, avoided, avoidedTiles, SeaPlanner.Obstacles.NONE);
            }
            if (!offered)
            {
                return new Choice(plain, 0, false, false, avoidedTiles, pools);
            }
            SeaRoute gusted = trimmedAfter.apply(0);
            boolean trimNow = trims(gusted, plain, through, stop, hull) && !waitsToTrim(trimmedAfter, gusted, latest, through, stop, hull);
            return trimNow ? new Choice(gusted, 0, false, true, avoidedTiles, pools) : new Choice(plain, 0, false, false, avoidedTiles, pools);
        }, planning);
    }

    /**
     * Checks whether a gust on offer is better trimmed a few ticks on than now
     * @param trimmedAfter plans the route for a trim made after a delay in ticks
     * @param now the route trimming now
     * @param latest the most ticks a trim can still wait
     * @param through the objectives to pass
     * @param stop whether to stop at the last objective
     * @param hull the boat's hull
     * @return true if a later trim is better
     */
    private static boolean waitsToTrim(IntFunction<SeaRoute> trimmedAfter, SeaRoute now, int latest, List<SeaPlanner.Objective> through, boolean stop, BoatHull hull)
    {
        boolean nowReaches = now.size() >= 2 && reaches(now, 0, through, stop, hull);
        for (int delay = 1; delay <= latest; delay++)
        {
            SeaRoute later = trimmedAfter.apply(delay);
            if (later.size() >= 2 && reaches(later, 0, through, stop, hull) && (!nowReaches || later.size() < now.size()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets how many ticks on a waited-for trim can still be made, by OFFER_TICKS less TRIM_MARGIN from when the gust was first offered
     * @return the ticks left, 0 when none
     */
    private int trimDelayLeft()
    {
        return offeredSince == NOT_OFFERED ? 0 : Math.max(0, OFFER_TICKS - TRIM_MARGIN - 1 - (world.tick() - offeredSince));
    }

    /**
     * Checks whether the route taking a gust is the one to sail over the one without
     * @param gusted the route taking the gust
     * @param without the route without it
     * @param through the objectives to pass
     * @param stop whether to stop at the last objective
     * @param hull the boat's hull
     * @return true if the gust is to be trimmed
     */
    private static boolean trims(SeaRoute gusted, SeaRoute without, List<SeaPlanner.Objective> through, boolean stop, BoatHull hull)
    {
        if (gusted.size() < 2)
        {
            return false;
        }
        boolean withoutReaches = reaches(without, 0, through, stop, hull);
        if (reaches(gusted, 0, through, stop, hull))
        {
            return !withoutReaches || gusted.size() <= without.size();
        }
        return !withoutReaches;
    }

    private void take(Choice choice, BoatPose pose)
    {
        SeaRoute planned = choice.route();
        if (choice.held())
        {
            move(planned, choice.from(), pose);
            return;
        }
        if (planned.size() < 2)
        {
            int at = route == null ? OFF_ROUTE : world.tick() - routeTick;
            if (at >= 0 && at + 1 < route.size() && route.poses().get(at).equals(pose))
            {
                note("[sailing] no other way found from " + pose.tile() + "; sailing on along the route held");
                movesNotFound = 0;
                move(route, at, pose);
                return;
            }
            movesNotFound++;
            holdOrStop(pose);
            return;
        }
        movesNotFound = 0;
        if (choice.trim() && helm.trimSails())
        {
            trimmedTick = world.tick();
        }
        route = planned;
        routeTick = world.tick();
        plannedFor = objectives;
        plannedStopping = stopping;
        avoidedWhenPlanned = choice.avoided();
        poolsWhenPlanned = choice.pools();
        plans++;
        SeaWorld.Boat boat = world.boat();
        complete = boat != null && reaches(planned, 0, objectives, stopping, boat.hull());
        if (!complete)
        {
            shortPlans++;
            note("[sailing] no way through the next objectives from " + pose.tile() + " found yet; sailing on toward them");
        }
        sailing.recordPlan(objectives.get(objectives.size() - 1).tile(), planned);
        move(planned, 0, pose);
    }

    /**
     * With no move planned, as when the boat already lies past the last objective, the helm is left as set unless the move it makes would put the hull on what lies in the way or aground, when the boat is stopped instead
     * @param pose the boat's pose
     */
    private void holdOrStop(BoatPose pose)
    {
        SeaWorld.Boat boat = world.boat();
        if (boat == null)
        {
            return;
        }
        BoatHull hull = boat.hull();
        BoatProfile profile = sailing.profile();
        int mode = sailing.throttle();
        BoatPose next = BoatKinematics.step(new BoatKinematics.State(pose, measuredSpeed(profile)), headingSet, mode, profile, gustNow().moves() > 0, rapidsBoost.stacks()).pose();
        if (SeaPlanner.clearOf(List.of(pose, next), 0, hull, hazards.now()) && hull.fits(sailing.sea(), next, HullRule.CALIBRATED))
        {
            note("[sailing] no move found from " + pose.tile() + "; holding the helm as set");
            return;
        }
        note("[sailing] no move found from " + pose.tile() + " and the helm as set runs into what lies ahead; stopping");
        throttle(mode, BoatKinematics.STOPPED);
    }

    /**
     * Makes the move out of a route's pose
     * @param planned the route
     * @param at the index of the route's pose the boat lies at
     * @param pose the boat's pose
     */
    private void move(SeaRoute planned, int at, BoatPose pose)
    {
        movedTick = world.tick();
        BoatPose next = planned.poses().get(at + 1);
        if (planned.speedAt(at + 1) > 0 || next.heading() != pose.heading())
        {
            int heading = pointByPoint ? next.heading() : headingFor(planned.poses(), at, headingSet);
            if (heading != headingSet)
            {
                helm.setHeading(heading);
                headingSet = heading;
            }
        }
        throttle(sailing.throttle(), planned.throttleTo(at + 1));
    }

    /**
     * Walks the facilities layer's ladder one click toward the wanted throttle
     * @param mode the throttle mode now
     * @param wanted the throttle mode wanted
     */
    private void throttle(int mode, int wanted)
    {
        if (mode == BoatKinematics.READY)
        {
            if (wanted == BoatKinematics.REVERSE)
            {
                helm.throttle(SLOWER);
            }
            return;
        }
        if (mode == wanted)
        {
            return;
        }
        if (wanted == BoatKinematics.STOPPED)
        {
            helm.throttle(STOP);
            stopClickedTick = world.tick();
            return;
        }
        helm.throttle(rung(wanted) > rung(mode) ? FASTER : SLOWER);
    }

    private static int rung(int mode)
    {
        for (int i = 0; i < LADDER.length; i++)
        {
            if (LADDER[i] == mode)
            {
                return i;
            }
        }
        return 1;
    }

    /**
     * Gets the compass point to have set for the move after the pose at index, a turn needing only the point it ends on
     * @param poses the route's poses
     * @param index the index of the pose the boat lies at
     * @param lastSent the heading last sent, or NO_HEADING
     * @return the compass point to set
     */
    static int headingFor(List<BoatPose> poses, int index, int lastSent)
    {
        int next = index + 1;
        if (lastSent != BoatKinematics.NO_HEADING && followedTo(poses, index, lastSent) >= next)
        {
            return lastSent;
        }
        int best = poses.get(next).heading();
        int bestReach = followedTo(poses, index, best);
        for (int heading = 0; heading < Compass.POINTS; heading++)
        {
            int reach = followedTo(poses, index, heading);
            if (reach > bestReach)
            {
                best = heading;
                bestReach = reach;
            }
        }
        return best;
    }

    /**
     * Finds the last pose index the boat keeps to from the pose at index with the heading set and left alone
     * @param poses the route's poses
     * @param index the index of the pose to start from
     * @param heading the compass point set
     * @return the last pose index kept to
     */
    private static int followedTo(List<BoatPose> poses, int index, int heading)
    {
        int angle = poses.get(index).angle();
        int reached = index;
        while (reached + 1 < poses.size())
        {
            angle = BoatKinematics.turnedAngle(angle, heading);
            if (angle != poses.get(reached + 1).angle())
            {
                break;
            }
            reached++;
        }
        return reached;
    }

    /**
     * Checks whether a route from a pose of it passes every objective
     * @param planned the route
     * @param from the pose index to start from
     * @param through the objectives to pass
     * @param stop whether the route must end stopped
     * @param hull the boat's hull
     * @return true if the route passes every objective
     */
    private static boolean reaches(SeaRoute planned, int from, List<SeaPlanner.Objective> through, boolean stop, BoatHull hull)
    {
        int next = 0;
        for (int index = from; index < planned.size(); index++)
        {
            BoatPose pose = planned.poses().get(index);
            BoatPose before = index > from ? planned.poses().get(index - 1) : null;
            while (next < through.size() && through.get(next).passedBetween(before, pose, hull))
            {
                next++;
            }
        }
        return next == through.size() && (!stop || planned.speedAt(planned.size() - 1) == 0);
    }

    /**
     * Gets the moves from now a trimmed gust boosts
     * @return the gust
     */
    private SeaPlanner.Gust gustNow()
    {
        if (trimmedTick == world.tick() - 1)
        {
            return new SeaPlanner.Gust(0, gustMoves(), immunityLeft());
        }
        return new SeaPlanner.Gust(0, Math.max(0, world.varbit(VarbitID.SAILING_BOAT_SPEEDBOOST_DURATION)), immunityLeft());
    }

    /**
     * Gets the moves from this tick's on that the last trim still keeps the fetid pools off
     * @return the moves left, 0 when none
     */
    private int immunityLeft()
    {
        if (trimmedTick == Integer.MIN_VALUE)
        {
            return 0;
        }
        return Math.max(0, IMMUNE_TICKS - (world.tick() - trimmedTick));
    }

    /**
     * Gets how many moves a gust trimmed now boosts
     * @return the moves boosted
     */
    private int gustMoves()
    {
        int lasts = sailing.stats().boostDuration();
        return lasts > 0 ? lasts : GUST_MOVES;
    }

    /**
     * Gets the boat's speed over the last tick, the fastest the boat is known to move at not above it
     * @param profile the boat's profile
     * @return the measured speed
     */
    private int measuredSpeed(BoatProfile profile)
    {
        int speed = sailing.speed();
        if (speed < 0)
        {
            return -profile.slowSpeed();
        }
        int known = 0;
        for (int step : profile.speeds())
        {
            if (step >= 0 && step <= speed)
            {
                known = step;
            }
        }
        return known;
    }

    private void note(String note)
    {
        if (!note.equals(noted))
        {
            noted = note;
            world.note(note);
        }
    }
}

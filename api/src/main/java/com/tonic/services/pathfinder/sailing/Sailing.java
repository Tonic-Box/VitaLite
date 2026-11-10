package com.tonic.services.pathfinder.sailing;

import com.tonic.services.pathfinder.sailing.sea.BoatHull;
import com.tonic.services.pathfinder.sailing.sea.BoatKinematics;
import com.tonic.services.pathfinder.sailing.sea.BoatPose;
import com.tonic.services.pathfinder.sailing.sea.BoatProfile;
import com.tonic.services.pathfinder.sailing.sea.FetidWaters;
import com.tonic.services.pathfinder.sailing.sea.SeaGraph;
import com.tonic.services.pathfinder.sailing.sea.SeaMap;
import com.tonic.services.pathfinder.sailing.sea.SeaPlanner;
import com.tonic.services.pathfinder.sailing.sea.SeaRoute;
import com.tonic.services.pathfinder.sailing.sea.WaterFamily;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * The player's boat and the voyages it sails: planning on a thread of its own, driving on the client thread each tick
 */
public final class Sailing
{
    private static final Object LOCK = new Object();
    private static final Executor PLANNING = Executors.newSingleThreadExecutor(runnable ->
    {
        Thread thread = new Thread(runnable, "sea-planner");
        thread.setDaemon(true);
        return thread;
    });
    private static SeaMap sea;
    private static SeaGraph graph;
    private static SeaPlanner planner;
    private static Sailing client;
    private static ClientSeaWorld clientWorld;
    private static boolean preloaded;
    private static final int KEPT_PLANS = 8;
    private static final int AT_HELM = 3;
    private static final int RECOVERY_DETOUR = 40;
    private static final int STOP_SAILS = 0;
    private static final int RAPIDS_READ_TICKS = 10;
    private static final BoatHull RAFT_HULL = new BoatHull(192, 320);

    private final SeaWorld world;
    private final Helm helm;
    private final SailDrive drive;
    private final SeaPilot pilot;
    private final OwnedBoats boats;
    private final Set<WorldPoint> avoidedTiles = new HashSet<>();
    private final ArrayDeque<PlanReport> plans = new ArrayDeque<>();
    private volatile BoatType preferredBoat;
    private SeaRapids rapidsInView;
    private FetidWaters poolsInView;
    private int poolsReadTick;
    private WorldPoint poolsBase;
    private int rapidsReadTick;
    private WorldPoint rapidsBase;
    private int planSerial;
    private Ports ports;
    private BoatPose lastPose;
    private int speed;

    Sailing(SeaWorld world, Helm helm)
    {
        this(world, helm, PLANNING);
    }

    Sailing(SeaWorld world, Helm helm, Executor planning)
    {
        this.world = world;
        this.helm = helm;
        this.drive = new SailDrive(world, this, helm);
        this.pilot = new SeaPilot(world, this, helm, planning);
        this.boats = new OwnedBoats(world);
    }

    /**
     * Gets the player's boat in the live client
     * @return the player's boat
     */
    public static Sailing get()
    {
        synchronized (LOCK)
        {
            if (client == null)
            {
                clientWorld = new ClientSeaWorld();
                client = new Sailing(clientWorld, new ClientHelm(clientWorld));
            }
            return client;
        }
    }

    /**
     * Drives the live client's voyage, loading the sea in the background the first time the player boards; client thread, every cycle
     * @param newTick whether a server tick has passed
     */
    public static void onClientCycle(boolean newTick)
    {
        Sailing sailing = get();
        if (newTick)
        {
            clientWorld.stepWalk();
            if (!preloaded && sailing.boarded())
            {
                preloaded = true;
                PLANNING.execute(sailing::planner);
            }
        }
        sailing.tick(newTick);
    }

    SeaPilot pilot()
    {
        return pilot;
    }

    /**
     * Checks whether the player is aboard a boat
     * @return true if aboard a boat
     */
    public boolean onBoat()
    {
        return world.boat() != null;
    }

    /**
     * Checks whether the player has boarded a boat, by the sidepanel's reckoning
     * @return true if boarded
     */
    public boolean boarded()
    {
        return world.varbit(VarbitID.SAILING_BOARDED_BOAT) == 1;
    }

    /**
     * Checks whether the player is locked in at the helm
     * @return true if at the helm
     */
    public boolean atHelm()
    {
        return world.varbit(VarbitID.SAILING_BOAT_FACILITY_LOCKEDIN) == AT_HELM;
    }

    /**
     * Gets the throttle mode
     * @return 0 stopped, 1 slow, 2 full, 3 astern, 4 moored
     */
    public int throttle()
    {
        return world.varbit(VarbitID.SAILING_SIDEPANEL_BOAT_MOVE_MODE);
    }

    /**
     * Checks whether the boat lies within docking reach of a port's gangplank
     * @param port the port to check
     * @return true if within docking reach
     */
    public boolean withinDockReach(Port port)
    {
        SeaWorld.Boat boat = world.boat();
        return port != null && port.land() != null && boat != null && distance(boat.pose().tile(), port.land()) <= SailDrive.DOCK_REACH;
    }

    /**
     * Gets how the boarded boat moves, the raft's when unknown
     * @return the boat's profile
     */
    public BoatProfile profile()
    {
        BoatStats stats = stats();
        return stats.type() == null ? BoatProfile.RAFT : BoatProfile.of(stats.baseSpeed(), stats.acceleration(), stats.speedCap());
    }

    /**
     * Gets the boarded boat's stats
     * @return the boat's stats
     */
    public BoatStats stats()
    {
        return BoatStats.of(world);
    }

    /**
     * Gets the player's boats
     * @return the player's boats
     */
    OwnedBoats boats()
    {
        return boats;
    }

    /**
     * Picks the kind of boat boarding takes, or null for the one last boarded
     * @param type the kind of boat, or null
     */
    void useBoat(BoatType type)
    {
        this.preferredBoat = type;
    }

    /**
     * Gets the boat boarding takes: the kind picked if owned, else the one last boarded, else the first owned
     * @return the boat to sail, or null
     */
    OwnedBoat boatToSail()
    {
        OwnedBoat picked = preferredBoat == null ? null : boats.ofType(preferredBoat);
        if (picked != null)
        {
            return picked;
        }
        OwnedBoat last = boats.lastBoarded();
        if (last != null)
        {
            return last;
        }
        List<OwnedBoat> owned = boats.all();
        return owned.isEmpty() ? null : owned.get(0);
    }

    /**
     * Finds the port to board at from a tile: where the boat is moored, unless recovering it at the nearest shipwright is nearer
     * @param from the tile to start from
     * @return the port to board at
     */
    Port boardingPort(WorldPoint from)
    {
        requireNonNull(from, "from");
        Port shipwright = null;
        for (Port port : ports().all())
        {
            if (Shipwrights.has(port) && port.placed() && port.allowed() && (shipwright == null || distance(from, port.land()) < distance(from, shipwright.land())))
            {
                shipwright = port;
            }
        }
        OwnedBoat boat = boatToSail();
        if (boat == null)
        {
            return ports().nearest(from);
        }
        Port moored = boat.dock() < 0 ? null : ports().byDock(boat.dock());
        boolean mooredUsable = moored != null && moored.placed() && moored.allowed();
        if (mooredUsable && (shipwright == null || distance(from, moored.land()) <= distance(from, shipwright.land()) + RECOVERY_DETOUR))
        {
            return moored;
        }
        return shipwright != null ? shipwright : moored;
    }

    /**
     * Trims the sails when the wind allows
     * @return whether the sails were trimmed
     */
    public boolean trim()
    {
        return world.invoke(helm::trimSails);
    }

    /**
     * Gets the boat's speed over the last tick in fine units, negative astern
     * @return the boat's speed
     */
    public int speed()
    {
        return speed;
    }

    /**
     * Gets where the player's boat is and faces
     * @return the boat's pose, or null when not aboard
     */
    public BoatPose pose()
    {
        SeaWorld.Boat boat = world.boat();
        return boat == null ? null : boat.pose();
    }

    /**
     * Gets the player's boat's hull
     * @return the boat's hull, or null when not aboard
     */
    public BoatHull hull()
    {
        SeaWorld.Boat boat = world.boat();
        return boat == null ? null : boat.hull();
    }

    /**
     * Gets the sea map bundled for the game's revision
     * @return the sea map
     */
    public SeaMap sea()
    {
        synchronized (LOCK)
        {
            if (sea == null)
            {
                int revision = SeaMap.available(world.revision());
                if (revision < 0)
                {
                    throw new IllegalStateException("no sea map is bundled for revision " + world.revision() + " or the ones before it");
                }
                if (revision != world.revision())
                {
                    world.note("no sea map for revision " + world.revision() + ", using revision " + revision + "'s");
                }
                try
                {
                    sea = SeaMap.load(revision);
                }
                catch (IOException e)
                {
                    throw new IllegalStateException("the sea map cannot be read", e);
                }
            }
            return sea;
        }
    }

    /**
     * Gets the sea's navigation web
     * @return the navigation web
     */
    public SeaGraph graph()
    {
        synchronized (LOCK)
        {
            if (graph == null)
            {
                try
                {
                    graph = SeaGraph.load();
                }
                catch (IOException e)
                {
                    throw new IllegalStateException("the sea graph cannot be read", e);
                }
            }
            return graph;
        }
    }

    /**
     * Gets the planner over the sea map and web
     * @return the planner
     */
    public SeaPlanner planner()
    {
        synchronized (LOCK)
        {
            if (planner == null)
            {
                planner = new SeaPlanner(sea(), graph());
            }
            return planner;
        }
    }

    /**
     * Gets the game's docks
     * @return the docks
     */
    public Ports ports()
    {
        synchronized (this)
        {
            if (ports == null)
            {
                Ports.DockTable table = world.dockTable();
                ports = table == null ? Ports.placedOnly(sea()) : Ports.load(table, sea());
            }
            return ports;
        }
    }

    /**
     * Plans from the boat to a tile without sailing it
     * @param goal the tile to plan to
     * @return the route, empty when no route or not aboard
     */
    public SeaRoute plan(WorldPoint goal)
    {
        requireNonNull(goal, "goal");
        Conditions now = conditions();
        return now.boat == null ? SeaRoute.EMPTY : plan(now, now.boat.pose(), goal, now.boat.hull(), avoidedTiles());
    }

    /**
     * Plans from a tile to another without sailing it, keeping off extra tiles: from the boat's pose when it lies on that tile, else from the tile at the boat's heading, with the raft's hull when not aboard
     * @param start the tile to plan from
     * @param goal the tile to plan to
     * @param avoid extra tiles to keep off
     * @return the route
     */
    public SeaRoute plan(WorldPoint start, WorldPoint goal, Set<WorldPoint> avoid)
    {
        requireNonNull(start, "start");
        requireNonNull(goal, "goal");
        requireNonNull(avoid, "avoid");
        Set<WorldPoint> avoided = new HashSet<>(avoidedTiles());
        avoided.addAll(avoid);
        Conditions now = conditions();
        BoatPose boat = now.boat == null ? null : now.boat.pose();
        BoatPose from = boat != null && boat.tile().getX() == start.getX() && boat.tile().getY() == start.getY() ? boat : BoatPose.atTile(start.getX(), start.getY(), boat == null ? 0 : boat.heading());
        return plan(now, from, goal, now.boat == null ? RAFT_HULL : now.boat.hull(), avoided);
    }

    /**
     * Plans from a pose to a tile with a hull, keeping off extra tiles
     * @param start the pose to plan from
     * @param goal the tile to plan to
     * @param hull the hull to plan with
     * @param avoid extra tiles to keep off
     * @return the route
     */
    public SeaRoute plan(BoatPose start, WorldPoint goal, BoatHull hull, Set<WorldPoint> avoid)
    {
        return plan(conditions(), start, goal, hull, avoid);
    }

    private SeaRoute plan(Conditions now, BoatPose start, WorldPoint goal, BoatHull hull, Set<WorldPoint> avoid)
    {
        SeaRoute planned = planner().forProfile(now.profile).plan(start, goal, hull, now.avoided, avoid);
        recordPlan(goal, planned);
        return planned;
    }

    /**
     * Reads the boat, the water it keeps out of and how it moves in one visit to the client thread
     * @return the conditions read
     */
    private Conditions conditions()
    {
        return world.invoke(() -> new Conditions(world.boat(), WaterFamily.avoided(world::varbit), profile()));
    }

    private static final class Conditions
    {
        private final SeaWorld.Boat boat;
        private final Set<WaterFamily> avoided;
        private final BoatProfile profile;

        Conditions(SeaWorld.Boat boat, Set<WaterFamily> avoided, BoatProfile profile)
        {
            this.boat = boat;
            this.avoided = avoided;
            this.profile = profile;
        }
    }

    /**
     * Gets the last few plans made, oldest first
     * @return the recent plans
     */
    public List<PlanReport> recentPlans()
    {
        synchronized (plans)
        {
            return new ArrayList<>(plans);
        }
    }

    void recordPlan(WorldPoint goal, SeaRoute route)
    {
        synchronized (plans)
        {
            if (plans.size() == KEPT_PLANS)
            {
                plans.removeFirst();
            }
            plans.addLast(new PlanReport(++planSerial, goal, route.size(), route.stats()));
        }
    }

    /**
     * Sails to a tile and stops there, taking the helm first when aboard but away from it
     * @param goal the tile to sail to
     */
    public void sailTo(WorldPoint goal)
    {
        requireNonNull(goal, "goal");
        world.invoke(() -> drive.sailTo(goal, null, null));
    }

    /**
     * Sails to a port and docks there
     * @param port the port to sail to
     */
    public void sailTo(Port port)
    {
        requirePlaced(port);
        world.invoke(() -> drive.sailTo(port.approach(), port, null));
    }

    /**
     * Boards at one port, sails to another and docks there
     * @param from the port to board at
     * @param to the port to sail to
     */
    public void sailTo(Port from, Port to)
    {
        requirePlaced(from);
        requirePlaced(to);
        world.invoke(() -> drive.sailTo(to.approach(), to, from));
    }

    /**
     * Sails to within a reach of a tile and stops there
     * @param goal the tile to sail to
     * @param reach how near the tile the boat may stop
     */
    public void sailWithin(WorldPoint goal, int reach)
    {
        requireNonNull(goal, "goal");
        if (reach < 1)
        {
            throw new IllegalArgumentException("reach must be at least 1, was " + reach);
        }
        world.invoke(() -> drive.sailWithin(goal, reach));
    }

    /**
     * Walks to a port's gangplank and boards the player's boat
     * @param port the port to board at
     */
    public void board(Port port)
    {
        requirePlaced(port);
        world.invoke(() -> drive.board(port));
    }

    /**
     * Docks at a port from within reach of its gangplank
     * @param port the port to dock at
     */
    public void dock(Port port)
    {
        requirePlaced(port);
        world.invoke(() -> drive.dock(port));
    }

    /**
     * Clicks the helm, which takes it
     * @return whether the helm was clicked
     */
    public boolean takeHelm()
    {
        return Boolean.TRUE.equals(world.invoke(helm::takeHelm));
    }

    /**
     * Steps off the helm onto a free deck tile beside the player
     * @return whether the step was clicked
     */
    public boolean leaveHelm()
    {
        return Boolean.TRUE.equals(world.invoke(helm::leaveHelm));
    }

    /**
     * Stops the boat when at the helm with the sails set and no voyage under way
     * @return whether the boat was stopped
     */
    public boolean stopUnderWay()
    {
        return Boolean.TRUE.equals(world.invoke(() ->
        {
            int mode = throttle();
            if (busy() || !atHelm() || mode == BoatKinematics.STOPPED || mode == BoatKinematics.READY)
            {
                return false;
            }
            helm.throttle(STOP_SAILS);
            return true;
        }));
    }

    /**
     * Drops whatever the drive is doing
     */
    public void cancel()
    {
        world.invoke(() ->
        {
            drive.cancel();
            helm.stopWalking();
        });
    }

    /**
     * Gets what the drive is doing
     * @return the drive's state
     */
    public State state()
    {
        return drive.state();
    }

    /**
     * Checks whether a voyage is under way
     * @return true if a voyage is under way
     */
    public boolean busy()
    {
        State state = drive.state();
        return pilot.busy() || state != State.IDLE && state != State.ARRIVED && state != State.FAILED;
    }

    /**
     * Gets why the drive last failed
     * @return the failure reason, empty otherwise
     */
    public String failure()
    {
        return drive.failure();
    }

    /**
     * Gets the route being followed
     * @return the route, or null
     */
    public SeaRoute currentRoute()
    {
        return pilot.busy() ? pilot.route() : null;
    }

    /**
     * Gets the tile being sailed to
     * @return the goal tile, or null
     */
    public WorldPoint goal()
    {
        return drive.goal();
    }

    /**
     * Marks a tile the boat keeps off; a route under way is replanned
     * @param tile the tile to keep off
     * @return true if the tile was not already marked
     */
    public boolean avoid(WorldPoint tile)
    {
        requireNonNull(tile, "tile");
        synchronized (avoidedTiles)
        {
            return avoidedTiles.add(tile);
        }
    }

    /**
     * Unmarks a tile marked with avoid
     * @param tile the tile to unmark
     * @return true if the tile was marked
     */
    public boolean allow(WorldPoint tile)
    {
        synchronized (avoidedTiles)
        {
            return avoidedTiles.remove(tile);
        }
    }

    /**
     * Gets the tiles the boat keeps off
     * @return the avoided tiles
     */
    public Set<WorldPoint> avoidedTiles()
    {
        synchronized (avoidedTiles)
        {
            return Collections.unmodifiableSet(new HashSet<>(avoidedTiles));
        }
    }

    /**
     * Gets the rapids in view, read again every few ticks and whenever the scene moves
     * @return the rapids in view
     */
    SeaRapids rapids()
    {
        WorldPoint base = world.sceneBase();
        if (rapidsInView == null || world.tick() - rapidsReadTick >= RAPIDS_READ_TICKS || !Objects.equals(base, rapidsBase))
        {
            rapidsInView = SeaRapids.inView(world, stats().rapidResistance());
            rapidsReadTick = world.tick();
            rapidsBase = base;
        }
        return rapidsInView;
    }

    /**
     * Gets the fetid pools in view, the same object until they change
     * @return the fetid pools in view
     */
    FetidWaters fetidPools()
    {
        WorldPoint base = world.sceneBase();
        if (poolsInView == null || world.tick() - poolsReadTick >= RAPIDS_READ_TICKS || !Objects.equals(base, poolsBase))
        {
            FetidWaters read = FetidPools.inView(world);
            poolsInView = poolsInView != null && poolsInView.sameAs(read) ? poolsInView : read;
            poolsReadTick = world.tick();
            poolsBase = base;
        }
        return poolsInView;
    }

    /**
     * Advances the drive once a server tick and gives the pilot its cycle; client thread, every cycle
     * @param newTick whether a server tick has passed
     */
    void tick(boolean newTick)
    {
        if (newTick)
        {
            readTick();
        }
        pilot.cycle(newTick);
    }

    private void readTick()
    {
        BoatPose pose = pose();
        if (pose == null || lastPose == null)
        {
            speed = 0;
        }
        else
        {
            int ramp = profile().ramp();
            int moved = (int) Math.round(Math.hypot(pose.fineX() - lastPose.fineX(), pose.fineY() - lastPose.fineY()) / ramp) * ramp;
            speed = throttle() == BoatKinematics.REVERSE ? -moved : moved;
        }
        lastPose = pose;
        drive.tick();
    }

    private static int distance(WorldPoint a, WorldPoint b)
    {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }

    private static void requirePlaced(Port port)
    {
        requireNonNull(port, "port");
        if (!port.placed())
        {
            throw new IllegalArgumentException(port.name() + " has no gangplank placed");
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
     * A plan made: its number, goal, length in ticks and cost to make
     */
    public static final class PlanReport
    {
        private final int serial;
        private final WorldPoint goal;
        private final int ticks;
        private final SeaRoute.Stats stats;

        PlanReport(int serial, WorldPoint goal, int ticks, SeaRoute.Stats stats)
        {
            this.serial = serial;
            this.goal = goal;
            this.ticks = ticks;
            this.stats = stats;
        }

        public int serial()
        {
            return serial;
        }

        public WorldPoint goal()
        {
            return goal;
        }

        public int ticks()
        {
            return ticks;
        }

        public SeaRoute.Stats stats()
        {
            return stats;
        }
    }

    /**
     * What the drive is doing
     */
    public enum State
    {
        IDLE, RECOVERING, BOARDING, TAKING_HELM, SAILING, DOCKING, ARRIVED, FAILED
    }
}

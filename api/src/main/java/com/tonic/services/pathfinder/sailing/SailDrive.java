package com.tonic.services.pathfinder.sailing;

import com.tonic.services.pathfinder.sailing.sea.BoatHull;
import com.tonic.services.pathfinder.sailing.sea.BoatKinematics;
import com.tonic.services.pathfinder.sailing.sea.BoatPose;
import com.tonic.services.pathfinder.sailing.sea.SeaPlanner;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Drives the player's boat a tick at a time
 */
final class SailDrive
{
    static final int ARRIVE_WITHIN = 1;
    static final int DOCK_REACH = 7;
    static final int WAIT_TICKS = 10;
    static final int WALK_TICKS = 300;
    static final int TRIES = 3;
    static final int STALL_TICKS = 3;
    static final int SETTLE_TICKS = 2;
    static final int PLAN_TRIES = 5;
    static final int RECOVER_TICKS = 30;
    private static final int STOP = 0;
    private static final String REMAIN_ABOARD = "Remain on the boat.";
    private static final int NOT_ASKED = Integer.MIN_VALUE / 2;
    private static final int TRAFFIC_RANGE = 24;
    private static final int TRAFFIC_TICKS = 6;
    private static final int HELD_BY_TRAFFIC_TICKS = 100;
    private static final int STUCK_TICKS = 30;
    private static final String DISEMBARK_ANYWAY = "Disembark anyway.";

    private final SeaWorld world;
    private final Sailing sailing;
    private final Helm helm;
    private Sailing.State state = Sailing.State.IDLE;
    private Phase phase = Phase.NONE;
    private WorldPoint goal;
    private Port port;
    private Port boardingPort;
    private int recoverSlot;
    private int throughReach;
    private boolean docking = true;
    private int stopReach;
    private BoatPose lastPose;
    private int stalls;
    private int stuckSinceTick = NOT_ASKED;
    private int heldSinceTick = NOT_ASKED;
    private int ticksInPhase;
    private int tries;
    private String failure = "";

    SailDrive(SeaWorld world, Sailing sailing, Helm helm)
    {
        this.world = world;
        this.sailing = sailing;
        this.helm = helm;
    }

    Sailing.State state()
    {
        return state;
    }

    String failure()
    {
        return failure;
    }

    WorldPoint goal()
    {
        return goal;
    }

    /**
     * Sails to a tile
     * @param goal the tile to sail to
     * @param port the port to dock at on arrival, or null
     * @param from the port to board at first, or null when already aboard
     */
    void sailTo(WorldPoint goal, Port port, Port from)
    {
        begin(goal, port, from);
        if (from != null)
        {
            startBoarding();
            return;
        }
        enter(Phase.SAIL, Sailing.State.SAILING);
    }

    /**
     * Sails to a port and stops there within reach of its gangplank, without docking
     * @param near the port to sail to
     */
    void sailNear(Port near)
    {
        sailTo(near.approach(), near, null);
        docking = false;
    }

    /**
     * Sails on through a tile without stopping
     * @param goal the tile to sail through
     * @param reach how near the tile counts as through it
     */
    void sailThrough(WorldPoint goal, int reach)
    {
        begin(goal, null, null);
        throughReach = reach;
        enter(Phase.SAIL, Sailing.State.SAILING);
    }

    /**
     * Sails to a tile and stops anywhere within reach of it
     * @param goal the tile to sail to
     * @param reach how near the tile the boat may stop
     */
    void sailWithin(WorldPoint goal, int reach)
    {
        begin(goal, null, null);
        stopReach = reach;
        enter(Phase.SAIL, Sailing.State.SAILING);
    }

    /**
     * Boards at a port, going nowhere
     * @param from the port to board at
     */
    void board(Port from)
    {
        begin(null, null, from);
        startBoarding();
    }

    /**
     * Walks to the gangplank
     */
    private void startBoarding()
    {
        OwnedBoat boat = sailing.boatToSail();
        if (boat == null || boat.dockedAt(boardingPort.dockId()))
        {
            enter(Phase.BOARD_WALK, Sailing.State.BOARDING);
            helm.walkToGangplank(boardingPort);
            return;
        }
        if (!Shipwrights.has(boardingPort))
        {
            fail("the boat lies elsewhere and " + boardingPort.name() + " has no shipwright to recover it");
            return;
        }
        recoverSlot = boat.slot();
        enter(Phase.RECOVER_WALK, Sailing.State.RECOVERING);
        walkToShipwright();
    }

    /**
     * Walks to the tile beside the boarding port's shipwright
     */
    private void walkToShipwright()
    {
        WorldPoint stand = Shipwrights.stand(boardingPort);
        helm.walkTo(stand != null ? stand : boardingPort.land());
    }

    /**
     * Docks at a port from where the boat stands
     * @param at the port to dock at
     */
    void dock(Port at)
    {
        begin(null, at, null);
        enter(Phase.DOCK, Sailing.State.DOCKING);
    }

    private void begin(WorldPoint goal, Port port, Port from)
    {
        this.goal = goal;
        this.port = port;
        this.boardingPort = from;
        this.lastPose = null;
        this.stalls = 0;
        this.stuckSinceTick = NOT_ASKED;
        this.heldSinceTick = NOT_ASKED;
        this.failure = "";
        this.tries = 0;
        this.throughReach = 0;
        this.docking = true;
        this.stopReach = 0;
        sailing.pilot().stop();
    }

    void cancel()
    {
        goal = null;
        port = null;
        boardingPort = null;
        sailing.pilot().stop();
        enter(Phase.NONE, Sailing.State.IDLE);
    }

    /**
     * One server tick of the drive, on the cycle thread
     */
    void tick()
    {
        ticksInPhase++;
        switch (phase)
        {
            case RECOVER_WALK:
                recoverWalk();
                break;
            case RECOVER_CLICK:
                recoverClick();
                break;
            case RECOVER_CONFIRM:
                recoverConfirm();
                break;
            case BOARD_WALK:
                boardWalk();
                break;
            case BOARD_CLICK:
                boardClick();
                break;
            case BOARD_CHOOSE:
                boardChoose();
                break;
            case STOW_CARGO:
                stowCargo();
                break;
            case TAKE_HELM:
                takeHelm();
                break;
            case SAIL:
                sail();
                break;
            case DOCK:
                dock();
                break;
            default:
                break;
        }
    }

    private void recoverWalk()
    {
        if (recovered())
        {
            toGangplank();
            return;
        }
        if (ticksInPhase > WALK_TICKS)
        {
            fail("the walk to " + boardingPort.name() + "'s shipwright took too long");
            return;
        }
        if (ticksInPhase <= SETTLE_TICKS || world.moving())
        {
            return;
        }
        if (helm.recoverBoat(Shipwrights.at(boardingPort)))
        {
            enter(Phase.RECOVER_CLICK, Sailing.State.RECOVERING);
        }
        else
        {
            fail(boardingPort.name() + "'s shipwright is not in sight to recover the boat");
        }
    }

    private void recoverClick()
    {
        if (recovered())
        {
            toGangplank();
            return;
        }
        if (helm.chooseSlot(recoverSlot))
        {
            enter(Phase.RECOVER_CONFIRM, Sailing.State.RECOVERING);
            return;
        }
        if (ticksInPhase > RECOVER_TICKS)
        {
            retry("asking " + boardingPort.name() + "'s shipwright to recover the boat", Phase.RECOVER_WALK, Sailing.State.RECOVERING);
        }
    }

    private void recoverConfirm()
    {
        if (recovered())
        {
            toGangplank();
            return;
        }
        helm.continueDialogue();
        if (ticksInPhase > RECOVER_TICKS)
        {
            retry("recovering the boat to " + boardingPort.name(), Phase.RECOVER_WALK, Sailing.State.RECOVERING);
        }
    }

    private boolean recovered()
    {
        OwnedBoat boat = sailing.boats().slot(recoverSlot);
        return boat != null && boat.dockedAt(boardingPort.dockId());
    }

    private void toGangplank()
    {
        helm.continueDialogue();
        enter(Phase.BOARD_WALK, Sailing.State.BOARDING);
        helm.walkToGangplank(boardingPort);
    }

    private void boardWalk()
    {
        if (sailing.boarded())
        {
            aboard();
            return;
        }
        if (ticksInPhase > WALK_TICKS)
        {
            fail("the walk to " + boardingPort.name() + "'s gangplank took too long");
            return;
        }
        if (!world.moving())
        {
            if (helm.board(boardingPort))
            {
                enter(Phase.BOARD_CLICK, Sailing.State.BOARDING);
            }
            else
            {
                fail(boardingPort.name() + " has no gangplank to board at");
            }
        }
    }

    private void boardClick()
    {
        if (sailing.boarded())
        {
            aboard();
            return;
        }
        OwnedBoat boat = sailing.boatToSail();
        if (boat != null && helm.chooseSlot(boat.slot()))
        {
            enter(Phase.BOARD_CHOOSE, Sailing.State.BOARDING);
            return;
        }
        if (ticksInPhase > WAIT_TICKS)
        {
            retry("boarding at " + boardingPort.name(), Phase.BOARD_WALK, Sailing.State.BOARDING);
        }
    }

    private void boardChoose()
    {
        if (sailing.boarded())
        {
            aboard();
        }
        else if (ticksInPhase > WAIT_TICKS)
        {
            retry("choosing the boat at " + boardingPort.name(), Phase.BOARD_WALK, Sailing.State.BOARDING);
        }
    }

    /**
     * Once boarded: arrived when boarding was all that was asked, else on to the helm to sail
     */
    private void aboard()
    {
        if (goal == null)
        {
            arrive();
        }
        else
        {
            enter(Phase.TAKE_HELM, Sailing.State.TAKING_HELM);
        }
    }

    private void takeHelm()
    {
        if (sailing.atHelm())
        {
            enter(Phase.SAIL, Sailing.State.SAILING);
            return;
        }
        if (world.holdingCargo())
        {
            enter(Phase.STOW_CARGO, Sailing.State.TAKING_HELM);
            return;
        }
        if (ticksInPhase == 1)
        {
            if (!helm.takeHelm())
            {
                fail("no helm to take on the boat");
            }
        }
        else if (ticksInPhase > WAIT_TICKS)
        {
            retry("taking the helm", Phase.TAKE_HELM, Sailing.State.TAKING_HELM);
        }
    }

    /**
     * Puts a held crate into the hold, as the helm refuses anyone with cargo in their hands, then takes the helm
     */
    private void stowCargo()
    {
        if (!world.holdingCargo())
        {
            enter(Phase.TAKE_HELM, Sailing.State.TAKING_HELM);
            return;
        }
        if (ticksInPhase == 1)
        {
            if (!helm.stowCargo())
            {
                fail("the crate held cannot be stowed: no cargo hold on the deck offers Deposit-held");
            }
        }
        else if (ticksInPhase > WAIT_TICKS)
        {
            retry("stowing the crate held", Phase.STOW_CARGO, Sailing.State.TAKING_HELM);
        }
    }

    /**
     * Sails on the pilot to the goal
     */
    private void sail()
    {
        SeaWorld.Boat boat = world.boat();
        if (boat == null)
        {
            fail("not aboard a boat");
            return;
        }
        if (!sailing.atHelm())
        {
            enter(Phase.TAKE_HELM, Sailing.State.TAKING_HELM);
            return;
        }
        BoatPose pose = boat.pose();
        BoatHull hull = boat.hull();
        int mode = sailing.throttle();
        int toGoal = distance(pose.tile(), goal);
        if (port != null && toGoal <= DOCK_REACH && distance(pose.tile(), port.land()) <= DOCK_REACH)
        {
            stopThere(mode, this::reachedPort);
            return;
        }
        if (throughReach > 0 && toGoal <= throughReach)
        {
            sailing.pilot().stop();
            arrive();
            return;
        }
        if (stopReach > 0 && toGoal <= stopReach || toGoal <= ARRIVE_WITHIN && stopped(mode))
        {
            stopThere(mode, port != null ? this::reachedPort : this::arrive);
            return;
        }
        if (stalled(pose, mode))
        {
            boolean held = heldByTraffic(pose, hull);
            if (held)
            {
                heldSinceTick = heldSinceTick == NOT_ASKED ? world.tick() - STALL_TICKS : heldSinceTick;
                stuckSinceTick = NOT_ASKED;
            }
            else
            {
                stuckSinceTick = stuckSinceTick == NOT_ASKED ? world.tick() - STALL_TICKS : stuckSinceTick;
                heldSinceTick = NOT_ASKED;
            }
            int stuck = world.tick() - (held ? heldSinceTick : stuckSinceTick);
            if (stuck > (held ? HELD_BY_TRAFFIC_TICKS : STUCK_TICKS))
            {
                fail((held ? "another boat has held the boat in place at " : "the boat has not moved at ") + pose + " for " + stuck + " ticks");
                return;
            }
        }
        else
        {
            stuckSinceTick = NOT_ASKED;
            heldSinceTick = NOT_ASKED;
        }
        if (sailing.pilot().movesNotFound() >= PLAN_TRIES)
        {
            fail("no route from " + pose.tile() + " to " + goal);
            return;
        }
        sailing.pilot().sail(List.of(new SeaPlanner.Objective(goal, throughReach > 0 ? throughReach : Math.max(ARRIVE_WITHIN, stopReach))), throughReach == 0, null);
    }

    /**
     * Stops piloting and the boat, then does what was arrived for once it lies stopped
     * @param mode the throttle mode
     * @param then what to do once the boat lies stopped
     */
    private void stopThere(int mode, Runnable then)
    {
        sailing.pilot().stop();
        if (stopped(mode))
        {
            then.run();
        }
        else
        {
            helm.throttle(STOP);
        }
    }

    /**
     * Checks whether the boat has sat on one pose for several ticks while the throttle was up
     * @param pose the boat's pose this tick
     * @param mode the throttle mode
     * @return true if the boat has stalled
     */
    private boolean stalled(BoatPose pose, int mode)
    {
        boolean moving = mode != BoatKinematics.STOPPED && mode != BoatKinematics.READY;
        stalls = moving && pose.equals(lastPose) ? stalls + 1 : 0;
        lastPose = pose;
        return stalls >= STALL_TICKS;
    }

    private void dock()
    {
        if (!sailing.boarded())
        {
            arrive();
            return;
        }
        SeaWorld.Boat boat = world.boat();
        if (boat == null)
        {
            arrive();
            return;
        }
        if (world.optionPresent(REMAIN_ABOARD))
        {
            leaveCargo();
            return;
        }
        if (ticksInPhase == 1)
        {
            if (distance(boat.pose().tile(), port.land()) > DOCK_REACH)
            {
                fail(port.name() + "'s gangplank is out of reach from " + boat.pose().tile());
            }
            else if (!helm.dock(port))
            {
                fail(port.name() + " has no gangplank to dock at");
            }
        }
        else if (ticksInPhase > WAIT_TICKS)
        {
            retry("docking at " + port.name(), Phase.DOCK, Sailing.State.DOCKING);
        }
    }

    /**
     * Docks at the port reached
     */
    private void reachedPort()
    {
        if (docking)
        {
            enter(Phase.DOCK, Sailing.State.DOCKING);
        }
        else
        {
            arrive();
        }
    }

    /**
     * Answers the gangplank's warning that cargo for its port is still in the hold
     */
    private void leaveCargo()
    {
        if (world.holdingCargo())
        {
            world.selectOption(DISEMBARK_ANYWAY);
            enter(Phase.DOCK, Sailing.State.DOCKING);
            ticksInPhase = WAIT_TICKS / 2;
            return;
        }
        world.selectOption(REMAIN_ABOARD);
        fail("some cargo for " + port.name() + " is still in the cargo hold: take it out before docking");
    }

    /**
     * Gets the tiles NPC ships cover now and over each of their next few ticks, on along the course they are moving
     * @param own the player's boat's pose
     * @return the tiles covered, indexed by tick
     */
    private List<Set<WorldPoint>> trafficCourse(BoatPose own)
    {
        List<Set<WorldPoint>> course = new ArrayList<>();
        for (int step = 0; step <= TRAFFIC_TICKS; step++)
        {
            course.add(new HashSet<>());
        }
        for (SeaWorld.Vessel other : world.vessels())
        {
            if (!other.npcOwned())
            {
                continue;
            }
            BoatPose next = other.next();
            if (distance(next.tile(), own.tile()) > TRAFFIC_RANGE)
            {
                continue;
            }
            int nowX = other.now().fineX();
            int nowY = other.now().fineY();
            BoatHull theirs = other.hull();
            for (int step = 0; step <= TRAFFIC_TICKS; step++)
            {
                course.get(step).addAll(theirs.tiles(new BoatPose(nowX + (next.fineX() - nowX) * step, nowY + (next.fineY() - nowY) * step, next.angle())));
            }
        }
        return course;
    }

    /**
     * Gets the tiles NPC ships cover over their next few ticks, whichever tick
     * @param own the player's boat's pose
     * @return the tiles covered
     */
    private Set<WorldPoint> traffic(BoatPose own)
    {
        Set<WorldPoint> tiles = new HashSet<>();
        for (Set<WorldPoint> step : trafficCourse(own))
        {
            tiles.addAll(step);
        }
        return tiles;
    }

    /**
     * Checks whether another boat lies on or right beside the boat's hull, as one holding it in place does
     * @param pose the boat's pose
     * @param hull the boat's hull
     * @return true if another boat holds the boat in place
     */
    private boolean heldByTraffic(BoatPose pose, BoatHull hull)
    {
        Set<WorldPoint> traffic = traffic(pose);
        for (WorldPoint tile : hull.tiles(pose))
        {
            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dy = -1; dy <= 1; dy++)
                {
                    if (traffic.contains(new WorldPoint(tile.getX() + dx, tile.getY() + dy, 0)))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean stopped(int mode)
    {
        return mode == BoatKinematics.STOPPED || mode == BoatKinematics.READY;
    }

    private static int distance(WorldPoint a, WorldPoint b)
    {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }

    private void arrive()
    {
        enter(Phase.NONE, Sailing.State.ARRIVED);
    }

    private void retry(String what, Phase again, Sailing.State state)
    {
        if (++tries >= TRIES)
        {
            fail(what + " failed " + TRIES + " times");
            return;
        }
        enter(again, state);
        if (again == Phase.BOARD_WALK)
        {
            helm.walkToGangplank(boardingPort);
        }
        else if (again == Phase.RECOVER_WALK)
        {
            walkToShipwright();
        }
    }

    /**
     * Gives up for a reason, stopping the boat first when it is under way at the helm, as it would sail on blind
     * @param reason why the drive failed
     */
    private void fail(String reason)
    {
        if (sailing.atHelm() && !stopped(sailing.throttle()))
        {
            helm.throttle(STOP);
        }
        failure = reason;
        sailing.pilot().stop();
        enter(Phase.NONE, Sailing.State.FAILED);
    }

    private void enter(Phase next, Sailing.State state)
    {
        this.phase = next;
        this.state = state;
        this.ticksInPhase = 0;
    }

    private enum Phase
    {
        NONE, RECOVER_WALK, RECOVER_CLICK, RECOVER_CONFIRM, BOARD_WALK, BOARD_CLICK, BOARD_CHOOSE, STOW_CARGO, TAKE_HELM, SAIL, DOCK
    }
}

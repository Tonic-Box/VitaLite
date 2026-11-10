package com.tonic.services.pathfinder.sailing.sea;

import java.util.Objects;

/**
 * How the server moves a boat each tick
 */
public final class BoatKinematics
{
    /**
     * Throttle mode: stopped
     */
    public static final int STOPPED = 0;
    /**
     * Throttle mode: slow ahead
     */
    public static final int SLOW = 1;
    /**
     * Throttle mode: full ahead
     */
    public static final int FULL = 2;
    /**
     * Throttle mode: astern
     */
    public static final int REVERSE = 3;
    /**
     * Throttle mode: ready
     */
    public static final int READY = 4;
    /**
     * Fine units a tick at full ahead
     */
    public static final int FULL_SPEED = 192;
    /**
     * Fine units a tick at full ahead with trimmed sails catching a gust
     */
    public static final int GUST_SPEED = FULL_SPEED + 64;
    /**
     * Fine units a tick at slow ahead
     */
    public static final int SLOW_SPEED = 64;
    /**
     * How much the speed changes a tick
     */
    public static final int RAMP = 64;
    /**
     * How far the angle turns a tick, in orientation units
     */
    public static final int TURN = Compass.STEP;
    /**
     * The grid a tick's move is rounded to
     */
    public static final int MOVE_GRID = 32;
    /**
     * The speed fetid pools slow a boat at full sail to, without a gust
     */
    public static final int POOL_FLOOR = 64;
    /**
     * The moves fetid pools keep fouling the boat for after it leaves them, the move that touched them included
     */
    public static final int FOUL_MOVES = 7;
    /**
     * The speed a fouled boat slows to off the pools
     */
    public static final int FOUL_SPEED = 256;
    /**
     * No heading set
     */
    public static final int NO_HEADING = -1;
    private static final String[] MODE_NAMES = {"stopped", "slow", "full", "astern", "ready"};

    private BoatKinematics()
    {
    }

    /**
     * Gets the speed the mode settles at, negative astern
     * @param mode throttle mode
     * @return the settled speed in fine units a tick
     */
    public static int targetSpeed(int mode)
    {
        return targetSpeed(mode, BoatProfile.RAFT);
    }

    /**
     * Gets the speed the mode settles at for that boat, negative astern
     * @param mode throttle mode
     * @param profile the boat's profile
     * @return the settled speed in fine units a tick
     */
    public static int targetSpeed(int mode, BoatProfile profile)
    {
        switch (mode)
        {
            case STOPPED:
            case READY:
                return 0;
            case SLOW:
                return profile.slowSpeed();
            case FULL:
                return profile.fullSpeed();
            case REVERSE:
                return -profile.slowSpeed();
            default:
                throw new IllegalArgumentException("mode must be 0..4");
        }
    }

    /**
     * Gets the name of a throttle mode, or its number for one not among the five
     * @param mode throttle mode
     * @return the mode's name
     */
    public static String modeName(int mode)
    {
        return mode >= 0 && mode < MODE_NAMES.length ? MODE_NAMES[mode] : Integer.toString(mode);
    }

    /**
     * Gets the speed this tick, a ramp step nearer the mode's target
     * @param speed the speed last tick
     * @param mode throttle mode
     * @return the speed this tick
     */
    public static int rampedSpeed(int speed, int mode)
    {
        return rampedSpeed(speed, mode, BoatProfile.RAFT);
    }

    /**
     * Gets the speed this tick, a ramp step nearer the mode's target
     * @param speed the speed last tick
     * @param mode throttle mode
     * @param profile the boat's profile
     * @return the speed this tick
     */
    public static int rampedSpeed(int speed, int mode, BoatProfile profile)
    {
        int target = targetSpeed(mode, profile);
        if (speed > profile.fullSpeed() && mode == FULL)
        {
            return speed;
        }
        if (speed < target)
        {
            return Math.min(target, speed + profile.ramp());
        }
        return Math.max(target, speed - profile.ramp());
    }

    /**
     * Gets the speed a fetid pool lets a boat keep for the move out of a pose it holds
     * @param before the speed before the move
     * @param after the speed the move would have without the pool
     * @param boosted whether a gust boosts the boat
     * @param profile the boat's profile
     * @return the speed kept
     */
    public static int pooledSpeed(int before, int after, boolean boosted, BoatProfile profile)
    {
        if (before < 0 || after < 0)
        {
            return after;
        }
        int floor = POOL_FLOOR + (boosted ? profile.ramp() : 0);
        return Math.min(after, Math.max(floor, before - profile.ramp()));
    }

    /**
     * Gets the speed fetid pools that touched the boat in the last FOUL_MOVES moves let it keep for a move off them
     * @param before the speed before the move
     * @param after the speed the move would have without the pools
     * @param profile the boat's profile
     * @return the speed kept
     */
    public static int fouledSpeed(int before, int after, BoatProfile profile)
    {
        if (before < 0 || after < 0)
        {
            return after;
        }
        int held = before - Math.max(0, Math.min(profile.ramp(), (before - FOUL_SPEED) / profile.ramp() * profile.ramp()));
        return Math.min(after, held);
    }

    /**
     * Gets the speed of the move out of a pose with the fetid pools known
     * @param before the speed before the move
     * @param after the speed the move would have without the pools
     * @param boosted whether a gust boosts the boat
     * @param pooled whether fetid pools hold the pose
     * @param fouling what the pools left on the boat
     * @param profile the boat's profile
     * @return the speed of the move
     */
    public static int poolSpeed(int before, int after, boolean boosted, boolean pooled, Fouling fouling, BoatProfile profile)
    {
        if (pooled)
        {
            return pooledSpeed(before, after, boosted, profile);
        }
        return fouling.fouls() ? fouledSpeed(before, after, profile) : after;
    }

    /**
     * Gets the speed a tick on with the gust known
     * @param speed the speed last tick
     * @param mode throttle mode
     * @param profile the boat's profile
     * @param boosted whether a gust boosts the boat
     * @return the speed this tick
     */
    public static int rampedSpeed(int speed, int mode, BoatProfile profile, boolean boosted)
    {
        return rampedSpeed(speed, mode, profile, boosted, 0);
    }

    /**
     * Gets the speed a tick on with the gust and the rapids known
     * @param speed the speed last tick
     * @param mode throttle mode
     * @param profile the boat's profile
     * @param boosted whether a gust boosts the boat
     * @param stacks the rapids boost stacks
     * @return the speed this tick
     */
    public static int rampedSpeed(int speed, int mode, BoatProfile profile, boolean boosted, int stacks)
    {
        if (mode != FULL)
        {
            return rampedSpeed(speed, mode, profile);
        }
        int target = profile.boostedSpeed(boosted, stacks);
        if (speed < target)
        {
            return Math.min(target, speed + profile.ramp());
        }
        return Math.max(target, speed - profile.ramp());
    }

    /**
     * Gets the angle this tick, turned toward the heading by at most a tick's turn
     * @param angle the angle last tick, in orientation units
     * @param heading the compass point set, or NO_HEADING
     * @return the turned angle
     */
    public static int turnedAngle(int angle, int heading)
    {
        if (heading == NO_HEADING)
        {
            return angle;
        }
        int arc = Compass.shortestArc(angle, Compass.bearingOf(heading));
        return (angle + Math.max(-TURN, Math.min(TURN, arc))) & Compass.ANGLE_MASK;
    }

    /**
     * Gets how far the tick moves the boat along x in fine units, as the server rounds it
     * @param speed the speed in fine units a tick
     * @param angle the angle in orientation units
     * @return the move along x in fine units
     */
    public static int advanceX(int speed, int angle)
    {
        return rounded(-speed * Math.sin(angle * (2 * Math.PI / Compass.TURN)));
    }

    /**
     * Gets how far the tick moves the boat along y in fine units, as the server rounds it
     * @param speed the speed in fine units a tick
     * @param angle the angle in orientation units
     * @return the move along y in fine units
     */
    public static int advanceY(int speed, int angle)
    {
        return rounded(-speed * Math.cos(angle * (2 * Math.PI / Compass.TURN)));
    }

    private static int rounded(double delta)
    {
        return (int) Math.round(delta / MOVE_GRID) * MOVE_GRID;
    }

    /**
     * Steps the boat one tick
     * @param state the boat's motion
     * @param heading the compass point set, or NO_HEADING
     * @param mode throttle mode
     * @return the boat's motion a tick on
     */
    public static State step(State state, int heading, int mode)
    {
        return step(state, heading, mode, BoatProfile.RAFT);
    }

    /**
     * Steps a boat of a kind one tick
     * @param state the boat's motion
     * @param heading the compass point set, or NO_HEADING
     * @param mode throttle mode
     * @param profile the boat's profile
     * @return the boat's motion a tick on
     */
    public static State step(State state, int heading, int mode, BoatProfile profile)
    {
        if (state == null || profile == null)
        {
            throw new IllegalArgumentException("state and profile cannot be null");
        }
        return moved(state, heading, rampedSpeed(state.speed(), mode, profile));
    }

    /**
     * Steps a boat of a kind one tick, the gust known
     * @param state the boat's motion
     * @param heading the compass point set, or NO_HEADING
     * @param mode throttle mode
     * @param profile the boat's profile
     * @param boosted whether a gust boosts the boat
     * @return the boat's motion a tick on
     */
    public static State step(State state, int heading, int mode, BoatProfile profile, boolean boosted)
    {
        return step(state, heading, mode, profile, boosted, 0);
    }

    /**
     * Steps a boat of a kind one tick, the gust and the rapids known
     * @param state the boat's motion
     * @param heading the compass point set, or NO_HEADING
     * @param mode throttle mode
     * @param profile the boat's profile
     * @param boosted whether a gust boosts the boat
     * @param stacks the rapids boost stacks
     * @return the boat's motion a tick on
     */
    public static State step(State state, int heading, int mode, BoatProfile profile, boolean boosted, int stacks)
    {
        if (state == null || profile == null)
        {
            throw new IllegalArgumentException("state and profile cannot be null");
        }
        return moved(state, heading, rampedSpeed(state.speed(), mode, profile, boosted, stacks));
    }

    /**
     * Steps a boat of a kind one tick, the gust, the rapids and the fetid pools known
     * @param state the boat's motion
     * @param heading the compass point set, or NO_HEADING
     * @param mode throttle mode
     * @param profile the boat's profile
     * @param boosted whether a gust boosts the boat
     * @param stacks the rapids boost stacks
     * @param pooled whether fetid pools hold the pose
     * @return the boat's motion a tick on
     */
    public static State step(State state, int heading, int mode, BoatProfile profile, boolean boosted, int stacks, boolean pooled)
    {
        if (state == null || profile == null)
        {
            throw new IllegalArgumentException("state and profile cannot be null");
        }
        return step(state, heading, mode, profile, boosted, stacks, pooled, Fouling.NONE);
    }

    /**
     * Steps a boat of a kind one tick, the gust, the rapids and the fetid pools known, with what the pools left on it
     * @param state the boat's motion
     * @param heading the compass point set, or NO_HEADING
     * @param mode throttle mode
     * @param profile the boat's profile
     * @param boosted whether a gust boosts the boat
     * @param stacks the rapids boost stacks
     * @param pooled whether fetid pools hold the pose
     * @param fouling what the pools left on the boat
     * @return the boat's motion a tick on
     */
    public static State step(State state, int heading, int mode, BoatProfile profile, boolean boosted, int stacks, boolean pooled, Fouling fouling)
    {
        if (state == null || profile == null || fouling == null)
        {
            throw new IllegalArgumentException("state, profile and fouling cannot be null");
        }
        int speed = rampedSpeed(state.speed(), mode, profile, boosted, stacks);
        return moved(state, heading, poolSpeed(state.speed(), speed, boosted, pooled, fouling, profile));
    }

    private static State moved(State state, int heading, int speed)
    {
        int angle = turnedAngle(state.pose().angle(), heading);
        BoatPose pose = new BoatPose(state.pose().fineX() + advanceX(speed, angle), state.pose().fineY() + advanceY(speed, angle), angle);
        return new State(pose, speed);
    }

    /**
     * Gets the boat some ticks on, holding the heading and throttle
     * @param state the boat's motion
     * @param heading the compass point set, or NO_HEADING
     * @param mode throttle mode
     * @param ticks how many ticks to step
     * @return the boat's motion that many ticks on
     */
    public static State after(State state, int heading, int mode, int ticks)
    {
        return after(state, heading, mode, ticks, BoatProfile.RAFT);
    }

    /**
     * Gets a boat of a kind some ticks on, holding the heading and throttle
     * @param state the boat's motion
     * @param heading the compass point set, or NO_HEADING
     * @param mode throttle mode
     * @param ticks how many ticks to step
     * @param profile the boat's profile
     * @return the boat's motion that many ticks on
     */
    public static State after(State state, int heading, int mode, int ticks, BoatProfile profile)
    {
        if (ticks < 0)
        {
            throw new IllegalArgumentException("ticks cannot be negative");
        }
        State next = state;
        for (int i = 0; i < ticks; i++)
        {
            next = step(next, heading, mode, profile);
        }
        return next;
    }

    /**
     * What fetid pools left on a boat as it came to a pose
     */
    public static final class Fouling
    {
        private final int moves;
        private final boolean contact;

        /**
         * Nothing left: clear of the pools
         */
        public static final Fouling NONE = new Fouling(0, false);

        public Fouling(int moves, boolean contact)
        {
            if (moves < 0 || moves >= FOUL_MOVES)
            {
                throw new IllegalArgumentException("moves must be 0 to " + (FOUL_MOVES - 1) + ", was " + moves);
            }
            this.moves = moves;
            this.contact = contact;
        }

        /**
         * Checks whether the pools still foul the boat on the move out of this pose
         * @return true if they still foul it
         */
        public boolean fouls()
        {
            return moves > 0;
        }

        /**
         * Gets what they leave on it at the next pose
         * @param pooled whether the pools touch the boat on this move
         * @return the fouling at the next pose
         */
        public Fouling next(boolean pooled)
        {
            return new Fouling(movesAfter(moves, contact, pooled), pooled);
        }

        /**
         * Gets the moves they foul it for at the next pose
         * @param moves the moves left now
         * @param contact whether the pools touched the boat on the last move
         * @param pooled whether the pools touch the boat on this move
         * @return the moves left at the next pose
         */
        public static int movesAfter(int moves, boolean contact, boolean pooled)
        {
            return Math.max(0, (pooled && !contact ? FOUL_MOVES : moves) - 1);
        }

        public int moves()
        {
            return moves;
        }

        public boolean contact()
        {
            return contact;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Fouling))
            {
                return false;
            }
            Fouling other = (Fouling) o;
            return moves == other.moves && contact == other.contact;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(moves, contact);
        }

        @Override
        public String toString()
        {
            return "Fouling[" + "moves=" + moves + ", " + "contact=" + contact + "]";
        }
    }

    /**
     * A boat's motion
     */
    public static final class State
    {
        private final BoatPose pose;
        private final int speed;

        public State(BoatPose pose, int speed)
        {
            this.pose = pose;
            this.speed = speed;
        }

        public BoatPose pose()
        {
            return pose;
        }

        public int speed()
        {
            return speed;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof State))
            {
                return false;
            }
            State other = (State) o;
            return Objects.equals(pose, other.pose) && speed == other.speed;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(pose, speed);
        }

        @Override
        public String toString()
        {
            return "State[" + "pose=" + pose + ", " + "speed=" + speed + "]";
        }
    }
}

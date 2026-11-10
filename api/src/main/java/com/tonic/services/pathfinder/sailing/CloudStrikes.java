package com.tonic.services.pathfinder.sailing;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * When and where the lightning clouds of stormy seas strike, watched once a tick
 */
final class CloudStrikes
{
    private static final int FORMING = 8876;
    private static final int WARNING = 8877;
    private static final int STRIKING = 13141;
    private static final int FIRST_STRIKE_AFTER_FORMING = 9;
    private static final int STRIKE_GAP = 4;
    private static final int UNKNOWN = Integer.MIN_VALUE;

    private final Map<Integer, Episode> episodes = new HashMap<>();

    /**
     * A strike a cloud will drop
     */
    public static final class Strike
    {
        private final WorldPoint tile;
        private final int tick;

        public Strike(WorldPoint tile, int tick)
        {
            this.tile = tile;
            this.tick = tick;
        }

        public WorldPoint tile()
        {
            return tile;
        }

        public int tick()
        {
            return tick;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Strike))
            {
                return false;
            }
            Strike other = (Strike) o;
            return Objects.equals(tile, other.tile) && tick == other.tick;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(tile, tick);
        }

        @Override
        public String toString()
        {
            return "Strike[" + "tile=" + tile + ", " + "tick=" + tick + "]";
        }
    }

    private static final class Episode
    {
        private final WorldPoint tile;
        private final int formedAt;
        private int struckAt = UNKNOWN;

        private Episode(WorldPoint tile, int formedAt)
        {
            this.tile = tile;
            this.formedAt = formedAt;
        }
    }

    /**
     * Notes where each cloud in view is in its cycle
     * @param world the game state to read
     * @param tick the current tick
     */
    public void watch(SeaWorld world, int tick)
    {
        Set<Integer> seen = new HashSet<>();
        for (SeaWorld.Creature npc : world.npcs())
        {
            if (npc.id() != NpcID.SAILING_SEA_STORMY_CLOUD)
            {
                continue;
            }
            seen.add(npc.index());
            note(npc.index(), npc.tile(), npc.animation(), tick);
        }
        episodes.keySet().retainAll(seen);
    }

    private void note(int index, WorldPoint at, int animation, int tick)
    {
        Episode episode = episodes.get(index);
        if (animation != FORMING && animation != WARNING && animation != STRIKING)
        {
            episodes.remove(index);
            return;
        }
        if (episode == null || !episode.tile.equals(at))
        {
            episode = new Episode(at, animation == FORMING ? tick : UNKNOWN);
            episodes.put(index, episode);
        }
        if (animation == STRIKING)
        {
            episode.struckAt = tick;
        }
    }

    /**
     * Gets the strikes due from a tick before now to the horizon, every tick of it for a cloud whose cycle is not yet known
     * @param tick the current tick
     * @param horizon how many ticks ahead to look
     * @return the strikes due
     */
    public List<Strike> due(int tick, int horizon)
    {
        List<Strike> strikes = new ArrayList<>();
        for (Episode episode : episodes.values())
        {
            if (episode.formedAt != UNKNOWN)
            {
                add(strikes, episode.tile, episode.formedAt + FIRST_STRIKE_AFTER_FORMING, tick, horizon);
                add(strikes, episode.tile, episode.formedAt + FIRST_STRIKE_AFTER_FORMING + STRIKE_GAP, tick, horizon);
            }
            else if (episode.struckAt != UNKNOWN)
            {
                add(strikes, episode.tile, episode.struckAt, tick, horizon);
                add(strikes, episode.tile, episode.struckAt + STRIKE_GAP, tick, horizon);
            }
            else
            {
                for (int at = tick; at <= tick + horizon; at++)
                {
                    strikes.add(new Strike(episode.tile, at));
                }
            }
        }
        return strikes;
    }

    private static void add(List<Strike> strikes, WorldPoint tile, int at, int tick, int horizon)
    {
        if (at >= tick - 1 && at <= tick + horizon)
        {
            strikes.add(new Strike(tile, at));
        }
    }

    /**
     * Forgets every cloud
     */
    public void clear()
    {
        episodes.clear();
    }
}

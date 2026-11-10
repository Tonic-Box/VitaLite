package com.tonic.services.pathfinder.sailing;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * What at sea hurts a boat that comes near, read from the NPCs on the open sea
 */
final class SeaDangers
{
    private static final int KRAKEN_REACH = 12;
    private static final int SHARK_REACH = 6;
    private static final Map<Integer, Integer> REACHES = Map.ofEntries(
            Map.entry(NpcID.SAILING_PYGMY_KRAKEN, KRAKEN_REACH),
            Map.entry(NpcID.SAILING_SPINED_KRAKEN, KRAKEN_REACH),
            Map.entry(NpcID.SAILING_ARMOURED_KRAKEN, KRAKEN_REACH),
            Map.entry(NpcID.SAILING_VAMPYRE_KRAKEN, KRAKEN_REACH),
            Map.entry(NpcID.SAILING_VEILED_KRAKEN, KRAKEN_REACH),
            Map.entry(NpcID.SAILING_HAMMERHEAD_SHARK, SHARK_REACH),
            Map.entry(NpcID.SAILING_TIGER_SHARK, SHARK_REACH),
            Map.entry(NpcID.SAILING_GREAT_WHITE_SHARK, SHARK_REACH));

    private SeaDangers()
    {
    }

    /**
     * Something on the sea a boat is kept away from
     */
    public static final class Danger
    {
        private final String name;
        private final int centreX;
        private final int centreY;
        private final int reach;

        public Danger(String name, int centreX, int centreY, int reach)
        {
            this.name = name;
            this.centreX = centreX;
            this.centreY = centreY;
            this.reach = reach;
        }

        /**
         * Measures how far a tile lies from the danger's middle
         * @param tile the tile to measure
         * @return the distance in tiles along the longer axis
         */
        public int distanceTo(WorldPoint tile)
        {
            return Math.max(Math.abs(tile.getX() - centreX), Math.abs(tile.getY() - centreY));
        }

        public String name()
        {
            return name;
        }

        public int centreX()
        {
            return centreX;
        }

        public int centreY()
        {
            return centreY;
        }

        public int reach()
        {
            return reach;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Danger))
            {
                return false;
            }
            Danger other = (Danger) o;
            return Objects.equals(name, other.name) && centreX == other.centreX && centreY == other.centreY && reach == other.reach;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(name, centreX, centreY, reach);
        }

        @Override
        public String toString()
        {
            return "Danger[" + "name=" + name + ", " + "centreX=" + centreX + ", " + "centreY=" + centreY + ", " + "reach=" + reach + "]";
        }
    }

    /**
     * Gets how near a boat may come to an NPC, in tiles from its middle
     * @param id the NPC id
     * @return the reach in tiles, or 0 when the NPC is not a danger
     */
    public static int reachOf(int id)
    {
        return REACHES.getOrDefault(id, 0);
    }

    /**
     * Gets the dangers on the open sea in view now
     * @param world the game state to read
     * @return the dangers in view
     */
    public static List<Danger> inView(SeaWorld world)
    {
        List<Danger> dangers = new ArrayList<>();
        for (SeaWorld.Creature npc : world.npcs())
        {
            int reach = reachOf(npc.id());
            if (reach == 0)
            {
                continue;
            }
            int half = npc.size() / 2;
            dangers.add(new Danger(npc.name(), npc.tile().getX() + half, npc.tile().getY() + half, reach));
        }
        return dangers;
    }
}

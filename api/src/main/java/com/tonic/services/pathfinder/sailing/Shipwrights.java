package com.tonic.services.pathfinder.sailing;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;

import java.util.Map;

/**
 * The shipwrights of the ports that have one, each offering Buy-boat, Customise-boat and Recover-boat
 */
final class Shipwrights
{
    /**
     * The shipwright's Buy-boat option, op 3
     */
    public static final int BUY_BOAT = 3;
    /**
     * The shipwright's Customise-boat option, op 4
     */
    public static final int CUSTOMISE_BOAT = 4;
    /**
     * The shipwright's Recover-boat option, op 5
     */
    public static final int RECOVER_BOAT = 5;
    private static final int NONE = -1;

    private static final Map<Integer, Integer> BY_DOCK_ROW = Map.ofEntries(
            Map.entry(8587, NpcID.SAILING_SHIPWRIGHT_PORT_SARIM),
            Map.entry(8590, NpcID.SAILING_SHIPWRIGHT_MUSA_POINT),
            Map.entry(8593, NpcID.SAILING_SHIPWRIGHT_CATHERBY),
            Map.entry(8595, NpcID.SAILING_SHIPWRIGHT_BRIMHAVEN),
            Map.entry(8597, NpcID.SAILING_SHIPWRIGHT_PORT_KHAZARD),
            Map.entry(8589, NpcID.SAILING_SHIPWRIGHT_LANDS_END),
            Map.entry(8594, NpcID.SAILING_SHIPWRIGHT_PORT_PISCARILIUS),
            Map.entry(8600, NpcID.SAILING_SHIPWRIGHT_CIVITAS_ILLA_FORTIS),
            Map.entry(8610, NpcID.SAILING_SHIPWRIGHT_RELLEKKA),
            Map.entry(8605, NpcID.SAILING_SHIPWRIGHT_ALDARIN),
            Map.entry(8601, NpcID.SAILING_SHIPWRIGHT_CORSAIR_COVE),
            Map.entry(8608, NpcID.SAILING_SHIPWRIGHT_PORT_ROBERTS),
            Map.entry(8609, NpcID.SAILING_SHIPWRIGHT_RED_ROCK),
            Map.entry(8613, NpcID.SAILING_SHIPWRIGHT_DEEPFIN_POINT),
            Map.entry(8616, NpcID.SAILING_SHIPWRIGHT_PRIFDDINAS));

    /**
     * Where each port's shipwright stands, often out of view from the gangplank, so a recovery walks here first
     */
    private static final Map<Integer, WorldPoint> STANDS = Map.of(
            8587, new WorldPoint(3024, 3205, 0),
            8590, new WorldPoint(2946, 3148, 0),
            8595, new WorldPoint(2769, 3228, 0));

    private Shipwrights()
    {
    }

    /**
     * Gets where a port's shipwright is found
     * @param port the port
     * @return the shipwright's tile, or null when not known
     */
    public static WorldPoint stand(Port port)
    {
        return port == null ? null : STANDS.get(port.row());
    }

    /**
     * Gets the NPC id of a port's shipwright
     * @param port the port
     * @return the shipwright's NPC id, or -1 when the port has none
     */
    public static int at(Port port)
    {
        return port == null ? NONE : BY_DOCK_ROW.getOrDefault(port.row(), NONE);
    }

    /**
     * Checks whether a port has a shipwright
     * @param port the port
     * @return true if the port has a shipwright, false otherwise
     */
    public static boolean has(Port port)
    {
        return at(port) != NONE;
    }
}

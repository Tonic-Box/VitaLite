package com.tonic.services.pathfinder.sailing;

import net.runelite.api.gameval.VarbitID;

import java.util.ArrayList;
import java.util.List;

/**
 * The player's boats, read from the five boat slot blocks of varbits (38 apart from SAILING_BOAT_1_OWNED)
 */
final class OwnedBoats
{
    /**
     * How many boats a player can own
     */
    public static final int SLOTS = 5;
    private static final int STRIDE = VarbitID.SAILING_BOAT_2_OWNED - VarbitID.SAILING_BOAT_1_OWNED;
    private static final int TYPE = VarbitID.SAILING_BOAT_1_TYPE - VarbitID.SAILING_BOAT_1_OWNED;
    private static final int PORT = VarbitID.SAILING_BOAT_1_PORT - VarbitID.SAILING_BOAT_1_OWNED;
    private static final int KEEL = VarbitID.SAILING_BOAT_1_KEEL - VarbitID.SAILING_BOAT_1_OWNED;
    private static final int HULL = VarbitID.SAILING_BOAT_1_HULL - VarbitID.SAILING_BOAT_1_OWNED;
    private static final int SAIL = VarbitID.SAILING_BOAT_1_SAIL - VarbitID.SAILING_BOAT_1_OWNED;
    private static final int STEERING = VarbitID.SAILING_BOAT_1_STEERING - VarbitID.SAILING_BOAT_1_OWNED;
    private static final int TELEPORT_FOCUS = VarbitID.SAILING_BOAT_1_TELEPORT_FOCUS - VarbitID.SAILING_BOAT_1_OWNED;
    private static final int CAPSIZED = 254;
    private static final int BOTTLED = 255;
    private static final int NOT_DOCKED = -1;

    private final SeaWorld world;

    public OwnedBoats(SeaWorld world)
    {
        this.world = world;
    }

    /**
     * Gets the boat in a slot
     * @param slot the boat slot, 1 to 5
     * @return the boat in the slot, or null when the slot is not owned
     */
    public OwnedBoat slot(int slot)
    {
        int first = VarbitID.SAILING_BOAT_1_OWNED + (requireSlot(slot) - 1) * STRIDE;
        if (world.varbit(first) != 1)
        {
            return null;
        }
        int port = world.varbit(first + PORT);
        int dock = port == CAPSIZED || port == BOTTLED ? NOT_DOCKED : port;
        int hp = world.varbit(VarbitID.SAILING_BOAT_1_STORED_HP + slot - 1);
        int maxHp = world.varbit(VarbitID.SAILING_BOAT_1_STORED_MAXHP + slot - 1);
        return new OwnedBoat(slot, BoatType.of(world.varbit(first + TYPE)), dock, port == CAPSIZED, port == BOTTLED, world.varbit(first + KEEL), world.varbit(first + HULL), world.varbit(first + SAIL), world.varbit(first + STEERING), world.varbit(first + TELEPORT_FOCUS), hp, maxHp);
    }

    /**
     * Gets every boat the player owns, by slot
     * @return the owned boats
     */
    public List<OwnedBoat> all()
    {
        List<OwnedBoat> boats = new ArrayList<>();
        for (int slot = 1; slot <= SLOTS; slot++)
        {
            OwnedBoat boat = slot(slot);
            if (boat != null)
            {
                boats.add(boat);
            }
        }
        return boats;
    }

    /**
     * Gets the boat the player last boarded of their own
     * @return the last boarded boat, or null when there is none
     */
    public OwnedBoat lastBoarded()
    {
        int slot = world.varbit(VarbitID.SAILING_LAST_PERSONAL_BOAT_BOARDED);
        return slot < 1 || slot > SLOTS ? null : slot(slot);
    }

    /**
     * Finds the first owned boat of a kind
     * @param type the boat type
     * @return the first owned boat of that type, or null when none is owned
     */
    public OwnedBoat ofType(BoatType type)
    {
        for (OwnedBoat boat : all())
        {
            if (boat.type() == type)
            {
                return boat;
            }
        }
        return null;
    }

    /**
     * Finds the first owned boat moored at a dock
     * @param dockId the dock id
     * @return the first boat moored there, or null when none is
     */
    public OwnedBoat dockedAt(int dockId)
    {
        for (OwnedBoat boat : all())
        {
            if (boat.dockedAt(dockId))
            {
                return boat;
            }
        }
        return null;
    }

    private static int requireSlot(int slot)
    {
        if (slot < 1 || slot > SLOTS)
        {
            throw new IllegalArgumentException("slot must be between 1 and " + SLOTS + ", was " + slot);
        }
        return slot;
    }
}

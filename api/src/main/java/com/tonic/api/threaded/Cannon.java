package com.tonic.api.threaded;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.data.locatables.NpcLocations;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.Walker;
import com.tonic.util.DialogueNode;
import com.tonic.util.WorldPointUtil;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;
import java.util.function.Supplier;

/**
 * Cannon class for placing and managing Dwarf multicannon
 */
public class Cannon
{
    @Setter
    private static boolean placed;

    /**
     * Place and manage cannon. Cannon will be placed at the players current
     * location, and opperation will be run until cannon is out of cannonballs
     * or opperation returns true.
     * NOTE: This method is blocking and needs to be run on its own thread.
     *
     * @param opperation Supplier<Boolean>
     */
    public static void run(final Supplier<Boolean> opperation)
    {
        Cannon.place();
        int i = 0;
        while(Cannon.isPlaced() && Cannon.reload() && i++ < 3)
        {
            if(opperation.get())
                break;

            Delays.waitUntil(() -> {
                if(repair())
                {
                    Delays.waitUntil(() -> PlayerEx.getLocal().isIdle());
                }
                return Cannon.ballsLeft() < 25;
            });
        }
        Cannon.pickup();
    }

    /**
     * Place and manage cannon. Cannon will be placed at location, and opperation
     * will be run until cannon is out of cannonballs or opperation returns true.
     * NOTE: This method is blocking and needs to be run on its own thread.
     * @param location WorldPoint
     * @param opperation Supplier<Boolean>
     */
    public static void run(final WorldPoint location, final Supplier<Boolean> opperation)
    {
        Cannon.place(location);
        while(Cannon.isPlaced() && Cannon.reload())
        {
            if(opperation.get())
                break;
            Delays.waitUntil(() -> Cannon.ballsLeft() < 10);
        }
        Cannon.pickup();
    }

    /**
     * Place cannon at current location
     * @return return true if cannon is placed
     */
    public static boolean place()
    {
        Client client = Static.getClient();
        return place(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * Place cannon at location
     * @param location WorldPoint
     * @return return true if cannon is placed
     */
    public static boolean place(WorldPoint location)
    {
        if(!ensureCannon())
        {
            Logger.warn("No cannon in inventory or placed");
            return false;
        }

        boolean placeCannon = true;
        WorldPoint current = getLocation();
        if(current != null)
        {
            if(current.distanceTo(location) > 3)
                pickup();
            else
            {
                location = current;
                placeCannon = false;
            }
        }

        Walker.walkTo(location);
        if(placeCannon)
        {
            if(!spotOpen(location, 3))
            {
                Logger.warn("Spot already occupied by cannon");
                return false;
            }
            InventoryAPI.interact(ItemID.CANNON_BASE, "Set-up");
            Delays.waitUntil(Cannon::isPlaced, 30);
        }

        placed = isPlaced();
        return isPlaced();
    }

    public static boolean spotOpen(final WorldPoint spot, int radius)
    {
        return new TileObjectQuery()
                .withNameContains("Dwarf multicannon")
                .within(spot, radius)
                .sortNearest()
                .first() == null;
    }

    /**
     * Reload cannon
     * @return boolean
     */
    public static boolean reload()
    {
        if(!isPlaced())
        {
            Logger.warn("No cannon placed");
            return false;
        }

        if(!ensureBalls())
        {
            Logger.warn("No cannonballs in inventory");
            return false;
        }

        WorldPoint location = getLocation();
        Client client = Static.getClient();
        if(!client.getLocalPlayer().getWorldLocation().equals(location))
            Walker.walkTo(location);

        TileObjectEx cannon = new TileObjectQuery()
                .withNameContains("Dwarf multicannon")
                .within(3)
                .sortNearest()
                .first();

        if(cannon == null)
        {
            Logger.warn("No cannon found at location");
            return false;
        }

        int balls = ballsLeft();
        TileObjectAPI.interact(cannon, "Fire");
        Delays.waitUntil(() -> balls < ballsLeft(), 5);
        return true;
    }

    public static boolean repair()
    {
        if(!isPlaced())
        {
            Logger.warn("No cannon placed");
            return false;
        }

        WorldPoint location = getLocation();
        Client client = Static.getClient();
        if(!client.getLocalPlayer().getWorldLocation().equals(location))
            Walker.walkTo(location);

        //RSTileObject cannon = TileObject.findObjectWithin(client, 3, "Broken multicannon");
        TileObjectEx cannon = new TileObjectQuery()
                .withAction("Repair")
                .within(3)
                .sortNearest()
                .first();
        if(cannon == null)
        {
            return false;
        }

        if(!cannon.hasAction("Repair"))
        {
            return false;
        }

        TileObjectAPI.interact(cannon, "Repair");
        return true;
    }

    /**
     * Pickup cannon
     */
    public static boolean pickup()
    {
        WorldPoint current = getLocation();
        if(current == null)
        {
            Logger.warn("No cannon placed");
            return false;
        }
        Walker.walkTo(current);
        TileObjectEx cannon = new TileObjectQuery()
                .withNameContains("Dwarf multicannon")
                .within(3)
                .sortNearest()
                .first();
        if(cannon == null)
        {
            Logger.warn("No cannon found at location");
            return false;
        }
        TileObjectAPI.interact(cannon, "Pick-up");
        boolean pass = Delays.waitUntil(() -> !isPlaced(), 5);
        if(pass)
            placed = false;
        return pass;
    }

    /**
     * Get cannon location
     * @return WorldPoint
     */
    public static WorldPoint getLocation()
    {
        return isPlaced() ? WorldPointUtil.fromJagexCoord(VarAPI.getVarp(VarPlayerID.OWNEDMCANNON)) : null;
    }

    /**
     * Get cannonballs left
     * @return int
     */
    public static int ballsLeft()
    {
        return isPlaced() ? VarAPI.getVarp(VarPlayerID.ROCKTHROWER) : 0;
    }

    /**
     * Check if cannon is placed
     * @return boolean
     */
    public static boolean isPlaced()
    {
        return VarAPI.getVarp(VarPlayerID.DROPCANNON) == 4;
    }

    /**
     * Ensure cannon is placed, or you have cannon in inventory as well as cannonballs
     * @return boolean
     */
    public static boolean ensure()
    {
        return ensureCannon() && ensureBalls();
    }

    /**
     * Ensure cannon is placed or in inventory
     * @return boolean
     */
    public static boolean ensureCannon()
    {
        return InventoryAPI.contains(ItemID.CANNON_BASE, ItemID.CANNON_STAND, ItemID.CANNON_BARRELS, ItemID.CANNON_FURNACE) || getLocation() != null;
    }

    /**
     * Ensure cannonballs are in inventory
     * @return boolean
     */
    public static boolean ensureBalls()
    {
        return InventoryAPI.containsAny(ItemID.STEEL_CANNONBALL);
    }

    public static void reclaimCannon()
    {
        NpcLocations.NULODION.talkTo();
        DialogueNode
                .get()
                .node("I've lost my cannon")
                .process();
    }

    public static boolean cachedIsPlaced()
    {
        return placed;
    }
}
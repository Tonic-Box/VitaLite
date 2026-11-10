package com.tonic.services.pathfinder.sailing;

import com.tonic.services.pathfinder.sailing.sea.FetidWaters;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

import java.util.ArrayList;
import java.util.List;

/**
 * The fetid pools in view, read from their locs (60359 to 60363, a tile to three square, turned as they lie), each tile of their footprints a pool's
 */
final class FetidPools
{
    private static final int[] POOLS = {ObjectID.SAILING_FETID_POOL, ObjectID.SAILING_FETID_POOL_2X2, ObjectID.SAILING_FETID_POOL_3X3A, ObjectID.SAILING_FETID_POOL_3X3B, ObjectID.SAILING_FETID_POOL_3X3C};

    private FetidPools()
    {
    }

    private static boolean isPool(int id)
    {
        for (int pool : POOLS)
        {
            if (pool == id)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the pools in view now
     * @param world the game state to read
     * @return the fetid water tiles in view
     */
    public static FetidWaters inView(SeaWorld world)
    {
        List<WorldPoint> tiles = new ArrayList<>();
        for (SeaWorld.Loc loc : world.locs(FetidPools::isPool))
        {
            for (int dx = 0; dx < loc.sizeX(); dx++)
            {
                for (int dy = 0; dy < loc.sizeY(); dy++)
                {
                    tiles.add(new WorldPoint(loc.corner().getX() + dx, loc.corner().getY() + dy, 0));
                }
            }
        }
        return tiles.isEmpty() ? FetidWaters.NONE : new FetidWaters(tiles);
    }
}

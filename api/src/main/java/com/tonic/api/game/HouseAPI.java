package com.tonic.api.game;

import com.tonic.data.WorldLocation;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

/**
 * POH API
 */
public class HouseAPI {
    public static final int HOUSE_LOCATION_VARBIT = VarbitID.POH_HOUSE_LOCATION;
    public static final WorldLocation[] HOUSE_LOCATIONS = {
            WorldLocation.RIMMINGTON_PORTAL,
            WorldLocation.TAVERLY_POH_PORTAL,
            WorldLocation.POLLNIVNEACH_POH_PORTAL,
            WorldLocation.RELLEKKA_POH_PORTAL,
            WorldLocation.BRIMHAVEN_POH_PORTAL,
            WorldLocation.YANILLE_POH_PORTAL,
            WorldLocation.PRIFDDINAS_POH_PORTAL,
            WorldLocation.HOSIDIUS_POH_PORTAL
    };

    /**
     * Get the outside location of the player's house
     * @return WorldPoint of the outside location, or null if no house or not logged in
     */
    public static WorldPoint getOutsideLocation()
    {
        if (!GameAPI.isLoggedIn())
        {
            return null;
        }

        int idx = VarAPI.getVar(HOUSE_LOCATION_VARBIT);
        if (idx > HOUSE_LOCATIONS.length || idx <= 0)
        {
            return null;
        }

        return WorldPointUtil.getCenter(HOUSE_LOCATIONS[idx - 1].getWorldArea());
    }
}

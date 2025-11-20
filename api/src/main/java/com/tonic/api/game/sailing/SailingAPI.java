package com.tonic.api.game.sailing;

import com.tonic.api.game.GameAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.PlayerEx;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;

/**
 * Sailing API
 */
public class SailingAPI
{
    /**
     * Sets sails to start navigating
     */
    public static void setSails()
    {
        if(!isNavigating())
        {
            return;
        }
        SailingTab.FACILITIES.open();
        if(MoveMode.getCurrent() != MoveMode.NONE)
            return;

        WidgetAPI.interact(1, InterfaceID.SailingSidepanel.FACILITIES_CONTENT_CLICKLAYER, 0);
    }

    /**
     * Unsets sails to stop navigating
     */
    public static void unSetSails()
    {
        if(!isNavigating())
        {
            return;
        }
        SailingTab.FACILITIES.open();
        if(MoveMode.getCurrent() == MoveMode.NONE)
            return;

        WidgetAPI.interact(1, InterfaceID.SailingSidepanel.FACILITIES_CONTENT_CLICKLAYER, 0);
    }

    /**
     * Checks if player is currently navigating
     * @return true if navigating, false otherwise
     */
    public static boolean isNavigating()
    {
        return VarAPI.getVar(VarbitID.SAILING_BOAT_FACILITY_LOCKEDIN) == 3;
    }

    /**
     * Sets the heading of the boat
     * @param heading Heading value (0-15)
     */
    public static void setHeading(Heading heading)
    {
        GameAPI.invokeMenuAction(heading.getValue(), 60, 0, 0, 0, PlayerEx.getLocal().getWorldViewId());
    }

    public static void directHeading(WorldPoint target)
    {
        Heading optimalHeading = Heading.getOptimalHeading(target);
        setHeading(optimalHeading);
    }

    public static boolean isOnBoat()
    {
        return VarAPI.getVar(VarbitID.SAILING_PLAYER_IS_ON_PLAYER_BOAT) == 1;
    }
}

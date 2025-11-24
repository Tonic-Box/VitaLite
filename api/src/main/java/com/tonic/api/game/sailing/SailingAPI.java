package com.tonic.api.game.sailing;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.SailingConstants;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.ClickType;
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
     * @return true if sails were set, false otherwise
     */
    public static boolean setSails()
    {
        if(!isNavigating())
            return false;

        SailingTab.FACILITIES.open();
        if(MoveMode.getCurrent() != MoveMode.STILL)
            return false;

        WidgetAPI.interact(1, InterfaceID.SailingSidepanel.FACILITIES_CONTENT_CLICKLAYER, 0);
        return true;
    }

    /**
     * Unsets sails to stop navigating
     * @return true if sails were unset, false otherwise
     */
    public static boolean unSetSails()
    {
        if(!isNavigating())
            return false;

        SailingTab.FACILITIES.open();
        if(MoveMode.getCurrent() == MoveMode.STILL)
            return false;

        WidgetAPI.interact(1, InterfaceID.SailingSidepanel.FACILITIES_CONTENT_CLICKLAYER, 0);
        return true;
    }

    /**
     * Checks if player is currently navigating
     * @return true if navigating, false otherwise
     */
    public static boolean isNavigating()
    {
        return VarAPI.getVar(VarbitID.SAILING_SIDEPANEL_PLAYER_AT_HELM) == 1;
    }

    /**
     * Sets the heading of the boat
     * @param heading Heading value (0-15)
     */
    public static void setHeading(Heading heading)
    {
        if(heading == null || getHeading() == heading)
            return;
        TClient client = Static.getClient();
        Static.invoke(() -> {
            ClickManager.click(ClickType.MOVEMENT);
            client.getPacketWriter().setHeadingPacket(heading.getValue());
        });
    }

    /**
     * Directs the boat towards a target WorldPoint
     * @param target Target WorldPoint
     * @return true if heading was set, false otherwise
     */
    public static boolean directHeading(WorldPoint target)
    {
        System.out.println(SailingAPI.getHeading());
        Heading optimalHeading = Heading.getOptimalHeading(target);
        if (optimalHeading == null) {
            return false;
        }
        setHeading(optimalHeading);
        return true;
    }

    public static Heading getHeading()
    {
        return Heading.fromValue(getHeadingValue());
    }

    public static int getHeadingValue()
    {
        return Static.invoke(() -> {
            TClient client = Static.getClient();
            int headingValue = client.getShipHeading();
            if(headingValue < 0)
                headingValue = VarAPI.getVar(VarbitID.SAILING_BOAT_SPAWNED_ANGLE);
            return headingValue / 128;
        });
    }

    /**
     * Sails the boat towards a target WorldPoint
     * @param target Target WorldPoint
     * @return true if sailing action was initiated, false otherwise
     */
    public static boolean sailTo(WorldPoint target) {
        if (!isNavigating()) {
            return false;
        }

        directHeading(target);

        if (!isMovingForward()) {
            return setSails();
        }
        return true;
    }

    /**
     * Checks if the player is currently on a boat
     * @return true if on boat, false otherwise
     */
    public static boolean isOnBoat()
    {
        return MoveMode.getCurrent() == MoveMode.ON_BOAT;
    }

    /**
     * Checks if the boat is moving forward
     * @return true if moving forward, false otherwise
     */
    public static boolean isMovingForward() {
        return MoveMode.getCurrent() == MoveMode.FORWARD;
    }

    /**
     * Checks if the boat is moving backward
     * @return true if moving backward, false otherwise
     */
    public static boolean isMovingBackward() {
        return MoveMode.getCurrent() == MoveMode.REVERSE;
    }

    /**
     * Checks if the boat is standing still
     * @return true if standing still, false otherwise
     */
    public static boolean isStandingStill() {
        return MoveMode.getCurrent() == MoveMode.STILL;
    }

    /**
     * Trims the sails on the boat
     * @return true if sails were trimmed, false otherwise
     */
    public static boolean trimSails() {
        if (!isOnBoat()) {
            return false;
        }
        TileObjectEx sail = TileObjectAPI.search()
                .withId(SailingConstants.SAILS)
                .nearest();

        if(sail != null) {
            TileObjectAPI.interact(sail, "Trim");
            return true;
        }
        return false;
    }

    /**
     * Opens the cargo hold on the boat
     * @return true if cargo hold was opened, false otherwise
     */
    public static boolean openCargo() {
        if (!isOnBoat()) {
            return false;
        }

        TileObjectEx cargo = TileObjectAPI.search()
                .withId(SailingConstants.CARGO_HOLDS)
                .nearest();

        if(cargo != null) {
            TileObjectAPI.interact(cargo, "open");
            return true;
        }
        return false;
    }
}

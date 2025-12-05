package com.tonic.api.game.sailing;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.GameAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.SailingConstants;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.services.pathfinder.sailing.BoatCollisionAPI;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.MenuAction;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;

/**
 * Sailing API
 */
public class SailingAPI
{
    @Getter
    @Setter
    private static volatile int speed = 0;

    /**
     * Sets sails to start navigating
     * @return true if sails were set, false otherwise
     */
    public static boolean setSails()
    {
        if(!isNavigating())
            return false;

        SailingTab.FACILITIES.open();
        if(MoveMode.getCurrent() != MoveMode.STILL && MoveMode.getCurrent() != MoveMode.STILL_WITH_WIND_CATCHER)
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
        return isOnBoat() && VarAPI.getVar(VarbitID.SAILING_SIDEPANEL_PLAYER_AT_HELM) == 1;
    }

    /**
     * Sets the heading of the boat
     * @param heading Heading value (0-15)
     */
    public static void setHeading(Heading heading)
    {
        if(heading == null || getHeading() == heading)
            return;

        //Menu action also sets some client side tracking stuff we care about
        GameAPI.invokeMenuAction(
                heading.getValue(),
                MenuAction.SET_HEADING.getId(),
                0, 0, 0,
                PlayerEx.getLocal().getWorldViewId()
        );
    }

    /**
     * Directs the boat towards a target WorldPoint
     * @param target Target WorldPoint
     * @return true if heading was set, false otherwise
     */
    public static boolean directHeading(WorldPoint target)
    {
        Heading optimalHeading = Heading.getOptimalHeading(target);
        if (optimalHeading == null || optimalHeading == getHeading()) {
            return false;
        }
        setHeading(optimalHeading);
        return true;
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
     * Checks if the player is on a sailing boat using worldview level check.
     *
     * @return true if player is on a sailing boat, false otherwise
     */
    public static boolean isOnBoat()
    {
        return Static.invoke(() -> !PlayerEx.getLocal().getWorldView().isTopLevel());
    }

    /**
     * Gets the current heading as a Heading enum (0-15).
     * Uses transform matrix analysis for real-time tracking during rotation.
     *
     * @return Heading enum representing the boat's current visual direction, or null if not on boat
     */
    public static Heading getHeading()
    {
        return Static.invoke(() -> {
            if(!isRotating())
            {
                return Heading.fromValue(getResolvedHeadingValue());
            }
            int rawValue = getHeadingRaw();
            if (rawValue == -1) {
                return null;
            }

            // Convert raw varbit value (0-2047) to heading value (0-15)
            // The varbit stores orientation in JAU (Jagex Angle Units) where 2048 = 360 degrees
            // Each heading = 128 JAU = 22.5 degrees
            int headingValue = rawValue / 128; // Integer division for heading index
            headingValue = headingValue % 16; // Ensure 0-15 range

            return Heading.fromValue(headingValue);
        });
    }

    /**
     * Gets the current heading in JAU (0-2047).
     * Uses transform matrix analysis for real-time heading during rotation.
     *
     * @return raw heading in JAU (0-2047), or -1 if not on boat
     */
    public static int getHeadingRaw()
    {
        return Static.invoke(() -> {
            if (!isOnBoat())
                return -1;
            WorldEntity boat = BoatCollisionAPI.getPlayerBoat();
            if (boat == null)
                return -1;
            return boat.getTargetOrientation();
        });
    }

    public static Heading getTargetHeading()
    {
        int headingValue = getTargetHeadingValue();
        return Heading.fromValue(headingValue);
    }

    public static int getTargetHeadingValue()
    {
        TClient client = Static.getClient();
        return Static.invoke(client::getShipHeading) / 128;
    }

    /**
     * Gets the current heading value (0-15) directly.
     * Convenience method using transform matrix for real-time rotation tracking.
     *
     * @return heading value (0-15), or -1 if not on boat
     */
    public static int getHeadingValue()
    {
        Heading heading = getHeading();
        return heading != null ? heading.getValue() : -1;
    }


    public static Heading getResolvedHeading()
    {
        int headingValue = getResolvedHeadingValue();
        return Heading.fromValue(headingValue);
    }

    /**
     * Gets the resolved heading from the varbit (0-15).
     * This reflects the last stable heading after rotation completes.
     *
     * @return resolved heading value (0-15)
     */
    public static int getResolvedHeadingValue()
    {
        return VarAPI.getVar(VarbitID.SAILING_BOAT_SPAWNED_ANGLE) / 128;
    }

    /**
     * Checks if the boat is currently rotating (changing heading).
     * Compares the target heading (client field) with the resolved heading (varbit).
     * Agnostic of our transform-based heading calculation.
     *
     * @return true if the boat is mid-rotation, false if stationary or not on boat
     */
    public static boolean isRotating()
    {
        return Static.invoke(() -> {
            if (!isOnBoat() || !isNavigating()) {
                return false;
            }

            int targetHeading = getTargetHeadingValue();
            return targetHeading >= 0 && targetHeading <= 15 && targetHeading != getResolvedHeadingValue();
        });
    }

    public static boolean sailsNeedTrimming()
    {
        TileObjectEx sail = TileObjectAPI.search()
                .withId(SailingConstants.SAILS)
                .withOpVisible(0)
                .nearest();
        return sail != null && sail.isOpVisible(0);
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

        if(sail != null && sail.isOpVisible(0)) {
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

    // interactions
    public static boolean navigate()
    {
        if(!isOnBoat())
            return false;

        if(isNavigating())
            return true;

        TileObjectEx helm = TileObjectAPI.search()
                .withNameContains("Helm")
                .nearest();
        if(helm != null)
        {
            helm.interact("Navigate");
            return true;
        }
        return false;
    }

    public static boolean stopNavigate()
    {
        if(!isOnBoat())
            return false;

        if(isNavigating())
            return true;

        TileObjectEx helm = TileObjectAPI.search()
                .withNameContains("Helm")
                .nearest();
        if(helm != null)
        {
            helm.interact("Stop-navigating");
            return true;
        }
        return false;
    }
}

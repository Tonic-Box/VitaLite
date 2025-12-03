package com.tonic.api.game.sailing;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.GameAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.SailingConstants;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.services.GameManager;
import com.tonic.services.pathfinder.sailing.BoatCollisionAPI;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.MenuAction;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

import java.util.ArrayList;
import java.util.List;

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
     * Checks if the player is on a sailing boat using direct WorldEntity check.
     * More reliable than varbit-based isOnBoat() as it checks the actual boat entity.
     *
     * @return true if player is on a sailing boat, false otherwise
     */
    public static boolean isOnBoat()
    {
        return Static.invoke(() -> BoatCollisionAPI.getPlayerBoat() != null);
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
            if (!isOnBoat()) {
                return -1;
            }
            return getAnimatedHeadingRaw();
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

    /**
     * Gets real-time boat heading by extracting rotation from transformation matrix.
     * Works during rotation animation, not just when varbit updates.
     *
     * Uses LocalPoint precision (128 sub-units per tile) to avoid the cardinal direction
     * snapping issue that occurred with tile-based geometry calculation.
     *
     * @return heading in JAU (0-2047), or -1 if not on boat
     */
    private static int getAnimatedHeadingRaw()
    {
        return Static.invoke(() -> {
            WorldEntity boat = BoatCollisionAPI.getPlayerBoat();
            if (boat == null) return -1;

            WorldView boatView = boat.getWorldView();
            if (boatView == null) return -1;

            // Use LocalPoint coordinates (128 units per tile) for precision
            // Center of boat-local tile (0,0) and +1 tile in Y direction
            // Boat's forward (bow) direction is along +Y in boat-local space
            LocalPoint origin = new LocalPoint(64, 64, boatView);
            LocalPoint testPoint = new LocalPoint(64, 64 + 128, boatView); // +1 tile Y

            LocalPoint transformedOrigin = boat.transformToMainWorld(origin);
            LocalPoint transformedTest = boat.transformToMainWorld(testPoint);

            if (transformedOrigin == null || transformedTest == null) {
                // Fallback to varbit
                return VarAPI.getVar(VarbitID.SAILING_BOAT_SPAWNED_ANGLE);
            }

            // Calculate direction vector (preserves full precision)
            double dx = transformedTest.getX() - transformedOrigin.getX();
            double dy = transformedTest.getY() - transformedOrigin.getY();

            // atan2 gives continuous angle from +X axis
            // RS coordinate system: +X = East, +Y = North
            // RS JAU: 0 = South, 512 = West, 1024 = North, 1536 = East
            // Use atan2(dx, dy) to measure angle from +Y (North) axis
            double angleRadians = Math.atan2(dx, dy);
            double jau = (angleRadians * 1024.0 / Math.PI);

            // Normalize to 0-2047 range
            while (jau < 0) jau += 2048;
            while (jau >= 2048) jau -= 2048;

            return (int) Math.round(jau);
        });
    }

    public static boolean sailsNeedTrimming()
    {
        return TileObjectAPI.search()
                .withId(SailingConstants.SAILS)
                .withOpVisible(0)
                .nearest() != null;
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
                .withOpVisible(0)
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

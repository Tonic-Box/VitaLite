package com.tonic.api.game.sailing;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.GameAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.SailingConstants;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.ClickType;
import com.tonic.services.pathfinder.sailing.BoatCollisionAPI;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;

/**
 * Sailing API
 */
public class SailingAPI
{
    @Setter
    @Getter
    private static volatile boolean sailsNeedTrimming = false;
    /**
     * Sets sails to start navigating
     * @return true if sails were set, false otherwise
     */
    public static boolean setSails()
    {
        System.out.println(BoatCollisionAPI.canPlayerBoatFitAtPoint(new WorldPoint(2836, 3332, 0)));
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
     * Trims the sails on the boat
     * @return true if sails were trimmed, false otherwise
     */
    public static boolean trimSails() {
        if (!isOnBoat() || !sailsNeedTrimming) {
            return false;
        }
        TileObjectEx sail = TileObjectAPI.search()
                .withId(SailingConstants.SAILS)
                .nearest();

        if(sail != null) {
            TileObjectAPI.interact(sail, "Trim");
            sailsNeedTrimming = false;
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
     * Gets the true real-time heading by analyzing the boat's collision footprint geometry.
     * This method calculates the heading based on which direction the hull tiles are oriented.
     * Works in real-time during rotation, not just when rotation completes.
     *
     * Algorithm:
     * 1. Get boat center and hull collision tiles
     * 2. Find the "forward" tiles (furthest from center in one direction)
     * 3. Calculate angle from center to forward point
     * 4. Convert to heading value (0-15)
     *
     * @return raw heading in JAU (0-2047), or -1 if not on boat
     */
    public static int getHeadingRaw()
    {
        return Static.invoke(() -> {
            if (!isOnBoat()) {
                return -1;
            }

            var boatCenter = BoatCollisionAPI.getPlayerBoatWorldPoint();
            var hullTiles = BoatCollisionAPI.getPlayerBoatCollision();

            if (boatCenter == null || hullTiles == null || hullTiles.isEmpty()) {
                // Fallback to varbit if hull analysis fails
                return VarAPI.getVar(VarbitID.SAILING_BOAT_SPAWNED_ANGLE);
            }

            // Calculate center of mass weighted toward tiles furthest from center
            // This gives us the "nose" direction of the boat
            double sumX = 0;
            double sumY = 0;
            double maxDist = 0;
            int count = 0;

            // Find the maximum distance to determine which tiles are "forward"
            for (var tile : hullTiles) {
                int dx = tile.getX() - boatCenter.getX();
                int dy = tile.getY() - boatCenter.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > maxDist) {
                    maxDist = dist;
                }
            }

            // Weight tiles by distance - tiles further from center contribute more
            // This finds the "nose" of the boat
            double threshold = maxDist * 0.6; // Consider tiles in forward 60% of hull
            for (var tile : hullTiles) {
                int dx = tile.getX() - boatCenter.getX();
                int dy = tile.getY() - boatCenter.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist >= threshold) {
                    // Weight by distance squared to emphasize forward tiles
                    double weight = dist * dist;
                    sumX += dx * weight;
                    sumY += dy * weight;
                    count++;
                }
            }

            if (count == 0) {
                // Fallback to varbit if calculation fails
                return VarAPI.getVar(VarbitID.SAILING_BOAT_SPAWNED_ANGLE);
            }

            // Calculate angle from center to weighted forward point
            double angle = Math.atan2(sumY, sumX);

            // Convert from math angle (radians, 0=East) to JAU (0=South)
            // Math: 0=East, π/2=North, π=West, -π/2=South
            // JAU: 0=South, 512=West, 1024=North, 1536=East
            double degrees = Math.toDegrees(angle);

            // Transform: rotate 270° to align 0=South
            double jauDegrees = 270 - degrees;

            // Normalize to 0-360
            while (jauDegrees < 0) jauDegrees += 360;
            while (jauDegrees >= 360) jauDegrees -= 360;

            // Convert to JAU (2048 units = 360 degrees)
            int jau = (int) Math.round((jauDegrees / 360.0) * 2048);
            jau = jau % 2048; // Ensure 0-2047 range

            return jau;
        });
    }

    /**
     * Gets the true real-time heading as a Heading enum (0-15).
     * Uses hull geometry analysis for real-time tracking during rotation.
     * Updates every frame as the boat rotates, not just when rotation completes.
     *
     * @return Heading enum representing the boat's current visual direction, or null if not on boat
     */
    public static Heading getHeading()
    {
        return Static.invoke(() -> {
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
     * Gets the true real-time heading value (0-15) directly.
     * Convenience method using hull geometry for real-time rotation tracking.
     *
     * @return heading value (0-15), or -1 if not on boat
     */
    public static int getHeadingValue()
    {
        Heading heading = getHeading();
        return heading != null ? heading.getValue() : -1;
    }

    public static int getResolvedHeading()
    {
        return VarAPI.getVar(VarbitID.SAILING_BOAT_SPAWNED_ANGLE) / 128;
    }
}

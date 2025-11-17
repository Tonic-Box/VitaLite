package com.tonic.api.game;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.ClickType;
import com.tonic.services.mouse.ClickVisualizationOverlay;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;

import java.util.List;
import java.util.Random;

/**
 * Movement API
 */
public class MovementAPI {
    private static final Random random = new Random();
    private static final int STAMINA_VARBIT = 25;
    private static final int RUN_VARP = 173;

    /**
     * Gets the destination world point of the player
     * @return The destination world point of the player, or null if none
     */
    public static WorldPoint getDestinationWorldPoint()
    {
        Client client = Static.getClient();
        LocalPoint lp = client.getLocalDestinationLocation();
        if (lp == null)
        {
            return null;
        }
        return WorldPoint.fromLocal(client, lp);
    }

    /**
     * Checks if run is enabled
     * @return True if run is enabled, false otherwise
     */
    public static boolean isRunEnabled()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getVarpValue(RUN_VARP)) == 1;
    }

    public static void toggleRun()
    {
        WidgetAPI.interact(1, InterfaceID.Orbs.RUNBUTTON, -1, -1);
    }

    /**
     * Checks if the stamina potion effect is active
     * @return True if the stamina potion effect is active, false otherwise
     */
    public static boolean staminaInEffect()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getVarbitValue(STAMINA_VARBIT)) > 0;
    }

    /**
     * Checks if the player is currently moving
     * @return True if the player is moving, false otherwise
     */
    public static boolean isMoving()
    {
        Client client = Static.getClient();
        WorldPoint wp = client.getLocalPlayer().getWorldLocation();
        WorldPoint dest = getDestinationWorldPoint();
        if(dest == null)
            return false;

        return !wp.equals(dest);
    }

    /**
     * Walks to the specified world point
     * @param worldPoint The world point to walk to
     */
    public static void walkToWorldPoint(WorldPoint worldPoint)
    {
        walkToWorldPoint(worldPoint.getX(), worldPoint.getY());
    }

    /**
     * Walks to the specified world coordinates
     * @param worldX The world x coordinate to walk to
     * @param worldY The world y coordinate to walk to
     */
    public static void walkToWorldPoint(int worldX, int worldY)
    {
        walkToWorldPoint(worldX, worldY, false);
    }

    public static void walkToWorldPoint(int worldX, int worldY, boolean ctrlDown)
    {
        TClient tClient = Static.getClient();
        Client client = Static.getClient();
        ClickVisualizationOverlay.recordWalkClick(new WorldPoint(worldX, worldY, client.getTopLevelWorldView().getPlane()));
        ClickManager.click(ClickType.MOVEMENT);
        Static.invoke(() -> {
            tClient.getPacketWriter().walkPacket(worldX, worldY, ctrlDown);
            ClickManager.clearClickBox();
        });
    }

    /**
     * Walks to a random point within the specified radius of the given world point
     * @param worldPoint The world point to walk to
     * @param radius The radius around the world point to walk to
     */
    public static void walkAproxWorldPoint(WorldPoint worldPoint, int radius)
    {

        int x = random.nextInt(radius * 2) + worldPoint.getX();
        int y = random.nextInt(radius * 2) + worldPoint.getY();
        walkToWorldPoint(x, y);
    }

    /**
     * Walks to a point relative to the player's current world position
     * @param offsetX The x offset from the player's current position
     * @param offsetY The y offset from the player's current position
     */
    public static void walkRelativeToWorldPoint(int offsetX, int offsetY)
    {
        Client client = Static.getClient();
        WorldPoint wp = client.getLocalPlayer().getWorldLocation();
        walkToWorldPoint(wp.getX() + offsetX, wp.getY() + offsetY);
    }

    /**
     * Walks towards the specified world point, if the distance is greater than 100, it
     * will walk to a point 100 units away in the direction of the target.
     * @param worldPoint The world point to walk towards
     * @return True if the player is within 100 units of the target, false otherwise
     */
    public static boolean walkTowards(WorldPoint worldPoint)
    {
        Client client = Static.getClient();
        if(client.getLocalPlayer() == null || worldPoint == null)
            return false;
        if (client.getLocalPlayer().getWorldLocation().distanceTo(worldPoint) > 100) {
            WorldView worldView = client.getTopLevelWorldView();
            WorldPoint local = client.getLocalPlayer().getWorldLocation();
            int distance = client.getLocalPlayer().getWorldLocation().distanceTo(worldPoint);
            int ratio = 100 / distance;
            int xDiff = worldPoint.getX() - local.getX();
            int yDiff = worldPoint.getY() - local.getY();
            int newX = (int) Math.round(local.getX() + xDiff * ratio + (yDiff * 0.75));
            int newY = (int) Math.round(local.getY() + yDiff * ratio + (xDiff * 0.75));
            WorldPoint nwp = new WorldPoint(newX, newY, worldView.getPlane());
            walkToWorldPoint(nwp);
            return false;
        }
        walkToWorldPoint(worldPoint);
        return true;
    }

    /**
     * Walks in cardinal directions (N, S, E, W) towards the target world point until a reachable point is found
     * @param target The target world point to walk towards
     */
    public static void cardinalWalk(WorldPoint target)
    {
        Client client = Static.getClient();
        WorldPoint local = client.getLocalPlayer().getWorldLocation();
        int x = target.getX();
        int y = target.getY();
        int plane = local.getPlane();
        WorldPoint dest = null;
        while(dest == null || !SceneAPI.isReachable(local, dest))
        {
            if(target.getX() > local.getX())
            {
                x++;
            }
            else if(target.getX() < local.getX())
            {
                x--;
            }
            if(target.getY() > local.getY())
            {
                y++;
            }
            else if(target.getY() < local.getY())
            {
                y--;
            }
            dest = new WorldPoint(x, y, plane);
        }
        walkToWorldPoint(dest);
    }

    /**
     * Walks in cardinal directions (N, S, E, W) away from the target world area until a reachable point is found
     * @param target The target world area to walk away from
     * @param distance The distance to walk away from the target area
     */
    public static void cardinalWalk(WorldArea target, int distance) {
        Client client = Static.getClient();
        WorldPoint localPlayer = client.getLocalPlayer().getWorldLocation();
        int x = target.getX();
        int y = target.getY();
        int width = target.getWidth();
        int height = target.getHeight();
        int plane = target.getPlane();
        WorldPoint dest = null;

        // Define the bounds of the NPC's area
        int leftBound = x;
        int rightBound = x + width;
        int bottomBound = y;
        int topBound = y + height;

        // Define potential cardinal direction movements
        int[][] directions = {
                {0, -1}, // south
                {0, 1},  // north
                {-1, 0}, // west
                {1, 0}   // east
        };

        // Iterate over cardinal directions to find a valid destination
        for (int[] dir : directions) {
            int newX = localPlayer.getX() + dir[0] * distance;
            int newY = localPlayer.getY() + dir[1] * distance;

            // Ensure the destination is outside the NPC's area and within the specified distance
            if ((newX < leftBound - distance || newX > rightBound + distance || newY < bottomBound - distance || newY > topBound + distance) &&
                    (newX != localPlayer.getX() || newY != localPlayer.getY())) {

                // Create a new potential destination WorldPoint
                dest = new WorldPoint(newX, newY, plane);

                // Check if the destination is reachable
                if (SceneAPI.isReachable(localPlayer, dest)) {
                    break;
                }

                // Reset destination if not reachable
                dest = null;
            }
        }

        // If a valid destination is found, move the player
        if (dest != null) {
            walkToWorldPoint(dest);
        }
    }

    /**
     * Checks if there is a path from the current world point to the target world point
     * @param current The current world point
     * @param target The target world point
     * @return True if there is a path, false otherwise
     */
    public static boolean canPathTo(WorldPoint current, WorldPoint target)
    {
        List<WorldPoint> pathTo = SceneAPI.checkPointsTo(current, target);
        return pathTo != null && pathTo.contains(target);
    }

    /**
     * Checks if there is a path from the local player's world point to the target world point
     * @param target The target world point
     * @return True if there is a path, false otherwise
     */
    public static boolean canPathTo(WorldPoint target) {
        Client client = Static.getClient();
        WorldPoint current = client.getLocalPlayer().getWorldLocation();
        return canPathTo(current, target);
    }
}

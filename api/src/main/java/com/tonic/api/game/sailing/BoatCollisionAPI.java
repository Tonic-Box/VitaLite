package com.tonic.api.game.sailing;

import com.tonic.Static;
import com.tonic.api.TClient;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

import java.util.*;

/**
 * API for boat-related operations and collision detection
 */
public class BoatCollisionAPI
{
    // Cache for performance optimization
    private static final Map<WorldEntity, Collection<WorldPoint>> boatCollisionCache = new HashMap<>();
    private static final Map<WorldEntity, Collection<WorldPoint>> boatHullCache = new HashMap<>();
    private static final Map<WorldEntity, Collection<WorldPoint>> boatDeckCache = new HashMap<>();
    private static int lastGameTick = -1;

    /**
     * Gets the player's boat WorldEntity
     * @return the boat WorldEntity, or null if not on a boat
     */
    public static WorldEntity getPlayerBoat()
    {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            Player player = client.getLocalPlayer();

            if (player == null) {
                return null;
            }

            WorldView playerView = player.getWorldView();

            // If player is in a sub-worldview, that's the boat
            if (!playerView.isTopLevel()) {
                // Get the WorldEntity that contains this worldview
                LocalPoint playerLocal = player.getLocalLocation();
                int worldViewId = playerLocal.getWorldView();

                return client.getTopLevelWorldView()
                        .worldEntities()
                        .byIndex(worldViewId);
            }

            return null;
        });
    }

    /**
     * Gets the collision tiles of a boat (WorldEntity) projected onto the main world
     * @param boat the WorldEntity (boat)
     * @return collection of WorldPoints where the boat has collision in the main world
     */
    public static Collection<WorldPoint> getBoatCollisionInMainWorld(WorldEntity boat)
    {
        return Static.invoke(() -> {
            if (boat == null) {
                return Collections.emptyList();
            }

            WorldView boatView = boat.getWorldView();
            if (boatView == null) {
                return Collections.emptyList();
            }

            // Get collision data from boat's worldview
            CollisionData[] collisionMaps = boatView.getCollisionMaps();
            if (collisionMaps == null) {
                return Collections.emptyList();
            }

            List<WorldPoint> collisionTiles = new ArrayList<>();
            Client client = Static.getClient();
            int plane = boatView.getPlane();

            // Iterate through all tiles in the boat's worldview
            int sizeX = boatView.getSizeX();
            int sizeY = boatView.getSizeY();

            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    // Check if this tile has collision
                    if (hasCollision(collisionMaps, plane, x, y)) {
                        // Create LocalPoint in boat's worldview
                        LocalPoint boatLocal = LocalPoint.fromScene(x, y, boatView);

                        // Transform to main world
                        LocalPoint mainWorldLocal = boat.transformToMainWorld(boatLocal);

                        if (mainWorldLocal != null) {
                            // Convert to WorldPoint
                            WorldPoint mainWorldPoint = WorldPoint.fromLocal(client, mainWorldLocal);
                            collisionTiles.add(mainWorldPoint);
                        }
                    }
                }
            }

            return collisionTiles;
        });
    }

    /**
     * Gets the collision tiles of the player's boat in the main world (cached)
     * @return collection of WorldPoints where the boat has collision, or empty if not on boat
     */
    public static Collection<WorldPoint> getPlayerBoatCollision()
    {
        return Static.invoke(() -> {
            WorldEntity boat = getPlayerBoat();
            if (boat == null) {
                return Collections.emptyList();
            }
            return getBoatCollisionCached(boat);
        });
    }

    /**
     * Gets cached boat collision in main world (refreshes each game tick)
     * @param boat the WorldEntity (boat)
     * @return collection of WorldPoints where the boat has collision
     */
    public static Collection<WorldPoint> getBoatCollisionCached(WorldEntity boat)
    {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            int currentTick = client.getTickCount();

            // Clear cache on new tick
            if (currentTick != lastGameTick) {
                boatCollisionCache.clear();
                boatHullCache.clear();
                boatDeckCache.clear();
                lastGameTick = currentTick;
            }

            // Return cached value if available
            return boatCollisionCache.computeIfAbsent(boat, BoatCollisionAPI::getBoatCollisionInMainWorld);
        });
    }

    /**
     * Gets only the boat hull collision (object-based) in the main world
     * @param boat the WorldEntity (boat)
     * @return collection of WorldPoints where the boat hull is
     */
    public static Collection<WorldPoint> getBoatHullInMainWorld(WorldEntity boat)
    {
        return Static.invoke(() -> {
            if (boat == null) {
                return Collections.emptyList();
            }

            WorldView boatView = boat.getWorldView();
            if (boatView == null) {
                return Collections.emptyList();
            }

            CollisionData[] collisionMaps = boatView.getCollisionMaps();
            if (collisionMaps == null) {
                return Collections.emptyList();
            }

            List<WorldPoint> hullTiles = new ArrayList<>();
            Client client = Static.getClient();
            int plane = boatView.getPlane();
            int sizeX = boatView.getSizeX();
            int sizeY = boatView.getSizeY();

            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    // Check only for object collision (boat hull)
                    if (hasObjectCollision(collisionMaps, plane, x, y)) {
                        LocalPoint boatLocal = LocalPoint.fromScene(x, y, boatView);
                        LocalPoint mainWorldLocal = boat.transformToMainWorld(boatLocal);

                        if (mainWorldLocal != null) {
                            WorldPoint mainWorldPoint = WorldPoint.fromLocal(client, mainWorldLocal);
                            hullTiles.add(mainWorldPoint);
                        }
                    }
                }
            }

            return hullTiles;
        });
    }

    /**
     * Gets the boat hull of the player's boat (cached)
     * @return collection of WorldPoints where the boat hull is, or empty if not on boat
     */
    public static Collection<WorldPoint> getPlayerBoatHull()
    {
        return Static.invoke(() -> {
            WorldEntity boat = getPlayerBoat();
            if (boat == null) {
                return Collections.emptyList();
            }
            return getBoatHullCached(boat);
        });
    }

    /**
     * Gets cached boat hull in main world (refreshes each game tick)
     * @param boat the WorldEntity (boat)
     * @return collection of WorldPoints where the boat hull is
     */
    public static Collection<WorldPoint> getBoatHullCached(WorldEntity boat)
    {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            int currentTick = client.getTickCount();

            // Clear cache on new tick
            if (currentTick != lastGameTick) {
                boatCollisionCache.clear();
                boatHullCache.clear();
                boatDeckCache.clear();
                lastGameTick = currentTick;
            }

            // Return cached value if available
            return boatHullCache.computeIfAbsent(boat, BoatCollisionAPI::getBoatHullInMainWorld);
        });
    }

    /**
     * Gets walkable deck tiles of the boat in main world
     * @param boat the WorldEntity (boat)
     * @return collection of WorldPoints where you can walk on the boat
     */
    public static Collection<WorldPoint> getBoatDeckInMainWorld(WorldEntity boat)
    {
        return Static.invoke(() -> {
            if (boat == null) {
                return Collections.emptyList();
            }

            WorldView boatView = boat.getWorldView();
            if (boatView == null) {
                return Collections.emptyList();
            }

            CollisionData[] collisionMaps = boatView.getCollisionMaps();
            if (collisionMaps == null) {
                return Collections.emptyList();
            }

            List<WorldPoint> deckTiles = new ArrayList<>();
            Client client = Static.getClient();
            int plane = boatView.getPlane();
            int sizeX = boatView.getSizeX();
            int sizeY = boatView.getSizeY();

            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    // Only tiles WITHOUT collision (walkable deck)
                    if (!hasCollision(collisionMaps, plane, x, y)) {
                        LocalPoint boatLocal = LocalPoint.fromScene(x, y, boatView);
                        LocalPoint mainWorldLocal = boat.transformToMainWorld(boatLocal);

                        if (mainWorldLocal != null) {
                            WorldPoint mainWorldPoint = WorldPoint.fromLocal(client, mainWorldLocal);
                            deckTiles.add(mainWorldPoint);
                        }
                    }
                }
            }

            return deckTiles;
        });
    }

    /**
     * Gets the walkable deck of the player's boat (cached)
     * @return collection of WorldPoints where you can walk on the boat, or empty if not on boat
     */
    public static Collection<WorldPoint> getPlayerBoatDeck()
    {
        return Static.invoke(() -> {
            WorldEntity boat = getPlayerBoat();
            if (boat == null) {
                return Collections.emptyList();
            }
            return getBoatDeckCached(boat);
        });
    }

    /**
     * Gets cached boat deck in main world (refreshes each game tick)
     * @param boat the WorldEntity (boat)
     * @return collection of WorldPoints where you can walk on the boat
     */
    public static Collection<WorldPoint> getBoatDeckCached(WorldEntity boat)
    {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            int currentTick = client.getTickCount();

            // Clear cache on new tick
            if (currentTick != lastGameTick) {
                boatCollisionCache.clear();
                boatHullCache.clear();
                boatDeckCache.clear();
                lastGameTick = currentTick;
            }

            // Return cached value if available
            return boatDeckCache.computeIfAbsent(boat, BoatCollisionAPI::getBoatDeckInMainWorld);
        });
    }

    /**
     * Checks if the boat overlaps with a WorldArea in the main world
     * @param boat the WorldEntity (boat)
     * @param area the WorldArea to check
     * @return true if the boat overlaps with the area
     */
    public static boolean boatOverlapsArea(WorldEntity boat, WorldArea area)
    {
        return Static.invoke(() -> {
            if (boat == null || area == null) {
                return false;
            }

            Collection<WorldPoint> collision = getBoatCollisionCached(boat);
            return collision.stream().anyMatch(area::contains);
        });
    }

    /**
     * Checks if the player's boat overlaps with a WorldArea
     * @param area the WorldArea to check
     * @return true if the player's boat overlaps with the area
     */
    public static boolean playerBoatOverlapsArea(WorldArea area)
    {
        return Static.invoke(() -> {
            WorldEntity boat = getPlayerBoat();
            if (boat == null) {
                return false;
            }
            return boatOverlapsArea(boat, area);
        });
    }

    /**
     * Checks if the boat's collision includes a specific WorldPoint
     * @param boat the WorldEntity (boat)
     * @param point the WorldPoint to check
     * @return true if the boat is over this tile
     */
    public static boolean boatContainsPoint(WorldEntity boat, WorldPoint point)
    {
        return Static.invoke(() -> {
            if (boat == null || point == null) {
                return false;
            }

            Collection<WorldPoint> collision = getBoatCollisionCached(boat);
            return collision.contains(point);
        });
    }

    /**
     * Checks if the player's boat is over a specific WorldPoint
     * @param point the WorldPoint to check
     * @return true if the player's boat is over this tile
     */
    public static boolean playerBoatContainsPoint(WorldPoint point)
    {
        return Static.invoke(() -> {
            WorldEntity boat = getPlayerBoat();
            if (boat == null) {
                return false;
            }
            return boatContainsPoint(boat, point);
        });
    }

    /**
     * Gets the boat's position in the main world
     * @param boat the WorldEntity (boat)
     * @return the boat's LocalPoint in the main world, or null
     */
    public static LocalPoint getBoatPosition(WorldEntity boat)
    {
        return Static.invoke(() -> {
            if (boat == null) {
                return null;
            }
            return boat.getLocalLocation();
        });
    }

    /**
     * Gets the player's boat position in the main world
     * @return the boat's LocalPoint in the main world, or null if not on boat
     */
    public static LocalPoint getPlayerBoatPosition()
    {
        return Static.invoke(() -> {
            WorldEntity boat = getPlayerBoat();
            if (boat == null) {
                return null;
            }
            return getBoatPosition(boat);
        });
    }

    /**
     * Gets the boat's WorldPoint in the main world
     * @param boat the WorldEntity (boat)
     * @return the boat's WorldPoint, or null
     */
    public static WorldPoint getBoatWorldPoint(WorldEntity boat)
    {
        return Static.invoke(() -> {
            LocalPoint local = getBoatPosition(boat);
            if (local == null) {
                return null;
            }
            return WorldPoint.fromLocal(Static.getClient(), local);
        });
    }

    /**
     * Gets the player's boat WorldPoint in the main world
     * @return the boat's WorldPoint, or null if not on boat
     */
    public static WorldPoint getPlayerBoatWorldPoint()
    {
        return Static.invoke(() -> {
            WorldEntity boat = getPlayerBoat();
            if (boat == null) {
                return null;
            }
            return getBoatWorldPoint(boat);
        });
    }

    /**
     * Finds any heading that would allow the boat to fit at the given WorldPoint.
     * Tries all 16 possible headings and returns the first one that works.
     * @param boat the WorldEntity (boat)
     * @param targetPoint the WorldPoint in main world to center the boat at
     * @return the first valid Heading, or null if no heading allows the boat to fit
     */
    public static Heading findAnyValidHeading(WorldEntity boat, WorldPoint targetPoint)
    {
        return Static.invoke(() -> {
            if (boat == null || targetPoint == null) {
                return null;
            }

            // Try all 16 possible headings
            for (Heading heading : Heading.values()) {
                if (canBoatFitAtPoint(boat, targetPoint, heading)) {
                    return heading;
                }
            }

            // No valid heading found
            return null;
        });
    }

    /**
     * Finds any heading that would allow the player's boat to fit at the given WorldPoint.
     * Tries all 16 possible headings and returns the first one that works.
     * @param targetPoint the WorldPoint in main world to center the boat at
     * @return the first valid Heading, or null if no heading allows the boat to fit or not on boat
     */
    public static Heading findAnyValidPlayerBoatHeading(WorldPoint targetPoint)
    {
        return Static.invoke(() -> {
            WorldEntity boat = getPlayerBoat();
            if (boat == null) {
                return null;
            }
            return findAnyValidHeading(boat, targetPoint);
        });
    }

    /**
     * Checks if the boat can fit centered at the given WorldPoint with a specific heading.
     * @param boat the WorldEntity (boat)
     * @param targetPoint the WorldPoint in main world to center the boat at
     * @param targetHeading the desired heading/orientation
     * @return true if the boat would fit without collision at that heading, false otherwise
     */
    public static boolean canBoatFitAtPoint(WorldEntity boat, WorldPoint targetPoint, Heading targetHeading)
    {
        return Static.invoke(() -> {
            if (boat == null || targetPoint == null || targetHeading == null) {
                return false;
            }

            // Get boat's current position and heading
            LocalPoint currentBoatLocal = boat.getLocalLocation();
            if (currentBoatLocal == null) {
                return false;
            }

            Client client = Static.getClient();
            WorldPoint currentBoatPos = WorldPoint.fromLocal(client, currentBoatLocal);

            // Get current boat collision footprint
            Collection<WorldPoint> currentCollision = getBoatCollisionInMainWorld(boat);
            if (currentCollision.isEmpty()) {
                return false;
            }

            // Get current and target heading values
            int currentHeadingValue = SailingAPI.getHeadingValue();
            if (currentHeadingValue == -1) {
                return false; // Not on boat
            }

            int targetHeadingValue = targetHeading.getValue();

            // Calculate rotation difference in heading units
            int headingDiff = targetHeadingValue - currentHeadingValue;

            // Normalize to -8 to 7 range (shortest rotation)
            while (headingDiff > 8) headingDiff -= 16;
            while (headingDiff < -8) headingDiff += 16;

            // Convert heading difference to degrees (each heading = 22.5°)
            double rotationDegrees = headingDiff * 22.5;
            double rotationRadians = Math.toRadians(rotationDegrees);

            // Precompute sin/cos for rotation
            double cos = Math.cos(rotationRadians);
            double sin = Math.sin(rotationRadians);

            // Get main world collision data
            WorldView mainWorld = client.getTopLevelWorldView();
            CollisionData[] mainWorldCollisionMaps = mainWorld.getCollisionMaps();
            if (mainWorldCollisionMaps == null) {
                return false;
            }

            // For each collision tile in current footprint
            for (WorldPoint collisionTile : currentCollision) {
                // Calculate offset from current boat center
                int dx = collisionTile.getX() - currentBoatPos.getX();
                int dy = collisionTile.getY() - currentBoatPos.getY();

                // Rotate the offset around the center
                int rotatedDx = (int) Math.round(dx * cos - dy * sin);
                int rotatedDy = (int) Math.round(dx * sin + dy * cos);

                // Apply rotated offset to target position
                WorldPoint projectedTile = new WorldPoint(
                        targetPoint.getX() + rotatedDx,
                        targetPoint.getY() + rotatedDy,
                        targetPoint.getPlane()
                );

                // Convert to scene coordinates
                LocalPoint checkLocal = LocalPoint.fromWorld(mainWorld, projectedTile);
                if (checkLocal == null) {
                    // Point is outside the loaded scene
                    return false;
                }

                int sceneX = checkLocal.getSceneX();
                int sceneY = checkLocal.getSceneY();
                int plane = projectedTile.getPlane();

                // Check if there's collision in the main world at this point
                if (hasCollision(mainWorldCollisionMaps, plane, sceneX, sceneY)) {
                    // Collision detected - boat won't fit
                    return false;
                }
            }

            // No collisions detected - boat would fit
            return true;
        });
    }

    /**
     * Checks if the player's boat can fit centered at the given WorldPoint at any heading.
     * @param targetPoint the WorldPoint in main world to center the boat at
     * @return true if the boat would fit without collision at any heading, false if no heading works or not on boat
     */
    public static boolean canPlayerBoatFitAtPoint(WorldPoint targetPoint)
    {
        return Static.invoke(() -> {
            WorldEntity boat = getPlayerBoat();
            if (boat == null) {
                return false;
            }
            return findAnyValidHeading(boat, targetPoint) != null;
        });
    }

    /**
     * Checks if the player's boat can fit centered at the given WorldPoint with a specific heading.
     * @param targetPoint the WorldPoint in main world to center the boat at
     * @param targetHeading the desired heading/orientation
     * @return true if the boat would fit without collision at that heading, false if it would collide or player not on boat
     */
    public static boolean canPlayerBoatFitAtPoint(WorldPoint targetPoint, Heading targetHeading)
    {
        return Static.invoke(() -> {
            WorldEntity boat = getPlayerBoat();
            if (boat == null) {
                return false;
            }
            return canBoatFitAtPoint(boat, targetPoint, targetHeading);
        });
    }

    /**
     * Finds the nearest valid point where the boat can fit at any heading, within a search radius.
     * @param boat the WorldEntity (boat)
     * @param targetPoint the desired WorldPoint to center the boat at
     * @param searchRadius the maximum distance to search for a valid point
     * @return the nearest valid WorldPoint, or null if no valid point found
     */
    public static WorldPoint findNearestValidBoatPosition(WorldEntity boat, WorldPoint targetPoint, int searchRadius)
    {
        return Static.invoke(() -> {
            if (boat == null || targetPoint == null) {
                return null;
            }

            // First check if target point itself is valid at any heading
            if (findAnyValidHeading(boat, targetPoint) != null) {
                return targetPoint;
            }

            // Spiral search outward from target point
            for (int radius = 1; radius <= searchRadius; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        // Only check points at current radius (edge of square)
                        if (Math.abs(dx) != radius && Math.abs(dy) != radius) {
                            continue;
                        }

                        WorldPoint checkPoint = new WorldPoint(
                                targetPoint.getX() + dx,
                                targetPoint.getY() + dy,
                                targetPoint.getPlane()
                        );

                        if (findAnyValidHeading(boat, checkPoint) != null) {
                            return checkPoint;
                        }
                    }
                }
            }

            // No valid position found
            return null;
        });
    }

    /**
     * Finds the nearest valid point where the boat can fit with a specific heading, within a search radius.
     * @param boat the WorldEntity (boat)
     * @param targetPoint the desired WorldPoint to center the boat at
     * @param targetHeading the desired heading/orientation
     * @param searchRadius the maximum distance to search for a valid point
     * @return the nearest valid WorldPoint, or null if no valid point found
     */
    public static WorldPoint findNearestValidBoatPosition(WorldEntity boat, WorldPoint targetPoint, Heading targetHeading, int searchRadius)
    {
        return Static.invoke(() -> {
            if (boat == null || targetPoint == null || targetHeading == null) {
                return null;
            }

            // First check if target point itself is valid
            if (canBoatFitAtPoint(boat, targetPoint, targetHeading)) {
                return targetPoint;
            }

            // Spiral search outward from target point
            for (int radius = 1; radius <= searchRadius; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        // Only check points at current radius (edge of square)
                        if (Math.abs(dx) != radius && Math.abs(dy) != radius) {
                            continue;
                        }

                        WorldPoint checkPoint = new WorldPoint(
                                targetPoint.getX() + dx,
                                targetPoint.getY() + dy,
                                targetPoint.getPlane()
                        );

                        if (canBoatFitAtPoint(boat, checkPoint, targetHeading)) {
                            return checkPoint;
                        }
                    }
                }
            }

            // No valid position found
            return null;
        });
    }

    /**
     * Finds the nearest valid point where the player's boat can fit at any heading.
     * @param targetPoint the desired WorldPoint to center the boat at
     * @param searchRadius the maximum distance to search for a valid point
     * @return the nearest valid WorldPoint, or null if no valid point found or not on boat
     */
    public static WorldPoint findNearestValidPlayerBoatPosition(WorldPoint targetPoint, int searchRadius)
    {
        return Static.invoke(() -> {
            WorldEntity boat = getPlayerBoat();
            if (boat == null) {
                return null;
            }
            return findNearestValidBoatPosition(boat, targetPoint, searchRadius);
        });
    }

    /**
     * Finds the nearest valid point where the player's boat can fit with a specific heading.
     * @param targetPoint the desired WorldPoint to center the boat at
     * @param targetHeading the desired heading/orientation
     * @param searchRadius the maximum distance to search for a valid point
     * @return the nearest valid WorldPoint, or null if no valid point found or not on boat
     */
    public static WorldPoint findNearestValidPlayerBoatPosition(WorldPoint targetPoint, Heading targetHeading, int searchRadius)
    {
        return Static.invoke(() -> {
            WorldEntity boat = getPlayerBoat();
            if (boat == null) {
                return null;
            }
            return findNearestValidBoatPosition(boat, targetPoint, targetHeading, searchRadius);
        });
    }

    /**
     * Checks if a tile has collision at the given coordinates
     * @param collisionMaps collision data array
     * @param plane the plane
     * @param sceneX scene X coordinate
     * @param sceneY scene Y coordinate
     * @return true if the tile is blocked
     */
    private static boolean hasCollision(CollisionData[] collisionMaps, int plane, int sceneX, int sceneY)
    {
        return Static.invoke(() -> {
            if (plane < 0 || plane >= collisionMaps.length) {
                return false;
            }

            CollisionData collision = collisionMaps[plane];
            if (collision == null) {
                return false;
            }

            int[][] flags = collision.getFlags();
            if (sceneX < 0 || sceneX >= flags.length || sceneY < 0 || sceneY >= flags[0].length) {
                return false;
            }

            int flag = flags[sceneX][sceneY];

            // Check for any collision flag
            return (flag & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0 ||
                    (flag & CollisionDataFlag.BLOCK_MOVEMENT_FLOOR) != 0 ||
                    (flag & CollisionDataFlag.BLOCK_MOVEMENT_OBJECT) != 0;
        });
    }

    /**
     * Checks if a tile has object collision (boat hull)
     * @param collisionMaps collision data array
     * @param plane the plane
     * @param sceneX scene X coordinate
     * @param sceneY scene Y coordinate
     * @return true if the tile has object collision
     */
    private static boolean hasObjectCollision(CollisionData[] collisionMaps, int plane, int sceneX, int sceneY)
    {
        return Static.invoke(() -> {
            if (plane < 0 || plane >= collisionMaps.length) {
                return false;
            }

            CollisionData collision = collisionMaps[plane];
            if (collision == null) {
                return false;
            }

            int[][] flags = collision.getFlags();
            if (sceneX < 0 || sceneX >= flags.length || sceneY < 0 || sceneY >= flags[0].length) {
                return false;
            }

            int flag = flags[sceneX][sceneY];

            // Only object collision (boat structure)
            return (flag & CollisionDataFlag.BLOCK_MOVEMENT_OBJECT) != 0;
        });
    }

    /**
     * Clears all cached boat collision data
     * Call this if you need to force a refresh
     */
    public static void clearCache()
    {
        boatCollisionCache.clear();
        boatHullCache.clear();
        boatDeckCache.clear();
    }
}

package com.tonic.api.game.sailing;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

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
    }

    /**
     * Gets the collision tiles of a boat (WorldEntity) projected onto the main world
     * @param boat the WorldEntity (boat)
     * @return collection of WorldPoints where the boat has collision in the main world
     */
    public static Collection<WorldPoint> getBoatCollisionInMainWorld(WorldEntity boat)
    {
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
    }

    /**
     * Gets the collision tiles of the player's boat in the main world (cached)
     * @return collection of WorldPoints where the boat has collision, or empty if not on boat
     */
    public static Collection<WorldPoint> getPlayerBoatCollision()
    {
        WorldEntity boat = getPlayerBoat();
        if (boat == null) {
            return Collections.emptyList();
        }
        return getBoatCollisionCached(boat);
    }

    /**
     * Gets cached boat collision in main world (refreshes each game tick)
     * @param boat the WorldEntity (boat)
     * @return collection of WorldPoints where the boat has collision
     */
    public static Collection<WorldPoint> getBoatCollisionCached(WorldEntity boat)
    {
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
    }

    /**
     * Gets only the boat hull collision (object-based) in the main world
     * @param boat the WorldEntity (boat)
     * @return collection of WorldPoints where the boat hull is
     */
    public static Collection<WorldPoint> getBoatHullInMainWorld(WorldEntity boat)
    {
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
    }

    /**
     * Gets the boat hull of the player's boat (cached)
     * @return collection of WorldPoints where the boat hull is, or empty if not on boat
     */
    public static Collection<WorldPoint> getPlayerBoatHull()
    {
        WorldEntity boat = getPlayerBoat();
        if (boat == null) {
            return Collections.emptyList();
        }
        return getBoatHullCached(boat);
    }

    /**
     * Gets cached boat hull in main world (refreshes each game tick)
     * @param boat the WorldEntity (boat)
     * @return collection of WorldPoints where the boat hull is
     */
    public static Collection<WorldPoint> getBoatHullCached(WorldEntity boat)
    {
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
    }

    /**
     * Gets walkable deck tiles of the boat in main world
     * @param boat the WorldEntity (boat)
     * @return collection of WorldPoints where you can walk on the boat
     */
    public static Collection<WorldPoint> getBoatDeckInMainWorld(WorldEntity boat)
    {
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
    }

    /**
     * Gets the walkable deck of the player's boat (cached)
     * @return collection of WorldPoints where you can walk on the boat, or empty if not on boat
     */
    public static Collection<WorldPoint> getPlayerBoatDeck()
    {
        WorldEntity boat = getPlayerBoat();
        if (boat == null) {
            return Collections.emptyList();
        }
        return getBoatDeckCached(boat);
    }

    /**
     * Gets cached boat deck in main world (refreshes each game tick)
     * @param boat the WorldEntity (boat)
     * @return collection of WorldPoints where you can walk on the boat
     */
    public static Collection<WorldPoint> getBoatDeckCached(WorldEntity boat)
    {
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
    }

    /**
     * Checks if the boat overlaps with a WorldArea in the main world
     * @param boat the WorldEntity (boat)
     * @param area the WorldArea to check
     * @return true if the boat overlaps with the area
     */
    public static boolean boatOverlapsArea(WorldEntity boat, WorldArea area)
    {
        if (boat == null || area == null) {
            return false;
        }

        Collection<WorldPoint> collision = getBoatCollisionCached(boat);
        return collision.stream().anyMatch(area::contains);
    }

    /**
     * Checks if the player's boat overlaps with a WorldArea
     * @param area the WorldArea to check
     * @return true if the player's boat overlaps with the area
     */
    public static boolean playerBoatOverlapsArea(WorldArea area)
    {
        WorldEntity boat = getPlayerBoat();
        if (boat == null) {
            return false;
        }
        return boatOverlapsArea(boat, area);
    }

    /**
     * Checks if the boat's collision includes a specific WorldPoint
     * @param boat the WorldEntity (boat)
     * @param point the WorldPoint to check
     * @return true if the boat is over this tile
     */
    public static boolean boatContainsPoint(WorldEntity boat, WorldPoint point)
    {
        if (boat == null || point == null) {
            return false;
        }

        Collection<WorldPoint> collision = getBoatCollisionCached(boat);
        return collision.contains(point);
    }

    /**
     * Checks if the player's boat is over a specific WorldPoint
     * @param point the WorldPoint to check
     * @return true if the player's boat is over this tile
     */
    public static boolean playerBoatContainsPoint(WorldPoint point)
    {
        WorldEntity boat = getPlayerBoat();
        if (boat == null) {
            return false;
        }
        return boatContainsPoint(boat, point);
    }

    /**
     * Gets the boat's position in the main world
     * @param boat the WorldEntity (boat)
     * @return the boat's LocalPoint in the main world, or null
     */
    public static LocalPoint getBoatPosition(WorldEntity boat)
    {
        if (boat == null) {
            return null;
        }
        return boat.getLocalLocation();
    }

    /**
     * Gets the player's boat position in the main world
     * @return the boat's LocalPoint in the main world, or null if not on boat
     */
    public static LocalPoint getPlayerBoatPosition()
    {
        WorldEntity boat = getPlayerBoat();
        if (boat == null) {
            return null;
        }
        return getBoatPosition(boat);
    }

    /**
     * Gets the boat's WorldPoint in the main world
     * @param boat the WorldEntity (boat)
     * @return the boat's WorldPoint, or null
     */
    public static WorldPoint getBoatWorldPoint(WorldEntity boat)
    {
        LocalPoint local = getBoatPosition(boat);
        if (local == null) {
            return null;
        }
        return WorldPoint.fromLocal(Static.getClient(), local);
    }

    /**
     * Gets the player's boat WorldPoint in the main world
     * @return the boat's WorldPoint, or null if not on boat
     */
    public static WorldPoint getPlayerBoatWorldPoint()
    {
        WorldEntity boat = getPlayerBoat();
        if (boat == null) {
            return null;
        }
        return getBoatWorldPoint(boat);
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

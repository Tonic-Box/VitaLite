package com.tonic.services.pathfinder.sailing;

import com.tonic.Static;
import com.tonic.api.game.sailing.Heading;
import com.tonic.api.game.sailing.SailingAPI;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.collision.CollisionMap;
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

            // Get boat view size (actual usable area, not including padding)
            int sizeX = boatView.getSizeX();
            int sizeY = boatView.getSizeY();

            // Iterate through boat view area only (skip boundary padding)
            // The collision map has padding, but boat view tiles start at offset (1,1)
            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    // Skip boundary markers (flag 0x00FFFFFF)
                    if (plane >= collisionMaps.length || collisionMaps[plane] == null) {
                        continue;
                    }

                    int[][] flagArray = collisionMaps[plane].getFlags();
                    if (x + 1 >= flagArray.length || y + 1 >= flagArray[0].length) {
                        continue;
                    }

                    int flag = flagArray[x + 1][y + 1]; // Offset by 1 to skip boundary

                    // Skip boundary markers (0x00FFFFFF) and check for actual collision
                    if (flag == 0x00FFFFFF || !hasCollision(collisionMaps, plane, x + 1, y + 1)) {
                        continue;
                    }

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

            // Get boat view size (actual usable area, not including padding)
            int sizeX = boatView.getSizeX();
            int sizeY = boatView.getSizeY();

            // Iterate through boat view area only (skip boundary padding)
            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    // Skip boundary markers (flag 0x00FFFFFF)
                    if (plane >= collisionMaps.length || collisionMaps[plane] == null) {
                        continue;
                    }

                    int[][] flagArray = collisionMaps[plane].getFlags();
                    if (x + 1 >= flagArray.length || y + 1 >= flagArray[0].length) {
                        continue;
                    }

                    int flag = flagArray[x + 1][y + 1]; // Offset by 1 to skip boundary

                    // Skip boundary markers and check for ANY collision (boat hull includes all collision tiles)
                    if (flag == 0x00FFFFFF || !hasCollision(collisionMaps, plane, x + 1, y + 1)) {
                        continue;
                    }

                    LocalPoint boatLocal = LocalPoint.fromScene(x, y, boatView);
                    LocalPoint mainWorldLocal = boat.transformToMainWorld(boatLocal);

                    if (mainWorldLocal != null) {
                        WorldPoint mainWorldPoint = WorldPoint.fromLocal(client, mainWorldLocal);
                        hullTiles.add(mainWorldPoint);
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
     * Gets only the outer perimeter (OBJECT collision tiles) of the player's boat
     * Used for heading calculation - excludes interior FULL/FLOOR tiles
     * @return collection of WorldPoints of the outer hull perimeter only
     */
    public static Collection<WorldPoint> getPlayerBoatPerimeter()
    {
        return Static.invoke(() -> {
            WorldEntity boat = getPlayerBoat();
            if (boat == null) {
                return Collections.emptyList();
            }
            return getBoatPerimeterInMainWorld(boat);
        });
    }

    /**
     * Gets outer perimeter tiles (OBJECT collision only) in main world
     * @param boat the WorldEntity (boat)
     * @return collection of WorldPoints of outer hull perimeter
     */
    public static Collection<WorldPoint> getBoatPerimeterInMainWorld(WorldEntity boat)
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

            List<WorldPoint> perimeterTiles = new ArrayList<>();
            Client client = Static.getClient();
            int plane = boatView.getPlane();

            int sizeX = boatView.getSizeX();
            int sizeY = boatView.getSizeY();

            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    if (plane >= collisionMaps.length || collisionMaps[plane] == null) {
                        continue;
                    }

                    int[][] flagArray = collisionMaps[plane].getFlags();
                    if (x + 1 >= flagArray.length || y + 1 >= flagArray[0].length) {
                        continue;
                    }

                    int flag = flagArray[x + 1][y + 1];

                    // Only OBJECT collision (outer perimeter only, not FULL/FLOOR)
                    if (flag == 0x00FFFFFF || !hasObjectCollision(collisionMaps, plane, x + 1, y + 1)) {
                        continue;
                    }

                    LocalPoint boatLocal = LocalPoint.fromScene(x, y, boatView);
                    LocalPoint mainWorldLocal = boat.transformToMainWorld(boatLocal);

                    if (mainWorldLocal != null) {
                        WorldPoint mainWorldPoint = WorldPoint.fromLocal(client, mainWorldLocal);
                        perimeterTiles.add(mainWorldPoint);
                    }
                }
            }

            return perimeterTiles;
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
     * Deck = tiles enclosed/surrounded by hull collision, not outside tiles
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

            Client client = Static.getClient();
            int plane = boatView.getPlane();
            int sizeX = boatView.getSizeX();
            int sizeY = boatView.getSizeY();

            if (plane >= collisionMaps.length || collisionMaps[plane] == null) {
                return Collections.emptyList();
            }

            int[][] flagArray = collisionMaps[plane].getFlags();

            // Step 1: Build barrier tile set (tiles with ANY collision - these block flood-fill)
            Set<String> barrierTiles = new HashSet<>();
            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    if (x + 1 >= flagArray.length || y + 1 >= flagArray[0].length) {
                        continue;
                    }
                    int flag = flagArray[x + 1][y + 1];
                    if (flag == 0x00FFFFFF) {
                        continue;
                    }
                    // ANY collision (OBJECT, FULL, or FLOOR) acts as barrier for flood-fill
                    if (hasCollision(collisionMaps, plane, x + 1, y + 1)) {
                        barrierTiles.add(x + "," + y);
                    }
                }
            }

            // Step 2: Flood-fill from edges to find "outside" tiles
            // Only spread through tiles with NO collision
            Set<String> outsideTiles = new HashSet<>();
            Set<String> visited = new HashSet<>();
            Queue<int[]> queue = new java.util.LinkedList<>();

            // Start flood-fill from all edge tiles that are NOT barriers
            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    // Only process edge tiles
                    if (x == 0 || y == 0 || x == sizeX - 1 || y == sizeY - 1) {
                        String key = x + "," + y;
                        if (!barrierTiles.contains(key) && !visited.contains(key)) {
                            queue.offer(new int[]{x, y});
                            visited.add(key);
                        }
                    }
                }
            }

            // Flood-fill to mark all outside tiles (spread only through non-barriers)
            while (!queue.isEmpty()) {
                int[] pos = queue.poll();
                int x = pos[0];
                int y = pos[1];
                String key = x + "," + y;
                outsideTiles.add(key);

                // Check 4 adjacent tiles
                int[][] neighbors = {{x-1, y}, {x+1, y}, {x, y-1}, {x, y+1}};
                for (int[] neighbor : neighbors) {
                    int nx = neighbor[0];
                    int ny = neighbor[1];

                    if (nx < 0 || ny < 0 || nx >= sizeX || ny >= sizeY) {
                        continue;
                    }

                    String nkey = nx + "," + ny;
                    // Only spread through non-barrier tiles
                    if (!barrierTiles.contains(nkey) && !visited.contains(nkey)) {
                        queue.offer(new int[]{nx, ny});
                        visited.add(nkey);
                    }
                }
            }

            // Step 3: Deck = tiles NOT outside AND NOT hull/structure
            // Deck = only empty interior tiles enclosed by hull
            List<WorldPoint> deckTiles = new ArrayList<>();
            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    String key = x + "," + y;

                    // Skip outside tiles
                    if (outsideTiles.contains(key)) {
                        continue;
                    }

                    // Skip ALL collision tiles (those are hull/structure, rendered separately as orange)
                    if (x + 1 < flagArray.length && y + 1 < flagArray[0].length) {
                        if (hasCollision(collisionMaps, plane, x + 1, y + 1)) {
                            continue;
                        }
                    }

                    // This tile is empty and enclosed by hull = walkable deck
                    LocalPoint boatLocal = LocalPoint.fromScene(x, y, boatView);
                    LocalPoint mainWorldLocal = boat.transformToMainWorld(boatLocal);

                    if (mainWorldLocal != null) {
                        WorldPoint mainWorldPoint = WorldPoint.fromLocal(client, mainWorldLocal);
                        deckTiles.add(mainWorldPoint);
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
     * Gets the boat's hull projected at a specific position with a specific heading.
     * @param boat the WorldEntity (boat)
     * @param targetPoint the WorldPoint in main world to center the boat at
     * @param targetHeading the desired heading/orientation
     * @return collection of WorldPoints where the boat hull would be, or empty if invalid
     */
    public static Collection<WorldPoint> getBoatHullAtPointWithHeading(WorldEntity boat, WorldPoint targetPoint, Heading targetHeading)
    {
        return Static.invoke(() -> {
            if (boat == null || targetPoint == null || targetHeading == null) {
                return Collections.emptyList();
            }

            // Get boat's current position and heading
            LocalPoint currentBoatLocal = boat.getLocalLocation();
            if (currentBoatLocal == null) {
                return Collections.emptyList();
            }

            Client client = Static.getClient();
            WorldPoint currentBoatPos = WorldPoint.fromLocal(client, currentBoatLocal);

            // Get current boat collision footprint
            Collection<WorldPoint> currentCollision = getBoatCollisionInMainWorld(boat);
            if (currentCollision.isEmpty()) {
                return Collections.emptyList();
            }

            // Get current and target heading values
            int currentHeadingValue = SailingAPI.getHeadingValue();
            if (currentHeadingValue == -1) {
                return Collections.emptyList(); // Not on boat
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

            // Calculate projected hull
            List<WorldPoint> projectedHull = new ArrayList<>();
            for (WorldPoint collisionTile : currentCollision) {
                // Calculate offset from current boat center
                int dx = collisionTile.getX() - currentBoatPos.getX();
                int dy = collisionTile.getY() - currentBoatPos.getY();

                // Rotate the offset around the center
                int rotatedDx = (int) Math.round(dx * cos - dy * sin);
                int rotatedDy = (int) Math.round(dx * sin + dy * cos);

                // Apply rotated offset to target position (plane 0)
                int worldX = targetPoint.getX() + rotatedDx;
                int worldY = targetPoint.getY() + rotatedDy;

                projectedHull.add(new WorldPoint(worldX, worldY, 0));
            }

            return projectedHull;
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
                System.out.println("BoatCollisionAPI.canBoatFitAtPoint: Boat collision footprint is empty!");
                return false;
            }

            // Get current and target heading values
            int currentHeadingValue = SailingAPI.getHeadingValue();
            if (currentHeadingValue == -1) {
                System.out.println("BoatCollisionAPI.canBoatFitAtPoint: Not on boat (headingValue=-1)");
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

            // Get global collision map
            CollisionMap collisionMap = Walker.getCollisionMap();
            if (collisionMap == null) {
                System.out.println("BoatCollisionAPI.canBoatFitAtPoint: GlobalCollisionMap is null!");
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

                // Apply rotated offset to target position (ONLY plane 0)
                int worldX = targetPoint.getX() + rotatedDx;
                int worldY = targetPoint.getY() + rotatedDy;

                // Check collision ONLY on plane 0 (boat sails at water level)
                // GlobalCollisionMap.walkable() returns true if walkable (no collision)
                if (!collisionMap.walkable((short) worldX, (short) worldY, (byte) 0)) {
                    // Collision detected - boat hull would hit land/rocks
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
     * Checks if a tile has deck collision (walkable boat deck)
     * Deck tiles have FULL or FLOOR collision flags but not OBJECT flags
     * @param collisionMaps collision data array
     * @param plane the plane
     * @param sceneX scene X coordinate
     * @param sceneY scene Y coordinate
     * @return true if the tile is walkable boat deck
     */
    private static boolean hasDeckCollision(CollisionData[] collisionMaps, int plane, int sceneX, int sceneY)
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

            // Deck = FULL or FLOOR collision (walkable boat deck)
            boolean hasFull = (flag & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0;
            boolean hasFloor = (flag & CollisionDataFlag.BLOCK_MOVEMENT_FLOOR) != 0;

            return hasFull || hasFloor;
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

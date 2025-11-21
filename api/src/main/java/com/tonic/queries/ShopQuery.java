package com.tonic.queries;

import com.tonic.data.locatables.NpcLocations;
import com.tonic.data.trading.Shop;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.queries.abstractions.AbstractQuery;
import com.tonic.services.pathfinder.model.WalkerPath;
import com.tonic.util.Distance;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.util.WildcardMatcher;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Query class for searching and filtering shops.
 * Provides a fluent API for chaining filter and sort operations.
 */
public class ShopQuery extends AbstractQuery<Shop, ShopQuery> {

    /**
     * Create a new ShopQuery with all shops.
     */
    public ShopQuery() {
        super(Arrays.asList(Shop.values()));
    }

    // ========== FILTER OPERATIONS ==========

    /**
     * Filter shops by their inventory IDs.
     * @param ids inventory IDs to filter by
     * @return this ShopQuery for method chaining
     */
    public ShopQuery withInventoryId(int... ids) {
        if (ids.length == 0) return this;
        if (ids.length == 1) {
            int targetId = ids[0];
            return keepIf(shop -> shop.getInventoryId() == targetId);
        }
        Set<Integer> idSet = Arrays.stream(ids).boxed().collect(Collectors.toSet());
        return keepIf(shop -> idSet.contains(shop.getInventoryId()));
    }

    /**
     * Filter shops by shopkeeper NPC.
     * @param shopkeepers NPC locations to filter by
     * @return this ShopQuery for method chaining
     */
    public ShopQuery withShopkeeper(NpcLocations... shopkeepers) {
        if (shopkeepers.length == 0) return this;
        if (shopkeepers.length == 1) {
            NpcLocations target = shopkeepers[0];
            return keepIf(shop -> target.equals(shop.getShopkeeper()));
        }
        Set<NpcLocations> shopkeeperSet = Set.of(shopkeepers);
        return keepIf(shop -> shopkeeperSet.contains(shop.getShopkeeper()));
    }

    /**
     * Filter shops by shopkeeper name containing substring (case-insensitive).
     * Useful for finding all shops with shopkeepers like "BARTENDER", "TRADER", etc.
     * @param namePart substring to search for in shopkeeper enum name
     * @return this ShopQuery for method chaining
     */
    public ShopQuery withShopkeeperNameContains(String namePart) {
        return keepIf(shop -> {
            NpcLocations shopkeeper = shop.getShopkeeper();
            return shopkeeper != null &&
                    shopkeeper.name().toLowerCase().contains(namePart.toLowerCase());
        });
    }

    /**
     * Filter shops by shopkeeper name matching a wildcard pattern (case-insensitive).
     * Supports '*' (any characters) and '?' (single character).
     * @param pattern wildcard pattern to match against shopkeeper enum name
     * @return this ShopQuery for method chaining
     */
    public ShopQuery withShopkeeperNameMatches(String pattern) {
        return keepIf(shop -> {
            NpcLocations shopkeeper = shop.getShopkeeper();
            return shopkeeper != null &&
                    WildcardMatcher.matches(pattern.toLowerCase(),
                            shopkeeper.name().toLowerCase());
        });
    }

    /**
     * Filter shops by the exact enum name (case-insensitive).
     * @param name exact shop enum name
     * @return this ShopQuery for method chaining
     */
    public ShopQuery withName(String name) {
        return keepIf(shop -> shop.name().equalsIgnoreCase(name));
    }

    /**
     * Filter shops whose enum name contains the specified substring (case-insensitive).
     * @param namePart substring to search for in shop name
     * @return this ShopQuery for method chaining
     */
    public ShopQuery withNameContains(String namePart) {
        return keepIf(shop -> shop.name().toLowerCase().contains(namePart.toLowerCase()));
    }

    /**
     * Filter shops by multiple exact enum names (case-insensitive).
     * @param names shop enum names to filter by
     * @return this ShopQuery for method chaining
     */
    public ShopQuery withNames(String... names) {
        return keepIf(shop -> Arrays.stream(names)
                .anyMatch(name -> shop.name().equalsIgnoreCase(name)));
    }

    /**
     * Filter shops whose enum name matches a wildcard pattern (case-insensitive).
     * Supports '*' (any characters) and '?' (single character).
     * @param pattern wildcard pattern
     * @return this ShopQuery for method chaining
     */
    public ShopQuery withNameMatches(String pattern) {
        return keepIf(shop -> WildcardMatcher.matches(
                pattern.toLowerCase(),
                shop.name().toLowerCase()
        ));
    }

    /**
     * Filter to only shops the player can currently access.
     * @return this ShopQuery for method chaining
     */
    public ShopQuery canAccess() {
        return keepIf(Shop::canAccess);
    }

    /**
     * Filter to shops with specific requirements.
     * @return this ShopQuery for method chaining
     */
    public ShopQuery withRequirements() {
        return keepIf(shop -> shop.getRequirements() != null);
    }

    /**
     * Filter to shops without requirements.
     * @return this ShopQuery for method chaining
     */
    public ShopQuery withoutRequirements() {
        return keepIf(shop -> shop.getRequirements() == null);
    }

    // ========== LOCATION FILTERS ==========

    /**
     * Filter to shops within a certain distance from the player.
     * @param distance maximum distance in tiles
     * @return this ShopQuery for method chaining
     */
    public ShopQuery within(int distance) {
        return keepIf(shop -> {
            WorldPoint location = shop.getLocation();
            if (location == null) return false;

            WorldPoint playerLocation = getPlayerLocation();
            if (playerLocation == null) return false;
            return Distance.euclidean(playerLocation, location) <= distance;
        });
    }

    /**
     * Filter to shops within a certain distance from a specific point.
     * @param center center point
     * @param distance maximum distance in tiles
     * @return this ShopQuery for method chaining
     */
    public ShopQuery within(WorldPoint center, int distance) {
        return keepIf(shop -> {
            WorldPoint location = shop.getLocation();
            if (location == null) return false;

            return Distance.euclidean(center, location) <= distance;
        });
    }

    /**
     * Filter to shops at a specific location.
     * @param location world point to match
     * @return this ShopQuery for method chaining
     */
    public ShopQuery atLocation(WorldPoint location) {
        return keepIf(shop -> location.equals(shop.getLocation()));
    }

    // ========== SORT OPERATIONS - EUCLIDEAN DISTANCE ==========

    /**
     * Sort shops by Euclidean distance from player (nearest first).
     * @return this ShopQuery for method chaining
     */
    public ShopQuery sortNearest() {
        WorldPoint playerLocation = getPlayerLocation();
        return playerLocation != null ? sortNearest(playerLocation) : this;
    }

    /**
     * Sort shops by Euclidean distance from a specific point (nearest first).
     * @param center center point
     * @return this ShopQuery for method chaining
     */
    public ShopQuery sortNearest(WorldPoint center) {
        return sort(byDistance(center, Distance::euclidean, false));
    }

    /**
     * Sort shops by Euclidean distance from the player (furthest first).
     * @return this ShopQuery for method chaining
     */
    public ShopQuery sortFurthest() {
        WorldPoint playerLocation = getPlayerLocation();
        return playerLocation != null ? sortFurthest(playerLocation) : this;
    }

    /**
     * Sort shops by Euclidean distance from a specific point (furthest first).
     * @param center center point
     * @return this ShopQuery for method chaining
     */
    public ShopQuery sortFurthest(WorldPoint center) {
        return sort(byDistance(center, Distance::euclidean, true));
    }

    // ========== SORT OPERATIONS - PATH DISTANCE ==========

    /**
     * Sort shops by pathfinding distance from player (nearest first).
     * Uses Distance.pathDistanceTo() for accurate walking distance.
     * @return this ShopQuery for method chaining
     */
    public ShopQuery sortShortestPath() {
        WorldPoint playerLocation = getPlayerLocation();
        return playerLocation != null ? sortShortestPath(playerLocation) : this;
    }

    /**
     * Sort shops by pathfinding distance from a specific point (nearest first).
     * @param center center point
     * @return this ShopQuery for method chaining
     */
    public ShopQuery sortShortestPath(WorldPoint center) {
        return sort(byDistance(center, Distance::pathDistanceTo, false));
    }

    /**
     * Sort shops by pathfinding distance from player (furthest first).
     * @return this ShopQuery for method chaining
     */
    public ShopQuery sortLongestPath() {
        WorldPoint playerLocation = getPlayerLocation();
        return playerLocation != null ? sortLongestPath(playerLocation) : this;
    }

    /**
     * Sort shops by pathfinding distance from a specific point (furthest first).
     * @param center center point
     * @return this ShopQuery for method chaining
     */
    public ShopQuery sortLongestPath(WorldPoint center) {
        return sort(byDistance(center, Distance::pathDistanceTo, true));
    }

    // ========== SORT OPERATIONS - GLOBAL PATH ==========

    /**
     * Sort shops by global pathfinding distance from player (nearest first).
     * Uses a Walker pathfinding system which accounts for transports and teleports.
     * @return this ShopQuery for method chaining
     */
    public ShopQuery sortShortestGlobalPath() {
        return sort(byGlobalPathLength(false));
    }

    /**
     * Sort shops by global pathfinding distance from player (furthest first).
     * Uses a Walker pathfinding system which accounts for transports and teleports.
     * @return this ShopQuery for method chaining
     */
    public ShopQuery sortLongestGlobalPath() {
        return sort(byGlobalPathLength(true));
    }

    // ========== TERMINAL OPERATIONS ==========

    /**
     * Get the nearest shop from the filtered list.
     * Terminal operation - executes the query.
     * @return nearest Shop or null if none found
     */
    public Shop nearest() {
        return sortNearest().first();
    }

    /**
     * Get the nearest shop to a specific point.
     * Terminal operation - executes the query.
     * @param center center point
     * @return nearest Shop or null if none found
     */
    public Shop nearest(WorldPoint center) {
        return sortNearest(center).first();
    }

    /**
     * Get the furthest shop from the filtered list.
     * Terminal operation - executes the query.
     * @return furthest Shop or null if none found
     */
    public Shop furthest() {
        return sortFurthest().first();
    }

    /**
     * Get the furthest shop from a specific point.
     * Terminal operation - executes the query.
     * @param center center point
     * @return furthest Shop or null if none found
     */
    public Shop furthest(WorldPoint center) {
        return sortFurthest(center).first();
    }

    /**
     * Get the shop with the shortest walking path from the filtered list.
     * Terminal operation - executes the query.
     * @return Shop with the shortest path or null if none found
     */
    public Shop shortestPath() {
        return sortShortestPath().first();
    }

    /**
     * Get the shop with the longest walking path from the filtered list.
     * Terminal operation - executes the query.
     * @return Shop with the longest path or null if none found
     */
    public Shop longestPath() {
        return sortLongestPath().first();
    }

    /**
     * Get the shop with the shortest global path from the filtered list.
     * Terminal operation - executes the query.
     * @return Shop with the shortest global path or null if none found
     */
    public Shop shortestGlobalPath() {
        return sortShortestGlobalPath().first();
    }

    /**
     * Get the shop with the longest global path from the filtered list.
     * Terminal operation - executes the query.
     * @return Shop with the longest global path or null if none found
     */
    public Shop longestGlobalPath() {
        return sortLongestGlobalPath().first();
    }

    /**
     * Get the currently open shop.
     * Terminal operation - does NOT use filtered results, always returns current open shop.
     * @return currently open Shop or null if no shop is open
     */
    public Shop getCurrent() {
        return Shop.getCurrent();
    }

    // ========== HELPER METHODS ==========

    /**
     * Get the player's current world location.
     * @return player's WorldPoint or null if unavailable
     */
    private WorldPoint getPlayerLocation() {
        return PlayerEx.getLocal() != null ? PlayerEx.getLocal().getWorldPoint() : null;
    }

    /**
     * Creates a comparator that sorts shops by distance.
     * Shops with null locations are sorted last.
     * @param center the center point to measure distance from.
     * @param distanceFunction function to calculate distance between two points (e.g., Distance::euclidean).
     * @param reversed if true, sort furthest first; if false, sort nearest first.
     * @return comparator for sorting shops.
     */
    private Comparator<Shop> byDistance(WorldPoint center,
                                        BiFunction<WorldPoint, WorldPoint, Integer> distanceFunction,
                                        boolean reversed) {
        Comparator<WorldPoint> distanceComparator = Comparator.comparingInt(
                location -> distanceFunction.apply(center, location));

        if (reversed) {
            distanceComparator = distanceComparator.reversed();
        }

        // the behavior of nullsLast is preserved regardless of reversal
        return Comparator.comparing(
                Shop::getLocation,
                Comparator.nullsLast(distanceComparator)
        );
    }

    /**
     * Create a comparator that sorts shops by global path length.
     * Shops with null locations or paths are sorted last.
     * @param reversed if true, sort longest first; if false, sort shortest first
     * @return comparator for sorting shops
     */
    private Comparator<Shop> byGlobalPathLength(boolean reversed) {
        Comparator<WorldPoint> lengthComparator = Comparator.comparingInt(this::getGlobalPathLength);

        if (reversed) {
            lengthComparator = lengthComparator.reversed();
        }

        // the behavior of nullsLast is preserved regardless of reversal
        return Comparator.comparing(
                Shop::getLocation,
                Comparator.nullsLast(lengthComparator)
        );
    }

    /**
     * Get the global path length to a location.
     * @param location target location
     * @return number of steps in a path, or Integer.MAX_VALUE if no path exists
     */
    private int getGlobalPathLength(WorldPoint location) {
        WalkerPath path = WalkerPath.get(location);
        return path.getSteps().isEmpty() ? Integer.MAX_VALUE : path.getSteps().size();
    }
}
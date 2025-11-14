package com.tonic.queries.combined;

import com.tonic.api.game.SceneAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.data.wrappers.abstractions.Entity;
import com.tonic.queries.abstractions.AbstractQuery;
import com.tonic.util.TextUtil;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;
import org.apache.commons.lang3.ArrayUtils;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tonic.services.GameManager.*;

/**
 * A query class to filter and sort locatable, identifiable and interactable entities in the game world.
 */
public class EntityQuery extends AbstractQuery<Entity, EntityQuery> {
    public EntityQuery() {
        super(Stream.of(npcList(), playerList(), tileItemList(), objectList())
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));
    }

    /**
     * Filters the query results to only include locatable interactables of the specified types.
     * @param types The classes of locatable interactables to include.
     * @return LocatableInteractableQuery
     */
    @SafeVarargs
    public final EntityQuery ofTypes(Class<? extends Entity>... types) {
        return removeIf(locint -> {
            for (Class<? extends Entity> type : types) {
                if (type.isAssignableFrom(locint.getClass())) {
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * Filters the query results to exclude players.
     * @return EntityQuery
     */
    public EntityQuery removePlayers()
    {
        return removeIf(locint -> locint instanceof PlayerEx);
    }

    /**
     * Filters the query results to exclude NPCs.
     * @return EntityQuery
     */
    public EntityQuery removeNpcs()
    {
        return removeIf(locint -> locint instanceof NpcEx);
    }

    /**
     * Filters the query results to exclude tile items.
     * @return EntityQuery
     */
    public EntityQuery removeTileItems()
    {
        return removeIf(locint -> locint instanceof TileItemEx);
    }

    /**
     * Filters the query results to exclude tile objects.
     * @return EntityQuery
     */
    public EntityQuery removeTileObjects()
    {
        return removeIf(locint -> locint instanceof TileObjectEx);
    }

    /**
     * Filters the query to only include entities with the specified IDs.
     * @param id The IDs to filter by.
     * @return EntityQuery
     */
    public EntityQuery withId(int... id)
    {
        return removeIf(o -> !ArrayUtils.contains(id, o.getId()));
    }

    /**
     * Filters the query to only include entities with the specified name.
     * @param name The name to filter by.
     * @return EntityQuery
     */
    public EntityQuery withName(String name)
    {
        return keepIf(o -> o.getName() != null && o.getName().equalsIgnoreCase(name));
    }

    /**
     * Filters the query to only include entities with names that contain the specified string.
     * @param name The string to filter by.
     * @return EntityQuery
     */
    public EntityQuery withNameContains(String name)
    {
        return keepIf(o -> o.getName() != null && o.getName().toLowerCase().contains(name.toLowerCase()));
    }

    /**
     * Filters the query to only include entities with the specified names.
     * @param names The names to filter by.
     * @return EntityQuery
     */
    public EntityQuery withNames(String... names)
    {
        return keepIf(o -> o.getName() != null && ArrayUtils.contains(names, o.getName()));
    }

    /**
     * Filters the query to only include entities with names that contain any of the specified strings.
     * @param names The strings to filter by.
     * @return EntityQuery
     */
    public EntityQuery withNamesContains(String... names)
    {
        return keepIf(o -> o.getName() != null && TextUtil.containsIgnoreCase(o.getName(), names));
    }

    /**
     * Filters the query to only include entities with names that match the specified wildcard pattern.
     * @param namePart The wildcard pattern to filter by.
     * @return EntityQuery
     */
    public EntityQuery withNameMatches(String namePart)
    {
        return keepIf(o -> o.getName() != null && WildcardMatcher.matches(namePart.toLowerCase(), Text.removeTags(o.getName().toLowerCase())));
    }

    /**
     * Filters the query results to only include entities with the specified action.
     * @param action The action to filter by.
     * @return EntityQuery
     */
    public EntityQuery withAction(String action)
    {
        return keepIf(locint -> locint.getActions() != null && TextUtil.containsIgnoreCaseInverse(action, locint.getActions()));
    }

    /**
     * Filters the query results to only include entities with an action that partially matches the specified string.
     * @param partial The partial action string to filter by.
     * @return EntityQuery
     */
    public EntityQuery withPartialAction(String partial) {
        return keepIf(locint -> locint.getActions() != null && TextUtil.containsIgnoreCaseInverse(partial, locint.getActions()));
    }

    /**
     * Filters the query results to only include entities beyond the specified distance from the local player.
     * @param distance The distance threshold.
     * @return EntityQuery
     */
    public EntityQuery beyondDistance(int distance) {
        return removeIf(locint -> locint.getWorldPoint().distanceTo(PlayerEx.getLocal().getWorldPoint()) <= distance);
    }

    /**
     * Filters the query results to only include entities within the specified distance from the local player.
     * @param distance The distance threshold.
     * @return EntityQuery
     */
    public EntityQuery withinDistance(int distance) {
        return removeIf(locint -> locint.getWorldPoint().distanceTo(PlayerEx.getLocal().getWorldPoint()) > distance);
    }

    /**
     * Sorts the query results by distance from the local player, nearest first.
     * @return EntityQuery
     */
    public EntityQuery sortNearest()
    {
        return sortNearest(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * Sorts the query results by distance from the specified center point, nearest first.
     * @param center The center point to measure distance from.
     * @return EntityQuery
     */
    public EntityQuery sortNearest(WorldPoint center)
    {
        Point2D point = new Point2D.Double(center.getX(), center.getY());
        return sort((o1, o2) -> {
            Point2D p0 = new Point2D.Double(o1.getWorldPoint().getX(), o1.getWorldPoint().getY());
            Point2D p1 = new Point2D.Double(o2.getWorldPoint().getX(), o2.getWorldPoint().getY());
            return Double.compare(point.distance(p0), point.distance(p1));
        });
    }

    /**
     * Sorts the query results by distance from the local player, furthest first.
     * @return EntityQuery
     */
    public EntityQuery sortFurthest()
    {
        return sortFurthest(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * Sorts the query results by distance from the specified center point, furthest first.
     * @param center The center point to measure distance from.
     * @return EntityQuery
     */
    public EntityQuery sortFurthest(WorldPoint center)
    {
        Point2D point = new Point2D.Double(center.getX(), center.getY());
        return sort((o1, o2) -> {
            Point2D p0 = new Point2D.Double(o1.getWorldPoint().getX(), o1.getWorldPoint().getY());
            Point2D p1 = new Point2D.Double(o2.getWorldPoint().getX(), o2.getWorldPoint().getY());
            return Double.compare(point.distance(p1), point.distance(p0));
        });
    }

    /**
     * sort by shortest path from the player
     * @return EntityQuery
     */
    public EntityQuery sortShortestPath()
    {
        return sortShortestPath(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * sort by shortest path from a specific point
     * @param center center point
     * @return EntityQuery
     */
    public EntityQuery sortShortestPath(WorldPoint center)
    {
        return sort((o1, o2) -> {
            List<WorldPoint> path1 = SceneAPI.pathTo(center, o1.getWorldPoint());
            List<WorldPoint> path2 = SceneAPI.pathTo(center, o2.getWorldPoint());
            int len1 = path1 == null ? Integer.MAX_VALUE : path1.size();
            int len2 = path2 == null ? Integer.MAX_VALUE : path2.size();
            return Integer.compare(len1, len2);
        });
    }

    /**
     * sort by longest path from the player
     * @return EntityQuery
     */
    public EntityQuery sortLongestPath()
    {
        return sortLongestPath(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * sort by longest path from a specific point
     * @param center center point
     * @return EntityQuery
     */
    public EntityQuery sortLongestPath(WorldPoint center)
    {
        return sort((o1, o2) -> {
            List<WorldPoint> path1 = SceneAPI.pathTo(center, o1.getWorldPoint());
            List<WorldPoint> path2 = SceneAPI.pathTo(center, o2.getWorldPoint());
            int len1 = path1 == null ? Integer.MAX_VALUE : path1.size();
            int len2 = path2 == null ? Integer.MAX_VALUE : path2.size();
            return Integer.compare(len2, len1);
        });
    }

    /**
     * Get the nearest entity from the filtered list
     * Terminal operation - executes the query
     */
    public Entity nearest() {
        // Apply filters and sort by distance, then get first
        return this.sortNearest().first();
    }

    /**
     * Get the nearest entity to a specific point
     * Terminal operation - executes the query
     */
    public Entity nearest(WorldPoint center) {
        return this.sortNearest(center).first();
    }

    /**
     * Get the farthest entity from the filtered list
     * Terminal operation - executes the query
     */
    public Entity farthest() {
        return this.sortFurthest().first();
    }

    /**
     * Get the farthest entity from a specific point
     * Terminal operation - executes the query
     */
    public Entity farthest(WorldPoint center) {
        return this.sortFurthest(center).first();
    }

    /**
     * Get the entity with the shortest path from the filtered list
     * Terminal operation - executes the query
     */
    public Entity shortestPath() {
        return this.sortShortestPath().first();
    }

    /**
     * Get the entity with the longest path from the filtered list
     * Terminal operation - executes the query
     */
    public Entity longestPath() {
        return this.sortLongestPath().first();
    }
}

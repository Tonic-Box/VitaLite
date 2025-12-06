package com.tonic.queries;

import com.tonic.api.game.SceneAPI;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.queries.abstractions.AbstractQuery;
import com.tonic.services.GameManager;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.util.Distance;
import com.tonic.util.Location;
import com.tonic.util.TextUtil;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;
import org.apache.commons.lang3.ArrayUtils;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A query to find {@link TileObjectEx}'s in the game world.
 */
public class TileObjectQuery extends AbstractQuery<TileObjectEx, TileObjectQuery>
{
    /**
     * Creates a new TileObjectQuery that queries all TileObjectEx's in the game world.
     */
    public TileObjectQuery()
    {
        super(GameManager.objectList());
    }

    public TileObjectQuery fromWorldView()
    {
        return keepIf(o -> o.getWorldViewId() == PlayerEx.getLocal().getWorldViewId());
    }

    /**
     * Filters the query to only include objects with the specified IDs.
     * @param id The IDs to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery withId(int... id)
    {
        return removeIf(o -> !ArrayUtils.contains(id, o.getId()));
    }

    /**
     * Filters the query to only include objects with the specified name.
     * @param name The name to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery withName(String name)
    {
        return keepIf(o -> o.getName() != null && o.getName().equalsIgnoreCase(name));
    }

    /**
     * Filters the query to only include objects with names that contain the specified string.
     * @param name The string to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery withNameContains(String name)
    {
        return keepIf(o -> o.getName() != null && o.getName().toLowerCase().contains(name.toLowerCase()));
    }

    /**
     * Filters the query to only include objects with the specified names.
     * @param names The names to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery withNames(String... names)
    {
        return keepIf(o -> o.getName() != null && ArrayUtils.contains(names, o.getName()));
    }

    /**
     * Filters the query to only include objects with names that contain any of the specified strings.
     * @param names The strings to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery withNamesContains(String... names)
    {
        return keepIf(o -> o.getName() != null && TextUtil.containsIgnoreCase(o.getName(), names));
    }

    /**
     * Filters the query to only include objects with names that match the specified wildcard pattern.
     * @param namePart The wildcard pattern to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery withNameMatches(String namePart)
    {
        return keepIf(o -> o.getName() != null && WildcardMatcher.matches(namePart.toLowerCase(), Text.removeTags(o.getName().toLowerCase())));
    }

    /**
     * Filters the query to only include objects with the specified action.
     * @param action The action to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery withAction(String action)
    {
        return keepIf(o -> o.getActions() != null && TextUtil.containsIgnoreCaseInverse(action, o.getActions()));
    }

    /**
     * Filters the query to only include objects with any of the specified actions.
     * @param actions The actions to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery withAnyOfActions(String... actions)
    {
        return keepIf(o -> {
            if(o.getActions() == null)
                return false;
            for(String action : actions)
            {
                if(TextUtil.containsIgnoreCaseInverse(action, o.getActions()))
                    return true;
            }
            return false;
        });
    }

    /**
     * Filters the query to only include objects that have the specified op visible.
     * @param op The op index to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery withOpVisible(int op)
    {
        return keepIf(o -> o.isOpVisible(op));
    }

    /**
     * Filters the query to only include objects that have the specified action visible.
     * @param action The action to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery withActionVisible(String action)
    {
        return keepIf(o -> o.isActionVisible(action));
    }

    /**
     * Filters the query to only include objects within the specified distance from the local player.
     * @param distance The distance to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery within(int distance)
    {
        return within(PlayerEx.getLocal().getWorldPoint(), distance);
    }

    /**
     * Filters the query to only include objects within the specified distance from the specified center point.
     * @param center The center point to measure distance from.
     * @param distance The distance to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery within(WorldPoint center, int distance)
    {
        return keepIf(o -> Distance.chebyshev(center, o.getWorldPoint()) <= distance);
    }

    /**
     * Filters the query to only include objects that have interactable tiles that are reachable
     * by the player.
     * @return TileObjectQuery
     */
    public TileObjectQuery isReachable()
    {
        return keepIf(TileObjectEx::isReachable);
    }

    /**
     * Filters the query to only include objects at the specified location.
     * @param location The location to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery atLocation(WorldPoint location)
    {
        return keepIf(o -> o.getWorldPoint().equals(location));
    }

    /**
     * Sorts the query results by distance from the local player, nearest first.
     * @return TileObjectQuery
     */
    public TileObjectQuery sortNearest()
    {
        return sortNearest(PlayerEx.getLocal().getWorldPoint());
    }

    /**
     * Sorts the query results by distance from the specified center point, nearest first.
     * @param center The center point to measure distance from.
     * @return TileObjectQuery
     */
    public TileObjectQuery sortNearest(WorldPoint center)
    {
        return sort((o1, o2) -> {
            int dist1 = Distance.chebyshev(center, o1.getWorldPoint());
            int dist2 = Distance.chebyshev(center, o2.getWorldPoint());
            return Integer.compare(dist1, dist2);
        });
    }

    /**
     * Sorts the query results by distance from the local player, furthest first.
     * @return TileObjectQuery
     */
    public TileObjectQuery sortFurthest()
    {
        return sortFurthest(PlayerEx.getLocal().getWorldPoint());
    }

    /**
     * Sorts the query results by distance from the specified center point, furthest first.
     * @param center The center point to measure distance from.
     * @return TileObjectQuery
     */
    public TileObjectQuery sortFurthest(WorldPoint center)
    {
        return sort((o1, o2) -> {
            int dist1 = Distance.chebyshev(center, o1.getWorldPoint());
            int dist2 = Distance.chebyshev(center, o2.getWorldPoint());
            return Integer.compare(dist2, dist1);
        });
    }

    /**
     * sort by shortest path from the player
     * @return TileObjectQuery
     */
    public TileObjectQuery sortShortestPath()
    {
        return sortShortestPath(PlayerEx.getLocal().getWorldPoint());
    }

    /**
     * sort by shortest path from a specific point
     * @param center center point
     * @return TileObjectQuery
     */
    public TileObjectQuery sortShortestPath(WorldPoint center)
    {
        return sort((o1, o2) -> {
            List<WorldPoint> path1 = SceneAPI.pathTo(center, o1.getInteractionPoint());
            List<WorldPoint> path2 = SceneAPI.pathTo(center, o2.getInteractionPoint());
            int len1 = path1 == null ? Integer.MAX_VALUE : path1.size();
            int len2 = path2 == null ? Integer.MAX_VALUE : path2.size();
            return Integer.compare(len1, len2);
        });
    }

    /**
     * sort by longest path from the player
     * @return TileObjectQuery
     */
    public TileObjectQuery sortLongestPath()
    {
        return sortLongestPath(PlayerEx.getLocal().getWorldPoint());
    }

    /**
     * sort by longest path from a specific point
     * @param center center point
     * @return TileObjectQuery
     */
    public TileObjectQuery sortLongestPath(WorldPoint center)
    {
        return sort((o1, o2) -> {
            List<WorldPoint> path1 = SceneAPI.pathTo(center, o1.getInteractionPoint());
            List<WorldPoint> path2 = SceneAPI.pathTo(center, o2.getInteractionPoint());
            int len1 = path1 == null ? Integer.MAX_VALUE : path1.size();
            int len2 = path2 == null ? Integer.MAX_VALUE : path2.size();
            return Integer.compare(len2, len1);
        });
    }

    /**
     * Filters the query to only include objects with actions that contain the specified string.
     * @param partial The string to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery withPartialAction(String partial) {
        return keepIf(o -> o.getActions() != null && TextUtil.containsIgnoreCaseInverse(partial, o.getActions()));
    }

    /**
     * Get the nearest object from the filtered list
     * Terminal operation - executes the query
     */
    public TileObjectEx nearest() {
        return this.sortNearest().first();
    }

    /**
     * Get the nearest object to a specific point
     * Terminal operation - executes the query
     */
    public TileObjectEx nearest(WorldPoint center) {
        return this.sortNearest(center).first();
    }

    /**
     * Get the farthest object from the filtered list
     * Terminal operation - executes the query
     */
    public TileObjectEx farthest() {
        return this.sortFurthest().first();
    }

    /**
     * Get the farthest object from a specific point
     * Terminal operation - executes the query
     */
    public TileObjectEx farthest(WorldPoint center) {
        return this.sortFurthest(center).first();
    }

    /**
     * Get the object with the shortest path from the filtered list
     * Terminal operation - executes the query
     */
    public TileObjectEx shortestPath() {
        return this.sortShortestPath().first();
    }

    /**
     * Get the object with the longest path from the filtered list
     * Terminal operation - executes the query
     */
    public TileObjectEx longestPath() {
        return this.sortLongestPath().first();
    }

    /**
     * Sorts the query results using a greedy nearest-neighbor approach based on shortest path.
     * Starting from the local player, selects the object with the shortest path, then from that
     * object's interaction point selects the next shortest path, and so on.
     * In case of path length ties, uses Chebyshev distance as a tiebreaker.
     * @return TileObjectQuery
     */
    public TileObjectQuery sortGreedyShortestPath()
    {
        return sortGreedyShortestPath(PlayerEx.getLocal().getWorldPoint());
    }

    /**
     * Sorts the query results using a greedy nearest-neighbor approach based on shortest path.
     * @param start The starting point for the greedy traversal.
     * @return TileObjectQuery
     */
    public TileObjectQuery sortGreedyShortestPath(WorldPoint start)
    {
        return postProcess(items -> {
            if (items.size() <= 1)
            {
                return items;
            }

            List<TileObjectEx> remaining = new ArrayList<>(items);
            List<TileObjectEx> ordered = new ArrayList<>(remaining.size());
            WorldPoint current = start;

            while (!remaining.isEmpty())
            {
                final WorldPoint pos = current;

                TileObjectEx nearest = Collections.min(remaining, (o1, o2) -> {
                    List<WorldPoint> path1 = SceneAPI.pathTo(pos, o1.getInteractionPoint());
                    List<WorldPoint> path2 = SceneAPI.pathTo(pos, o2.getInteractionPoint());
                    int len1 = path1 == null ? Integer.MAX_VALUE : path1.size();
                    int len2 = path2 == null ? Integer.MAX_VALUE : path2.size();

                    int cmp = Integer.compare(len1, len2);
                    if (cmp != 0)
                    {
                        return cmp;
                    }

                    return Integer.compare(
                            Distance.chebyshev(pos, o1.getWorldPoint()),
                            Distance.chebyshev(pos, o2.getWorldPoint())
                    );
                });

                ordered.add(nearest);
                remaining.remove(nearest);
                current = nearest.getInteractionPoint();
            }

            return ordered;
        });
    }
}

package com.tonic.queries;

import com.tonic.api.game.SceneAPI;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.queries.abstractions.AbstractQuery;
import com.tonic.services.GameManager;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.util.Distance;
import com.tonic.util.Location;
import com.tonic.util.TextUtil;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * A query builder for {@link TileItemEx} objects.
 */
public class TileItemQuery extends AbstractQuery<TileItemEx, TileItemQuery>
{
    /**
     * Creates a new TileItemQuery instance initialized with all tile items in the game.
     */
    public TileItemQuery() {
        super(GameManager.tileItemList());
    }

    /**
     * Filters the query to include only items with the specified IDs.
     * @param id the item IDs to filter by
     * @return TileItemQuery
     */
    public TileItemQuery withId(int... id)
    {
        return removeIf(o -> !ArrayUtils.contains(id, o.getId()));
    }

    /**
     * Filters the query to include only items with the specified canonical IDs.
     * @param id the canonical item IDs to filter by
     * @return TileItemQuery
     */
    public TileItemQuery withCanonicalId(int... id)
    {
        return removeIf(o -> !ArrayUtils.contains(id, o.getCanonicalId()));
    }

    /**
     * Filters the query to include only items with the specified name.
     * @param name the item name to filter by
     * @return TileItemQuery
     */
    public TileItemQuery withName(String name)
    {
        return keepIf(o -> o.getName() != null && o.getName().equalsIgnoreCase(name));
    }

    /**
     * Filters the query to include only items with names containing any of the specified substrings, ignoring case.
     * @param names the substrings to filter by
     * @return TileItemQuery
     */
    public TileItemQuery withNames(String... names)
    {
        return keepIf(o -> o.getName() != null && TextUtil.containsIgnoreCase(o.getName(), names));
    }

    /**
     * Filters to only items with a shop price greater than the specified value.
     * @param price the minimum shop price
     * @return TileItemQuery
     */
    public TileItemQuery greaterThanShopPrice(int price)
    {
        return removeIf(o -> (long) o.getQuantity() * o.getShopPrice() <= price);
    }

    /**
     * Filters to only items with a shop price less than the specified value.
     * @param price the maximum shop price
     * @return TileItemQuery
     */
    public TileItemQuery lessThanShopPrice(int price)
    {
        return removeIf(o -> (long) o.getQuantity() * o.getShopPrice() >= price);
    }

    /**
     * Filters to only items with a Grand Exchange price greater than the specified value.
     * @param price the minimum Grand Exchange price
     * @return TileItemQuery
     */
    public TileItemQuery greaterThanGePrice(int price)
    {
        return removeIf(o -> (long) o.getQuantity() * o.getGePrice() <= price);
    }

    /**
     * Filters to only items with a Grand Exchange price less than the specified value.
     * @param price the maximum Grand Exchange price
     * @return TileItemQuery
     */
    public TileItemQuery lessThanGePrice(int price)
    {
        return removeIf(o -> (long) o.getQuantity() * o.getGePrice() >= price);
    }

    /**
     * Filters to only items with a high alchemy value greater than the specified value.
     * @param value the minimum high alchemy value
     * @return TileItemQuery
     */
    public TileItemQuery greaterThanHighAlchValue(int value)
    {
        return removeIf(o -> (long) o.getQuantity() * o.getHighAlchValue() <= value);
    }

    /**
     * Filters to only items with a high alchemy value less than the specified value.
     * @param value the maximum high alchemy value
     * @return TileItemQuery
     */
    public TileItemQuery lessThanHighAlchValue(int value)
    {
        return removeIf(o -> (long) o.getQuantity() * o.getHighAlchValue() >= value);
    }

    /**
     * Filters to only items with a low alchemy value greater than the specified value.
     * @param value the minimum low alchemy value
     * @return TileItemQuery
     */
    public TileItemQuery greaterThanLowAlchValue(int value)
    {
        return removeIf(o -> (long) o.getQuantity() * o.getLowAlchValue() <= value);
    }

    /**
     * Filters to only items with a low alchemy value less than the specified value.
     * @param value the maximum low alchemy value
     * @return TileItemQuery
     */
    public TileItemQuery lessThanLowAlchValue(int value)
    {
        return removeIf(o -> (long) o.getQuantity() * o.getLowAlchValue() >= value);
    }

    /**
     * Filters to only items whose names contain the specified substring, ignoring case.
     * @param namePart the substring to filter by
     * @return TileItemQuery
     */
    public TileItemQuery withNameContains(String namePart)
    {
        return removeIf(o -> !o.getName().toLowerCase().contains(namePart.toLowerCase()));
    }

    /**
     * Filters to only items whose names match the specified wildcard pattern, ignoring case.
     * @param namePart the wildcard pattern to filter by
     * @return TileItemQuery
     */
    public TileItemQuery withNameMatches(String namePart)
    {
        return removeIf(o -> !WildcardMatcher.matches(namePart.toLowerCase(), Text.removeTags(o.getName().toLowerCase())));
    }

    /**
     * Filters to only items within the specified distance from the local player.
     * @param distance the maximum distance
     * @return TileItemQuery
     */
    public TileItemQuery within(int distance)
    {
        WorldPoint player = PlayerEx.getLocal().getWorldPoint();
        return keepIf(o -> Distance.chebyshev(player, o.getWorldPoint()) <= distance);
    }

    /**
     * Filters to only items within the specified distance from the given world point.
     * @param center the center world point
     * @param distance the maximum distance
     * @return TileItemQuery
     */
    public TileItemQuery within(WorldPoint center, int distance)
    {
        return keepIf(o -> Distance.chebyshev(center, o.getWorldPoint()) <= distance);
    }

    /**
     * Filters to only items located at the specified world point.
     * @param location the world point to filter by
     * @return TileItemQuery
     */
    public TileItemQuery atLocation(WorldPoint location)
    {
        return keepIf(o -> o.getWorldPoint().equals(location));
    }

    /**
     * Sorts the items by distance from the local player, nearest first.
     * @return TileItemQuery
     */
    public TileItemQuery sortNearest()
    {
        return sortNearest(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * Sorts the items by distance from the specified world point, nearest first.
     * @param center the center world point
     * @return TileItemQuery
     */
    public TileItemQuery sortNearest(WorldPoint center)
    {
        Point2D point = new Point2D.Double(center.getX(), center.getY());
        return sort((o1, o2) -> {
            Point2D p0 = new Point2D.Double(o1.getWorldPoint().getX(), o1.getWorldPoint().getY());
            Point2D p1 = new Point2D.Double(o2.getWorldPoint().getX(), o2.getWorldPoint().getY());
            return Double.compare(point.distance(p0), point.distance(p1));
        });
    }

    /**
     * Sorts the items by distance from the local player, furthest first.
     * @return TileItemQuery
     */
    public TileItemQuery sortFurthest()
    {
        return sortFurthest(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * Sorts the items by distance from the specified world point, furthest first.
     * @param center the center world point
     * @return TileItemQuery
     */
    public TileItemQuery sortFurthest(WorldPoint center)
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
     * @return TileItemQuery
     */
    public TileItemQuery sortShortestPath()
    {
        return sortShortestPath(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * sort by shortest path from a specific point
     * @param center center point
     * @return TileItemQuery
     */
    public TileItemQuery sortShortestPath(WorldPoint center)
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
     * sort by furthest path from the player
     * @return TileItemQuery
     */
    public TileItemQuery sortLongestPath()
    {
        return sortLongestPath(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * sort by furthest path from a specific point
     * @param center center point
     * @return TileItemQuery
     */
    public TileItemQuery sortLongestPath(WorldPoint center)
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
     * Get the nearest item from the filtered list
     * Terminal operation - executes the query
     */
    public TileItemEx nearest() {
        // Apply filters and sort by distance, then get first
        return this.sortNearest().first();
    }

    /**
     * Get the nearest item to a specific point
     * Terminal operation - executes the query
     */
    public TileItemEx nearest(WorldPoint center) {
        return this.sortNearest(center).first();
    }

    /**
     * Get the farthest item from the filtered list
     * Terminal operation - executes the query
     */
    public TileItemEx farthest() {
        return this.sortFurthest().first();
    }

    /**
     * Get the farthest item from a specific point
     * Terminal operation - executes the query
     */
    public TileItemEx farthest(WorldPoint center) {
        return this.sortFurthest(center).first();
    }

    /**
     * Get the item with the shortest path from the filtered list
     * Terminal operation - executes the query
     */
    public TileItemEx shortestPath() {
        return this.sortShortestPath().first();
    }

    /**
     * Get the item with the furthest path from the filtered list
     * Terminal operation - executes the query
     */
    public TileItemEx longestPath() {
        return this.sortLongestPath().first();
    }
}

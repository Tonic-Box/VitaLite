package com.tonic.queries;

import com.tonic.queries.abstractions.AbstractQuery;
import com.tonic.services.GameManager;
import com.tonic.data.TileObjectEx;
import com.tonic.util.Location;
import com.tonic.util.TextUtil;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.geom.Point2D;

/**
 * A query to find {@link TileObjectEx}'s in the game world.
 * @param <T> The type of TileObjectEx to query for (or ?)
 */
public class TileObjectQuery<T extends TileObjectEx> extends AbstractQuery<TileObjectEx, TileObjectQuery<T>>
{
    /**
     * Creates a new TileObjectQuery that queries all TileObjectEx's in the game world.
     */
    public TileObjectQuery()
    {
        super(GameManager.objectList());
    }

    /**
     * Filters the query to only include objects with the specified IDs.
     * @param id The IDs to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> withId(int... id)
    {
        return removeIf(o -> !ArrayUtils.contains(id, o.getId()));
    }

    /**
     * Filters the query to only include objects with the specified name.
     * @param name The name to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> withName(String name)
    {
        return keepIf(o -> o.getName() != null && o.getName().equalsIgnoreCase(name));
    }

    /**
     * Filters the query to only include objects with names that contain the specified string.
     * @param name The string to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> withNameContains(String name)
    {
        return keepIf(o -> o.getName() != null && o.getName().toLowerCase().contains(name.toLowerCase()));
    }

    /**
     * Filters the query to only include objects with the specified names.
     * @param names The names to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> withNames(String... names)
    {
        return keepIf(o -> o.getName() != null && ArrayUtils.contains(names, o.getName()));
    }

    /**
     * Filters the query to only include objects with names that contain any of the specified strings.
     * @param names The strings to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> withNamesContains(String... names)
    {
        return keepIf(o -> o.getName() != null && TextUtil.containsIgnoreCase(o.getName(), names));
    }

    /**
     * Filters the query to only include objects with names that match the specified wildcard pattern.
     * @param namePart The wildcard pattern to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> withNameMatches(String namePart)
    {
        return keepIf(o -> o.getName() != null && WildcardMatcher.matches(namePart.toLowerCase(), Text.removeTags(o.getName().toLowerCase())));
    }

    /**
     * Filters the query to only include objects with the specified action.
     * @param action The action to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> withAction(String action)
    {
        return keepIf(o -> o.getActions() != null && TextUtil.containsIgnoreCase(action, o.getActions()));
    }

    /**
     * Filters the query to only include objects within the specified distance from the local player.
     * @param distance The distance to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> within(int distance)
    {
        return keepIf(o -> Location.within(client.getLocalPlayer().getWorldLocation(), o.getWorldLocation(), distance));
    }

    /**
     * Filters the query to only include objects within the specified distance from the specified center point.
     * @param center The center point to measure distance from.
     * @param distance The distance to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> within(WorldPoint center, int distance)
    {
        return keepIf(o -> Location.within(center, o.getWorldLocation(), distance));
    }

    /**
     * Filters the query to only include objects at the specified location.
     * @param location The location to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> atLocation(WorldPoint location)
    {
        return keepIf(o -> o.getWorldLocation().equals(location));
    }

    /**
     * Sorts the query results by distance from the local player, nearest first.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> sortNearest()
    {
        return sortNearest(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * Sorts the query results by distance from the specified center point, nearest first.
     * @param center The center point to measure distance from.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> sortNearest(WorldPoint center)
    {
        Point2D point = new Point2D.Double(center.getX(), center.getY());
        return sort((o1, o2) -> {
            Point2D p0 = new Point2D.Double(o1.getWorldLocation().getX(), o1.getWorldLocation().getY());
            Point2D p1 = new Point2D.Double(o2.getWorldLocation().getX(), o2.getWorldLocation().getY());
            return Double.compare(point.distance(p0), point.distance(p1));
        });
    }

    /**
     * Sorts the query results by distance from the local player, furthest first.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> sortFurthest()
    {
        return sortFurthest(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * Sorts the query results by distance from the specified center point, furthest first.
     * @param center The center point to measure distance from.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> sortFurthest(WorldPoint center)
    {
        Point2D point = new Point2D.Double(center.getX(), center.getY());
        return sort((o1, o2) -> {
            Point2D p0 = new Point2D.Double(o1.getWorldLocation().getX(), o1.getWorldLocation().getY());
            Point2D p1 = new Point2D.Double(o2.getWorldLocation().getX(), o2.getWorldLocation().getY());
            return Double.compare(point.distance(p1), point.distance(p0));
        });
    }

    /**
     * Filters the query to only include objects with actions that contain the specified string.
     * @param partial The string to filter by.
     * @return TileObjectQuery
     */
    public TileObjectQuery<T> withPartialAction(String partial) {
        return keepIf(o -> o.getActions() != null && TextUtil.containsIgnoreCase(partial, o.getActions()));
    }
}

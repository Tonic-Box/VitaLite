package com.tonic.queries;

import com.tonic.queries.abstractions.AbstractQuery;
import com.tonic.services.GameCache;
import com.tonic.data.TileObjectEx;
import com.tonic.util.Location;
import com.tonic.util.TextUtil;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.geom.Point2D;

public class TileObjectQuery<T extends TileObjectEx> extends AbstractQuery<TileObjectEx, TileObjectQuery<T>>
{
    public TileObjectQuery()
    {
        super(GameCache.objectList());
    }

    public TileObjectQuery<T> withId(int... id)
    {
        return removeIf(o -> !ArrayUtils.contains(id, o.getId()));
    }

    public TileObjectQuery<T> withName(String name)
    {
        return keepIf(o -> o.getName() != null && o.getName().equalsIgnoreCase(name));
    }

    public TileObjectQuery<T> withNameContains(String name)
    {
        return keepIf(o -> o.getName() != null && o.getName().toLowerCase().contains(name.toLowerCase()));
    }

    public TileObjectQuery<T> withNames(String... names)
    {
        return keepIf(o -> o.getName() != null && ArrayUtils.contains(names, o.getName()));
    }

    public TileObjectQuery<T> withNamesContains(String... names)
    {
        return keepIf(o -> o.getName() != null && TextUtil.containsIgnoreCase(o.getName(), names));
    }

    public TileObjectQuery<T> withAction(String action)
    {
        return keepIf(o -> o.getActions() != null && TextUtil.containsIgnoreCase(action, o.getActions()));
    }

    public TileObjectQuery<T> within(int distance)
    {
        return keepIf(o -> Location.within(client.getLocalPlayer().getWorldLocation(), o.getWorldLocation(), distance));
    }

    public TileObjectQuery<T> within(WorldPoint center, int distance)
    {
        return keepIf(o -> Location.within(center, o.getWorldLocation(), distance));
    }

    public TileObjectQuery<T> atLocation(WorldPoint location)
    {
        return keepIf(o -> o.getWorldLocation().equals(location));
    }

    public TileObjectQuery<T> sortNearest()
    {
        return sortNearest(client.getLocalPlayer().getWorldLocation());
    }

    public TileObjectQuery<T> sortNearest(WorldPoint center)
    {
        Point2D point = new Point2D.Double(center.getX(), center.getY());
        return sort((o1, o2) -> {
            Point2D p0 = new Point2D.Double(o1.getWorldLocation().getX(), o1.getWorldLocation().getY());
            Point2D p1 = new Point2D.Double(o2.getWorldLocation().getX(), o2.getWorldLocation().getY());
            return Double.compare(point.distance(p0), point.distance(p1));
        });
    }

    public TileObjectQuery<T> sortFurthest()
    {
        return sortFurthest(client.getLocalPlayer().getWorldLocation());
    }

    public TileObjectQuery<T> sortFurthest(WorldPoint center)
    {
        Point2D point = new Point2D.Double(center.getX(), center.getY());
        return sort((o1, o2) -> {
            Point2D p0 = new Point2D.Double(o1.getWorldLocation().getX(), o1.getWorldLocation().getY());
            Point2D p1 = new Point2D.Double(o2.getWorldLocation().getX(), o2.getWorldLocation().getY());
            return Double.compare(point.distance(p1), point.distance(p0));
        });
    }
}

package com.tonic.queries.abstractions;

import com.tonic.api.entities.ActorAPI;
import com.tonic.util.Location;
import com.tonic.util.TextUtil;
import net.runelite.api.Actor;
import net.runelite.api.coords.WorldPoint;

import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractActorQuery<T extends Actor, Q extends AbstractActorQuery<T, Q>> extends AbstractQuery<T, Q>
{
    public AbstractActorQuery(List<T> cache) {
        super(cache);
    }

    /**
     * filter by name
     * @param name actor name
     * @return ActorQuery
     */
    public Q withName(String name)
    {
        return removeIf(o -> !name.equalsIgnoreCase(TextUtil.sanitize(o.getName())));
    }

    /**
     * filter by name
     * @param name actor name
     * @return ActorQuery
     */
    public Q withNameContains(String name)
    {
        return removeIf(o -> o.getName() == null ||  !TextUtil.sanitize(o.getName()).toLowerCase().contains(name.toLowerCase()));
    }

    /**
     * filter to npcs the player can attack
     * @return ActorQuery
     */
    public Q canAttack()
    {
        return keepIf(ActorAPI::canAttack);
    }

    /**
     * filter by distance
     * @param distance distance
     * @return ActorQuery
     */
    public Q within(int distance) {
        return keepIf(o -> {
            WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
            return Location.getDistance(playerLoc, o.getWorldLocation()) <= distance;
        });
    }

    /**
     * filter by distance from a specific point
     * @param center center point
     * @param distance distance
     * @return ActorQuery
     */
    public Q within(WorldPoint center, int distance)
    {
        return keepIf(o -> Location.within(center, o.getWorldLocation(), distance));
    }

    /**
     * filter by exact location
     * @param location location
     * @return ActorQuery
     */
    public Q atLocation(WorldPoint location)
    {
        return keepIf(o -> o.getWorldLocation().equals(location));
    }

    /**
     * sort by nearest to the player
     * @return ActorQuery
     */
    public Q sortNearest()
    {
        return sortNearest(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * sort by nearest to a specific point
     * @param center center point
     * @return ActorQuery
     */
    public Q sortNearest(WorldPoint center)
    {
        Point2D point = new Point2D.Double(center.getX(), center.getY());
        return sort((o1, o2) -> {
            Point2D p0 = new Point2D.Double(o1.getWorldLocation().getX(), o1.getWorldLocation().getY());
            Point2D p1 = new Point2D.Double(o2.getWorldLocation().getX(), o2.getWorldLocation().getY());
            return Double.compare(point.distance(p0), point.distance(p1));
        });
    }

    /**
     * sort by furthest from the player
     * @return ActorQuery
     */
    public Q sortFurthest()
    {
        return sortFurthest(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * sort by furthest from a specific point
     * @param center center point
     * @return ActorQuery
     */
    public Q sortFurthest(WorldPoint center)
    {
        Point2D point = new Point2D.Double(center.getX(), center.getY());
        return sort((o1, o2) -> {
            Point2D p0 = new Point2D.Double(o1.getWorldLocation().getX(), o1.getWorldLocation().getY());
            Point2D p1 = new Point2D.Double(o2.getWorldLocation().getX(), o2.getWorldLocation().getY());
            return Double.compare(point.distance(p1), point.distance(p0));
        });
    }

    /**
     * Get the nearest Actor from the filtered list
     * Terminal operation - executes the query
     */
    public T nearest() {
        // Apply filters and sort by distance, then get first
        return this.sortNearest().first();
    }

    /**
     * Get the nearest Actor to a specific point
     * Terminal operation - executes the query
     */
    public T nearest(WorldPoint center) {
        return this.sortNearest(center).first();
    }

    /**
     * Get the farthest Actor from the filtered list
     * Terminal operation - executes the query
     */
    public T farthest() {
        return this.sortFurthest().first();
    }

    /**
     * Get the farthest Actor from a specific point
     * Terminal operation - executes the query
     */
    public T farthest(WorldPoint center) {
        return this.sortFurthest(center).first();
    }
}
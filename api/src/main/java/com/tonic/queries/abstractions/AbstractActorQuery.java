package com.tonic.queries.abstractions;

import com.tonic.api.entities.ActorAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.data.wrappers.ActorEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.util.Distance;
import com.tonic.util.Location;
import com.tonic.util.TextUtil;
import net.runelite.api.Actor;
import net.runelite.api.coords.WorldPoint;
import java.awt.geom.Point2D;
import java.util.List;

public abstract class AbstractActorQuery<T extends ActorEx<?>, Q extends AbstractActorQuery<T, Q>> extends AbstractQuery<T, Q>
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
        return keepIf(n -> n.canAttack());
    }

    /**
     * filter reachable actors
     * @return ActorQuery
     */
    public Q isReachable()
    {
        WorldPoint playerLoc = PlayerEx.getLocal().getWorldPoint();
        return keepIf(o -> SceneAPI.isReachable(playerLoc, o.getWorldPoint()));
    }

    /**
     * filter actors that the player has line of sight to
     * @return ActorQuery
     */
    public Q hasLos()
    {
        WorldPoint playerLoc = PlayerEx.getLocal().getWorldPoint();
        return keepIf(o -> SceneAPI.hasLineOfSightTo(playerLoc, o.getWorldPoint()));
    }

    /**
     * filter actors that are already interacting
     * @return ActorQuery
     */
    public Q free()
    {
        return keepIf(o -> !o.getActor().isInteracting());
    }

    /**
     * filter actors that are currently interacting with a specific target actor.
     *
     * @param target actor
     * @return ActorQuery
     */
    public Q interactingWith(ActorEx<?> target) {
        return keepIf(o -> o.getInteracting() != null && o.getInteracting().equals(target));
    }

    /**
     * filter by distance
     * @param distance distance
     * @return ActorQuery
     */
    public Q within(int distance) {
        return keepIf(o -> {
            WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
            return Distance.chebyshev(playerLoc, o.getWorldPoint()) <= distance;
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
        return keepIf(o -> Distance.chebyshev(center, o.getWorldPoint()) <= distance);
    }

    /**
     * filter by exact location
     * @param location location
     * @return ActorQuery
     */
    public Q atLocation(WorldPoint location)
    {
        return keepIf(o -> o.getWorldPoint().equals(location));
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
            Point2D p0 = new Point2D.Double(o1.getWorldPoint().getX(), o1.getWorldPoint().getY());
            Point2D p1 = new Point2D.Double(o2.getWorldPoint().getX(), o2.getWorldPoint().getY());
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
            Point2D p0 = new Point2D.Double(o1.getWorldPoint().getX(), o1.getWorldPoint().getY());
            Point2D p1 = new Point2D.Double(o2.getWorldPoint().getX(), o2.getWorldPoint().getY());
            return Double.compare(point.distance(p1), point.distance(p0));
        });
    }

    /**
     * sort by shortest path from the player
     * @return ActorQuery
     */
    public Q sortShortestPath()
    {
        return sortShortestPath(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * sort by shortest path from a specific point
     * @param center center point
     * @return ActorQuery
     */
    public Q sortShortestPath(WorldPoint center)
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
     * @return ActorQuery
     */
    public Q sortLongestPath()
    {
        return sortLongestPath(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * sort by furthest path from a specific point
     * @param center center point
     * @return ActorQuery
     */
    public Q sortLongestPath(WorldPoint center)
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

    /**
     * Get the Actor with the shortest path from the filtered list
     * Terminal operation - executes the query
     */
    public T shortestPath() {
        return this.sortShortestPath().first();
    }

    /**
     * Get the Actor with the furthest path from the filtered list
     * Terminal operation - executes the query
     */
    public T longestPath() {
        return this.sortLongestPath().first();
    }
}
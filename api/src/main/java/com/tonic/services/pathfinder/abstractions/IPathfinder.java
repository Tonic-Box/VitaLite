package com.tonic.services.pathfinder.abstractions;

import com.tonic.services.pathfinder.teleports.Teleport;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

/**
 * Plans a route from a start tile to a target tile or to the nearest of some areas.
 */
public interface IPathfinder
{
    /**
     * @param start where the route starts
     * @param target the tile to reach
     * @return the steps, first to last, or empty when there is no route
     */
    List<? extends IStep> find(WorldPoint start, WorldPoint target);

    /**
     * @param start where the route starts
     * @param worldAreas the areas any tile of which ends the route
     * @return the steps, first to last, or empty when there is no route
     */
    List<? extends IStep> find(WorldPoint start, WorldArea... worldAreas);

    /**
     * @param start where the route starts
     * @param worldAreas the areas any tile of which ends the route
     * @return the steps, first to last, or empty when there is no route
     */
    List<? extends IStep> find(WorldPoint start, List<WorldArea> worldAreas);

    /**
     * @return the teleport the last route begins with, or null when it is walked from the start
     */
    Teleport getTeleport();
}

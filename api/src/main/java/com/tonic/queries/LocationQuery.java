package com.tonic.queries;

import com.tonic.Static;
import com.tonic.api.game.SceneAPI;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.queries.abstractions.AbstractQuery;
import com.tonic.services.GameManager;
import com.tonic.util.Distance;
import com.tonic.util.Location;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A query class for filtering and manipulating Tile locations in the game world.
 */
public class LocationQuery extends AbstractQuery<Tile, LocationQuery>
{
    /**
     * Constructs a new LocationQuery instance, initializing it with all tiles from the game manager.
     */
    public LocationQuery() {
        super(GameManager.getTiles());
    }

    /**
     * Filters the query to only include tiles that are reachable from the player's current position.
     *
     * @return LocationQuery
     */
    public LocationQuery isReachable() {
        Tile player = SceneAPI.getTile(PlayerEx.getLocal().getWorldPoint());
        keepIf(tile -> SceneAPI.isReachable(player, tile));
        return this;
    }

    /**
     * Filters the query to only include tiles that have a line of sight to the player's current position.
     *
     * @return LocationQuery
     */
    public LocationQuery hasLosTo() {
        Tile player = SceneAPI.getTile(PlayerEx.getLocal().getWorldPoint());
        return keepIf(tile -> SceneAPI.hasLineOfSightTo(player, tile));
    }

    /**
     * Filters the query to only include tiles within a specified distance from the player.
     *
     * @param distance The maximum distance from the player.
     * @return LocationQuery
     */
    public LocationQuery withinDistance(int distance) {
        Tile player = SceneAPI.getTile(PlayerEx.getLocal().getWorldPoint());
        return keepIf(tile -> Distance.chebyshev(player, tile) <= distance);
    }

    /**
     * Filters the query to exclude tiles within a specified distance from the player.
     *
     * @param distance The minimum distance from the player.
     * @return LocationQuery
     */
    public LocationQuery beyondDistance(int distance) {
        Tile player = SceneAPI.getTile(PlayerEx.getLocal().getWorldPoint());
        return removeIf(tile -> Distance.chebyshev(player, tile) <= distance);
    }

    /**
     * Filters the query to only include tiles that are within a specified pathing distance from the player.
     *
     * @param distance The maximum pathing distance from the player.
     * @return LocationQuery
     */
    public LocationQuery withinPathingDistance(int distance) {
        Tile player = SceneAPI.getTile(PlayerEx.getLocal().getWorldPoint());
        return keepIf(tile -> {
            var path = SceneAPI.pathTo(player, tile);
            return path != null && path.size() <= distance;
        });
    }

    /**
     * Filters the query to exclude tiles that are within a specified pathing distance from the player.
     *
     * @param distance The minimum pathing distance from the player.
     * @return LocationQuery
     */
    public LocationQuery beyondPathingDistance(int distance) {
        Tile player = SceneAPI.getTile(PlayerEx.getLocal().getWorldPoint());
        return removeIf(tile -> {
            var path = SceneAPI.pathTo(player, tile);
            return path != null && path.size() <= distance;
        });
    }

    /**
     * Filters the query to only include tiles within a specified WorldArea.
     *
     * @param area The WorldArea to check against.
     * @return LocationQuery
     */
    public LocationQuery withinArea(WorldArea area)
    {
        return keepIf(tile -> area.contains(tile.getWorldLocation()));
    }

    /**
     * Filters the query to exclude tiles within a specified WorldArea.
     *
     * @param area The WorldArea to check against.
     * @return LocationQuery
     */
    public LocationQuery outsideArea(WorldArea area)
    {
        return removeIf(tile -> area.contains(tile.getWorldLocation()));
    }

    /**
     * Filters the query to only include tiles that contain any tile objects (decorative, game, ground, or wall).
     *
     * @return LocationQuery
     */
    public LocationQuery hasTileObject()
    {
        return keepIf(tile ->
                tile.getDecorativeObject() != null
                || tile.getGameObjects() != null && tile.getGameObjects().length > 0
                || tile.getGroundObject() != null
                || tile.getWallObject() != null
        );
    }

    /**
     * Converts the collected tiles to a list of WorldPoint locations.
     *
     * @return List of WorldPoint locations.
     */
    public List<WorldPoint> toWorldPointList()
    {
        return Static.invoke(() -> collect().stream().map(Tile::getWorldLocation).collect(Collectors.toList()));
    }

    /**
     * Converts the collected tiles to a list of LocalPoint locations.
     *
     * @return List of LocalPoint locations.
     */
    public List<LocalPoint> toLocalPointList()
    {
        return Static.invoke(() -> collect().stream().map(Tile::getLocalLocation).collect(Collectors.toList()));
    }
}

package com.tonic.queries;

import com.tonic.Static;
import com.tonic.api.game.SceneAPI;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.queries.abstractions.AbstractQuery;
import com.tonic.services.GameManager;
import com.tonic.util.Location;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.stream.Collectors;

public class LocationQuery extends AbstractQuery<Tile, LocationQuery>
{
    public LocationQuery() {
        super(GameManager.getTiles());
    }

    public LocationQuery isReachable() {
        Tile player = Location.toTile(PlayerEx.getLocal().getWorldPoint());
        keepIf(tile -> Location.isReachable(player, tile));
        return this;
    }

    public LocationQuery hasLosTo() {
        Tile player = Location.toTile(PlayerEx.getLocal().getWorldPoint());
        return keepIf(tile -> Location.hasLineOfSightTo(player, tile));
    }

    public LocationQuery withinDistance(int distance) {
        Tile player = Location.toTile(PlayerEx.getLocal().getWorldPoint());
        return keepIf(tile -> Location.getDistance(player, tile) <= distance);
    }

    public LocationQuery beyondDistance(int distance) {
        Tile player = Location.toTile(PlayerEx.getLocal().getWorldPoint());
        return removeIf(tile -> Location.getDistance(player, tile) <= distance);
    }

    public LocationQuery withinPathingDistance(int distance) {
        Tile player = Location.toTile(PlayerEx.getLocal().getWorldPoint());
        return keepIf(tile -> {
            var path = SceneAPI.pathTo(player, tile);
            return path != null && path.size() <= distance;
        });
    }

    public LocationQuery beyondPathingDistance(int distance) {
        Tile player = Location.toTile(PlayerEx.getLocal().getWorldPoint());
        return removeIf(tile -> {
            var path = SceneAPI.pathTo(player, tile);
            return path != null && path.size() <= distance;
        });
    }

    public LocationQuery withinArea(WorldArea area)
    {
        return keepIf(tile -> area.contains(tile.getWorldLocation()));
    }

    public LocationQuery outsideArea(WorldArea area)
    {
        return removeIf(tile -> area.contains(tile.getWorldLocation()));
    }

    public LocationQuery hasTileObject()
    {
        return keepIf(tile ->
                tile.getDecorativeObject() != null
                || tile.getGameObjects() != null && tile.getGameObjects().length > 0
                || tile.getGroundObject() != null
                || tile.getWallObject() != null
        );
    }

    public List<WorldPoint> toWorldPointList()
    {
        return Static.invoke(() -> collect().stream().map(Tile::getWorldLocation).collect(Collectors.toList()));
    }

    public List<LocalPoint> toLocalPointList()
    {
        return Static.invoke(() -> collect().stream().map(Tile::getLocalLocation).collect(Collectors.toList()));
    }
}

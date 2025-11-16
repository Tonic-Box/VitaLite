package com.tonic.data.wrappers.abstractions;

import com.tonic.api.game.SceneAPI;
import com.tonic.data.wrappers.TileObjectEx;
import net.runelite.api.GameObject;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;

public interface Locatable
{
    default WorldPoint getReachablePoint()
    {
        if(this instanceof TileObjectEx) {
            TileObjectEx obj = (TileObjectEx) this;
            return obj.getInteractionPoint();
        }
        return getWorldPoint();
    }

    /**
     * Gets the world point of this locatable.
     *
     * @return the world point
     */
    WorldPoint getWorldPoint();

    /**
     * Gets the world area of this locatable.
     *
     * @return the world area
     */
    WorldArea getWorldArea();

    /**
     * Gets the local point of this locatable.
     *
     * @return the local point
     */
    LocalPoint getLocalPoint();

    /**
     * Gets the tile of this locatable.
     *
     * @return the tile
     */
    Tile getTile();

    Shape getShape();

    /**
     * Calculates the distance to another locatable.
     *
     * @param other the other locatable
     * @return the distance
     */
    default int distanceTo(Locatable other)
    {
        WorldPoint to;
        if(other instanceof TileObjectEx) {
            TileObjectEx obj = (TileObjectEx) other;
            to = obj.getInteractionPoint(getWorldPoint());
        }
        else {
            to = other.getWorldPoint();
        }
        return distanceTo(to);
    }

    default int distanceTo(WorldPoint other)
    {
        WorldPoint from;
        if(this instanceof TileObjectEx)
        {
            TileObjectEx obj = (TileObjectEx) this;
            from = obj.getInteractionPoint(other);
        }
        else {
            from = getWorldPoint();
        }
        var path = SceneAPI.pathTo(from, other);
        return path != null ? path.size() : Integer.MAX_VALUE;
    }

    default int distanceTo(int x, int y, int z)
    {
        return distanceTo(new WorldPoint(x, y, z));
    }

    default int distanceTo(Tile tile)
    {
        return distanceTo(tile.getWorldLocation());
    }
}

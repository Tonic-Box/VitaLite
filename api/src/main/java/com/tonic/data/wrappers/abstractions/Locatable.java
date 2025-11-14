package com.tonic.data.wrappers.abstractions;

import com.tonic.api.game.SceneAPI;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;

public interface Locatable
{
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
        var path = SceneAPI.pathTo(getWorldPoint(), other.getWorldPoint());
        return path != null ? path.size() : Integer.MAX_VALUE;
    }
}

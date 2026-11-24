package com.tonic.services.pathfinder.transports;

import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * A tile a transport leaves from or lands on, with its packed key for graph lookups.
 */
@Getter
public class Loc
{
    private final int worldX;
    private final int worldY;
    private final int plane;
    private int packed;

    /**
     * @param worldX the world x
     * @param worldY the world y
     * @param plane the plane
     */
    public Loc(int worldX, int worldY, int plane)
    {
        this.worldX = worldX;
        this.worldY = worldY;
        this.plane = plane;
        this.packed = WorldPointUtil.compress(worldX, worldY, plane);
    }

    /**
     * @param point a tile
     * @return the tile as a Loc
     */
    public static Loc of(WorldPoint point)
    {
        return new Loc(point.getX(), point.getY(), point.getPlane());
    }

    /**
     * Works the packed key out again from the coordinates, for a Loc read from a file whose key may not match them.
     */
    void repack()
    {
        packed = WorldPointUtil.compress(worldX, worldY, plane);
    }

    /**
     * @return the tile
     */
    public WorldPoint toWorldPoint()
    {
        return new WorldPoint(worldX, worldY, plane);
    }
}

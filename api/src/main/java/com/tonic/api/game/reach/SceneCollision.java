package com.tonic.api.game.reach;

import lombok.Getter;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

/**
 * One plane of a world view's collision, read by scene tile: padded maps (boats) keep their flags one tile in
 */
public final class SceneCollision
{
    /**
     * The flags that take a whole tile up
     */
    public static final int BLOCK_TILE = CollisionDataFlag.BLOCK_MOVEMENT_FULL;

    private final int[][] flags;
    private final int inset;
    @Getter
    private final int sizeX;
    @Getter
    private final int sizeY;
    @Getter
    private final int baseX;
    @Getter
    private final int baseY;
    @Getter
    private final int plane;

    /**
     * @param flags the collision flags as the client keeps them
     * @param inset how far in the flags start, 1 for padded maps and 0 otherwise
     * @param sizeX the scene's width in tiles
     * @param sizeY the scene's height in tiles
     * @param baseX the world x of scene column 0
     * @param baseY the world y of scene row 0
     * @param plane the plane
     */
    public SceneCollision(int[][] flags, int inset, int sizeX, int sizeY, int baseX, int baseY, int plane)
    {
        this.flags = flags;
        this.inset = inset;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.baseX = baseX;
        this.baseY = baseY;
        this.plane = plane;
    }

    /**
     * Reads a world view's collision on one plane, on the client thread
     * @param view the world view
     * @param plane the plane
     * @return the collision, or null when the view has none for that plane
     */
    public static SceneCollision of(WorldView view, int plane)
    {
        if (view == null || plane < 0)
        {
            return null;
        }

        CollisionData[] maps = view.getCollisionMaps();
        if (maps == null || plane >= maps.length || maps[plane] == null)
        {
            return null;
        }

        int[][] flags = maps[plane].getFlags();
        int inset = flags.length > view.getSizeX() ? 1 : 0;
        return new SceneCollision(flags, inset, view.getSizeX(), view.getSizeY(), view.getBaseX(), view.getBaseY(), plane);
    }

    /**
     * @param x the scene column
     * @param y the scene row
     * @return whether the tile lies in the scene
     */
    public boolean inside(int x, int y)
    {
        return x >= 0 && y >= 0 && x < sizeX && y < sizeY;
    }

    /**
     * Gets a tile's flags
     * @param x the scene column, inside the scene
     * @param y the scene row, inside the scene
     * @return the flags
     */
    public int flags(int x, int y)
    {
        return flags[x + inset][y + inset];
    }

    /**
     * @param x the scene column, inside the scene
     * @param y the scene row, inside the scene
     * @return whether something takes the tile up
     */
    public boolean blocked(int x, int y)
    {
        return (flags(x, y) & BLOCK_TILE) != 0;
    }

    /**
     * Turns a scene tile into a world point
     * @param x the scene column
     * @param y the scene row
     * @return the world point
     */
    public WorldPoint toWorld(int x, int y)
    {
        return new WorldPoint(baseX + x, baseY + y, plane);
    }

    /**
     * @param point a world point
     * @return its scene column
     */
    public int sceneX(WorldPoint point)
    {
        return point.getX() - baseX;
    }

    /**
     * @param point a world point
     * @return its scene row
     */
    public int sceneY(WorldPoint point)
    {
        return point.getY() - baseY;
    }
}

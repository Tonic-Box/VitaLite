package com.tonic.services.ground;

import net.runelite.api.Scene;
import net.runelite.api.WorldView;

/**
 * The ground of one world view as the renderers draw it: its corner heights and which level each tile's floor is on.
 */
final class GroundSurface
{
    private static final int EXTENDED_OFFSET = 40;
    static final int PLANES = 4;

    private static final int MAX_PLANE = PLANES - 1;

    private final int sizeX;
    private final int sizeY;
    private final int baseX;
    private final int baseY;
    private final int offset;
    private final int[][][] heights;
    private final byte[][][] settings;

    private GroundSurface(int sizeX, int sizeY, int baseX, int baseY, int offset, int[][][] heights, byte[][][] settings)
    {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.baseX = baseX;
        this.baseY = baseY;
        this.offset = offset;
        this.heights = heights;
        this.settings = settings;
    }

    static GroundSurface of(WorldView worldView)
    {
        if (worldView == null)
        {
            return null;
        }
        Scene scene = worldView.getScene();
        if (scene == null || scene.getTileHeights() == null || scene.getExtendedTileSettings() == null)
        {
            return null;
        }
        int offset = worldView.isTopLevel() ? EXTENDED_OFFSET : 0;
        return new GroundSurface(worldView.getSizeX(), worldView.getSizeY(), worldView.getBaseX(), worldView.getBaseY(), offset, scene.getTileHeights(), scene.getExtendedTileSettings());
    }

    int minTile()
    {
        return -offset;
    }

    int maxTileX()
    {
        return sizeX + offset;
    }

    int maxTileY()
    {
        return sizeY + offset;
    }

    int baseX()
    {
        return baseX;
    }

    int baseY()
    {
        return baseY;
    }

    int height(int plane, int cornerX, int cornerY)
    {
        int[][] level = heights[plane];
        int x = Math.min(Math.max(cornerX + offset, 0), level.length - 1);
        int y = Math.min(Math.max(cornerY + offset, 0), level[x].length - 1);
        return level[x][y];
    }

    boolean containsTile(int tileX, int tileY)
    {
        byte[][] bridges = settings[1];
        int x = tileX + offset;
        int y = tileY + offset;
        return x >= 0 && y >= 0 && x < bridges.length && y < bridges[x].length;
    }

    int surfacePlane(int plane, int tileX, int tileY)
    {
        if (plane >= MAX_PLANE)
        {
            return plane;
        }
        byte[][] bridges = settings[1];
        int x = tileX + offset;
        int y = tileY + offset;
        if (x < 0 || y < 0 || x >= bridges.length || y >= bridges[x].length)
        {
            return plane;
        }
        return (bridges[x][y] & 2) == 2 ? plane + 1 : plane;
    }
}

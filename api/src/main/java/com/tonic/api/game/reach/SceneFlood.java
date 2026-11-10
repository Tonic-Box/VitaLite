package com.tonic.api.game.reach;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import net.runelite.api.CollisionDataFlag;

/**
 * The tiles of a scene that can be walked to from one tile, stepping straight across edges with no wall onto tiles nothing takes up
 */
public final class SceneFlood
{
    private static final int[][] STEPS = {
            {0, -1, CollisionDataFlag.BLOCK_MOVEMENT_SOUTH},
            {0, 1, CollisionDataFlag.BLOCK_MOVEMENT_NORTH},
            {-1, 0, CollisionDataFlag.BLOCK_MOVEMENT_WEST},
            {1, 0, CollisionDataFlag.BLOCK_MOVEMENT_EAST}
    };

    private SceneFlood()
    {
    }

    /**
     * Floods the scene from a tile
     * @param map the plane's collision
     * @param x the scene column walked from
     * @param y the scene row walked from
     * @return the walkable tiles by scene column then row, the start included, all false when the start lies outside the scene
     */
    public static boolean[][] flood(SceneCollision map, int x, int y)
    {
        boolean[][] visited = new boolean[map.getSizeX()][map.getSizeY()];
        if (!map.inside(x, y))
        {
            return visited;
        }

        IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        visited[x][y] = true;
        queue.enqueue(x << 16 | y);
        while (!queue.isEmpty())
        {
            int point = queue.dequeueInt();
            int fromX = point >>> 16;
            int fromY = point & 0xFFFF;
            int from = map.flags(fromX, fromY);
            for (int[] step : STEPS)
            {
                int toX = fromX + step[0];
                int toY = fromY + step[1];
                if (!map.inside(toX, toY) || visited[toX][toY] || (from & step[2]) != 0 || map.blocked(toX, toY))
                {
                    continue;
                }

                visited[toX][toY] = true;
                queue.enqueue(toX << 16 | toY);
            }
        }
        return visited;
    }
}

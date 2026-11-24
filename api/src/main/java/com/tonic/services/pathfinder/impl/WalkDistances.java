package com.tonic.services.pathfinder.impl;

import com.tonic.Static;
import com.tonic.api.game.reach.SceneCollision;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;

/**
 * How many steps the walk from one tile takes to each tile of the loaded scene over its live collision, doors as they stand.
 */
public final class WalkDistances
{
    /**
     * The distance to a tile that cannot be walked to.
     */
    public static final int UNREACHABLE = -1;

    private static final WalkDistances NOWHERE = new WalkDistances(0, 0, -1, 0, 0, new int[0]);
    private static final int[] DX = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DY = {1, 1, 1, 0, 0, -1, -1, -1};

    private final int baseX;
    private final int baseY;
    private final int plane;
    private final int sizeX;
    private final int sizeY;
    private final int[] steps;

    private WalkDistances(int baseX, int baseY, int plane, int sizeX, int sizeY, int[] steps)
    {
        this.baseX = baseX;
        this.baseY = baseY;
        this.plane = plane;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.steps = steps;
    }

    /**
     * Searches the loaded scene from a tile, on the client thread.
     *
     * @param start the tile walked from, in the scene's own coordinates
     * @return the distances, nowhere reachable when the tile is off the scene
     */
    public static WalkDistances from(WorldPoint start)
    {
        if (start == null)
        {
            return NOWHERE;
        }
        return Static.invoke(() ->
        {
            Client client = Static.getClient();
            WorldView view = client.getTopLevelWorldView();
            SceneCollision scene = view == null ? null : SceneCollision.of(view, start.getPlane());
            if (scene == null)
            {
                return NOWHERE;
            }
            int sizeX = scene.getSizeX();
            int sizeY = scene.getSizeY();
            int startX = start.getX() - scene.getBaseX();
            int startY = start.getY() - scene.getBaseY();
            if (startX < 0 || startY < 0 || startX >= sizeX || startY >= sizeY)
            {
                return NOWHERE;
            }
            int[][] flags = new int[sizeX][sizeY];
            for (int x = 0; x < sizeX; x++)
            {
                for (int y = 0; y < sizeY; y++)
                {
                    flags[x][y] = scene.flags(x, y);
                }
            }
            return new WalkDistances(scene.getBaseX(), scene.getBaseY(), start.getPlane(), sizeX, sizeY, search(new LiveCollision(flags), sizeX, sizeY, startX, startY));
        });
    }

    private static int[] search(LiveCollision collision, int sizeX, int sizeY, int startX, int startY)
    {
        int[] steps = new int[sizeX * sizeY];
        Arrays.fill(steps, UNREACHABLE);
        int[] queue = new int[sizeX * sizeY];
        int head = 0;
        int tail = 0;
        steps[startX * sizeY + startY] = 0;
        queue[tail++] = startX * sizeY + startY;
        while (head < tail)
        {
            int at = queue[head++];
            int x = at / sizeY;
            int y = at % sizeY;
            for (int direction = 0; direction < DX.length; direction++)
            {
                int toX = x + DX[direction];
                int toY = y + DY[direction];
                if (toX < 0 || toY < 0 || toX >= sizeX || toY >= sizeY || steps[toX * sizeY + toY] != UNREACHABLE)
                {
                    continue;
                }
                if (collision.canMove(x, y, DX[direction], DY[direction]))
                {
                    steps[toX * sizeY + toY] = steps[at] + 1;
                    queue[tail++] = toX * sizeY + toY;
                }
            }
        }
        return steps;
    }

    /**
     * @param target the tile walked to, in the scene's own coordinates
     * @return how many steps the walk takes, or UNREACHABLE when it cannot be made or the tile is off the scene
     */
    public int to(WorldPoint target)
    {
        if (target == null || target.getPlane() != plane)
        {
            return UNREACHABLE;
        }
        int x = target.getX() - baseX;
        int y = target.getY() - baseY;
        if (x < 0 || y < 0 || x >= sizeX || y >= sizeY)
        {
            return UNREACHABLE;
        }
        return steps[x * sizeY + y];
    }
}

package com.tonic.services.pathfinder;

import net.runelite.api.coords.WorldPoint;

/**
 * How long the local player has stood on one tile, counted in game ticks on the client thread.
 */
public final class PlayerMotion
{
    private static volatile WorldPoint last;
    private static volatile int stillTicks;
    private static volatile boolean moved;
    private static int counted;

    private PlayerMotion()
    {
    }

    /**
     * Notes where the player stands this game tick. Call it once a game tick on the client thread.
     *
     * @param position the player's tile, or null when there is no player
     */
    public static void tick(WorldPoint position)
    {
        moved = position != null && !position.equals(last);
        counted = moved || position == null ? 0 : counted + 1;
        stillTicks = counted;
        last = position;
    }

    /**
     * @return how many game ticks the player has stood on the tile it stands on
     */
    public static int stillTicks()
    {
        return stillTicks;
    }

    /**
     * @return whether the player changed tile on the last game tick
     */
    public static boolean movedThisTick()
    {
        return moved;
    }
}

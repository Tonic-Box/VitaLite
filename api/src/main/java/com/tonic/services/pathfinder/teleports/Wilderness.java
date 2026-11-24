package com.tonic.services.pathfinder.teleports;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

/**
 * The Wilderness level any tile sits at by its place on the map, counted in bands of eight rows north of the ditch up to level 56, underground included.
 */
public final class Wilderness
{
    private static final int BASE_Y = 3520;
    private static final int TOP_Y = 3967;
    private static final int MIN_X = 2944;
    private static final int MAX_X = 3392;
    private static final int UNDERGROUND_OFFSET = 6400;
    private static final int BAND = 8;

    private Wilderness()
    {
    }

    /**
     * @param position a tile, or null
     * @return the Wilderness level it sits at, 0 anywhere outside the Wilderness
     */
    public static int level(WorldPoint position)
    {
        if (position == null || position.getX() < MIN_X || position.getX() > MAX_X)
        {
            return 0;
        }
        int y = position.getY();
        if (y > UNDERGROUND_OFFSET)
        {
            y -= UNDERGROUND_OFFSET;
        }
        if (y <= BASE_Y || y > TOP_Y)
        {
            return 0;
        }
        return ((y - BASE_Y) / BAND) + 1;
    }

    /**
     * @param area an area, or null
     * @return whether any tile of it is in the Wilderness, judged by its northern corners
     */
    public static boolean touches(WorldArea area)
    {
        if (area == null)
        {
            return false;
        }
        int north = area.getY() + area.getHeight() - 1;
        int east = area.getX() + area.getWidth() - 1;
        return level(new WorldPoint(area.getX(), north, area.getPlane())) > 0 || level(new WorldPoint(east, north, area.getPlane())) > 0;
    }
}

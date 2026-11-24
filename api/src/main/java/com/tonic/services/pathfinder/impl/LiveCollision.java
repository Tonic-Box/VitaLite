package com.tonic.services.pathfinder.impl;

import net.runelite.api.CollisionDataFlag;

/**
 * A copy of one plane of the scene's movement flags, from which locs can be taken out again, and the client's rule for whether a step may be taken over it.
 */
final class LiveCollision
{
    /**
     * Every flag set, as the client keeps the scene's border tiles and as a tile off the scene reads.
     */
    static final int BLOCK_FULL = 0xFFFFFF;

    private static final int BLOCK_NORTH_WEST = CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST;
    private static final int BLOCK_NORTH = CollisionDataFlag.BLOCK_MOVEMENT_NORTH;
    private static final int BLOCK_NORTH_EAST = CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST;
    private static final int BLOCK_EAST = CollisionDataFlag.BLOCK_MOVEMENT_EAST;
    private static final int BLOCK_SOUTH_EAST = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST;
    private static final int BLOCK_SOUTH = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH;
    private static final int BLOCK_SOUTH_WEST = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST;
    private static final int BLOCK_WEST = CollisionDataFlag.BLOCK_MOVEMENT_WEST;
    private static final int BLOCK_OBJECT = CollisionDataFlag.BLOCK_MOVEMENT_OBJECT;
    private static final int BLOCK_FLOOR_DECORATION = CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION;
    private static final int BLOCK_TILE = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
    private static final int SHAPE_WALL_STRAIGHT = 0;
    private static final int SHAPE_WALL_DIAGONAL_CORNER = 1;
    private static final int SHAPE_WALL_L_CORNER = 2;
    private static final int SHAPE_WALL_SQUARE_CORNER = 3;

    private final int sizeX;
    private final int sizeY;
    private final int[][] flags;

    /**
     * @param flags the plane's flags by scene column and row, owned by this copy from now on
     */
    LiveCollision(int[][] flags)
    {
        this.flags = flags;
        this.sizeX = flags.length;
        this.sizeY = flags.length == 0 ? 0 : flags[0].length;
    }

    /**
     * @param x the scene column
     * @param y the scene row
     * @return the tile's flags, or BLOCK_FULL off the scene
     */
    int flags(int x, int y)
    {
        return inside(x, y) ? flags[x][y] : BLOCK_FULL;
    }

    /**
     * @param x the scene column
     * @param y the scene row
     * @return whether something takes the tile up
     */
    boolean blocked(int x, int y)
    {
        return (flags(x, y) & BLOCK_TILE) != 0;
    }

    /**
     * Takes a wall's movement flags out, on its own tile and the tile it faces.
     *
     * @param x the scene column
     * @param y the scene row
     * @param shape the wall's shape
     * @param rotation the wall's rotation
     */
    void removeWall(int x, int y, int shape, int rotation)
    {
        switch (shape)
        {
            case SHAPE_WALL_STRAIGHT:
                switch (rotation)
                {
                    case 0:
                        remove(x, y, BLOCK_WEST);
                        remove(x - 1, y, BLOCK_EAST);
                        break;
                    case 1:
                        remove(x, y, BLOCK_NORTH);
                        remove(x, y + 1, BLOCK_SOUTH);
                        break;
                    case 2:
                        remove(x, y, BLOCK_EAST);
                        remove(x + 1, y, BLOCK_WEST);
                        break;
                    default:
                        remove(x, y, BLOCK_SOUTH);
                        remove(x, y - 1, BLOCK_NORTH);
                        break;
                }
                break;
            case SHAPE_WALL_DIAGONAL_CORNER:
            case SHAPE_WALL_SQUARE_CORNER:
                switch (rotation)
                {
                    case 0:
                        remove(x, y, BLOCK_NORTH_WEST);
                        remove(x - 1, y + 1, BLOCK_SOUTH_EAST);
                        break;
                    case 1:
                        remove(x, y, BLOCK_NORTH_EAST);
                        remove(x + 1, y + 1, BLOCK_SOUTH_WEST);
                        break;
                    case 2:
                        remove(x, y, BLOCK_SOUTH_EAST);
                        remove(x + 1, y - 1, BLOCK_NORTH_WEST);
                        break;
                    default:
                        remove(x, y, BLOCK_SOUTH_WEST);
                        remove(x - 1, y - 1, BLOCK_NORTH_EAST);
                        break;
                }
                break;
            case SHAPE_WALL_L_CORNER:
                switch (rotation)
                {
                    case 0:
                        remove(x, y, BLOCK_NORTH | BLOCK_WEST);
                        remove(x - 1, y, BLOCK_EAST);
                        remove(x, y + 1, BLOCK_SOUTH);
                        break;
                    case 1:
                        remove(x, y, BLOCK_NORTH | BLOCK_EAST);
                        remove(x, y + 1, BLOCK_SOUTH);
                        remove(x + 1, y, BLOCK_WEST);
                        break;
                    case 2:
                        remove(x, y, BLOCK_SOUTH | BLOCK_EAST);
                        remove(x + 1, y, BLOCK_WEST);
                        remove(x, y - 1, BLOCK_NORTH);
                        break;
                    default:
                        remove(x, y, BLOCK_SOUTH | BLOCK_WEST);
                        remove(x, y - 1, BLOCK_NORTH);
                        remove(x - 1, y, BLOCK_EAST);
                        break;
                }
                break;
            default:
                break;
        }
    }

    /**
     * Takes an object's flag out of the tiles it covers.
     *
     * @param x the scene column of its south-west tile
     * @param y the scene row of its south-west tile
     * @param width how many columns it covers
     * @param height how many rows it covers
     */
    void removeObject(int x, int y, int width, int height)
    {
        for (int tx = x; tx < x + width; tx++)
        {
            for (int ty = y; ty < y + height; ty++)
            {
                remove(tx, ty, BLOCK_OBJECT);
            }
        }
    }

    /**
     * Takes a blocking floor decoration's flag out of its tile.
     *
     * @param x the scene column
     * @param y the scene row
     */
    void removeFloorDecoration(int x, int y)
    {
        remove(x, y, BLOCK_FLOOR_DECORATION);
    }

    /**
     * Whether a step one tile in a direction may be taken, as the client's movement allows it: the tile stepped onto must not be taken up, no wall may stand on the edge crossed, and a diagonal step needs both tiles beside it clear and no wall on the way round either of them.
     *
     * @param x the column stepped from
     * @param y the row stepped from
     * @param dx -1, 0 or 1
     * @param dy -1, 0 or 1
     * @return true when the step is free
     */
    boolean canMove(int x, int y, int dx, int dy)
    {
        if (dx == 0 && dy == 0)
        {
            return false;
        }

        int from = flags(x, y);
        if (blocked(x + dx, y + dy))
        {
            return false;
        }
        if (dx == 0 || dy == 0)
        {
            return (from & edge(dx, dy)) == 0;
        }

        int vertical = edge(0, dy);
        int horizontal = edge(dx, 0);
        return (from & (vertical | horizontal | corner(dx, dy))) == 0 && (flags(x + dx, y) & vertical) == 0 && (flags(x, y + dy) & horizontal) == 0 && !blocked(x + dx, y) && !blocked(x, y + dy);
    }

    private void remove(int x, int y, int flag)
    {
        if (inside(x, y))
        {
            flags[x][y] &= ~flag;
        }
    }

    private boolean inside(int x, int y)
    {
        return x >= 0 && y >= 0 && x < sizeX && y < sizeY;
    }

    private static int edge(int dx, int dy)
    {
        if (dy > 0)
        {
            return BLOCK_NORTH;
        }
        if (dy < 0)
        {
            return BLOCK_SOUTH;
        }
        return dx > 0 ? BLOCK_EAST : BLOCK_WEST;
    }

    private static int corner(int dx, int dy)
    {
        if (dy > 0)
        {
            return dx > 0 ? BLOCK_NORTH_EAST : BLOCK_NORTH_WEST;
        }
        return dx > 0 ? BLOCK_SOUTH_EAST : BLOCK_SOUTH_WEST;
    }
}

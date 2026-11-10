package com.tonic.api.game.reach;

import net.runelite.api.CollisionDataFlag;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * The server's rule for which tiles an object or actor can be interacted with from, over scene collision read on the tile stood on
 */
public final class ReachRule
{
    public static final int BLOCK_ACCESS_NORTH = 1;
    public static final int BLOCK_ACCESS_EAST = 2;
    public static final int BLOCK_ACCESS_SOUTH = 4;
    public static final int BLOCK_ACCESS_WEST = 8;

    public static final int SHAPE_WALL_STRAIGHT = 0;
    public static final int SHAPE_WALL_DIAGONAL_CORNER = 1;
    public static final int SHAPE_WALL_L_CORNER = 2;
    public static final int SHAPE_WALL_SQUARE_CORNER = 3;
    public static final int SHAPE_WALL_DECORATION_DIAGONAL_OFFSET = 6;
    public static final int SHAPE_WALL_DECORATION_DIAGONAL = 7;
    public static final int SHAPE_WALL_DECORATION_DIAGONAL_BOTH = 8;
    public static final int SHAPE_DIAGONAL_WALL = 9;
    public static final int SHAPE_OBJECT = 10;
    public static final int SHAPE_OBJECT_DIAGONAL = 11;
    public static final int SHAPE_GROUND_DECORATION = 22;

    private static final int SIDE_BITS = BLOCK_ACCESS_NORTH | BLOCK_ACCESS_EAST | BLOCK_ACCESS_SOUTH | BLOCK_ACCESS_WEST;
    private static final int NORTH = CollisionDataFlag.BLOCK_MOVEMENT_NORTH;
    private static final int EAST = CollisionDataFlag.BLOCK_MOVEMENT_EAST;
    private static final int SOUTH = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH;
    private static final int WEST = CollisionDataFlag.BLOCK_MOVEMENT_WEST;
    private static final int[][] SIDES = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

    private ReachRule()
    {
    }

    /**
     * A placed object as the rule sees it, in scene tiles
     */
    public static final class Target
    {
        private final int x;
        private final int y;
        private final int shape;
        private final int rotation;
        private final int sizeX;
        private final int sizeY;
        private final int accessFlags;

        /**
         * @param x the south-west scene column
         * @param y the south-west scene row
         * @param shape the object shape, 0 to 22
         * @param rotation its rotation, 0 to 3
         * @param sizeX its footprint width after rotation
         * @param sizeY its footprint height after rotation
         * @param accessFlags the definition's unrotated approach-side mask
         */
        public Target(int x, int y, int shape, int rotation, int sizeX, int sizeY, int accessFlags)
        {
            this.x = x;
            this.y = y;
            this.shape = shape;
            this.rotation = rotation & 3;
            this.sizeX = Math.max(1, sizeX);
            this.sizeY = Math.max(1, sizeY);
            this.accessFlags = accessFlags;
        }
    }

    /**
     * Whether an object can be interacted with from a tile
     * @param map the plane's collision
     * @param x the scene column stood on
     * @param y the scene row stood on
     * @param target the object
     * @return whether the server accepts the interaction from there
     */
    public static boolean reached(SceneCollision map, int x, int y, Target target)
    {
        if (x == target.x && y == target.y)
        {
            return true;
        }
        if (target.shape <= SHAPE_WALL_SQUARE_CORNER || target.shape == SHAPE_DIAGONAL_WALL)
        {
            return wall(map, x, y, target);
        }
        if (target.shape < SHAPE_DIAGONAL_WALL)
        {
            return wallDecoration(map, x, y, target);
        }
        if (hasFootprint(target.shape))
        {
            return inside(x, y, target) || rectangle(map, x, y, target);
        }
        return false;
    }

    /**
     * The tiles an object can be interacted with from that can be stood on: its footprint and the ring round it
     * @param map the plane's collision
     * @param target the object
     * @return the tiles in world coordinates, west to east then south to north
     */
    public static List<WorldPoint> tiles(SceneCollision map, Target target)
    {
        List<WorldPoint> tiles = new ArrayList<>();
        for (int x = target.x - 1; x <= target.x + target.sizeX; x++)
        {
            for (int y = target.y - 1; y <= target.y + target.sizeY; y++)
            {
                if (map.inside(x, y) && !map.blocked(x, y) && reached(map, x, y, target))
                {
                    tiles.add(map.toWorld(x, y));
                }
            }
        }
        return tiles;
    }

    /**
     * The tiles an actor can be interacted with from: straight beside its footprint where the footprint can step out,
     * and two tiles out where a counter stands between that blocks movement but not line of sight
     * @param map the plane's collision
     * @param x the actor's south-west scene column
     * @param y the actor's south-west scene row
     * @param size its width and height in tiles
     * @return the tiles in world coordinates
     */
    public static List<WorldPoint> actorTiles(SceneCollision map, int x, int y, int size)
    {
        List<WorldPoint> tiles = new ArrayList<>();
        int width = Math.max(1, size);
        for (int along = 0; along < width; along++)
        {
            for (int[] side : SIDES)
            {
                int fromX = side[0] == 0 ? x + along : side[0] > 0 ? x + width - 1 : x;
                int fromY = side[1] == 0 ? y + along : side[1] > 0 ? y + width - 1 : y;
                int toX = fromX + side[0];
                int toY = fromY + side[1];
                if (!map.inside(fromX, fromY) || !map.inside(toX, toY))
                {
                    continue;
                }

                if (!map.blocked(toX, toY))
                {
                    if ((map.flags(fromX, fromY) & edge(side[0], side[1])) == 0)
                    {
                        tiles.add(map.toWorld(toX, toY));
                    }
                    continue;
                }

                int beyondX = toX + side[0];
                int beyondY = toY + side[1];
                if (map.inside(beyondX, beyondY) && overCounter(map, fromX, fromY, toX, toY, beyondX, beyondY, side))
                {
                    tiles.add(map.toWorld(beyondX, beyondY));
                }
            }
        }
        return tiles;
    }

    /**
     * Whether an actor can be interacted with from a tile
     * @param map the plane's collision
     * @param x the actor's south-west scene column
     * @param y the actor's south-west scene row
     * @param size its width and height in tiles
     * @param point the tile stood on, in world coordinates
     * @return whether the tile is one of the actor's interaction tiles
     */
    public static boolean actorReached(SceneCollision map, int x, int y, int size, WorldPoint point)
    {
        return actorTiles(map, x, y, size).contains(point);
    }

    /**
     * Turns an approach-side mask with its object, keeping the four side bits only
     * @param flags the definition's mask for the unrotated object
     * @param rotation the placed object's rotation, 0 to 3
     * @return the blocked sides of the placed object
     */
    public static int rotatedAccess(int flags, int rotation)
    {
        int sides = flags & SIDE_BITS;
        int turn = rotation & 3;
        if (turn == 0)
        {
            return sides;
        }
        return ((sides << turn) & SIDE_BITS) | (sides >> (4 - turn));
    }

    private static boolean overCounter(SceneCollision map, int fromX, int fromY, int toX, int toY, int beyondX, int beyondY, int[] side)
    {
        if (map.blocked(beyondX, beyondY))
        {
            return false;
        }

        int counter = map.flags(toX, toY);
        if ((counter & CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL) != 0)
        {
            return false;
        }

        int sightEdge = sightEdge(side[0], side[1]);
        return (map.flags(fromX, fromY) & sightEdge) == 0 && (counter & sightEdge) == 0;
    }

    private static boolean wall(SceneCollision map, int x, int y, Target target)
    {
        int dx = target.x;
        int dy = target.y;
        switch (target.shape)
        {
            case SHAPE_WALL_STRAIGHT:
                switch (target.rotation)
                {
                    case 0:
                        return at(x, y, dx - 1, dy)
                                || at(x, y, dx, dy + 1) && open(map, x, y, SOUTH)
                                || at(x, y, dx, dy - 1) && open(map, x, y, NORTH);
                    case 1:
                        return at(x, y, dx, dy + 1)
                                || at(x, y, dx - 1, dy) && open(map, x, y, EAST)
                                || at(x, y, dx + 1, dy) && open(map, x, y, WEST);
                    case 2:
                        return at(x, y, dx + 1, dy)
                                || at(x, y, dx, dy + 1) && open(map, x, y, SOUTH)
                                || at(x, y, dx, dy - 1) && open(map, x, y, NORTH);
                    default:
                        return at(x, y, dx, dy - 1)
                                || at(x, y, dx - 1, dy) && open(map, x, y, EAST)
                                || at(x, y, dx + 1, dy) && open(map, x, y, WEST);
                }
            case SHAPE_WALL_L_CORNER:
                switch (target.rotation)
                {
                    case 0:
                        return at(x, y, dx - 1, dy)
                                || at(x, y, dx, dy + 1)
                                || at(x, y, dx + 1, dy) && open(map, x, y, WEST)
                                || at(x, y, dx, dy - 1) && open(map, x, y, NORTH);
                    case 1:
                        return at(x, y, dx - 1, dy) && open(map, x, y, EAST)
                                || at(x, y, dx, dy + 1)
                                || at(x, y, dx + 1, dy)
                                || at(x, y, dx, dy - 1) && open(map, x, y, NORTH);
                    case 2:
                        return at(x, y, dx - 1, dy) && open(map, x, y, EAST)
                                || at(x, y, dx, dy + 1) && open(map, x, y, SOUTH)
                                || at(x, y, dx + 1, dy)
                                || at(x, y, dx, dy - 1);
                    default:
                        return at(x, y, dx - 1, dy)
                                || at(x, y, dx, dy + 1) && open(map, x, y, SOUTH)
                                || at(x, y, dx + 1, dy) && open(map, x, y, WEST)
                                || at(x, y, dx, dy - 1);
                }
            case SHAPE_DIAGONAL_WALL:
                return besideWithNoWallBetween(map, x, y, dx, dy);
            default:
                return false;
        }
    }

    private static boolean wallDecoration(SceneCollision map, int x, int y, Target target)
    {
        int dx = target.x;
        int dy = target.y;
        if (target.shape == SHAPE_WALL_DECORATION_DIAGONAL_BOTH)
        {
            return besideWithNoWallBetween(map, x, y, dx, dy);
        }
        if (target.shape != SHAPE_WALL_DECORATION_DIAGONAL_OFFSET && target.shape != SHAPE_WALL_DECORATION_DIAGONAL)
        {
            return false;
        }

        int angle = target.shape == SHAPE_WALL_DECORATION_DIAGONAL ? (target.rotation + 2) & 3 : target.rotation;
        switch (angle)
        {
            case 0:
                return at(x, y, dx + 1, dy) && noWall(map, x, y, WEST)
                        || at(x, y, dx, dy - 1) && noWall(map, x, y, NORTH);
            case 1:
                return at(x, y, dx - 1, dy) && noWall(map, x, y, EAST)
                        || at(x, y, dx, dy - 1) && noWall(map, x, y, NORTH);
            case 2:
                return at(x, y, dx - 1, dy) && noWall(map, x, y, EAST)
                        || at(x, y, dx, dy + 1) && noWall(map, x, y, SOUTH);
            default:
                return at(x, y, dx + 1, dy) && noWall(map, x, y, WEST)
                        || at(x, y, dx, dy + 1) && noWall(map, x, y, SOUTH);
        }
    }

    private static boolean rectangle(SceneCollision map, int x, int y, Target target)
    {
        int access = rotatedAccess(target.accessFlags, target.rotation);
        int west = target.x;
        int south = target.y;
        int east = west + target.sizeX - 1;
        int north = south + target.sizeY - 1;
        if (x == west - 1 && y >= south && y <= north)
        {
            return noWall(map, x, y, EAST) && (access & BLOCK_ACCESS_WEST) == 0;
        }
        if (x == east + 1 && y >= south && y <= north)
        {
            return noWall(map, x, y, WEST) && (access & BLOCK_ACCESS_EAST) == 0;
        }
        if (y == south - 1 && x >= west && x <= east)
        {
            return noWall(map, x, y, NORTH) && (access & BLOCK_ACCESS_SOUTH) == 0;
        }
        if (y == north + 1 && x >= west && x <= east)
        {
            return noWall(map, x, y, SOUTH) && (access & BLOCK_ACCESS_NORTH) == 0;
        }
        return false;
    }

    private static boolean besideWithNoWallBetween(SceneCollision map, int x, int y, int dx, int dy)
    {
        return at(x, y, dx, dy + 1) && noWall(map, x, y, SOUTH)
                || at(x, y, dx, dy - 1) && noWall(map, x, y, NORTH)
                || at(x, y, dx - 1, dy) && noWall(map, x, y, EAST)
                || at(x, y, dx + 1, dy) && noWall(map, x, y, WEST);
    }

    private static boolean hasFootprint(int shape)
    {
        return shape == SHAPE_OBJECT || shape == SHAPE_OBJECT_DIAGONAL || shape == SHAPE_GROUND_DECORATION;
    }

    private static boolean inside(int x, int y, Target target)
    {
        return x >= target.x && x < target.x + target.sizeX && y >= target.y && y < target.y + target.sizeY;
    }

    private static boolean at(int x, int y, int atX, int atY)
    {
        return x == atX && y == atY;
    }

    private static boolean open(SceneCollision map, int x, int y, int wall)
    {
        return map.inside(x, y) && (map.flags(x, y) & (wall | SceneCollision.BLOCK_TILE)) == 0;
    }

    private static boolean noWall(SceneCollision map, int x, int y, int wall)
    {
        return map.inside(x, y) && (map.flags(x, y) & wall) == 0;
    }

    private static int edge(int dx, int dy)
    {
        if (dy > 0)
        {
            return NORTH;
        }
        if (dx > 0)
        {
            return EAST;
        }
        if (dy < 0)
        {
            return SOUTH;
        }
        return WEST;
    }

    private static int sightEdge(int dx, int dy)
    {
        if (dy > 0)
        {
            return CollisionDataFlag.BLOCK_LINE_OF_SIGHT_NORTH;
        }
        if (dx > 0)
        {
            return CollisionDataFlag.BLOCK_LINE_OF_SIGHT_EAST;
        }
        if (dy < 0)
        {
            return CollisionDataFlag.BLOCK_LINE_OF_SIGHT_SOUTH;
        }
        return CollisionDataFlag.BLOCK_LINE_OF_SIGHT_WEST;
    }
}

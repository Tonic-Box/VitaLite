package com.tonic.data;

import com.tonic.api.game.SceneAPI;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldPoint;

import java.util.HashSet;
import java.util.Set;

public class Walls
{
    public static Walls of(WorldPoint worldPoint)
    {
        return of(worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane());
    }

    public static Walls of(int x, int y, int plane)
    {
        return new Walls(x, y, plane);
    }

    public static Walls of(WallObject wallObject)
    {
        return new Walls(wallObject);
    }

    private final Set<Wall> walls = new HashSet<>();
    Walls(int x, int y, int plane)
    {
        process(x, y, plane);
        process(x - 1, y, plane, Wall.EAST); // West
        process(x + 1, y, plane, Wall.WEST); // East
        process(x, y - 1, plane, Wall.NORTH); // South
        process(x, y + 1, plane, Wall.SOUTH); // North
    }

    Walls(WallObject wallObject)
    {
        final int orientationA = wallObject.getOrientationA();
        final int orientationB = wallObject.getOrientationB();

        if(orientationA != 0)
        {
            walls.add(Wall.of(orientationA));
        }

        if(orientationB != 0)
        {
            walls.add(Wall.of(orientationB));
        }
    }

    private void process(int x, int y, int plane)
    {
        Tile tile = SceneAPI.getAt(x, y, plane);
        if(tile == null)
        {
            return;
        }

        if(tile.getWallObject() == null)
        {
            return;
        }

        WallObject wallObject = tile.getWallObject();
        final int orientationA = wallObject.getOrientationA();
        final int orientationB = wallObject.getOrientationB();

        if(orientationA != 0)
        {
            walls.add(Wall.of(orientationA));
        }

        if(orientationB != 0)
        {
            walls.add(Wall.of(orientationB));
        }
    }

    private void process(int x, int y, int plane, Wall wall)
    {
        if(wall == null)
        {
            return;
        }

        Tile tile = SceneAPI.getAt(x, y, plane);
        if(tile == null)
        {
            return;
        }

        if(tile.getWallObject() == null)
        {
            return;
        }

        WallObject wallObject = tile.getWallObject();
        final int orientationA = wallObject.getOrientationA();
        final int orientationB = wallObject.getOrientationB();

        if(orientationA != 0)
        {
            Wall wallA = Wall.of(orientationA);
            if(wallA == wall)
            {
                walls.add(wallA.negate());
            }
        }

        if(orientationB != 0)
        {
            Wall wallB = Wall.of(orientationB);
            if(wallB == wall)
            {
                walls.add(wallB.negate());
            }
        }
    }

    public boolean hasWall(Wall wall)
    {
        return walls.contains(wall);
    }

    public boolean hasNorthWall()
    {
        return hasWall(Wall.NORTH);
    }

    public boolean hasSouthWall()
    {
        return hasWall(Wall.SOUTH);
    }

    public boolean hasEastWall()
    {
        return hasWall(Wall.EAST);
    }

    public boolean hasWestWall()
    {
        return hasWall(Wall.WEST);
    }

    @RequiredArgsConstructor
    public enum Wall {
        WEST(1),
        EAST(4),
        NORTH(2),
        SOUTH(8)
        ;

        private final int orientation;

        public Wall negate()
        {
            switch (this) {
                case NORTH:
                    return SOUTH;
                case SOUTH:
                    return NORTH;
                case EAST:
                    return WEST;
                case WEST:
                    return EAST;
                default:
                    throw new IllegalArgumentException();
            }
        }

        public static Wall of(int orientation)
        {
            for(Wall wall : values())
            {
                if(wall.orientation == orientation)
                {
                    return wall;
                }
            }
            return null;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("Walls: ");
        for(Wall wall : walls)
        {
            sb.append(wall.name()).append(", ");
        }
        return sb.toString();
    }
}

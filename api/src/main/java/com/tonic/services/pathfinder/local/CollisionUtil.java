package com.tonic.services.pathfinder.local;

import com.tonic.util.WorldPointUtil;
import gnu.trove.map.hash.TIntIntHashMap;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class CollisionUtil
{
    public static final Predicate<Set<MovementFlag>> FULL_BLOCKING = flags -> flags.contains(MovementFlag.BLOCK_MOVEMENT_FULL) || flags.contains(MovementFlag.BLOCK_MOVEMENT_OBJECT) || flags.contains(MovementFlag.BLOCK_MOVEMENT_FLOOR) || flags.contains(MovementFlag.BLOCK_MOVEMENT_FLOOR_DECORATION);
    public static final Predicate<Set<MovementFlag>> BLOCKED_NORTH = flags -> FULL_BLOCKING.test(flags) || flags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH) || flags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH_EAST) || flags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH_WEST);
    public static final Predicate<Set<MovementFlag>> BLOCKED_EAST = flags -> FULL_BLOCKING.test(flags) || flags.contains(MovementFlag.BLOCK_MOVEMENT_EAST) || flags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH_EAST) || flags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH_EAST);
    public static final Predicate<Set<MovementFlag>> BLOCKED_SOUTH = flags -> FULL_BLOCKING.test(flags) || flags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH) || flags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH_EAST) || flags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH_WEST);
    public static final Predicate<Set<MovementFlag>> BLOCKED_WEST = flags -> FULL_BLOCKING.test(flags) || flags.contains(MovementFlag.BLOCK_MOVEMENT_WEST) || flags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH_WEST) || flags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH_WEST);

    private final TIntIntHashMap collisionMap;

    public CollisionUtil(TIntIntHashMap collisionMap)
    {
        this.collisionMap = collisionMap;
    }

    public boolean blockedNorth(int x, int y)
    {
        return BLOCKED_NORTH.test(getFlags(x, y)) || BLOCKED_SOUTH.test(getFlags(x, y + 1));
    }

    public boolean blockedEast(int x, int y)
    {
        return BLOCKED_EAST.test(getFlags(x, y)) || BLOCKED_WEST.test(getFlags(x + 1, y));
    }

    public boolean blockedSouth(int x, int y)
    {
        return BLOCKED_SOUTH.test(getFlags(x, y)) || BLOCKED_NORTH.test(getFlags(x, y - 1));
    }

    public boolean blockedWest(int x, int y)
    {
        return BLOCKED_WEST.test(getFlags(x, y)) || BLOCKED_EAST.test(getFlags(x - 1, y));
    }

    public boolean blockedNorthEast(int x, int y)
    {
        return blockedNorth(x, y) || blockedEast(x, y) || blockedSouth(x, y + 1) || blockedWest(x + 1, y) || blockedSouth(x + 1, y + 1) || blockedWest(x + 1, y + 1);
    }

    public boolean blockedSouthEast(int x, int y)
    {
        return blockedSouth(x, y) || blockedEast(x, y) || blockedNorth(x, y - 1) || blockedWest(x + 1, y) || blockedNorth(x + 1, y - 1) || blockedWest(x + 1, y - 1);
    }

    public boolean blockedSouthWest(int x, int y)
    {
        return blockedSouth(x, y) || blockedWest(x, y) || blockedNorth(x, y - 1) || blockedEast(x - 1, y) || blockedNorth(x - 1, y - 1) || blockedEast(x - 1, y - 1);
    }

    public boolean blockedNorthWest(int x, int y)
    {
        return blockedNorth(x, y) || blockedWest(x, y) || blockedSouth(x, y + 1) || blockedEast(x - 1, y) || blockedSouth(x - 1, y + 1) || blockedEast(x - 1, y + 1);
    }

    private Set<MovementFlag> getFlags(int x, int y)
    {
        return MovementFlag.getSetFlags(collisionMap.get(x << 14 | y));
    }
}
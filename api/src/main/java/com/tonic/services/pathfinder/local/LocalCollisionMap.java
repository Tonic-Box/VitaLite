package com.tonic.services.pathfinder.local;

import com.tonic.Static;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.services.GameManager;
import com.tonic.services.pathfinder.Walker;
import com.tonic.util.WorldPointUtil;
import gnu.trove.map.hash.TIntIntHashMap;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LocalCollisionMap
{
    public static final Predicate<Set<MovementFlag>> FULL_BLOCKING = flags -> flags.contains(MovementFlag.BLOCK_MOVEMENT_FULL) || flags.contains(MovementFlag.BLOCK_MOVEMENT_OBJECT) || flags.contains(MovementFlag.BLOCK_MOVEMENT_FLOOR) || flags.contains(MovementFlag.BLOCK_MOVEMENT_FLOOR_DECORATION);
    public static final Predicate<Set<MovementFlag>> BLOCKED_NORTH = flags -> FULL_BLOCKING.test(flags) || flags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH) || flags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH_EAST) || flags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH_WEST);
    public static final Predicate<Set<MovementFlag>> BLOCKED_EAST = flags -> FULL_BLOCKING.test(flags) || flags.contains(MovementFlag.BLOCK_MOVEMENT_EAST) || flags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH_EAST) || flags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH_EAST);
    public static final Predicate<Set<MovementFlag>> BLOCKED_SOUTH = flags -> FULL_BLOCKING.test(flags) || flags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH) || flags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH_EAST) || flags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH_WEST);
    public static final Predicate<Set<MovementFlag>> BLOCKED_WEST = flags -> FULL_BLOCKING.test(flags) || flags.contains(MovementFlag.BLOCK_MOVEMENT_WEST) || flags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH_WEST) || flags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH_WEST);

    private final TIntIntHashMap collisionMap;
    @Getter
    private final List<Integer> ignoreTiles;
    public LocalCollisionMap()
    {
        this.collisionMap = getCollision();
        this.ignoreTiles = getDoored();
    }

    public LocalCollisionMap(boolean ignoreDoors)
    {
        this.collisionMap = getCollision();
        this.ignoreTiles = ignoreDoors ? getDoored() : new ArrayList<>();
    }

    public byte all(short x, short y, byte z)
    {
        if(x < 6000)
        {
            return Walker.getCollisionMap().all(x, y, z);
        }

        byte n = (byte) (n(x,y,z) ? 0 : 1);
        byte e = (byte) (e(x,y,z) ? 0 : 1);
        byte s = (byte) (s(x,y,z) ? 0 : 1);
        byte w = (byte) (w(x,y,z) ? 0 : 1);
        if((n | e | s | w) == 0)
        {
            return 0;
        }

        byte sw = (byte) (sw(x,y,z) ? 0 : 1);
        byte se = (byte) (se(x,y,z) ? 0 : 1);
        byte nw = (byte) (nw(x,y,z) ? 0 : 1);
        byte ne = (byte) (ne(x,y,z) ? 0 : 1);

        return (byte) (nw | (n << 1) | (ne << 2) | (w << 3) | (e << 4) | (sw << 5) | (s << 6) | (se << 7));
    }

    public boolean n(int x, int y, int z)
    {
        if(isDoored(x, y, z) || isDoored(x, y + 1, z))
            return false;
        return BLOCKED_NORTH.test(getFlags(x, y, z)) || BLOCKED_SOUTH.test(getFlags(x, y + 1, z));
    }

    public boolean e(int x, int y, int z)
    {
        if (isDoored(x, y, z) || isDoored(x + 1, y, z))
            return false;
        return BLOCKED_EAST.test(getFlags(x, y, z)) || BLOCKED_WEST.test(getFlags(x + 1, y, z));
    }

    public boolean s(int x, int y, int z)
    {
        if (isDoored(x, y, z) || isDoored(x, y - 1, z))
            return false;
        return BLOCKED_SOUTH.test(getFlags(x, y, z)) || BLOCKED_NORTH.test(getFlags(x, y - 1, z));
    }

    public boolean w(int x, int y, int z)
    {
        if (isDoored(x, y, z) || isDoored(x - 1, y, z))
            return false;
        return BLOCKED_WEST.test(getFlags(x, y, z)) || BLOCKED_EAST.test(getFlags(x - 1, y, z));
    }

    public boolean ne(int x, int y, int z)
    {
        return n(x, y, z) || e(x, y, z) || s(x, y + 1, z) || w(x + 1, y, z) || s(x + 1, y + 1, z) || w(x + 1, y + 1, z);
    }

    public boolean se(int x, int y, int z)
    {
        return s(x, y, z) || e(x, y, z) || n(x, y - 1, z) || w(x + 1, y, z) || n(x + 1, y - 1, z) || w(x + 1, y - 1, z);
    }

    public boolean sw(int x, int y, int z)
    {
        return s(x, y, z) || w(x, y, z) || n(x, y - 1, z) || e(x - 1, y, z) || n(x - 1, y - 1, z) || e(x - 1, y - 1, z);
    }

    public boolean nw(int x, int y, int z)
    {
        return n(x, y, z) || w(x, y, z) || s(x, y + 1, z) || e(x - 1, y, z) || s(x - 1, y + 1, z) || e(x - 1, y + 1, z);
    }

    private Set<MovementFlag> getFlags(int x, int y, int z)
    {
        int compressed = WorldPointUtil.compress(x, y, z);
        if(!collisionMap.contains(compressed))
        {
            Set<MovementFlag> flags = new HashSet<>();
            flags.add(MovementFlag.BLOCK_MOVEMENT_FULL);
            return flags;
        }
        return MovementFlag.getSetFlags(collisionMap.get(WorldPointUtil.compress(x, y, z)));
    }

    private boolean isDoored(int x, int y, int z)
    {
        int packed = WorldPointUtil.compress(x, y, z);
        return ignoreTiles.contains(packed);
    }

    private List<Integer> getDoored()
    {
        return Static.invoke(() ->
                GameManager.objectStream()
                        .filter(o -> {
                            String name = o.getName() == null ? "" :  o.getName().toLowerCase();
                            return (name.contains("door") || name.contains("gate")) && !name.contains("trapdoor");
                        })
                        .map(door -> WorldPointUtil.compress(door.getWorldPoint().getX(), door.getWorldPoint().getY(), door.getWorldPoint().getPlane()))
                        .collect(Collectors.toList())
        );
    }

    private TIntIntHashMap getCollision()
    {
        TIntIntHashMap collisionMap = new TIntIntHashMap();
        WorldView wv = PlayerEx.getLocal().getWorldView();
        if(wv.getCollisionMaps() == null || wv.getCollisionMaps()[wv.getPlane()] == null)
            return collisionMap;

        int[][] flags = wv.getCollisionMaps()[wv.getPlane()].getFlags();
        WorldPoint point;
        for(int x = 0; x < flags.length; x++)
        {
            for(int y = 0; y < flags[x].length; y++)
            {
                point = WorldPoint.fromScene(wv, x, y, wv.getPlane());
                collisionMap.put(WorldPointUtil.compress(point.getX(), point.getY(), wv.getPlane()), flags[x][y]);
            }
        }
        return collisionMap;
    }

    public static boolean canStep(int x, int y, int plane) {
        WorldView wv = PlayerEx.getLocal().getWorldView();
        if (wv.getCollisionMaps() == null || wv.getCollisionMaps()[plane] == null)
            return false;
        int sceneX = x - wv.getBaseX();
        int SceneY = y - wv.getBaseY();
        if(
                sceneX < 0 || SceneY < 0 || sceneX >= wv.getCollisionMaps()[plane].getFlags().length || SceneY >= wv.getCollisionMaps()[plane].getFlags()[0].length)
            return false;
        return (wv.getCollisionMaps()[plane].getFlags()[sceneX][SceneY] & MovementFlag.BLOCKING_FLAGS) == 0;
    }
}
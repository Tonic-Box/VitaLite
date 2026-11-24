package com.tonic.services.pathfinder.impl;

import com.tonic.api.TObjectComposition;
import com.tonic.api.game.reach.SceneCollision;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.collision.Exclusion;
import com.tonic.services.pathfinder.collision.Flags;
import com.tonic.util.WorldPointUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * The loaded scene as the pathfinder sees it: its tiles with the scene's own collision, doors and gates left passable as the walker opens them on the way but crossed straight, and, in an instance, where its tiles were copied from in the world. Tiles the layer does not cover are planned over the static world map.
 */
public final class LiveLayer
{
    /**
     * No scene: every tile is planned over the static world map.
     */
    public static final LiveLayer NONE = new LiveLayer(0, 0, 0, 0, Margins.NONE, new byte[0][], new boolean[0][], InstanceMap.NONE, null);

    private static final int PLANES = 4;
    private static final int[] DX = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DY = {1, 1, 1, 0, 0, -1, -1, -1};
    private static final int[] FLAG = {Flags.NORTHWEST, Flags.NORTH, Flags.NORTHEAST, Flags.WEST, Flags.EAST, Flags.SOUTHWEST, Flags.SOUTH, Flags.SOUTHEAST};

    private final int baseX;
    private final int baseY;
    private final int sizeX;
    private final int sizeY;
    private final Margins margins;
    private final byte[][] flags;
    private final boolean[][] solid;
    private final InstanceMap map;
    private final WorldPoint origin;

    private LiveLayer(int baseX, int baseY, int sizeX, int sizeY, Margins margins, byte[][] flags, boolean[][] solid, InstanceMap map, WorldPoint origin)
    {
        this.baseX = baseX;
        this.baseY = baseY;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.margins = margins;
        this.flags = flags;
        this.solid = solid;
        this.map = map;
        this.origin = origin;
    }

    /**
     * Takes the top-level scene as it stands now. Call it on the client thread.
     *
     * @param client the client
     * @return the layer, or NONE without a scene
     */
    public static LiveLayer of(Client client)
    {
        WorldView view = client == null ? null : client.getTopLevelWorldView();
        Scene scene = view == null ? null : view.getScene();
        if (scene == null || view.getCollisionMaps() == null)
        {
            return NONE;
        }

        int sizeX = view.getSizeX();
        int sizeY = view.getSizeY();
        LiveCollision[] collision = new LiveCollision[PLANES];
        Margins margins = Margins.NONE;
        for (int plane = 0; plane < PLANES; plane++)
        {
            int[][] copy = copyFlags(SceneCollision.of(view, plane), sizeX, sizeY);
            margins = plane == 0 ? Margins.of(copy, sizeX, sizeY) : margins;
            collision[plane] = new LiveCollision(copy);
        }

        List<int[]> doorways = new ArrayList<>();
        takeOutPassableLocs(client, scene, collision, doorways, sizeX, sizeY);

        byte[][] flags = new byte[PLANES][];
        boolean[][] solid = new boolean[PLANES][];
        for (int plane = 0; plane < PLANES; plane++)
        {
            flags[plane] = directions(collision[plane], sizeX, sizeY);
            solid[plane] = solid(collision[plane], sizeX, sizeY);
        }
        for (int[] doorway : doorways)
        {
            for (int x = doorway[1]; x < doorway[1] + doorway[3]; x++)
            {
                for (int y = doorway[2]; y < doorway[2] + doorway[4]; y++)
                {
                    straightenTile(flags[doorway[0]], sizeX, sizeY, x, y);
                }
            }
        }

        InstanceMap map = InstanceMap.of(view);
        if (!map.instanced())
        {
            shutBlockedTiles(flags, solid, view.getBaseX(), view.getBaseY(), sizeX, sizeY);
        }
        Player local = client.getLocalPlayer();
        WorldPoint origin = local == null ? null : local.getWorldLocation();
        return new LiveLayer(view.getBaseX(), view.getBaseY(), sizeX, sizeY, map.instanced() ? Margins.NONE : margins, flags, solid, map, origin);
    }

    /**
     * How far in from each edge of the scene the layer starts outside an instance. The client blocks a border of rows at the scene's edges whatever stands on them, wider on some edges than others and varying with how much scene is loaded, so the border is measured: the rows blocked all the way across, and the row beside them, whose steps into the border read as blocked, are left to the static map, which knows the world beyond.
     */
    private static final class Margins
    {
        static final Margins NONE = new Margins(0, 0, 0, 0);

        final int west;
        final int south;
        final int east;
        final int north;

        Margins(int west, int south, int east, int north)
        {
            this.west = west;
            this.south = south;
            this.east = east;
            this.north = north;
        }

        static Margins of(int[][] flags, int sizeX, int sizeY)
        {
            int west = 0;
            while (west < sizeX / 2 && columnBlocked(flags, west, sizeY))
            {
                west++;
            }
            int east = 0;
            while (east < sizeX / 2 && columnBlocked(flags, sizeX - 1 - east, sizeY))
            {
                east++;
            }
            int south = 0;
            while (south < sizeY / 2 && rowBlocked(flags, south, sizeX))
            {
                south++;
            }
            int north = 0;
            while (north < sizeY / 2 && rowBlocked(flags, sizeY - 1 - north, sizeX))
            {
                north++;
            }
            return new Margins(west + 1, south + 1, east + 1, north + 1);
        }

        private static boolean columnBlocked(int[][] flags, int x, int sizeY)
        {
            for (int y = 0; y < sizeY; y++)
            {
                if (flags[x][y] != LiveCollision.BLOCK_FULL)
                {
                    return false;
                }
            }
            return true;
        }

        private static boolean rowBlocked(int[][] flags, int y, int sizeX)
        {
            for (int x = 0; x < sizeX; x++)
            {
                if (flags[x][y] != LiveCollision.BLOCK_FULL)
                {
                    return false;
                }
            }
            return true;
        }
    }

    private static int[][] copyFlags(SceneCollision source, int sizeX, int sizeY)
    {
        int[][] copy = new int[sizeX][sizeY];
        for (int x = 0; x < sizeX; x++)
        {
            for (int y = 0; y < sizeY; y++)
            {
                copy[x][y] = source == null ? LiveCollision.BLOCK_FULL : source.flags(x, y);
            }
        }
        return copy;
    }

    /**
     * Takes out of the copies the walls, objects and floor decorations of every colliding loc the walker passes through, and notes the footprint of every loc it passes through, colliding or not, as {plane, x, y, width, height} so its doorway can be crossed straight. A tile's index in the scene is the plane its locs collide on, bridges having been moved down by the client.
     */
    private static void takeOutPassableLocs(Client client, Scene scene, LiveCollision[] collision, List<int[]> doorways, int sizeX, int sizeY)
    {
        Tile[][][] tiles = scene.getTiles();
        for (int plane = 0; plane < Math.min(PLANES, tiles.length); plane++)
        {
            for (int x = 0; x < Math.min(sizeX, tiles[plane].length); x++)
            {
                for (int y = 0; y < Math.min(sizeY, tiles[plane][x].length); y++)
                {
                    Tile tile = tiles[plane][x][y];
                    if (tile == null)
                    {
                        continue;
                    }
                    WallObject wall = tile.getWallObject();
                    ObjectComposition wallDefinition = wall == null ? null : definition(client, wall.getId());
                    if (wall != null && !Exclusion.blocks(wallDefinition, wall.getId()))
                    {
                        if (collides(wallDefinition))
                        {
                            int config = wall.getConfig();
                            collision[plane].removeWall(x, y, config & 0x1F, (config >>> 6) & 3);
                        }
                        doorways.add(new int[] {plane, x, y, 1, 1});
                    }
                    GroundObject ground = tile.getGroundObject();
                    ObjectComposition groundDefinition = ground == null ? null : definition(client, ground.getId());
                    if (ground != null && !Exclusion.blocks(groundDefinition, ground.getId()))
                    {
                        if (collides(groundDefinition))
                        {
                            collision[plane].removeFloorDecoration(x, y);
                        }
                        doorways.add(new int[] {plane, x, y, 1, 1});
                    }
                    GameObject[] objects = tile.getGameObjects();
                    if (objects == null)
                    {
                        continue;
                    }
                    for (GameObject object : objects)
                    {
                        if (object == null || object.getId() < 0 || object.getRenderable() instanceof Actor)
                        {
                            continue;
                        }
                        Point min = object.getSceneMinLocation();
                        Point max = object.getSceneMaxLocation();
                        if (min == null || max == null || min.getX() != x || min.getY() != y)
                        {
                            continue;
                        }
                        ObjectComposition objectDefinition = definition(client, object.getId());
                        if (Exclusion.blocks(objectDefinition, object.getId()))
                        {
                            continue;
                        }
                        int width = max.getX() - min.getX() + 1;
                        int height = max.getY() - min.getY() + 1;
                        if (collides(objectDefinition))
                        {
                            collision[plane].removeObject(x, y, width, height);
                        }
                        doorways.add(new int[] {plane, x, y, width, height});
                    }
                }
            }
        }
    }

    /**
     * @return the loc's definition, transformed where it transforms, or null when there is none
     */
    private static ObjectComposition definition(Client client, int id)
    {
        ObjectComposition definition = client.getObjectDefinition(id);
        if (definition != null && definition.getImpostorIds() != null)
        {
            ObjectComposition impostor = definition.getImpostor();
            if (impostor != null)
            {
                definition = impostor;
            }
        }
        return definition;
    }

    /**
     * @return whether the loc added collision when it was placed, so there is collision of its to take out
     */
    private static boolean collides(ObjectComposition definition)
    {
        return definition != null && ((TObjectComposition) definition).getInteractType() != 0;
    }

    /**
     * Shuts the tiles Exclusion blocks that the scene holds: no step off them, none onto them, and none stood on.
     */
    private static void shutBlockedTiles(byte[][] flags, boolean[][] solid, int baseX, int baseY, int sizeX, int sizeY)
    {
        for (int[] tile : Exclusion.BLOCKED_TILES)
        {
            int x = tile[0] - baseX;
            int y = tile[1] - baseY;
            int plane = tile[2];
            if (x < 0 || y < 0 || x >= sizeX || y >= sizeY || plane < 0 || plane >= flags.length)
            {
                continue;
            }
            flags[plane][x * sizeY + y] = 0;
            solid[plane][x * sizeY + y] = true;
            for (int direction = 0; direction < DX.length; direction++)
            {
                int fromX = x + DX[direction];
                int fromY = y + DY[direction];
                if (fromX >= 0 && fromY >= 0 && fromX < sizeX && fromY < sizeY)
                {
                    flags[plane][fromX * sizeY + fromY] &= (byte) ~FLAG[DX.length - 1 - direction];
                }
            }
        }
    }

    /**
     * Clears a tile's diagonal steps and the diagonal steps onto it from the tiles those come from, so a route crosses a doorway straight on, as the server walks it.
     */
    private static void straightenTile(byte[] plane, int sizeX, int sizeY, int x, int y)
    {
        if (x < 0 || y < 0 || x >= sizeX || y >= sizeY)
        {
            return;
        }
        for (int direction = 0; direction < DX.length; direction++)
        {
            if (DX[direction] == 0 || DY[direction] == 0)
            {
                continue;
            }
            plane[x * sizeY + y] &= (byte) ~FLAG[direction];
            int fromX = x + DX[direction];
            int fromY = y + DY[direction];
            if (fromX >= 0 && fromY >= 0 && fromX < sizeX && fromY < sizeY)
            {
                plane[fromX * sizeY + fromY] &= (byte) ~FLAG[DX.length - 1 - direction];
            }
        }
    }

    private static byte[] directions(LiveCollision collision, int sizeX, int sizeY)
    {
        byte[] out = new byte[sizeX * sizeY];
        for (int x = 0; x < sizeX; x++)
        {
            for (int y = 0; y < sizeY; y++)
            {
                int open = 0;
                for (int direction = 0; direction < DX.length; direction++)
                {
                    if (collision.canMove(x, y, DX[direction], DY[direction]))
                    {
                        open |= FLAG[direction];
                    }
                }
                out[x * sizeY + y] = (byte) open;
            }
        }
        return out;
    }

    /**
     * Which tiles a loc or the floor fills, so none can be stood on. Their steps off are kept, as the collision gives them, so a walk can still start from one the server set the avatar down on.
     */
    private static boolean[] solid(LiveCollision collision, int sizeX, int sizeY)
    {
        boolean[] out = new boolean[sizeX * sizeY];
        for (int x = 0; x < sizeX; x++)
        {
            for (int y = 0; y < sizeY; y++)
            {
                out[x * sizeY + y] = collision.blocked(x, y);
            }
        }
        return out;
    }

    /**
     * @param x the world x
     * @param y the world y
     * @param plane the plane
     * @return whether the layer plans this tile, rather than the static map
     */
    public boolean covers(int x, int y, int plane)
    {
        int localX = x - baseX;
        int localY = y - baseY;
        return plane >= 0 && plane < flags.length && localX >= margins.west && localY >= margins.south && localX < sizeX - margins.east && localY < sizeY - margins.north;
    }

    /**
     * @param packed a tile key
     * @return whether the layer plans this tile
     */
    public boolean covers(int packed)
    {
        return covers(WorldPointUtil.getCompressedX(packed), WorldPointUtil.getCompressedY(packed), WorldPointUtil.getCompressedPlane(packed));
    }

    /**
     * @param x the world x of a tile the layer covers
     * @param y the world y
     * @param plane the plane
     * @return the directions a step may be taken in, as Flags
     */
    public byte all(int x, int y, int plane)
    {
        return flags[plane][(x - baseX) * sizeY + (y - baseY)];
    }

    /**
     * Whether a tile can be stood on and left: over the scene where the layer covers it, else over the static map. A tile a loc fills is not, though the scene's collision gives it steps off.
     *
     * @param packed a tile key
     * @return whether it can be walked to
     */
    public boolean walkable(int packed)
    {
        if (!covers(packed))
        {
            return Walker.getCollisionMap().walkable(packed);
        }
        int x = WorldPointUtil.getCompressedX(packed);
        int y = WorldPointUtil.getCompressedY(packed);
        int plane = WorldPointUtil.getCompressedPlane(packed);
        return !solid[plane][(x - baseX) * sizeY + (y - baseY)] && all(x, y, plane) != 0;
    }

    /**
     * @return whether the scene is an instance, whose tiles were copied from elsewhere in the world
     */
    public boolean instanced()
    {
        return map.instanced();
    }

    /**
     * Where a tile of the layer was copied from, for finding what the world keyed there, such as a transport.
     *
     * @param packed a tile key the layer covers
     * @return the key of the world tile it was copied from, the key itself outside an instance, or -1 when it was not copied
     */
    public int templateKey(int packed)
    {
        if (!map.instanced())
        {
            return packed;
        }
        WorldPoint template = map.toTemplate(WorldPointUtil.fromCompressed(packed));
        return template == null ? -1 : WorldPointUtil.compress(template);
    }

    /**
     * Where a world tile was copied to in the layer.
     *
     * @param templateKey a world tile's key
     * @return the keys of its copies the layer covers, or the key itself when it has none or outside an instance
     */
    public IntArrayList liveKeys(int templateKey)
    {
        IntArrayList keys = new IntArrayList();
        if (map.instanced())
        {
            for (WorldPoint copy : map.toLive(WorldPointUtil.fromCompressed(templateKey)))
            {
                if (covers(copy.getX(), copy.getY(), copy.getPlane()))
                {
                    keys.add(WorldPointUtil.compress(copy));
                }
            }
        }
        if (keys.isEmpty())
        {
            keys.add(templateKey);
        }
        return keys;
    }

    /**
     * Where a planned tile stands in the scene: its copy nearest where the player stood when the layer was taken, on the same plane first, inside an instance, else the tile itself.
     *
     * @param packed a planned tile's key, a world tile or one of the layer's own
     * @return the tile to walk to
     */
    public WorldPoint live(int packed)
    {
        WorldPoint tile = WorldPointUtil.fromCompressed(packed);
        if (!map.instanced() || covers(packed))
        {
            return tile;
        }
        WorldPoint copy = map.nearestLive(tile, origin);
        return copy == null ? tile : copy;
    }
}

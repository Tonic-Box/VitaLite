package com.tonic.services.pathfinder.impl;

import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where the tiles of an instanced scene were copied from in the world, and where world tiles were copied to in it, taken from the scene's template chunks at one moment so it can be read from any thread. Outside an instance every tile is its own template.
 */
public final class InstanceMap
{
    /**
     * The map of a scene that was not copied from elsewhere: every tile is its own template.
     */
    public static final InstanceMap NONE = new InstanceMap(0, 0, null);

    private static final int CHUNK_SIZE = 8;
    private static final int CHUNK_MASK = CHUNK_SIZE - 1;

    private final int baseX;
    private final int baseY;
    private final int[][][] chunks;

    private InstanceMap(int baseX, int baseY, int[][][] chunks)
    {
        this.baseX = baseX;
        this.baseY = baseY;
        this.chunks = chunks;
    }

    /**
     * Takes a world view's template chunks. Call it on the client thread.
     *
     * @param view the world view, or null
     * @return its map, or NONE when it is not an instance
     */
    public static InstanceMap of(WorldView view)
    {
        if (view == null || !view.isInstance() || view.getInstanceTemplateChunks() == null)
        {
            return NONE;
        }
        int[][][] source = view.getInstanceTemplateChunks();
        int[][][] copy = new int[source.length][][];
        for (int plane = 0; plane < source.length; plane++)
        {
            copy[plane] = new int[source[plane].length][];
            for (int x = 0; x < source[plane].length; x++)
            {
                copy[plane][x] = source[plane][x].clone();
            }
        }
        return new InstanceMap(view.getBaseX(), view.getBaseY(), copy);
    }

    /**
     * @return whether the scene is an instance
     */
    public boolean instanced()
    {
        return chunks != null;
    }

    /**
     * Where a tile of the scene was copied from.
     *
     * @param live a tile in the scene's coordinates
     * @return the world tile it is a copy of; the tile itself outside an instance; null for an instance tile that lies outside the scene or was not copied from anywhere
     */
    public WorldPoint toTemplate(WorldPoint live)
    {
        if (live == null || chunks == null)
        {
            return live;
        }
        int sceneX = live.getX() - baseX;
        int sceneY = live.getY() - baseY;
        int plane = live.getPlane();
        if (sceneX < 0 || sceneY < 0 || plane < 0 || plane >= chunks.length || sceneX / CHUNK_SIZE >= chunks[plane].length || sceneY / CHUNK_SIZE >= chunks[plane][sceneX / CHUNK_SIZE].length)
        {
            return null;
        }
        int chunk = chunks[plane][sceneX / CHUNK_SIZE][sceneY / CHUNK_SIZE];
        if (chunk == -1)
        {
            return null;
        }
        int rotation = chunk >> 1 & 0x3;
        int templateY = (chunk >> 3 & 0x7FF) * CHUNK_SIZE;
        int templateX = (chunk >> 14 & 0x3FF) * CHUNK_SIZE;
        int templatePlane = chunk >> 24 & 0x3;
        int[] local = inverse(sceneX & CHUNK_MASK, sceneY & CHUNK_MASK, rotation);
        return new WorldPoint(templateX + local[0], templateY + local[1], templatePlane);
    }

    /**
     * Where a world tile was copied to in the scene.
     *
     * @param template the world tile
     * @return every copy of it in the scene; the tile itself outside an instance; empty for null or a tile not copied here
     */
    public List<WorldPoint> toLive(WorldPoint template)
    {
        if (template == null)
        {
            return Collections.emptyList();
        }
        if (chunks == null)
        {
            return Collections.singletonList(template);
        }
        List<WorldPoint> copies = new ArrayList<>();
        for (int plane = 0; plane < chunks.length; plane++)
        {
            for (int x = 0; x < chunks[plane].length; x++)
            {
                for (int y = 0; y < chunks[plane][x].length; y++)
                {
                    int chunk = chunks[plane][x][y];
                    if (chunk == -1)
                    {
                        continue;
                    }
                    int templateY = (chunk >> 3 & 0x7FF) * CHUNK_SIZE;
                    int templateX = (chunk >> 14 & 0x3FF) * CHUNK_SIZE;
                    int templatePlane = chunk >> 24 & 0x3;
                    if (templatePlane != template.getPlane() || template.getX() < templateX || template.getX() >= templateX + CHUNK_SIZE || template.getY() < templateY || template.getY() >= templateY + CHUNK_SIZE)
                    {
                        continue;
                    }
                    int[] local = forward(template.getX() & CHUNK_MASK, template.getY() & CHUNK_MASK, chunk >> 1 & 0x3);
                    copies.add(new WorldPoint(baseX + x * CHUNK_SIZE + local[0], baseY + y * CHUNK_SIZE + local[1], plane));
                }
            }
        }
        return copies;
    }

    /**
     * The copy of a world tile nearest a tile of the scene, on the same plane first.
     *
     * @param template the world tile
     * @param from where to measure from, or null for the first copy
     * @return the nearest copy; the tile itself outside an instance; null when it was not copied here
     */
    public WorldPoint nearestLive(WorldPoint template, WorldPoint from)
    {
        WorldPoint best = null;
        for (WorldPoint copy : toLive(template))
        {
            if (best == null || from != null && closer(copy, best, from))
            {
                best = copy;
            }
        }
        return best;
    }

    private static boolean closer(WorldPoint candidate, WorldPoint best, WorldPoint from)
    {
        boolean candidateLevel = candidate.getPlane() == from.getPlane();
        boolean bestLevel = best.getPlane() == from.getPlane();
        if (candidateLevel != bestLevel)
        {
            return candidateLevel;
        }
        return candidate.distanceTo2D(from) < best.distanceTo2D(from);
    }

    private static int[] forward(int x, int y, int rotation)
    {
        switch (rotation & 3)
        {
            case 1:
                return new int[] {y, CHUNK_MASK - x};
            case 2:
                return new int[] {CHUNK_MASK - x, CHUNK_MASK - y};
            case 3:
                return new int[] {CHUNK_MASK - y, x};
            default:
                return new int[] {x, y};
        }
    }

    private static int[] inverse(int x, int y, int rotation)
    {
        switch (rotation & 3)
        {
            case 1:
                return new int[] {CHUNK_MASK - y, x};
            case 2:
                return new int[] {CHUNK_MASK - x, CHUNK_MASK - y};
            case 3:
                return new int[] {y, CHUNK_MASK - x};
            default:
                return new int[] {x, y};
        }
    }
}

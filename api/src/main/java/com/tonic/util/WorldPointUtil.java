package com.tonic.util;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;

import static net.runelite.api.Constants.*;

public class WorldPointUtil {
    public static WorldPoint get(WorldPoint worldPoint)
    {
        return fromInstance(worldPoint);
    }

    public static WorldPoint translate(WorldPoint worldPoint)
    {
        return toInstance(worldPoint).get(0);
    }

    /**
     * Gets the coordinate of the tile that contains the passed world point,
     * accounting for instances.
     *
     * @param worldPoint the instance worldpoint
     * @return the tile coordinate containing the local point
     */
    private static WorldPoint fromInstance(WorldPoint worldPoint)
    {
        //get local
        Client client = Static.getClient();
        WorldView worldView = client.getTopLevelWorldView();
        LocalPoint localPoint = LocalPoint.fromWorld(worldView, worldPoint);

        // if local point is null or not in an instanced region, return the world point as is
        if(localPoint == null || !worldView.isInstance())
            return worldPoint;

        // get position in the scene
        int sceneX = localPoint.getSceneX();
        int sceneY = localPoint.getSceneY();

        // get chunk from scene
        int chunkX = sceneX / CHUNK_SIZE;
        int chunkY = sceneY / CHUNK_SIZE;

        // get the template chunk for the chunk
        int[][][] instanceTemplateChunks = worldView.getInstanceTemplateChunks();
        int templateChunk = instanceTemplateChunks[worldPoint.getPlane()][chunkX][chunkY];

        int rotation = templateChunk >> 1 & 0x3;
        int templateChunkY = (templateChunk >> 3 & 0x7FF) * CHUNK_SIZE;
        int templateChunkX = (templateChunk >> 14 & 0x3FF) * CHUNK_SIZE;
        int templateChunkPlane = templateChunk >> 24 & 0x3;

        // calculate world point of the template
        int x = templateChunkX + (sceneX & (CHUNK_SIZE - 1));
        int y = templateChunkY + (sceneY & (CHUNK_SIZE - 1));

        // create and rotate point back to 0, to match with template
        return rotate(new WorldPoint(x, y, templateChunkPlane), 4 - rotation);
    }

    public static ArrayList<WorldPoint> toInstance(WorldPoint worldPoint)
    {
        Client client = Static.getClient();
        WorldView worldView = client.getTopLevelWorldView();

        // if not in an instanced region, return the world point as is
        if (!worldView.isInstance())
        {
            return new ArrayList<>(Collections.singletonList(worldPoint));
        }

        // find instance chunks using the template point. there might be more than one.
        ArrayList<WorldPoint> worldPoints = new ArrayList<>();
        int[][][] instanceTemplateChunks = worldView.getInstanceTemplateChunks();
        for (int z = 0; z < instanceTemplateChunks.length; z++)
        {
            for (int x = 0; x < instanceTemplateChunks[z].length; ++x)
            {
                for (int y = 0; y < instanceTemplateChunks[z][x].length; ++y)
                {
                    int chunkData = instanceTemplateChunks[z][x][y];
                    int rotation = chunkData >> 1 & 0x3;
                    int templateChunkY = (chunkData >> 3 & 0x7FF) * CHUNK_SIZE;
                    int templateChunkX = (chunkData >> 14 & 0x3FF) * CHUNK_SIZE;
                    int plane = chunkData >> 24 & 0x3;
                    if (worldPoint.getX() >= templateChunkX && worldPoint.getX() < templateChunkX + CHUNK_SIZE
                            && worldPoint.getY() >= templateChunkY && worldPoint.getY() < templateChunkY + CHUNK_SIZE
                            && plane == worldPoint.getPlane())
                    {
                        WorldPoint p = new WorldPoint(worldView.getBaseX() + x * CHUNK_SIZE + (worldPoint.getX() & (CHUNK_SIZE - 1)),
                                worldView.getBaseY() + y * CHUNK_SIZE + (worldPoint.getY() & (CHUNK_SIZE - 1)),
                                z);
                        p = rotate(p, rotation);
                        worldPoints.add(p);
                    }
                }
            }
        }
        if(worldPoints.isEmpty())
            worldPoints.add(worldPoint);
        return worldPoints;
    }

    /**
     * Rotate the coordinates in the chunk according to chunk rotation
     *
     * @param point    point
     * @param rotation rotation
     * @return world point
     */
    private static WorldPoint rotate(WorldPoint point, int rotation)
    {
        int chunkX = point.getX() & -CHUNK_SIZE;
        int chunkY = point.getY() & -CHUNK_SIZE;
        int x = point.getX() & (CHUNK_SIZE - 1);
        int y = point.getY() & (CHUNK_SIZE - 1);
        switch (rotation)
        {
            case 1:
                return new WorldPoint(chunkX + y, chunkY + (CHUNK_SIZE - 1 - x), point.getPlane());
            case 2:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - x), chunkY + (CHUNK_SIZE - 1 - y), point.getPlane());
            case 3:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - y), chunkY + x, point.getPlane());
        }
        return point;
    }

    public static int compress(WorldPoint wp) {
        return wp.getX() | wp.getY() << 14 | wp.getPlane() << 29;
    }

    public static int compress(int x, int y, int z) {
        return x | y << 14 | z << 29;
    }

    public static WorldPoint fromCompressed(int compressed)
    {
        int x = compressed & 0x3FFF;
        int y = (compressed >>> 14) & 0x7FFF;
        int z = (compressed >>> 29) & 7;
        return new WorldPoint(x, y, z);
    }

    public static short getCompressedX(int compressed)
    {
        return (short) (compressed & 0x3FFF);
    }

    public static short getCompressedY(int compressed)
    {
        return (short) ((compressed >>> 14) & 0x7FFF);
    }

    public static byte getCompressedPlane(int compressed)
    {
        return (byte)((compressed >>> 29) & 7);
    }

    public static int dx(int compressed, int n)
    {
        return compressed + n;
    }

    public static int dy(int compressed, int n)
    {
        return compressed + (n << 14);
    }

    public static int dxy(int compressed, int nx, int ny)
    {
        return compressed + nx + (ny << 14);
    }
}

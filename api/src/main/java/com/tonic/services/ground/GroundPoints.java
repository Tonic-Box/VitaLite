package com.tonic.services.ground;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.WorldView;

/**
 * The canvas points projected from the ground this frame and the last, so a shape drawn through them can be found on the ground again.
 */
final class GroundPoints
{
    private static final int MAX_PLANE = GroundSurface.PLANES - 1;
    private static final int MAX_POINTS = 1 << 18;

    private static Long2ObjectOpenHashMap<GroundPoint> current = new Long2ObjectOpenHashMap<>();
    private static Long2ObjectOpenHashMap<GroundPoint> previous = new Long2ObjectOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<GroundSurface> surfaces = new Int2ObjectOpenHashMap<>();
    private static boolean active;
    private static boolean inArea;
    private static int areaPlane;

    private GroundPoints()
    {
    }

    static void projected(Point point, Client client, int worldViewId, int x, int y, int z)
    {
        if (!active || point == null || !client.isClientThread())
        {
            return;
        }

        WorldView worldView = client.getWorldView(worldViewId);
        if (worldView == null)
        {
            return;
        }

        int plane = inArea ? (areaPlane < 0 ? worldView.getPlane() : areaPlane) : levelAt(worldView, x, y, z);
        if (plane < 0)
        {
            return;
        }

        add(point.getX(), point.getY(), worldViewId, x, y, plane);
    }

    static void enterArea(int plane)
    {
        inArea = true;
        areaPlane = plane;
    }

    static void exitArea()
    {
        inArea = false;
    }

    static void beginFrame(boolean capture)
    {
        Long2ObjectOpenHashMap<GroundPoint> recycled = previous;
        previous = current;
        current = recycled;
        current.clear();
        if (!capture)
        {
            previous.clear();
        }
        active = capture;
        inArea = false;
        surfaces.clear();
    }

    static GroundPoint lookup(int canvasX, int canvasY)
    {
        long key = key(canvasX, canvasY);
        GroundPoint found = current.get(key);
        return found != null ? found : previous.get(key);
    }

    private static void add(int canvasX, int canvasY, int worldView, int x, int y, int plane)
    {
        if (current.size() >= MAX_POINTS)
        {
            current.clear();
        }

        long key = key(canvasX, canvasY);
        GroundPoint head = current.get(key);
        if (head == null)
        {
            current.put(key, new GroundPoint(worldView, x, y, plane));
            return;
        }

        GroundPoint last = head;
        for (GroundPoint known = head; known != null; known = known.next)
        {
            if (known.isAt(worldView, x, y, plane))
            {
                return;
            }
            last = known;
        }
        last.next = new GroundPoint(worldView, x, y, plane);
    }

    private static int levelAt(WorldView worldView, int x, int y, int z)
    {
        int current = worldView.getPlane();
        if (onGround(worldView, x, y, z, current))
        {
            return current;
        }
        for (int level = 0; level <= MAX_PLANE; level++)
        {
            if (level != current && onGround(worldView, x, y, z, level))
            {
                return level;
            }
        }
        return -1;
    }

    private static boolean onGround(WorldView worldView, int x, int y, int z, int level)
    {
        int plane = groundPlaneOf(worldView, x, y, level);
        return plane >= 0 && worldView.getTileHeight(x, y, plane) == z;
    }

    private static int groundPlaneOf(WorldView worldView, int x, int y, int level)
    {
        if (x < 0 || y < 0)
        {
            return -1;
        }
        GroundSurface surface = surfaceOf(worldView);
        int tileX = x >> 7;
        int tileY = y >> 7;
        if (surface == null || !surface.containsTile(tileX, tileY))
        {
            return -1;
        }
        return surface.surfacePlane(level, tileX, tileY);
    }

    private static GroundSurface surfaceOf(WorldView worldView)
    {
        GroundSurface surface = surfaces.get(worldView.getId());
        if (surface == null)
        {
            surface = GroundSurface.of(worldView);
            if (surface != null)
            {
                surfaces.put(worldView.getId(), surface);
            }
        }
        return surface;
    }

    private static long key(int canvasX, int canvasY)
    {
        return ((long) canvasX << 32) | (canvasY & 0xFFFFFFFFL);
    }
}

package com.tonic.services.pathfinder.impl;

import com.tonic.services.pathfinder.collision.CollisionMap;
import com.tonic.services.pathfinder.collision.Flags;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportAvailability;
import com.tonic.util.WorldPointUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

/**
 * Whether a route exists that never leaves a square of the world, over the static collision where doors stand open and the transports with both ends inside the square: a search the size of the loaded scene, quick enough to ask before planning a walk.
 */
public final class BoundedReach
{
    private static final int[][] STEPS = {{-1, 1, Flags.NORTHWEST}, {0, 1, Flags.NORTH}, {1, 1, Flags.NORTHEAST}, {-1, 0, Flags.WEST}, {1, 0, Flags.EAST}, {-1, -1, Flags.SOUTHWEST}, {0, -1, Flags.SOUTH}, {1, -1, Flags.SOUTHEAST}};

    private final CollisionMap map;
    private final Int2ObjectMap<ObjectArrayList<Transport>> transports;
    private final TransportAvailability availability;
    private final int westX;
    private final int southY;
    private final int size;

    /**
     * @param map the static collision
     * @param transports the transports by the packed tile they leave from
     * @param availability which of them may be taken now
     * @param westX the square's west x
     * @param southY the square's south y
     * @param size the square's width and height in tiles
     */
    public BoundedReach(CollisionMap map, Int2ObjectMap<ObjectArrayList<Transport>> transports, TransportAvailability availability, int westX, int southY, int size)
    {
        this.map = map;
        this.transports = transports;
        this.availability = availability;
        this.westX = westX;
        this.southY = southY;
        this.size = size;
    }

    /**
     * Searches from a tile for any tile of the targets without leaving the square.
     *
     * @param start where the route starts
     * @param targets the areas any tile of which ends it
     * @return whether one can be reached; false when the start or every target lies outside the square
     */
    public boolean anyReachable(WorldPoint start, List<WorldArea> targets)
    {
        IntOpenHashSet goals = new IntOpenHashSet();
        for (int goal : WorldPointUtil.toCompressedPoints(targets.toArray(new WorldArea[0])))
        {
            if (inside(goal))
            {
                goals.add(goal);
            }
        }
        int first = WorldPointUtil.compress(start);
        if (goals.isEmpty() || !inside(first))
        {
            return false;
        }

        IntOpenHashSet visited = new IntOpenHashSet();
        IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        visited.add(first);
        queue.enqueue(first);
        while (!queue.isEmpty())
        {
            int node = queue.dequeueInt();
            if (goals.contains(node))
            {
                return true;
            }
            short x = WorldPointUtil.getCompressedX(node);
            short y = WorldPointUtil.getCompressedY(node);
            byte plane = WorldPointUtil.getCompressedPlane(node);
            byte open = map.all(x, y, plane);
            for (int[] step : STEPS)
            {
                if ((open & step[2]) != 0)
                {
                    visit(WorldPointUtil.compress(x + step[0], y + step[1], plane), visited, queue);
                }
            }
            ObjectArrayList<Transport> leaving = transports.get(node);
            if (leaving != null)
            {
                for (Transport transport : leaving)
                {
                    if (availability.allows(transport))
                    {
                        visit(transport.getDestination().getPacked(), visited, queue);
                    }
                }
            }
        }
        return false;
    }

    private void visit(int tile, IntOpenHashSet visited, IntArrayFIFOQueue queue)
    {
        if (inside(tile) && visited.add(tile))
        {
            queue.enqueue(tile);
        }
    }

    private boolean inside(int tile)
    {
        int x = WorldPointUtil.getCompressedX(tile) - westX;
        int y = WorldPointUtil.getCompressedY(tile) - southY;
        return x >= 0 && y >= 0 && x < size && y < size;
    }
}

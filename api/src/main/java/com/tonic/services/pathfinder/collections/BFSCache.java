package com.tonic.services.pathfinder.collections;

import com.tonic.services.pathfinder.impl.LiveLayer;
import com.tonic.services.pathfinder.impl.Step;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.WorldPointUtil;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The search's route tree: which tiles it has reached, the tile each was reached from and the transport taken to reach it, kept a 64x64 map square at a time, a byte a tile holding the step a tile was walked to by, that it began a route, or that its parent lies further off and is kept aside; clearing it is a counter's increment.
 */
public class BFSCache
{
    private static final int SQUARE_SHIFT = 6;
    private static final int SQUARE_MASK = (1 << SQUARE_SHIFT) - 1;
    private static final int SQUARE_TILES = 1 << (SQUARE_SHIFT * 2);
    private static final int SQUARES_Y = (0x7FFF >> SQUARE_SHIFT) + 1;
    private static final int SQUARES = ((0x3FFF >> SQUARE_SHIFT) + 1) * SQUARES_Y;
    private static final int PLANES = 8;
    private static final int[] DX = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DY = {1, 1, 1, 0, 0, -1, -1, -1};
    private static final byte[] STEP_BY_OFFSET = new byte[9];

    static
    {
        for (int direction = 0; direction < DX.length; direction++)
        {
            STEP_BY_OFFSET[(DX[direction] + 1) * 3 + DY[direction] + 1] = (byte) (direction + 1);
        }
    }

    private static final byte UNREACHED = 0;
    private static final byte ROOT = (byte) (DX.length + 1);
    private static final byte FAR = (byte) (DX.length + 2);

    private static final class Square
    {
        final byte[] state = new byte[SQUARE_TILES];
        int generation;
    }

    private Square[][] planes = new Square[PLANES][];
    private int generation = 1;
    private int size;
    private final Int2IntOpenHashMap farParents = new Int2IntOpenHashMap();
    private final Int2ObjectOpenHashMap<Transport> reachedThrough = new Int2ObjectOpenHashMap<>();

    /**
     * Marks a tile reached.
     *
     * @param point the tile reached
     * @param parent the tile it was reached from, or -1 when a route begins on it
     * @return false when the tile had already been reached, which leaves it as it was
     */
    public boolean put(int point, int parent)
    {
        Square square = writable(point);
        int index = index(point);
        if (square.state[index] != UNREACHED)
        {
            return false;
        }
        byte step = step(point, parent);
        square.state[index] = step;
        if (step == FAR)
        {
            farParents.put(point, parent);
        }
        size++;
        return true;
    }

    /**
     * Marks a tile reached from another through a transport.
     *
     * @param point the tile reached
     * @param parent the tile the transport was taken from
     * @param transport the transport taken
     * @return false when the tile had already been reached, which leaves it as it was
     */
    public boolean put(int point, int parent, Transport transport)
    {
        if (!put(point, parent))
        {
            return false;
        }
        reachedThrough.put(point, transport);
        return true;
    }

    /**
     * @param point a tile
     * @return whether the search has already reached it
     */
    public boolean contains(int point)
    {
        Square square = readable(point);
        return square != null && square.state[index(point)] != UNREACHED;
    }

    /**
     * @param position a tile the search has reached
     * @return the tile it was reached from, or -1 when a route began on it or it was not reached
     */
    public int get(int position)
    {
        Square square = readable(position);
        byte step = square == null ? UNREACHED : square.state[index(position)];
        if (step == UNREACHED || step == ROOT)
        {
            return -1;
        }
        if (step == FAR)
        {
            return farParents.get(position);
        }
        return WorldPointUtil.compress(WorldPointUtil.getCompressedX(position) - DX[step - 1], WorldPointUtil.getCompressedY(position) - DY[step - 1], WorldPointUtil.getCompressedPlane(position));
    }

    /**
     * Forgets every tile for the next search, keeping the squares to be reused.
     */
    public void clear()
    {
        if (generation == Integer.MAX_VALUE)
        {
            planes = new Square[PLANES][];
            generation = 0;
        }
        generation++;
        size = 0;
        farParents.clear();
        reachedThrough.clear();
    }

    /**
     * @return how many tiles the search has reached
     */
    public int size()
    {
        return size;
    }

    /**
     * The route from where the search began to a tile it reached, a step per tile, each naming the transport taken from it.
     *
     * @param position the tile reached
     * @param live the scene the search planned over, for placing the steps in it
     * @return the steps, first to last
     */
    public List<Step> path(int position, LiveLayer live)
    {
        IntArrayList positions = new IntArrayList();
        for (int at = position; at != -1; at = get(at))
        {
            positions.add(at);
        }
        List<Step> path = new ArrayList<>(positions.size());
        for (int i = positions.size() - 1; i >= 0; i--)
        {
            Transport transport = i > 0 ? reachedThrough.get(positions.getInt(i - 1)) : null;
            path.add(new Step(positions.getInt(i), transport, live));
        }
        return path;
    }

    private static byte step(int point, int parent)
    {
        if (parent == -1)
        {
            return ROOT;
        }
        int parentX = WorldPointUtil.getCompressedX(parent);
        int parentY = WorldPointUtil.getCompressedY(parent);
        int parentPlane = WorldPointUtil.getCompressedPlane(parent);
        int dx = WorldPointUtil.getCompressedX(point) - parentX;
        int dy = WorldPointUtil.getCompressedY(point) - parentY;
        if (parentPlane != WorldPointUtil.getCompressedPlane(point) || dx < -1 || dx > 1 || dy < -1 || dy > 1 || WorldPointUtil.compress(parentX, parentY, parentPlane) != parent)
        {
            return FAR;
        }
        byte step = STEP_BY_OFFSET[(dx + 1) * 3 + dy + 1];
        return step == UNREACHED ? FAR : step;
    }

    private Square readable(int point)
    {
        Square[] plane = planes[WorldPointUtil.getCompressedPlane(point)];
        if (plane == null)
        {
            return null;
        }
        Square square = plane[square(point)];
        return square == null || square.generation != generation ? null : square;
    }

    private Square writable(int point)
    {
        int planeIndex = WorldPointUtil.getCompressedPlane(point);
        Square[] plane = planes[planeIndex];
        if (plane == null)
        {
            plane = new Square[SQUARES];
            planes[planeIndex] = plane;
        }
        int squareIndex = square(point);
        Square square = plane[squareIndex];
        if (square == null)
        {
            square = new Square();
            plane[squareIndex] = square;
        }
        if (square.generation != generation)
        {
            Arrays.fill(square.state, UNREACHED);
            square.generation = generation;
        }
        return square;
    }

    private static int square(int point)
    {
        return (WorldPointUtil.getCompressedX(point) >> SQUARE_SHIFT) * SQUARES_Y + (WorldPointUtil.getCompressedY(point) >> SQUARE_SHIFT);
    }

    private static int index(int point)
    {
        return (WorldPointUtil.getCompressedX(point) & SQUARE_MASK) << SQUARE_SHIFT | (WorldPointUtil.getCompressedY(point) & SQUARE_MASK);
    }
}

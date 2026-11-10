package com.tonic.services.pathfinder.sailing.sea;

import com.tonic.services.pathfinder.collision.SparseBitSet;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ShortOpenHashMap;
import net.runelite.api.coords.WorldPoint;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.function.IntConsumer;

/**
 * The sea's navigation web ("GWEB" v1)
 */
public final class SeaGraph
{
    private static final String RESOURCE = "/com/tonic/services/pathfinder/graph.dat";
    private static final byte[] MAGIC = {'G', 'W', 'E', 'B'};
    private static final int VERSION = 1;
    private static final int X_BITS = 13;
    private static final int Y_BITS = 15;
    private static final int X_MASK = (1 << X_BITS) - 1;
    private static final int Y_MASK = (1 << Y_BITS) - 1;
    private static final int PLANE_SHIFT = X_BITS + Y_BITS;
    private static final int PLANE_MASK = 0xF;
    private static final int NONE = -1;
    private static final int MOST_ITERATIONS = 100_000;

    private final SparseBitSet nodes;
    private final Int2ObjectOpenHashMap<IntList> neighbours;
    private final Long2ShortOpenHashMap edgeMasks;

    SeaGraph(SparseBitSet nodes, Int2ObjectOpenHashMap<IntList> neighbours, Long2ShortOpenHashMap edgeMasks)
    {
        this.nodes = nodes;
        this.neighbours = neighbours;
        this.edgeMasks = edgeMasks;
    }

    /**
     * Reads the web bundled with the api
     * @return the web
     */
    public static SeaGraph load() throws IOException
    {
        InputStream stream = SeaGraph.class.getResourceAsStream(RESOURCE);
        if (stream == null)
        {
            throw new IOException(RESOURCE + " not found on classpath");
        }
        try (stream)
        {
            return load(stream);
        }
    }

    /**
     * Reads a web in GWEB form
     * @param stream Stream holding the web
     * @return the web
     */
    public static SeaGraph load(InputStream stream) throws IOException
    {
        DataInputStream in = new DataInputStream(stream);
        byte[] magic = new byte[MAGIC.length];
        in.readFully(magic);
        for (int i = 0; i < MAGIC.length; i++)
        {
            if (magic[i] != MAGIC[i])
            {
                throw new IOException("not a sea graph (bad magic)");
            }
        }
        int version = in.readByte();
        if (version != VERSION)
        {
            throw new IOException("unsupported sea graph version " + version);
        }
        byte[] nodeBytes = new byte[in.readInt()];
        in.readFully(nodeBytes);
        SparseBitSet nodes = readNodes(nodeBytes);
        int edgeCount = readVarInt(in);
        Int2ObjectOpenHashMap<IntList> neighbours = new Int2ObjectOpenHashMap<>();
        Long2ShortOpenHashMap edgeMasks = new Long2ShortOpenHashMap();
        for (int i = 0; i < edgeCount; i++)
        {
            int source = in.readInt();
            int target = in.readInt();
            short mask = in.readShort();
            neighbours.computeIfAbsent(source, key -> new IntArrayList()).add(target);
            neighbours.computeIfAbsent(target, key -> new IntArrayList()).add(source);
            edgeMasks.put(edgeKey(source, target), mask);
        }
        return new SeaGraph(nodes, neighbours, edgeMasks);
    }

    private static SparseBitSet readNodes(byte[] bytes) throws IOException
    {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes)))
        {
            return (SparseBitSet) in.readObject();
        }
        catch (ClassNotFoundException e)
        {
            throw new IOException("the sea graph's node set is not a bit set", e);
        }
    }

    private static int readVarInt(DataInputStream in) throws IOException
    {
        long result = 0;
        int shift = 0;
        while (true)
        {
            byte b = in.readByte();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0)
            {
                return (int) result;
            }
            shift += 7;
            if (shift >= Long.SIZE)
            {
                throw new IOException("varint too long");
            }
        }
    }

    /**
     * Packs a tile into the web's own key
     * @param x Tile x
     * @param y Tile y
     * @param plane Tile plane
     * @return the packed key
     */
    public static int pack(int x, int y, int plane)
    {
        return (x & X_MASK) | (y & Y_MASK) << X_BITS | (plane & PLANE_MASK) << PLANE_SHIFT;
    }

    public static int x(int packed)
    {
        return packed & X_MASK;
    }

    public static int y(int packed)
    {
        return (packed >> X_BITS) & Y_MASK;
    }

    public static int plane(int packed)
    {
        return (packed >> PLANE_SHIFT) & PLANE_MASK;
    }

    /**
     * Counts how many nodes the web has
     * @return the node count
     */
    public int nodeCount()
    {
        return nodes.cardinality();
    }

    /**
     * Counts how many edges the web has
     * @return the edge count
     */
    public int edgeCount()
    {
        return edgeMasks.size();
    }

    /**
     * Checks whether the tile is a node
     * @param packed Packed tile key
     * @return true if the tile is a node
     */
    public boolean hasNode(int packed)
    {
        return nodes.get(packed);
    }

    /**
     * Gets the nodes an edge joins to this one
     * @param packed Packed node key
     * @return the neighbours, none for a tile that is not a node
     */
    public IntList neighbours(int packed)
    {
        IntList list = neighbours.get(packed);
        return list == null ? IntList.of() : list;
    }

    /**
     * Visits every node
     * @param action Called with each node's key
     */
    public void forEachNode(IntConsumer action)
    {
        for (int node = nodes.nextSetBit(0); node >= 0; node = nodes.nextSetBit(node + 1))
        {
            action.accept(node);
        }
    }

    /**
     * Visits every edge once
     * @param action Called with each edge's two nodes
     */
    public void forEachEdge(EdgeConsumer action)
    {
        for (long key : edgeMasks.keySet())
        {
            action.accept((int) (key >>> Integer.SIZE), (int) key);
        }
    }

    /**
     * Receives an edge as the two nodes it joins
     */
    @FunctionalInterface
    public interface EdgeConsumer
    {
        void accept(int from, int to);
    }

    /**
     * Gets the families on the edge's line
     * @param from Packed key of one node
     * @param to Packed key of the other node
     * @return the families, a bit per family numbered from 1
     */
    public int edgeMask(int from, int to)
    {
        return edgeMasks.getOrDefault(edgeKey(from, to), (short) 0) & 0xFFFF;
    }

    /**
     * Checks whether the edge's line crosses none of the avoided families
     * @param from Packed key of one node
     * @param to Packed key of the other node
     * @param avoided Families to keep out of
     * @return true if the edge can be crossed
     */
    public boolean crossable(int from, int to, Set<WaterFamily> avoided)
    {
        int mask = edgeMask(from, to);
        for (WaterFamily family : avoided)
        {
            if (family.number() >= 1 && family.number() <= Short.SIZE && (mask & 1 << (family.number() - 1)) != 0)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds the nearest node by Chebyshev distance
     * @param x Tile x
     * @param y Tile y
     * @param plane Tile plane
     * @param maxRadius Furthest distance searched
     * @return the node's key, or -1 when none is in reach
     */
    public int nearestNode(int x, int y, int plane, int maxRadius)
    {
        if (nodes.get(pack(x, y, plane)))
        {
            return pack(x, y, plane);
        }
        for (int r = 1; r <= maxRadius; r++)
        {
            for (int dx = -r; dx <= r; dx++)
            {
                for (int dy = -r; dy <= r; dy++)
                {
                    if (Math.abs(dx) != r && Math.abs(dy) != r)
                    {
                        continue;
                    }
                    int packed = pack(x + dx, y + dy, plane);
                    if (nodes.get(packed))
                    {
                        return packed;
                    }
                }
            }
        }
        return NONE;
    }

    /**
     * Finds the nodes of the cheapest way through the web from one node to another, by A* on Chebyshev distance, taking no edge whose line crosses water the boat keeps out of
     * @param from Packed key of the start node
     * @param to Packed key of the goal node
     * @param avoided Families to keep out of
     * @return the path's tiles, empty when there is none
     */
    public List<WorldPoint> path(int from, int to, Set<WaterFamily> avoided)
    {
        if (avoided == null)
        {
            throw new IllegalArgumentException("avoided cannot be null");
        }
        if (from == to)
        {
            return List.of(tile(from));
        }
        Int2IntOpenHashMap costs = new Int2IntOpenHashMap();
        costs.defaultReturnValue(Integer.MAX_VALUE);
        Int2IntOpenHashMap parents = new Int2IntOpenHashMap();
        parents.defaultReturnValue(NONE);
        IntOpenHashSet closed = new IntOpenHashSet();
        PriorityQueue<long[]> open = new PriorityQueue<>(Comparator.comparingLong(a -> a[0]));
        costs.put(from, 0);
        open.add(new long[] {distance(from, to), from});
        int iterations = 0;
        while (!open.isEmpty() && iterations++ < MOST_ITERATIONS)
        {
            int current = (int) open.poll()[1];
            if (!closed.add(current))
            {
                continue;
            }
            if (current == to)
            {
                return unwind(parents, to);
            }
            int cost = costs.get(current);
            IntList next = neighbours(current);
            for (int i = 0; i < next.size(); i++)
            {
                int neighbour = next.getInt(i);
                if (closed.contains(neighbour) || !crossable(current, neighbour, avoided))
                {
                    continue;
                }
                int tentative = cost + distance(current, neighbour);
                if (tentative < costs.get(neighbour))
                {
                    costs.put(neighbour, tentative);
                    parents.put(neighbour, current);
                    open.add(new long[] {(long) tentative + distance(neighbour, to), neighbour});
                }
            }
        }
        return Collections.emptyList();
    }

    private static List<WorldPoint> unwind(Int2IntOpenHashMap parents, int to)
    {
        List<WorldPoint> path = new ArrayList<>();
        for (int node = to; node != NONE; node = parents.get(node))
        {
            path.add(tile(node));
        }
        Collections.reverse(path);
        return path;
    }

    private static WorldPoint tile(int packed)
    {
        return new WorldPoint(x(packed), y(packed), plane(packed));
    }

    private static int distance(int a, int b)
    {
        return Math.max(Math.abs(x(a) - x(b)), Math.abs(y(a) - y(b)));
    }

    private static long edgeKey(int a, int b)
    {
        return a <= b ? (long) a << Integer.SIZE | (b & 0xFFFFFFFFL) : (long) b << Integer.SIZE | (a & 0xFFFFFFFFL);
    }
}

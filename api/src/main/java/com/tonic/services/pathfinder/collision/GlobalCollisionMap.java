package com.tonic.services.pathfinder.collision;

import com.tonic.services.pathfinder.SharedMapFile;
import com.tonic.services.pathfinder.Walker;
import com.tonic.util.WorldPointUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The world's collision, read from a file every local client maps: the blocked-edge bits as an index of 2048-bit blocks with each distinct block stored once, and the ways out of every tile of each 64x64 map square that holds a blocked edge, each distinct square's table stored once.
 */
public class GlobalCollisionMap implements CollisionMap
{
    public static final int W_FLAG = 1 << 30;

    /**
     * The largest x the map holds; an instance's own tiles lie past it and are never on this map.
     */
    public static final int MAX_X = 8191;

    /**
     * The highest plane the map holds.
     */
    public static final int MAX_PLANE = 3;

    private static final String RESOURCE = "collision.dat";
    private static final int FORMAT = 2;
    private static final int HEADER_INTS = 4;
    private static final int BLOCK_SHIFT = 11;
    private static final int BLOCK_WORDS = 32;
    private static final int SQUARE_SHIFT = 6;
    private static final int SQUARE = 1 << SQUARE_SHIFT;
    private static final int SQUARE_MASK = SQUARE - 1;
    private static final int SQUARE_TILES = SQUARE * SQUARE;
    private static final int SQUARES_Y_SHIFT = 9;
    private static final int SQUARES_PER_PLANE = ((MAX_X + 1) >> SQUARE_SHIFT) << SQUARES_Y_SHIFT;
    private static final int SQUARE_COUNT = SQUARES_PER_PLANE * (MAX_PLANE + 1);
    private static final int GRID = SQUARE + 2;
    private static final int TABLE_NONE = 0;
    private static final int TABLE_OPEN = 1;
    private static final int TABLE_SHUT = 2;
    private static final int TABLE_FIRST = 3;

    private final int slots;
    private final IntBuffer index;
    private final LongBuffer blocks;
    private final IntBuffer squares;
    private final ByteBuffer tables;

    private GlobalCollisionMap(ByteBuffer data)
    {
        slots = data.getInt(0);
        int blockCount = data.getInt(4);
        int tableCount = data.getInt(8);
        int indexAt = HEADER_INTS * 4;
        int blocksAt = (indexAt + slots * 4 + 7) & -8;
        int squaresAt = blocksAt + blockCount * BLOCK_WORDS * 8;
        int tablesAt = squaresAt + SQUARE_COUNT * 4;
        index = SharedMapFile.slice(data, indexAt, slots * 4).asIntBuffer();
        blocks = SharedMapFile.slice(data, blocksAt, blockCount * BLOCK_WORDS * 8).asLongBuffer();
        squares = SharedMapFile.slice(data, squaresAt, SQUARE_COUNT * 4).asIntBuffer();
        tables = SharedMapFile.slice(data, tablesAt, tableCount * SQUARE_TILES);
    }

    /**
     * Maps the collision map built from the walker's vcol map, shared by every local client.
     *
     * @return the collision map
     * @throws IOException when the map cannot be read, built or mapped
     */
    public static GlobalCollisionMap load() throws IOException
    {
        return new GlobalCollisionMap(SharedMapFile.open("collision", RESOURCE, FORMAT, out ->
        {
            InputStream is = Walker.class.getResourceAsStream(RESOURCE);
            if (is == null)
            {
                throw new IOException("Missing walker resource " + RESOURCE);
            }
            write(VcolCodec.readStream(is), out);
        }));
    }

    @Override
    public byte all(short x, short y, byte z)
    {
        int table = table(x, y, z);
        return table != TABLE_NONE ? decoded(table, x, y) : computedAll(x, y, z);
    }

    /**
     * Works out the ways out of a tile from its edges, as the stored tables were.
     *
     * @param x the world x
     * @param y the world y
     * @param z the plane
     * @return the ways out, as Flags
     */
    public byte computedAll(short x, short y, byte z)
    {
        byte n = n(x, y, z);
        byte e = e(x, y, z);
        byte s = s(x, y, z);
        byte w = w(x, y, z);
        if ((n | e | s | w) == 0)
        {
            return 0;
        }
        byte sw = (byte) (s & w & w(x, (short) (y - 1), z) & s((short) (x - 1), y, z));
        byte se = (byte) (s & e & e(x, (short) (y - 1), z) & s((short) (x + 1), y, z));
        byte nw = (byte) (n & w & w(x, (short) (y + 1), z) & n((short) (x - 1), y, z));
        byte ne = (byte) (n & e & e(x, (short) (y + 1), z) & n((short) (x + 1), y, z));
        return (byte) (nw | (n << 1) | (ne << 2) | (w << 3) | (e << 4) | (sw << 5) | (s << 6) | (se << 7));
    }

    @Override
    public boolean walkable(short x, short y, byte z)
    {
        int table = table(x, y, z);
        if (table != TABLE_NONE)
        {
            return decoded(table, x, y) != 0;
        }
        return (n(x, y, z) | e(x, y, z) | s(x, y, z) | w(x, y, z)) == 1;
    }

    @Override
    public boolean walkable(int packed)
    {
        return walkable(WorldPointUtil.getCompressedX(packed), WorldPointUtil.getCompressedY(packed), WorldPointUtil.getCompressedPlane(packed));
    }

    @Override
    public byte n(short x, short y, byte z)
    {
        return outside(x, y, z) ? 0 : open(x | (y << 13) | (z << 28));
    }

    @Override
    public byte e(short x, short y, byte z)
    {
        return outside(x, y, z) ? 0 : open(x | (y << 13) | (z << 28) | W_FLAG);
    }

    private byte open(int bit)
    {
        int slot = bit >>> BLOCK_SHIFT;
        if (slot >= slots)
        {
            return 1;
        }
        int block = index.get(slot);
        boolean blocked = block != 0 && (blocks.get(((block - 1) << 5) | ((bit >>> 6) & (BLOCK_WORDS - 1))) & (1L << bit)) != 0;
        return (byte) (blocked ? 0 : 1);
    }

    private int table(short x, short y, byte z)
    {
        return outside(x, y, z) ? TABLE_NONE : squares.get(square(x, y, z));
    }

    private byte decoded(int table, short x, short y)
    {
        if (table == TABLE_OPEN)
        {
            return (byte) 0xFF;
        }
        if (table == TABLE_SHUT)
        {
            return 0;
        }
        return tables.get((table - TABLE_FIRST) * SQUARE_TILES + ((x & SQUARE_MASK) << SQUARE_SHIFT | (y & SQUARE_MASK)));
    }

    private static boolean outside(int x, int y, int z)
    {
        return x < 0 || x > MAX_X || y < 0 || z < 0 || z > MAX_PLANE;
    }

    private static int square(int x, int y, int z)
    {
        return z * SQUARES_PER_PLANE + ((x >> SQUARE_SHIFT) << SQUARES_Y_SHIFT | (y >> SQUARE_SHIFT));
    }

    private static void write(SparseBitSet bitSet, SharedMapFile.Output out) throws IOException
    {
        long[][][] bits = bitSet.bits;
        Map<Key, Integer> blockIds = new HashMap<>();
        List<long[]> distinctBlocks = new ArrayList<>();
        int slotCount = 0;
        int[] slotBlocks = new int[bits.length * BLOCK_WORDS];
        for (int level1 = 0; level1 < bits.length; level1++)
        {
            long[][] level2 = bits[level1];
            if (level2 == null)
            {
                continue;
            }
            for (int at = 0; at < level2.length; at++)
            {
                long[] words = level2[at];
                if (words == null || Arrays.stream(words).allMatch(word -> word == 0))
                {
                    continue;
                }
                int slot = level1 * BLOCK_WORDS + at;
                Key key = new Key(words);
                Integer id = blockIds.get(key);
                if (id == null)
                {
                    distinctBlocks.add(words);
                    id = distinctBlocks.size();
                    blockIds.put(key, id);
                }
                slotBlocks[slot] = id;
                slotCount = slot + 1;
            }
        }

        int[] squareTables = new int[SQUARE_COUNT];
        List<byte[]> distinctTables = decodeSquares(bitSet, squareTables);

        out.writeInt(slotCount);
        out.writeInt(distinctBlocks.size());
        out.writeInt(distinctTables.size());
        out.writeInt(0);
        for (int slot = 0; slot < slotCount; slot++)
        {
            out.writeInt(slotBlocks[slot]);
        }
        if ((slotCount & 1) != 0)
        {
            out.writeInt(0);
        }
        for (long[] words : distinctBlocks)
        {
            for (long word : words)
            {
                out.writeLong(word);
            }
        }
        for (int table : squareTables)
        {
            out.writeInt(table);
        }
        for (byte[] table : distinctTables)
        {
            ByteBuffer longs = ByteBuffer.wrap(table).order(ByteOrder.LITTLE_ENDIAN);
            while (longs.hasRemaining())
            {
                out.writeLong(longs.getLong());
            }
        }
    }

    /**
     * Works out the ways out of every tile of each map square, on every plane, that holds a blocked edge.
     *
     * @param bitSet the blocked edges
     * @param squareTables filled with each square's table id
     * @return the distinct tables, the first being TABLE_FIRST
     */
    private static List<byte[]> decodeSquares(SparseBitSet bitSet, int[] squareTables)
    {
        Map<Key, Integer> tableIds = new HashMap<>();
        List<byte[]> distinct = new ArrayList<>();
        byte[] north = new byte[GRID * GRID];
        byte[] east = new byte[GRID * GRID];
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1))
        {
            int x = i & MAX_X;
            int y = (i >> 13) & 0x7FFF;
            int z = (i >> 28) & 3;
            int square = square(x, y, z);
            if (squareTables[square] != TABLE_NONE)
            {
                continue;
            }
            byte[] table = decodeSquare(bitSet, x & ~SQUARE_MASK, y & ~SQUARE_MASK, z, north, east);
            if (uniform(table, (byte) 0xFF))
            {
                squareTables[square] = TABLE_OPEN;
                continue;
            }
            if (uniform(table, (byte) 0))
            {
                squareTables[square] = TABLE_SHUT;
                continue;
            }
            Key key = new Key(table);
            Integer id = tableIds.get(key);
            if (id == null)
            {
                distinct.add(table);
                id = TABLE_FIRST + distinct.size() - 1;
                tableIds.put(key, id);
            }
            squareTables[square] = id;
        }
        return distinct;
    }

    private static byte[] decodeSquare(SparseBitSet bitSet, int baseX, int baseY, int z, byte[] north, byte[] east)
    {
        for (int gx = 0; gx < GRID; gx++)
        {
            for (int gy = 0; gy < GRID; gy++)
            {
                int x = baseX + gx - 1;
                int y = baseY + gy - 1;
                north[gx * GRID + gy] = edge(bitSet, x, y, z, 0);
                east[gx * GRID + gy] = edge(bitSet, x, y, z, W_FLAG);
            }
        }
        byte[] all = new byte[SQUARE_TILES];
        for (int dx = 0; dx < SQUARE; dx++)
        {
            for (int dy = 0; dy < SQUARE; dy++)
            {
                int at = (dx + 1) * GRID + dy + 1;
                byte n = north[at];
                byte e = east[at];
                byte s = north[at - 1];
                byte w = east[at - GRID];
                byte ways = 0;
                if ((n | e | s | w) != 0)
                {
                    byte sw = (byte) (s & w & east[at - GRID - 1] & north[at - GRID - 1]);
                    byte se = (byte) (s & e & east[at - 1] & north[at + GRID - 1]);
                    byte nw = (byte) (n & w & east[at - GRID + 1] & north[at - GRID]);
                    byte ne = (byte) (n & e & east[at + 1] & north[at + GRID]);
                    ways = (byte) (nw | (n << 1) | (ne << 2) | (w << 3) | (e << 4) | (sw << 5) | (s << 6) | (se << 7));
                }
                all[dx << SQUARE_SHIFT | dy] = ways;
            }
        }
        return all;
    }

    private static boolean uniform(byte[] table, byte value)
    {
        for (byte ways : table)
        {
            if (ways != value)
            {
                return false;
            }
        }
        return true;
    }

    private static byte edge(SparseBitSet bitSet, int x, int y, int z, int flag)
    {
        if (outside(x, y, z))
        {
            return 0;
        }
        return (byte) (bitSet.get(x | (y << 13) | (z << 28) | flag) ? 0 : 1);
    }

    private static final class Key
    {
        private final long[] words;
        private final byte[] table;
        private final int hash;

        private Key(long[] words)
        {
            this.words = words;
            this.table = null;
            this.hash = Arrays.hashCode(words);
        }

        private Key(byte[] table)
        {
            this.words = null;
            this.table = table;
            this.hash = Arrays.hashCode(table);
        }

        @Override
        public int hashCode()
        {
            return hash;
        }

        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof Key))
            {
                return false;
            }
            Key key = (Key) other;
            return Arrays.equals(words, key.words) && Arrays.equals(table, key.table);
        }
    }
}

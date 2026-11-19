package com.tonic.services.pathfinder.collision;

import com.tonic.Logger;
import com.tonic.services.pathfinder.SharedMapFile;
import com.tonic.services.pathfinder.Walker;
import com.tonic.util.WorldPointUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The world's collision bits, kept as an index of 2048-bit blocks with every distinct block stored once, read from a file all local clients map.
 */
public class GlobalCollisionMap implements CollisionMap {
    public static final int W_FLAG = 1 << 30;
    private static final int FORMAT = 1;
    private static final int BLOCK_SHIFT = 11;
    private static final int BLOCK_WORDS = 32;

    private final int slots;
    private final IntBuffer index;
    private final LongBuffer blocks;

    private GlobalCollisionMap(ByteBuffer data) {
        slots = data.getInt(0);
        int blockCount = data.getInt(4);
        int blocksAt = (8 + slots * 4 + 7) & -8;
        index = SharedMapFile.slice(data, 8, slots * 4).asIntBuffer();
        blocks = SharedMapFile.slice(data, blocksAt, blockCount * BLOCK_WORDS * 8).asLongBuffer();
    }

    private boolean bit(int i) {
        if ((i + 1) < 1) {
            throw new IndexOutOfBoundsException("i=" + i);
        }
        int slot = i >>> BLOCK_SHIFT;
        if (slot >= slots) {
            return false;
        }
        int block = index.get(slot);
        return block != 0 && (blocks.get(((block - 1) << 5) | ((i >>> 6) & (BLOCK_WORDS - 1))) & (1L << i)) != 0;
    }

    private byte get(int index) {
        return (byte)(bit(index) ? 0 : 1);
    }

    @Override
    public byte all(short x, short y, byte z)
    {
        byte n = n(x,y,z);
        byte e = e(x,y,z);
        byte s = s(x,y,z);
        byte w = w(x,y,z);
        if((n | e | s | w) == 0)
        {
            return 0;
        }
        byte sw = (byte) (s & w & w(x, (short)(y - 1), z) & s((short)(x - 1), y, z));
        byte se = (byte) (s & e & e(x, (short)(y - 1), z) & s((short)(x + 1), y, z));
        byte nw = (byte) (n & w & w(x, (short)(y + 1), z) & n((short)(x - 1), y, z));
        byte ne = (byte) (n & e & e(x, (short)(y + 1), z) & n((short)(x + 1), y, z));

        return (byte) (nw | (n << 1) | (ne << 2) | (w << 3) | (e << 4) | (sw << 5) | (s << 6) | (se << 7));
    }

    @Override
    public boolean walkable(short x, short y, byte z) {
        return (n(x, y, z) | e(x, y, z) | s(x, y, z) | w(x, y, z)) == 1;
    }

    public boolean walkable(int packed) {
        final short x = WorldPointUtil.getCompressedX(packed);
        final short y = WorldPointUtil.getCompressedY(packed);
        final byte plane = WorldPointUtil.getCompressedPlane(packed);
        return walkable(x, y, plane);
    }

    @Override
    public byte n(short x, short y, byte z) {
        return get((x & 8191) | ((y & 32767) << 13) | (z << 28));
    }

    @Override
    public byte e(short x, short y, byte z) {
        return get((x & 8191) | ((y & 32767) << 13) | (z << 28) | W_FLAG);
    }

    /**
     * Maps the collision map built from the walker's map, shared by every local client.
     *
     * @return the collision map
     * @throws IOException when the map cannot be read, built or mapped
     */
    public static GlobalCollisionMap load() throws IOException {
        return new GlobalCollisionMap(SharedMapFile.open("collision", "map.dat", FORMAT, out -> {
            try (InputStream is = Walker.class.getResourceAsStream("map.dat")) {
                if (is == null) {
                    throw new IOException("Missing walker resource map.dat");
                }
                write(readBitSet(is), out);
            }
        }));
    }

    /**
     * Loads a collision map from a saved bit set, held by this client alone.
     *
     * @param filePath the saved bit set
     * @return the collision map, or null when the file does not exist
     * @throws IOException when the file cannot be read
     */
    public static GlobalCollisionMap load(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            Logger.error("File not found: " + filePath);
            return null;
        }

        try (InputStream is = new FileInputStream(file)) {
            SparseBitSet bitSet = readBitSet(is);
            return new GlobalCollisionMap(SharedMapFile.build(out -> write(bitSet, out)));
        }
    }

    private static SparseBitSet readBitSet(InputStream is) throws IOException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(is))) {
            return (SparseBitSet) objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Unreadable collision map", e);
        }
    }

    private static void write(SparseBitSet bitSet, SharedMapFile.Output out) throws IOException {
        long[][][] bits = bitSet.bits;
        Map<Block, Integer> ids = new HashMap<>();
        List<long[]> distinct = new ArrayList<>();
        int slotCount = 0;
        int[] slotBlocks = new int[bits.length * BLOCK_WORDS];
        for (int level1 = 0; level1 < bits.length; level1++) {
            long[][] level2 = bits[level1];
            if (level2 == null) {
                continue;
            }
            for (int at = 0; at < level2.length; at++) {
                long[] words = level2[at];
                if (words == null || Arrays.stream(words).allMatch(word -> word == 0)) {
                    continue;
                }
                int slot = level1 * BLOCK_WORDS + at;
                Integer id = ids.get(new Block(words));
                if (id == null) {
                    distinct.add(words);
                    id = distinct.size();
                    ids.put(new Block(words), id);
                }
                slotBlocks[slot] = id;
                slotCount = slot + 1;
            }
        }

        out.writeInt(slotCount);
        out.writeInt(distinct.size());
        for (int slot = 0; slot < slotCount; slot++) {
            out.writeInt(slotBlocks[slot]);
        }
        if ((slotCount & 1) != 0) {
            out.writeInt(0);
        }
        for (long[] words : distinct) {
            for (long word : words) {
                out.writeLong(word);
            }
        }
    }

    private static final class Block {
        private final long[] words;
        private final int hash;

        private Block(long[] words) {
            this.words = words;
            this.hash = Arrays.hashCode(words);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Block && Arrays.equals(words, ((Block) other).words);
        }
    }
}

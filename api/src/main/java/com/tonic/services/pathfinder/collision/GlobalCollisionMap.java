package com.tonic.services.pathfinder.collision;

import com.tonic.Logger;
import com.tonic.services.pathfinder.Walker;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.coords.WorldPoint;

import java.io.*;

public class GlobalCollisionMap implements CollisionMap {
    public static final int W_FLAG = 1 << 30;
    private final SparseBitSet bitSet;

    private GlobalCollisionMap(SparseBitSet bitSet) {
        this.bitSet = bitSet;
    }

    private byte get(int index) {
        return (byte)(bitSet.get(index) ? 0 : 1);
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

    public static GlobalCollisionMap load() throws IOException, ClassNotFoundException {
        try (InputStream is = Walker.class.getResourceAsStream("map.dat")) {
            ObjectInputStream objectInputStream = new ObjectInputStream(is);
            SparseBitSet bitSet = (SparseBitSet) objectInputStream.readObject();
            objectInputStream.close();
            return new GlobalCollisionMap(bitSet);
        }
    }

    public static GlobalCollisionMap load(String filePath) throws IOException, ClassNotFoundException {
        File file = new File(filePath);

        // Check if the file exists
        if (!file.exists() || !file.isFile()) {
            Logger.error("File not found: " + filePath);
            return null;
        }

        // Proceed with loading the file
        try (InputStream is = new FileInputStream(file);
             ObjectInputStream objectInputStream = new ObjectInputStream(is)) {
            SparseBitSet bitSet = (SparseBitSet) objectInputStream.readObject();
            return new GlobalCollisionMap(bitSet);
        }
    }
}
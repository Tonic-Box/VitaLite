package com.tonic.services.pathfinder.objects;

import com.tonic.services.pathfinder.SharedMapFile;
import com.tonic.services.pathfinder.Walker;
import lombok.Getter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

/**
 * The object ids at each world tile, read from a file built from the saved object map that all local clients map.
 * Each tile keeps its first id beside its coordinate; the few tiles holding more than one keep the rest in a
 * separate overflow table.
 */
public class ObjectMap {
    private static final int NO_OBJECT = -1;
    private static final int FORMAT = 1;
    private static final int HEADER = 16;

    private final IntBuffer coordinates;
    private final IntBuffer firstIds;
    private final IntBuffer extraTiles;
    private final IntBuffer extraOffsets;
    private final IntBuffer extraIds;
    @Getter
    private final int version;

    private ObjectMap(ByteBuffer data) {
        version = data.getInt(0);
        int tiles = data.getInt(4);
        int extras = data.getInt(8);
        int extraIdCount = data.getInt(12);
        int at = HEADER;
        coordinates = SharedMapFile.slice(data, at, tiles * 4).asIntBuffer();
        at += tiles * 4;
        firstIds = SharedMapFile.slice(data, at, tiles * 4).asIntBuffer();
        at += tiles * 4;
        extraTiles = SharedMapFile.slice(data, at, extras * 4).asIntBuffer();
        at += extras * 4;
        extraOffsets = SharedMapFile.slice(data, at, extras * 4).asIntBuffer();
        at += extras * 4;
        extraIds = SharedMapFile.slice(data, at, extraIdCount * 4).asIntBuffer();
    }

    /**
     * @param x the world x
     * @param y the world y
     * @param z the plane
     * @return the object ids on that tile, empty when it has none
     */
    public List<Integer> getObjects(int x, int y, int z) {
        int tile = binarySearch(coordinates, packCoordinate((short) x, (short) y, (byte) z));
        if (tile < 0 || firstIds.get(tile) == NO_OBJECT) {
            return Collections.emptyList();
        }

        int extra = binarySearch(extraTiles, tile);
        if (extra < 0) {
            List<Integer> result = new ArrayList<>(1);
            result.add(firstIds.get(tile));
            return result;
        }

        int start = extraOffsets.get(extra);
        int end = extra + 1 < extraOffsets.limit() ? extraOffsets.get(extra + 1) : extraIds.limit();
        List<Integer> result = new ArrayList<>(1 + end - start);
        result.add(firstIds.get(tile));
        for (int i = start; i < end; i++) {
            result.add(extraIds.get(i));
        }
        return result;
    }

    /**
     * @param x the world x
     * @param y the world y
     * @param z the plane
     * @return whether the map lists that tile
     */
    public boolean hasObjects(int x, int y, int z) {
        return binarySearch(coordinates, packCoordinate((short) x, (short) y, (byte) z)) >= 0;
    }

    /**
     * @return the number of tiles the map lists
     */
    public int size() {
        return coordinates.limit();
    }

    private static int packCoordinate(short x, short y, byte z) {
        return (x & 8191) | ((y & 32767) << 13) | ((z & 15) << 28);
    }

    private static int binarySearch(IntBuffer values, int key) {
        int low = 0;
        int high = values.limit() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int value = values.get(mid);
            if (value < key) {
                low = mid + 1;
            } else if (value > key) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -(low + 1);
    }

    /**
     * Maps the object map built from the one bundled with the walker, shared by every local client.
     *
     * @return the map
     * @throws IOException when the resource is missing, cannot be read, is not a version 2 object map, or the shared file cannot be built or mapped
     */
    public static ObjectMap load() throws IOException {
        long startTime = System.currentTimeMillis();
        ObjectMap map = new ObjectMap(SharedMapFile.open("objects", "objects.dat", FORMAT, ObjectMap::write));
        System.out.println("Mapped object map: " + map.size() + " coordinates in " + (System.currentTimeMillis() - startTime) + "ms");
        return map;
    }

    private static void write(SharedMapFile.Output out) throws IOException {
        InputStream inputStream = Walker.class.getResourceAsStream("objects.dat");
        if (inputStream == null) {
            throw new IOException("Missing walker resource objects.dat");
        }

        int version;
        int[] coordinates;
        int[] firstIds;
        IntList extraTiles = new IntList();
        IntList extraOffsets = new IntList();
        IntList extraIds = new IntList();

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(inputStream, 131072))) {
            version = dis.readInt();
            if (version != 2) {
                throw new IOException("Unsupported compressed format version: " + version);
            }

            int entryCount = VarInt.readVarInt(dis);
            if (entryCount < 0 || entryCount > 10_000_000) {
                throw new IOException("Invalid entry count: " + entryCount);
            }

            coordinates = new int[entryCount];
            firstIds = new int[entryCount];
            long currentCoord = 0;

            for (int i = 0; i < entryCount; i++) {
                currentCoord += VarInt.readVarLong(dis);
                if (currentCoord < 0 || currentCoord > Integer.MAX_VALUE) {
                    throw new IOException("Invalid coordinate at entry " + i + ": " + currentCoord);
                }
                coordinates[i] = (int) currentCoord;

                int objectCount = VarInt.readVarInt(dis);
                if (objectCount < 0 || objectCount > 1000) {
                    throw new IOException("Invalid object count at entry " + i + ": " + objectCount);
                }

                if (objectCount == 0) {
                    firstIds[i] = NO_OBJECT;
                    continue;
                }

                firstIds[i] = VarInt.readVarInt(dis);
                if (objectCount > 1) {
                    extraTiles.add(i);
                    extraOffsets.add(extraIds.size());
                    for (int j = 1; j < objectCount; j++) {
                        extraIds.add(VarInt.readVarInt(dis));
                    }
                }
            }
        }

        out.writeInt(version);
        out.writeInt(coordinates.length);
        out.writeInt(extraTiles.size());
        out.writeInt(extraIds.size());
        writeAll(out, coordinates, coordinates.length);
        writeAll(out, firstIds, firstIds.length);
        writeAll(out, extraTiles.values, extraTiles.size());
        writeAll(out, extraOffsets.values, extraOffsets.size());
        writeAll(out, extraIds.values, extraIds.size());
    }

    private static void writeAll(SharedMapFile.Output out, int[] values, int count) throws IOException {
        for (int i = 0; i < count; i++) {
            out.writeInt(values[i]);
        }
    }

    private static final class IntList {
        private int[] values = new int[1024];
        private int size;

        void add(int value) {
            if (size == values.length) {
                values = Arrays.copyOf(values, size * 2);
            }
            values[size++] = value;
        }

        int size() {
            return size;
        }
    }
}

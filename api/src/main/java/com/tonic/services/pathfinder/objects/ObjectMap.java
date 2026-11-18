package com.tonic.services.pathfinder.objects;

import com.tonic.services.pathfinder.Walker;
import lombok.Getter;

import java.io.*;
import java.util.*;

/**
 * The object ids at each world tile, read from the saved object map. Each tile keeps its first id beside its
 * coordinate; the few tiles holding more than one keep the rest in a separate overflow table.
 */
public class ObjectMap {
    private static final int NO_OBJECT = -1;

    private final int[] coordinates;
    private final int[] firstIds;
    private final int[] extraTiles;
    private final int[] extraOffsets;
    private final int[] extraIds;
    @Getter
    private final int version;

    private ObjectMap(int[] coordinates, int[] firstIds, int[] extraTiles, int[] extraOffsets, int[] extraIds, int version) {
        this.coordinates = coordinates;
        this.firstIds = firstIds;
        this.extraTiles = extraTiles;
        this.extraOffsets = extraOffsets;
        this.extraIds = extraIds;
        this.version = version;
    }

    /**
     * @param x the world x
     * @param y the world y
     * @param z the plane
     * @return the object ids on that tile, empty when it has none
     */
    public List<Integer> getObjects(int x, int y, int z) {
        int tile = Arrays.binarySearch(coordinates, packCoordinate((short) x, (short) y, (byte) z));
        if (tile < 0 || firstIds[tile] == NO_OBJECT) {
            return Collections.emptyList();
        }

        int extra = Arrays.binarySearch(extraTiles, tile);
        if (extra < 0) {
            List<Integer> result = new ArrayList<>(1);
            result.add(firstIds[tile]);
            return result;
        }

        int start = extraOffsets[extra];
        int end = extra + 1 < extraOffsets.length ? extraOffsets[extra + 1] : extraIds.length;
        List<Integer> result = new ArrayList<>(1 + end - start);
        result.add(firstIds[tile]);
        for (int i = start; i < end; i++) {
            result.add(extraIds[i]);
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
        return Arrays.binarySearch(coordinates, packCoordinate((short) x, (short) y, (byte) z)) >= 0;
    }

    /**
     * @return the number of tiles the map lists
     */
    public int size() {
        return coordinates.length;
    }

    private static int packCoordinate(short x, short y, byte z) {
        return (x & 8191) | ((y & 32767) << 13) | ((z & 15) << 28);
    }

    /**
     * Loads the object map bundled with the walker.
     *
     * @return the map, or null when the resource is missing
     * @throws IOException when the resource cannot be read or is not a version 2 object map
     */
    public static ObjectMap load() throws IOException {
        InputStream inputStream = Walker.class.getResourceAsStream("objects.dat");
        if (inputStream == null) {
            System.err.println("Object map input stream is null");
            return null;
        }

        long startTime = System.currentTimeMillis();
        int version;
        int[] coordinates;
        int[] firstIds;
        IntList extraTiles = new IntList();
        IntList extraOffsets = new IntList();
        IntList extraIds = new IntList();
        long objectTotal = 0;

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
                objectTotal += objectCount;

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

        long loadTime = System.currentTimeMillis() - startTime;
        System.out.println("Loaded compressed object map: " + coordinates.length + " coordinates, " + objectTotal + " objects in " + loadTime + "ms");

        return new ObjectMap(coordinates, firstIds, extraTiles.toArray(), extraOffsets.toArray(), extraIds.toArray(), version);
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

        int[] toArray() {
            return Arrays.copyOf(values, size);
        }
    }
}

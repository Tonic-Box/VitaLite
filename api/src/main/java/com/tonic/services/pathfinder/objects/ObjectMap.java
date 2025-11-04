package com.tonic.services.pathfinder.objects;

import com.tonic.services.pathfinder.Walker;
import lombok.Getter;

import java.io.*;
import java.util.*;

/**
 * Reads object ID data from a saved object map file.
 * Provides efficient lookup of object IDs at world coordinates.
 */
public class ObjectMap {
    private final long[] coordinates;
    private final int[] offsets;
    private final int[] objectIds;
    @Getter
    private final int version;

    private ObjectMap(long[] coordinates, int[] offsets, int[] objectIds, int version) {
        this.coordinates = coordinates;
        this.offsets = offsets;
        this.objectIds = objectIds;
        this.version = version;
    }

    /**
     * Gets all object IDs at the specified world coordinates.
     */
    public List<Integer> getObjects(int x, int y, int z) {
        long packedCoord = packCoordinate((short) x, (short)y, (byte)z);
        int index = Arrays.binarySearch(coordinates, packedCoord);

        if (index < 0) {
            return Collections.emptyList();
        }

        int startOffset = offsets[index];
        int endOffset = (index + 1 < offsets.length) ? offsets[index + 1] : objectIds.length;

        List<Integer> result = new ArrayList<>(endOffset - startOffset);
        for (int i = startOffset; i < endOffset; i++) {
            result.add(objectIds[i]);
        }

        return result;
    }

    public boolean hasObjects(int x, int y, int z) {
        long packedCoord = packCoordinate((short) x, (short)y, (byte)z);
        return Arrays.binarySearch(coordinates, packedCoord) >= 0;
    }

    public int size() {
        return coordinates.length;
    }

    private long packCoordinate(short x, short y, byte z) {
        return (x & 8191) | ((long)(y & 32767) << 13) | ((long)(z & 15) << 28);
    }

    /**
     * Loads compressed object map from InputStream.
     */
    public static ObjectMap load() throws IOException {
        InputStream inputStream = Walker.class.getResourceAsStream("objects.dat");
        if (inputStream == null) {
            System.err.println("Object map input stream is null");
            return null;
        }

        long startTime = System.currentTimeMillis();
        int version;
        long[] coordinates;
        int[] offsets;
        int[] objectIds;

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(inputStream, 131072))) {

            version = dis.readInt();

            if (version != 2) {
                throw new IOException("Unsupported compressed format version: " + version);
            }

            // Read entry count
            int entryCount = VarInt.readVarInt(dis);

            if (entryCount < 0 || entryCount > 10_000_000) {
                throw new IOException("Invalid entry count: " + entryCount);
            }

            coordinates = new long[entryCount];
            offsets = new int[entryCount];
            List<Integer> objectIdList = new ArrayList<>(entryCount * 3);

            long currentCoord = 0;
            int currentOffset = 0;

            for (int i = 0; i < entryCount; i++) {
                // Read delta-encoded coordinate
                long delta = VarInt.readVarLong(dis);
                currentCoord += delta;
                coordinates[i] = currentCoord;

                // Read object count
                int objectCount = VarInt.readVarInt(dis);

                if (objectCount < 0 || objectCount > 1000) {
                    throw new IOException("Invalid object count at entry " + i + ": " + objectCount);
                }

                offsets[i] = currentOffset;

                // Read object IDs
                for (int j = 0; j < objectCount; j++) {
                    objectIdList.add(VarInt.readVarInt(dis));
                }

                currentOffset += objectCount;
            }

            // Convert to primitive array
            objectIds = new int[objectIdList.size()];
            for (int i = 0; i < objectIdList.size(); i++) {
                objectIds[i] = objectIdList.get(i);
            }
        }

        long loadTime = System.currentTimeMillis() - startTime;
        System.out.println("Loaded compressed object map: " + coordinates.length +
                " coordinates, " + objectIds.length + " objects in " + loadTime + "ms");

        return new ObjectMap(coordinates, offsets, objectIds, version);
    }
}
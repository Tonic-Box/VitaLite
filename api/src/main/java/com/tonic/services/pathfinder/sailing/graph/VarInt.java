package com.tonic.services.pathfinder.sailing.graph;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Variable-length integer encoding for space-efficient storage.
 * Uses 1-5 bytes for ints (vs fixed 4 bytes).
 */
public final class VarInt {

    private VarInt() {
        // Utility class
    }

    /**
     * Reads a variable-length integer.
     */
    public static int readVarInt(DataInputStream dis) throws IOException {
        return (int) readVarLong(dis);
    }

    /**
     * Reads a variable-length long.
     * Uses 1 byte for 0-127, 2 bytes for 0-16383, etc.
     */
    public static long readVarLong(DataInputStream dis) throws IOException {
        long result = 0;
        int shift = 0;
        while (true) {
            byte b = dis.readByte();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
            if (shift >= 64) {
                throw new IOException("VarLong too long");
            }
        }
    }
}

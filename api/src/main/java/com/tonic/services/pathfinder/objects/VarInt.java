package com.tonic.services.pathfinder.objects;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Variable-length integer encoding for space-efficient storage.
 * Uses 1-5 bytes for ints, 1-9 bytes for longs (vs fixed 4/8 bytes).
 */
public class VarInt {

    /**
     * Writes a variable-length integer.
     * Small numbers use fewer bytes.
     */
    public static void writeVarInt(DataOutputStream dos, int value) throws IOException {
        writeVarLong(dos, value & 0xFFFFFFFFL);
    }

    /**
     * Reads a variable-length integer.
     */
    public static int readVarInt(DataInputStream dis) throws IOException {
        return (int) readVarLong(dis);
    }

    /**
     * Writes a variable-length long.
     * Uses 1 byte for 0-127, 2 bytes for 0-16383, etc.
     */
    public static void writeVarLong(DataOutputStream dos, long value) throws IOException {
        while (true) {
            if ((value & ~0x7FL) == 0) {
                dos.writeByte((int) value);
                return;
            } else {
                dos.writeByte((int) ((value & 0x7F) | 0x80));
                value >>>= 7;
            }
        }
    }

    /**
     * Reads a variable-length long.
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

    /**
     * Calculates size of a VarInt in bytes (1-5).
     */
    public static int varIntSize(int value) {
        return varLongSize(value & 0xFFFFFFFFL);
    }

    /**
     * Calculates size of a VarLong in bytes (1-9).
     */
    public static int varLongSize(long value) {
        int size = 1;
        while ((value & ~0x7FL) != 0) {
            size++;
            value >>>= 7;
        }
        return size;
    }
}
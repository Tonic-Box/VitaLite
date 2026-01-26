package com.tonic.services.pathfinder.collections;

import java.util.Arrays;

public class IntToBoolPairMap
{
    private final int[] keys;
    private final byte[] vals; // bits 0 and 1 hold your booleans
    private final int mask;

    public IntToBoolPairMap(int capacity) {
        int size = Integer.highestOneBit(capacity - 1) << 1; // power of 2
        keys = new int[size];
        vals = new byte[size];
        Arrays.fill(keys, Integer.MIN_VALUE); // empty sentinel
        mask = size - 1;
    }

    public byte get(int key) {
        int idx = mix(key) & mask;
        while (keys[idx] != key) {
            if (keys[idx] == Integer.MIN_VALUE) return -1; // not found
            idx = (idx + 1) & mask;
        }
        return vals[idx];
    }

    public boolean put(int key, boolean a, boolean b) {
        int idx = mix(key) & mask;
        while (keys[idx] != Integer.MIN_VALUE && keys[idx] != key) {
            idx = (idx + 1) & mask;
        }
        keys[idx] = key;
        vals[idx] = (byte) ((a ? 1 : 0) | (b ? 2 : 0));
        return bothSet(key);
    }

    public void setA(int key) {
        int idx = mix(key) & mask;
        while (keys[idx] != Integer.MIN_VALUE && keys[idx] != key) {
            idx = (idx + 1) & mask;
        }
        keys[idx] = key;
        vals[idx] |= 1;
    }

    public void setB(int key) {
        int idx = mix(key) & mask;
        while (keys[idx] != Integer.MIN_VALUE && keys[idx] != key) {
            idx = (idx + 1) & mask;
        }
        keys[idx] = key;
        vals[idx] |= 2;
    }

    private static int mix(int x) {
        x ^= x >>> 16;
        x *= 0x85ebca6b;
        x ^= x >>> 13;
        return x;
    }

    public boolean bothSet(int key) {
        int idx = mix(key) & mask;
        while (keys[idx] != key) {
            if (keys[idx] == Integer.MIN_VALUE) return false;
            idx = (idx + 1) & mask;
        }
        return (vals[idx] & 3) == 3;
    }

    public boolean containsKey(int key) {
        int idx = mix(key) & mask;
        while (keys[idx] != key) {
            if (keys[idx] == Integer.MIN_VALUE) return false;
            idx = (idx + 1) & mask;
        }
        return true;
    }
}

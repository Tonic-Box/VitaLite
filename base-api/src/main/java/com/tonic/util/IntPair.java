package com.tonic.util;

import lombok.Getter;

import java.util.concurrent.ThreadLocalRandom;

@Getter
public class IntPair {
    private final int key;
    private final int value;

    public IntPair(int key, int value) {
        this.key = key;
        this.value = value;
    }

    public static IntPair of(int key, int value) {
        return new IntPair(key, value);
    }

    public int randomEnclosed()
    {
        return ThreadLocalRandom.current().nextInt(key, value);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            IntPair pair = (IntPair) o;
            return this.key == pair.key && this.value == pair.value;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return 31 * Integer.hashCode(this.key) + Integer.hashCode(this.value);
    }

    public String toString() {
        return "IntPair{key=" + this.key + ", value=" + this.value + "}";
    }
}
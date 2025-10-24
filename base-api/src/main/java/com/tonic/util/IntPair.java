package com.tonic.util;

import java.util.concurrent.ThreadLocalRandom;

public class IntPair extends Pair<Integer, Integer> {

    public IntPair(int key, int value) {
        super(key, value);
    }

    public static IntPair of(int key, int value) {
        return new IntPair(key, value);
    }

    public int randomEnclosed() {
        return ThreadLocalRandom.current().nextInt(this.getKey(), this.getValue());
    }

    @Override
    public String toString() {
        return "IntPair{key=" + this.getKey() + ", value=" + this.getValue() + "}";
    }
}
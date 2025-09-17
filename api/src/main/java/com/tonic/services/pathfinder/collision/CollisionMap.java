package com.tonic.services.pathfinder.collision;

public interface CollisionMap {
    boolean walkable(int packed);
    boolean walkable(short x, short y, byte z);
    byte all(short x, short y, byte z);
    byte n(short x, short y, byte z);
    byte e(short x, short y, byte z);

    default byte s(short x, short y, byte z) {
        return n(x, (short) (y - 1), z);
    }

    default byte w(short x, short y, byte z) {
        return e((short)(x - 1), y, z);
    }

    default byte ne(short x, short y, byte z) {
        return (byte)(n(x, y, z) & e(x, y, z) & e(x, (short)(y + 1), z) & n((short)(x + 1), y, z));
    }

    default byte nw(short x, short y, byte z) {
        return (byte)(n(x, y, z) & w(x, y, z) & w(x, (short)(y + 1), z) & n((short)(x - 1), y, z));
    }

    default byte se(short x, short y, byte z) {
        return (byte)(s(x, y, z) & e(x, y, z) & e(x, (short)(y - 1), z) & s((short)(x + 1), y, z));
    }

    default byte sw(short x, short y, byte z) {
        return (byte)(s(x, y, z) & w(x, y, z) & w(x, (short)(y - 1), z) & s((short)(x - 1), y, z));
    }
}
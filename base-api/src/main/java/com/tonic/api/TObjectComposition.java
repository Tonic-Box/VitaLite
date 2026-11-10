package com.tonic.api;

/**
 * An object definition's approach-side mask: which sides of the object it cannot be used from
 */
public interface TObjectComposition
{
    /**
     * Gets the definition's approach-side mask as the cache stores it
     * @return the mask, bits 1 north, 2 east, 4 south and 8 west blocked, 0 when the definition has none
     */
    int getBlockAccessFlags();

    /**
     * Gets the blocked sides of the object placed at a rotation
     * @param rotation the placed object's rotation, 0 to 3, each step turning north to east
     * @return the blocked side bits, 1 north, 2 east, 4 south and 8 west
     */
    default int rotateBlockAccessFlags(int rotation) {
        int sides = getBlockAccessFlags() & 0xF;
        int turn = rotation & 0x3;
        if (turn == 0) {
            return sides;
        }
        return ((sides << turn) | (sides >> (4 - turn))) & 0xF;
    }
}

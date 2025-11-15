package com.tonic.api;

public interface TObjectComposition
{
    int getBlockAccessFlags();

    default int rotateBlockAccessFlags(int rotation) {
        // Rotate the 4 bits by rotation positions
        rotation = rotation & 0x3; // Ensure 0-3

        // Extract the 4 directional bits
        int dirBits = getBlockAccessFlags() & 0xF;

        // Rotate them
        int rotated = ((dirBits << rotation) | (dirBits >> (4 - rotation))) & 0xF;

        // Preserve any higher bits and combine
        return (getBlockAccessFlags() & ~0xF) | rotated;
    }
}

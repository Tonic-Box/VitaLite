package com.tonic.api;

/**
 * What the client reads from an object definition and does not keep: its approach-side mask and whether it collides
 */
public interface TObjectComposition
{
    /**
     * Gets the definition's approach-side mask as the cache stores it
     * @return the mask, bits 1 north, 2 east, 4 south and 8 west blocked, 0 when the definition has none
     */
    int getBlockAccessFlags();

    /**
     * Gets the definition's interact type, 0 for a hollow definition
     * @return 0 when it does not collide, 1 when it blocks only as an object, 2 when it also blocks projectiles
     */
    int getInteractType();
}

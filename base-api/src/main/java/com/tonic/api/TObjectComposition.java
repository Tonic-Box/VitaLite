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
}

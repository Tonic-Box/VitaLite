package com.tonic.api;

public interface TPlayerComposition
{
    /**
     * Gets where each equipment slot's faces start in the model last built from this appearance's equipment.
     *
     * @return the first face of each of the 12 slots, then the face count, or null when the model last built was not
     * from the equipment worn now
     */
    int[] getSlotFaces();
}

package com.tonic.api;

public interface TModel
{
    /**
     * Gets the model's vertex groups: for each label the animation frames move, the vertices that carry it.
     *
     * @return the vertex indices by label, or null for a model with no labels
     */
    int[][] getVertexGroups();

    /**
     * Sets each face's transparency, 0 opaque to 255 clear.
     *
     * @param transparencies one per face, or null for a wholly opaque model
     */
    void setFaceTransparencies(byte[] transparencies);
}

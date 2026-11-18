package com.tonic.api;

/**
 * The 3D rasterizer's projection state.
 */
public interface TRasterState
{
    /**
     * @return the zoom points are projected with
     */
    int getZoom();

    /**
     * Sets the zoom points are projected with.
     *
     * @param zoom the zoom
     */
    void setZoom(int zoom);
}

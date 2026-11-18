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

    /**
     * @return the width of the area triangles are filled in
     */
    int getRasterWidth();

    /**
     * @return the height of the area triangles are filled in
     */
    int getRasterHeight();

    /**
     * Sets the height of the area triangles are filled in; 0 fills nothing while still projecting and picking.
     *
     * @param height the height
     */
    void setRasterHeight(int height);
}

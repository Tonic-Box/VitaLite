package com.tonic.services.ground;

/**
 * Something drawn on the ground of the world, under whatever stands on it: tile markers, paths, areas. Painted once a frame on the client thread.
 */
@FunctionalInterface
public interface GroundOverlay
{
    /**
     * Draws this frame's shapes.
     *
     * @param canvas where to draw them
     */
    void paint(GroundCanvas canvas);

    /**
     * What this frame's shapes depend on. While it equals what it was when the overlay last painted, the shapes painted then are kept and not painted again.
     *
     * @return what the shapes depend on, or null to paint every frame
     */
    default Object dependsOn()
    {
        return null;
    }
}

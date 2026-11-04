package com.tonic.services;

import lombok.Getter;

/**
 * Whether what the desktop draws flat over the world is left out, as while a VR headset shows the world
 * itself: RuneLite's overlays laid over the scene and the game's own markers there, all placed for the
 * desktop's camera, not the headset's.
 */
public final class WorldOverlays
{
    /**
     * -- GETTER --
     *
     * @return whether what is drawn flat over the world is left out
     */
    @Getter
    private static volatile boolean hidden;

    private WorldOverlays()
    {
    }

    /**
     * Leaves out, or brings back, what is drawn flat over the world.
     *
     * @param hide whether it is left out
     */
    public static void setHidden(boolean hide)
    {
        hidden = hide;
    }

    /**
     * Gets whether a RuneLite overlay layer is left out: the two laid over the scene, under the interfaces.
     *
     * @param layer the overlay layer, a RuneLite OverlayLayer
     * @return whether it is left out
     */
    public static boolean skips(Object layer)
    {
        if (!hidden || !(layer instanceof Enum))
        {
            return false;
        }
        String name = ((Enum<?>) layer).name();
        return name.equals("ABOVE_SCENE") || name.equals("UNDER_WIDGETS");
    }
}

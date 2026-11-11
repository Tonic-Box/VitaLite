package com.tonic.services.ground;

import com.tonic.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of overlays drawn on the ground of the world, under what stands on it.
 */
public final class GroundOverlays
{
    private static final long ERROR_QUIET_MILLIS = 5000;

    private static final List<Painted> overlays = new CopyOnWriteArrayList<>();
    private static long lastErrorMillis;

    private GroundOverlays()
    {
    }

    /**
     * Adds an overlay to draw on the ground every frame, after those added before it.
     *
     * @param overlay what to draw
     */
    public static void add(GroundOverlay overlay)
    {
        if (overlay == null)
        {
            throw new IllegalArgumentException("overlay cannot be null");
        }
        overlays.add(new Painted(overlay));
    }

    /**
     * Removes an overlay added before.
     *
     * @param overlay the overlay
     * @return whether it was added
     */
    public static boolean remove(GroundOverlay overlay)
    {
        return overlays.removeIf(painted -> painted.overlay == overlay);
    }

    static void paintInto(List<GroundLayer> frame)
    {
        for (Painted painted : overlays)
        {
            frame.add(painted.paint());
        }
    }

    private static void report(GroundOverlay overlay, RuntimeException cause)
    {
        long now = System.currentTimeMillis();
        if (now - lastErrorMillis < ERROR_QUIET_MILLIS)
        {
            return;
        }
        lastErrorMillis = now;
        Logger.error("Ground overlay " + overlay.getClass().getName() + " failed: " + cause);
    }

    private static final class Painted
    {
        private final GroundOverlay overlay;
        private final GroundLayer layer = new GroundLayer();
        private Object paintedFor;

        Painted(GroundOverlay overlay)
        {
            this.overlay = overlay;
        }

        GroundLayer paint()
        {
            try
            {
                Object dependsOn = overlay.dependsOn();
                if (dependsOn != null && dependsOn.equals(paintedFor))
                {
                    return layer;
                }
                layer.clear();
                paintedFor = null;
                overlay.paint(GroundCanvas.over(layer));
                paintedFor = dependsOn;
            }
            catch (RuntimeException e)
            {
                layer.clear();
                report(overlay, e);
            }
            return layer;
        }
    }
}

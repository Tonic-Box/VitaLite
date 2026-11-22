package com.tonic.services.streamer;

import java.awt.Graphics2D;

/**
 * RuneLite's notification flash as the mirror shows it: a flash started by content that is not stock is drawn for the real view only. Flashes are drawn on the client thread; they may be started on any thread.
 */
public final class MirrorFlash
{
    private static volatile boolean hidden;
    private static boolean hiding;

    private MirrorFlash()
    {
    }

    /**
     * Notes who started the flash just set. Called by the notifier each time a notification sets its flash, whether or not streamer mode is on.
     */
    public static void flashSet()
    {
        hidden = !StockContent.callerIsStock(StockContent.NOTIFIER);
    }

    /**
     * Starts drawing the flash. Called by the notifier while streamer mode is on.
     *
     * @param graphics the graphics the flash is drawn with
     * @return the graphics to draw it with
     */
    public static Graphics2D begin(Graphics2D graphics)
    {
        hiding = hidden;
        return hiding ? MirrorHiding.hide(graphics) : graphics;
    }

    /**
     * Ends drawing the flash. Called by the notifier while streamer mode is on.
     */
    public static void done()
    {
        if (hiding)
        {
            hiding = false;
            MirrorHiding.end();
        }
    }
}

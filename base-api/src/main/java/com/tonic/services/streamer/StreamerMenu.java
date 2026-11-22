package com.tonic.services.streamer;

import com.tonic.Logger;
import com.tonic.api.TMenu;
import com.tonic.services.profiler.frames.FrameProfiler;

/**
 * Where the client's menu drawing reports to the streamer mirror, which shows the menu and the mouseover text with only stock entries. The client calls in here; the mirror registers the handler that does the work.
 */
public final class StreamerMenu
{
    private static final int MENU = FrameProfiler.timer("mirror.menu");
    private static final int MOUSEOVER = FrameProfiler.timer("mirror.mouseover");
    private static volatile Handler handler;
    private static boolean failureReported;
    private static long menuBegan;
    private static long textBegan;

    private StreamerMenu()
    {
    }

    /**
     * @param menuHandler the handler to report to, or null to stop
     */
    public static void setHandler(Handler menuHandler)
    {
        handler = menuHandler;
    }

    /**
     * Called by the menu after it creates an entry on a caller's behalf, while streamer mode is on.
     *
     * @param entry the entry, the client's MenuEntry
     */
    public static void created(Object entry)
    {
        Handler current = handler;
        if (current != null)
        {
            current.created(entry);
        }
    }

    /**
     * Called by the menu before it draws, while streamer mode is on.
     *
     * @param menu the menu
     */
    public static void drawing(TMenu menu)
    {
        menuBegan = FrameProfiler.begin();
        Handler current = handler;
        if (current != null)
        {
            current.drawing(menu);
        }
    }

    /**
     * Called by the menu after it draws, while streamer mode is on.
     *
     * @param menu the menu
     */
    public static void drawn(TMenu menu)
    {
        Handler current = handler;
        if (current != null)
        {
            current.drawn(menu);
        }
        FrameProfiler.end(MENU, menuBegan);
    }

    /**
     * Called by the menu as it is laid out, while streamer mode is on.
     *
     * @param menu the menu
     * @param x where it is opened, across the canvas
     * @param y where it is opened, down the canvas
     */
    public static void laidOut(TMenu menu, int x, int y)
    {
        Handler current = handler;
        if (current != null)
        {
            current.laidOut(menu, x, y);
        }
    }

    /**
     * Called by the client before it draws the mouseover text, while streamer mode is on.
     *
     * @param text the text
     * @param x where it is drawn, across the canvas
     * @param y its baseline, down the canvas
     * @return whether the mirror shows other text, so the client must report the rest of the draw
     */
    public static boolean textBegin(String text, int x, int y)
    {
        long began = FrameProfiler.begin();
        Handler current = handler;
        boolean split = current != null && current.textBegin(text, x, y);
        if (split)
        {
            textBegan = began;
        }
        else
        {
            FrameProfiler.end(MOUSEOVER, began);
        }
        return split;
    }

    /**
     * Called by the client after it draws the real mouseover text, to give the menu the mirror's entries.
     *
     * @return whether the menu now holds the mirror's entries
     */
    public static boolean textSwap()
    {
        Handler current = handler;
        return current != null && current.textSwap();
    }

    /**
     * Works out the mirror's mouseover text and starts drawing it for the mirror.
     *
     * @param text the real text
     * @return the text to draw for the mirror, or null for none
     */
    public static String textMirror(String text)
    {
        Handler current = handler;
        return current == null ? null : current.textMirror(text);
    }

    /**
     * Called by the client once the mouseover text is done, to put the menu back.
     */
    public static void textDone()
    {
        Handler current = handler;
        if (current != null)
        {
            current.textDone();
        }
        FrameProfiler.end(MOUSEOVER, textBegan);
    }

    /**
     * Reports that drawing the mouseover text for the mirror failed, logging the first failure only. Called by the client, which then draws on as usual.
     *
     * @param error what went wrong
     */
    public static void failed(Throwable error)
    {
        if (!failureReported)
        {
            failureReported = true;
            Logger.error("Streamer mirror could not draw the mouseover text: " + error);
        }
    }

    /**
     * What the streamer mirror does as the client draws its menu.
     */
    public interface Handler
    {
        /**
         * @param entry the entry just created, the client's MenuEntry
         */
        void created(Object entry);

        /**
         * @param menu the menu about to draw
         */
        void drawing(TMenu menu);

        /**
         * @param menu the menu that drew
         */
        void drawn(TMenu menu);

        /**
         * @param menu the menu being laid out
         * @param x where it is opened, across the canvas
         * @param y where it is opened, down the canvas
         */
        void laidOut(TMenu menu, int x, int y);

        /**
         * @param text the mouseover text about to be drawn
         * @param x where it is drawn, across the canvas
         * @param y its baseline, down the canvas
         * @return whether the mirror shows other text
         */
        boolean textBegin(String text, int x, int y);

        /**
         * @return whether the menu now holds the mirror's entries
         */
        boolean textSwap();

        /**
         * @param text the real text
         * @return the text to draw for the mirror, or null for none
         */
        String textMirror(String text);

        /**
         * Puts the menu back once the mouseover text is done.
         */
        void textDone();
    }
}

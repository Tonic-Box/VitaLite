package com.tonic.services.streamer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Whether streamer mode is on, read by every hook streamer mode adds; the api registers how the mode is switched, so settings can switch it without depending on the api.
 */
public final class Streamer
{
    /**
     * True while the mirror runs.
     */
    public static volatile boolean active;

    /**
     * True while the client draws a frame that is handed to the mirror; the hooks that draw for the mirror read only this field, so frames the mirror skips, and every frame while streamer mode is off, are drawn as stock.
     */
    public static volatile boolean mirroring;

    /**
     * The most frames a second the client hands to the mirror.
     */
    public static volatile int frameRate = MirrorFrameRate.SIXTY.getFramesPerSecond();

    /**
     * The thread printing the mirror's sidebar, while it prints, for which MirrorColor gives stock colours.
     */
    public static volatile Thread mirrorPrinter;

    private static final String VITA_WELCOME = "<col=FFFFFF>Welcome to </col><col=00FFFF>VitaLite</col>";

    private static volatile String stockWelcome;

    private static final List<Consumer<Boolean>> listeners = new CopyOnWriteArrayList<>();
    private static volatile Consumer<Boolean> switcher;
    private static boolean uiReady;
    private static final List<Runnable> uiTasks = new ArrayList<>();

    private Streamer()
    {
    }

    /**
     * Gives the login screen VitaLite's welcome line in place of the stock one. Called by the client each time it reads its welcome line.
     *
     * @param stock the client's own welcome line
     * @return VitaLite's welcome line
     */
    public static String welcomeText(String stock)
    {
        stockWelcome = stock;
        return VITA_WELCOME;
    }

    /**
     * @param text a line the login screen draws
     * @return the client's own welcome line when the text is VitaLite's, which the streamer mirror shows instead, otherwise null
     */
    public static String stockWelcome(String text)
    {
        return VITA_WELCOME.equals(text) ? stockWelcome : null;
    }

    /**
     * @param switcher turns streamer mode on or off
     */
    public static void setSwitcher(Consumer<Boolean> switcher)
    {
        Streamer.switcher = switcher;
    }

    /**
     * Asks for streamer mode to be turned on or off.
     *
     * @param on whether it should be on
     * @return false when streamer mode is not available yet
     */
    public static boolean request(boolean on)
    {
        Consumer<Boolean> current = switcher;
        if (current == null)
        {
            return false;
        }
        current.accept(on);
        return true;
    }

    /**
     * Runs a task once RuneLite's window is built, at once if it already is.
     *
     * @param task what needs the window, such as adding a title bar button
     */
    public static synchronized void whenUiReady(Runnable task)
    {
        if (uiReady)
        {
            task.run();
        }
        else
        {
            uiTasks.add(task);
        }
    }

    /**
     * Marks RuneLite's window as built and runs the task waiting for it. Called at the end of the window's setup.
     */
    public static synchronized void uiReady()
    {
        uiReady = true;
        for (Runnable task : uiTasks)
        {
            task.run();
        }
        uiTasks.clear();
    }

    /**
     * @param listener told whenever streamer mode turns on or off
     */
    public static void addListener(Consumer<Boolean> listener)
    {
        listeners.add(listener);
    }

    /**
     * Tells the listeners streamer mode changed.
     *
     * @param on whether it is now on
     */
    public static void changed(boolean on)
    {
        for (Consumer<Boolean> listener : listeners)
        {
            listener.accept(on);
        }
    }
}

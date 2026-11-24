package com.tonic.services.worldselect;

import com.tonic.Static;
import com.tonic.services.streamer.Streamer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The gates the injected world select hooks read: whether VitaLite's world select stands in for the client's, and whether it is open.
 */
public final class WorldSelectBridge
{
    /**
     * True while VitaLite's world select is used in place of the client's: the setting is on and streamer mode is off.
     */
    public static volatile boolean enabled;

    /**
     * True while VitaLite's world select is open and painted each frame.
     */
    public static volatile boolean open;

    private static final List<Runnable> disableListeners = new CopyOnWriteArrayList<>();

    static
    {
        Streamer.addListener(on -> refresh());
        refresh();
    }

    private WorldSelectBridge()
    {
    }

    /**
     * Works out again whether VitaLite's world select is used, after the setting or streamer mode changes, and tells the listeners when it stops being used.
     */
    public static void refresh()
    {
        boolean was = enabled;
        enabled = Static.getVitaConfig().shouldUseCustomWorldSelect() && !Streamer.active;
        if (was && !enabled)
        {
            for (Runnable listener : disableListeners)
            {
                listener.run();
            }
        }
    }

    /**
     * @param listener run whenever VitaLite's world select stops being used, so an open one can close
     */
    public static void onDisabled(Runnable listener)
    {
        disableListeners.add(listener);
    }
}

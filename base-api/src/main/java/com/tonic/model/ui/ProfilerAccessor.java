package com.tonic.model.ui;

/**
 * Registry for the Profiler window toggle functionality.
 * This allows the profiler plugin (loaded in a separate classloader) to register
 * its toggle callback, which can then be invoked from VitaLiteOptionsPanel.
 */
public class ProfilerAccessor {
    private static volatile Runnable toggleCallback;

    /**
     * Register the profiler toggle callback.
     * This should be called by ProfilerPlugin when it starts up.
     */
    public static void registerToggleCallback(Runnable callback) {
        toggleCallback = callback;
    }

    /**
     * Unregister the profiler toggle callback.
     * This should be called by ProfilerPlugin when it shuts down.
     */
    public static void unregisterToggleCallback() {
        toggleCallback = null;
    }

    /**
     * Toggle the profiler window visibility.
     * @return true if the toggle was successful, false if the profiler is not available
     */
    public static boolean toggle() {
        Runnable callback = toggleCallback;
        if (callback != null) {
            callback.run();
            return true;
        }
        return false;
    }

    /**
     * @return true if the profiler is available (plugin is loaded and enabled)
     */
    public static boolean isAvailable() {
        return toggleCallback != null;
    }
}

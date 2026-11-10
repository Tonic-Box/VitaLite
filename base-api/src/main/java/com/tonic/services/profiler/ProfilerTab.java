package com.tonic.services.profiler;

import javax.swing.JComponent;

/**
 * One tab of the profiler window
 */
public interface ProfilerTab {
    /**
     * Gets the tab's title
     * @return the title shown on the tab
     */
    String title();

    /**
     * Gets the tab's content
     * @return the component shown when the tab is selected
     */
    JComponent component();

    /**
     * Called when the tab becomes the selected one
     */
    default void shown() {
    }

    /**
     * Called when another tab is selected or the window closes
     */
    default void hidden() {
    }

    /**
     * Refreshes the tab's data while it is the selected one
     */
    default void refresh() {
    }

    /**
     * Stops whatever the tab runs and releases its resources
     */
    default void dispose() {
    }
}

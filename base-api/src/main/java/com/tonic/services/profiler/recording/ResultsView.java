package com.tonic.services.profiler.recording;

import javax.swing.JComponent;

/**
 * One view of a recording's results inside the Recording tab
 */
interface ResultsView {
    /**
     * Gets the view's title
     * @return the title of its sub-tab
     */
    String title();

    /**
     * Gets the view's content
     * @return the component shown in its sub-tab
     */
    JComponent component();

    /**
     * Shows a recording's results
     * @param results the results, or null to clear the view
     */
    void show(MethodTimingResults results);
}

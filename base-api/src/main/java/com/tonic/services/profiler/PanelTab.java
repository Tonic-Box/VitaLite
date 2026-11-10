package com.tonic.services.profiler;

import javax.swing.JComponent;

/**
 * A tab around a ready-made panel that only needs cleaning up on close
 */
final class PanelTab implements ProfilerTab {
    private final String title;
    private final JComponent component;
    private final Runnable cleanup;

    /**
     * Creates the tab
     * @param title the tab's title
     * @param component the panel shown
     * @param cleanup run when the window closes
     */
    PanelTab(String title, JComponent component, Runnable cleanup) {
        this.title = title;
        this.component = component;
        this.cleanup = cleanup;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public JComponent component() {
        return component;
    }

    @Override
    public void dispose() {
        cleanup.run();
    }
}

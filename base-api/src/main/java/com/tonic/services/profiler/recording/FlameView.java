package com.tonic.services.profiler.recording;

import com.tonic.services.profiler.visualization.FlameGraphPanel;

import javax.swing.*;
import java.awt.*;

import static com.tonic.services.profiler.ProfilerTheme.*;
import static com.tonic.services.profiler.recording.RecordingViews.*;

/**
 * The recording's Java samples as a flame graph, outermost frames at the top
 */
final class FlameView implements ResultsView {
    private final FlameGraphPanel flame = new FlameGraphPanel();
    private final JPanel panel = viewPanel();

    /**
     * Creates the view
     */
    FlameView() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        JButton reset = button("Reset Zoom", NEUTRAL);
        reset.addActionListener(e -> flame.resetZoom());
        top.add(reset);
        top.add(label("Click a frame to focus it, scroll to zoom, drag with the middle button to pan."));
        panel.add(top, BorderLayout.NORTH);
        panel.add(flame, BorderLayout.CENTER);
    }

    @Override
    public String title() {
        return "Flame Graph";
    }

    @Override
    public JComponent component() {
        return panel;
    }

    @Override
    public void show(MethodTimingResults results) {
        flame.setRoot(results == null || results.flameGraphRoot.getSamples() == 0 ? null : results.flameGraphRoot);
    }
}

package com.tonic.services.profiler.recording;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import static com.tonic.services.profiler.ProfilerTheme.*;
import static com.tonic.services.profiler.recording.RecordingViews.*;

/**
 * Samples of threads inside native code: mostly waiting on sockets, locks, the window system or the GPU, not CPU work
 */
final class NativeView implements ResultsView {
    private final DefaultTableModel model = model(new Class<?>[]{String.class, Integer.class, Double.class, Integer.class}, "Method", "Samples In It", "Share", "Samples On Stack");
    private final JLabel summary = label("Threads blocked in native code show here, kept out of the CPU views.");
    private final JPanel panel = viewPanel();

    /**
     * Creates the view
     */
    NativeView() {
        panel.add(summary, BorderLayout.NORTH);
        JTable table = table(new JTable(model));
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(420);
        table.setDefaultRenderer(Double.class, percentRenderer());
        panel.add(scroll(table), BorderLayout.CENTER);
    }

    @Override
    public String title() {
        return "Blocked / Native";
    }

    @Override
    public JComponent component() {
        return panel;
    }

    @Override
    public void show(MethodTimingResults results) {
        model.setRowCount(0);
        if (results == null) {
            return;
        }
        for (MethodStats method : results.getNativeMethodsBySelfTime()) {
            model.addRow(new Object[]{method.methodKey, method.getSelfSamples(), Math.round(percent(method.getSelfSamples(), results.nativeSamples) * 10) / 10.0, method.getTotalSamples()});
        }
        summary.setText(String.format("%,d samples of threads inside native code: waiting on I/O, locks, the window system or the GPU. This is not CPU use.", results.nativeSamples));
    }
}

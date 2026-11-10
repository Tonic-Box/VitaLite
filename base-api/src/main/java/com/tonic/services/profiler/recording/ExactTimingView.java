package com.tonic.services.profiler.recording;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static com.tonic.services.profiler.ProfilerTheme.*;
import static com.tonic.services.profiler.recording.RecordingViews.*;

/**
 * Exact timings from code instrumented with MethodProfiler.begin and end, independent of recordings
 */
final class ExactTimingView implements ResultsView {
    private static final int TOTAL_MS = 2;
    private final DefaultTableModel model = model(new Class<?>[]{String.class, Long.class, Double.class, Double.class, Double.class, Double.class}, "Label", "Calls", "Total ms", "Average ms", "Min ms", "Max ms");
    private final JTable table = table(new JTable(model));
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField filter = field(new JTextField(18));
    private final JCheckBox enabled = new JCheckBox("Collect timings");
    private final JLabel summary = label(" ");
    private final JPanel panel = viewPanel();
    private double totalMs;

    /**
     * Creates the view
     */
    ExactTimingView() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        enabled.setOpaque(false);
        enabled.setForeground(TEXT);
        enabled.setSelected(MethodProfiler.isEnabled());
        enabled.addActionListener(e -> {
            MethodProfiler.setEnabled(enabled.isSelected());
            refresh();
        });
        top.add(enabled);
        top.add(label("Filter:"));
        top.add(filter);
        JButton refresh = button("Refresh", ACCENT);
        refresh.addActionListener(e -> refresh());
        top.add(refresh);
        JButton export = button("Export", ACCENT);
        export.addActionListener(e -> export());
        top.add(export);
        JButton clear = button("Clear", WARNING);
        clear.addActionListener(e -> {
            MethodProfiler.clear();
            refresh();
        });
        top.add(clear);
        panel.add(top, BorderLayout.NORTH);

        table.setRowSorter(sorter);
        table.setDefaultRenderer(Object.class, new ShareRenderer());
        table.setDefaultRenderer(Long.class, new ShareRenderer());
        table.setDefaultRenderer(Double.class, new ShareRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(320);
        onChange(filter, () -> sorter.setRowFilter(nameFilter(filter.getText())));
        panel.add(scroll(table), BorderLayout.CENTER);
        panel.add(summary, BorderLayout.SOUTH);
    }

    @Override
    public String title() {
        return "Exact Timing";
    }

    @Override
    public JComponent component() {
        return panel;
    }

    @Override
    public void show(MethodTimingResults results) {
        refresh();
    }

    /**
     * Reloads the timings collected so far
     */
    void refresh() {
        List<MethodProfiler.MethodTiming> timings = MethodProfiler.getAllTimings();
        model.setRowCount(0);
        totalMs = 0;
        for (MethodProfiler.MethodTiming timing : timings) {
            totalMs += timing.getTotalMs();
            model.addRow(new Object[]{timing.getLabel(), timing.getCallCount(), timing.getTotalMs(), timing.getAverageMs(), timing.getMinMs(), timing.getMaxMs()});
        }
        enabled.setSelected(MethodProfiler.isEnabled());
        summary.setText(timings.isEmpty() ? (MethodProfiler.isEnabled() ? "Collecting. Time code with MethodProfiler.begin(label) and end(label), or MethodProfiler.time(label)." : "Off. MethodProfiler calls cost nothing until timings are collected.") : String.format("%,d labels, %.1f ms timed in total", timings.size(), totalMs));
    }

    private void export() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("exact_timing_" + System.currentTimeMillis() + ".csv"));
        if (chooser.showSaveDialog(panel) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        String report = file.getName().toLowerCase().endsWith(".csv") ? MethodProfiler.generateCSVReport() : MethodProfiler.generateReport();
        try {
            Files.write(file.toPath(), report.getBytes(StandardCharsets.UTF_8));
            summary.setText("Exported " + file.getName());
        } catch (Exception e) {
            summary.setText("Export failed: " + e.getMessage());
        }
    }

    private final class ShareRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            Object shown = value instanceof Double ? String.format("%,.3f", (Double) value) : value instanceof Long ? String.format("%,d", (Long) value) : value;
            Component cell = super.getTableCellRendererComponent(table, shown, selected, focused, row, column);
            setHorizontalAlignment(value instanceof Number ? RIGHT : LEFT);
            if (!selected) {
                double rowTotal = (Double) model.getValueAt(table.convertRowIndexToModel(row), TOTAL_MS);
                cell.setBackground(heatBackground(totalMs == 0 ? 0 : rowTotal * 100 / totalMs));
                cell.setForeground(TEXT);
            }
            return cell;
        }
    }
}

package com.tonic.services.profiler.recording;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.function.Consumer;

import static com.tonic.services.profiler.ProfilerTheme.*;
import static com.tonic.services.profiler.recording.RecordingViews.*;

/**
 * Every sampled method with the samples it was running in (self) and on the stack in (total)
 */
final class MethodsView implements ResultsView {
    static final int SELF_PERCENT = 2;
    private final DefaultTableModel model = model(new Class<?>[]{String.class, Integer.class, Double.class, Long.class, Integer.class, Double.class, Long.class, String.class}, "Method", "Self Samples", "Self %", "Self ≈ms", "Total Samples", "Total %", "Total ≈ms", "Confidence");
    private final JTable table = table(new JTable(model));
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField filter = field(new JTextField(24));
    private final JLabel summary = label("Record to see which methods use the CPU.");
    private final JPanel panel = viewPanel();

    /**
     * Creates the view
     * @param onSelect told the class and method name of the method the user selects
     */
    MethodsView(Consumer<String> onSelect) {
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        search.setOpaque(false);
        search.add(label("Filter:"));
        search.add(filter);
        top.add(search, BorderLayout.WEST);
        top.add(summary, BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);

        table.setRowSorter(sorter);
        table.setDefaultRenderer(Object.class, new HeatRenderer());
        table.setDefaultRenderer(Integer.class, new HeatRenderer());
        table.setDefaultRenderer(Long.class, new HeatRenderer());
        table.setDefaultRenderer(Double.class, new HeatRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(380);
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0) {
                onSelect.accept((String) model.getValueAt(table.convertRowIndexToModel(row), 0));
            }
        });
        onChange(filter, () -> sorter.setRowFilter(nameFilter(filter.getText())));
        panel.add(scroll(table), BorderLayout.CENTER);
    }

    @Override
    public String title() {
        return "Methods";
    }

    @Override
    public JComponent component() {
        return panel;
    }

    @Override
    public void show(MethodTimingResults results) {
        model.setRowCount(0);
        if (results == null) {
            summary.setText("Record to see which methods use the CPU.");
            return;
        }
        int samples = results.totalSamples;
        int period = results.samplePeriodMs;
        for (MethodStats method : results.getAllMethodsBySelfTime()) {
            model.addRow(new Object[]{method.methodKey, method.getSelfSamples(), percent(method.getSelfSamples(), samples), (long) method.getSelfSamples() * period, method.getTotalSamples(), percent(method.getTotalSamples(), samples), (long) method.getTotalSamples() * period, confidence(method.getSelfSamples())});
        }
        String cut = results.truncatedSamples == 0 ? "" : String.format(" %.0f%% of stacks were cut short, so their outermost callers are missing.", percent(results.truncatedSamples, samples));
        summary.setText(String.format("%,d Java samples in %s, one per thread every %d ms; ≈ms is samples × period, not measured time.%s", samples, results.getFormattedDuration(), period, cut));
    }

    private final class HeatRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            Object shown = value instanceof Double ? String.format("%.1f%%", (Double) value) : value instanceof Number ? String.format("%,d", ((Number) value).longValue()) : value;
            Component cell = super.getTableCellRendererComponent(table, shown, selected, focused, row, column);
            setHorizontalAlignment(value instanceof Number ? RIGHT : LEFT);
            if (!selected) {
                cell.setBackground(heatBackground((Double) model.getValueAt(table.convertRowIndexToModel(row), SELF_PERCENT)));
                cell.setForeground(TEXT);
            }
            return cell;
        }
    }
}

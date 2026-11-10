package com.tonic.services.profiler.recording;

import com.tonic.services.profiler.heap.HeapSnapshot;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

import static com.tonic.services.profiler.ProfilerTheme.*;
import static com.tonic.services.profiler.recording.RecordingViews.*;

/**
 * What the recording allocated, by class and by the method that allocated it
 */
final class AllocationsView implements ResultsView {
    private final DefaultTableModel byClass = model(new Class<?>[]{String.class, Long.class, Double.class, Long.class}, "Allocated Class", "Bytes", "Share", "Events");
    private final DefaultTableModel bySite = model(new Class<?>[]{String.class, Long.class, Double.class, Long.class}, "Allocating Method", "Bytes", "Share", "Events");
    private final JLabel summary = label("Tick Record allocations before recording to see what allocates.");
    private final JPanel panel = viewPanel();

    /**
     * Creates the view
     */
    AllocationsView() {
        panel.add(summary, BorderLayout.NORTH);
        JPanel tables = new JPanel(new GridLayout(1, 2, 10, 0));
        tables.setOpaque(false);
        tables.add(scroll(styled(byClass)));
        tables.add(scroll(styled(bySite)));
        panel.add(tables, BorderLayout.CENTER);
    }

    @Override
    public String title() {
        return "Allocations";
    }

    @Override
    public JComponent component() {
        return panel;
    }

    @Override
    public void show(MethodTimingResults results) {
        byClass.setRowCount(0);
        bySite.setRowCount(0);
        if (results == null || results.allocations == null) {
            summary.setText("Tick Record allocations before recording to see what allocates.");
            return;
        }
        MethodTimingResults.Allocations allocations = results.allocations;
        fill(byClass, allocations.byClass, allocations.totalBytes, true);
        fill(bySite, allocations.bySite, allocations.totalBytes, false);
        summary.setText(String.format("About %s allocated in %s. JFR samples allocations by TLAB, so bytes are estimates weighted by buffer size.", formatBytes(allocations.totalBytes), results.getFormattedDuration()));
    }

    private static void fill(DefaultTableModel model, Map<String, long[]> totals, long all, boolean readable) {
        for (Map.Entry<String, long[]> entry : MethodTimingResults.Allocations.sorted(totals)) {
            long[] total = entry.getValue();
            model.addRow(new Object[]{readable ? HeapSnapshot.readable(entry.getKey()) : entry.getKey(), total[0], Math.round(percent(total[0], all) * 10) / 10.0, total[1]});
        }
    }

    private static JTable styled(DefaultTableModel model) {
        JTable table = table(new JTable(model));
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.setDefaultRenderer(Double.class, percentRenderer());
        table.setDefaultRenderer(Long.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                Object shown = table.convertColumnIndexToModel(column) == 1 ? formatBytes((Long) value) : String.format("%,d", (Long) value);
                Component cell = super.getTableCellRendererComponent(table, shown, selected, focused, row, column);
                setHorizontalAlignment(RIGHT);
                return cell;
            }
        });
        return table;
    }
}

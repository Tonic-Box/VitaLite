package com.tonic.services.profiler.recording;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

import static com.tonic.services.profiler.ProfilerTheme.*;
import static com.tonic.services.profiler.recording.RecordingViews.*;

/**
 * The selected method's callers and callees, counted from frames that were next to each other in the sampled stacks
 */
final class MethodDetailsView implements ResultsView {
    private final JLabel heading = label("Select a method in the Methods view.");
    private final DefaultTableModel callers = model(new Class<?>[]{String.class, Integer.class}, "Called by", "Samples");
    private final DefaultTableModel callees = model(new Class<?>[]{String.class, Integer.class}, "Calls", "Samples");
    private final JPanel panel = viewPanel();
    private MethodTimingResults results;

    /**
     * Creates the view
     */
    MethodDetailsView() {
        heading.setFont(UI_BOLD);
        panel.add(heading, BorderLayout.NORTH);
        JPanel tables = new JPanel(new GridLayout(1, 2, 10, 0));
        tables.setOpaque(false);
        tables.add(scroll(sortable(callers)));
        tables.add(scroll(sortable(callees)));
        panel.add(tables, BorderLayout.CENTER);
    }

    @Override
    public String title() {
        return "Method Details";
    }

    @Override
    public JComponent component() {
        return panel;
    }

    @Override
    public void show(MethodTimingResults results) {
        this.results = results;
        callers.setRowCount(0);
        callees.setRowCount(0);
        heading.setText("Select a method in the Methods view.");
    }

    /**
     * Shows one method's callers and callees
     * @param methodKey the class and method name
     */
    void select(String methodKey) {
        callers.setRowCount(0);
        callees.setRowCount(0);
        CallTreeNode node = results == null ? null : results.getMethodNode(methodKey);
        if (node == null) {
            heading.setText(methodKey + ": not sampled");
            return;
        }
        heading.setText(String.format("%s: %,d samples running, %,d on the stack", methodKey, node.getSelfSamples(), node.getTotalSamples()));
        for (Map.Entry<String, Integer> caller : node.getCallersSorted()) {
            callers.addRow(new Object[]{caller.getKey(), caller.getValue()});
        }
        for (Map.Entry<String, Integer> callee : node.getCalleesSorted()) {
            callees.addRow(new Object[]{callee.getKey(), callee.getValue()});
        }
    }

    private static JTable sortable(DefaultTableModel model) {
        JTable table = table(new JTable(model));
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(320);
        return table;
    }
}

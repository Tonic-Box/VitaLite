package com.tonic.services.profiler.heap;

import com.tonic.services.profiler.ProfilerTab;
import com.tonic.services.profiler.ProfilerTheme;
import com.tonic.util.ThreadPool;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.tonic.services.profiler.ProfilerTheme.*;

/**
 * Heap snapshots taken on demand and the per-class growth between any two of them
 */
public final class HeapTab implements ProfilerTab {
    private static final int MAX_SNAPSHOTS = 20;
    private static final List<Numbered> SNAPSHOTS = new ArrayList<>();
    private static int captured;

    private final JButton capture = button("Capture Snapshot", ACCENT);
    private final JLabel status = label("Snapshots run a full garbage collection, which pauses the client briefly.");
    private final JComboBox<Object> baseline = new JComboBox<>();
    private final JComboBox<Numbered> compare = new JComboBox<>();
    private final JTextField filter = field(new JTextField(20));
    private final JCheckBox changedOnly = new JCheckBox("Changed only", true);
    private final JLabel summary = label(" ");
    private final DiffModel model = new DiffModel();
    private final JTable table = table(new JTable(model));
    private final TableRowSorter<DiffModel> sorter = new TableRowSorter<>(model);
    private final JPanel panel;
    private boolean updating;

    /**
     * Creates the tab
     */
    public HeapTab() {
        panel = page(new BorderLayout(0, 10));

        JPanel top = new JPanel(new GridLayout(3, 1, 0, 6));
        top.setOpaque(false);
        JPanel actions = row();
        capture.setPreferredSize(new Dimension(160, 30));
        capture.addActionListener(e -> capture());
        actions.add(capture);
        JButton clear = button("Clear Snapshots", NEUTRAL);
        clear.setPreferredSize(new Dimension(150, 30));
        clear.addActionListener(e -> clearSnapshots());
        actions.add(clear);
        JButton export = button("Export CSV", ACCENT);
        export.addActionListener(e -> export());
        actions.add(export);
        actions.add(status);
        top.add(actions);

        JPanel selection = row();
        selection.add(label("Baseline:"));
        style(baseline);
        selection.add(baseline);
        selection.add(label("Compare:"));
        style(compare);
        selection.add(compare);
        selection.add(label("Filter:"));
        selection.add(filter);
        changedOnly.setOpaque(false);
        changedOnly.setForeground(TEXT);
        selection.add(changedOnly);
        top.add(selection);
        top.add(summary);
        panel.add(top, BorderLayout.NORTH);

        table.setRowSorter(sorter);
        table.setDefaultRenderer(Long.class, new NumberRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(380);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        panel.add(scroll(table), BorderLayout.CENTER);

        baseline.addActionListener(e -> showDiff());
        compare.addActionListener(e -> showDiff());
        changedOnly.addActionListener(e -> applyFilter());
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        reloadChoices();
    }

    @Override
    public String title() {
        return "Heap Snapshots";
    }

    @Override
    public JComponent component() {
        return panel;
    }

    private void capture() {
        capture.setEnabled(false);
        status.setText("Capturing...");
        ThreadPool.submit(() -> {
            String failure = null;
            try {
                HeapSnapshot snapshot = HeapSnapshot.capture();
                synchronized (SNAPSHOTS) {
                    SNAPSHOTS.add(new Numbered(++captured, snapshot));
                    while (SNAPSHOTS.size() > MAX_SNAPSHOTS) {
                        SNAPSHOTS.remove(0);
                    }
                }
            } catch (RuntimeException e) {
                failure = e.getMessage();
            }
            String error = failure;
            SwingUtilities.invokeLater(() -> {
                capture.setEnabled(true);
                status.setText(error == null ? "Snapshot taken. Compare it with an earlier one to see what grew." : "Capture failed: " + error);
                reloadChoices();
            });
        });
    }

    private void clearSnapshots() {
        synchronized (SNAPSHOTS) {
            SNAPSHOTS.clear();
        }
        reloadChoices();
    }

    private void reloadChoices() {
        List<Numbered> snapshots;
        synchronized (SNAPSHOTS) {
            snapshots = new ArrayList<>(SNAPSHOTS);
        }
        updating = true;
        baseline.removeAllItems();
        compare.removeAllItems();
        baseline.addItem("None (show the whole heap)");
        for (Numbered snapshot : snapshots) {
            baseline.addItem(snapshot);
            compare.addItem(snapshot);
        }
        if (!snapshots.isEmpty()) {
            compare.setSelectedIndex(snapshots.size() - 1);
            baseline.setSelectedIndex(snapshots.size() >= 2 ? snapshots.size() - 1 : 0);
        }
        updating = false;
        showDiff();
    }

    private void showDiff() {
        if (updating) {
            return;
        }
        Numbered after = (Numbered) compare.getSelectedItem();
        if (after == null) {
            model.set(Collections.emptyList());
            summary.setText("No snapshots yet.");
            return;
        }
        Numbered before = baseline.getSelectedItem() instanceof Numbered ? (Numbered) baseline.getSelectedItem() : null;
        List<HeapDiff.Row> rows = HeapDiff.between(before == null ? null : before.snapshot, after.snapshot);
        model.set(rows);
        applyFilter();
        if (before == null) {
            summary.setText(String.format("Snapshot #%d: %s in %,d objects of %,d classes", after.number, formatBytes(after.snapshot.totalBytes), after.snapshot.totalInstances, rows.size()));
            return;
        }
        long bytes = after.snapshot.totalBytes - before.snapshot.totalBytes;
        long objects = after.snapshot.totalInstances - before.snapshot.totalInstances;
        long changed = rows.stream().filter(row -> row.byteDelta() != 0 || row.instanceDelta() != 0).count();
        summary.setText(String.format("#%d to #%d: %s%s, %+,d objects, %,d classes changed", before.number, after.number, bytes >= 0 ? "+" : "", formatBytes(bytes), objects, changed));
    }

    private void applyFilter() {
        String text = filter.getText().trim().toLowerCase(Locale.ROOT);
        boolean onlyChanged = changedOnly.isSelected() && baseline.getSelectedItem() instanceof Numbered;
        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DiffModel, ? extends Integer> entry) {
                HeapDiff.Row row = model.row(entry.getIdentifier());
                boolean changed = row.byteDelta() != 0 || row.instanceDelta() != 0;
                return (!onlyChanged || changed) && (text.isEmpty() || HeapSnapshot.readable(row.name).toLowerCase(Locale.ROOT).contains(text));
            }
        });
    }

    private void export() {
        if (model.getRowCount() == 0) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("heap_diff_" + System.currentTimeMillis() + ".csv"));
        if (chooser.showSaveDialog(panel) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try (PrintWriter writer = new PrintWriter(chooser.getSelectedFile())) {
            writer.println("Class,Module,InstancesBefore,InstancesAfter,InstanceDelta,BytesBefore,BytesAfter,ByteDelta");
            for (int view = 0; view < table.getRowCount(); view++) {
                HeapDiff.Row row = model.row(table.convertRowIndexToModel(view));
                writer.printf(Locale.ROOT, "\"%s\",\"%s\",%d,%d,%d,%d,%d,%d%n", HeapSnapshot.readable(row.name), row.module, row.instancesBefore, row.instancesAfter, row.instanceDelta(), row.bytesBefore, row.bytesAfter, row.byteDelta());
            }
            status.setText("Exported " + chooser.getSelectedFile().getName());
        } catch (Exception e) {
            status.setText("Export failed: " + e.getMessage());
        }
    }

    private static JPanel row() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        return row;
    }

    private static void style(JComboBox<?> box) {
        box.setBackground(PANEL);
        box.setForeground(TEXT);
    }

    private static final class Numbered {
        final int number;
        final HeapSnapshot snapshot;

        Numbered(int number, HeapSnapshot snapshot) {
            this.number = number;
            this.snapshot = snapshot;
        }

        @Override
        public String toString() {
            return String.format("#%d  %s  %s", number, new SimpleDateFormat("HH:mm:ss").format(new Date(snapshot.time)), formatBytes(snapshot.totalBytes));
        }
    }

    private static final class DiffModel extends AbstractTableModel {
        private final String[] columns = {"Class", "Module", "Count Before", "Count After", "Count Change", "Bytes Before", "Bytes After", "Bytes Change"};
        private List<HeapDiff.Row> rows = new ArrayList<>();

        void set(List<HeapDiff.Row> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        HeapDiff.Row row(int index) {
            return rows.get(index);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column < 2 ? String.class : Long.class;
        }

        @Override
        public Object getValueAt(int index, int column) {
            HeapDiff.Row row = rows.get(index);
            switch (column) {
                case 0:
                    return HeapSnapshot.readable(row.name);
                case 1:
                    return row.module;
                case 2:
                    return row.instancesBefore;
                case 3:
                    return row.instancesAfter;
                case 4:
                    return row.instanceDelta();
                case 5:
                    return row.bytesBefore;
                case 6:
                    return row.bytesAfter;
                default:
                    return row.byteDelta();
            }
        }
    }

    private static final class NumberRenderer extends DefaultTableCellRenderer {
        NumberRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            int modelColumn = table.convertColumnIndexToModel(column);
            long number = (Long) value;
            boolean delta = modelColumn == 4 || modelColumn == 7;
            String text = modelColumn >= 5 ? formatBytes(number) : String.format("%,d", number);
            Component cell = super.getTableCellRendererComponent(table, delta && number > 0 ? "+" + text : text, selected, focused, row, column);
            if (!selected) {
                cell.setForeground(delta && number > 0 ? ProfilerTheme.WARNING : delta && number < 0 ? SUCCESS : TEXT);
            }
            return cell;
        }
    }
}

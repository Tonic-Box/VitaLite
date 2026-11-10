package com.tonic.services.profiler.threads;

import com.sun.management.ThreadMXBean;
import com.tonic.services.profiler.ProfilerTab;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.tonic.services.profiler.ProfilerTheme.*;

/**
 * The live threads with how much CPU each used since the last refresh, how long they blocked and waited, and the
 * selected thread's stack and locks
 */
public final class ThreadsTab implements ProfilerTab {
    private static final int CPU_PERCENT = 3;

    private final ThreadMXBean threads = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private final ThreadModel model = new ThreadModel();
    private final JTable table = table(new JTable(model));
    private final TableRowSorter<ThreadModel> sorter = new TableRowSorter<>(model);
    private final JTextField filter = field(new JTextField(20));
    private final JLabel summary = label(" ");
    private final JTextArea stack = textArea(MONO);
    private final JPanel panel;
    private final Map<Long, Long> lastCpu = new HashMap<>();
    private long lastSampleNanos;
    private boolean enabledContention;
    private long selectedId = -1;

    /**
     * Creates the tab
     */
    public ThreadsTab() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(label("Filter:"));
        top.add(filter);
        top.add(summary);

        table.setRowSorter(sorter);
        sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(CPU_PERCENT, SortOrder.DESCENDING)));
        table.setDefaultRenderer(Double.class, new NumberRenderer());
        table.setDefaultRenderer(Long.class, new NumberRenderer());
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0) {
                selectedId = model.row(table.convertRowIndexToModel(row)).id;
                showStack();
            }
        });
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

        JPanel stackPanel = section("Stack and locks");
        stackPanel.setLayout(new BorderLayout());
        stackPanel.add(scroll(stack), BorderLayout.CENTER);
        stackPanel.setPreferredSize(new Dimension(0, 260));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll(table), stackPanel);
        split.setResizeWeight(0.6);
        split.setBorder(null);

        panel = page(new BorderLayout(0, 8));
        panel.add(top, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
    }

    @Override
    public String title() {
        return "Threads";
    }

    @Override
    public JComponent component() {
        return panel;
    }

    @Override
    public void shown() {
        if (threads.isThreadContentionMonitoringSupported() && !threads.isThreadContentionMonitoringEnabled()) {
            threads.setThreadContentionMonitoringEnabled(true);
            enabledContention = true;
        }
        refresh();
    }

    @Override
    public void hidden() {
        if (enabledContention) {
            threads.setThreadContentionMonitoringEnabled(false);
            enabledContention = false;
        }
    }

    @Override
    public void refresh() {
        long[] ids = threads.getAllThreadIds();
        ThreadInfo[] infos = threads.getThreadInfo(ids, 0);
        long[] cpu = threads.isThreadCpuTimeEnabled() ? threads.getThreadCpuTime(ids) : new long[0];
        long now = System.nanoTime();
        long elapsed = lastSampleNanos == 0 ? 0 : now - lastSampleNanos;

        List<Row> rows = new ArrayList<>();
        Map<Long, Long> cpuNow = new HashMap<>();
        double totalPercent = 0;
        for (int index = 0; index < ids.length; index++) {
            ThreadInfo info = infos[index];
            if (info == null) {
                continue;
            }
            long cpuNanos = index < cpu.length ? cpu[index] : -1;
            Long previous = lastCpu.get(ids[index]);
            double percent = cpuNanos >= 0 && previous != null && elapsed > 0 ? Math.max(0, (cpuNanos - previous) * 100.0 / elapsed) : 0;
            if (cpuNanos >= 0) {
                cpuNow.put(ids[index], cpuNanos);
            }
            totalPercent += percent;
            rows.add(new Row(info, percent, cpuNanos));
        }
        lastCpu.clear();
        lastCpu.putAll(cpuNow);
        lastSampleNanos = now;

        model.set(rows);
        boolean reselected = reselect();
        summary.setText(String.format("%d threads, %.0f%% of a core in use between refreshes; %s", rows.size(), totalPercent, threads.isThreadContentionMonitoringEnabled() ? "blocked and waited times count from when this tab was opened" : "blocked and waited times unavailable"));
        if (selectedId >= 0 && !reselected) {
            showStack();
        }
    }

    @Override
    public void dispose() {
        hidden();
    }

    private boolean reselect() {
        for (int view = 0; view < table.getRowCount(); view++) {
            if (model.row(table.convertRowIndexToModel(view)).id == selectedId) {
                table.getSelectionModel().setSelectionInterval(view, view);
                return true;
            }
        }
        return false;
    }

    private void applyFilter() {
        String text = filter.getText().trim().toLowerCase(Locale.ROOT);
        sorter.setRowFilter(text.isEmpty() ? null : new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends ThreadModel, ? extends Integer> entry) {
                return model.row(entry.getIdentifier()).name.toLowerCase(Locale.ROOT).contains(text);
            }
        });
    }

    private void showStack() {
        ThreadInfo[] found = threads.getThreadInfo(new long[]{selectedId}, threads.isObjectMonitorUsageSupported(), threads.isSynchronizerUsageSupported());
        ThreadInfo info = found.length == 0 ? null : found[0];
        if (info == null) {
            setText(stack, "Thread " + selectedId + " has ended.");
            return;
        }
        StringBuilder text = new StringBuilder();
        text.append(String.format("\"%s\" #%d %s%s\n", info.getThreadName(), info.getThreadId(), info.getThreadState(), info.isDaemon() ? " daemon" : ""));
        if (info.getLockName() != null) {
            text.append("  waiting on ").append(info.getLockName());
            if (info.getLockOwnerName() != null) {
                text.append(" held by \"").append(info.getLockOwnerName()).append("\" #").append(info.getLockOwnerId());
            }
            text.append('\n');
        }
        StackTraceElement[] frames = info.getStackTrace();
        MonitorInfo[] monitors = info.getLockedMonitors();
        for (int depth = 0; depth < frames.length; depth++) {
            text.append("    at ").append(frames[depth]).append('\n');
            for (MonitorInfo monitor : monitors) {
                if (monitor.getLockedStackDepth() == depth) {
                    text.append("      - locked ").append(monitor).append('\n');
                }
            }
        }
        LockInfo[] synchronizers = info.getLockedSynchronizers();
        if (synchronizers.length > 0) {
            text.append("\n  Holds:\n");
            for (LockInfo lock : synchronizers) {
                text.append("    ").append(lock).append('\n');
            }
        }
        setText(stack, text.toString());
    }

    private static final class Row {
        final long id;
        final String name;
        final Thread.State state;
        final boolean daemon;
        final double cpuPercent;
        final long cpuMs;
        final long blockedCount;
        final long blockedMs;
        final long waitedCount;
        final long waitedMs;

        Row(ThreadInfo info, double cpuPercent, long cpuNanos) {
            this.id = info.getThreadId();
            this.name = info.getThreadName();
            this.state = info.getThreadState();
            this.daemon = info.isDaemon();
            this.cpuPercent = cpuPercent;
            this.cpuMs = cpuNanos < 0 ? -1 : cpuNanos / 1_000_000;
            this.blockedCount = info.getBlockedCount();
            this.blockedMs = info.getBlockedTime();
            this.waitedCount = info.getWaitedCount();
            this.waitedMs = info.getWaitedTime();
        }
    }

    private static final class ThreadModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Name", "State", "CPU %", "CPU ms", "Blocked", "Blocked ms", "Waited", "Waited ms", "Daemon"};
        private final Class<?>[] types = {Long.class, String.class, String.class, Double.class, Long.class, Long.class, Long.class, Long.class, Long.class, String.class};
        private List<Row> rows = new ArrayList<>();

        void set(List<Row> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        Row row(int index) {
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
            return types[column];
        }

        @Override
        public Object getValueAt(int index, int column) {
            Row row = rows.get(index);
            switch (column) {
                case 0:
                    return row.id;
                case 1:
                    return row.name;
                case 2:
                    return row.state.toString();
                case CPU_PERCENT:
                    return row.cpuPercent;
                case 4:
                    return row.cpuMs;
                case 5:
                    return row.blockedCount;
                case 6:
                    return row.blockedMs;
                case 7:
                    return row.waitedCount;
                case 8:
                    return row.waitedMs;
                default:
                    return row.daemon ? "daemon" : "";
            }
        }
    }

    private static final class NumberRenderer extends DefaultTableCellRenderer {
        NumberRenderer() {
            setHorizontalAlignment(RIGHT);
        }

        @Override
        protected void setValue(Object value) {
            if (value instanceof Double) {
                setText(String.format("%.1f", (Double) value));
            } else if (value instanceof Long) {
                long number = (Long) value;
                setText(number < 0 ? "-" : String.format("%,d", number));
            } else {
                setText(String.valueOf(value));
            }
        }
    }
}

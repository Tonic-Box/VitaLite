package com.tonic.services.profiler.jvm;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;
import com.tonic.services.profiler.DiagnosticCommands;
import com.tonic.services.profiler.ProfilerTab;
import com.tonic.services.profiler.VM;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.lang.management.*;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.tonic.services.profiler.ProfilerTheme.*;

/**
 * The running JVM: what it is and how it was launched, its flags, its saved properties, its off-heap memory and the
 * real layout of any class
 */
public final class JvmTab implements ProfilerTab {
    private final HotSpotDiagnosticMXBean hotspot = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
    private final JTextArea overview = textArea(MONO);
    private final FlagModel flagModel = new FlagModel();
    private final JTable flagTable = table(new JTable(flagModel));
    private final TableRowSorter<FlagModel> flagSorter = new TableRowSorter<>(flagModel);
    private final JTextField flagFilter = field(new JTextField(20));
    private final JCheckBox changedOnly = new JCheckBox("Changed from default only");
    private final DefaultTableModel propertyModel = readOnlyModel("Property", "Value", "Source");
    private final JTable propertyTable = table(new JTable(propertyModel));
    private final TableRowSorter<DefaultTableModel> propertySorter = new TableRowSorter<>(propertyModel);
    private final JTextField propertyFilter = field(new JTextField(20));
    private final DefaultTableModel layoutModel = readOnlyModel("Offset", "Size", "Type", "Field", "Declared in");
    private final JTextField layoutClass = field(new JTextField(40));
    private final JLabel layoutSummary = label(" ");
    private final JPanel panel;
    private boolean loaded;

    /**
     * Creates the tab
     */
    public JvmTab() {
        JTabbedPane sections = new JTabbedPane();
        sections.setBackground(PANEL);
        sections.setForeground(TEXT);
        sections.addTab("Overview", scroll(overview));
        sections.addTab("Flags", flagsPage());
        sections.addTab("Properties", propertiesPage());
        sections.addTab("Class Layout", layoutPage());
        panel = page(new BorderLayout());
        panel.add(sections, BorderLayout.CENTER);
    }

    @Override
    public String title() {
        return "JVM";
    }

    @Override
    public JComponent component() {
        return panel;
    }

    @Override
    public void shown() {
        if (!loaded) {
            loaded = true;
            loadFlags();
            loadProperties();
        }
        refresh();
    }

    @Override
    public void refresh() {
        setText(overview, overviewText());
    }

    private String overviewText() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        StringBuilder text = new StringBuilder();
        text.append("JVM\n");
        text.append(String.format("  %s %s (%s)\n", runtime.getVmName(), runtime.getVmVersion(), runtime.getVmVendor()));
        text.append(String.format("  Java %s, PID %d, up %s\n", System.getProperty("java.version"), ProcessHandle.current().pid(), uptime(runtime.getUptime())));
        text.append("  GC: ").append(ManagementFactory.getGarbageCollectorMXBeans().stream().map(MemoryManagerMXBean::getName).collect(Collectors.joining(", "))).append('\n');
        CompilationMXBean compiler = ManagementFactory.getCompilationMXBean();
        if (compiler != null) {
            text.append(String.format("  Compiler: %s, %d ms compiling\n", compiler.getName(), compiler.getTotalCompilationTime()));
        }
        text.append(String.format("  Privileged access: %s\n\n", VM.isAvailable() ? "available" : "unavailable"));

        text.append("Launch arguments").append(VM.isAvailable() ? "\n" : " (from the management bean)\n");
        List<String> arguments = VM.isAvailable() ? Arrays.asList(VM.getRuntimeArguments()) : runtime.getInputArguments();
        for (String argument : arguments) {
            text.append("  ").append(argument).append('\n');
        }

        text.append("\nOff-heap memory\n");
        for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
            text.append(String.format("  %-7s buffers: %d, %s used, %s capacity\n", pool.getName(), pool.getCount(), formatBytes(pool.getMemoryUsed()), formatBytes(pool.getTotalCapacity())));
        }
        if (VM.isAvailable()) {
            text.append(String.format("  Direct memory limit: %s\n", formatBytes(VM.maxDirectMemory())));
        }
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.NON_HEAP && !pool.getName().startsWith("CodeHeap")) {
                MemoryUsage usage = pool.getUsage();
                text.append(String.format("  %s: %s used, %s committed\n", pool.getName(), formatBytes(usage.getUsed()), formatBytes(usage.getCommitted())));
            }
        }

        ClassLoadingMXBean classes = ManagementFactory.getClassLoadingMXBean();
        text.append("\nClasses\n");
        text.append(String.format("  %d loaded now, %d loaded in total, %d unloaded\n", classes.getLoadedClassCount(), classes.getTotalLoadedClassCount(), classes.getUnloadedClassCount()));
        if (VM.isAvailable()) {
            text.append("\nFinalizer queue\n");
            text.append(String.format("  %d waiting, %d at most\n", VM.getFinalRefCount(), VM.getPeakFinalRefCount()));
        }
        return text.toString();
    }

    private JPanel flagsPage() {
        JPanel page = page(new BorderLayout(0, 10));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(label("Filter:"));
        controls.add(flagFilter);
        changedOnly.setOpaque(false);
        changedOnly.setForeground(TEXT);
        controls.add(changedOnly);
        JButton reload = button("Reload", ACCENT);
        reload.addActionListener(e -> loadFlags());
        controls.add(reload);
        controls.add(label("Values in bold can be changed while running: double-click to edit."));
        page.add(controls, BorderLayout.NORTH);

        flagTable.setRowSorter(flagSorter);
        flagTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                Component cell = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                boolean writeable = flagModel.isWriteable(table.convertRowIndexToModel(row));
                cell.setFont(writeable && column == FlagModel.VALUE ? cell.getFont().deriveFont(Font.BOLD) : cell.getFont());
                return cell;
            }
        });
        flagFilter.getDocument().addDocumentListener(onChange(this::filterFlags));
        changedOnly.addActionListener(e -> filterFlags());
        page.add(scroll(flagTable), BorderLayout.CENTER);
        return page;
    }

    private JPanel propertiesPage() {
        JPanel page = page(new BorderLayout(0, 10));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(label("Filter:"));
        controls.add(propertyFilter);
        controls.add(label("System properties, plus the startup ones the JDK hides from them."));
        page.add(controls, BorderLayout.NORTH);
        propertyTable.setRowSorter(propertySorter);
        propertyFilter.getDocument().addDocumentListener(onChange(() -> propertySorter.setRowFilter(containsFilter(propertyFilter.getText()))));
        page.add(scroll(propertyTable), BorderLayout.CENTER);
        return page;
    }

    private JPanel layoutPage() {
        JPanel page = page(new BorderLayout(0, 10));
        JPanel top = new JPanel(new GridLayout(2, 1, 0, 6));
        top.setOpaque(false);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(label("Class:"));
        controls.add(layoutClass);
        JButton inspect = button("Inspect", ACCENT);
        inspect.addActionListener(e -> inspect());
        layoutClass.addActionListener(e -> inspect());
        controls.add(inspect);
        top.add(controls);
        top.add(layoutSummary);
        page.add(top, BorderLayout.NORTH);
        page.add(scroll(table(new JTable(layoutModel))), BorderLayout.CENTER);
        return page;
    }

    private void loadFlags() {
        try {
            Set<String> writeable = hotspot.getDiagnosticOptions().stream().filter(VMOption::isWriteable).map(VMOption::getName).collect(Collectors.toSet());
            flagModel.set(VmFlags.parse(DiagnosticCommands.run("vmFlags", "-all")), writeable);
        } catch (RuntimeException e) {
            flagModel.set(Collections.emptyList(), Collections.emptySet());
            JOptionPane.showMessageDialog(panel, "Could not read the flags: " + e.getMessage(), "JVM", JOptionPane.ERROR_MESSAGE);
        }
        filterFlags();
    }

    private void loadProperties() {
        propertyModel.setRowCount(0);
        Map<String, String[]> rows = new TreeMap<>();
        System.getProperties().forEach((key, value) -> rows.put(String.valueOf(key), new String[]{String.valueOf(value), "System"}));
        if (VM.isAvailable()) {
            VM.getSavedProperties().forEach((key, value) -> rows.putIfAbsent(key, new String[]{value, "Startup only (hidden)"}));
        }
        rows.forEach((key, row) -> propertyModel.addRow(new Object[]{key, row[0], row[1]}));
    }

    private void filterFlags() {
        String text = flagFilter.getText().trim().toLowerCase(Locale.ROOT);
        boolean changed = changedOnly.isSelected();
        flagSorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends FlagModel, ? extends Integer> entry) {
                VmFlags.Flag flag = flagModel.flag(entry.getIdentifier());
                return (!changed || !flag.isDefault()) && (text.isEmpty() || flag.name.toLowerCase(Locale.ROOT).contains(text));
            }
        });
    }

    private void inspect() {
        layoutModel.setRowCount(0);
        String name = layoutClass.getText().trim();
        if (name.isEmpty()) {
            return;
        }
        Class<?> type = findClass(name);
        if (type == null) {
            layoutSummary.setText("No loaded class named " + name);
            return;
        }
        if (!VM.isAvailable()) {
            layoutSummary.setText("Class layout needs privileged JVM access, which is unavailable");
            return;
        }
        try {
            ClassLayout layout = ClassLayout.of(type);
            for (ClassLayout.Slot slot : layout.slots) {
                layoutModel.addRow(new Object[]{slot.offset, slot.size, slot.field.getType().getTypeName(), slot.field.getName(), slot.field.getDeclaringClass().getName()});
            }
            String padding = layout.contendedPadding > 0 ? String.format(", %d bytes of @Contended padding", layout.contendedPadding) : "";
            layoutSummary.setText(String.format("%s: %d bytes per instance (header %d, references %d bytes, %d-byte alignment, %d fields%s)", type.getName(), layout.shallowSize, layout.headerSize, layout.referenceSize, layout.alignment, layout.slots.size(), padding));
        } catch (RuntimeException e) {
            layoutSummary.setText(e.getMessage());
        }
    }

    private static Class<?> findClass(String name) {
        Set<ClassLoader> loaders = new LinkedHashSet<>();
        loaders.add(ClassLoader.getSystemClassLoader());
        loaders.add(Thread.currentThread().getContextClassLoader());
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getContextClassLoader() != null) {
                loaders.add(thread.getContextClassLoader());
            }
        }
        for (ClassLoader loader : loaders) {
            try {
                return Class.forName(name, false, loader);
            } catch (ClassNotFoundException | LinkageError ignored) {
            }
        }
        return null;
    }

    private static String uptime(long millis) {
        Duration duration = Duration.ofMillis(millis);
        return String.format("%dh %02dm %02ds", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static RowFilter<Object, Object> containsFilter(String text) {
        String needle = text.trim().toLowerCase(Locale.ROOT);
        return needle.isEmpty() ? null : new RowFilter<>() {
            @Override
            public boolean include(Entry<?, ?> entry) {
                for (int column = 0; column < entry.getValueCount(); column++) {
                    if (entry.getStringValue(column).toLowerCase(Locale.ROOT).contains(needle)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private static DocumentListener onChange(Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        };
    }

    private final class FlagModel extends AbstractTableModel {
        static final int VALUE = 2;
        private final String[] columns = {"Name", "Type", "Value", "Kind", "Origin"};
        private List<VmFlags.Flag> flags = new ArrayList<>();
        private Set<String> writeable = new HashSet<>();

        void set(List<VmFlags.Flag> flags, Set<String> writeable) {
            this.flags = new ArrayList<>(flags);
            this.writeable = writeable;
            fireTableDataChanged();
        }

        VmFlags.Flag flag(int row) {
            return flags.get(row);
        }

        boolean isWriteable(int row) {
            return writeable.contains(flags.get(row).name);
        }

        @Override
        public int getRowCount() {
            return flags.size();
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
        public Object getValueAt(int row, int column) {
            VmFlags.Flag flag = flags.get(row);
            switch (column) {
                case 0:
                    return flag.name;
                case 1:
                    return flag.type;
                case VALUE:
                    return flag.value;
                case 3:
                    return flag.kinds;
                default:
                    return flag.origin;
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == VALUE && isWriteable(row);
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            String name = flags.get(row).name;
            try {
                hotspot.setVMOption(name, String.valueOf(value).trim());
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(panel, "Could not set " + name + ": " + e.getMessage(), "JVM", JOptionPane.ERROR_MESSAGE);
            }
            SwingUtilities.invokeLater(JvmTab.this::loadFlags);
        }
    }
}

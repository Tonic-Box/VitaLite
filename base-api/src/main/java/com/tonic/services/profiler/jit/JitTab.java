package com.tonic.services.profiler.jit;

import com.tonic.services.profiler.DiagnosticCommands;
import com.tonic.services.profiler.ProfilerTab;
import com.tonic.services.profiler.ProfilerTheme;
import com.tonic.util.ThreadPool;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import static com.tonic.services.profiler.ProfilerTheme.*;

/**
 * The JIT compiler: code cache use, what it has compiled at which tier, its queue, and directives that change how
 * methods compile
 */
public final class JitTab implements ProfilerTab {
    private static final String ALL_TIERS = "All tiers";

    private final JTextArea status = textArea(MONO);
    private final JTextArea queue = textArea(MONO);
    private final MethodModel methodModel = new MethodModel();
    private final JTable methodTable = table(new JTable(methodModel));
    private final TableRowSorter<MethodModel> methodSorter = new TableRowSorter<>(methodModel);
    private final JTextField methodFilter = field(new JTextField(24));
    private final JComboBox<String> tierFilter = new JComboBox<>(new String[]{ALL_TIERS, "Native wrapper", "C1", "C1 limited profile", "C1 full profile", "C2"});
    private final JLabel methodSummary = label("Press Load to list the compiled methods.");
    private final JButton load = button("Load", ACCENT);
    private final JTextField pattern = field(new JTextField(30));
    private final JComboBox<Directives.Action> action = new JComboBox<>(Directives.Action.values());
    private final JLabel directiveReply = label(" ");
    private final JTextArea directives = textArea(MONO);
    private final DefaultListModel<Directives.Rule> rules = new DefaultListModel<>();
    private final JList<Directives.Rule> ruleList = new JList<>(rules);
    private final JPanel panel;

    /**
     * Creates the tab
     */
    public JitTab() {
        JTabbedPane sections = new JTabbedPane();
        sections.setBackground(PANEL);
        sections.setForeground(TEXT);
        sections.addTab("Compiled Methods", methodsPage());
        sections.addTab("Compile Queue", scroll(queue));
        sections.addTab("Directives", directivesPage());

        JPanel statusPanel = section("Compiler");
        statusPanel.setLayout(new BorderLayout());
        statusPanel.add(status, BorderLayout.CENTER);

        panel = page(new BorderLayout(0, 10));
        panel.add(statusPanel, BorderLayout.NORTH);
        panel.add(sections, BorderLayout.CENTER);
    }

    @Override
    public String title() {
        return "JIT";
    }

    @Override
    public JComponent component() {
        return panel;
    }

    @Override
    public void shown() {
        refresh();
        showDirectives();
    }

    @Override
    public void refresh() {
        try {
            setText(status, statusText(CodeCache.parse(DiagnosticCommands.run("compilerCodecache"))));
            setText(queue, DiagnosticCommands.run("compilerQueue"));
        } catch (RuntimeException e) {
            setText(status, "The compiler's state could not be read: " + e.getMessage());
        }
    }

    private static String statusText(CodeCache cache) {
        StringBuilder text = new StringBuilder();
        CompilationMXBean compiler = ManagementFactory.getCompilationMXBean();
        if (compiler != null) {
            text.append(String.format("%s, %d ms compiling, compilation %s\n", compiler.getName(), compiler.getTotalCompilationTime(), cache.compilation));
        }
        for (CodeCache.Segment segment : cache.segments) {
            text.append(String.format("%-24s %,9d KB used of %,9d KB (peak %,d KB)\n", segment.name, segment.usedKb, segment.sizeKb, segment.maxUsedKb));
        }
        text.append(String.format("%,d compiled methods, %,d adapters, %,d code blobs", cache.nmethods, cache.adapters, cache.blobs));
        return text.toString();
    }

    private JPanel methodsPage() {
        JPanel page = page(new BorderLayout(0, 8));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(load);
        controls.add(label("Filter:"));
        controls.add(methodFilter);
        tierFilter.setBackground(PANEL);
        tierFilter.setForeground(TEXT);
        controls.add(tierFilter);

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 6));
        top.setOpaque(false);
        top.add(controls);
        top.add(methodSummary);
        page.add(top, BorderLayout.NORTH);

        methodTable.setRowSorter(methodSorter);
        methodSorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(MethodModel.SIZE, SortOrder.DESCENDING)));
        methodTable.getColumnModel().getColumn(MethodModel.NAME).setPreferredWidth(420);
        page.add(scroll(methodTable), BorderLayout.CENTER);

        load.addActionListener(e -> loadMethods());
        methodFilter.getDocument().addDocumentListener(onChange(this::filterMethods));
        tierFilter.addActionListener(e -> filterMethods());
        return page;
    }

    private JPanel directivesPage() {
        JPanel page = page(new BorderLayout(0, 8));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        form.setOpaque(false);
        form.add(label("Method:"));
        pattern.setToolTipText("Class and method with * wildcards, such as net.runelite.client.Foo.bar or java.lang.String.*");
        form.add(pattern);
        action.setBackground(PANEL);
        action.setForeground(TEXT);
        form.add(action);
        JButton add = button("Add", ACCENT);
        add.addActionListener(e -> runDirective(() -> Directives.add(new Directives.Rule(pattern.getText(), (Directives.Action) action.getSelectedItem()))));
        form.add(add);
        JButton remove = button("Remove Selected", WARNING);
        remove.setPreferredSize(new Dimension(160, 30));
        remove.addActionListener(e -> {
            Directives.Rule selected = ruleList.getSelectedValue();
            if (selected != null) {
                runDirective(() -> Directives.remove(selected));
            }
        });
        form.add(remove);
        JButton clear = button("Clear All", ProfilerTheme.ERROR);
        clear.addActionListener(e -> runDirective(Directives::clear));
        form.add(clear);

        JPanel top = new JPanel(new GridLayout(3, 1, 0, 4));
        top.setOpaque(false);
        top.add(form);
        top.add(label("Rules apply to compilations from now on. Code already compiled keeps running until it is recompiled."));
        top.add(directiveReply);
        page.add(top, BorderLayout.NORTH);

        ruleList.setBackground(PANEL);
        ruleList.setForeground(TEXT);
        ruleList.setFont(MONO);
        JPanel rulesPanel = section("Rules in force");
        rulesPanel.setLayout(new BorderLayout());
        rulesPanel.add(scroll(ruleList), BorderLayout.CENTER);
        JPanel stackPanel = section("Compiler directive stack");
        stackPanel.setLayout(new BorderLayout());
        stackPanel.add(scroll(directives), BorderLayout.CENTER);
        JPanel lists = new JPanel(new GridLayout(1, 2, 10, 0));
        lists.setOpaque(false);
        lists.add(rulesPanel);
        lists.add(stackPanel);
        page.add(lists, BorderLayout.CENTER);
        return page;
    }

    private void loadMethods() {
        load.setEnabled(false);
        methodSummary.setText("Loading...");
        ThreadPool.submit(() -> {
            List<CodeList.Method> methods;
            String error = null;
            try {
                methods = CodeList.parse(DiagnosticCommands.run("compilerCodelist"));
            } catch (RuntimeException e) {
                methods = Collections.emptyList();
                error = e.getMessage();
            }
            List<CodeList.Method> loaded = methods;
            String failure = error;
            SwingUtilities.invokeLater(() -> {
                methodModel.set(loaded);
                methodSummary.setText(failure != null ? "Could not list the compiled methods: " + failure : summary(loaded));
                filterMethods();
                load.setEnabled(true);
                load.setText("Reload");
            });
        });
    }

    private static String summary(List<CodeList.Method> methods) {
        Map<Integer, Integer> tiers = new TreeMap<>();
        long bytes = 0;
        for (CodeList.Method method : methods) {
            tiers.merge(method.tier, 1, Integer::sum);
            bytes += method.codeSize;
        }
        StringBuilder text = new StringBuilder(String.format("%,d methods, %s of code:", methods.size(), formatBytes(bytes)));
        tiers.forEach((tier, count) -> text.append(String.format("  %s %,d", CodeList.tierName(tier), count)));
        return text.toString();
    }

    private void filterMethods() {
        String text = methodFilter.getText().trim().toLowerCase(Locale.ROOT);
        String tier = (String) tierFilter.getSelectedItem();
        methodSorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends MethodModel, ? extends Integer> entry) {
                CodeList.Method method = methodModel.method(entry.getIdentifier());
                return (ALL_TIERS.equals(tier) || method.tierName().equals(tier)) && (text.isEmpty() || method.name.toLowerCase(Locale.ROOT).contains(text));
            }
        });
    }

    private void runDirective(Supplier<String> command) {
        try {
            String reply = command.get();
            directiveReply.setText(reply.isEmpty() ? "Done." : reply);
        } catch (RuntimeException e) {
            directiveReply.setText(e.getMessage());
        }
        showDirectives();
    }

    private void showDirectives() {
        rules.clear();
        for (Directives.Rule rule : Directives.rules()) {
            rules.addElement(rule);
        }
        try {
            setText(directives, summarizeDirectives(Directives.print()));
        } catch (RuntimeException e) {
            setText(directives, "The directives could not be read: " + e.getMessage());
        }
    }

    /**
     * Shortens the directive listing to one line per directive: what it matches, whether it excludes, its inline rules
     * @param listing the Compiler.directives_print output
     * @return one line per directive, newest first
     */
    static String summarizeDirectives(String listing) {
        StringBuilder text = new StringBuilder();
        String[] blocks = listing.split("(?m)^Directive:");
        for (String block : blocks) {
            if (block.trim().isEmpty()) {
                continue;
            }
            String match = valueOf(block, "matching:");
            String inline = valueOf(block, "inline:");
            boolean exclude = block.contains("Exclude:true");
            boolean isDefault = block.startsWith(" (default)");
            text.append(isDefault ? "(default) " : "").append(match);
            if (exclude) {
                text.append("  -> not compiled");
            }
            if (!inline.isEmpty() && !inline.equals("-")) {
                text.append("  -> inline ").append(inline);
            }
            text.append('\n');
        }
        return text.toString();
    }

    private static String valueOf(String block, String name) {
        for (String line : block.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(name)) {
                return trimmed.substring(name.length()).trim();
            }
        }
        return "";
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

    private static final class MethodModel extends AbstractTableModel {
        static final int NAME = 3;
        static final int SIZE = 5;
        private final String[] columns = {"ID", "Tier", "State", "Method", "Signature", "Code size"};
        private List<CodeList.Method> methods = new ArrayList<>();

        void set(List<CodeList.Method> methods) {
            this.methods = new ArrayList<>(methods);
            fireTableDataChanged();
        }

        CodeList.Method method(int row) {
            return methods.get(row);
        }

        @Override
        public int getRowCount() {
            return methods.size();
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
            return column == 0 || column == SIZE ? Long.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            CodeList.Method method = methods.get(row);
            switch (column) {
                case 0:
                    return (long) method.compileId;
                case 1:
                    return method.tierName();
                case 2:
                    return method.stateName();
                case NAME:
                    return method.name;
                case 4:
                    return method.signature;
                default:
                    return method.codeSize;
            }
        }
    }
}

package com.tonic.services.profiler.recording;

import com.tonic.services.profiler.visualization.FlameGraphNode;
import com.tonic.services.profiler.visualization.FlameGraphPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JFR-based method timing recording tab with multiple views:
 * - Table View: Flat list of methods sorted by time
 * - Flame Graph: Visual stack trace representation
 * - Call Tree: Hierarchical method breakdown
 * - Method Details: Callers/callees for selected method
 */
public class RecordingTab extends JPanel {
    private static final Color BG_COLOR = new Color(30, 31, 34);
    private static final Color PANEL_BG = new Color(40, 42, 46);
    private static final Color TEXT_COLOR = new Color(200, 200, 200);
    private static final Color ACCENT_COLOR = new Color(64, 156, 255);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);
    private static final Color ERROR_COLOR = new Color(244, 67, 54);

    // Hot method colors (for table row highlighting)
    private static final Color HOT_RED = new Color(244, 67, 54, 60);
    private static final Color HOT_ORANGE = new Color(255, 152, 0, 60);
    private static final Color HOT_YELLOW = new Color(255, 235, 59, 40);

    // Confidence level colors
    private static final Color CONFIDENCE_HIGH = new Color(76, 175, 80);
    private static final Color CONFIDENCE_MEDIUM = new Color(255, 193, 7);
    private static final Color CONFIDENCE_LOW = new Color(255, 152, 0);
    private static final Color CONFIDENCE_VERY_LOW = new Color(244, 67, 54);

    private final JFRMethodRecorder recorder;

    // Control components
    private JButton recordButton, stopButton, clearButton, exportButton;
    private JComboBox<String> sampleRateCombo;
    private JRadioButton allMethodsRadio, packageRadio, classRadio;
    private JTextField packageField, classField;
    private JLabel statusLabel, sampleCountLabel;

    // Sub-tabs
    private JTabbedPane viewTabs;

    // Table View components
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JLabel resultsSummaryLabel;

    // Flame Graph components
    private FlameGraphPanel flameGraphPanel;

    // Call Tree components
    private JTree callTree;
    private DefaultTreeModel callTreeModel;
    private JLabel callTreeSummaryLabel;

    // Method Details components
    private JPanel methodDetailsPanel;
    private JLabel methodNameLabel;
    private JTable callersTable, calleesTable;
    private DefaultTableModel callersModel, calleesModel;

    // Exact Timing components
    private JTable exactTimingTable;
    private DefaultTableModel exactTimingModel;
    private JLabel exactTimingSummaryLabel;
    private JCheckBox methodProfilerEnabledCheckbox;
    private JTextField exactTimingFilterField;

    // State
    private Timer statusTimer;
    private long recordingStartTime;
    private MethodTimingResults lastResults;
    private String selectedMethodKey;

    public RecordingTab() {
        this.recorder = new JFRMethodRecorder();

        setLayout(new BorderLayout(5, 5));
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        initializeUI();
        startStatusTimer();
    }

    private void initializeUI() {
        // Top - Controls
        add(createControlsPanel(), BorderLayout.NORTH);

        // Center - Tabbed views
        viewTabs = new JTabbedPane();
        viewTabs.setBackground(PANEL_BG);
        viewTabs.setForeground(TEXT_COLOR);
        styleTabPane(viewTabs);

        // Add view tabs
        viewTabs.addTab("Table", createTableViewPanel());
        viewTabs.addTab("Flame Graph", createFlameGraphPanel());
        viewTabs.addTab("Call Tree", createCallTreePanel());
        viewTabs.addTab("Method Details", createMethodDetailsPanel());
        viewTabs.addTab("Exact Timing", createExactTimingPanel());

        add(viewTabs, BorderLayout.CENTER);

        // Bottom - Actions
        add(createActionsPanel(), BorderLayout.SOUTH);
    }

    private void styleTabPane(JTabbedPane tabs) {
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tabs.setOpaque(true);
        tabs.setBackground(BG_COLOR);
    }

    // ==================== Controls Panel ====================

    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setOpaque(false);

        // Row 1: Record/Stop buttons and sample rate
        JPanel buttonsPanel = createStyledPanel("Recording Controls");
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 5));

        recordButton = createStyledButton("\u25CF Record", SUCCESS_COLOR);
        stopButton = createStyledButton("\u25A0 Stop", ERROR_COLOR);
        stopButton.setEnabled(false);

        recordButton.addActionListener(e -> startRecording());
        stopButton.addActionListener(e -> stopRecording());

        buttonsPanel.add(recordButton);
        buttonsPanel.add(stopButton);

        buttonsPanel.add(Box.createHorizontalStrut(15));
        buttonsPanel.add(createStyledLabel("Sample Rate:"));

        sampleRateCombo = new JComboBox<>(new String[]{"1ms", "5ms", "10ms", "20ms", "50ms"});
        sampleRateCombo.setSelectedIndex(2);
        styleComboBox(sampleRateCombo);
        buttonsPanel.add(sampleRateCombo);

        buttonsPanel.add(Box.createHorizontalStrut(15));
        statusLabel = createStyledLabel("Ready");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        buttonsPanel.add(statusLabel);

        buttonsPanel.add(Box.createHorizontalStrut(8));
        sampleCountLabel = createStyledLabel("");
        buttonsPanel.add(sampleCountLabel);

        panel.add(buttonsPanel, BorderLayout.NORTH);

        // Row 2: Filter options (more compact)
        JPanel filterPanel = createStyledPanel("Scope Filter");
        filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 3));

        ButtonGroup filterGroup = new ButtonGroup();

        allMethodsRadio = new JRadioButton("All");
        styleRadioButton(allMethodsRadio);
        allMethodsRadio.setSelected(true);
        filterGroup.add(allMethodsRadio);
        filterPanel.add(allMethodsRadio);

        packageRadio = new JRadioButton("Package:");
        styleRadioButton(packageRadio);
        filterGroup.add(packageRadio);
        filterPanel.add(packageRadio);

        packageField = new JTextField("com.tonic", 20);
        styleTextField(packageField);
        filterPanel.add(packageField);

        filterPanel.add(Box.createHorizontalStrut(10));

        classRadio = new JRadioButton("Class:");
        styleRadioButton(classRadio);
        filterGroup.add(classRadio);
        filterPanel.add(classRadio);

        classField = new JTextField("", 25);
        styleTextField(classField);
        filterPanel.add(classField);

        allMethodsRadio.addActionListener(e -> updateFilterFields());
        packageRadio.addActionListener(e -> updateFilterFields());
        classRadio.addActionListener(e -> updateFilterFields());
        updateFilterFields();

        panel.add(filterPanel, BorderLayout.CENTER);

        return panel;
    }

    private void updateFilterFields() {
        packageField.setEnabled(packageRadio.isSelected());
        classField.setEnabled(classRadio.isSelected());
    }

    // ==================== Table View Panel ====================

    private JPanel createTableViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(PANEL_BG);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        String[] columns = {"Method", "Self %", "~Self Time", "Total %", "~Total Time", "Samples", "Confidence"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(tableModel);
        styleTable(resultsTable);
        resultsTable.setDefaultRenderer(Object.class, new HeatmapCellRenderer());

        // Column widths
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(280);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(55);
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(75);
        resultsTable.getColumnModel().getColumn(3).setPreferredWidth(55);
        resultsTable.getColumnModel().getColumn(4).setPreferredWidth(75);
        resultsTable.getColumnModel().getColumn(5).setPreferredWidth(55);
        resultsTable.getColumnModel().getColumn(6).setPreferredWidth(65);

        // Selection listener for method details
        resultsTable.getSelectionModel().addListSelectionListener(this::onTableSelectionChanged);

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBackground(PANEL_BG);
        scrollPane.getViewport().setBackground(PANEL_BG);
        panel.add(scrollPane, BorderLayout.CENTER);

        resultsSummaryLabel = createStyledLabel("Click Record to start profiling");
        resultsSummaryLabel.setBorder(new EmptyBorder(5, 5, 0, 0));
        panel.add(resultsSummaryLabel, BorderLayout.SOUTH);

        return panel;
    }

    private void onTableSelectionChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;

        int row = resultsTable.getSelectedRow();
        if (row >= 0 && lastResults != null) {
            List<MethodStats> methods = lastResults.getAllMethodsBySelfTime();
            if (row < methods.size()) {
                selectedMethodKey = methods.get(row).methodKey;
                updateMethodDetails();
                // Switch to method details tab
                viewTabs.setSelectedIndex(3);
            }
        }
    }

    // ==================== Flame Graph Panel ====================

    private JPanel createFlameGraphPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(PANEL_BG);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        flameGraphPanel = new FlameGraphPanel();
        panel.add(flameGraphPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setOpaque(false);

        JButton resetZoomBtn = createStyledButton("Reset Zoom", ACCENT_COLOR);
        resetZoomBtn.setPreferredSize(new Dimension(100, 24));
        resetZoomBtn.addActionListener(e -> flameGraphPanel.resetZoom());
        controlPanel.add(resetZoomBtn);

        controlPanel.add(createStyledLabel("Click frame to zoom, click header to reset"));

        panel.add(controlPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== Call Tree Panel ====================

    private JPanel createCallTreePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(PANEL_BG);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("(No data)");
        callTreeModel = new DefaultTreeModel(rootNode);
        callTree = new JTree(callTreeModel);
        callTree.setBackground(PANEL_BG);
        callTree.setForeground(TEXT_COLOR);
        callTree.setFont(new Font("Consolas", Font.PLAIN, 12));
        callTree.setCellRenderer(new CallTreeCellRenderer());
        callTree.setRootVisible(true);
        callTree.setShowsRootHandles(true);

        // Selection listener
        callTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) callTree.getLastSelectedPathComponent();
            if (node != null && node.getUserObject() instanceof CallTreeNodeWrapper) {
                CallTreeNodeWrapper wrapper = (CallTreeNodeWrapper) node.getUserObject();
                selectedMethodKey = wrapper.node.getMethodKey();
                updateMethodDetails();
            }
        });

        JScrollPane scrollPane = new JScrollPane(callTree);
        scrollPane.setBackground(PANEL_BG);
        scrollPane.getViewport().setBackground(PANEL_BG);
        panel.add(scrollPane, BorderLayout.CENTER);

        callTreeSummaryLabel = createStyledLabel("Record profiling data to see call tree");
        callTreeSummaryLabel.setBorder(new EmptyBorder(5, 5, 0, 0));
        panel.add(callTreeSummaryLabel, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== Method Details Panel ====================

    private JPanel createMethodDetailsPanel() {
        methodDetailsPanel = new JPanel(new BorderLayout(5, 5));
        methodDetailsPanel.setBackground(PANEL_BG);
        methodDetailsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Header with method name
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(50, 52, 56));
        headerPanel.setBorder(new EmptyBorder(8, 10, 8, 10));

        methodNameLabel = new JLabel("Select a method from Table or Call Tree");
        methodNameLabel.setForeground(TEXT_COLOR);
        methodNameLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        headerPanel.add(methodNameLabel, BorderLayout.CENTER);

        methodDetailsPanel.add(headerPanel, BorderLayout.NORTH);

        // Split pane for callers and callees
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBackground(PANEL_BG);
        splitPane.setDividerSize(5);
        splitPane.setResizeWeight(0.5);

        // Callers panel
        JPanel callersPanel = createStyledPanel("Callers (who calls this method)");
        callersPanel.setLayout(new BorderLayout());
        String[] callerCols = {"Method", "Count"};
        callersModel = new DefaultTableModel(callerCols, 0);
        callersTable = new JTable(callersModel);
        styleTable(callersTable);
        callersTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        callersTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        callersPanel.add(new JScrollPane(callersTable), BorderLayout.CENTER);
        splitPane.setLeftComponent(callersPanel);

        // Callees panel
        JPanel calleesPanel = createStyledPanel("Callees (methods this calls)");
        calleesPanel.setLayout(new BorderLayout());
        String[] calleeCols = {"Method", "Count"};
        calleesModel = new DefaultTableModel(calleeCols, 0);
        calleesTable = new JTable(calleesModel);
        styleTable(calleesTable);
        calleesTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        calleesTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        calleesPanel.add(new JScrollPane(calleesTable), BorderLayout.CENTER);
        splitPane.setRightComponent(calleesPanel);

        methodDetailsPanel.add(splitPane, BorderLayout.CENTER);

        return methodDetailsPanel;
    }

    private void updateMethodDetails() {
        if (selectedMethodKey == null || lastResults == null) {
            methodNameLabel.setText("Select a method from Table or Call Tree");
            callersModel.setRowCount(0);
            calleesModel.setRowCount(0);
            return;
        }

        CallTreeNode node = lastResults.getMethodNode(selectedMethodKey);
        if (node == null) {
            methodNameLabel.setText("No details for: " + selectedMethodKey);
            callersModel.setRowCount(0);
            calleesModel.setRowCount(0);
            return;
        }

        // Update header
        methodNameLabel.setText(String.format("%s  |  Self: %d samples (%.1f%%)  |  Total: %d samples (%.1f%%)",
                node.getDisplayName(),
                node.getSelfSamples(), node.getSelfPercent(lastResults.totalSamples),
                node.getTotalSamples(), node.getTotalPercent(lastResults.totalSamples)));

        // Update callers
        callersModel.setRowCount(0);
        for (Map.Entry<String, Integer> entry : node.getCallersSorted()) {
            String shortName = getShortMethodName(entry.getKey());
            callersModel.addRow(new Object[]{shortName, entry.getValue()});
        }

        // Update callees
        calleesModel.setRowCount(0);
        for (Map.Entry<String, Integer> entry : node.getCalleesSorted()) {
            String shortName = getShortMethodName(entry.getKey());
            calleesModel.addRow(new Object[]{shortName, entry.getValue()});
        }
    }

    private String getShortMethodName(String methodKey) {
        int lastDot = methodKey.lastIndexOf('.');
        if (lastDot > 0) {
            String beforeMethod = methodKey.substring(0, lastDot);
            int secondLastDot = beforeMethod.lastIndexOf('.');
            if (secondLastDot > 0) {
                return methodKey.substring(secondLastDot + 1);
            }
        }
        return methodKey;
    }

    // ==================== Exact Timing Panel ====================

    private JPanel createExactTimingPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(PANEL_BG);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Info header with toggle checkbox
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(new Color(50, 52, 56));
        infoPanel.setBorder(new EmptyBorder(8, 10, 8, 10));

        JLabel infoLabel = new JLabel("<html><b>Exact Timing</b> - Use MethodProfiler.begin()/end() in your code for precise measurements</html>");
        infoLabel.setForeground(TEXT_COLOR);
        infoPanel.add(infoLabel, BorderLayout.CENTER);

        // Right side controls panel
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controlsPanel.setBackground(new Color(50, 52, 56));

        // Filter field
        controlsPanel.add(createStyledLabel("Filter:"));
        exactTimingFilterField = new JTextField(15);
        styleTextField(exactTimingFilterField);
        exactTimingFilterField.setToolTipText("Filter by method name prefix");
        exactTimingFilterField.addActionListener(e -> refreshExactTimingTable());
        // Also refresh on each key release for live filtering
        exactTimingFilterField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                refreshExactTimingTable();
            }
        });
        controlsPanel.add(exactTimingFilterField);

        controlsPanel.add(Box.createHorizontalStrut(10));

        // Enable/disable toggle
        methodProfilerEnabledCheckbox = new JCheckBox("Enable Recording");
        methodProfilerEnabledCheckbox.setBackground(new Color(50, 52, 56));
        methodProfilerEnabledCheckbox.setForeground(TEXT_COLOR);
        methodProfilerEnabledCheckbox.setFont(new Font("Segoe UI", Font.BOLD, 11));
        methodProfilerEnabledCheckbox.setSelected(MethodProfiler.isEnabled());
        methodProfilerEnabledCheckbox.addActionListener(e -> {
            MethodProfiler.setEnabled(methodProfilerEnabledCheckbox.isSelected());
            if (methodProfilerEnabledCheckbox.isSelected()) {
                exactTimingSummaryLabel.setText("Recording enabled - MethodProfiler calls will now be tracked");
            } else {
                exactTimingSummaryLabel.setText("Recording disabled - MethodProfiler calls are no-op (zero overhead)");
            }
        });
        controlsPanel.add(methodProfilerEnabledCheckbox);

        infoPanel.add(controlsPanel, BorderLayout.EAST);

        panel.add(infoPanel, BorderLayout.NORTH);

        String[] columns = {"Method", "Calls", "Total Time", "Avg Time", "Min", "Max"};
        exactTimingModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        exactTimingTable = new JTable(exactTimingModel);
        styleTable(exactTimingTable);
        exactTimingTable.setDefaultRenderer(Object.class, new ExactTimingCellRenderer());

        exactTimingTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        exactTimingTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        exactTimingTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        exactTimingTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        exactTimingTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        exactTimingTable.getColumnModel().getColumn(5).setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(exactTimingTable);
        scrollPane.setBackground(PANEL_BG);
        scrollPane.getViewport().setBackground(PANEL_BG);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        exactTimingSummaryLabel = createStyledLabel("No data yet");
        exactTimingSummaryLabel.setBorder(new EmptyBorder(5, 5, 5, 0));
        bottomPanel.add(exactTimingSummaryLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);

        JButton refreshBtn = createStyledButton("Refresh", ACCENT_COLOR);
        refreshBtn.setPreferredSize(new Dimension(80, 24));
        refreshBtn.addActionListener(e -> refreshExactTimingTable());

        JButton exportExactBtn = createStyledButton("Export", ACCENT_COLOR);
        exportExactBtn.setPreferredSize(new Dimension(80, 24));
        exportExactBtn.addActionListener(e -> exportExactTimingReport());

        JButton clearExactBtn = createStyledButton("Clear", WARNING_COLOR);
        clearExactBtn.setPreferredSize(new Dimension(80, 24));
        clearExactBtn.addActionListener(e -> {
            MethodProfiler.clear();
            refreshExactTimingTable();
        });

        buttonPanel.add(refreshBtn);
        buttonPanel.add(exportExactBtn);
        buttonPanel.add(clearExactBtn);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== Actions Panel ====================

    private JPanel createActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(false);

        exportButton = createStyledButton("Export CSV", ACCENT_COLOR);
        exportButton.setEnabled(false);
        exportButton.addActionListener(e -> exportResults());

        clearButton = createStyledButton("Clear All", WARNING_COLOR);
        clearButton.addActionListener(e -> {
            clearResults();
            MethodProfiler.clear();
            refreshExactTimingTable();
        });

        panel.add(exportButton);
        panel.add(clearButton);

        return panel;
    }

    // ==================== Recording Logic ====================

    private void startRecording() {
        try {
            recorder.setSamplePeriodMs(getSelectedSampleRate());
            recorder.setFilterMode(getSelectedFilterMode());
            recorder.setPackageFilter(packageField.getText().trim());
            recorder.setClassFilter(classField.getText().trim());

            recorder.startRecording();
            recordingStartTime = System.currentTimeMillis();

            recordButton.setEnabled(false);
            stopButton.setEnabled(true);
            setControlsEnabled(false);

            statusLabel.setText("Recording...");
            statusLabel.setForeground(SUCCESS_COLOR);
            resultsSummaryLabel.setText("Recording in progress...");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Failed to start recording: " + e.getMessage(),
                "Recording Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopRecording() {
        recorder.stopRecording();

        statusLabel.setText("Analyzing...");
        statusLabel.setForeground(WARNING_COLOR);

        SwingWorker<MethodTimingResults, Void> worker = new SwingWorker<MethodTimingResults, Void>() {
            @Override
            protected MethodTimingResults doInBackground() throws Exception {
                return recorder.analyze();
            }

            @Override
            protected void done() {
                try {
                    lastResults = get();
                    updateAllViews();

                    recordButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    setControlsEnabled(true);
                    exportButton.setEnabled(lastResults != null && lastResults.hasData());

                    statusLabel.setText("Ready");
                    statusLabel.setForeground(TEXT_COLOR);
                    sampleCountLabel.setText("");

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(RecordingTab.this,
                        "Failed to analyze recording: " + e.getMessage(),
                        "Analysis Error", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Error");
                    statusLabel.setForeground(ERROR_COLOR);
                }
            }
        };
        worker.execute();
    }

    private void setControlsEnabled(boolean enabled) {
        sampleRateCombo.setEnabled(enabled);
        allMethodsRadio.setEnabled(enabled);
        packageRadio.setEnabled(enabled);
        classRadio.setEnabled(enabled);
        if (enabled) updateFilterFields();
        else {
            packageField.setEnabled(false);
            classField.setEnabled(false);
        }
    }

    private void updateAllViews() {
        updateTableView();
        updateFlameGraph();
        updateCallTree();
        selectedMethodKey = null;
        updateMethodDetails();
    }

    private void updateTableView() {
        tableModel.setRowCount(0);

        if (lastResults == null || !lastResults.hasData()) {
            resultsSummaryLabel.setText("No data captured. Try longer recording or check filters.");
            return;
        }

        List<MethodStats> methods = lastResults.getAllMethodsBySelfTime();
        int totalSamples = lastResults.totalSamples;
        int samplePeriod = lastResults.samplePeriodMs;

        for (MethodStats ms : methods) {
            int selfSamples = ms.getSelfSamples();
            String confidence = getConfidenceLevel(selfSamples);

            tableModel.addRow(new Object[]{
                ms.getDisplayName(),
                String.format("%.1f%%", ms.getSelfPercent(totalSamples)),
                formatTime(ms.getEstimatedSelfMs(samplePeriod)),
                String.format("%.1f%%", ms.getTotalPercent(totalSamples)),
                formatTime(ms.getEstimatedTotalMs(samplePeriod)),
                selfSamples,
                confidence
            });
        }

        resultsSummaryLabel.setText(String.format(
            "%d methods | %d samples | %s | %dms rate",
            lastResults.getMethodCount(), lastResults.totalSamples,
            lastResults.getFormattedDuration(), lastResults.samplePeriodMs));
    }

    private void updateFlameGraph() {
        if (lastResults != null && lastResults.flameGraphRoot != null) {
            flameGraphPanel.setRoot(lastResults.flameGraphRoot);
        } else {
            flameGraphPanel.setRoot(null);
        }
    }

    private void updateCallTree() {
        if (lastResults == null || lastResults.callTreeRoot == null) {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("(No data)");
            callTreeModel.setRoot(root);
            callTreeSummaryLabel.setText("No data");
            return;
        }

        CallTreeNode rootNode = lastResults.callTreeRoot;
        DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode(
            new CallTreeNodeWrapper(rootNode, lastResults.totalSamples));

        buildTreeNodes(treeRoot, rootNode, lastResults.totalSamples);

        callTreeModel.setRoot(treeRoot);
        callTree.expandRow(0);

        callTreeSummaryLabel.setText(String.format("%d unique call paths",
            countTreeNodes(rootNode) - 1));
    }

    private void buildTreeNodes(DefaultMutableTreeNode parent, CallTreeNode callNode, int totalSamples) {
        for (CallTreeNode child : callNode.getChildrenSorted()) {
            DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(
                new CallTreeNodeWrapper(child, totalSamples));
            parent.add(childTreeNode);
            buildTreeNodes(childTreeNode, child, totalSamples);
        }
    }

    private int countTreeNodes(CallTreeNode node) {
        int count = 1;
        for (CallTreeNode child : node.getChildren()) {
            count += countTreeNodes(child);
        }
        return count;
    }

    private void refreshExactTimingTable() {
        exactTimingModel.setRowCount(0);

        if (!MethodProfiler.hasData()) {
            exactTimingSummaryLabel.setText("No data. Use MethodProfiler.begin()/end() in code.");
            return;
        }

        List<MethodProfiler.MethodTiming> timings = MethodProfiler.getAllTimingsByAverage();

        // Apply filter if present
        String filterText = exactTimingFilterField != null ? exactTimingFilterField.getText().trim().toLowerCase() : "";
        List<MethodProfiler.MethodTiming> filteredTimings = timings;
        if (!filterText.isEmpty()) {
            filteredTimings = timings.stream()
                .filter(t -> t.getLabel().toLowerCase().startsWith(filterText) ||
                             t.getDisplayName().toLowerCase().startsWith(filterText))
                .collect(java.util.stream.Collectors.toList());
        }

        for (MethodProfiler.MethodTiming timing : filteredTimings) {
            exactTimingModel.addRow(new Object[]{
                timing.getDisplayName(),
                timing.getCallCount(),
                timing.getFormattedTotalTime(),
                timing.getFormattedAverageTime(),
                timing.getFormattedMinTime(),
                timing.getFormattedMaxTime()
            });
        }

        long totalCalls = filteredTimings.stream().mapToLong(MethodProfiler.MethodTiming::getCallCount).sum();
        double totalMs = filteredTimings.stream().mapToDouble(MethodProfiler.MethodTiming::getTotalMs).sum();

        String filterInfo = filterText.isEmpty() ? "" : String.format(" (filtered from %d)", timings.size());
        exactTimingSummaryLabel.setText(String.format(
            "%d methods%s | %,d calls | %s total",
            filteredTimings.size(), filterInfo, totalCalls, formatTime((long) totalMs)));
    }

    private void clearResults() {
        tableModel.setRowCount(0);
        lastResults = null;
        exportButton.setEnabled(false);
        resultsSummaryLabel.setText("Results cleared");
        flameGraphPanel.setRoot(null);
        callTreeModel.setRoot(new DefaultMutableTreeNode("(No data)"));
        selectedMethodKey = null;
        updateMethodDetails();
    }

    private void exportResults() {
        if (lastResults == null || !lastResults.hasData()) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("method_timing_" +
            new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(chooser.getSelectedFile()))) {
                writer.println("Method,Self %,~Self Time (ms),Total %,~Total Time (ms),Samples,Confidence,Class,Method Name");

                List<MethodStats> methods = lastResults.getAllMethodsBySelfTime();
                int totalSamples = lastResults.totalSamples;
                int samplePeriod = lastResults.samplePeriodMs;

                for (MethodStats ms : methods) {
                    writer.printf("%s,%.2f,%d,%.2f,%d,%d,%s,%s,%s%n",
                        ms.methodKey,
                        ms.getSelfPercent(totalSamples),
                        ms.getEstimatedSelfMs(samplePeriod),
                        ms.getTotalPercent(totalSamples),
                        ms.getEstimatedTotalMs(samplePeriod),
                        ms.getSelfSamples(),
                        getConfidenceLevel(ms.getSelfSamples()),
                        ms.className,
                        ms.methodName);
                }

                JOptionPane.showMessageDialog(this,
                    "Exported to: " + chooser.getSelectedFile().getName(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Export failed: " + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportExactTimingReport() {
        if (!MethodProfiler.hasData()) {
            JOptionPane.showMessageDialog(this,
                "No MethodProfiler data to export.\nEnable recording and run some profiled code first.",
                "No Data", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Ask user for format
        String[] options = {"Text Report", "CSV", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
            "Choose export format:",
            "Export Exact Timing",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);

        if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) return;

        boolean csv = (choice == 1);
        String extension = csv ? ".csv" : ".txt";
        String defaultName = "method_profiler_" +
            new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + extension;

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(chooser.getSelectedFile()))) {
                // Get filter and apply to export
                String filterText = exactTimingFilterField != null ? exactTimingFilterField.getText().trim().toLowerCase() : "";
                List<MethodProfiler.MethodTiming> timings = MethodProfiler.getAllTimings();

                if (!filterText.isEmpty()) {
                    timings = timings.stream()
                        .filter(t -> t.getLabel().toLowerCase().startsWith(filterText) ||
                                     t.getDisplayName().toLowerCase().startsWith(filterText))
                        .collect(java.util.stream.Collectors.toList());
                }

                if (csv) {
                    // Generate filtered CSV
                    writer.println("Method,Calls,TotalMs,AverageMs,MinMs,MaxMs");
                    for (MethodProfiler.MethodTiming timing : timings) {
                        writer.printf("\"%s\",%d,%.6f,%.6f,%.6f,%.6f%n",
                            timing.getLabel().replace("\"", "\"\""),
                            timing.getCallCount(),
                            timing.getTotalMs(),
                            timing.getAverageMs(),
                            timing.getMinMs(),
                            timing.getMaxMs());
                    }
                } else {
                    // Generate filtered text report
                    writer.println("=== MethodProfiler Report ===");
                    writer.printf("Generated: %s%n", java.time.LocalDateTime.now());
                    if (!filterText.isEmpty()) {
                        writer.printf("Filter: \"%s\"%n", filterText);
                    }
                    writer.printf("Methods tracked: %d%n%n", timings.size());

                    if (timings.isEmpty()) {
                        writer.println("No data recorded (or all filtered out).");
                    } else {
                        long totalCalls = timings.stream().mapToLong(MethodProfiler.MethodTiming::getCallCount).sum();
                        double totalMs = timings.stream().mapToDouble(MethodProfiler.MethodTiming::getTotalMs).sum();

                        writer.printf("Total calls: %,d%n", totalCalls);
                        writer.printf("Total time: %.2fms%n%n", totalMs);

                        writer.printf("%-50s %12s %12s %12s %12s %12s%n",
                            "Method", "Calls", "Total", "Average", "Min", "Max");
                        writer.println("-".repeat(110));

                        for (MethodProfiler.MethodTiming timing : timings) {
                            String label = timing.getLabel();
                            if (label.length() > 50) {
                                label = label.substring(0, 47) + "...";
                            }
                            writer.printf("%-50s %,12d %12s %12s %12s %12s%n",
                                label,
                                timing.getCallCount(),
                                timing.getFormattedTotalTime(),
                                timing.getFormattedAverageTime(),
                                timing.getFormattedMinTime(),
                                timing.getFormattedMaxTime());
                        }
                    }
                }

                String filterNote = filterText.isEmpty() ? "" : " (filtered)";
                JOptionPane.showMessageDialog(this,
                    "Exported to: " + chooser.getSelectedFile().getName() + filterNote,
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Export failed: " + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ==================== Helpers ====================

    private int getSelectedSampleRate() {
        String selected = (String) sampleRateCombo.getSelectedItem();
        return selected != null ? Integer.parseInt(selected.replace("ms", "")) : 10;
    }

    private JFRMethodRecorder.FilterMode getSelectedFilterMode() {
        if (packageRadio.isSelected()) return JFRMethodRecorder.FilterMode.PACKAGE;
        if (classRadio.isSelected()) return JFRMethodRecorder.FilterMode.CLASS;
        return JFRMethodRecorder.FilterMode.ALL;
    }

    private void startStatusTimer() {
        statusTimer = new Timer(500, e -> {
            if (recorder.isRecording()) {
                long elapsed = System.currentTimeMillis() - recordingStartTime;
                long samples = elapsed / recorder.getSamplePeriodMs();
                sampleCountLabel.setText(String.format("~%d samples, %.1fs", samples, elapsed / 1000.0));
            }

            // Auto-refresh exact timing
            if (MethodProfiler.hasData() && viewTabs.getSelectedIndex() == 4) {
                if (System.currentTimeMillis() % 2000 < 500) {
                    refreshExactTimingTable();
                }
            }
        });
        statusTimer.start();
    }

    public void cleanup() {
        if (statusTimer != null) statusTimer.stop();
        if (recorder.isRecording()) recorder.stopRecording();
    }

    private String formatTime(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return String.format("%.2fs", ms / 1000.0);
        return String.format("%dm %ds", ms / 60000, (ms % 60000) / 1000);
    }

    private String getConfidenceLevel(int samples) {
        if (samples >= 100) return "High";
        if (samples >= 30) return "Medium";
        if (samples >= 10) return "Low";
        return "Very Low";
    }

    // ==================== UI Styling Helpers ====================

    private JPanel createStyledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 63, 65)),
                title, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 11), TEXT_COLOR),
            new EmptyBorder(3, 5, 3, 5)));
        return panel;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(90, 26));
        return button;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_COLOR);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return label;
    }

    private void styleComboBox(JComboBox<?> combo) {
        combo.setBackground(PANEL_BG);
        combo.setForeground(TEXT_COLOR);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
    }

    private void styleRadioButton(JRadioButton radio) {
        radio.setBackground(PANEL_BG);
        radio.setForeground(TEXT_COLOR);
        radio.setFont(new Font("Segoe UI", Font.PLAIN, 11));
    }

    private void styleTextField(JTextField field) {
        field.setBackground(new Color(50, 52, 56));
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setFont(new Font("Consolas", Font.PLAIN, 11));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 63, 65)),
            new EmptyBorder(2, 4, 2, 4)));
    }

    private void styleTable(JTable table) {
        table.setBackground(PANEL_BG);
        table.setForeground(TEXT_COLOR);
        table.setGridColor(new Color(60, 63, 65));
        table.setSelectionBackground(ACCENT_COLOR);
        table.setSelectionForeground(Color.WHITE);
        table.setRowHeight(22);
        table.setFont(new Font("Consolas", Font.PLAIN, 11));
        table.getTableHeader().setBackground(new Color(50, 52, 56));
        table.getTableHeader().setForeground(TEXT_COLOR);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
    }

    // ==================== Cell Renderers ====================

    private class HeatmapCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected && lastResults != null && row < tableModel.getRowCount()) {
                Object selfPercentObj = tableModel.getValueAt(row, 1);
                if (selfPercentObj != null) {
                    try {
                        double selfPercent = Double.parseDouble(selfPercentObj.toString().replace("%", ""));
                        if (selfPercent >= 20) c.setBackground(HOT_RED);
                        else if (selfPercent >= 10) c.setBackground(HOT_ORANGE);
                        else if (selfPercent >= 5) c.setBackground(HOT_YELLOW);
                        else c.setBackground(PANEL_BG);
                    } catch (NumberFormatException e) {
                        c.setBackground(PANEL_BG);
                    }
                }

                if (column == 6 && value != null) {
                    switch (value.toString()) {
                        case "High": setForeground(CONFIDENCE_HIGH); break;
                        case "Medium": setForeground(CONFIDENCE_MEDIUM); break;
                        case "Low": setForeground(CONFIDENCE_LOW); break;
                        case "Very Low": setForeground(CONFIDENCE_VERY_LOW); break;
                        default: setForeground(TEXT_COLOR);
                    }
                } else {
                    setForeground(TEXT_COLOR);
                }
            } else {
                setForeground(TEXT_COLOR);
            }

            setHorizontalAlignment(column == 6 ? SwingConstants.CENTER : (column >= 1 ? SwingConstants.RIGHT : SwingConstants.LEFT));
            return c;
        }
    }

    private class ExactTimingCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                c.setBackground(PANEL_BG);
                c.setForeground(TEXT_COLOR);

                List<MethodProfiler.MethodTiming> timings = MethodProfiler.getAllTimingsByAverage();
                if (row < timings.size()) {
                    double avgMs = timings.get(row).getAverageMs();
                    if (avgMs >= 100) c.setBackground(HOT_RED);
                    else if (avgMs >= 10) c.setBackground(HOT_ORANGE);
                    else if (avgMs >= 1) c.setBackground(HOT_YELLOW);
                }
            }

            setHorizontalAlignment(column >= 1 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return c;
        }
    }

    private class CallTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            setBackground(sel ? ACCENT_COLOR : PANEL_BG);
            setForeground(TEXT_COLOR);
            setFont(new Font("Consolas", Font.PLAIN, 11));
            setOpaque(true);

            if (value instanceof DefaultMutableTreeNode) {
                Object userObj = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObj instanceof CallTreeNodeWrapper) {
                    CallTreeNodeWrapper wrapper = (CallTreeNodeWrapper) userObj;
                    setText(wrapper.toString());

                    double selfPercent = wrapper.node.getSelfPercent(wrapper.totalSamples);
                    if (!sel) {
                        if (selfPercent >= 20) setBackground(HOT_RED);
                        else if (selfPercent >= 10) setBackground(HOT_ORANGE);
                        else if (selfPercent >= 5) setBackground(HOT_YELLOW);
                    }
                }
            }

            return this;
        }
    }

    /**
     * Wrapper for CallTreeNode to provide custom toString for JTree display
     */
    private static class CallTreeNodeWrapper {
        final CallTreeNode node;
        final int totalSamples;

        CallTreeNodeWrapper(CallTreeNode node, int totalSamples) {
            this.node = node;
            this.totalSamples = totalSamples;
        }

        @Override
        public String toString() {
            if (node.getMethodKey().equals("(root)") || node.getMethodKey().equals("(all)")) {
                return String.format("(all) - %d samples", node.getTotalSamples());
            }
            return String.format("%s  [%.1f%% self, %d samples]",
                node.getDisplayName(),
                node.getSelfPercent(totalSamples),
                node.getTotalSamples());
        }
    }
}

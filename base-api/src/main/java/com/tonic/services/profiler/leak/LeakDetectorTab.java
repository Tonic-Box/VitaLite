package com.tonic.services.profiler.leak;

import com.tonic.services.profiler.leak.LeakDetector.MemoryStats;
import com.tonic.services.profiler.leak.LeakSuspicion.LeakConfidence;
import com.tonic.services.profiler.sampling.HeapHistogramSampler;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

/**
 * UI tab for leak detection and analysis
 */
public class LeakDetectorTab extends JPanel {
    private final LeakDetector leakDetector;
    private final LeakTableModel tableModel;
    private JTable leakTable;
    private JTextArea detailsArea;
    private final Timer refreshTimer;

    // Treemap view
    private com.tonic.services.profiler.visualization.TreemapPanel treemapPanel;
    private JPanel viewContainer;
    private CardLayout viewCardLayout;
    private JButton toggleViewButton;
    private boolean showingTreemap = false;

    // Statistics labels
    private JLabel statusLabel;
    private JLabel snapshotsLabel;
    private JLabel memoryDeltaLabel;
    private JLabel instanceDeltaLabel;
    private JLabel heapUsageLabel;
    private JProgressBar heapProgressBar;
    private JLabel classLoadersLabel;
    private JLabel suspiciousClassLoadersLabel;

    // Control buttons
    private JButton startButton;
    private JButton stopButton;
    private JButton baselineButton;
    private JButton gcButton;
    private JButton refreshButton;

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.0");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public LeakDetectorTab() {
        HeapHistogramSampler histogramSampler = new HeapHistogramSampler();
        this.leakDetector = new LeakDetector(histogramSampler);
        this.tableModel = new LeakTableModel();

        // Initialize components first
        initializeComponents();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel: Statistics and controls
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        // Statistics panel
        JPanel statsPanel = createStatisticsPanel();
        topPanel.add(statsPanel, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = createControlPanel();
        topPanel.add(controlPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center: Split pane with table and details
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.7);

        // Table panel
        JPanel tablePanel = createTablePanel();
        splitPane.setTopComponent(tablePanel);

        // Details panel
        JPanel detailsPanel = createDetailsPanel();
        splitPane.setBottomComponent(detailsPanel);

        add(splitPane, BorderLayout.CENTER);

        // Refresh timer
        refreshTimer = new Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshUI();
            }
        });
        refreshTimer.start();

        // Initial state
        updateButtonStates();
    }

    private void initializeComponents() {
        // Initialize labels
        statusLabel = new JLabel("Not monitoring");
        statusLabel.setForeground(Color.GRAY);
        snapshotsLabel = new JLabel("0");
        memoryDeltaLabel = new JLabel("+0 bytes");
        instanceDeltaLabel = new JLabel("+0");
        heapUsageLabel = new JLabel("0%");
        heapProgressBar = new JProgressBar(0, 100);
        heapProgressBar.setPreferredSize(new Dimension(200, 20));
        heapProgressBar.setStringPainted(true);
        classLoadersLabel = new JLabel("0");
        suspiciousClassLoadersLabel = new JLabel("0");
        suspiciousClassLoadersLabel.setForeground(Color.GRAY);

        // Initialize buttons
        startButton = new JButton("Start Monitoring");
        startButton.addActionListener(e -> startMonitoring());

        stopButton = new JButton("Stop");
        stopButton.addActionListener(e -> stopMonitoring());

        baselineButton = new JButton("Set Baseline");
        baselineButton.addActionListener(e -> setBaseline());

        gcButton = new JButton("Force GC & Capture");
        gcButton.addActionListener(e -> forceGCAndCapture());

        refreshButton = new JButton("Refresh Analysis");
        refreshButton.addActionListener(e -> refreshAnalysis());

        // Initialize details area
        detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detailsArea.setText("Select a suspicious class from the table above to view details.");

        // Initialize leak table
        leakTable = new JTable(tableModel);
        leakTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leakTable.setAutoCreateRowSorter(false);
        leakTable.setRowHeight(24);

        // Initialize treemap
        treemapPanel = new com.tonic.services.profiler.visualization.TreemapPanel();

        // Initialize toggle button
        toggleViewButton = new JButton("Show Treemap");
        toggleViewButton.addActionListener(e -> toggleView());
    }

    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Memory Statistics"));

        // Row 1: Status and snapshots
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        row1.add(new JLabel("Status:"));
        row1.add(statusLabel);
        row1.add(Box.createHorizontalStrut(20));
        row1.add(new JLabel("Snapshots:"));
        row1.add(snapshotsLabel);
        panel.add(row1);

        // Row 2: Memory and instance deltas
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        row2.add(new JLabel("Memory Delta:"));
        row2.add(memoryDeltaLabel);
        row2.add(Box.createHorizontalStrut(20));
        row2.add(new JLabel("Instance Delta:"));
        row2.add(instanceDeltaLabel);
        panel.add(row2);

        // Row 3: Heap usage with progress bar
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        row3.add(new JLabel("Heap Usage:"));
        row3.add(heapUsageLabel);
        row3.add(heapProgressBar);
        panel.add(row3);

        // Row 4: ClassLoader statistics
        JPanel row4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        row4.add(new JLabel("ClassLoaders:"));
        row4.add(classLoadersLabel);
        row4.add(Box.createHorizontalStrut(20));
        row4.add(new JLabel("Suspicious:"));
        row4.add(suspiciousClassLoadersLabel);
        panel.add(row4);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));

        // Row 1: Monitoring controls
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        row1.add(startButton);
        row1.add(stopButton);
        row1.add(baselineButton);
        panel.add(row1);

        // Row 2: Analysis controls
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        row2.add(gcButton);
        row2.add(refreshButton);
        panel.add(row2);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Memory Analysis View"));

        // Top: Toggle button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(toggleViewButton);
        panel.add(topPanel, BorderLayout.NORTH);

        // Center: Card layout with table and treemap views
        viewCardLayout = new CardLayout();
        viewContainer = new JPanel(viewCardLayout);

        // Table view
        JPanel tableView = createTableView();
        viewContainer.add(tableView, "table");

        // Treemap view
        JPanel treemapView = createTreemapView();
        viewContainer.add(treemapView, "treemap");

        panel.add(viewContainer, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTableView() {
        JPanel panel = new JPanel(new BorderLayout());

        // Column widths
        TableColumn col0 = leakTable.getColumnModel().getColumn(0); // Confidence
        col0.setPreferredWidth(80);
        col0.setMaxWidth(100);

        TableColumn col1 = leakTable.getColumnModel().getColumn(1); // Type
        col1.setPreferredWidth(100);
        col1.setMaxWidth(120);

        TableColumn col2 = leakTable.getColumnModel().getColumn(2); // Class
        col2.setPreferredWidth(300);

        TableColumn col3 = leakTable.getColumnModel().getColumn(3); // Instances
        col3.setPreferredWidth(100);

        TableColumn col4 = leakTable.getColumnModel().getColumn(4); // Delta
        col4.setPreferredWidth(100);

        TableColumn col5 = leakTable.getColumnModel().getColumn(5); // Growth Rate
        col5.setPreferredWidth(120);

        TableColumn col6 = leakTable.getColumnModel().getColumn(6); // Memory
        col6.setPreferredWidth(100);

        // Custom renderer for confidence column
        col0.setCellRenderer(new ConfidenceCellRenderer());

        // Custom renderer for numeric columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        col3.setCellRenderer(rightRenderer);
        col4.setCellRenderer(rightRenderer);
        col5.setCellRenderer(rightRenderer);
        col6.setCellRenderer(rightRenderer);

        // Selection listener
        leakTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDetailsArea();
            }
        });

        JScrollPane scrollPane = new JScrollPane(leakTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Info label at bottom
        JLabel infoLabel = new JLabel("Click a row to see detailed analysis");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC));
        panel.add(infoLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createTreemapView() {
        JPanel panel = new JPanel(new BorderLayout());

        panel.add(treemapPanel, BorderLayout.CENTER);

        // Info label at bottom
        JLabel infoLabel = new JLabel("Click to drill down | Click breadcrumb to zoom out");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC));
        panel.add(infoLabel, BorderLayout.SOUTH);

        return panel;
    }

    private void toggleView() {
        showingTreemap = !showingTreemap;
        if (showingTreemap) {
            updateTreemapData();
            viewCardLayout.show(viewContainer, "treemap");
            toggleViewButton.setText("Show Table");
        } else {
            viewCardLayout.show(viewContainer, "table");
            toggleViewButton.setText("Show Treemap");
        }
    }

    private void updateTreemapData() {
        com.tonic.services.profiler.sampling.HeapHistogramSample[] histogram =
            new com.tonic.services.profiler.sampling.HeapHistogramSampler().captureHistogram()
                .toArray(new com.tonic.services.profiler.sampling.HeapHistogramSample[0]);

        if (histogram.length == 0) {
            return;
        }

        // Build package hierarchy
        Map<String, PackageNode> packages = new HashMap<>();

        for (com.tonic.services.profiler.sampling.HeapHistogramSample sample : histogram) {
            String className = sample.className;
            long bytes = sample.totalBytes;

            // Extract package name
            String packageName = "";
            int lastDot = className.lastIndexOf('.');
            if (lastDot > 0) {
                packageName = className.substring(0, lastDot);
            } else {
                packageName = "(default)";
            }

            // Get or create package node
            PackageNode pkgNode = packages.get(packageName);
            if (pkgNode == null) {
                pkgNode = new PackageNode(packageName);
                packages.put(packageName, pkgNode);
            }

            pkgNode.addBytes(bytes);
            pkgNode.addClass(className, bytes);
        }

        // Create root node
        com.tonic.services.profiler.visualization.TreemapNode root =
            new com.tonic.services.profiler.visualization.TreemapNode(
                "Heap Memory",
                getTotalBytes(packages),
                new Color(64, 156, 255)
            );

        // Sort packages by size and take top 50
        List<PackageNode> sortedPackages = new ArrayList<>(packages.values());
        sortedPackages.sort((a, b) -> Long.compare(b.totalBytes, a.totalBytes));

        int count = 0;
        for (PackageNode pkgNode : sortedPackages) {
            if (count++ >= 50) break;

            Color pkgColor = getColorForSize(pkgNode.totalBytes, getTotalBytes(packages));
            com.tonic.services.profiler.visualization.TreemapNode packageNode =
                new com.tonic.services.profiler.visualization.TreemapNode(
                    pkgNode.name,
                    pkgNode.totalBytes,
                    pkgColor
                );

            // Add top classes in this package
            List<Map.Entry<String, Long>> sortedClasses = new ArrayList<>(pkgNode.classes.entrySet());
            sortedClasses.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

            int classCount = 0;
            for (Map.Entry<String, Long> entry : sortedClasses) {
                if (classCount++ >= 20) break;

                String simpleClassName = entry.getKey();
                int lastDot = simpleClassName.lastIndexOf('.');
                if (lastDot >= 0) {
                    simpleClassName = simpleClassName.substring(lastDot + 1);
                }

                Color classColor = brighten(pkgColor, 0.3f);
                com.tonic.services.profiler.visualization.TreemapNode classNode =
                    new com.tonic.services.profiler.visualization.TreemapNode(
                        simpleClassName,
                        entry.getValue(),
                        classColor
                    );

                packageNode.addChild(classNode);
            }

            root.addChild(packageNode);
        }

        treemapPanel.setRoot(root);
    }

    private long getTotalBytes(Map<String, PackageNode> packages) {
        long total = 0;
        for (PackageNode node : packages.values()) {
            total += node.totalBytes;
        }
        return total;
    }

    private Color getColorForSize(long size, long maxSize) {
        float ratio = (float) size / maxSize;

        if (ratio > 0.5f) {
            return new Color(244, 67, 54); // Red - large
        } else if (ratio > 0.2f) {
            return new Color(255, 152, 0); // Orange - medium
        } else if (ratio > 0.05f) {
            return new Color(255, 193, 7); // Yellow - small
        } else {
            return new Color(76, 175, 80); // Green - tiny
        }
    }

    private Color brighten(Color color, float factor) {
        int r = Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor));
        int g = Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor));
        int b = Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor));
        return new Color(r, g, b);
    }

    private static class PackageNode {
        String name;
        long totalBytes;
        Map<String, Long> classes;

        PackageNode(String name) {
            this.name = name;
            this.totalBytes = 0;
            this.classes = new HashMap<>();
        }

        void addBytes(long bytes) {
            this.totalBytes += bytes;
        }

        void addClass(String className, long bytes) {
            classes.put(className, bytes);
        }
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Leak Details"));

        JScrollPane scrollPane = new JScrollPane(detailsArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void startMonitoring() {
        leakDetector.startMonitoring();
        updateButtonStates();
        refreshUI();
    }

    private void stopMonitoring() {
        leakDetector.stopMonitoring();
        updateButtonStates();
    }

    private void setBaseline() {
        leakDetector.captureBaseline();
        refreshAnalysis();
        JOptionPane.showMessageDialog(this,
            "Baseline captured!\n\nMonitoring will now track growth relative to this point.",
            "Baseline Set",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void forceGCAndCapture() {
        leakDetector.captureSnapshotAfterGC();
        refreshAnalysis();
    }

    private void refreshAnalysis() {
        List<LeakSuspicion> suspicions = leakDetector.analyzeSuspiciousClasses();
        tableModel.setSuspicions(suspicions);
        refreshUI();
    }

    private void refreshUI() {
        // Update statistics
        MemoryStats stats = leakDetector.getMemoryStats();

        if (leakDetector.isMonitoring()) {
            statusLabel.setText("Monitoring");
            statusLabel.setForeground(new Color(0x4CAF50)); // Green
        } else {
            statusLabel.setText("Stopped");
            statusLabel.setForeground(Color.GRAY);
        }

        snapshotsLabel.setText(String.valueOf(stats.snapshotCount));

        long memDelta = stats.getBytesDelta();
        memoryDeltaLabel.setText(formatBytes(memDelta));
        memoryDeltaLabel.setForeground(memDelta > 0 ? new Color(0xFF5722) : new Color(0x4CAF50));

        long instDelta = stats.getInstancesDelta();
        instanceDeltaLabel.setText((instDelta >= 0 ? "+" : "") + NUMBER_FORMAT.format(instDelta));
        instanceDeltaLabel.setForeground(instDelta > 0 ? new Color(0xFF5722) : new Color(0x4CAF50));

        double heapPercent = stats.getHeapUsagePercent();
        heapUsageLabel.setText(PERCENT_FORMAT.format(heapPercent) + "%");
        heapProgressBar.setValue((int) heapPercent);

        // Color code heap usage
        if (heapPercent > 90) {
            heapProgressBar.setForeground(new Color(0xD32F2F)); // Red
        } else if (heapPercent > 75) {
            heapProgressBar.setForeground(new Color(0xFF9800)); // Orange
        } else {
            heapProgressBar.setForeground(new Color(0x4CAF50)); // Green
        }

        // Update ClassLoader statistics
        ClassLoaderAnalyzer.ClassLoaderAnalysis clAnalysis = leakDetector.getClassLoaderAnalysis();
        classLoadersLabel.setText(String.valueOf(clAnalysis.activeClassLoaderCount));

        int suspiciousCount = clAnalysis.getSuspiciousCount();
        suspiciousClassLoadersLabel.setText(String.valueOf(suspiciousCount));
        if (suspiciousCount > 0) {
            suspiciousClassLoadersLabel.setForeground(new Color(0xFF5722)); // Orange/Red
        } else {
            suspiciousClassLoadersLabel.setForeground(new Color(0x4CAF50)); // Green
        }
    }

    private void updateButtonStates() {
        boolean monitoring = leakDetector.isMonitoring();
        startButton.setEnabled(!monitoring);
        stopButton.setEnabled(monitoring);
    }

    private void updateDetailsArea() {
        int selectedRow = leakTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= tableModel.getSuspicions().size()) {
            detailsArea.setText("Select a suspicious class from the table above to view details.");
            return;
        }

        LeakSuspicion suspicion = tableModel.getSuspicions().get(selectedRow);

        StringBuilder details = new StringBuilder();
        details.append("=".repeat(80)).append("\n");
        details.append("LEAK ANALYSIS: ").append(suspicion.getClassName()).append("\n");
        details.append("=".repeat(80)).append("\n\n");

        // Confidence and Type
        details.append("Confidence:      ").append(suspicion.getConfidence().getLabel())
               .append(" (").append(suspicion.getConfidence().name()).append(")\n");
        details.append("Leak Type:       ").append(suspicion.getType().getLabel()).append("\n");
        details.append("Description:     ").append(suspicion.getType().getDescription()).append("\n\n");

        // Instance metrics
        details.append("-".repeat(80)).append("\n");
        details.append("INSTANCE METRICS\n");
        details.append("-".repeat(80)).append("\n");
        details.append(String.format("Baseline Count:  %,d instances\n", suspicion.getBaselineInstances()));
        details.append(String.format("Current Count:   %,d instances\n", suspicion.getCurrentInstances()));
        details.append(String.format("Delta:           %+,d instances (%+.1f%%)\n",
            suspicion.getInstanceDelta(),
            suspicion.getRelativeGrowth()));
        details.append(String.format("Growth Rate:     %.1f instances/minute\n\n", suspicion.getGrowthRate()));

        // Memory metrics
        details.append("-".repeat(80)).append("\n");
        details.append("MEMORY METRICS\n");
        details.append("-".repeat(80)).append("\n");
        details.append(String.format("Baseline Memory: %s\n", formatBytes(suspicion.getBaselineBytes())));
        details.append(String.format("Current Memory:  %s\n", formatBytes(suspicion.getCurrentBytes())));
        details.append(String.format("Delta:           %s\n\n", formatBytes(suspicion.getBytesDelta())));

        // Tracking info
        details.append("-".repeat(80)).append("\n");
        details.append("TRACKING INFO\n");
        details.append("-".repeat(80)).append("\n");
        details.append(String.format("Samples:         %d snapshots\n", suspicion.getSamplesCollected()));
        details.append(String.format("First Seen:      %s\n", TIME_FORMAT.format(new Date(suspicion.getFirstSeenTimestamp()))));
        details.append(String.format("Last Seen:       %s\n", TIME_FORMAT.format(new Date(suspicion.getLastSeenTimestamp()))));
        long durationMin = (suspicion.getLastSeenTimestamp() - suspicion.getFirstSeenTimestamp()) / 60000;
        details.append(String.format("Duration:        %d minutes\n\n", durationMin));

        // Recommendations
        details.append("-".repeat(80)).append("\n");
        details.append("RECOMMENDATIONS\n");
        details.append("-".repeat(80)).append("\n");
        details.append(getRecommendations(suspicion));

        detailsArea.setText(details.toString());
        detailsArea.setCaretPosition(0);
    }

    private String getRecommendations(LeakSuspicion suspicion) {
        StringBuilder rec = new StringBuilder();

        switch (suspicion.getType()) {
            case CLASSLOADER:
                rec.append("• Check if plugins are being properly unloaded\n");
                rec.append("• Verify no static references to plugin classes\n");
                rec.append("• Review plugin reload/hot-swap mechanisms\n");
                break;
            case THREAD:
                rec.append("• Ensure threads are properly shut down\n");
                rec.append("• Check for ExecutorService leaks (not calling shutdown)\n");
                rec.append("• Review ThreadLocal usage and cleanup\n");
                break;
            case COLLECTION:
                rec.append("• Add eviction policy to collections (size limit, LRU, etc.)\n");
                rec.append("• Check for listeners/callbacks not being removed\n");
                rec.append("• Review cache invalidation logic\n");
                break;
            case CACHE:
                rec.append("• Implement cache eviction strategy (LRU, TTL, size-based)\n");
                rec.append("• Add monitoring for cache hit/miss rates\n");
                rec.append("• Consider using WeakReference/SoftReference for values\n");
                break;
            case LISTENER:
                rec.append("• Ensure event listeners are removed when no longer needed\n");
                rec.append("• Use weak listeners where appropriate\n");
                rec.append("• Review component lifecycle and cleanup\n");
                break;
            case ARRAY:
                rec.append("• Review array allocation patterns\n");
                rec.append("• Check for arrays being held in collections\n");
                rec.append("• Consider using object pooling for large arrays\n");
                break;
            default:
                rec.append("• Review object lifecycle and cleanup\n");
                rec.append("• Check for references preventing garbage collection\n");
                rec.append("• Use heap dump analysis for detailed investigation\n");
        }

        return rec.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) {
            return "-" + formatBytes(-bytes);
        }
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        leakDetector.stopMonitoring();
    }

    /**
     * Table model for leak suspicions
     */
    private static class LeakTableModel extends AbstractTableModel {
        private final String[] columnNames = {
            "Confidence", "Type", "Class Name", "Instances", "Delta", "Growth/min", "Memory"
        };
        private List<LeakSuspicion> suspicions = new ArrayList<>();

        public void setSuspicions(List<LeakSuspicion> suspicions) {
            this.suspicions = new ArrayList<>(suspicions);
            fireTableDataChanged();
        }

        public List<LeakSuspicion> getSuspicions() {
            return suspicions;
        }

        @Override
        public int getRowCount() {
            return suspicions.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            LeakSuspicion suspicion = suspicions.get(row);
            switch (column) {
                case 0: return suspicion.getConfidence();
                case 1: return suspicion.getType().getLabel();
                case 2: return suspicion.getClassName();
                case 3: return NUMBER_FORMAT.format(suspicion.getCurrentInstances());
                case 4: return (suspicion.getInstanceDelta() >= 0 ? "+" : "") +
                              NUMBER_FORMAT.format(suspicion.getInstanceDelta());
                case 5: return String.format("%.1f", suspicion.getGrowthRate());
                case 6: return formatBytesStatic(suspicion.getCurrentBytes());
                default: return "";
            }
        }

        private static String formatBytesStatic(long bytes) {
            if (bytes < 1024) {
                return bytes + " B";
            } else if (bytes < 1024 * 1024) {
                return String.format("%.1f KB", bytes / 1024.0);
            } else if (bytes < 1024 * 1024 * 1024) {
                return String.format("%.1f MB", bytes / (1024.0 * 1024));
            } else {
                return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
            }
        }
    }

    /**
     * Custom cell renderer for confidence column with color coding
     */
    private static class ConfidenceCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                      boolean isSelected, boolean hasFocus,
                                                      int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof LeakConfidence) {
                LeakConfidence confidence = (LeakConfidence) value;
                setText(confidence.getLabel());

                if (!isSelected) {
                    Color color = new Color(confidence.getColor());
                    setForeground(color);
                    setFont(getFont().deriveFont(Font.BOLD));
                }
            }

            return c;
        }
    }
}

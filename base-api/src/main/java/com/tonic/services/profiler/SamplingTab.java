package com.tonic.services.profiler;

import com.tonic.services.profiler.sampling.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Sampling profiler tab with CPU and Memory sampling capabilities
 */
public class SamplingTab extends JPanel {
    private static final Color BG_COLOR = new Color(30, 31, 34);
    private static final Color PANEL_BG = new Color(40, 42, 46);
    private static final Color TEXT_COLOR = new Color(200, 200, 200);
    private static final Color ACCENT_COLOR = new Color(64, 156, 255);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);
    private static final Color ERROR_COLOR = new Color(244, 67, 54);

    // Samplers
    private CPUSampler cpuSampler;
    private MemorySampler memorySampler;
    private SampleAnalyzer analyzer;
    private SampleExporter exporter;

    // UI Components - Controls
    private JButton cpuStartBtn, cpuStopBtn, memStartBtn, memStopBtn;
    private JButton cpuExportBtn, memExportBtn, clearBtn;
    private JLabel statusLabel, cpuStatusLabel, memStatusLabel;
    private JComboBox<String> cpuIntervalCombo, memIntervalCombo;
    private JCheckBox filterSystemCheckbox;

    // UI Components - Results
    private JTabbedPane resultsTabs;
    private DefaultTableModel cpuHotspotsModel, cpuThreadsModel, cpuPackagesModel;
    private JTable cpuHotspotsTable, cpuThreadsTable, cpuPackagesTable;
    private JTextArea memSummaryArea;
    private DefaultTableModel heapHistogramModel;
    private JTable heapHistogramTable;
    private JButton captureHistogramBtn;

    // Flame graph
    private com.tonic.services.profiler.visualization.FlameGraphPanel flameGraphPanel;

    // Analysis results
    private SampleAnalyzer.CPUAnalysisResults cpuResults;
    private SampleAnalyzer.MemoryAnalysisResults memResults;

    // Status update timer
    private Timer statusTimer;

    public SamplingTab() {
        setLayout(new BorderLayout(10, 10));
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // Initialize components
        initializeSamplers();
        initializeUI();

        // Start status update timer
        statusTimer = new Timer(1000, e -> updateStatus());
        statusTimer.start();
    }

    private void initializeSamplers() {
        cpuSampler = new CPUSampler(100_000); // 100K samples max
        memorySampler = new MemorySampler(10_000); // 10K samples max
        analyzer = new SampleAnalyzer();
        exporter = new SampleExporter();

        // Initialize flame graph
        flameGraphPanel = new com.tonic.services.profiler.visualization.FlameGraphPanel();
    }

    private void initializeUI() {
        // Top - Control Panel
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);

        // Center - Results
        resultsTabs = new JTabbedPane();
        resultsTabs.setBackground(PANEL_BG);
        resultsTabs.setForeground(TEXT_COLOR);
        styleTabPane(resultsTabs);

        resultsTabs.addTab("CPU Results", createCPUResultsPanel());
        resultsTabs.addTab("Memory Results", createMemoryResultsPanel());

        add(resultsTabs, BorderLayout.CENTER);

        // Bottom - Status Bar
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setOpaque(false);

        // CPU Sampling Controls
        panel.add(createCPUControlPanel());

        // Memory Sampling Controls
        panel.add(createMemoryControlPanel());

        return panel;
    }

    private JPanel createCPUControlPanel() {
        JPanel panel = createStyledPanel("CPU Sampling");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        cpuStartBtn = createStyledButton("Start CPU", SUCCESS_COLOR);
        cpuStopBtn = createStyledButton("Stop", ERROR_COLOR);
        cpuExportBtn = createStyledButton("Export CSV", ACCENT_COLOR);

        cpuStartBtn.addActionListener(e -> startCPUSampling());
        cpuStopBtn.addActionListener(e -> stopCPUSampling());
        cpuExportBtn.addActionListener(e -> exportCPUResults());

        cpuStopBtn.setEnabled(false);
        cpuExportBtn.setEnabled(false);

        panel.add(cpuStartBtn);
        panel.add(cpuStopBtn);
        panel.add(cpuExportBtn);

        panel.add(createStyledLabel("Interval:"));
        cpuIntervalCombo = new JComboBox<>(new String[]{"10ms", "25ms", "50ms", "100ms"});
        cpuIntervalCombo.setSelectedIndex(2); // Default 50ms
        styleComboBox(cpuIntervalCombo);
        panel.add(cpuIntervalCombo);

        filterSystemCheckbox = new JCheckBox("Filter System Classes");
        filterSystemCheckbox.setForeground(TEXT_COLOR);
        filterSystemCheckbox.setBackground(PANEL_BG);
        panel.add(filterSystemCheckbox);

        cpuStatusLabel = createStyledLabel("Idle");
        cpuStatusLabel.setForeground(TEXT_COLOR);
        panel.add(cpuStatusLabel);

        return panel;
    }

    private JPanel createMemoryControlPanel() {
        JPanel panel = createStyledPanel("Memory Sampling");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        memStartBtn = createStyledButton("Start Memory", SUCCESS_COLOR);
        memStopBtn = createStyledButton("Stop", ERROR_COLOR);
        memExportBtn = createStyledButton("Export CSV", ACCENT_COLOR);

        memStartBtn.addActionListener(e -> startMemorySampling());
        memStopBtn.addActionListener(e -> stopMemorySampling());
        memExportBtn.addActionListener(e -> exportMemoryResults());

        memStopBtn.setEnabled(false);
        memExportBtn.setEnabled(false);

        panel.add(memStartBtn);
        panel.add(memStopBtn);
        panel.add(memExportBtn);

        panel.add(createStyledLabel("Interval:"));
        memIntervalCombo = new JComboBox<>(new String[]{"500ms", "1s", "2s", "5s"});
        memIntervalCombo.setSelectedIndex(1); // Default 1s
        styleComboBox(memIntervalCombo);
        panel.add(memIntervalCombo);

        memStatusLabel = createStyledLabel("Idle");
        memStatusLabel.setForeground(TEXT_COLOR);
        panel.add(memStatusLabel);

        clearBtn = createStyledButton("Clear All", new Color(100, 100, 100));
        clearBtn.addActionListener(e -> clearAllData());
        panel.add(clearBtn);

        return panel;
    }

    private JPanel createCPUResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_COLOR);

        JTabbedPane tabs = new JTabbedPane();
        styleTabPane(tabs);

        // Method Hotspots Tab
        tabs.addTab("Method Hotspots", createMethodHotspotsPanel());

        // Flame Graph Tab
        tabs.addTab("Flame Graph", createFlameGraphPanel());

        // Thread Breakdown Tab
        tabs.addTab("Thread Breakdown", createThreadBreakdownPanel());

        // Package Aggregation Tab
        tabs.addTab("Package Aggregation", createPackageAggregationPanel());

        panel.add(tabs, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFlameGraphPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        panel.add(flameGraphPanel, BorderLayout.CENTER);

        // Info label at bottom
        JLabel infoLabel = new JLabel("Click on a frame to zoom in | Click header to reset zoom");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoLabel.setForeground(TEXT_COLOR);
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC));
        panel.add(infoLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMethodHotspotsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        String[] columns = {"Method", "Class", "Package", "Self %", "Total %", "Self Samples", "Total Samples"};
        cpuHotspotsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        cpuHotspotsTable = new JTable(cpuHotspotsModel);
        styleTable(cpuHotspotsTable);

        JScrollPane scrollPane = new JScrollPane(cpuHotspotsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createThreadBreakdownPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        String[] columns = {"Thread ID", "Thread Name", "Samples", "CPU Time (ms)", "Runnable", "Blocked", "Waiting"};
        cpuThreadsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        cpuThreadsTable = new JTable(cpuThreadsModel);
        styleTable(cpuThreadsTable);

        JScrollPane scrollPane = new JScrollPane(cpuThreadsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPackageAggregationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        String[] columns = {"Package", "Total Samples", "Self Samples", "Method Count"};
        cpuPackagesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        cpuPackagesTable = new JTable(cpuPackagesModel);
        styleTable(cpuPackagesTable);

        JScrollPane scrollPane = new JScrollPane(cpuPackagesTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMemoryResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_COLOR);

        JTabbedPane tabs = new JTabbedPane();
        styleTabPane(tabs);

        // Summary Tab
        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setBackground(BG_COLOR);

        memSummaryArea = new JTextArea();
        memSummaryArea.setEditable(false);
        memSummaryArea.setBackground(PANEL_BG);
        memSummaryArea.setForeground(TEXT_COLOR);
        memSummaryArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        memSummaryArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane summaryScroll = new JScrollPane(memSummaryArea);
        summaryScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        summaryPanel.add(summaryScroll, BorderLayout.CENTER);

        tabs.addTab("Summary", summaryPanel);

        // Heap Histogram Tab
        tabs.addTab("Heap Histogram", createHeapHistogramPanel());

        panel.add(tabs, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createHeapHistogramPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(BG_COLOR);

        // Top control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setBackground(PANEL_BG);

        captureHistogramBtn = createStyledButton("Capture Heap Histogram", ACCENT_COLOR);
        captureHistogramBtn.addActionListener(e -> captureHeapHistogram());
        controlPanel.add(captureHistogramBtn);

        JLabel helpLabel = createStyledLabel("(Captures current heap class distribution - may take a moment)");
        helpLabel.setForeground(new Color(150, 150, 150));
        controlPanel.add(helpLabel);

        panel.add(controlPanel, BorderLayout.NORTH);

        // Histogram table
        String[] columns = {"Class Name", "Package", "Instances", "Total Bytes", "Avg Size", "% of Heap"};
        heapHistogramModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        heapHistogramTable = new JTable(heapHistogramModel);
        styleTable(heapHistogramTable);

        JScrollPane scrollPane = new JScrollPane(heapHistogramTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(20, 21, 24)));

        statusLabel = createStyledLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(statusLabel);

        return panel;
    }

    // ==================== Sampling Control Methods ====================

    private void startCPUSampling() {
        // Parse interval
        String intervalStr = (String) cpuIntervalCombo.getSelectedItem();
        int intervalMs = parseInterval(intervalStr);

        cpuSampler.setSamplingInterval(intervalMs);
        cpuSampler.setFilterSystemFrames(filterSystemCheckbox.isSelected());
        cpuSampler.start();

        cpuStartBtn.setEnabled(false);
        cpuStopBtn.setEnabled(true);
        cpuIntervalCombo.setEnabled(false);
        filterSystemCheckbox.setEnabled(false);
    }

    private void stopCPUSampling() {
        cpuSampler.stop();

        cpuStartBtn.setEnabled(true);
        cpuStopBtn.setEnabled(false);
        cpuExportBtn.setEnabled(true);
        cpuIntervalCombo.setEnabled(true);
        filterSystemCheckbox.setEnabled(true);

        // Analyze results
        analyzeCPUResults();
    }

    private void startMemorySampling() {
        String intervalStr = (String) memIntervalCombo.getSelectedItem();
        int intervalMs = parseInterval(intervalStr);

        memorySampler.setSamplingInterval(intervalMs);
        memorySampler.start();

        memStartBtn.setEnabled(false);
        memStopBtn.setEnabled(true);
        memIntervalCombo.setEnabled(false);
    }

    private void stopMemorySampling() {
        memorySampler.stop();

        memStartBtn.setEnabled(true);
        memStopBtn.setEnabled(false);
        memExportBtn.setEnabled(true);
        memIntervalCombo.setEnabled(true);

        // Analyze results
        analyzeMemoryResults();
    }

    private int parseInterval(String interval) {
        interval = interval.toLowerCase().replace("ms", "").replace("s", "");
        try {
            int value = Integer.parseInt(interval.trim());
            // If no 'ms' suffix, assume seconds
            return interval.contains("ms") ? value : value * 1000;
        } catch (NumberFormatException e) {
            return 50; // Default 50ms
        }
    }

    private void clearAllData() {
        cpuSampler.clear();
        memorySampler.clear();

        cpuHotspotsModel.setRowCount(0);
        cpuThreadsModel.setRowCount(0);
        cpuPackagesModel.setRowCount(0);
        memSummaryArea.setText("");
        heapHistogramModel.setRowCount(0);

        cpuResults = null;
        memResults = null;

        cpuExportBtn.setEnabled(false);
        memExportBtn.setEnabled(false);
    }

    // ==================== Analysis Methods ====================

    private void analyzeCPUResults() {
        SwingUtilities.invokeLater(() -> {
            cpuResults = analyzer.analyzeCPU(cpuSampler.getStackSamples());
            displayCPUResults();
        });
    }

    private void analyzeMemoryResults() {
        SwingUtilities.invokeLater(() -> {
            memResults = analyzer.analyzeMemory(
                memorySampler.getHeapSamples(),
                memorySampler.getGCSamples()
            );
            displayMemoryResults();
        });
    }

    private void displayCPUResults() {
        if (cpuResults == null) return;

        // Update method hotspots
        cpuHotspotsModel.setRowCount(0);
        List<SampleAnalyzer.MethodStats> topMethods = cpuResults.getTopMethods(100);
        for (SampleAnalyzer.MethodStats stats : topMethods) {
            cpuHotspotsModel.addRow(new Object[]{
                stats.method.getMethodName(),
                stats.method.getSimpleClassName(),
                stats.method.getPackageName(),
                String.format("%.2f%%", stats.selfTimePercent),
                String.format("%.2f%%", stats.totalTimePercent),
                stats.selfSamples,
                stats.totalSamples
            });
        }

        // Update thread breakdown
        cpuThreadsModel.setRowCount(0);
        for (SampleAnalyzer.ThreadStats stats : cpuResults.threadBreakdown.values()) {
            cpuThreadsModel.addRow(new Object[]{
                stats.threadId,
                stats.threadName,
                stats.sampleCount,
                stats.totalCpuTime / 1_000_000, // Convert to ms
                stats.states.getOrDefault(Thread.State.RUNNABLE, 0),
                stats.states.getOrDefault(Thread.State.BLOCKED, 0),
                stats.states.getOrDefault(Thread.State.WAITING, 0) +
                    stats.states.getOrDefault(Thread.State.TIMED_WAITING, 0)
            });
        }

        // Update package aggregation
        cpuPackagesModel.setRowCount(0);
        List<SampleAnalyzer.PackageStats> topPackages = cpuResults.getTopPackages(50);
        for (SampleAnalyzer.PackageStats stats : topPackages) {
            cpuPackagesModel.addRow(new Object[]{
                stats.packageName,
                stats.totalSamples,
                stats.selfSamples,
                stats.methodCount
            });
        }

        // Build flame graph
        buildFlameGraph();
    }

    private void buildFlameGraph() {
        List<com.tonic.services.profiler.sampling.StackSample> samples = cpuSampler.getStackSamples().getAll();
        if (samples.isEmpty()) {
            return;
        }

        // Create root node
        com.tonic.services.profiler.visualization.FlameGraphNode root =
            new com.tonic.services.profiler.visualization.FlameGraphNode("(root)", "", "");

        // Build call tree from stack samples
        for (com.tonic.services.profiler.sampling.StackSample sample : samples) {
            StackTraceElement[] stack = sample.stackTrace;

            if (stack.length == 0) {
                continue;
            }

            // Traverse from bottom to top (root to leaf)
            com.tonic.services.profiler.visualization.FlameGraphNode current = root;
            current.addSample();

            for (int i = stack.length - 1; i >= 0; i--) {
                StackTraceElement frame = stack[i];

                String methodName = frame.getMethodName();
                String className = frame.getClassName();
                String packageName = "";

                int lastDot = className.lastIndexOf('.');
                if (lastDot >= 0) {
                    packageName = className.substring(0, lastDot);
                    className = className.substring(lastDot + 1);
                }

                current = current.getOrCreateChild(methodName, className, packageName);
                current.addSample();
            }
        }

        flameGraphPanel.setRoot(root);
    }

    private void displayMemoryResults() {
        if (memResults == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("=== Memory Sampling Results ===\n\n");

        sb.append("Allocation Analysis:\n");
        sb.append(String.format("  Average Allocation Rate:  %.2f MB/s\n", memResults.averageAllocationRate));
        sb.append(String.format("  Peak Allocation Rate:     %.2f MB/s\n", memResults.peakAllocationRate));
        sb.append(String.format("  Total Estimated Alloc:    %.2f MB\n", memResults.totalEstimatedAllocation));
        sb.append(String.format("  Heap Growth Trend:        %s\n\n", memResults.heapGrowthTrend));

        sb.append("Garbage Collection:\n");
        sb.append(String.format("  Total GC Events:          %d\n", memResults.totalGCEvents));
        sb.append(String.format("  Average GC Overhead:      %.2f%%\n", memResults.averageGCOverhead));
        sb.append(String.format("  Full GC Count:            %d\n", memResults.fullGCCount));

        memSummaryArea.setText(sb.toString());
    }

    private void captureHeapHistogram() {
        captureHistogramBtn.setEnabled(false);
        captureHistogramBtn.setText("Capturing...");

        // Run in background thread to avoid UI freeze
        new Thread(() -> {
            try {
                List<HeapHistogramSample> histogram = memorySampler.captureHeapHistogram();
                SwingUtilities.invokeLater(() -> {
                    displayHeapHistogram(histogram);
                    captureHistogramBtn.setEnabled(true);
                    captureHistogramBtn.setText("Capture Heap Histogram");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        "Failed to capture heap histogram: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    captureHistogramBtn.setEnabled(true);
                    captureHistogramBtn.setText("Capture Heap Histogram");
                });
            }
        }).start();
    }

    private void displayHeapHistogram(List<HeapHistogramSample> histogram) {
        heapHistogramModel.setRowCount(0);

        if (histogram == null || histogram.isEmpty()) {
            return;
        }

        // Calculate total for percentage
        long totalHeapBytes = histogram.stream().mapToLong(s -> s.totalBytes).sum();

        // Display top entries
        int count = 0;
        for (HeapHistogramSample sample : histogram) {
            if (count++ >= 500) break; // Limit to top 500

            heapHistogramModel.addRow(new Object[]{
                sample.simpleClassName,
                sample.packageName,
                String.format("%,d", sample.instanceCount),
                formatBytes(sample.totalBytes),
                formatBytes(sample.averageSize),
                String.format("%.2f%%", sample.getPercentOfTotal(totalHeapBytes))
            });
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // ==================== Export Methods ====================

    private void exportCPUResults() {
        if (cpuResults == null) {
            JOptionPane.showMessageDialog(this, "No CPU results to export", "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export CPU Results");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        fileChooser.setSelectedFile(new File("cpu_profile_" + timestamp + ".csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                exporter.exportCPUToCSV(writer, cpuResults);
                JOptionPane.showMessageDialog(this, "CPU results exported successfully!", "Export", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportMemoryResults() {
        if (memResults == null) {
            JOptionPane.showMessageDialog(this, "No memory results to export", "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Memory Results");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        fileChooser.setSelectedFile(new File("memory_profile_" + timestamp + ".csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                exporter.exportMemoryToCSV(writer, memResults);
                exporter.exportHeapSamplesToCSV(writer, memorySampler.getHeapSamples());
                exporter.exportGCSamplesToCSV(writer, memorySampler.getGCSamples());

                // Export heap histogram if captured
                List<HeapHistogramSample> histogram = memorySampler.getLatestHistogram();
                if (histogram != null && !histogram.isEmpty()) {
                    writer.println();
                    exporter.exportHeapHistogramToCSV(writer, histogram);
                }

                JOptionPane.showMessageDialog(this, "Memory results exported successfully!", "Export", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ==================== Status Update ====================

    private void updateStatus() {
        // Update CPU status
        if (cpuSampler.isRunning()) {
            CPUSampler.SamplingStats stats = cpuSampler.getStats();
            cpuStatusLabel.setText(String.format("Sampling: %d samples (%d dropped)",
                stats.currentSamples, stats.droppedSamples));
        } else if (cpuResults != null) {
            cpuStatusLabel.setText(String.format("Stopped: %d samples analyzed", cpuResults.totalSamples));
        } else {
            cpuStatusLabel.setText("Idle");
        }

        // Update memory status
        if (memorySampler.isRunning()) {
            MemorySampler.SamplingStats stats = memorySampler.getStats();
            memStatusLabel.setText(String.format("Sampling: %d samples (%d GC events)",
                stats.currentHeapSamples, stats.gcEventCount));
        } else if (memResults != null) {
            memStatusLabel.setText(String.format("Stopped: %.2f MB/s avg rate", memResults.averageAllocationRate));
        } else {
            memStatusLabel.setText("Idle");
        }

        // Update status bar
        boolean anyRunning = cpuSampler.isRunning() || memorySampler.isRunning();
        if (anyRunning) {
            statusLabel.setText("Sampling in progress...");
            statusLabel.setForeground(SUCCESS_COLOR);
        } else {
            statusLabel.setText("Ready");
            statusLabel.setForeground(TEXT_COLOR);
        }
    }

    // ==================== Styling Methods ====================

    private JPanel createStyledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(50, 52, 56)),
            title,
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 12),
            TEXT_COLOR
        ));
        return panel;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_COLOR);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return label;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void styleComboBox(JComboBox<?> combo) {
        combo.setBackground(PANEL_BG);
        combo.setForeground(TEXT_COLOR);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
    }

    private void styleTable(JTable table) {
        table.setBackground(PANEL_BG);
        table.setForeground(TEXT_COLOR);
        table.setFont(new Font("Consolas", Font.PLAIN, 11));
        table.setRowHeight(22);
        table.getTableHeader().setBackground(new Color(50, 52, 56));
        table.getTableHeader().setForeground(TEXT_COLOR);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private void styleTabPane(JTabbedPane tabs) {
        tabs.setBackground(PANEL_BG);
        tabs.setForeground(TEXT_COLOR);
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    }

    // ==================== Cleanup ====================

    /**
     * Stop all samplers and clean up resources
     */
    public void cleanup() {
        // Stop status timer
        if (statusTimer != null) {
            statusTimer.stop();
            statusTimer = null;
        }

        // Stop CPU sampler if running
        if (cpuSampler != null && cpuSampler.isRunning()) {
            cpuSampler.stop();
        }

        // Stop memory sampler if running
        if (memorySampler != null && memorySampler.isRunning()) {
            memorySampler.stop();
        }
    }
}

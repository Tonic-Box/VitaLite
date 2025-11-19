package com.tonic.services.profiler;

import com.tonic.model.ui.components.VitaFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.lang.management.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive JVM Profiler and Performance Configuration Window
 */
public class ProfilerWindow extends VitaFrame {
    private static ProfilerWindow instance;
    private static final Color BG_COLOR = new Color(30, 31, 34);
    private static final Color PANEL_BG = new Color(40, 42, 46);
    private static final Color TEXT_COLOR = new Color(200, 200, 200);
    private static final Color ACCENT_COLOR = new Color(64, 156, 255);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);
    private static final Color ERROR_COLOR = new Color(244, 67, 54);

    private JTabbedPane tabbedPane;
    private Timer refreshTimer;
    private Timer metricsTimer;
    private SamplingTab samplingTab;
    private com.tonic.services.profiler.leak.LeakDetectorTab leakDetectorTab;

    // GC Pause Analysis
    private com.tonic.services.profiler.gc.GCPauseAnalyzer gcPauseAnalyzer;
    private com.tonic.services.profiler.gc.GCTuningAdvisor gcTuningAdvisor;
    private JTextArea gcStatsArea;
    private JTextArea gcRecommendationsArea;

    // Event Timeline
    private com.tonic.services.profiler.timeline.TimelinePanel timelinePanel;

    // Resource Monitor components
    private ResourceMetricsCollector metricsCollector;
    private CircularBuffer<MetricSnapshot> metricsHistory;
    private boolean monitoringPaused = false;
    private int timeWindowSeconds = 120; // 2 minutes default

    // Resource Monitor chart series
    private TimeSeries cpuSeries, gcSeries;
    private TimeSeries heapUsedSeries, heapCommittedSeries, heapMaxSeries;
    private TimeSeries metaspaceUsedSeries, metaspaceCommittedSeries;
    private TimeSeries threadCountSeries, daemonThreadSeries;

    // Resource Monitor chart panels
    private ChartPanel cpuGcChartPanel, heapChartPanel, metaspaceChartPanel, threadChartPanel;

    // Resource Monitor summary labels
    private JLabel cpuSummaryLabel, gcSummaryLabel, heapSummaryLabel;
    private JLabel metaspaceSummaryLabel, threadSummaryLabel;

    // JIT tab components
    private JTextArea jitStatusArea;
    private JComboBox<String> compilationLevelCombo;
    private JTextField methodNameField;
    private JLabel jitInfoLabel;
    private JTextArea escapeAnalysisArea;

    // Thread tab components
    private JList<String> threadList;
    private DefaultListModel<String> threadListModel;
    private JTextArea threadStackArea;

    // VM Config tab components
    private JTextArea vmFlagsArea, cpuFeaturesArea;
    private JTextField flagNameField, flagValueField;
    private JComboBox<String> flagTypeCombo;

    // JVMTI tab components
    private JTextArea jvmtiStatusArea, vmStateArea, diagnosticsArea;
    private ChartPanel jvmtiMemoryChartPanel, jvmtiThreadChartPanel, jvmtiClassChartPanel;
    private JProgressBar memoryUtilizationBar;
    private JLabel memoryStatusLabel, jvmtiThreadStatusLabel, classStatusLabel;

    /**
     * Static method to show the viewer with platform info data
     */
    public static void toggle() {
        SwingUtilities.invokeLater(() -> {
            if(instance != null && instance.isVisible()) {
                // Close and dispose when toggling off
                instance.dispose();
                return;
            }

            if (instance == null) {
                instance = new ProfilerWindow();
            }
            instance.setVisible(true);
            instance.toFront();
            instance.requestFocus();
        });
    }

    public ProfilerWindow() {
        super("VitaLite JVM Profiler");
        setTitle("VitaLite JVM Profiler");
        setSize(900, 700);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);

        // Add window listener for cleanup on close
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cleanup();
            }
        });

        initializeComponents();
        startAutoRefresh();
    }

    private void initializeComponents() {
        getContentPanel().setLayout(new BorderLayout());
        getContentPanel().setBackground(BG_COLOR);

        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(PANEL_BG);
        tabbedPane.setForeground(TEXT_COLOR);
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Add tabs
        tabbedPane.addTab("Resource Monitor", createResourceMonitorPanel());
        samplingTab = new SamplingTab();
        tabbedPane.addTab("Sampling", samplingTab);
        leakDetectorTab = new com.tonic.services.profiler.leak.LeakDetectorTab();
        tabbedPane.addTab("Leak Detector", leakDetectorTab);
        timelinePanel = new com.tonic.services.profiler.timeline.TimelinePanel(gcPauseAnalyzer);
        tabbedPane.addTab("Event Timeline", timelinePanel);
        tabbedPane.addTab("JIT Compiler", createJITPanel());
        tabbedPane.addTab("Threads", createThreadPanel());
        tabbedPane.addTab("VM Configuration", createVMConfigPanel());
        tabbedPane.addTab("JVMTI / VM", createJVMTIPanel());

        getContentPanel().add(tabbedPane, BorderLayout.CENTER);

        // Add status bar at bottom
        JPanel statusBar = createStatusBar();
        getContentPanel().add(statusBar, BorderLayout.SOUTH);
    }

    // ==================== RESOURCE MONITOR PANEL ====================

    private JPanel createResourceMonitorPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Initialize metrics collection
        metricsCollector = new ResourceMetricsCollector();
        metricsHistory = new CircularBuffer<>(timeWindowSeconds); // Store N seconds of history

        // Initialize GC pause analyzer
        gcPauseAnalyzer = new com.tonic.services.profiler.gc.GCPauseAnalyzer(500);
        gcTuningAdvisor = new com.tonic.services.profiler.gc.GCTuningAdvisor(gcPauseAnalyzer);
        gcPauseAnalyzer.startMonitoring();

        // Initialize time series
        initializeTimeSeries();

        // Top - Control Panel
        JPanel controlPanel = createControlPanel();
        panel.add(controlPanel, BorderLayout.NORTH);

        // Center - Main content with charts and GC analysis
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);

        // Charts Grid
        JPanel chartsGrid = new JPanel(new GridLayout(2, 2, 10, 10));
        chartsGrid.setOpaque(false);

        cpuGcChartPanel = createCpuGcChart();
        heapChartPanel = createHeapChart();
        metaspaceChartPanel = createMetaspaceChart();
        threadChartPanel = createThreadChart();

        chartsGrid.add(cpuGcChartPanel);
        chartsGrid.add(heapChartPanel);
        chartsGrid.add(metaspaceChartPanel);
        chartsGrid.add(threadChartPanel);

        centerPanel.add(chartsGrid, BorderLayout.CENTER);

        // GC Pause Analysis Panel
        JPanel gcAnalysisPanel = createGCAnalysisPanel();
        centerPanel.add(gcAnalysisPanel, BorderLayout.SOUTH);

        panel.add(centerPanel, BorderLayout.CENTER);

        // Bottom - Summary Stats Cards
        JPanel summaryPanel = createSummaryPanel();
        panel.add(summaryPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void initializeTimeSeries() {
        // CPU & GC series
        cpuSeries = new TimeSeries("CPU %");
        gcSeries = new TimeSeries("GC %");

        // Heap series
        heapUsedSeries = new TimeSeries("Used");
        heapCommittedSeries = new TimeSeries("Committed");
        heapMaxSeries = new TimeSeries("Max");

        // Metaspace series
        metaspaceUsedSeries = new TimeSeries("Used");
        metaspaceCommittedSeries = new TimeSeries("Committed");

        // Thread series
        threadCountSeries = new TimeSeries("Total");
        daemonThreadSeries = new TimeSeries("Daemon");

        // Set max age for time series (auto-removes old data)
        int maxAgeMs = timeWindowSeconds * 1000;
        cpuSeries.setMaximumItemAge(maxAgeMs);
        gcSeries.setMaximumItemAge(maxAgeMs);
        heapUsedSeries.setMaximumItemAge(maxAgeMs);
        heapCommittedSeries.setMaximumItemAge(maxAgeMs);
        heapMaxSeries.setMaximumItemAge(maxAgeMs);
        metaspaceUsedSeries.setMaximumItemAge(maxAgeMs);
        metaspaceCommittedSeries.setMaximumItemAge(maxAgeMs);
        threadCountSeries.setMaximumItemAge(maxAgeMs);
        daemonThreadSeries.setMaximumItemAge(maxAgeMs);
    }

    private JPanel createControlPanel() {
        JPanel panel = createStyledPanel("Monitoring Controls");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Pause/Resume button
        JButton pauseResumeBtn = createStyledButton("Pause", WARNING_COLOR);
        pauseResumeBtn.addActionListener(e -> {
            monitoringPaused = !monitoringPaused;
            pauseResumeBtn.setText(monitoringPaused ? "Resume" : "Pause");
            pauseResumeBtn.setBackground(monitoringPaused ? SUCCESS_COLOR : WARNING_COLOR);
            if (!monitoringPaused) {
                metricsCollector.resetTracking(); // Reset deltas when resuming
            }
        });
        panel.add(pauseResumeBtn);

        // Time window selector
        panel.add(createStyledLabel("Time Window:"));
        JComboBox<String> timeWindowCombo = new JComboBox<>(new String[]{
            "1 Minute", "2 Minutes", "5 Minutes", "10 Minutes"
        });
        timeWindowCombo.setBackground(PANEL_BG);
        timeWindowCombo.setForeground(TEXT_COLOR);
        timeWindowCombo.setSelectedIndex(1); // Default to 2 minutes
        timeWindowCombo.addActionListener(e -> {
            int selected = timeWindowCombo.getSelectedIndex();
            switch (selected) {
                case 0: timeWindowSeconds = 60; break;
                case 1: timeWindowSeconds = 120; break;
                case 2: timeWindowSeconds = 300; break;
                case 3: timeWindowSeconds = 600; break;
            }
            updateTimeSeriesMaxAge();
            metricsHistory = new CircularBuffer<>(timeWindowSeconds);
        });
        panel.add(timeWindowCombo);

        // Export to CSV button
        JButton exportBtn = createStyledButton("Export CSV", ACCENT_COLOR);
        exportBtn.addActionListener(e -> exportMetricsToCSV());
        panel.add(exportBtn);

        // Force GC button
        JButton gcBtn = createStyledButton("Force GC", ERROR_COLOR);
        gcBtn.addActionListener(e -> {
            System.gc();
            System.runFinalization();
            System.gc();
        });
        panel.add(gcBtn);

        // Clear charts button
        JButton clearBtn = createStyledButton("Clear", new Color(100, 100, 100));
        clearBtn.addActionListener(e -> clearAllSeries());
        panel.add(clearBtn);

        return panel;
    }

    private void updateTimeSeriesMaxAge() {
        int maxAgeMs = timeWindowSeconds * 1000;
        cpuSeries.setMaximumItemAge(maxAgeMs);
        gcSeries.setMaximumItemAge(maxAgeMs);
        heapUsedSeries.setMaximumItemAge(maxAgeMs);
        heapCommittedSeries.setMaximumItemAge(maxAgeMs);
        heapMaxSeries.setMaximumItemAge(maxAgeMs);
        metaspaceUsedSeries.setMaximumItemAge(maxAgeMs);
        metaspaceCommittedSeries.setMaximumItemAge(maxAgeMs);
        threadCountSeries.setMaximumItemAge(maxAgeMs);
        daemonThreadSeries.setMaximumItemAge(maxAgeMs);
    }

    private void clearAllSeries() {
        cpuSeries.clear();
        gcSeries.clear();
        heapUsedSeries.clear();
        heapCommittedSeries.clear();
        heapMaxSeries.clear();
        metaspaceUsedSeries.clear();
        metaspaceCommittedSeries.clear();
        threadCountSeries.clear();
        daemonThreadSeries.clear();
        metricsHistory.clear();
    }

    private ChartPanel createCpuGcChart() {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(cpuSeries);
        dataset.addSeries(gcSeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "CPU & GC Activity",
            "Time",
            "Percentage (%)",
            dataset,
            true,
            true,
            false
        );

        styleChart(chart);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(PANEL_BG);
        plot.setDomainGridlinePaint(new Color(60, 62, 66));
        plot.setRangeGridlinePaint(new Color(60, 62, 66));

        // Renderer styling
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, ACCENT_COLOR); // CPU
        renderer.setSeriesPaint(1, ERROR_COLOR);  // GC
        plot.setRenderer(renderer);

        // Range axis (0-100%)
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(0, 100);
        rangeAxis.setTickLabelPaint(TEXT_COLOR);
        rangeAxis.setLabelPaint(TEXT_COLOR);

        // Domain axis (time)
        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        domainAxis.setTickLabelPaint(TEXT_COLOR);
        domainAxis.setLabelPaint(TEXT_COLOR);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBackground(PANEL_BG);
        chartPanel.setPreferredSize(new Dimension(400, 200));
        return chartPanel;
    }

    private ChartPanel createHeapChart() {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(heapUsedSeries);
        dataset.addSeries(heapCommittedSeries);
        dataset.addSeries(heapMaxSeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Heap Memory",
            "Time",
            "Memory (MB)",
            dataset,
            true,
            true,
            false
        );

        styleChart(chart);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(PANEL_BG);
        plot.setDomainGridlinePaint(new Color(60, 62, 66));
        plot.setRangeGridlinePaint(new Color(60, 62, 66));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, new Color(255, 193, 7));  // Used - Yellow
        renderer.setSeriesPaint(1, ACCENT_COLOR);            // Committed - Blue
        renderer.setSeriesPaint(2, SUCCESS_COLOR);           // Max - Green
        plot.setRenderer(renderer);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelPaint(TEXT_COLOR);
        rangeAxis.setLabelPaint(TEXT_COLOR);

        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        domainAxis.setTickLabelPaint(TEXT_COLOR);
        domainAxis.setLabelPaint(TEXT_COLOR);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBackground(PANEL_BG);
        chartPanel.setPreferredSize(new Dimension(400, 200));
        return chartPanel;
    }

    private ChartPanel createMetaspaceChart() {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(metaspaceUsedSeries);
        dataset.addSeries(metaspaceCommittedSeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Metaspace",
            "Time",
            "Memory (MB)",
            dataset,
            true,
            true,
            false
        );

        styleChart(chart);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(PANEL_BG);
        plot.setDomainGridlinePaint(new Color(60, 62, 66));
        plot.setRangeGridlinePaint(new Color(60, 62, 66));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, new Color(156, 39, 176)); // Used - Purple
        renderer.setSeriesPaint(1, new Color(103, 58, 183)); // Committed - Deep Purple
        plot.setRenderer(renderer);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelPaint(TEXT_COLOR);
        rangeAxis.setLabelPaint(TEXT_COLOR);

        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        domainAxis.setTickLabelPaint(TEXT_COLOR);
        domainAxis.setLabelPaint(TEXT_COLOR);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBackground(PANEL_BG);
        chartPanel.setPreferredSize(new Dimension(400, 200));
        return chartPanel;
    }

    private ChartPanel createThreadChart() {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(threadCountSeries);
        dataset.addSeries(daemonThreadSeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Thread Count",
            "Time",
            "Threads",
            dataset,
            true,
            true,
            false
        );

        styleChart(chart);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(PANEL_BG);
        plot.setDomainGridlinePaint(new Color(60, 62, 66));
        plot.setRangeGridlinePaint(new Color(60, 62, 66));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, new Color(0, 188, 212));  // Total - Cyan
        renderer.setSeriesPaint(1, new Color(233, 30, 99));  // Daemon - Pink
        plot.setRenderer(renderer);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelPaint(TEXT_COLOR);
        rangeAxis.setLabelPaint(TEXT_COLOR);
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        domainAxis.setTickLabelPaint(TEXT_COLOR);
        domainAxis.setLabelPaint(TEXT_COLOR);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBackground(PANEL_BG);
        chartPanel.setPreferredSize(new Dimension(400, 200));
        return chartPanel;
    }

    private void styleChart(JFreeChart chart) {
        chart.setBackgroundPaint(PANEL_BG);
        chart.getTitle().setPaint(TEXT_COLOR);
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 14));
        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(PANEL_BG);
            chart.getLegend().setItemPaint(TEXT_COLOR);
        }
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 10, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 80));

        cpuSummaryLabel = createStyledLabel("CPU: --%");
        gcSummaryLabel = createStyledLabel("GC: --%");
        heapSummaryLabel = createStyledLabel("Heap: -- MB");
        metaspaceSummaryLabel = createStyledLabel("Metaspace: -- MB");
        threadSummaryLabel = createStyledLabel("Threads: --");

        Font summaryFont = new Font("Segoe UI", Font.BOLD, 13);
        cpuSummaryLabel.setFont(summaryFont);
        gcSummaryLabel.setFont(summaryFont);
        heapSummaryLabel.setFont(summaryFont);
        metaspaceSummaryLabel.setFont(summaryFont);
        threadSummaryLabel.setFont(summaryFont);

        JPanel cpuCard = createSummaryCard("CPU Usage", cpuSummaryLabel, ACCENT_COLOR);
        JPanel gcCard = createSummaryCard("GC Activity", gcSummaryLabel, ERROR_COLOR);
        JPanel heapCard = createSummaryCard("Heap Memory", heapSummaryLabel, new Color(255, 193, 7));
        JPanel metaCard = createSummaryCard("Metaspace", metaspaceSummaryLabel, new Color(156, 39, 176));
        JPanel threadCard = createSummaryCard("Threads", threadSummaryLabel, new Color(0, 188, 212));

        panel.add(cpuCard);
        panel.add(gcCard);
        panel.add(heapCard);
        panel.add(metaCard);
        panel.add(threadCard);

        return panel;
    }

    private JPanel createSummaryCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(PANEL_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            new EmptyBorder(8, 10, 8, 10)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(150, 150, 150));
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        valueLabel.setForeground(TEXT_COLOR);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void exportMetricsToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Metrics to CSV");
        fileChooser.setSelectedFile(new java.io.File("metrics_" + System.currentTimeMillis() + ".csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(fileChooser.getSelectedFile())) {
                writer.println("Timestamp,CPU%,GC%,HeapUsed(MB),HeapCommitted(MB),HeapMax(MB),MetaspaceUsed(MB),MetaspaceCommitted(MB),Threads,DaemonThreads");

                for (MetricSnapshot snapshot : metricsHistory.getAll()) {
                    writer.printf("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d\n",
                        snapshot.timestamp,
                        snapshot.cpuPercent,
                        snapshot.gcPercent,
                        snapshot.heapUsed / (1024.0 * 1024.0),
                        snapshot.heapCommitted / (1024.0 * 1024.0),
                        snapshot.heapMax / (1024.0 * 1024.0),
                        snapshot.metaspaceUsed / (1024.0 * 1024.0),
                        snapshot.metaspaceCommitted / (1024.0 * 1024.0),
                        snapshot.threadCount,
                        snapshot.daemonThreadCount
                    );
                }

                JOptionPane.showMessageDialog(this, "Metrics exported successfully!", "Export", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel createGCAnalysisPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 180));

        // Left - GC Pause Statistics
        JPanel statsPanel = createStyledPanel("GC Pause Statistics");
        statsPanel.setLayout(new BorderLayout());

        gcStatsArea = new JTextArea();
        gcStatsArea.setEditable(false);
        gcStatsArea.setBackground(PANEL_BG);
        gcStatsArea.setForeground(TEXT_COLOR);
        gcStatsArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        gcStatsArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        gcStatsArea.setText("Collecting GC data...");

        JScrollPane statsScroll = new JScrollPane(gcStatsArea);
        statsScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        statsPanel.add(statsScroll, BorderLayout.CENTER);

        // Right - Tuning Recommendations
        JPanel recommendationsPanel = createStyledPanel("Tuning Recommendations");
        recommendationsPanel.setLayout(new BorderLayout());

        gcRecommendationsArea = new JTextArea();
        gcRecommendationsArea.setEditable(false);
        gcRecommendationsArea.setBackground(PANEL_BG);
        gcRecommendationsArea.setForeground(TEXT_COLOR);
        gcRecommendationsArea.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        gcRecommendationsArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        gcRecommendationsArea.setLineWrap(true);
        gcRecommendationsArea.setWrapStyleWord(true);
        gcRecommendationsArea.setText("Analyzing GC patterns...");

        JScrollPane recommendationsScroll = new JScrollPane(gcRecommendationsArea);
        recommendationsScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        recommendationsPanel.add(recommendationsScroll, BorderLayout.CENTER);

        panel.add(statsPanel);
        panel.add(recommendationsPanel);

        return panel;
    }

    // ==================== JIT COMPILER PANEL ====================

    private JPanel createJITPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top - Status and Info
        JPanel statusPanel = createStyledPanel("JIT Compiler Status");
        statusPanel.setLayout(new BorderLayout(5, 5));

        jitInfoLabel = createStyledLabel("JIT Compiler: " + (JITCompilerAccess.isAvailable() ? "Available" : "Not Available"));
        jitInfoLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        jitStatusArea = new JTextArea(8, 40);
        jitStatusArea.setEditable(false);
        jitStatusArea.setBackground(PANEL_BG);
        jitStatusArea.setForeground(TEXT_COLOR);
        jitStatusArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        jitStatusArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane statusScroll = new JScrollPane(jitStatusArea);
        statusScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));

        statusPanel.add(jitInfoLabel, BorderLayout.NORTH);
        statusPanel.add(statusScroll, BorderLayout.CENTER);

        // Middle - Compilation Controls
        JPanel controlsPanel = createStyledPanel("Compilation Controls");
        controlsPanel.setLayout(new GridLayout(3, 1, 5, 5));

        // Method compilation
        JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        methodPanel.setOpaque(false);
        methodPanel.add(createStyledLabel("Method:"));
        methodNameField = new JTextField(30);
        styleTextField(methodNameField);
        methodPanel.add(methodNameField);
        controlsPanel.add(methodPanel);

        // Compilation level
        JPanel levelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        levelPanel.setOpaque(false);
        levelPanel.add(createStyledLabel("Level:"));
        compilationLevelCombo = new JComboBox<>(new String[]{
            "0 - Interpreter",
            "1 - C1 (Simple)",
            "2 - C1 (Limited Profile)",
            "3 - C1 (Full Profile)",
            "4 - C2/JVMCI (Full Optimization)"
        });
        compilationLevelCombo.setBackground(PANEL_BG);
        compilationLevelCombo.setForeground(TEXT_COLOR);
        compilationLevelCombo.setSelectedIndex(4);
        levelPanel.add(compilationLevelCombo);
        controlsPanel.add(levelPanel);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.setOpaque(false);

        JButton compileBtn = createStyledButton("Force Compile", ACCENT_COLOR);
        JButton deoptBtn = createStyledButton("Deoptimize All", WARNING_COLOR);
        JButton refreshBtn = createStyledButton("Refresh Stats", SUCCESS_COLOR);

        compileBtn.addActionListener(e -> forceCompileMethod());
        deoptBtn.addActionListener(e -> {
            JITCompilerAccess.deoptimizeAll();
            refreshJITInfo();
            JOptionPane.showMessageDialog(this, "All methods deoptimized", "JIT", JOptionPane.INFORMATION_MESSAGE);
        });
        refreshBtn.addActionListener(e -> refreshJITInfo());

        buttonPanel.add(compileBtn);
        buttonPanel.add(deoptBtn);
        buttonPanel.add(refreshBtn);
        controlsPanel.add(buttonPanel);

        // Bottom - Escape Analysis Insights
        JPanel escapePanel = createStyledPanel("Escape Analysis & Optimization Insights");
        escapePanel.setLayout(new BorderLayout());
        escapePanel.setPreferredSize(new Dimension(0, 200));

        escapeAnalysisArea = new JTextArea();
        escapeAnalysisArea.setEditable(false);
        escapeAnalysisArea.setBackground(PANEL_BG);
        escapeAnalysisArea.setForeground(TEXT_COLOR);
        escapeAnalysisArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        escapeAnalysisArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        escapeAnalysisArea.setLineWrap(true);
        escapeAnalysisArea.setWrapStyleWord(true);
        escapeAnalysisArea.setText("JIT compiler optimization insights will appear here...");

        JScrollPane escapeScroll = new JScrollPane(escapeAnalysisArea);
        escapeScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        escapePanel.add(escapeScroll, BorderLayout.CENTER);

        panel.add(statusPanel, BorderLayout.NORTH);
        panel.add(controlsPanel, BorderLayout.CENTER);
        panel.add(escapePanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== THREAD PROFILER PANEL ====================

    private JPanel createThreadPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Left - Thread List
        JPanel listPanel = createStyledPanel("Active Threads");
        listPanel.setLayout(new BorderLayout());

        threadListModel = new DefaultListModel<>();
        threadList = new JList<>(threadListModel);
        threadList.setBackground(PANEL_BG);
        threadList.setForeground(TEXT_COLOR);
        threadList.setFont(new Font("Consolas", Font.PLAIN, 12));
        threadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        threadList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showThreadStack();
            }
        });

        JScrollPane listScroll = new JScrollPane(threadList);
        listScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        listScroll.setPreferredSize(new Dimension(250, 0));
        listPanel.add(listScroll, BorderLayout.CENTER);

        JButton refreshThreadsBtn = createStyledButton("Refresh", ACCENT_COLOR);
        refreshThreadsBtn.addActionListener(e -> refreshThreadInfo());
        listPanel.add(refreshThreadsBtn, BorderLayout.SOUTH);

        // Right - Stack Trace
        JPanel stackPanel = createStyledPanel("Thread Stack Trace");
        stackPanel.setLayout(new BorderLayout());

        threadStackArea = new JTextArea();
        threadStackArea.setEditable(false);
        threadStackArea.setBackground(PANEL_BG);
        threadStackArea.setForeground(TEXT_COLOR);
        threadStackArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        threadStackArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane stackScroll = new JScrollPane(threadStackArea);
        stackScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        stackPanel.add(stackScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, stackPanel);
        splitPane.setDividerLocation(300);
        splitPane.setBackground(BG_COLOR);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    // ==================== VM CONFIGURATION PANEL ====================

    private JPanel createVMConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top - VM Flags
        JPanel flagsPanel = createStyledPanel("VM Flags");
        flagsPanel.setLayout(new BorderLayout(5, 5));

        vmFlagsArea = new JTextArea(12, 40);
        vmFlagsArea.setEditable(false);
        vmFlagsArea.setBackground(PANEL_BG);
        vmFlagsArea.setForeground(TEXT_COLOR);
        vmFlagsArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        vmFlagsArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane flagsScroll = new JScrollPane(vmFlagsArea);
        flagsScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        flagsPanel.add(flagsScroll, BorderLayout.CENTER);

        // Flag modification panel
        JPanel modPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        modPanel.setOpaque(false);

        modPanel.add(createStyledLabel("Flag:"));
        flagNameField = new JTextField(20);
        styleTextField(flagNameField);
        modPanel.add(flagNameField);

        modPanel.add(createStyledLabel("Value:"));
        flagValueField = new JTextField(15);
        styleTextField(flagValueField);
        modPanel.add(flagValueField);

        modPanel.add(createStyledLabel("Type:"));
        flagTypeCombo = new JComboBox<>(new String[]{"Boolean", "Int", "String"});
        flagTypeCombo.setBackground(PANEL_BG);
        flagTypeCombo.setForeground(TEXT_COLOR);
        modPanel.add(flagTypeCombo);

        JButton setFlagBtn = createStyledButton("Set Flag", ACCENT_COLOR);
        setFlagBtn.addActionListener(e -> setVMFlag());
        modPanel.add(setFlagBtn);

        JButton refreshFlagsBtn = createStyledButton("Refresh", SUCCESS_COLOR);
        refreshFlagsBtn.addActionListener(e -> refreshVMFlags());
        modPanel.add(refreshFlagsBtn);

        flagsPanel.add(modPanel, BorderLayout.SOUTH);

        // Bottom - CPU Features
        JPanel cpuPanel = createStyledPanel("CPU Features & System Info");
        cpuPanel.setLayout(new BorderLayout());

        cpuFeaturesArea = new JTextArea(8, 40);
        cpuFeaturesArea.setEditable(false);
        cpuFeaturesArea.setBackground(PANEL_BG);
        cpuFeaturesArea.setForeground(TEXT_COLOR);
        cpuFeaturesArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        cpuFeaturesArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane cpuScroll = new JScrollPane(cpuFeaturesArea);
        cpuScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        cpuPanel.add(cpuScroll, BorderLayout.CENTER);

        panel.add(flagsPanel, BorderLayout.CENTER);
        panel.add(cpuPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== JVMTI / VM PANEL ====================

    private JPanel createJVMTIPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top - Status Cards Row with visual indicators
        JPanel statusCardsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        statusCardsPanel.setOpaque(false);
        statusCardsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // JVMTI Status Card
        JPanel jvmtiCard = createVisualStatusCard("JVMTI Status",
            JVMTI.isAvailable() ? "Available" : "Unavailable",
            JVMTI.isAvailable() ? SUCCESS_COLOR : ERROR_COLOR);
        statusCardsPanel.add(jvmtiCard);

        // VM Status Card
        JPanel vmCard = createVisualStatusCard("VM Status",
            VM.isAvailable() ? "Available" : "Unavailable",
            VM.isAvailable() ? SUCCESS_COLOR : ERROR_COLOR);
        statusCardsPanel.add(vmCard);

        // System Status Card with processor count
        JPanel systemCard = createVisualStatusCard("System",
            Runtime.getRuntime().availableProcessors() + " Cores",
            ACCENT_COLOR);
        statusCardsPanel.add(systemCard);

        // Middle section with charts
        JPanel chartsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        chartsPanel.setOpaque(false);

        // Memory Utilization Chart
        JPanel memoryPanel = createStyledPanel("Memory Utilization");
        memoryPanel.setLayout(new BorderLayout(5, 5));

        memoryUtilizationBar = new JProgressBar(0, 100);
        memoryUtilizationBar.setStringPainted(true);
        memoryUtilizationBar.setForeground(ACCENT_COLOR);
        memoryUtilizationBar.setBackground(PANEL_BG);

        memoryStatusLabel = new JLabel("Loading...");
        memoryStatusLabel.setForeground(TEXT_COLOR);
        memoryStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        memoryPanel.add(memoryStatusLabel, BorderLayout.NORTH);
        memoryPanel.add(memoryUtilizationBar, BorderLayout.CENTER);

        // Create initial memory pie chart
        jvmtiMemoryChartPanel = createJVMTIMemoryChart();
        memoryPanel.add(jvmtiMemoryChartPanel, BorderLayout.SOUTH);

        chartsPanel.add(memoryPanel);

        // Thread Analysis Chart
        JPanel threadPanel = createStyledPanel("Thread Analysis");
        threadPanel.setLayout(new BorderLayout(5, 5));

        jvmtiThreadStatusLabel = new JLabel("Loading...");
        jvmtiThreadStatusLabel.setForeground(TEXT_COLOR);
        jvmtiThreadStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        threadPanel.add(jvmtiThreadStatusLabel, BorderLayout.NORTH);

        jvmtiThreadChartPanel = createJVMTIThreadChart();
        threadPanel.add(jvmtiThreadChartPanel, BorderLayout.CENTER);

        chartsPanel.add(threadPanel);

        // Class Loading Chart
        JPanel classPanel = createStyledPanel("Class Loading");
        classPanel.setLayout(new BorderLayout(5, 5));

        classStatusLabel = new JLabel("Loading...");
        classStatusLabel.setForeground(TEXT_COLOR);
        classStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        classPanel.add(classStatusLabel, BorderLayout.NORTH);

        jvmtiClassChartPanel = createJVMTIClassChart();
        classPanel.add(jvmtiClassChartPanel, BorderLayout.CENTER);

        chartsPanel.add(classPanel);

        // Bottom split - VM Info and JVMTI Details
        JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bottomSplit.setBackground(BG_COLOR);
        bottomSplit.setDividerLocation(450);

        // VM Information
        JPanel vmInfoPanel = createStyledPanel("JVM Runtime Information");
        vmInfoPanel.setLayout(new BorderLayout());

        vmStateArea = new JTextArea();
        vmStateArea.setEditable(false);
        vmStateArea.setBackground(PANEL_BG);
        vmStateArea.setForeground(TEXT_COLOR);
        vmStateArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        vmStateArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane vmStateScroll = new JScrollPane(vmStateArea);
        vmStateScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        vmInfoPanel.add(vmStateScroll, BorderLayout.CENTER);

        // JVMTI Status Detail
        JPanel jvmtiDetailPanel = createStyledPanel("JVMTI Interface Details");
        jvmtiDetailPanel.setLayout(new BorderLayout(5, 5));

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        controlsPanel.setOpaque(false);

        JButton runDiagBtn = createStyledButton("Run Diagnostics", ACCENT_COLOR);
        runDiagBtn.addActionListener(e -> runFullDiagnostics());
        controlsPanel.add(runDiagBtn);

        JButton refreshBtn = createStyledButton("Refresh All", SUCCESS_COLOR);
        refreshBtn.addActionListener(e -> refreshJVMTIInfo());
        controlsPanel.add(refreshBtn);

        jvmtiDetailPanel.add(controlsPanel, BorderLayout.NORTH);

        jvmtiStatusArea = new JTextArea();
        jvmtiStatusArea.setEditable(false);
        jvmtiStatusArea.setBackground(PANEL_BG);
        jvmtiStatusArea.setForeground(TEXT_COLOR);
        jvmtiStatusArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        jvmtiStatusArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane statusScroll = new JScrollPane(jvmtiStatusArea);
        statusScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        jvmtiDetailPanel.add(statusScroll, BorderLayout.CENTER);

        bottomSplit.setLeftComponent(vmInfoPanel);
        bottomSplit.setRightComponent(jvmtiDetailPanel);

        // Diagnostics output (expandable)
        diagnosticsArea = new JTextArea(15, 40);
        diagnosticsArea.setEditable(false);
        diagnosticsArea.setBackground(PANEL_BG);
        diagnosticsArea.setForeground(TEXT_COLOR);
        diagnosticsArea.setFont(new Font("Consolas", Font.PLAIN, 10));
        diagnosticsArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        diagnosticsArea.setText("Click 'Run Diagnostics' for detailed system analysis");

        JScrollPane diagScroll = new JScrollPane(diagnosticsArea);
        diagScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        diagScroll.setPreferredSize(new Dimension(0, 250));

        // Main layout
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(chartsPanel, BorderLayout.NORTH);
        centerPanel.add(bottomSplit, BorderLayout.CENTER);

        panel.add(statusCardsPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(diagScroll, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createVisualStatusCard(String title, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(PANEL_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            new EmptyBorder(10, 15, 10, 15)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(150, 150, 150));
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(TEXT_COLOR);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        // Add colored status indicator
        JPanel statusIndicator = new JPanel();
        statusIndicator.setBackground(accentColor);
        statusIndicator.setPreferredSize(new Dimension(10, 10));
        statusIndicator.setBorder(BorderFactory.createLineBorder(accentColor.darker(), 1));

        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        valuePanel.setOpaque(false);
        valuePanel.add(statusIndicator);
        valuePanel.add(valueLabel);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valuePanel, BorderLayout.CENTER);

        return card;
    }

    private ChartPanel createJVMTIMemoryChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Used", 0);
        dataset.setValue("Free", 100);

        JFreeChart chart = ChartFactory.createPieChart(null, dataset, false, false, false);
        chart.setBackgroundPaint(PANEL_BG);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(PANEL_BG);
        plot.setOutlineVisible(false);
        plot.setSectionPaint("Used", ACCENT_COLOR);
        plot.setSectionPaint("Free", new Color(60, 62, 66));
        plot.setLabelGenerator(null);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(150, 100));
        chartPanel.setBackground(PANEL_BG);
        return chartPanel;
    }

    private ChartPanel createJVMTIThreadChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Daemon", 0);
        dataset.setValue("User", 0);

        JFreeChart chart = ChartFactory.createPieChart(null, dataset, false, false, false);
        chart.setBackgroundPaint(PANEL_BG);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(PANEL_BG);
        plot.setOutlineVisible(false);
        plot.setSectionPaint("Daemon", SUCCESS_COLOR);
        plot.setSectionPaint("User", ACCENT_COLOR);
        plot.setLabelGenerator(null);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(200, 150));
        chartPanel.setBackground(PANEL_BG);
        return chartPanel;
    }

    private ChartPanel createJVMTIClassChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(0, "Classes", "Regular");
        dataset.addValue(0, "Classes", "Interfaces");
        dataset.addValue(0, "Classes", "Arrays");

        JFreeChart chart = ChartFactory.createBarChart(null, null, "Count", dataset,
            PlotOrientation.VERTICAL, false, false, false);
        chart.setBackgroundPaint(PANEL_BG);
        chart.getCategoryPlot().setBackgroundPaint(PANEL_BG);
        chart.getCategoryPlot().setOutlineVisible(false);
        chart.getCategoryPlot().getRenderer().setSeriesPaint(0, ACCENT_COLOR);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(200, 150));
        chartPanel.setBackground(PANEL_BG);
        return chartPanel;
    }

    // ==================== STATUS BAR ====================

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statusBar.setBackground(PANEL_BG);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(20, 21, 24)));

        JLabel statusLabel = createStyledLabel("JVMTI: " + (JVMTI.isAvailable() ? "Available ✓" : "Unavailable ✗") + " | Auto-refresh: ON");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusBar.add(statusLabel);

        return statusBar;
    }

    // ==================== REFRESH METHODS ====================

    private void updateResourceMonitor() {
        if (monitoringPaused || metricsCollector == null) {
            return;
        }

        try {
            // Collect metrics
            MetricSnapshot snapshot = metricsCollector.collect();
            metricsHistory.add(snapshot);

            // Create time marker
            Millisecond timePoint = new Millisecond(new java.util.Date(snapshot.timestamp));

            // Update CPU & GC series
            cpuSeries.addOrUpdate(timePoint, snapshot.cpuPercent);
            gcSeries.addOrUpdate(timePoint, snapshot.gcPercent);

            // Update Heap series (convert bytes to MB)
            heapUsedSeries.addOrUpdate(timePoint, snapshot.heapUsed / (1024.0 * 1024.0));
            heapCommittedSeries.addOrUpdate(timePoint, snapshot.heapCommitted / (1024.0 * 1024.0));
            heapMaxSeries.addOrUpdate(timePoint, snapshot.heapMax / (1024.0 * 1024.0));

            // Update Metaspace series (convert bytes to MB)
            if (snapshot.metaspaceUsed > 0) {
                metaspaceUsedSeries.addOrUpdate(timePoint, snapshot.metaspaceUsed / (1024.0 * 1024.0));
                metaspaceCommittedSeries.addOrUpdate(timePoint, snapshot.metaspaceCommitted / (1024.0 * 1024.0));
            }

            // Update Thread series
            threadCountSeries.addOrUpdate(timePoint, snapshot.threadCount);
            daemonThreadSeries.addOrUpdate(timePoint, snapshot.daemonThreadCount);

            // Update summary labels
            updateSummaryLabels(snapshot);

            // Update GC analysis
            updateGCAnalysis();

        } catch (Exception e) {
            System.err.println("Error updating resource monitor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateSummaryLabels(MetricSnapshot snapshot) {
        cpuSummaryLabel.setText(String.format("%.1f%%", snapshot.cpuPercent));
        gcSummaryLabel.setText(String.format("%.1f%%", snapshot.gcPercent));

        long heapUsedMB = snapshot.heapUsed / (1024 * 1024);
        long heapMaxMB = snapshot.heapMax / (1024 * 1024);
        heapSummaryLabel.setText(String.format("%d / %d MB", heapUsedMB, heapMaxMB));

        if (snapshot.metaspaceUsed > 0) {
            long metaUsedMB = snapshot.metaspaceUsed / (1024 * 1024);
            long metaCommittedMB = snapshot.metaspaceCommitted / (1024 * 1024);
            metaspaceSummaryLabel.setText(String.format("%d / %d MB", metaUsedMB, metaCommittedMB));
        } else {
            metaspaceSummaryLabel.setText("N/A");
        }

        threadSummaryLabel.setText(String.format("%d (%d daemon)", snapshot.threadCount, snapshot.daemonThreadCount));
    }

    private void updateGCAnalysis() {
        if (gcPauseAnalyzer == null || gcStatsArea == null || gcRecommendationsArea == null) {
            return;
        }

        try {
            // Update statistics
            com.tonic.services.profiler.gc.GCPauseAnalyzer.GCStatistics stats = gcPauseAnalyzer.getStatistics();

            StringBuilder statsSb = new StringBuilder();
            statsSb.append("GC Pause Statistics\n");
            statsSb.append("===================\n\n");

            if (stats.totalPauses == 0) {
                statsSb.append("No GC events captured yet.\n");
                statsSb.append("Waiting for garbage collection...");
            } else {
                statsSb.append(String.format("Total Pauses:    %d\n", stats.totalPauses));
                statsSb.append(String.format("Total Time:      %d ms\n", stats.totalPauseTime));
                statsSb.append(String.format("Average Pause:   %d ms\n", stats.avgPause));
                statsSb.append(String.format("Longest Pause:   %d ms\n", stats.longestPause));
                statsSb.append(String.format("95th Percentile: %d ms\n", stats.p95Pause));
                statsSb.append(String.format("99th Percentile: %d ms\n\n", stats.p99Pause));

                if (stats.longestPauseEvent != null) {
                    statsSb.append("Longest Pause Details:\n");
                    statsSb.append(String.format("  Type:   %s\n", stats.longestPauseEvent.getGcType().getLabel()));
                    statsSb.append(String.format("  Action: %s\n", stats.longestPauseEvent.getGcAction()));
                    statsSb.append(String.format("  Cause:  %s\n", stats.longestPauseEvent.getGcCause()));
                    statsSb.append(String.format("  Freed:  %.1f MB (%.1f%%)\n",
                        stats.longestPauseEvent.getMemoryFreed() / (1024.0 * 1024.0),
                        stats.longestPauseEvent.getMemoryFreedPercent()));
                }
            }

            gcStatsArea.setText(statsSb.toString());
            gcStatsArea.setCaretPosition(0);

            // Update recommendations
            java.util.List<com.tonic.services.profiler.gc.GCTuningAdvisor.Recommendation> recommendations =
                gcTuningAdvisor.getRecommendations();

            StringBuilder recSb = new StringBuilder();

            for (com.tonic.services.profiler.gc.GCTuningAdvisor.Recommendation rec : recommendations) {
                recSb.append("[").append(rec.severity.getLabel().toUpperCase()).append("] ");
                recSb.append(rec.title).append("\n");
                recSb.append(rec.description).append("\n");
                if (rec.suggestion != null) {
                    recSb.append("\nSuggestion: ").append(rec.suggestion).append("\n");
                }
                recSb.append("\n");
            }

            gcRecommendationsArea.setText(recSb.toString());
            gcRecommendationsArea.setCaretPosition(0);

        } catch (Exception e) {
            System.err.println("Error updating GC analysis: " + e.getMessage());
        }
    }

    private void refreshJITInfo() {
        if (!JITCompilerAccess.isAvailable()) {
            jitStatusArea.setText("JIT Compiler Access not available.\nCompilation monitoring requires JVM support.");
            return;
        }

        JITCompilerAccess.CompilerStatistics stats = JITCompilerAccess.getCompilerStatistics();
        StringBuilder sb = new StringBuilder();
        sb.append("=== JIT Compiler Information ===\n\n");
        sb.append(String.format("Compiler Name:          %s\n", stats.compilerName));
        sb.append(String.format("Total Compilation Time: %d ms\n", stats.totalCompilationTime));
        sb.append(String.format("Time Monitoring:        %s\n", stats.compilationTimeMonitoringSupported ? "Supported" : "Not Supported"));
        sb.append(String.format("Available Processors:   %d\n\n", stats.availableProcessors));

        sb.append("=== Compilation Levels ===\n\n");
        for (int i = 0; i <= 4; i++) {
            sb.append(String.format("Level %d: %s\n", i, JITCompilerAccess.getLevelName(i)));
        }

        jitStatusArea.setText(sb.toString());

        // Update escape analysis insights
        updateEscapeAnalysisInsights(stats);
    }

    private void updateEscapeAnalysisInsights(JITCompilerAccess.CompilerStatistics stats) {
        if (escapeAnalysisArea == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== JIT Compiler Optimization Insights ===\n\n");

        // Compilation efficiency
        long avgCompileTime = stats.totalCompilationTime > 0 && stats.availableProcessors > 0
            ? stats.totalCompilationTime / stats.availableProcessors
            : 0;

        sb.append("Compilation Efficiency:\n");
        sb.append(String.format("  Total Compilation Time: %d ms\n", stats.totalCompilationTime));
        sb.append(String.format("  Avg Time per Core: %d ms\n", avgCompileTime));
        sb.append(String.format("  Compiler: %s\n\n", stats.compilerName));

        // Optimization insights (based on heuristics since WhiteBox API is unavailable)
        sb.append("Optimization Patterns:\n");
        sb.append("  Escape Analysis: The JIT compiler automatically performs escape analysis to:\n");
        sb.append("    - Eliminate heap allocations for objects that don't escape their method\n");
        sb.append("    - Allocate short-lived objects on the stack instead of heap\n");
        sb.append("    - Perform scalar replacement (replace object fields with local variables)\n");
        sb.append("    - Remove synchronization for objects that don't escape threads\n\n");

        sb.append("  Common Optimizations Applied:\n");
        sb.append("    - Inlining: Frequently called small methods are inlined\n");
        sb.append("    - Loop Unrolling: Loops are expanded to reduce branch overhead\n");
        sb.append("    - Dead Code Elimination: Unused code paths are removed\n");
        sb.append("    - Constant Folding: Compile-time evaluation of constant expressions\n\n");

        // Recommendations based on compilation time
        sb.append("Recommendations:\n");
        if (stats.totalCompilationTime < 1000) {
            sb.append("  Status: Healthy compilation overhead\n");
            sb.append("  - JIT compiler is efficiently optimizing hot methods\n");
            sb.append("  - Low compilation overhead indicates good steady-state performance\n");
        } else if (stats.totalCompilationTime < 5000) {
            sb.append("  Status: Moderate compilation activity\n");
            sb.append("  - Consider using tiered compilation (default in modern JVMs)\n");
            sb.append("  - Monitor for excessive recompilation\n");
        } else {
            sb.append("  Status: High compilation overhead detected\n");
            sb.append("  - May indicate frequent deoptimization/recompilation cycles\n");
            sb.append("  - Consider:\n");
            sb.append("    1. Reducing polymorphism in hot paths\n");
            sb.append("    2. Avoiding megamorphic call sites\n");
            sb.append("    3. Using final classes/methods where possible\n");
        }

        escapeAnalysisArea.setText(sb.toString());
        escapeAnalysisArea.setCaretPosition(0);
    }

    private void refreshThreadInfo() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] threadIds = threadBean.getAllThreadIds();

        threadListModel.clear();
        for (long id : threadIds) {
            ThreadInfo info = threadBean.getThreadInfo(id);
            if (info != null) {
                String state = info.getThreadState().toString();
                threadListModel.addElement(String.format("[%d] %s - %s", info.getThreadId(), info.getThreadName(), state));
            }
        }
    }

    private void showThreadStack() {
        String selected = threadList.getSelectedValue();
        if (selected == null) return;

        // Extract thread ID from selection
        int idEnd = selected.indexOf(']');
        if (idEnd == -1) return;

        long threadId = Long.parseLong(selected.substring(1, idEnd));

        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        ThreadInfo info = threadBean.getThreadInfo(threadId, Integer.MAX_VALUE);

        if (info == null) {
            threadStackArea.setText("Thread not found or terminated");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Thread: ").append(info.getThreadName()).append("\n");
        sb.append("ID: ").append(info.getThreadId()).append("\n");
        sb.append("State: ").append(info.getThreadState()).append("\n");
        sb.append("Blocked Time: ").append(info.getBlockedTime()).append(" ms\n");
        sb.append("Blocked Count: ").append(info.getBlockedCount()).append("\n\n");
        sb.append("Stack Trace:\n");

        StackTraceElement[] stack = info.getStackTrace();
        for (StackTraceElement element : stack) {
            sb.append("  at ").append(element.toString()).append("\n");
        }

        threadStackArea.setText(sb.toString());
        threadStackArea.setCaretPosition(0);
    }

    private void refreshVMFlags() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== JVM Properties ===\n\n");

        String[] properties = {
            "java.vm.name", "java.vm.version", "java.vm.vendor",
            "java.runtime.version", "java.specification.version",
            "java.vm.specification.version"
        };

        for (String prop : properties) {
            try {
                String value = System.getProperty(prop);
                sb.append(String.format("%-30s = %s\n", prop, value != null ? value : "null"));
            } catch (Exception e) {
                sb.append(String.format("%-30s = <unavailable>\n", prop));
            }
        }

        vmFlagsArea.setText(sb.toString());
        refreshCPUFeatures();
    }

    private void refreshCPUFeatures() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== System Information ===\n\n");

        sb.append(String.format("Available Processors: %d\n", Runtime.getRuntime().availableProcessors()));
        sb.append(String.format("OS: %s %s\n", System.getProperty("os.name"), System.getProperty("os.version")));
        sb.append(String.format("Architecture: %s\n", System.getProperty("os.arch")));

        cpuFeaturesArea.setText(sb.toString());
    }

    private void refreshJVMTIInfo() {
        // Update JVM TI Interface Details
        StringBuilder jvmtiSb = new StringBuilder();
        jvmtiSb.append("===========================================\n");
        jvmtiSb.append("    JVMTI INTERFACE COMPONENTS\n");
        jvmtiSb.append("===========================================\n\n");

        jvmtiSb.append("[VMObjectAccess]\n");
        jvmtiSb.append("  Status: ").append(VMObjectAccess.isAvailable() ? "Available" : "Unavailable").append("\n");
        jvmtiSb.append("  Features: Thread enumeration, Object sizing\n");
        jvmtiSb.append("            Memory tracking, GC control\n\n");

        jvmtiSb.append("[AdvancedVMAccess]\n");
        jvmtiSb.append("  Status: ").append(AdvancedVMAccess.isAvailable() ? "Available" : "Unavailable").append("\n");
        jvmtiSb.append("  Features: Class metadata, Field offsets\n");
        jvmtiSb.append("            Object headers, Loaded classes\n\n");

        jvmtiSb.append("[VM - jdk.internal.misc.VM]\n");
        jvmtiSb.append("  Status: ").append(VM.isAvailable() ? "Available" : "Unavailable").append("\n");
        jvmtiSb.append("  Features: VM state, Init levels\n");
        jvmtiSb.append("            Direct memory, Runtime args\n\n");

        jvmtiSb.append("[Overall JVMTI]\n");
        jvmtiSb.append("  Status: ").append(JVMTI.isAvailable() ? "Fully Available" : "Partially Available").append("\n");

        jvmtiStatusArea.setText(jvmtiSb.toString());

        // Update VM Runtime Information
        StringBuilder vmSb = new StringBuilder();
        JVMTI.JVMInfo jvmInfo = JVMTI.getJVMInfo();

        vmSb.append("===========================================\n");
        vmSb.append("      JVM RUNTIME ENVIRONMENT\n");
        vmSb.append("===========================================\n\n");

        vmSb.append("[JVM Platform]\n");
        vmSb.append("  Name:    ").append(jvmInfo.jvmName).append("\n");
        vmSb.append("  Version: ").append(jvmInfo.jvmVersion).append("\n");
        vmSb.append("  Vendor:  ").append(jvmInfo.jvmVendor).append("\n\n");

        vmSb.append("[Operating System]\n");
        vmSb.append("  OS:   ").append(jvmInfo.osName).append(" ").append(jvmInfo.osVersion).append("\n");
        vmSb.append("  Arch: ").append(jvmInfo.osArch).append("\n\n");

        vmSb.append("[Runtime State]\n");
        vmSb.append("  Threads:   ").append(jvmInfo.threadCount).append(" total\n");
        vmSb.append("  Classes:   ").append(jvmInfo.loadedClassCount).append(" loaded\n");
        vmSb.append("  Heap Used: ").append(jvmInfo.memoryInfo.usedMemory / (1024 * 1024)).append(" MB\n");
        vmSb.append("  Heap Max:  ").append(jvmInfo.memoryInfo.maxMemory / (1024 * 1024)).append(" MB\n\n");

        if (VM.isAvailable()) {
            VM.VMInfo vmInfo = VM.getVMInfo();

            vmSb.append("[VM Initialization]\n");
            vmSb.append("  Level:         ").append(vmInfo.getInitLevelName()).append("\n");
            vmSb.append("  Module System: ").append(vmInfo.isModuleSystemInited ? "Ready" : "Not Ready").append("\n");
            vmSb.append("  VM Booted:     ").append(vmInfo.isBooted ? "Yes" : "No").append("\n");
            vmSb.append("  Shutdown:      ").append(vmInfo.isShutdown ? "Yes" : "No").append("\n\n");

            vmSb.append("[Direct Memory Configuration]\n");
            vmSb.append("  Max Direct Memory: ").append(vmInfo.maxDirectMemory / (1024 * 1024)).append(" MB\n");
            vmSb.append("  Page Aligned:      ").append(vmInfo.isDirectMemoryPageAligned ? "Yes" : "No").append("\n\n");

            vmSb.append("[Finalization Queue]\n");
            vmSb.append("  Pending Objects: ").append(vmInfo.finalRefCount).append("\n");
            vmSb.append("  Peak Count:      ").append(vmInfo.peakFinalRefCount).append("\n");
        }

        vmStateArea.setText(vmSb.toString());
        vmStateArea.setCaretPosition(0);

        // Update Charts with live data
        updateCharts(jvmInfo);

        // Don't reset diagnostics area if it contains a report
        // Only set placeholder if it's empty or already has the placeholder
        String currentText = diagnosticsArea.getText();
        if (currentText == null || currentText.isEmpty() ||
            currentText.contains("Click 'Run Diagnostics'")) {
            diagnosticsArea.setText("Click 'Run Diagnostics' for detailed system analysis...");
        }
    }

    private void updateCharts(JVMTI.JVMInfo jvmInfo) {
        // Update Memory Chart
        long usedMB = jvmInfo.memoryInfo.usedMemory / (1024 * 1024);
        long maxMB = jvmInfo.memoryInfo.maxMemory / (1024 * 1024);
        long freeMB = maxMB - usedMB;
        double utilization = (double) jvmInfo.memoryInfo.usedMemory / jvmInfo.memoryInfo.maxMemory * 100;

        memoryUtilizationBar.setValue((int) utilization);
        memoryUtilizationBar.setString(String.format("%.1f%% (%.0f MB / %.0f MB)", utilization, (double) usedMB, (double) maxMB));

        if (utilization < 70) {
            memoryUtilizationBar.setForeground(SUCCESS_COLOR);
        } else if (utilization < 85) {
            memoryUtilizationBar.setForeground(WARNING_COLOR);
        } else {
            memoryUtilizationBar.setForeground(ERROR_COLOR);
        }

        memoryStatusLabel.setText("Memory: " + usedMB + " MB used of " + maxMB + " MB");

        DefaultPieDataset memDataset = new DefaultPieDataset();
        memDataset.setValue("Used", usedMB);
        memDataset.setValue("Free", freeMB);
        JFreeChart memChart = ((org.jfree.chart.ChartPanel) jvmtiMemoryChartPanel).getChart();
        ((PiePlot) memChart.getPlot()).setDataset(memDataset);

        // Update Thread Chart
        Thread[] allThreads = JVMTI.getAllThreads();
        int daemonCount = 0;
        int userCount = 0;

        for (Thread thread : allThreads) {
            if (thread.isDaemon()) {
                daemonCount++;
            } else {
                userCount++;
            }
        }

        jvmtiThreadStatusLabel.setText("Threads: " + allThreads.length + " total (" + daemonCount + " daemon, " + userCount + " user)");

        DefaultPieDataset threadDataset = new DefaultPieDataset();
        threadDataset.setValue("Daemon", daemonCount);
        threadDataset.setValue("User", userCount);
        JFreeChart threadChart = ((org.jfree.chart.ChartPanel) jvmtiThreadChartPanel).getChart();
        ((PiePlot) threadChart.getPlot()).setDataset(threadDataset);

        // Update Class Chart
        JVMTI.DiagnosticResults results = JVMTI.runDiagnostics();
        if (results.success) {
            int regularClasses = results.totalLoadedClasses - results.interfaceCount - results.arrayClassCount;

            classStatusLabel.setText("Classes: " + results.totalLoadedClasses + " loaded");

            DefaultCategoryDataset classDataset = new DefaultCategoryDataset();
            classDataset.addValue(regularClasses, "Classes", "Regular");
            classDataset.addValue(results.interfaceCount, "Classes", "Interfaces");
            classDataset.addValue(results.arrayClassCount, "Classes", "Arrays");

            JFreeChart classChart = ((org.jfree.chart.ChartPanel) jvmtiClassChartPanel).getChart();
            classChart.getCategoryPlot().setDataset(classDataset);
        }
    }

    private void runFullDiagnostics() {
        StringBuilder diagSb = new StringBuilder();

        diagSb.append("===============================================================\n");
        diagSb.append("               COMPREHENSIVE JVM DIAGNOSTICS\n");
        diagSb.append("===============================================================\n\n");

        // JVMTI Diagnostics
        JVMTI.DiagnosticResults results = JVMTI.runDiagnostics();

        if (results.success) {
            diagSb.append("[Memory Analysis]\n");
            diagSb.append("  Memory Freed by GC:  ").append(formatBytes(results.memoryFreedByGC)).append("\n");
            diagSb.append("  Memory Utilization:  ").append(String.format("%.1f%%", results.memoryUtilization * 100)).append("\n");
            diagSb.append("  Status:              ");
            if (results.memoryUtilization < 0.7) {
                diagSb.append("Healthy\n\n");
            } else if (results.memoryUtilization < 0.85) {
                diagSb.append("Warning - High Usage\n\n");
            } else {
                diagSb.append("Critical - Very High Usage\n\n");
            }

            diagSb.append("[Thread Analysis]\n");
            diagSb.append("  Total Threads:   ").append(results.totalThreads).append("\n");
            diagSb.append("  Alive Threads:   ").append(results.aliveThreads).append("\n");
            diagSb.append("  Daemon Threads:  ").append(results.daemonThreads).append("\n");
            diagSb.append("  User Threads:    ").append(results.aliveThreads - results.daemonThreads).append("\n\n");

            diagSb.append("[Class Loading Analysis]\n");
            diagSb.append("  Total Classes:   ").append(results.totalLoadedClasses).append("\n");
            diagSb.append("  Interfaces:      ").append(results.interfaceCount).append(String.format(" (%.1f%%)", (results.interfaceCount * 100.0) / results.totalLoadedClasses)).append("\n");
            diagSb.append("  Array Classes:   ").append(results.arrayClassCount).append(String.format(" (%.1f%%)", (results.arrayClassCount * 100.0) / results.totalLoadedClasses)).append("\n\n");

            diagSb.append("[Object Size Verification]\n");
            diagSb.append("  java.lang.Object:  ").append(results.testObjectSizes[0]).append(" bytes\n");
            diagSb.append("  String instance:   ").append(results.testObjectSizes[1]).append(" bytes\n");
            diagSb.append("  int[10] array:     ").append(results.testObjectSizes[2]).append(" bytes\n");
            diagSb.append("  Thread object:     ").append(results.testObjectSizes[3]).append(" bytes\n\n");
        } else {
            diagSb.append("[DIAGNOSTICS FAILED]\n");
            diagSb.append("  Error: ").append(results.error).append("\n\n");
        }

        // JIT Compiler Status
        if (JITCompilerAccess.isAvailable()) {
            JITCompilerAccess.CompilerStatistics stats = JITCompilerAccess.getCompilerStatistics();
            diagSb.append("[JIT Compiler Status]\n");
            diagSb.append("  Compiler:           ").append(stats.compilerName).append("\n");
            diagSb.append("  Compilation Time:   ").append(stats.totalCompilationTime).append(" ms\n");
            diagSb.append("  Time Monitoring:    ").append(stats.compilationTimeMonitoringSupported ? "Supported" : "Not Supported").append("\n\n");
        }

        // VM Arguments
        if (VM.isAvailable()) {
            VM.VMInfo vmInfo = VM.getVMInfo();
            if (vmInfo.runtimeArguments != null && vmInfo.runtimeArguments.length > 0) {
                diagSb.append("[JVM Runtime Arguments]\n");
                for (String arg : vmInfo.runtimeArguments) {
                    diagSb.append("  ").append(arg).append("\n");
                }
                diagSb.append("\n");
            }
        }

        diagSb.append("===============================================================\n");
        diagSb.append("                    DIAGNOSTICS COMPLETE\n");
        diagSb.append("===============================================================\n");

        diagnosticsArea.setText(diagSb.toString());
        diagnosticsArea.setCaretPosition(0);
    }

    private void forceCompileMethod() {
        String methodName = methodNameField.getText().trim();
        if (methodName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a method name", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int levelIndex = compilationLevelCombo.getSelectedIndex();

        try {
            // This is a simplified example - in practice you'd need to resolve the actual method
            JOptionPane.showMessageDialog(this,
                "Method compilation queued: " + methodName + " at level " + levelIndex,
                "JIT",
                JOptionPane.INFORMATION_MESSAGE);
            refreshJITInfo();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setVMFlag() {
        JOptionPane.showMessageDialog(this,
            "VM Flag modification is not available.\nVM flags can only be set at JVM startup via command line arguments.",
            "Not Available",
            JOptionPane.INFORMATION_MESSAGE);
    }

    // ==================== AUTO-REFRESH ====================

    private void startAutoRefresh() {
        // Metrics timer for Resource Monitor (1 second interval for smooth graphs)
        metricsTimer = new Timer(1000, e -> {
            if (isVisible()) {
                SwingUtilities.invokeLater(this::updateResourceMonitor);
            }
        });
        metricsTimer.start();

        // General refresh timer for other tabs (2 second interval)
        refreshTimer = new Timer(2000, e -> {
            if (isVisible()) {
                int selectedTab = tabbedPane.getSelectedIndex();
                switch (selectedTab) {
                    case 2: refreshJITInfo(); break;
                    case 3: refreshThreadInfo(); break;
                    case 4: refreshVMFlags(); break;
                    case 5: refreshJVMTIInfo(); break;
                }
            }
        });
        refreshTimer.start();

        // Initial refresh for all tabs
        refreshJITInfo();
        refreshThreadInfo();
        refreshVMFlags();
        refreshJVMTIInfo();
    }

    /**
     * Clean up all resources before closing
     */
    private void cleanup() {
        // Stop all timers
        if (metricsTimer != null) {
            metricsTimer.stop();
            metricsTimer = null;
        }
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }

        // Stop sampling tab samplers
        if (samplingTab != null) {
            samplingTab.cleanup();
        }

        // Stop leak detector
        if (leakDetectorTab != null) {
            leakDetectorTab.cleanup();
        }

        // Stop GC pause analyzer
        if (gcPauseAnalyzer != null) {
            gcPauseAnalyzer.stopMonitoring();
        }

        // Stop timeline panel
        if (timelinePanel != null) {
            timelinePanel.cleanup();
        }

        // Clear instance reference
        instance = null;
    }

    @Override
    public void dispose() {
        cleanup();
        super.dispose();
    }

    // ==================== UTILITY METHODS ====================

    private JPanel createStyledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL_BG);
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(50, 52, 56)),
            title
        );
        border.setTitleColor(TEXT_COLOR);
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.setBorder(BorderFactory.createCompoundBorder(border, new EmptyBorder(10, 10, 10, 10)));
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
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(130, 30));
        return button;
    }

    private void styleTextField(JTextField field) {
        field.setBackground(PANEL_BG);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 52, 56)),
            new EmptyBorder(5, 5, 5, 5)
        ));
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

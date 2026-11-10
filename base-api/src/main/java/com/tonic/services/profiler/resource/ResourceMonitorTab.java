package com.tonic.services.profiler.resource;

import com.tonic.services.profiler.CircularBuffer;
import com.tonic.services.profiler.MetricSnapshot;
import com.tonic.services.profiler.ProfilerTab;
import com.tonic.services.profiler.ResourceMetricsCollector;
import com.tonic.services.profiler.gc.FrameImpact;
import com.tonic.services.profiler.gc.GCPauseAnalyzer;
import com.tonic.services.profiler.gc.GCPauseEvent;
import com.tonic.util.ThreadPool;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.tonic.services.profiler.ProfilerTheme.*;

/**
 * Live CPU, GC, heap, metaspace and thread charts with GC pause statistics and their frame impact
 */
public final class ResourceMonitorTab implements ProfilerTab {
    private static final double MEGABYTE = 1024.0 * 1024.0;
    private static final long IMPACT_WINDOW_MS = 60_000;
    private static final int[] WINDOW_SECONDS = {60, 120, 300, 600};
    private static final Color YELLOW = new Color(255, 193, 7);
    private static final Color PURPLE = new Color(156, 39, 176);
    private static final Color DEEP_PURPLE = new Color(103, 58, 183);
    private static final Color CYAN = new Color(0, 188, 212);
    private static final Color PINK = new Color(233, 30, 99);

    private final ResourceMetricsCollector collector = new ResourceMetricsCollector();
    private final GCPauseAnalyzer pauses = new GCPauseAnalyzer(500);
    private final TimeSeries cpu = new TimeSeries("CPU %");
    private final TimeSeries gc = new TimeSeries("GC %");
    private final TimeSeries heapUsed = new TimeSeries("Used");
    private final TimeSeries heapCommitted = new TimeSeries("Committed");
    private final TimeSeries heapMax = new TimeSeries("Max");
    private final TimeSeries metaspaceUsed = new TimeSeries("Used");
    private final TimeSeries metaspaceCommitted = new TimeSeries("Committed");
    private final TimeSeries threads = new TimeSeries("Total");
    private final TimeSeries daemonThreads = new TimeSeries("Daemon");
    private final TimeSeries[] allSeries = {cpu, gc, heapUsed, heapCommitted, heapMax, metaspaceUsed, metaspaceCommitted, threads, daemonThreads};
    private final JLabel cpuLabel = summaryLabel("--%");
    private final JLabel gcLabel = summaryLabel("--%");
    private final JLabel heapLabel = summaryLabel("-- MB");
    private final JLabel metaspaceLabel = summaryLabel("-- MB");
    private final JLabel threadLabel = summaryLabel("--");
    private final JTextArea pauseStats = textArea(MONO);
    private final JTextArea impact = textArea(MONO);
    private final JPanel panel;

    private CircularBuffer<MetricSnapshot> history;
    private int windowSeconds = 120;
    private boolean paused;

    /**
     * Creates the tab and starts listening for GC pauses
     */
    public ResourceMonitorTab() {
        history = new CircularBuffer<>(windowSeconds);
        setMaximumAge();
        pauses.startMonitoring();

        panel = page(new BorderLayout(10, 10));
        panel.add(controls(), BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(2, 2, 10, 10));
        charts.setOpaque(false);
        ChartPanel cpuChart = timeChart("CPU & GC Activity", "Percentage (%)", dataset(cpu, gc), ACCENT, ERROR);
        cpuChart.getChart().getXYPlot().getRangeAxis().setRange(0, 100);
        ChartPanel threadChart = timeChart("Thread Count", "Threads", dataset(threads, daemonThreads), CYAN, PINK);
        threadChart.getChart().getXYPlot().getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        charts.add(cpuChart);
        charts.add(timeChart("Heap Memory", "Memory (MB)", dataset(heapUsed, heapCommitted, heapMax), YELLOW, ACCENT, SUCCESS));
        charts.add(timeChart("Metaspace", "Memory (MB)", dataset(metaspaceUsed, metaspaceCommitted), PURPLE, DEEP_PURPLE));
        charts.add(threadChart);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(charts, BorderLayout.CENTER);
        center.add(pausePanel(), BorderLayout.SOUTH);
        panel.add(center, BorderLayout.CENTER);
        panel.add(summary(), BorderLayout.SOUTH);
    }

    @Override
    public String title() {
        return "Resource Monitor";
    }

    @Override
    public JComponent component() {
        return panel;
    }

    /**
     * Takes one metrics sample and updates the charts, called once a second while the window is open
     */
    public void collect() {
        if (paused) {
            return;
        }
        MetricSnapshot snapshot = collector.collect();
        history.add(snapshot);

        Millisecond time = new Millisecond(new Date(snapshot.timestamp));
        cpu.addOrUpdate(time, snapshot.cpuPercent);
        gc.addOrUpdate(time, snapshot.gcPercent);
        heapUsed.addOrUpdate(time, snapshot.heapUsed / MEGABYTE);
        heapCommitted.addOrUpdate(time, snapshot.heapCommitted / MEGABYTE);
        heapMax.addOrUpdate(time, snapshot.heapMax / MEGABYTE);
        if (snapshot.metaspaceUsed > 0) {
            metaspaceUsed.addOrUpdate(time, snapshot.metaspaceUsed / MEGABYTE);
            metaspaceCommitted.addOrUpdate(time, snapshot.metaspaceCommitted / MEGABYTE);
        }
        threads.addOrUpdate(time, snapshot.threadCount);
        daemonThreads.addOrUpdate(time, snapshot.daemonThreadCount);

        updateSummary(snapshot);
        updatePauses();
    }

    @Override
    public void dispose() {
        pauses.stopMonitoring();
    }

    private JPanel controls() {
        JPanel controls = section("Monitoring Controls");
        controls.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton pause = button("Pause", WARNING);
        pause.addActionListener(e -> {
            paused = !paused;
            pause.setText(paused ? "Resume" : "Pause");
            pause.setBackground(paused ? SUCCESS : WARNING);
            if (!paused) {
                collector.resetTracking();
            }
        });
        controls.add(pause);

        controls.add(label("Time Window:"));
        JComboBox<String> window = new JComboBox<>(new String[]{"1 Minute", "2 Minutes", "5 Minutes", "10 Minutes"});
        window.setBackground(PANEL);
        window.setForeground(TEXT);
        window.setSelectedIndex(1);
        window.addActionListener(e -> resize(WINDOW_SECONDS[window.getSelectedIndex()]));
        controls.add(window);

        JButton export = button("Export CSV", ACCENT);
        export.addActionListener(e -> export());
        controls.add(export);

        JButton forceGc = button("Force GC", ERROR);
        forceGc.addActionListener(e -> ThreadPool.submit(System::gc));
        controls.add(forceGc);

        JButton clear = button("Clear", NEUTRAL);
        clear.addActionListener(e -> {
            for (TimeSeries series : allSeries) {
                series.clear();
            }
            history.clear();
        });
        controls.add(clear);
        return controls;
    }

    private JPanel pausePanel() {
        JPanel pausePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        pausePanel.setOpaque(false);
        pausePanel.setPreferredSize(new Dimension(0, 180));

        JPanel stats = section("GC Pause Statistics");
        stats.setLayout(new BorderLayout());
        pauseStats.setText("Collecting GC data...");
        stats.add(scroll(pauseStats), BorderLayout.CENTER);

        JPanel frames = section("Frame Impact (last 60 s)");
        frames.setLayout(new BorderLayout());
        frames.add(scroll(impact), BorderLayout.CENTER);

        pausePanel.add(stats);
        pausePanel.add(frames);
        return pausePanel;
    }

    private JPanel summary() {
        JPanel summary = new JPanel(new GridLayout(1, 5, 10, 0));
        summary.setOpaque(false);
        summary.setPreferredSize(new Dimension(0, 80));
        summary.add(card("CPU Usage", cpuLabel, ACCENT));
        summary.add(card("GC Activity", gcLabel, ERROR));
        summary.add(card("Heap Memory", heapLabel, YELLOW));
        summary.add(card("Metaspace", metaspaceLabel, PURPLE));
        summary.add(card("Threads", threadLabel, CYAN));
        return summary;
    }

    private void updateSummary(MetricSnapshot snapshot) {
        cpuLabel.setText(String.format("%.1f%%", snapshot.cpuPercent));
        gcLabel.setText(String.format("%.1f%%", snapshot.gcPercent));
        heapLabel.setText(String.format("%d / %d MB", snapshot.heapUsed >> 20, snapshot.heapMax >> 20));
        metaspaceLabel.setText(snapshot.metaspaceUsed > 0 ? String.format("%d / %d MB", snapshot.metaspaceUsed >> 20, snapshot.metaspaceCommitted >> 20) : "N/A");
        threadLabel.setText(String.format("%d (%d daemon)", snapshot.threadCount, snapshot.daemonThreadCount));
    }

    private void updatePauses() {
        GCPauseAnalyzer.GCStatistics stats = pauses.getStatistics();
        StringBuilder text = new StringBuilder();
        if (stats.pauses == 0) {
            text.append("No GC pauses yet.");
        } else {
            text.append(stats.pauses == 1 ? "Last pause\n\n" : String.format("Last %d pauses\n\n", stats.pauses));
            text.append(String.format("Total Time:      %d ms\n", stats.totalPauseTime));
            text.append(String.format("Average Pause:   %d ms\n", stats.averagePause));
            text.append(String.format("95th Percentile: %d ms\n", stats.p95Pause));
            text.append(String.format("99th Percentile: %d ms\n", stats.p99Pause));
            GCPauseEvent longest = stats.longest;
            text.append(String.format("Longest Pause:   %d ms\n", longest.getDuration()));
            text.append(String.format("  %s, %s, %s\n", longest.getGcName(), longest.getGcAction(), longest.getGcCause()));
            text.append(String.format("  Freed %.1f MB (%.1f%% of the heap in use)\n", longest.getMemoryFreed() / MEGABYTE, longest.getMemoryFreedPercent()));
        }
        setText(pauseStats, text.toString());

        FrameImpact frames = FrameImpact.of(pauses.getEvents(), System.currentTimeMillis(), IMPACT_WINDOW_MS);
        StringBuilder impactText = new StringBuilder();
        impactText.append(String.format("Pauses:            %d\n", frames.pauses));
        impactText.append(String.format("Over %d ms (60 FPS): %d\n", FrameImpact.FRAME_60_FPS, frames.over60Fps));
        impactText.append(String.format("Over %d ms (30 FPS): %d\n", FrameImpact.FRAME_30_FPS, frames.over30Fps));
        impactText.append(String.format("Longest:           %d ms\n", frames.longestMs));
        impactText.append(String.format("GC time:           %.0f ms/min\n", frames.msPerMinute()));
        setText(impact, impactText.toString());
    }

    private void resize(int seconds) {
        windowSeconds = seconds;
        setMaximumAge();
        CircularBuffer<MetricSnapshot> resized = new CircularBuffer<>(seconds);
        for (MetricSnapshot snapshot : history.getAll()) {
            resized.add(snapshot);
        }
        history = resized;
    }

    private void setMaximumAge() {
        for (TimeSeries series : allSeries) {
            series.setMaximumItemAge(windowSeconds * 1000L);
        }
    }

    private void export() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Metrics to CSV");
        chooser.setSelectedFile(new File("metrics_" + System.currentTimeMillis() + ".csv"));
        if (chooser.showSaveDialog(panel) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        List<MetricSnapshot> snapshots = history.getAll();
        try (PrintWriter writer = new PrintWriter(chooser.getSelectedFile())) {
            writer.println("Timestamp,CPU%,GC%,HeapUsed(MB),HeapCommitted(MB),HeapMax(MB),MetaspaceUsed(MB),MetaspaceCommitted(MB),Threads,DaemonThreads");
            for (MetricSnapshot snapshot : snapshots) {
                writer.printf(Locale.ROOT, "%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d%n", snapshot.timestamp, snapshot.cpuPercent, snapshot.gcPercent, snapshot.heapUsed / MEGABYTE, snapshot.heapCommitted / MEGABYTE, snapshot.heapMax / MEGABYTE, snapshot.metaspaceUsed / MEGABYTE, snapshot.metaspaceCommitted / MEGABYTE, snapshot.threadCount, snapshot.daemonThreadCount);
            }
            JOptionPane.showMessageDialog(panel, "Metrics exported successfully!", "Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(panel, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static TimeSeriesCollection dataset(TimeSeries... series) {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (TimeSeries each : series) {
            dataset.addSeries(each);
        }
        return dataset;
    }

    private static JLabel summaryLabel(String text) {
        JLabel label = label(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return label;
    }

    private static JPanel card(String title, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(accent, 2), new EmptyBorder(8, 10, 8, 10)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(MUTED);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        value.setForeground(TEXT);
        value.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }
}

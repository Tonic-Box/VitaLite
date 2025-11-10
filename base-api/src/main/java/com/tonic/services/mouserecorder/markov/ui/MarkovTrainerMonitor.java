package com.tonic.services.mouserecorder.markov.ui;

import com.tonic.model.ui.components.VitaFrame;
import com.tonic.services.mouserecorder.markov.MarkovChainData;
import com.tonic.services.mouserecorder.markov.MarkovMouseTrainer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Date;

/**
 * Real-time monitoring dashboard for Markov Mouse Trainer.
 * Singleton window that displays statistics, graphs, and visualizations.
 */
public class MarkovTrainerMonitor extends VitaFrame
{
    private static MarkovTrainerMonitor instance;
    private final MarkovMouseTrainer trainer;

    // UI Components
    private JLabel statusLabel;
    private JLabel transitionsLabel;
    private JLabel statesLabel;
    private JLabel samplesLabel;
    private JLabel recordingRateLabel;
    private JLabel qualityLabel;
    private JProgressBar qualityBar;
    private JLabel persistenceLabel;
    private JButton startStopButton;

    // Visualization panels
    private TimeSeries transitionSeries;
    private TimeSeries sampleRateSeries;
    private LiveMousePanel liveMousePanel;
    private StateSpacePanel stateSpacePanel;

    // Update timers
    private Timer statsTimer;
    private Timer liveTrackingTimer;

    private long lastSampleCount = 0;
    private long lastUpdateTime = System.currentTimeMillis();

    private MarkovTrainerMonitor(MarkovMouseTrainer trainer)
    {
        super("Markov Mouse Trainer - Monitoring Dashboard");
        this.trainer = trainer;

        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        initializeUI();
        startTimers();
    }

    /**
     * Gets or creates the singleton instance.
     */
    public static synchronized MarkovTrainerMonitor getInstance(MarkovMouseTrainer trainer)
    {
        if (instance == null)
        {
            instance = new MarkovTrainerMonitor(trainer);
        }
        return instance;
    }

    /**
     * Shows the monitor window.
     */
    public static void show(MarkovMouseTrainer trainer)
    {
        MarkovTrainerMonitor monitor = getInstance(trainer);
        monitor.setVisible(true);
        monitor.toFront();
    }

    /**
     * Hides the monitor window.
     */
    public static void hideMonitor()
    {
        if (instance != null)
        {
            instance.setVisible(false);
        }
    }

    /**
     * Toggles monitor visibility.
     */
    public static void toggleMonitor(MarkovMouseTrainer trainer)
    {
        MarkovTrainerMonitor monitor = getInstance(trainer);
        if (monitor.isVisible())
        {
            monitor.setVisible(false);
        }
        else
        {
            monitor.setVisible(true);
            monitor.toFront();
        }
    }

    /**
     * Checks if monitor is currently visible.
     */
    public static boolean isMonitorVisible()
    {
        return instance != null && instance.isVisible();
    }

    private void initializeUI()
    {
        getContentPanel().setLayout(new BorderLayout(10, 10));

        JPanel controlPanel = createControlPanel();
        addToContent(controlPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(250);
        splitPane.setOpaque(false);

        JPanel statsPanel = createStatsPanel();
        splitPane.setLeftComponent(statsPanel);

        JTabbedPane tabbedPane = createVisualizationTabs();
        splitPane.setRightComponent(tabbedPane);

        addToContent(splitPane, BorderLayout.CENTER);

        JPanel statusBar = createStatusBar();
        addToContent(statusBar, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel()
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(false);

        startStopButton = new JButton("Start Recording");
        startStopButton.addActionListener(e -> toggleTraining());
        panel.add(startStopButton);

        JButton forceSaveButton = new JButton("Force Save");
        forceSaveButton.addActionListener(e -> {
            trainer.forceSave();
            JOptionPane.showMessageDialog(this, "Model saved to disk!", "Save Complete", JOptionPane.INFORMATION_MESSAGE);
        });
        panel.add(forceSaveButton);

        JButton exportButton = new JButton("Export JSON");
        exportButton.addActionListener(e -> exportToJSON());
        panel.add(exportButton);

        JButton clearDataButton = new JButton("Clear Data");
        clearDataButton.setForeground(new Color(255, 100, 100));
        clearDataButton.addActionListener(e -> clearTrainingData());
        panel.add(clearDataButton);

        return panel;
    }

    private JPanel createStatsPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(60, 62, 66)),
            "Statistics",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(200, 200, 200)
        ));
        panel.setBackground(new Color(30, 31, 34));

        // Status
        statusLabel = createStatLabel("Status: Idle");
        statusLabel.setForeground(new Color(255, 200, 0));
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(15));

        // Quality section
        JPanel qualityPanel = new JPanel();
        qualityPanel.setLayout(new BoxLayout(qualityPanel, BoxLayout.Y_AXIS));
        qualityPanel.setOpaque(false);
        qualityPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        qualityLabel = createStatLabel("Quality: Minimum");
        qualityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        qualityPanel.add(qualityLabel);
        qualityPanel.add(Box.createVerticalStrut(5));

        qualityBar = new JProgressBar(0, 100);
        qualityBar.setValue(0);
        qualityBar.setStringPainted(true);
        qualityBar.setPreferredSize(new Dimension(220, 25));
        qualityBar.setMaximumSize(new Dimension(220, 25));
        qualityBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        qualityBar.setForeground(new Color(100, 150, 255));
        qualityPanel.add(qualityBar);

        panel.add(qualityPanel);
        panel.add(Box.createVerticalStrut(15));

        transitionsLabel = createStatLabel("Transitions: 0");
        panel.add(transitionsLabel);
        panel.add(Box.createVerticalStrut(10));

        statesLabel = createStatLabel("Unique States: 0");
        panel.add(statesLabel);
        panel.add(Box.createVerticalStrut(10));

        samplesLabel = createStatLabel("Samples: 0");
        panel.add(samplesLabel);
        panel.add(Box.createVerticalStrut(10));

        recordingRateLabel = createStatLabel("Recording Rate: 0 Hz");
        panel.add(recordingRateLabel);

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JLabel createStatLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(200, 200, 200));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(2, 10, 2, 10));
        return label;
    }

    private JTabbedPane createVisualizationTabs()
    {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(30, 31, 34));
        tabbedPane.setForeground(new Color(200, 200, 200));

        JPanel graphTab = createGraphTab();
        tabbedPane.addTab("Progress Graphs", graphTab);

        liveMousePanel = new LiveMousePanel(trainer);
        tabbedPane.addTab("Live Mouse Tracking", liveMousePanel);

        stateSpacePanel = new StateSpacePanel(trainer);
        tabbedPane.addTab("State Space", stateSpacePanel);

        return tabbedPane;
    }

    private JPanel createGraphTab()
    {
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        panel.setBackground(new Color(30, 31, 34));

        transitionSeries = new TimeSeries("Transitions");
        TimeSeriesCollection transitionDataset = new TimeSeriesCollection(transitionSeries);
        JFreeChart transitionChart = ChartFactory.createTimeSeriesChart(
            "Transitions Over Time",
            "Time",
            "Total Transitions",
            transitionDataset,
            false,
            true,
            false
        );
        styleChart(transitionChart);
        ChartPanel transitionPanel = new ChartPanel(transitionChart);
        transitionPanel.setBackground(new Color(30, 31, 34));
        panel.add(transitionPanel);

        sampleRateSeries = new TimeSeries("Sample Rate");
        TimeSeriesCollection rateDataset = new TimeSeriesCollection(sampleRateSeries);
        JFreeChart rateChart = ChartFactory.createTimeSeriesChart(
            "Recording Rate (samples/sec)",
            "Time",
            "Samples/sec",
            rateDataset,
            false,
            true,
            false
        );
        styleChart(rateChart);
        ChartPanel ratePanel = new ChartPanel(rateChart);
        ratePanel.setBackground(new Color(30, 31, 34));
        panel.add(ratePanel);

        return panel;
    }

    private void styleChart(JFreeChart chart)
    {
        chart.setBackgroundPaint(new Color(30, 31, 34));
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(new Color(40, 42, 46));
        plot.setDomainGridlinePaint(new Color(60, 62, 66));
        plot.setRangeGridlinePaint(new Color(60, 62, 66));
        chart.getTitle().setPaint(new Color(200, 200, 200));
    }

    private JPanel createStatusBar()
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(new Color(35, 37, 41));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(20, 21, 24)));

        persistenceLabel = new JLabel("[*] Auto-save: 0 unsaved | Last: never");
        persistenceLabel.setForeground(new Color(100, 200, 100));
        persistenceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(persistenceLabel);

        return panel;
    }

    private void startTimers()
    {
        statsTimer = new Timer(1000, e -> updateStats());
        statsTimer.start();
        liveTrackingTimer = new Timer(100, e -> updateLiveTracking());
        liveTrackingTimer.start();
    }

    private void updateStats()
    {
        if (trainer == null) return;

        MarkovMouseTrainer.TrainingStatistics stats = trainer.getStatistics();

        if (trainer.isRunning())
        {
            statusLabel.setText("Status: Recording");
            statusLabel.setForeground(new Color(100, 200, 100));
            startStopButton.setText("Stop Recording");
        }
        else
        {
            statusLabel.setText("Status: Idle");
            statusLabel.setForeground(new Color(255, 200, 0));
            startStopButton.setText("Start Recording");
        }

        long transitions = stats.chain.totalTransitions;
        transitionsLabel.setText("Transitions: " + formatNumber(transitions));
        statesLabel.setText("Unique States: " + formatNumber(stats.chain.uniqueStates));
        samplesLabel.setText("Samples: " + formatNumber(stats.chain.totalSamples));

        long currentTime = System.currentTimeMillis();
        long timeDelta = currentTime - lastUpdateTime;
        if (timeDelta > 0)
        {
            long sampleDelta = stats.chain.totalSamples - lastSampleCount;
            double rate = (sampleDelta * 1000.0) / timeDelta;
            recordingRateLabel.setText(String.format("Recording Rate: %.1f Hz", rate));

            Second now = new Second(new Date());
            transitionSeries.addOrUpdate(now, transitions);
            sampleRateSeries.addOrUpdate(now, rate);

            if (transitionSeries.getItemCount() > 300)
            {
                transitionSeries.delete(0, 0);
            }
            if (sampleRateSeries.getItemCount() > 300)
            {
                sampleRateSeries.delete(0, 0);
            }
        }

        lastSampleCount = stats.chain.totalSamples;
        lastUpdateTime = currentTime;

        updateQuality(transitions);

        if (stats.persistence != null)
        {
            long timeSince = stats.persistence.getTimeSinceLastSave() / 1000;
            String timeSinceStr = timeSince < 0 ? "never" : timeSince + "s ago";
            persistenceLabel.setText(String.format("[*] Auto-save: %d unsaved | Last: %s",
                stats.persistence.unsavedTransitions, timeSinceStr));

            if (stats.persistence.unsavedTransitions > 0)
            {
                persistenceLabel.setForeground(new Color(255, 200, 0));
            }
            else
            {
                persistenceLabel.setForeground(new Color(100, 200, 100));
            }
        }
    }

    private void updateQuality(long transitions)
    {
        MarkovChainData.ChainStatistics chainStats = trainer.getChainData().getStatistics();

        double quantityScore = Math.min(transitions / 10000.0, 1.0);
        double diversityScore = Math.min(chainStats.uniqueMovements / 500.0, 1.0);
        double breadthScore = Math.min(chainStats.avgTransitionsPerState / 20.0, 1.0);

        double overallQuality = (quantityScore * 0.3) + (diversityScore * 0.4) + (breadthScore * 0.3);
        int percentage = (int) (overallQuality * 100);

        String qualityText;
        Color qualityColor;

        if (percentage < 30)
        {
            qualityText = "Poor";
            qualityColor = new Color(255, 100, 100);
        }
        else if (percentage < 50)
        {
            qualityText = "Fair";
            qualityColor = new Color(255, 150, 80);
        }
        else if (percentage < 70)
        {
            qualityText = "Good";
            qualityColor = new Color(255, 200, 0);
        }
        else if (percentage < 85)
        {
            qualityText = "Very Good";
            qualityColor = new Color(150, 220, 100);
        }
        else
        {
            qualityText = "Excellent";
            qualityColor = new Color(100, 255, 100);
        }

        qualityBar.setValue(percentage);
        qualityBar.setString(percentage + "% (" + chainStats.uniqueMovements + " movements)");
        qualityBar.setForeground(qualityColor);
        qualityLabel.setText("Quality: " + qualityText);
        qualityLabel.setForeground(qualityColor);
    }

    private void updateLiveTracking()
    {
        if (liveMousePanel != null && liveMousePanel.isShowing())
        {
            liveMousePanel.repaint();
        }

        if (stateSpacePanel != null && stateSpacePanel.isShowing())
        {
            stateSpacePanel.updateIfNeeded();
        }
    }

    private void toggleTraining()
    {
        if (trainer.isRunning())
        {
            trainer.stop();
        }
        else
        {
            trainer.start();
        }
    }

    private void exportToJSON()
    {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Markov Model");
        fileChooser.setSelectedFile(new java.io.File("markov_model.json"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION)
        {
            try
            {
                trainer.save(fileChooser.getSelectedFile());
                JOptionPane.showMessageDialog(this,
                    "Model exported successfully!",
                    "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            catch (Exception ex)
            {
                JOptionPane.showMessageDialog(this,
                    "Failed to export model: " + ex.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearTrainingData()
    {
        int confirm = JOptionPane.showConfirmDialog(this,
            "This will permanently delete all training data!\n" +
            "Both in-memory data and markov.dat will be cleared.\n\n" +
            "Are you sure you want to continue?",
            "Clear Training Data",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION)
        {
            try
            {
                trainer.clearData();
                JOptionPane.showMessageDialog(this,
                    "Training data cleared successfully!\n" +
                    "You can now start recording fresh data.",
                    "Data Cleared",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            catch (Exception ex)
            {
                JOptionPane.showMessageDialog(this,
                    "Failed to clear data: " + ex.getMessage(),
                    "Clear Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String formatNumber(long number)
    {
        if (number >= 1_000_000)
        {
            return new DecimalFormat("#.##M").format(number / 1_000_000.0);
        }
        else if (number >= 1_000)
        {
            return new DecimalFormat("#.#K").format(number / 1_000.0);
        }
        else
        {
            return String.valueOf(number);
        }
    }

    @Override
    public void dispose()
    {
        if (statsTimer != null) statsTimer.stop();
        if (liveTrackingTimer != null) liveTrackingTimer.stop();
        super.dispose();
    }
}

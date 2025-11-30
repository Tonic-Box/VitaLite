package com.tonic.services.mouserecorder.trajectory.ui;

import com.tonic.model.ui.components.VitaFrame;
import com.tonic.services.mouserecorder.MouseDataPoint;
import com.tonic.services.mouserecorder.trajectory.Trajectory;
import com.tonic.services.mouserecorder.trajectory.TrajectoryDatabase;
import com.tonic.services.mouserecorder.trajectory.TrajectoryGeneratorConfig;
import com.tonic.services.mouserecorder.trajectory.TrajectoryService;
import com.tonic.util.config.ConfigFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Training monitor UI for trajectory recording system.
 * Provides real-time visualization, statistics, and training controls.
 */
public class TrajectoryTrainerMonitor extends VitaFrame
{
    private static TrajectoryTrainerMonitor instance = null;

    private final JLabel statusLabel;
    private final JLabel countLabel;
    private final JLabel avgDistanceLabel;
    private final JLabel avgDurationLabel;
    private final JLabel avgPointsLabel;
    private final JLabel qualityLabel;
    private final JLabel saveStatusLabel;
    private final JButton saveButton;
    private final JButton clearButton;
    private final TrajectoryVisualizationPanel visualizationPanel;
    private final Timer updateTimer;

    public static TrajectoryTrainerMonitor getInstance()
    {
        if (instance == null)
        {
            if (SwingUtilities.isEventDispatchThread())
            {
                instance = new TrajectoryTrainerMonitor();
            }
            else
            {
                try
                {
                    SwingUtilities.invokeAndWait(() -> {
                        if (instance == null)
                        {
                            instance = new TrajectoryTrainerMonitor();
                        }
                    });
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Failed to create TrajectoryTrainerMonitor on EDT", e);
                }
            }
        }
        return instance;
    }

    private TrajectoryTrainerMonitor()
    {
        super("Trajectory Training Monitor");

        getContentPanel().setLayout(new BorderLayout(10, 10));

        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new GridLayout(0, 2, 10, 5));
        statsPanel.setBackground(new Color(40, 41, 44));
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 61, 64)),
                "Training Statistics",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(200, 200, 200)
            ),
            new EmptyBorder(10, 10, 10, 10)
        ));

        statusLabel = createValueLabel("Stopped", Color.ORANGE);
        countLabel = createValueLabel("0", Color.GREEN);
        avgDistanceLabel = createValueLabel("0.0 px", Color.CYAN);
        avgDurationLabel = createValueLabel("0 ms", Color.CYAN);
        avgPointsLabel = createValueLabel("0", Color.CYAN);
        qualityLabel = createValueLabel("No Data", Color.YELLOW);
        saveStatusLabel = createValueLabel("All Saved", Color.GREEN);

        statsPanel.add(createStatRow("Status:", statusLabel));
        statsPanel.add(createStatRow("Trajectories:", countLabel));
        statsPanel.add(createStatRow("Save Status:", saveStatusLabel));
        statsPanel.add(createStatRow("Avg Distance:", avgDistanceLabel));
        statsPanel.add(createStatRow("Avg Duration:", avgDurationLabel));
        statsPanel.add(createStatRow("Avg Points/Path:", avgPointsLabel));
        statsPanel.add(createStatRow("Quality:", qualityLabel));

        JPanel settingsPanel = createSettingsPanel();
        visualizationPanel = new TrajectoryVisualizationPanel();

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlPanel.setBackground(new Color(30, 31, 34));

        saveButton = createButton("Save Now");
        clearButton = createButton("Clear All");

        saveButton.addActionListener(e -> saveData());
        clearButton.addActionListener(e -> clearData());

        controlPanel.add(saveButton);
        controlPanel.add(clearButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(30, 31, 34));

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(new Color(30, 31, 34));
        northPanel.add(statsPanel, BorderLayout.NORTH);
        northPanel.add(settingsPanel, BorderLayout.SOUTH);

        topPanel.add(northPanel, BorderLayout.NORTH);
        topPanel.add(visualizationPanel, BorderLayout.CENTER);

        getContentPanel().add(topPanel, BorderLayout.CENTER);
        getContentPanel().add(controlPanel, BorderLayout.SOUTH);

        updateTimer = new Timer(true);
        updateTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                SwingUtilities.invokeLater(() -> updateStatistics());
            }
        }, 0, 500);

        setSize(800, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        updateStatistics();
    }

    private JPanel createStatRow(String label, JLabel valueLabel)
    {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBackground(new Color(40, 41, 44));

        JLabel labelComp = new JLabel(label);
        labelComp.setForeground(Color.WHITE);
        labelComp.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        panel.add(labelComp, BorderLayout.WEST);
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }

    private JLabel createValueLabel(String text, Color color)
    {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    private JButton createButton(String text)
    {
        JButton button = new JButton(text);
        button.setBackground(new Color(60, 61, 64));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        return button;
    }

    private JPanel createSettingsPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(40, 41, 44));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 61, 64)),
                "Recording Settings",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(200, 200, 200)
            ),
            new EmptyBorder(10, 10, 10, 10)
        ));

        TrajectoryGeneratorConfig config = ConfigFactory.create(TrajectoryGeneratorConfig.class);

        // Visualization History Slider
        JPanel historyPanel = new JPanel(new BorderLayout(10, 0));
        historyPanel.setBackground(new Color(40, 41, 44));
        historyPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel historyLabel = new JLabel("Visualization History:");
        historyLabel.setForeground(Color.WHITE);
        historyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        historyLabel.setToolTipText("Number of recent trajectories to display in visualization");

        JSlider historySlider = new JSlider(5, 100, config.getVisualizationHistoryCount());
        historySlider.setBackground(new Color(40, 41, 44));
        historySlider.setForeground(new Color(100, 200, 100));
        historySlider.setMajorTickSpacing(20);
        historySlider.setMinorTickSpacing(5);
        historySlider.setPaintTicks(true);
        historySlider.setPaintLabels(false);

        JLabel historyValue = new JLabel(config.getVisualizationHistoryCount() + " paths");
        historyValue.setForeground(new Color(100, 200, 100));
        historyValue.setFont(new Font("Segoe UI", Font.BOLD, 11));
        historyValue.setPreferredSize(new Dimension(60, 20));
        historyValue.setHorizontalAlignment(SwingConstants.RIGHT);

        historySlider.addChangeListener(e -> {
            int value = historySlider.getValue();
            historyValue.setText(value + " paths");
            config.setVisualizationHistoryCount(value);
        });

        JPanel historySliderPanel = new JPanel(new BorderLayout());
        historySliderPanel.setBackground(new Color(40, 41, 44));
        historySliderPanel.add(historySlider, BorderLayout.CENTER);
        historySliderPanel.add(historyValue, BorderLayout.EAST);

        historyPanel.add(historyLabel, BorderLayout.WEST);
        historyPanel.add(historySliderPanel, BorderLayout.CENTER);

        panel.add(historyPanel);

        return panel;
    }

    private void saveData()
    {
        TrajectoryService.saveToFile();
        updateStatistics();
        JOptionPane.showMessageDialog(this, "Training data saved!\n(Auto-saves every 30 seconds)", "Save Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearData()
    {
        int result = JOptionPane.showConfirmDialog(this,
            "Clear all training data? Data will be saved first.\nThis cannot be undone.",
            "Confirm Clear",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION)
        {
            TrajectoryService.saveToFile();
            TrajectoryService.clearData();
            updateStatistics();
        }
    }

    private void updateStatistics()
    {
        TrajectoryDatabase database = TrajectoryService.getDatabase();
        TrajectoryDatabase.TrajectoryStatistics stats = database.getStatistics();

        if (TrajectoryService.isRecording())
        {
            statusLabel.setText("Recording");
            statusLabel.setForeground(Color.GREEN);
        }
        else
        {
            statusLabel.setText("Stopped");
            statusLabel.setForeground(Color.ORANGE);
        }

        countLabel.setText(String.valueOf(stats.getTotalCount()));

        int unsavedCount = TrajectoryService.getUnsavedCount();
        if (unsavedCount == 0)
        {
            saveStatusLabel.setText("All Saved");
            saveStatusLabel.setForeground(Color.GREEN);
        }
        else
        {
            saveStatusLabel.setText(unsavedCount + " Unsaved");
            saveStatusLabel.setForeground(Color.YELLOW);
        }

        avgDistanceLabel.setText(String.format("%.1f px", stats.getAvgDistance()));
        avgDurationLabel.setText(String.format("%.0f ms", stats.getAvgDuration()));
        avgPointsLabel.setText(String.format("%.1f", stats.getAvgPointsPerTrajectory()));

        String quality = evaluateQuality(stats.getTotalCount());
        qualityLabel.setText(quality);
        qualityLabel.setForeground(getQualityColor(quality));

        visualizationPanel.updateTrajectories(database.getAllTrajectories());
    }

    private String evaluateQuality(int count)
    {
        if (count < 50) return "Poor - Record more varied movements";
        if (count < 150) return "Fair - Getting better";
        if (count < 300) return "Good - Solid coverage";
        if (count < 500) return "Very Good - Excellent coverage";
        if (count < 1000) return "Excellent - Comprehensive";
        return "Outstanding - Maximum coverage";
    }

    private Color getQualityColor(String quality)
    {
        switch (quality)
        {
            case "Poor - Record more varied movements": return Color.RED;
            case "Fair - Getting better": return Color.ORANGE;
            case "Good - Solid coverage": return Color.YELLOW;
            case "Very Good - Excellent coverage": return new Color(150, 255, 150);
            case "Excellent - Comprehensive": return new Color(75, 255, 75);
            case "Outstanding - Maximum coverage": return Color.GREEN;
            default: return Color.GRAY;
        }
    }

    @Override
    public void dispose()
    {
        updateTimer.cancel();
        instance = null;
        super.dispose();
    }

    private static class TrajectoryVisualizationPanel extends JPanel
    {
        private List<Trajectory> trajectories;

        public TrajectoryVisualizationPanel()
        {
            setBackground(new Color(30, 31, 34));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(new Color(60, 61, 64)),
                    "Trajectory Visualization",
                    TitledBorder.LEFT,
                    TitledBorder.TOP,
                    new Font("Segoe UI", Font.BOLD, 12),
                    new Color(200, 200, 200)
                ),
                new EmptyBorder(10, 10, 10, 10)
            ));
            setPreferredSize(new Dimension(0, 300));
        }

        public void updateTrajectories(List<Trajectory> trajectories)
        {
            this.trajectories = trajectories;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            if (trajectories == null || trajectories.isEmpty()) return;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;

            TrajectoryGeneratorConfig config = ConfigFactory.create(TrajectoryGeneratorConfig.class);
            int displayCount = Math.min(config.getVisualizationHistoryCount(), trajectories.size());

            for (int i = trajectories.size() - displayCount; i < trajectories.size(); i++)
            {
                Trajectory traj = trajectories.get(i);
                List<MouseDataPoint> points = traj.getPoints();

                if (points.size() < 2) continue;

                MouseDataPoint start = points.get(0);
                int offsetX = centerX - start.getX() / 2;
                int offsetY = centerY - start.getY() / 2;

                float alpha = 0.3f + (float) (i - (trajectories.size() - displayCount)) / displayCount * 0.7f;
                g2d.setColor(new Color(100, 150, 255, (int) (alpha * 255)));

                for (int j = 0; j < points.size() - 1; j++)
                {
                    MouseDataPoint p1 = points.get(j);
                    MouseDataPoint p2 = points.get(j + 1);

                    g2d.drawLine(
                        offsetX + p1.getX() / 2,
                        offsetY + p1.getY() / 2,
                        offsetX + p2.getX() / 2,
                        offsetY + p2.getY() / 2
                    );
                }

                g2d.setColor(new Color(100, 255, 100, (int) (alpha * 255)));
                g2d.fillOval(offsetX + start.getX() / 2 - 3, offsetY + start.getY() / 2 - 3, 6, 6);

                MouseDataPoint end = points.get(points.size() - 1);
                g2d.setColor(new Color(255, 100, 100, (int) (alpha * 255)));
                g2d.fillOval(offsetX + end.getX() / 2 - 3, offsetY + end.getY() / 2 - 3, 6, 6);
            }
        }
    }
}

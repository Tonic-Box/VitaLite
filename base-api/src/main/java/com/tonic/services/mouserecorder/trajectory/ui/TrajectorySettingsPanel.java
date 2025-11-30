package com.tonic.services.mouserecorder.trajectory.ui;

import com.tonic.model.ui.components.VitaFrame;
import com.tonic.services.mouserecorder.trajectory.TrajectoryGeneratorConfig;
import com.tonic.util.config.ConfigFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Settings window for trajectory generator configuration.
 * Provides controls for retrieval, blending, and noise parameters.
 */
public class TrajectorySettingsPanel extends VitaFrame
{
    private static TrajectorySettingsPanel instance = null;

    private final TrajectoryGeneratorConfig config;

    private JSlider retrievalCountSlider;
    private JSlider variationAnalysisCountSlider;
    private JSlider minSimilaritySlider;
    private JSlider blendRandomnessSlider;
    private JComboBox<String> noiseTypeCombo;
    private JSlider whiteNoiseSigmaSlider;
    private JSlider correlatedNoiseSigmaSlider;
    private JSlider correlatedNoiseCorrelationSlider;
    private JSlider maxSamplesSlider;
    private JSlider movementDurationSlider;
    private JSlider minDistanceSlider;
    private JSlider maxInstantJumpDistSlider;
    private JSlider instantJumpChanceSlider;
    private JCheckBox adaptiveProfilingCheckbox;
    private JCheckBox rapidModeCheckbox;
    private JSlider rapidModeThresholdSlider;
    private JSlider rapidModeShortDistSlider;
    private JSlider rapidModeMediumDistSlider;
    private JSlider rapidModeSampleReductionSlider;
    private JSlider rapidModeRandomChanceSlider;

    private JLabel retrievalCountLabel;
    private JLabel variationAnalysisCountLabel;
    private JLabel minSimilarityLabel;
    private JLabel blendRandomnessLabel;
    private JLabel whiteNoiseSigmaLabel;
    private JLabel correlatedNoiseSigmaLabel;
    private JLabel correlatedNoiseCorrelationLabel;
    private JLabel maxSamplesLabel;
    private JLabel movementDurationLabel;
    private JLabel minDistanceLabel;
    private JLabel maxInstantJumpDistLabel;
    private JLabel instantJumpChanceLabel;
    private JLabel rapidModeThresholdLabel;
    private JLabel rapidModeShortDistLabel;
    private JLabel rapidModeMediumDistLabel;
    private JLabel rapidModeSampleReductionLabel;
    private JLabel rapidModeRandomChanceLabel;

    public static TrajectorySettingsPanel getInstance()
    {
        if (instance == null)
        {
            if (SwingUtilities.isEventDispatchThread())
            {
                instance = new TrajectorySettingsPanel();
            }
            else
            {
                try
                {
                    SwingUtilities.invokeAndWait(() -> {
                        if (instance == null)
                        {
                            instance = new TrajectorySettingsPanel();
                        }
                    });
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Failed to create TrajectorySettingsPanel on EDT", e);
                }
            }
        }
        return instance;
    }

    private TrajectorySettingsPanel()
    {
        super("Movement Settings");
        this.config = ConfigFactory.create(TrajectoryGeneratorConfig.class);

        getContentPanel().setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(createSettingsContent());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        getContentPanel().add(scrollPane, BorderLayout.CENTER);
        getContentPanel().add(createButtonPanel(), BorderLayout.SOUTH);

        setSize(1020, 700);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        loadConfigValues();
    }

    private JPanel createSettingsContent()
    {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(new Color(30, 31, 34));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));

        content.add(createNaturalMovementSection());
        content.add(Box.createVerticalStrut(15));
        content.add(createRapidModeSection());
        content.add(Box.createVerticalStrut(15));
        content.add(createRetrievalSection());
        content.add(Box.createVerticalStrut(15));
        content.add(createNoiseSection());

        return content;
    }

    private JPanel createNaturalMovementSection()
    {
        JPanel panel = createSectionPanel("Natural Movement (Anti-Detection)");

        JPanel checkboxPanel = new JPanel(new BorderLayout(10, 0));
        checkboxPanel.setBackground(new Color(40, 41, 44));
        checkboxPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel adaptiveLabel = new JLabel("Adaptive Profiling:");
        adaptiveLabel.setForeground(Color.WHITE);
        adaptiveLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        adaptiveProfilingCheckbox = new JCheckBox();
        adaptiveProfilingCheckbox.setBackground(new Color(40, 41, 44));
        adaptiveProfilingCheckbox.setForeground(Color.WHITE);
        adaptiveProfilingCheckbox.addActionListener(e ->
            config.setUseAdaptiveProfiling(adaptiveProfilingCheckbox.isSelected()));

        checkboxPanel.add(adaptiveLabel, BorderLayout.WEST);
        checkboxPanel.add(adaptiveProfilingCheckbox, BorderLayout.CENTER);
        panel.add(checkboxPanel);

        panel.add(createSliderRow("Max Samples:", 1, 10, 5, 1, " samples",
            value -> {
                maxSamplesLabel.setText(value + " samples");
                config.setMaxSamplesPerMovement(value);
            },
            value -> maxSamplesSlider = value,
            label -> maxSamplesLabel = label));

        panel.add(createSliderRow("Movement Duration:", 50, 500, 250, 50, " ms",
            value -> {
                movementDurationLabel.setText(value + " ms");
                config.setMovementDurationMs(value);
            },
            value -> movementDurationSlider = value,
            label -> movementDurationLabel = label));

        panel.add(createSliderRow("Min Distance for Path:", 0, 200, 50, 20, " px",
            value -> {
                minDistanceLabel.setText(value + " px");
                config.setMinDistanceForTrajectory(value);
            },
            value -> minDistanceSlider = value,
            label -> minDistanceLabel = label));

        panel.add(createSliderRow("Max Distance for Jump:", 50, 250, 120, 25, " px",
            value -> {
                maxInstantJumpDistLabel.setText(value + " px");
                config.setMaxDistanceForInstantJump(value);
            },
            value -> maxInstantJumpDistSlider = value,
            label -> maxInstantJumpDistLabel = label));

        panel.add(createSliderRow("Instant Jump Chance:", 0, 50, 15, 10, "%",
            value -> {
                instantJumpChanceLabel.setText(value + "%");
                config.setInstantJumpChance(value / 100.0);
            },
            value -> instantJumpChanceSlider = value,
            label -> instantJumpChanceLabel = label));

        JLabel helpText = new JLabel("<html><i>Adaptive learns from training data. Manual overrides active when disabled.</i></html>");
        helpText.setForeground(new Color(150, 150, 150));
        helpText.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        helpText.setBorder(new EmptyBorder(5, 0, 0, 0));
        panel.add(helpText);

        return panel;
    }

    private JPanel createRapidModeSection()
    {
        JPanel panel = createSectionPanel("Rapid Action Mode");

        JPanel checkboxPanel = new JPanel(new BorderLayout(10, 0));
        checkboxPanel.setBackground(new Color(40, 41, 44));
        checkboxPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel rapidModeLabel = new JLabel("Enable Rapid Mode:");
        rapidModeLabel.setForeground(Color.WHITE);
        rapidModeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        rapidModeCheckbox = new JCheckBox();
        rapidModeCheckbox.setBackground(new Color(40, 41, 44));
        rapidModeCheckbox.setForeground(Color.WHITE);
        rapidModeCheckbox.addActionListener(e ->
            config.setRapidModeEnabled(rapidModeCheckbox.isSelected()));

        checkboxPanel.add(rapidModeLabel, BorderLayout.WEST);
        checkboxPanel.add(rapidModeCheckbox, BorderLayout.CENTER);
        panel.add(checkboxPanel);

        panel.add(createSliderRow("Time Threshold:", 100, 5000, 1800, 100, " ms",
            value -> {
                rapidModeThresholdLabel.setText(value + " ms");
                config.setRapidModeThresholdMs(value);
            },
            value -> rapidModeThresholdSlider = value,
            label -> rapidModeThresholdLabel = label));

        panel.add(createSliderRow("Short Distance:", 50, 200, 100, 25, " px",
            value -> {
                rapidModeShortDistLabel.setText(value + " px");
                config.setRapidModeShortDistanceThreshold(value);
            },
            value -> rapidModeShortDistSlider = value,
            label -> rapidModeShortDistLabel = label));

        panel.add(createSliderRow("Medium Distance:", 100, 400, 200, 50, " px",
            value -> {
                rapidModeMediumDistLabel.setText(value + " px");
                config.setRapidModeMediumDistanceThreshold(value);
            },
            value -> rapidModeMediumDistSlider = value,
            label -> rapidModeMediumDistLabel = label));

        panel.add(createSliderRow("Sample Reduction:", 30, 90, 60, 10, "%",
            value -> {
                rapidModeSampleReductionLabel.setText(value + "%");
                config.setRapidModeSampleReduction(value / 100.0);
            },
            value -> rapidModeSampleReductionSlider = value,
            label -> rapidModeSampleReductionLabel = label));

        panel.add(createSliderRow("Random Speedup Chance:", 0, 100, 0, 5, "%",
            value -> {
                rapidModeRandomChanceLabel.setText(value + "%");
                config.setRapidModeRandomChance(value / 100.0);
            },
            value -> rapidModeRandomChanceSlider = value,
            label -> rapidModeRandomChanceLabel = label));

        JLabel helpText = new JLabel("<html><i>Rapid mode activates when movements occur within threshold time. Random speedup chance adds variation by occasionally applying speedup to longer movements.</i></html>");
        helpText.setForeground(new Color(150, 150, 150));
        helpText.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        helpText.setBorder(new EmptyBorder(5, 0, 0, 0));
        panel.add(helpText);

        return panel;
    }

    private JPanel createRetrievalSection()
    {
        JPanel panel = createSectionPanel("Retrieval & Blending");

        panel.add(createSliderRow("Retrieval Count:", 1, 10, 3, 1, "",
            value -> {
                retrievalCountLabel.setText(value + " paths");
                config.setRetrievalCount(value);
            },
            value -> retrievalCountSlider = value,
            label -> retrievalCountLabel = label));

        panel.add(createSliderRow("Variation Analysis Count:", 1, 10, 5, 1, "",
            value -> {
                variationAnalysisCountLabel.setText(value + " paths");
                config.setVariationAnalysisCount(value);
            },
            value -> variationAnalysisCountSlider = value,
            label -> variationAnalysisCountLabel = label));

        panel.add(createSliderRow("Min Similarity:", 0, 100, 30, 10, "%",
            value -> {
                minSimilarityLabel.setText(value + "%");
                config.setMinSimilarity(value / 100.0);
            },
            value -> minSimilaritySlider = value,
            label -> minSimilarityLabel = label));

        panel.add(createSliderRow("Blend Randomness:", 0, 100, 30, 10, "%",
            value -> {
                blendRandomnessLabel.setText(value + "%");
                config.setBlendRandomness(value / 100.0);
            },
            value -> blendRandomnessSlider = value,
            label -> blendRandomnessLabel = label));

        return panel;
    }

    private JPanel createNoiseSection()
    {
        JPanel panel = createSectionPanel("Noise Generation");

        JPanel noiseTypePanel = new JPanel(new BorderLayout(10, 0));
        noiseTypePanel.setBackground(new Color(40, 41, 44));
        noiseTypePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel typeLabel = new JLabel("Noise Type:");
        typeLabel.setForeground(Color.WHITE);
        typeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        noiseTypeCombo = new JComboBox<>(new String[]{"NONE", "WHITE", "CORRELATED"});
        noiseTypeCombo.setBackground(new Color(50, 51, 54));
        noiseTypeCombo.setForeground(Color.WHITE);
        noiseTypeCombo.addActionListener(e -> {
            String type = (String) noiseTypeCombo.getSelectedItem();
            config.setNoiseType(type);
            updateNoiseControlsEnabled(type);
        });

        noiseTypePanel.add(typeLabel, BorderLayout.WEST);
        noiseTypePanel.add(noiseTypeCombo, BorderLayout.CENTER);
        panel.add(noiseTypePanel);

        panel.add(createSliderRow("White Noise Sigma:", 0, 50, 15, 10, "",
            value -> {
                whiteNoiseSigmaLabel.setText(String.format("%.1f px", value / 10.0));
                config.setWhiteNoiseSigma(value / 10.0);
            },
            value -> whiteNoiseSigmaSlider = value,
            label -> whiteNoiseSigmaLabel = label));

        panel.add(createSliderRow("Correlated Noise Sigma:", 0, 50, 20, 10, "",
            value -> {
                correlatedNoiseSigmaLabel.setText(String.format("%.1f px", value / 10.0));
                config.setCorrelatedNoiseSigma(value / 10.0);
            },
            value -> correlatedNoiseSigmaSlider = value,
            label -> correlatedNoiseSigmaLabel = label));

        panel.add(createSliderRow("Noise Correlation:", 0, 100, 70, 10, "%",
            value -> {
                correlatedNoiseCorrelationLabel.setText(String.format("%.2f", value / 100.0));
                config.setCorrelatedNoiseCorrelation(value / 100.0);
            },
            value -> correlatedNoiseCorrelationSlider = value,
            label -> correlatedNoiseCorrelationLabel = label));

        return panel;
    }

    private void updateNoiseControlsEnabled(String noiseType)
    {
        boolean whiteEnabled = "WHITE".equals(noiseType);
        boolean correlatedEnabled = "CORRELATED".equals(noiseType);

        whiteNoiseSigmaSlider.setEnabled(whiteEnabled);
        whiteNoiseSigmaLabel.setEnabled(whiteEnabled);

        correlatedNoiseSigmaSlider.setEnabled(correlatedEnabled);
        correlatedNoiseSigmaLabel.setEnabled(correlatedEnabled);
        correlatedNoiseCorrelationSlider.setEnabled(correlatedEnabled);
        correlatedNoiseCorrelationLabel.setEnabled(correlatedEnabled);
    }

    private JPanel createSectionPanel(String title)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(40, 41, 44));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 61, 64)),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(200, 200, 200)
            ),
            new EmptyBorder(10, 10, 10, 10)
        ));
        return panel;
    }

    private JPanel createSliderRow(String labelText, int min, int max, int defaultValue, int majorTick, String suffix,
                                   java.util.function.Consumer<Integer> onValueChange,
                                   java.util.function.Consumer<JSlider> onSliderCreated,
                                   java.util.function.Consumer<JLabel> onLabelCreated)
    {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(new Color(40, 41, 44));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        row.add(label, BorderLayout.WEST);

        JSlider slider = new JSlider(min, max, defaultValue);
        slider.setBackground(new Color(40, 41, 44));
        slider.setForeground(new Color(100, 200, 100));
        slider.setMajorTickSpacing(majorTick);
        slider.setPaintTicks(true);
        slider.setPaintLabels(false);

        JLabel valueLabel = new JLabel(defaultValue + suffix);
        valueLabel.setForeground(new Color(100, 200, 100));
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        valueLabel.setPreferredSize(new Dimension(80, 20));
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        slider.addChangeListener(e -> {
            int value = slider.getValue();
            onValueChange.accept(value);
        });

        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.setBackground(new Color(40, 41, 44));
        sliderPanel.add(slider, BorderLayout.CENTER);
        sliderPanel.add(valueLabel, BorderLayout.EAST);

        row.add(sliderPanel, BorderLayout.CENTER);

        onSliderCreated.accept(slider);
        onLabelCreated.accept(valueLabel);

        return row;
    }

    private JPanel createButtonPanel()
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(new Color(30, 31, 34));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(20, 21, 24)));

        JButton docButton = new JButton("Documentation");
        docButton.setBackground(new Color(45, 85, 125));
        docButton.setForeground(Color.WHITE);
        docButton.setFocusPainted(false);
        docButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        docButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        docButton.addActionListener(e -> openDocumentation());
        panel.add(docButton);

        JButton resetButton = new JButton("Reset to Defaults");
        resetButton.setBackground(new Color(60, 61, 64));
        resetButton.setForeground(Color.WHITE);
        resetButton.setFocusPainted(false);
        resetButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        resetButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resetButton.addActionListener(e -> resetToDefaults());
        panel.add(resetButton);

        return panel;
    }

    private void openDocumentation()
    {
        try
        {
            Desktop.getDesktop().browse(new java.net.URI("https://github.com/Tonic-Box/VitaLite/blob/main/docs/MOUSE-MOVEMENT.md"));
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this,
                "Could not open documentation in browser.\nURL: https://github.com/Tonic-Box/VitaLite/blob/main/docs/MOUSE-MOVEMENT.md",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadConfigValues()
    {
        adaptiveProfilingCheckbox.setSelected(config.shouldUseAdaptiveProfiling());
        maxSamplesSlider.setValue(config.getMaxSamplesPerMovement());
        movementDurationSlider.setValue(config.getMovementDurationMs());
        minDistanceSlider.setValue(config.getMinDistanceForTrajectory());
        maxInstantJumpDistSlider.setValue(config.getMaxDistanceForInstantJump());
        instantJumpChanceSlider.setValue((int) (config.getInstantJumpChance() * 100));

        rapidModeCheckbox.setSelected(config.isRapidModeEnabled());
        rapidModeThresholdSlider.setValue(config.getRapidModeThresholdMs());
        rapidModeShortDistSlider.setValue(config.getRapidModeShortDistanceThreshold());
        rapidModeMediumDistSlider.setValue(config.getRapidModeMediumDistanceThreshold());
        rapidModeSampleReductionSlider.setValue((int) (config.getRapidModeSampleReduction() * 100));
        rapidModeRandomChanceSlider.setValue((int) (config.getRapidModeRandomChance() * 100));

        retrievalCountSlider.setValue(config.getRetrievalCount());
        variationAnalysisCountSlider.setValue(config.getVariationAnalysisCount());
        minSimilaritySlider.setValue((int) (config.getMinSimilarity() * 100));
        blendRandomnessSlider.setValue((int) (config.getBlendRandomness() * 100));
        noiseTypeCombo.setSelectedItem(config.getNoiseType());
        whiteNoiseSigmaSlider.setValue((int) (config.getWhiteNoiseSigma() * 10));
        correlatedNoiseSigmaSlider.setValue((int) (config.getCorrelatedNoiseSigma() * 10));
        correlatedNoiseCorrelationSlider.setValue((int) (config.getCorrelatedNoiseCorrelation() * 100));

        updateNoiseControlsEnabled(config.getNoiseType());
    }

    private void resetToDefaults()
    {
        int result = JOptionPane.showConfirmDialog(this,
            "Reset all settings to default values?",
            "Confirm Reset",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION)
        {
            adaptiveProfilingCheckbox.setSelected(true);
            maxSamplesSlider.setValue(5);
            movementDurationSlider.setValue(250);
            minDistanceSlider.setValue(50);
            maxInstantJumpDistSlider.setValue(120);
            instantJumpChanceSlider.setValue(15);

            rapidModeCheckbox.setSelected(true);
            rapidModeThresholdSlider.setValue(1800);
            rapidModeShortDistSlider.setValue(100);
            rapidModeMediumDistSlider.setValue(200);
            rapidModeSampleReductionSlider.setValue(60);
            rapidModeRandomChanceSlider.setValue(0);

            retrievalCountSlider.setValue(3);
            variationAnalysisCountSlider.setValue(5);
            minSimilaritySlider.setValue(30);
            blendRandomnessSlider.setValue(30);
            noiseTypeCombo.setSelectedItem("NONE");
            whiteNoiseSigmaSlider.setValue(15);
            correlatedNoiseSigmaSlider.setValue(20);
            correlatedNoiseCorrelationSlider.setValue(70);

            config.setUseAdaptiveProfiling(true);
            config.setMaxSamplesPerMovement(5);
            config.setMovementDurationMs(250);
            config.setMinDistanceForTrajectory(50);
            config.setMaxDistanceForInstantJump(120);
            config.setInstantJumpChance(0.15);
            config.setRapidModeEnabled(true);
            config.setRapidModeThresholdMs(1800);
            config.setRapidModeShortDistanceThreshold(100);
            config.setRapidModeMediumDistanceThreshold(200);
            config.setRapidModeSampleReduction(0.6);
            config.setRapidModeRandomChance(0.0);
            config.setRetrievalCount(3);
            config.setVariationAnalysisCount(5);
            config.setMinSimilarity(0.3);
            config.setBlendRandomness(0.3);
            config.setNoiseType("NONE");
            config.setWhiteNoiseSigma(1.5);
            config.setCorrelatedNoiseSigma(2.0);
            config.setCorrelatedNoiseCorrelation(0.7);

            JOptionPane.showMessageDialog(this,
                "Settings reset to defaults!",
                "Reset Complete",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
}

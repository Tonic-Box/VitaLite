package com.tonic.services.mouserecorder.markov.ui;

import com.tonic.services.mouserecorder.markov.MarkovGeneratorConfig;
import com.tonic.util.config.ConfigFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Hashtable;

/**
 * Settings panel for tuning Markov mouse generator parameters.
 * Provides UI controls for adjusting path generation behavior with persistent config.
 */
public class MarkovSettingsPanel extends JPanel
{
    private final MarkovGeneratorConfig config;

    // Bias controls
    private JSlider biasMinSlider;
    private JSlider biasMaxSlider;
    private JSlider biasDistanceSlider;

    // Generation controls
    private JSlider maxStepsSlider;
    private JSlider targetToleranceSlider;

    // Temporal jitter controls
    private JCheckBox temporalJitterCheckbox;
    private JSlider temporalJitterAmountSlider;

    // Bezier smoothing controls
    private JCheckBox bezierSmoothingCheckbox;
    private JSlider bezierTensionSlider;

    // Advanced options
    private JCheckBox velocityAwareBiasingCheckbox;
    private JCheckBox secondOrderMarkovCheckbox;

    // Value labels
    private JLabel biasMinValueLabel;
    private JLabel biasMaxValueLabel;
    private JLabel biasDistanceValueLabel;
    private JLabel maxStepsValueLabel;
    private JLabel targetToleranceValueLabel;
    private JLabel temporalJitterValueLabel;
    private JLabel bezierTensionValueLabel;

    public MarkovSettingsPanel()
    {
        this.config = ConfigFactory.create(MarkovGeneratorConfig.class);

        setLayout(new BorderLayout());
        setBackground(new Color(30, 31, 34));

        JScrollPane scrollPane = new JScrollPane(createSettingsContent());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        loadConfigValues();
    }

    private JPanel createSettingsContent()
    {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(new Color(30, 31, 34));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));

        content.add(createBiasSection());
        content.add(Box.createVerticalStrut(15));
        content.add(createGenerationSection());
        content.add(Box.createVerticalStrut(15));
        content.add(createTemporalJitterSection());
        content.add(Box.createVerticalStrut(15));
        content.add(createBezierSection());
        content.add(Box.createVerticalStrut(15));
        content.add(createAdvancedSection());

        return content;
    }

    private JPanel createBiasSection()
    {
        JPanel panel = createSectionPanel("Target Biasing");

        panel.add(createSliderRow("Minimum Bias:", 0, 100, 15, 25, "%",
            value -> biasMinValueLabel.setText(value / 100.0 + ""),
            value -> biasMinSlider = value,
            label -> biasMinValueLabel = label));

        panel.add(createSliderRow("Maximum Bias:", 0, 100, 75, 25, "%",
            value -> biasMaxValueLabel.setText(value / 100.0 + ""),
            value -> biasMaxSlider = value,
            label -> biasMaxValueLabel = label));

        panel.add(createSliderRow("Bias Distance:", 100, 1000, 400, 100, "px",
            value -> biasDistanceValueLabel.setText(value + " px"),
            value -> biasDistanceSlider = value,
            label -> biasDistanceValueLabel = label));

        return panel;
    }

    private JPanel createGenerationSection()
    {
        JPanel panel = createSectionPanel("Generation Parameters");

        panel.add(createSliderRow("Max Steps:", 50, 500, 200, 50, "",
            value -> maxStepsValueLabel.setText(value + " steps"),
            value -> maxStepsSlider = value,
            label -> maxStepsValueLabel = label));

        panel.add(createSliderRow("Target Tolerance:", 5, 20, 10, 5, "px",
            value -> targetToleranceValueLabel.setText(value + " px"),
            value -> targetToleranceSlider = value,
            label -> targetToleranceValueLabel = label));

        return panel;
    }

    private JPanel createTemporalJitterSection()
    {
        JPanel panel = createSectionPanel("Temporal Jitter");

        temporalJitterCheckbox = new JCheckBox("Enable Temporal Jitter");
        temporalJitterCheckbox.setForeground(Color.WHITE);
        temporalJitterCheckbox.setBackground(new Color(40, 41, 44));
        temporalJitterCheckbox.setFocusPainted(false);
        temporalJitterCheckbox.addActionListener(e -> {
            boolean enabled = temporalJitterCheckbox.isSelected();
            temporalJitterAmountSlider.setEnabled(enabled);
            temporalJitterValueLabel.setEnabled(enabled);
        });
        panel.add(temporalJitterCheckbox);

        panel.add(createSliderRow("Jitter Amount:", 0, 100, 30, 25, "%",
            value -> temporalJitterValueLabel.setText(value + "%"),
            value -> {
                temporalJitterAmountSlider = value;
                temporalJitterAmountSlider.setEnabled(temporalJitterCheckbox.isSelected());
            },
            label -> {
                temporalJitterValueLabel = label;
                temporalJitterValueLabel.setEnabled(temporalJitterCheckbox.isSelected());
            }));

        return panel;
    }

    private JPanel createBezierSection()
    {
        JPanel panel = createSectionPanel("Bezier Smoothing");

        bezierSmoothingCheckbox = new JCheckBox("Enable Bezier Smoothing");
        bezierSmoothingCheckbox.setForeground(Color.WHITE);
        bezierSmoothingCheckbox.setBackground(new Color(40, 41, 44));
        bezierSmoothingCheckbox.setFocusPainted(false);
        bezierSmoothingCheckbox.addActionListener(e -> {
            boolean enabled = bezierSmoothingCheckbox.isSelected();
            bezierTensionSlider.setEnabled(enabled);
            bezierTensionValueLabel.setEnabled(enabled);
        });
        panel.add(bezierSmoothingCheckbox);

        panel.add(createSliderRow("Smoothing Strength:", 0, 100, 50, 25, "%",
            value -> bezierTensionValueLabel.setText(value + "%"),
            value -> {
                bezierTensionSlider = value;
                bezierTensionSlider.setEnabled(bezierSmoothingCheckbox.isSelected());
            },
            label -> {
                bezierTensionValueLabel = label;
                bezierTensionValueLabel.setEnabled(bezierSmoothingCheckbox.isSelected());
            }));

        return panel;
    }

    private JPanel createAdvancedSection()
    {
        JPanel panel = createSectionPanel("Advanced Options");

        velocityAwareBiasingCheckbox = new JCheckBox("Velocity-Aware Biasing (considers momentum)");
        velocityAwareBiasingCheckbox.setForeground(Color.WHITE);
        velocityAwareBiasingCheckbox.setBackground(new Color(40, 41, 44));
        velocityAwareBiasingCheckbox.setFocusPainted(false);
        panel.add(velocityAwareBiasingCheckbox);

        panel.add(Box.createVerticalStrut(5));

        secondOrderMarkovCheckbox = new JCheckBox("Second-Order Markov (requires more training data)");
        secondOrderMarkovCheckbox.setForeground(Color.WHITE);
        secondOrderMarkovCheckbox.setBackground(new Color(40, 41, 44));
        secondOrderMarkovCheckbox.setFocusPainted(false);
        panel.add(secondOrderMarkovCheckbox);

        return panel;
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

        JButton resetButton = new JButton("Reset to Defaults");
        resetButton.setBackground(new Color(60, 61, 64));
        resetButton.setForeground(Color.WHITE);
        resetButton.setFocusPainted(false);
        resetButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        resetButton.addActionListener(e -> resetToDefaults());
        panel.add(resetButton);

        JButton applyButton = new JButton("Apply Settings");
        applyButton.setBackground(new Color(100, 200, 100));
        applyButton.setForeground(Color.BLACK);
        applyButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        applyButton.setFocusPainted(false);
        applyButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        applyButton.addActionListener(e -> saveConfig());
        panel.add(applyButton);

        return panel;
    }

    private void loadConfigValues()
    {
        biasMinSlider.setValue((int) (config.getBiasMinimum() * 100));
        biasMaxSlider.setValue((int) (config.getBiasMaximum() * 100));
        biasDistanceSlider.setValue((int) config.getBiasDistance());
        maxStepsSlider.setValue(config.getMaxSteps());
        targetToleranceSlider.setValue((int) config.getTargetTolerance());

        temporalJitterCheckbox.setSelected(config.isTemporalJitterEnabled());
        temporalJitterAmountSlider.setValue((int) (config.getTemporalJitterAmount() * 100));
        temporalJitterAmountSlider.setEnabled(config.isTemporalJitterEnabled());
        temporalJitterValueLabel.setEnabled(config.isTemporalJitterEnabled());

        bezierSmoothingCheckbox.setSelected(config.isBezierSmoothingEnabled());
        bezierTensionSlider.setValue((int) (config.getBezierTension() * 100));
        bezierTensionSlider.setEnabled(config.isBezierSmoothingEnabled());
        bezierTensionValueLabel.setEnabled(config.isBezierSmoothingEnabled());

        velocityAwareBiasingCheckbox.setSelected(config.isVelocityAwareBiasingEnabled());
        secondOrderMarkovCheckbox.setSelected(config.isSecondOrderMarkovEnabled());
    }

    private void saveConfig()
    {
        config.setBiasMinimum(biasMinSlider.getValue() / 100.0);
        config.setBiasMaximum(biasMaxSlider.getValue() / 100.0);
        config.setBiasDistance(biasDistanceSlider.getValue());
        config.setMaxSteps(maxStepsSlider.getValue());
        config.setTargetTolerance(targetToleranceSlider.getValue());

        config.setTemporalJitterEnabled(temporalJitterCheckbox.isSelected());
        config.setTemporalJitterAmount(temporalJitterAmountSlider.getValue() / 100.0);

        config.setBezierSmoothingEnabled(bezierSmoothingCheckbox.isSelected());
        config.setBezierTension(bezierTensionSlider.getValue() / 100.0);

        config.setVelocityAwareBiasingEnabled(velocityAwareBiasingCheckbox.isSelected());
        config.setSecondOrderMarkovEnabled(secondOrderMarkovCheckbox.isSelected());

        JOptionPane.showMessageDialog(this,
            "Settings saved successfully!\nRestart training or generate new paths to see changes.",
            "Settings Applied",
            JOptionPane.INFORMATION_MESSAGE);
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
            biasMinSlider.setValue(15);
            biasMaxSlider.setValue(75);
            biasDistanceSlider.setValue(400);
            maxStepsSlider.setValue(200);
            targetToleranceSlider.setValue(10);

            temporalJitterCheckbox.setSelected(false);
            temporalJitterAmountSlider.setValue(30);

            bezierSmoothingCheckbox.setSelected(false);
            bezierTensionSlider.setValue(50);

            velocityAwareBiasingCheckbox.setSelected(false);
            secondOrderMarkovCheckbox.setSelected(false);

            saveConfig();
        }
    }
}

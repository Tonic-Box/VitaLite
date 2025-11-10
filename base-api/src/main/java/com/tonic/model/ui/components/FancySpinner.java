package com.tonic.model.ui.components;

import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class FancySpinner extends JPanel {
    private static final Color CARD_BACKGROUND = new Color(55, 55, 60);
    private static final Color BACKGROUND_COLOR = new Color(55, 55, 60);
    private static final Color TEXT_COLOR = new Color(200, 200, 205);
    private static final Color HEADER_COLOR = new Color(64, 169, 211);
    private static final Color BORDER_COLOR = new Color(70, 70, 75);
    private static final Color SEPARATOR_COLOR = new Color(70, 70, 75);
    private static final Color SPINNER_TEXT_COLOR = new Color(245, 245, 250);

    @Getter
    private final JSpinner spinner;
    private boolean isHovered = false;
    private JLabel titleLabel;
    private List<ChangeListener> changeListeners = new ArrayList<>();

    /**
     * Creates a FancySpinner with integer values
     */
    public FancySpinner(String title, int min, int max, int value) {
        super();
        spinner = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
        initializeStyle(title);
    }

    /**
     * Creates a FancySpinner with double values
     */
    public FancySpinner(String title, double min, double max, double value, double step) {
        super();
        spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        initializeStyle(title);
    }

    private void initializeStyle(String title) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(8, 12, 8, 12));
        setMaximumSize(new Dimension(VPluginPanel.PANEL_WIDTH - 20, 67));
        setAlignmentX(Component.CENTER_ALIGNMENT);

        // Create title label
        titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(HEADER_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(titleLabel);

        add(Box.createVerticalStrut(4));

        // Style spinner
        styleSpinner(spinner);
        spinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(spinner);

        add(Box.createVerticalStrut(4));

        // Add change listener to spinner
        spinner.addChangeListener(this::fireChangeEvent);

        // Add hover effects
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(BACKGROUND_COLOR);
        spinner.setForeground(SPINNER_TEXT_COLOR);
        spinner.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        spinner.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

        // Style the text field inside the spinner
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JSpinner.DefaultEditor defaultEditor = (JSpinner.DefaultEditor) editor;
            JTextField textField = defaultEditor.getTextField();
            textField.setBackground(BACKGROUND_COLOR);
            textField.setForeground(SPINNER_TEXT_COLOR);
            textField.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            textField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        }

        // Style the spinner buttons
        for (Component comp : spinner.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                button.setBackground(new Color(70, 70, 75));
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setFocusPainted(false);
                button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }
    }

    private void fireChangeEvent(ChangeEvent e) {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : changeListeners) {
            listener.stateChanged(event);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isHovered) {
            g2d.setColor(new Color(60, 60, 65));
        } else {
            g2d.setColor(CARD_BACKGROUND);
        }
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

        g2d.setColor(SEPARATOR_COLOR);
        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
    }

    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changeListeners.remove(listener);
    }

    public Number getValue() {
        return (Number) spinner.getValue();
    }

    public void setValue(Number value) {
        spinner.setValue(value);
    }

    @Override
    public void setEnabled(boolean enabled) {
        spinner.setEnabled(enabled);
        titleLabel.setForeground(enabled ? HEADER_COLOR : new Color(100, 100, 105));
    }

    public void setRange(Number min, Number max) {
        SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
        model.setMinimum((Comparable) min);
        model.setMaximum((Comparable) max);
    }

    public void setStep(Number step) {
        SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
        model.setStepSize(step);
    }
}

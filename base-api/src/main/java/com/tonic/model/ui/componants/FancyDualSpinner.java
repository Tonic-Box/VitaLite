package com.tonic.model.ui.componants;

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

public class FancyDualSpinner extends JPanel {
    private static final Color CARD_BACKGROUND = new Color(55, 55, 60);
    private static final Color BACKGROUND_COLOR = new Color(55, 55, 60);
    private static final Color TEXT_COLOR = new Color(200, 200, 205);
    private static final Color HEADER_COLOR = new Color(64, 169, 211);
    private static final Color BORDER_COLOR = new Color(70, 70, 75);
    private static final Color SEPARATOR_COLOR = new Color(70, 70, 75);
    private static final Color SPINNER_TEXT_COLOR = new Color(245, 245, 250);

    @Getter
    private final JSpinner leftSpinner;
    @Getter
    private final JSpinner rightSpinner;
    private boolean isHovered = false;
    private JLabel titleLabel;
    private List<ChangeListener> changeListeners = new ArrayList<>();

    public FancyDualSpinner(String title, int leftMin, int leftMax, int leftValue,
                           int rightMin, int rightMax, int rightValue) {
        super();

        leftSpinner = new JSpinner(new SpinnerNumberModel(leftValue, leftMin, leftMax, 1));
        rightSpinner = new JSpinner(new SpinnerNumberModel(rightValue, rightMin, rightMax, 1));

        initializeStyle(title);
    }

    public FancyDualSpinner(String title, double leftMin, double leftMax, double leftValue, double leftStep,
                           double rightMin, double rightMax, double rightValue, double rightStep) {
        super();

        leftSpinner = new JSpinner(new SpinnerNumberModel(leftValue, leftMin, leftMax, leftStep));
        rightSpinner = new JSpinner(new SpinnerNumberModel(rightValue, rightMin, rightMax, rightStep));

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

        // Create spinner panel
        JPanel spinnerPanel = new JPanel();
        spinnerPanel.setLayout(new BoxLayout(spinnerPanel, BoxLayout.X_AXIS));
        spinnerPanel.setOpaque(false);
        spinnerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        spinnerPanel.setMaximumSize(new Dimension(VPluginPanel.PANEL_WIDTH - 44, 26));

        // Style left spinner
        styleSpinner(leftSpinner);
        leftSpinner.setMaximumSize(new Dimension((VPluginPanel.PANEL_WIDTH - 48) / 2, 26));
        leftSpinner.setPreferredSize(new Dimension((VPluginPanel.PANEL_WIDTH - 48) / 2, 26));

        // Style right spinner
        styleSpinner(rightSpinner);
        rightSpinner.setMaximumSize(new Dimension((VPluginPanel.PANEL_WIDTH - 48) / 2, 26));
        rightSpinner.setPreferredSize(new Dimension((VPluginPanel.PANEL_WIDTH - 48) / 2, 26));

        // Add spinners to panel with gap
        spinnerPanel.add(leftSpinner);
        spinnerPanel.add(Box.createHorizontalStrut(4));
        spinnerPanel.add(rightSpinner);

        add(spinnerPanel);
        add(Box.createVerticalStrut(4));

        // Add change listeners to spinners
        leftSpinner.addChangeListener(this::fireChangeEvent);
        rightSpinner.addChangeListener(this::fireChangeEvent);

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

    public Number getLeftValue() {
        return (Number) leftSpinner.getValue();
    }

    public Number getRightValue() {
        return (Number) rightSpinner.getValue();
    }

    public void setLeftValue(Number value) {
        leftSpinner.setValue(value);
    }

    public void setRightValue(Number value) {
        rightSpinner.setValue(value);
    }

    public void setEnabled(boolean enabled) {
        leftSpinner.setEnabled(enabled);
        rightSpinner.setEnabled(enabled);
        titleLabel.setForeground(enabled ? HEADER_COLOR : new Color(100, 100, 105));
    }

    public void setLeftRange(Number min, Number max) {
        SpinnerNumberModel model = (SpinnerNumberModel) leftSpinner.getModel();
        model.setMinimum((Comparable) min);
        model.setMaximum((Comparable) max);
    }

    public void setRightRange(Number min, Number max) {
        SpinnerNumberModel model = (SpinnerNumberModel) rightSpinner.getModel();
        model.setMinimum((Comparable) min);
        model.setMaximum((Comparable) max);
    }
}
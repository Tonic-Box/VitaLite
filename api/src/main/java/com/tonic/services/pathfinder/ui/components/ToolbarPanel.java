package com.tonic.services.pathfinder.ui.components;

import com.tonic.services.pathfinder.ui.TransportEditorFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Toolbar panel with file operations and main action buttons
 */
public class ToolbarPanel extends JPanel {

    private static final Color BACKGROUND_COLOR = new Color(45, 47, 49);
    private static final Color BUTTON_COLOR = new Color(75, 110, 175);
    private static final Color BUTTON_DISABLED_COLOR = new Color(60, 60, 60);

    private final TransportEditorFrame parent;

    // Buttons
    private JButton loadButton;
    private JButton saveButton;
    private JButton saveAsButton;
    private JButton newTransportButton;

    public ToolbarPanel(TransportEditorFrame parent) {
        this.parent = parent;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        setBackground(BACKGROUND_COLOR);
        setBorder(new EmptyBorder(8, 10, 8, 10));

        // Create buttons
        loadButton = createToolbarButton("Re-Load JSON", "Load transports from file");
        saveButton = createToolbarButton("Save", "Save changes to current file");
        saveAsButton = createToolbarButton("Save As...", "Save to a new file");
        newTransportButton = createToolbarButton("New Transport", "Add a new transport");

        // Initially disable save button
        saveButton.setEnabled(false);
    }

    private void setupLayout() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // File operations group
        add(loadButton);
        add(createSeparator());
        add(saveButton);
        add(saveAsButton);
        add(createSeparator());

        // Transport operations group
        add(newTransportButton);

        // Add spacer and info
        add(Box.createHorizontalGlue());

        JLabel infoLabel = new JLabel("VitaLite Transport Editor v1.0");
        infoLabel.setForeground(Color.LIGHT_GRAY);
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        add(infoLabel);
    }

    private void setupEventHandlers() {
        loadButton.addActionListener(e -> parent.loadTransportsFromFile());
        saveButton.addActionListener(e -> parent.saveTransportsToFile());
        saveAsButton.addActionListener(e -> parent.saveTransportsAsNewFile());
        newTransportButton.addActionListener(e -> parent.addNewTransport());
    }

    private JButton createToolbarButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setBackground(BUTTON_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(6, 12, 6, 12));
        button.setToolTipText(tooltip);

        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(BUTTON_COLOR.brighter());
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(BUTTON_COLOR);
                }
            }
        });

        return button;
    }

    private Component createSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 20));
        separator.setForeground(new Color(85, 85, 85));
        return separator;
    }

    // Public API

    public void updateSaveButtonState(boolean hasUnsavedChanges) {
        saveButton.setEnabled(hasUnsavedChanges);
        if (hasUnsavedChanges) {
            saveButton.setBackground(BUTTON_COLOR);
        } else {
            saveButton.setBackground(BUTTON_DISABLED_COLOR);
        }
    }
}
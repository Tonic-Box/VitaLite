package com.tonic.services.pathfinder.ui.components;

import com.tonic.services.pathfinder.model.TransportDto;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.ui.TransportEditorFrame;
import net.runelite.api.coords.WorldPoint;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;

/**
 * Panel for editing transport details including coordinates, action, object ID, and requirements
 */
public class TransportDetailPanel extends JPanel {

    private static final Color BACKGROUND_COLOR = new Color(60, 63, 65);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(85, 85, 85);
    private static final Color FIELD_COLOR = new Color(69, 73, 74);

    private final TransportEditorFrame parent;
    private TransportDto currentTransport;

    // Source coordinate fields
    private JSpinner sourceXField;
    private JSpinner sourceYField;
    private JSpinner sourcePlaneField;

    // Destination coordinate fields
    private JSpinner destXField;
    private JSpinner destYField;
    private JSpinner destPlaneField;

    // Transport details
    private JTextField actionField;
    private JSpinner objectIdField;

    // Requirements editor
    private RequirementsEditorPanel requirementsPanel;

    // Control flags
    private boolean isUpdating = false;

    public TransportDetailPanel(TransportEditorFrame parent) {
        this.parent = parent;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        clearSelection();
    }

    private void initializeComponents() {
        setBackground(BACKGROUND_COLOR);
        setBorder(new TitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            "Transport Details",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12),
            TEXT_COLOR
        ));

        // Initialize coordinate spinners
        sourceXField = createCoordinateSpinner();
        sourceYField = createCoordinateSpinner();
        sourcePlaneField = createPlaneSpinner();

        destXField = createCoordinateSpinner();
        destYField = createCoordinateSpinner();
        destPlaneField = createPlaneSpinner();

        // Initialize action field
        actionField = createStyledTextField();
        actionField.setToolTipText("Object interaction action (e.g., 'Climb-up', 'Open', 'Enter')");

        // Initialize object ID spinner
        objectIdField = createObjectIdSpinner();

        // Initialize requirements panel
        requirementsPanel = new RequirementsEditorPanel(this);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();

        // Add compact coordinate and details panel (fixed height)
        JPanel transportConfigPanel = createCompactCoordinatesAndDetailsPanel();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0; gbc.weighty = 0.0; // No vertical expansion
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 8, 0);
        mainPanel.add(transportConfigPanel, gbc);

        // Add requirements panel (takes remaining space)
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 1.0; // Takes all remaining vertical space
        gbc.insets = new Insets(0, 0, 0, 0);
        mainPanel.add(requirementsPanel, gbc);

        // Add to scroll pane
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createCompactCoordinatesAndDetailsPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(new TitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            "Transport Configuration",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("SansSerif", Font.PLAIN, 11),
            TEXT_COLOR
        ));

        // Create compact grid panel
        JPanel gridPanel = new JPanel(new GridBagLayout());
        gridPanel.setBackground(BACKGROUND_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 10);

        // Source coordinates row
        addCompactLabel(gridPanel, "Source:", 0, 0, gbc);
        addCompactField(gridPanel, sourceXField, 1, 0, gbc, 80);
        addCompactField(gridPanel, sourceYField, 2, 0, gbc, 80);
        addCompactField(gridPanel, sourcePlaneField, 3, 0, gbc, 60);
        addCompactLabel(gridPanel, "(X, Y, Plane)", 4, 0, gbc);

        // Destination coordinates row
        addCompactLabel(gridPanel, "Destination:", 0, 1, gbc);
        addCompactField(gridPanel, destXField, 1, 1, gbc, 80);
        addCompactField(gridPanel, destYField, 2, 1, gbc, 80);
        addCompactField(gridPanel, destPlaneField, 3, 1, gbc, 60);
        addCompactLabel(gridPanel, "(X, Y, Plane)", 4, 1, gbc);

        // Transport details row
        addCompactLabel(gridPanel, "Action:", 0, 2, gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gridPanel.add(actionField, gbc);

        addCompactLabel(gridPanel, "Object ID:", 3, 2, gbc);
        gbc.gridwidth = 1;
        addCompactField(gridPanel, objectIdField, 4, 2, gbc, 100);

        mainPanel.add(gridPanel);
        return mainPanel;
    }

    private void addCompactLabel(JPanel panel, String text, int x, int y, GridBagConstraints gbc) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_COLOR);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        gbc.gridx = x; gbc.gridy = y; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
        panel.add(label, gbc);
    }

    private void addCompactField(JPanel panel, JComponent field, int x, int y, GridBagConstraints gbc, int width) {
        field.setPreferredSize(new Dimension(width, 25));
        gbc.gridx = x; gbc.gridy = y; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
    }


    private JSpinner createCoordinateSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, -32768, 32767, 1));
        styleSpinner(spinner);
        return spinner;
    }

    private JSpinner createPlaneSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));
        styleSpinner(spinner);
        return spinner;
    }

    private JSpinner createObjectIdSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        styleSpinner(spinner);
        return spinner;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(FIELD_COLOR);
        spinner.setForeground(TEXT_COLOR);

        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setBackground(FIELD_COLOR);
            textField.setForeground(TEXT_COLOR);
            textField.setCaretColor(TEXT_COLOR);
            textField.setBorder(new EmptyBorder(3, 5, 3, 5));
        }
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setBackground(FIELD_COLOR);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setBorder(new EmptyBorder(5, 8, 5, 8));
        return field;
    }

    private void setupEventHandlers() {
        // Create change listeners for all fields
        ChangeListener changeListener = e -> {
            if (!isUpdating && currentTransport != null) {
                updateTransportFromFields();
            }
        };

        DocumentListener documentListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onChange(); }
            public void removeUpdate(DocumentEvent e) { onChange(); }
            public void changedUpdate(DocumentEvent e) { onChange(); }

            private void onChange() {
                if (!isUpdating && currentTransport != null) {
                    updateTransportFromFields();
                }
            }
        };

        // Add listeners to coordinate fields
        sourceXField.addChangeListener(changeListener);
        sourceYField.addChangeListener(changeListener);
        sourcePlaneField.addChangeListener(changeListener);

        destXField.addChangeListener(changeListener);
        destYField.addChangeListener(changeListener);
        destPlaneField.addChangeListener(changeListener);

        objectIdField.addChangeListener(changeListener);

        // Add listener to action field
        actionField.getDocument().addDocumentListener(documentListener);
    }

    // Public API

    public void displayTransport(TransportDto transport) {
        this.currentTransport = transport;
        isUpdating = true;

        if (transport != null) {
            updateFieldsFromTransport(transport);
            setFieldsEnabled(true);
        } else {
            clearFields();
            setFieldsEnabled(false);
        }

        isUpdating = false;
    }

    public void clearSelection() {
        displayTransport(null);
    }

    public void onRequirementsChanged() {
        if (!isUpdating && currentTransport != null) {
            updateTransportFromFields();
        }
    }

    // Private methods

    private void updateFieldsFromTransport(TransportDto transport) {
        // Update source coordinates
        sourceXField.setValue(transport.getSource().getX());
        sourceYField.setValue(transport.getSource().getY());
        sourcePlaneField.setValue(transport.getSource().getPlane());

        // Update destination coordinates
        destXField.setValue(transport.getDestination().getX());
        destYField.setValue(transport.getDestination().getY());
        destPlaneField.setValue(transport.getDestination().getPlane());

        // Update transport details
        actionField.setText(transport.getAction());
        objectIdField.setValue(transport.getObjectId());

        // Update requirements
        requirementsPanel.setRequirements(transport.getRequirements());
    }

    private void updateTransportFromFields() {
        if (currentTransport == null) return;

        try {
            // Create new WorldPoints
            WorldPoint source = new WorldPoint(
                (Integer) sourceXField.getValue(),
                (Integer) sourceYField.getValue(),
                (Integer) sourcePlaneField.getValue()
            );

            WorldPoint destination = new WorldPoint(
                (Integer) destXField.getValue(),
                (Integer) destYField.getValue(),
                (Integer) destPlaneField.getValue()
            );

            // Get requirements from panel
            Requirements requirements = requirementsPanel.getRequirements();

            // Create new TransportDto with updated values
            TransportDto updatedTransport = new TransportDto(
                source,
                destination,
                actionField.getText().trim(),
                (Integer) objectIdField.getValue(),
                requirements
            );

            // Notify parent to replace the transport in the list (since TransportDto is immutable)
            parent.onTransportUpdated(currentTransport, updatedTransport);

            // Update our reference to the new transport
            currentTransport = updatedTransport;

        } catch (Exception e) {
            e.printStackTrace();
            // Could show error dialog here
        }
    }

    private void clearFields() {
        sourceXField.setValue(0);
        sourceYField.setValue(0);
        sourcePlaneField.setValue(0);

        destXField.setValue(0);
        destYField.setValue(0);
        destPlaneField.setValue(0);

        actionField.setText("");
        objectIdField.setValue(0);

        requirementsPanel.clearRequirements();
    }

    private void setFieldsEnabled(boolean enabled) {
        sourceXField.setEnabled(enabled);
        sourceYField.setEnabled(enabled);
        sourcePlaneField.setEnabled(enabled);

        destXField.setEnabled(enabled);
        destYField.setEnabled(enabled);
        destPlaneField.setEnabled(enabled);

        actionField.setEnabled(enabled);
        objectIdField.setEnabled(enabled);

        requirementsPanel.setEnabled(enabled);
    }

    // Create type alias for change listener
    private interface ChangeListener extends javax.swing.event.ChangeListener {}
}
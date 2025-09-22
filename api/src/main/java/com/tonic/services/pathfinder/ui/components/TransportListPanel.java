package com.tonic.services.pathfinder.ui.components;

import com.tonic.services.pathfinder.model.TransportDto;
import com.tonic.services.pathfinder.ui.TransportEditorFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Panel containing the transport list with search and filtering capabilities
 */
public class TransportListPanel extends JPanel {

    private static final Color BACKGROUND_COLOR = new Color(60, 63, 65);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(85, 85, 85);

    private final TransportEditorFrame parent;
    private final DefaultListModel<TransportDto> listModel;
    private final JList<TransportDto> transportList;
    private final JTextField searchField;
    private final JComboBox<String> actionFilter;
    private final JLabel countLabel;

    private List<TransportDto> allTransports = new ArrayList<>();
    private List<TransportDto> filteredTransports = new ArrayList<>();
    private javax.swing.event.ListSelectionListener selectionListener;

    public TransportListPanel(TransportEditorFrame parent) {
        this.parent = parent;
        this.listModel = new DefaultListModel<>();
        this.transportList = new JList<>(listModel);
        this.searchField = new JTextField();
        this.actionFilter = new JComboBox<>();
        this.countLabel = new JLabel("0 transports");

        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        setBackground(BACKGROUND_COLOR);
        setBorder(new TitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            "Transports",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12),
            TEXT_COLOR
        ));

        // Configure search field
        searchField.setBackground(new Color(69, 73, 74));
        searchField.setForeground(TEXT_COLOR);
        searchField.setCaretColor(TEXT_COLOR);
        searchField.setBorder(new EmptyBorder(5, 8, 5, 8));
        searchField.setToolTipText("Search by coordinates, action, or object ID");

        // Configure action filter
        actionFilter.setBackground(new Color(69, 73, 74));
        actionFilter.setForeground(TEXT_COLOR);
        actionFilter.addItem("All Actions");

        // Configure transport list
        transportList.setBackground(new Color(69, 73, 74));
        transportList.setForeground(TEXT_COLOR);
        transportList.setSelectionBackground(new Color(75, 110, 175));
        transportList.setSelectionForeground(Color.WHITE);
        transportList.setCellRenderer(new TransportListCellRenderer());
        transportList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Configure count label
        countLabel.setForeground(TEXT_COLOR);
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Create header panel with search and filter
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Add scrollable list
        JScrollPane scrollPane = new JScrollPane(transportList);
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(new Color(69, 73, 74));
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        add(scrollPane, BorderLayout.CENTER);

        // Add footer with count and buttons
        JPanel footerPanel = createFooterPanel();
        add(footerPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(5, 5, 10, 5));

        GridBagConstraints gbc = new GridBagConstraints();

        // Search label
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(TEXT_COLOR);
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel.add(searchLabel, gbc);

        // Search field
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 5, 0);
        panel.add(searchField, gbc);

        // Filter label
        JLabel filterLabel = new JLabel("Action:");
        filterLabel.setForeground(TEXT_COLOR);
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel.add(filterLabel, gbc);

        // Action filter
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(actionFilter, gbc);

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(10, 5, 5, 5));

        // Count label on left
        panel.add(countLabel, BorderLayout.WEST);

        // Button panel on right
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setBackground(BACKGROUND_COLOR);

        JButton addButton = createStyledButton("Add", this::onAddTransport);
        JButton duplicateButton = createStyledButton("Duplicate", this::onDuplicateTransport);
        JButton deleteButton = createStyledButton("Delete", this::onDeleteTransport);

        buttonPanel.add(addButton);
        buttonPanel.add(Box.createHorizontalStrut(5));
        buttonPanel.add(duplicateButton);
        buttonPanel.add(Box.createHorizontalStrut(5));
        buttonPanel.add(deleteButton);

        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    private JButton createStyledButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setBackground(new Color(75, 110, 175));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(5, 10, 5, 10));
        button.addActionListener(action);
        return button;
    }

    private void setupEventHandlers() {
        // Search field listener
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
        });

        // Action filter listener
        actionFilter.addActionListener(e -> applyFilters());

        // Create and store the selection listener
        selectionListener = e -> {
            System.out.println("List selection event: valueIsAdjusting=" + e.getValueIsAdjusting());
            if (!e.getValueIsAdjusting()) {
                TransportDto selected = transportList.getSelectedValue();
                System.out.println("Selection changed to: " + (selected != null ? selected.getAction() : "null"));
                parent.onTransportSelected(selected);
                System.out.println("onTransportSelected completed");
            }
        };

        // Add the selection listener
        transportList.addListSelectionListener(selectionListener);

        // Double-click to edit
        transportList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TransportDto selected = transportList.getSelectedValue();
                    if (selected != null) {
                        parent.onTransportSelected(selected);
                    }
                }
            }
        });
    }

    // Public API

    public void refreshTransportList(List<TransportDto> transports) {
        System.out.println("refreshTransportList called with " + transports.size() + " transports");
        this.allTransports = new ArrayList<>(transports);
        System.out.println("allTransports updated, calling updateActionFilter...");
        updateActionFilter();
        System.out.println("updateActionFilter completed, calling applyFilters...");
        applyFilters();
        System.out.println("refreshTransportList completed");
    }

    public void selectTransport(TransportDto transport) {
        transportList.setSelectedValue(transport, true);
    }

    public void refreshCurrentSelection() {
        int selectedIndex = transportList.getSelectedIndex();
        if (selectedIndex >= 0) {
            // Force repaint of the selected item by temporarily removing and re-adding it
            TransportDto selectedTransport = listModel.getElementAt(selectedIndex);
            listModel.setElementAt(selectedTransport, selectedIndex);
        }
    }

    // Private methods

    private void updateActionFilter() {
        String selectedAction = (String) actionFilter.getSelectedItem();
        actionFilter.removeAllItems();
        actionFilter.addItem("All Actions");

        allTransports.stream()
            .map(TransportDto::getAction)
            .distinct()
            .sorted()
            .forEach(actionFilter::addItem);

        if (selectedAction != null) {
            actionFilter.setSelectedItem(selectedAction);
        }
    }

    private void applyFilters() {
        System.out.println("applyFilters called");
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedAction = (String) actionFilter.getSelectedItem();
        System.out.println("Search text: '" + searchText + "', Selected action: '" + selectedAction + "'");

        filteredTransports = allTransports.stream()
            .filter(transport -> matchesSearch(transport, searchText))
            .filter(transport -> matchesActionFilter(transport, selectedAction))
            .collect(Collectors.toList());

        System.out.println("Filtered to " + filteredTransports.size() + " transports, calling updateListModel...");
        updateListModel();
        System.out.println("updateListModel completed, calling updateCountLabel...");
        updateCountLabel();
        System.out.println("applyFilters completed");
    }

    private boolean matchesSearch(TransportDto transport, String searchText) {
        if (searchText.isEmpty()) {
            return true;
        }

        // Search in coordinates
        String sourceCoords = String.format("%d,%d,%d",
            transport.getSource().getX(),
            transport.getSource().getY(),
            transport.getSource().getPlane());

        String destCoords = String.format("%d,%d,%d",
            transport.getDestination().getX(),
            transport.getDestination().getY(),
            transport.getDestination().getPlane());

        if (sourceCoords.contains(searchText) || destCoords.contains(searchText)) {
            return true;
        }

        // Search in action
        if (transport.getAction().toLowerCase().contains(searchText)) {
            return true;
        }

        // Search in object ID
        if (transport.getObjectId().toString().contains(searchText)) {
            return true;
        }

        return false;
    }

    private boolean matchesActionFilter(TransportDto transport, String selectedAction) {
        return "All Actions".equals(selectedAction) ||
               transport.getAction().equals(selectedAction);
    }

    private void updateListModel() {
        System.out.println("updateListModel called with " + filteredTransports.size() + " elements");

        // Temporarily remove listener to prevent events during update
        transportList.removeListSelectionListener(selectionListener);

        try {
            // Use setListData for better performance with large datasets
            TransportDto[] transportArray = filteredTransports.toArray(new TransportDto[0]);
            System.out.println("Setting list data with " + transportArray.length + " elements...");
            transportList.setListData(transportArray);
            System.out.println("List data set successfully");
        } finally {
            // Always re-add the listener, even if there's an exception
            transportList.addListSelectionListener(selectionListener);
            System.out.println("Selection listener re-added");
        }

        System.out.println("updateListModel completed");
    }

    private void updateCountLabel() {
        int total = allTransports.size();
        int filtered = filteredTransports.size();

        if (total == filtered) {
            countLabel.setText(total + " transports");
        } else {
            countLabel.setText(filtered + " of " + total + " transports");
        }
    }

    // Button event handlers

    private void onAddTransport(ActionEvent e) {
        parent.addNewTransport();
    }

    private void onDuplicateTransport(ActionEvent e) {
        TransportDto selected = transportList.getSelectedValue();
        if (selected != null) {
            parent.duplicateTransport(selected);
        } else {
            JOptionPane.showMessageDialog(this,
                "Please select a transport to duplicate.",
                "No Selection",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onDeleteTransport(ActionEvent e) {
        System.out.println("onDeleteTransport called from UI button");

        TransportDto selected = transportList.getSelectedValue();
        System.out.println("Selected transport: " + (selected != null ? selected.getAction() : "null"));

        if (selected != null) {
            System.out.println("Calling parent.deleteTransport...");
            parent.deleteTransport(selected);
            System.out.println("parent.deleteTransport returned");
        } else {
            System.out.println("No transport selected, showing message dialog");
            JOptionPane.showMessageDialog(this,
                "Please select a transport to delete.",
                "No Selection",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // Custom cell renderer for transport list
    private static class TransportListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof TransportDto) {
                TransportDto transport = (TransportDto) value;

                String displayText = String.format(
                    "<html><div style='font-size: 11px;'><b>%s</b> (ID: %d)<br>" +
                    "<span style='font-size: 10px; color: #CCCCCC;'>%d,%d,%d to %d,%d,%d</span></div></html>",
                    transport.getAction(),
                    transport.getObjectId(),
                    transport.getSource().getX(),
                    transport.getSource().getY(),
                    transport.getSource().getPlane(),
                    transport.getDestination().getX(),
                    transport.getDestination().getY(),
                    transport.getDestination().getPlane()
                );

                setText(displayText);
            }

            return this;
        }
    }
}
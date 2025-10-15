package com.tonic.model.ui;

import com.tonic.model.PlatformInfoData;
import com.tonic.model.ui.components.VitaFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Map;

public class PlatformInfoViewer extends VitaFrame {

    private static PlatformInfoViewer instance;
    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel statusLabel;

    private PlatformInfoViewer() {
        super("Platform Information Viewer");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        // IMPORTANT: Get the content panel from VitaFrame
        JPanel contentPanel = getContentPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBackground(new Color(35, 45, 60));

        // Create table model with non-editable cells
        tableModel = new DefaultTableModel(new Object[]{"Field Name", "Value"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create table with custom row height calculation
        table = new JTable(tableModel);

        // Set custom cell renderer for wrapping text
        MultiLineCellRenderer multiLineRenderer = new MultiLineCellRenderer();
        table.setDefaultRenderer(Object.class, multiLineRenderer);

        // Styling - Dark blue/gray theme
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(40, 60, 90));  // Dark blue
        table.getTableHeader().setForeground(Color.WHITE);
        table.setShowGrid(true);
        table.setGridColor(new Color(60, 80, 110));  // Medium blue-gray
        table.setSelectionBackground(new Color(70, 110, 160));  // Bright blue
        table.setSelectionForeground(Color.WHITE);

        // Set default row height
        table.setRowHeight(30);

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(250);
        table.getColumnModel().getColumn(1).setPreferredWidth(550);

        // Add table to scroll pane with dark theme
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scrollPane.getViewport().setBackground(new Color(45, 55, 70));
        scrollPane.setBackground(new Color(35, 45, 60));

        // Add a status bar at the bottom with dark theme
        statusLabel = new JLabel(" Ready", SwingConstants.LEFT);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(200, 210, 220));  // Light gray text
        statusLabel.setBackground(new Color(30, 40, 55));  // Dark background
        statusLabel.setOpaque(true);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 80, 110)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // IMPORTANT: Add components to the content panel, not directly to frame
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(statusLabel, BorderLayout.SOUTH);
    }

    /**
     * Get singleton instance
     */
    public static PlatformInfoViewer getInstance() {
        if (instance == null) {
            instance = new PlatformInfoViewer();
        }
        return instance;
    }

    /**
     * Static method to show the viewer with platform info data
     */
    public static void toggle() {
        SwingUtilities.invokeLater(() -> {
            if(instance != null && instance.isVisible()) {
                instance.setVisible(false);
                return;
            }

            if (instance == null) {
                instance = new PlatformInfoViewer();
            }
            instance.updateData(PlatformInfoData.getPlatformInfo());
            instance.setVisible(true);
            instance.toFront();
            instance.requestFocus();
        });
    }

    /**
     * Post update from PlatformInfoData (called when data changes)
     */
    public void postUpdate(Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            updateData(data);
            if (isVisible()) {
                repaint();
            }
        });
    }

    /**
     * Update the table with new data
     * @param data Map containing field names and values
     */
    private void updateData(Map<String, String> data) {
        // Clear existing data
        tableModel.setRowCount(0);

        if (data == null || data.isEmpty()) {
            tableModel.addRow(new Object[]{"No Data", "No platform information available"});
            adjustRowHeights();
            statusLabel.setText(" No data available");
            return;
        }

        // Sort entries by key (field name) and add to table
        data.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue() != null ? entry.getValue() : "null";
                    tableModel.addRow(new Object[]{key, value});
                });

        // Adjust row heights for multi-line content
        adjustRowHeights();

        // Update status bar
        statusLabel.setText(" Displaying " + data.size() + " field(s)");
    }

    /**
     * Adjust row heights based on cell content
     */
    private void adjustRowHeights() {
        for (int row = 0; row < table.getRowCount(); row++) {
            int maxHeight = table.getRowHeight(); // Default height

            for (int column = 0; column < table.getColumnCount(); column++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);

                // Get the column width
                TableColumn tableColumn = table.getColumnModel().getColumn(column);
                int columnWidth = tableColumn.getWidth();

                // Calculate required height for the content
                comp.setSize(columnWidth, Short.MAX_VALUE);
                int preferredHeight = comp.getPreferredSize().height;

                // Track maximum height needed for this row
                maxHeight = Math.max(maxHeight, preferredHeight);
            }

            // Set the row height
            table.setRowHeight(row, maxHeight);
        }
    }

    /**
     * Custom cell renderer for multi-line text support
     */
    private static class MultiLineCellRenderer extends JTextArea implements TableCellRenderer {

        public MultiLineCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                // Alternating dark row colors with good contrast
                setBackground(row % 2 == 0 ? new Color(45, 55, 70) : new Color(55, 65, 85));
                setForeground(Color.WHITE);  // White text for visibility
            }

            setFont(table.getFont());
            setText(value != null ? value.toString() : "");

            // Calculate the required height for this cell
            setSize(table.getColumnModel().getColumn(column).getWidth(), Short.MAX_VALUE);

            return this;
        }
    }
}
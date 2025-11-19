package com.tonic.services.profiler.timeline;

import com.tonic.services.profiler.gc.GCPauseAnalyzer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Timeline visualization panel showing JVM events
 */
public class TimelinePanel extends JPanel {
    private static final Color BG_COLOR = new Color(30, 31, 34);
    private static final Color PANEL_BG = new Color(40, 42, 46);
    private static final Color TEXT_COLOR = new Color(200, 200, 200);
    private static final Color ACCENT_COLOR = new Color(64, 156, 255);

    private final TimelineCollector collector;
    private JTable eventTable;
    private DefaultTableModel tableModel;
    private JTextArea statsArea;
    private JTextArea detailsArea;
    private Timer refreshTimer;

    public TimelinePanel(GCPauseAnalyzer gcAnalyzer) {
        this.collector = new TimelineCollector(500, gcAnalyzer);

        setLayout(new BorderLayout(10, 10));
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initializeComponents();
        startAutoRefresh();
    }

    private void initializeComponents() {
        // Top - Statistics Panel
        JPanel statsPanel = createStyledPanel("Timeline Statistics");
        statsPanel.setLayout(new BorderLayout());
        statsPanel.setPreferredSize(new Dimension(0, 100));

        statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setBackground(PANEL_BG);
        statsArea.setForeground(TEXT_COLOR);
        statsArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        statsArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        statsArea.setText("Collecting events...");

        JScrollPane statsScroll = new JScrollPane(statsArea);
        statsScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        statsPanel.add(statsScroll, BorderLayout.CENTER);

        add(statsPanel, BorderLayout.NORTH);

        // Center - Event Table and Details
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setBackground(BG_COLOR);
        centerSplit.setDividerLocation(600);

        // Left - Event Table
        JPanel tablePanel = createStyledPanel("Event History");
        tablePanel.setLayout(new BorderLayout());

        String[] columns = {"Time", "Type", "Description", "Duration", "Severity"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        eventTable = new JTable(tableModel);
        eventTable.setBackground(PANEL_BG);
        eventTable.setForeground(TEXT_COLOR);
        eventTable.setFont(new Font("Consolas", Font.PLAIN, 11));
        eventTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eventTable.setGridColor(new Color(50, 52, 56));
        eventTable.setRowHeight(22);
        eventTable.getTableHeader().setBackground(new Color(50, 52, 56));
        eventTable.getTableHeader().setForeground(TEXT_COLOR);

        eventTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showEventDetails();
            }
        });

        JScrollPane tableScroll = new JScrollPane(eventTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        tablePanel.add(tableScroll, BorderLayout.CENTER);

        // Right - Event Details
        JPanel detailsPanel = createStyledPanel("Event Details");
        detailsPanel.setLayout(new BorderLayout());

        detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setBackground(PANEL_BG);
        detailsArea.setForeground(TEXT_COLOR);
        detailsArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        detailsArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setText("Select an event to view details");

        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        detailsScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 56)));
        detailsPanel.add(detailsScroll, BorderLayout.CENTER);

        centerSplit.setLeftComponent(tablePanel);
        centerSplit.setRightComponent(detailsPanel);

        add(centerSplit, BorderLayout.CENTER);
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer(2000, e -> refreshTimeline());
        refreshTimer.start();
    }

    private void refreshTimeline() {
        SwingUtilities.invokeLater(() -> {
            collector.collectEvents();
            updateStatistics();
            updateEventTable();
        });
    }

    private void updateStatistics() {
        TimelineCollector.TimelineStatistics stats = collector.getStatistics();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total Events: %d  |  GC: %d  |  Compilation: %d  |  Thread: %d  |  ",
            stats.totalEvents, stats.gcEvents, stats.compilationEvents, stats.threadEvents));

        if (stats.totalEvents > 0) {
            sb.append(String.format("Time Span: %.1f seconds  |  Longest Event: %d ms",
                stats.timeSpan / 1000.0, stats.longestEvent));
        } else {
            sb.append("No events captured yet");
        }

        statsArea.setText(sb.toString());
    }

    private void updateEventTable() {
        List<TimelineEvent> events = collector.getAllEvents();

        // Store selected row
        int selectedRow = eventTable.getSelectedRow();

        // Clear table
        tableModel.setRowCount(0);

        // Add events (most recent first)
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        for (int i = events.size() - 1; i >= 0; i--) {
            TimelineEvent event = events.get(i);

            String time = sdf.format(new Date(event.getTimestamp()));
            String type = event.getType().getLabel();
            String description = event.getDescription();
            String duration = event.getDuration() > 0 ? event.getDuration() + " ms" : "-";
            String severity = event.getSeverity().getLabel();

            tableModel.addRow(new Object[]{time, type, description, duration, severity});
        }

        // Restore selection
        if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
            eventTable.setRowSelectionInterval(selectedRow, selectedRow);
        }
    }

    private void showEventDetails() {
        int selectedRow = eventTable.getSelectedRow();
        if (selectedRow < 0) {
            detailsArea.setText("Select an event to view details");
            return;
        }

        List<TimelineEvent> events = collector.getAllEvents();
        int eventIndex = events.size() - 1 - selectedRow;

        if (eventIndex >= 0 && eventIndex < events.size()) {
            TimelineEvent event = events.get(eventIndex);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

            StringBuilder sb = new StringBuilder();
            sb.append("Event Type: ").append(event.getType().getLabel()).append("\n");
            sb.append("Description: ").append(event.getType().getDescription()).append("\n\n");
            sb.append("Timestamp: ").append(sdf.format(new Date(event.getTimestamp()))).append("\n");
            sb.append("Duration: ").append(event.getDuration()).append(" ms\n");
            sb.append("Severity: ").append(event.getSeverity().getLabel()).append("\n\n");
            sb.append("Details:\n");
            sb.append(event.getDetails());

            detailsArea.setText(sb.toString());
            detailsArea.setCaretPosition(0);
        }
    }

    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
    }

    private JPanel createStyledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL_BG);
        javax.swing.border.TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(50, 52, 56)),
            title
        );
        border.setTitleColor(TEXT_COLOR);
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.setBorder(BorderFactory.createCompoundBorder(border, new EmptyBorder(10, 10, 10, 10)));
        return panel;
    }
}

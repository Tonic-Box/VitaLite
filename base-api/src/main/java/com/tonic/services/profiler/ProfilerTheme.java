package com.tonic.services.profiler;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * The profiler's colours, fonts and the styled components its tabs are built from
 */
public final class ProfilerTheme {
    public static final Color BACKGROUND = new Color(30, 31, 34);
    public static final Color PANEL = new Color(40, 42, 46);
    public static final Color BORDER = new Color(50, 52, 56);
    public static final Color GRID = new Color(60, 62, 66);
    public static final Color TEXT = new Color(200, 200, 200);
    public static final Color MUTED = new Color(150, 150, 150);
    public static final Color ACCENT = new Color(64, 156, 255);
    public static final Color SUCCESS = new Color(76, 175, 80);
    public static final Color WARNING = new Color(255, 152, 0);
    public static final Color ERROR = new Color(244, 67, 54);
    public static final Color NEUTRAL = new Color(100, 100, 100);

    public static final Font UI = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font UI_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font UI_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font MONO = new Font("Consolas", Font.PLAIN, 11);

    private ProfilerTheme() {
    }

    /**
     * Creates a page panel with the standard padding
     * @param layout the panel's layout
     * @return the panel
     */
    public static JPanel page(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        return panel;
    }

    /**
     * Creates a panel with a titled border
     * @param title the border's title
     * @return the panel
     */
    public static JPanel section(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL);
        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER), title);
        border.setTitleColor(TEXT);
        border.setTitleFont(UI_BOLD);
        panel.setBorder(BorderFactory.createCompoundBorder(border, new EmptyBorder(10, 10, 10, 10)));
        return panel;
    }

    /**
     * Creates a label in the text colour
     * @param text the label's text
     * @return the label
     */
    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(UI);
        return label;
    }

    /**
     * Creates a flat coloured button
     * @param text the button's text
     * @param color its background colour
     * @return the button
     */
    public static JButton button(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(130, 30));
        return button;
    }

    /**
     * Styles a text field to match the profiler
     * @param field the field to style
     * @return the field
     */
    public static JTextField field(JTextField field) {
        field.setBackground(PANEL);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), new EmptyBorder(5, 5, 5, 5)));
        return field;
    }

    /**
     * Creates a read-only text area
     * @param font the area's font
     * @return the text area
     */
    public static JTextArea textArea(Font font) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setBackground(PANEL);
        area.setForeground(TEXT);
        area.setFont(font);
        area.setBorder(new EmptyBorder(5, 5, 5, 5));
        return area;
    }

    /**
     * Sets a text area's text, keeping its scroll position when the text is unchanged
     * @param area the text area
     * @param text the new text
     */
    public static void setText(JTextArea area, String text) {
        if (text.equals(area.getText())) {
            return;
        }
        area.setText(text);
        area.setCaretPosition(0);
    }

    /**
     * Wraps a component in a bordered scroll pane
     * @param component the component to scroll
     * @return the scroll pane
     */
    public static JScrollPane scroll(Component component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(PANEL);
        return scroll;
    }

    /**
     * Styles a table to match the profiler
     * @param table the table to style
     * @return the table
     */
    public static JTable table(JTable table) {
        table.setBackground(PANEL);
        table.setForeground(TEXT);
        table.setGridColor(BORDER);
        table.setSelectionBackground(ACCENT.darker());
        table.setSelectionForeground(Color.WHITE);
        table.setFont(UI);
        table.setRowHeight(22);
        JTableHeader header = table.getTableHeader();
        header.setBackground(BACKGROUND);
        header.setForeground(TEXT);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return table;
    }

    /**
     * Creates a time series line chart
     * @param title the chart's title
     * @param axis the value axis label
     * @param dataset the series to draw
     * @param colors one colour per series, in order
     * @return the chart in a panel
     */
    public static ChartPanel timeChart(String title, String axis, TimeSeriesCollection dataset, Color... colors) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "Time", axis, dataset, true, true, false);
        chart.setBackgroundPaint(PANEL);
        chart.getTitle().setPaint(TEXT);
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 14));
        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(PANEL);
            chart.getLegend().setItemPaint(TEXT);
        }

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(PANEL);
        plot.setDomainGridlinePaint(GRID);
        plot.setRangeGridlinePaint(GRID);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        for (int index = 0; index < colors.length; index++) {
            renderer.setSeriesPaint(index, colors[index]);
        }
        plot.setRenderer(renderer);

        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setTickLabelPaint(TEXT);
        range.setLabelPaint(TEXT);
        DateAxis domain = (DateAxis) plot.getDomainAxis();
        domain.setTickLabelPaint(TEXT);
        domain.setLabelPaint(TEXT);

        ChartPanel panel = new ChartPanel(chart);
        panel.setBackground(PANEL);
        panel.setPreferredSize(new Dimension(400, 200));
        return panel;
    }

    /**
     * Formats a byte count with a binary unit
     * @param bytes the byte count
     * @return the count as B, KB, MB or GB
     */
    public static String formatBytes(long bytes) {
        long magnitude = Math.abs(bytes);
        if (magnitude < 1024) {
            return bytes + " B";
        }
        if (magnitude < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (magnitude < 1024L * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

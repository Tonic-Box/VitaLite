package com.tonic.services.profiler.recording;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Locale;

import static com.tonic.services.profiler.ProfilerTheme.*;

/**
 * Shared helpers for the Recording tab's views
 */
final class RecordingViews {
    static final Color HOT = new Color(244, 67, 54, 60);
    static final Color WARM = new Color(255, 152, 0, 60);
    static final Color MILD = new Color(255, 235, 59, 40);

    private RecordingViews() {
    }

    /**
     * Creates a table model whose cells cannot be edited
     * @param types the class of each column, for sorting
     * @param columns the column names
     * @return the model
     */
    static DefaultTableModel model(Class<?>[] types, String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return types[column];
            }
        };
    }

    /**
     * Gets a background that marks how much of the recording a share is
     * @param percent the share in percent
     * @return a translucent colour, or null below one percent
     */
    static Color heat(double percent) {
        return percent >= 10 ? HOT : percent >= 5 ? WARM : percent >= 1 ? MILD : null;
    }

    /**
     * Gets an opaque row background for a share, the heat colour blended over the panel colour
     * @param percent the share in percent
     * @return the background
     */
    static Color heatBackground(double percent) {
        Color overlay = heat(percent);
        if (overlay == null) {
            return PANEL;
        }
        float alpha = overlay.getAlpha() / 255f;
        return new Color(Math.round(overlay.getRed() * alpha + PANEL.getRed() * (1 - alpha)), Math.round(overlay.getGreen() * alpha + PANEL.getGreen() * (1 - alpha)), Math.round(overlay.getBlue() * alpha + PANEL.getBlue() * (1 - alpha)));
    }

    /**
     * Creates a renderer that shows a percentage value right-aligned with a % sign
     * @return the renderer
     */
    static DefaultTableCellRenderer percentRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(value instanceof Double ? String.format("%.1f%%", (Double) value) : String.valueOf(value));
            }
        };
        renderer.setHorizontalAlignment(SwingConstants.RIGHT);
        return renderer;
    }

    /**
     * Describes how far a sample count can be trusted
     * @param samples the samples behind a number
     * @return HIGH, MEDIUM, LOW or VERY LOW
     */
    static String confidence(long samples) {
        return samples >= 100 ? "HIGH" : samples >= 30 ? "MEDIUM" : samples >= 10 ? "LOW" : "VERY LOW";
    }

    /**
     * Shortens a class and method name to the simple class name and method
     * @param methodKey a name such as java.lang.String.indexOf
     * @return the short name, such as String.indexOf
     */
    static String shortName(String methodKey) {
        int method = methodKey.lastIndexOf('.');
        if (method <= 0) {
            return methodKey;
        }
        int type = methodKey.lastIndexOf('.', method - 1);
        return methodKey.substring(type + 1);
    }

    /**
     * Gets a percentage of a total
     * @param part the part
     * @param total the total
     * @return the part as a percentage, 0 when the total is 0
     */
    static double percent(long part, long total) {
        return total == 0 ? 0 : part * 100.0 / total;
    }

    /**
     * Creates a page panel for a view
     * @return the panel
     */
    static JPanel viewPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return panel;
    }

    /**
     * Runs an action whenever a text field's text changes
     * @param field the field
     * @param action the action
     */
    static void onChange(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        });
    }

    /**
     * Builds a row filter that keeps rows whose first column contains some text
     * @param text the text, ignoring case
     * @return the filter, or null for no filtering
     */
    static RowFilter<Object, Object> nameFilter(String text) {
        String needle = text.trim().toLowerCase(Locale.ROOT);
        return needle.isEmpty() ? null : new RowFilter<Object, Object>() {
            @Override
            public boolean include(Entry<?, ?> entry) {
                return entry.getStringValue(0).toLowerCase(Locale.ROOT).contains(needle);
            }
        };
    }
}

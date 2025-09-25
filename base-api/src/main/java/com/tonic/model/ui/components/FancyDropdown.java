package com.tonic.model.ui.components;

import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class FancyDropdown<T> extends JPanel {
    private final JComboBox<T> comboBox;
    private static final Color CARD_BACKGROUND = new Color(55, 55, 60);
    private static final Color BACKGROUND_COLOR = new Color(55, 55, 60);
    private static final Color HOVER_COLOR = new Color(64, 169, 211, 50);
    private static final Color SELECTED_COLOR = new Color(64, 169, 211);
    private static final Color TEXT_COLOR = new Color(200, 200, 205);
    private static final Color HEADER_COLOR = new Color(64, 169, 211);
    private static final Color BORDER_COLOR = new Color(70, 70, 75);
    private static final Color POPUP_BACKGROUND = new Color(45, 45, 50);
    private static final Color SEPARATOR_COLOR = new Color(70, 70, 75);

    @Getter
    private T selectedValue;
    private boolean isHovered = false;
    private JLabel titleLabel;

    public FancyDropdown(String title, List<String> items) {
        super();
        comboBox = new JComboBox<>();
        for (String item : items) {
            comboBox.addItem((T) item);
        }
        initializeStyle(title);
    }

    public FancyDropdown(String title, Class<? extends Enum<?>> enumClass) {
        super();
        comboBox = new JComboBox<>();
        if (enumClass.isEnum()) {
            Enum<?>[] enumConstants = (Enum<?>[]) enumClass.getEnumConstants();
            for (Enum<?> enumConstant : enumConstants) {
                comboBox.addItem((T) enumConstant);
            }
        }
        initializeStyle(title);
    }

    public FancyDropdown(String title, T[] items) {
        super();
        comboBox = new JComboBox<>(items);
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

        // Style the combo box
        comboBox.setMaximumSize(new Dimension(VPluginPanel.PANEL_WIDTH - 44, 26));
        comboBox.setPreferredSize(new Dimension(VPluginPanel.PANEL_WIDTH - 44, 26));
        comboBox.setBackground(BACKGROUND_COLOR);
        comboBox.setForeground(new Color(245, 245, 250));
        comboBox.setFocusable(true);
        comboBox.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        comboBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        comboBox.setUI(new FancyComboBoxUI());
        comboBox.setRenderer(new FancyComboBoxRenderer());

        comboBox.addActionListener(e -> selectedValue = (T) comboBox.getSelectedItem());

        add(comboBox);

        add(Box.createVerticalStrut(4));

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

        comboBox.addMouseListener(new MouseAdapter() {
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

    public void addSelectionListener(ActionListener listener) {
        comboBox.addActionListener(listener);
    }

    public void setSelectedItem(Object item) {
        comboBox.setSelectedItem(item);
        selectedValue = (T) item;
    }

    public T getSelectedItem() {
        return (T) comboBox.getSelectedItem();
    }

    public void addItem(T item) {
        comboBox.addItem(item);
    }

    public void removeItem(T item) {
        comboBox.removeItem(item);
    }

    public void setEnabled(boolean enabled) {
        comboBox.setEnabled(enabled);
        titleLabel.setForeground(enabled ? HEADER_COLOR : new Color(100, 100, 105));
    }

    private static class FancyComboBoxUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton button = new JButton();
            button.setBackground(BACKGROUND_COLOR);
            button.setBorder(BorderFactory.createEmptyBorder());
            button.setFocusPainted(false);
            button.setContentAreaFilled(false);
            return button;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color bgColor = BACKGROUND_COLOR;
            if (hasFocus) {
                bgColor = new Color(
                    Math.min(255, BACKGROUND_COLOR.getRed() + 10),
                    Math.min(255, BACKGROUND_COLOR.getGreen() + 10),
                    Math.min(255, BACKGROUND_COLOR.getBlue() + 10)
                );
            }

            g2d.setColor(bgColor);
            g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 5, 5);
        }

        @Override
        protected ComboPopup createPopup() {
            return new FancyComboPopup(comboBox);
        }
    }

    private static class FancyComboBoxRenderer extends BasicComboBoxRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            if (isSelected) {
                setBackground(SELECTED_COLOR);
                setForeground(Color.WHITE);
            } else {
                setBackground(POPUP_BACKGROUND);
                setForeground(TEXT_COLOR);
            }

            return this;
        }
    }

    private static class FancyComboPopup extends javax.swing.plaf.basic.BasicComboPopup {
        public FancyComboPopup(JComboBox combo) {
            super(combo);
        }

        @Override
        protected void configurePopup() {
            super.configurePopup();
            setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
            setBackground(POPUP_BACKGROUND);
        }

        @Override
        protected void configureList() {
            super.configureList();
            list.setBackground(POPUP_BACKGROUND);
            list.setForeground(TEXT_COLOR);
            list.setSelectionBackground(SELECTED_COLOR);
            list.setSelectionForeground(Color.WHITE);
            list.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }
    }
}
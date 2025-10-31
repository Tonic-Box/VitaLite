package com.tonic.model.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CollapsiblePanel extends JPanel {
    private static final Color HEADER_BACKGROUND = new Color(55, 55, 60);
    private static final Color HEADER_HOVER = new Color(65, 65, 70);
    private static final Color HEADER_TEXT = new Color(245, 245, 250);
    private static final Color ACCENT_COLOR = new Color(64, 169, 211);
    private static final Color BORDER_COLOR = new Color(70, 70, 75);

    private final JPanel headerPanel;
    private final JPanel contentPanel;
    private final JLabel titleLabel;
    private final JLabel arrowLabel;
    private boolean expanded = false;

    public CollapsiblePanel(String title) {
        setLayout(new BorderLayout());
        setOpaque(false);
        setMaximumSize(new Dimension(VPluginPanel.PANEL_WIDTH - 20, Integer.MAX_VALUE));

        // Header panel
        headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bgColor = getMousePosition() != null ? HEADER_HOVER : HEADER_BACKGROUND;
                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                // Bottom border accent
                g2d.setColor(expanded ? ACCENT_COLOR : BORDER_COLOR);
                g2d.fillRoundRect(0, getHeight() - 2, getWidth(), 2, 2, 2);
            }
        };
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setPreferredSize(new Dimension(VPluginPanel.PANEL_WIDTH - 20, 40));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        headerPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Title
        titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(HEADER_TEXT);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Arrow indicator
        arrowLabel = new JLabel(">");
        arrowLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        arrowLabel.setForeground(ACCENT_COLOR);
        headerPanel.add(arrowLabel, BorderLayout.EAST);

        // Content panel
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        contentPanel.setVisible(false);

        // Click listener
        headerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                toggleExpanded();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                headerPanel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                headerPanel.repaint();
            }
        });

        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    public void toggleExpanded() {
        expanded = !expanded;
        contentPanel.setVisible(expanded);
        arrowLabel.setText(expanded ? "^" : ">");
        headerPanel.repaint();

        // Trigger layout update
        revalidate();
        repaint();

        // Update parent container
        Container parent = getParent();
        while (parent != null) {
            parent.revalidate();
            parent.repaint();
            parent = parent.getParent();
        }
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded != expanded) {
            toggleExpanded();
        }
    }

    public void addContent(Component component) {
        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent) component;
            jComponent.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Adjust sizing for components that need to fit within the collapsible panel
            Dimension maxSize = jComponent.getMaximumSize();
            Dimension prefSize = jComponent.getPreferredSize();
            if (maxSize != null && maxSize.width > 0) {
                int availableWidth = VPluginPanel.PANEL_WIDTH - 50;
                jComponent.setMaximumSize(new Dimension(availableWidth, maxSize.height));
                if (prefSize != null) {
                    jComponent.setPreferredSize(new Dimension(availableWidth, prefSize.height));
                }
            }
        }
        contentPanel.add(component);
    }

    public void addVerticalStrut(int height) {
        contentPanel.add(Box.createVerticalStrut(height));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension headerSize = headerPanel.getPreferredSize();
        if (expanded) {
            Dimension contentSize = contentPanel.getPreferredSize();
            return new Dimension(
                VPluginPanel.PANEL_WIDTH - 20,
                headerSize.height + contentSize.height
            );
        }
        return new Dimension(VPluginPanel.PANEL_WIDTH - 20, headerSize.height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(VPluginPanel.PANEL_WIDTH - 20, getPreferredSize().height);
    }
}

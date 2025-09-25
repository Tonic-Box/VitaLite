package com.tonic.model.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class OptionPanel extends JPanel
{
    private final Color CARD_BACKGROUND = new Color(55, 55, 60);
    private final Color SEPARATOR_COLOR = new Color(70, 70, 75);
    private final Color TEXT_COLOR = new Color(200, 200, 205);
    private final Color HEADER_COLOR = new Color(64, 169, 211);
    private boolean isHovered = false;

    public OptionPanel()
    {
        setLayout(new BorderLayout(10, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(5, 8, 5, 8));
        setMaximumSize(new Dimension(VPluginPanel.PANEL_WIDTH - 20, 40));
    }

    public void init(String title, String description, ToggleSlider toggle, Runnable onClick)
    {
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(HEADER_COLOR);
        textPanel.add(titleLabel);

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        descLabel.setForeground(TEXT_COLOR);
        textPanel.add(descLabel);

        add(textPanel, BorderLayout.CENTER);
        add(toggle, BorderLayout.EAST);

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

        toggle.addMouseListener(new MouseAdapter() {
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

        toggle.addActionListener(e -> onClick.run());
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
}

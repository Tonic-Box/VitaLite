package com.tonic.model.ui.componants;

import javax.swing.*;
import java.awt.*;

public class FancyCard extends JPanel {
    private static final Color BACKGROUND_GRADIENT_START = new Color(45, 45, 50);
    private static final Color BACKGROUND_GRADIENT_END = new Color(35, 35, 40);
    private static final Color HEADER_COLOR = new Color(245, 245, 250);
    private static final Color ACCENT_COLOR = new Color(64, 169, 211);
    private static final Color ACCENT_GLOW = new Color(64, 169, 211, 30);
    private static final Color CARD_BACKGROUND = new Color(55, 55, 60);
    private final JLabel titleLabel;
    private final JLabel taglineLabel;
    private final JPanel contentPanel;

    public FancyCard(String title, String subText) {
        setOpaque(false);
        setLayout(new BorderLayout());

        // Inner panel for content with padding
        contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(HEADER_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(titleLabel);

        contentPanel.add(Box.createVerticalStrut(8));

        taglineLabel = new JLabel();
        taglineLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        taglineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        taglineLabel.setHorizontalAlignment(SwingConstants.CENTER);
        setTaglineText(subText);
        contentPanel.add(taglineLabel);

        add(contentPanel, BorderLayout.CENTER);

        // Set maximum width but let height be determined by content
        setMaximumSize(new Dimension(VPluginPanel.PANEL_WIDTH - 20, Integer.MAX_VALUE));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(ACCENT_GLOW);
        int glowRadius = 20;
        for (int i = glowRadius; i > 0; i--) {
            float alpha = (float)(glowRadius - i) / glowRadius * 0.3f;
            g2d.setColor(new Color(64, 169, 211, (int)(alpha * 255)));
            g2d.fillRoundRect(i/2, i/2, getWidth() - i, getHeight() - i, 15, 15);
        }

        g2d.setColor(CARD_BACKGROUND);
        g2d.fillRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 10, 10);
    }

    @Override
    public Dimension getPreferredSize() {
        // Calculate preferred size based on content
        Dimension contentSize = contentPanel.getPreferredSize();
        // Add some padding for the glow effect
        return new Dimension(
                VPluginPanel.PANEL_WIDTH - 20,
                contentSize.height + 10  // Extra padding for visual balance
        );
    }

    public void setTaglineText(String text) {
        // Adjust width to account for padding
        String wrappedText = String.format(
                "<html><div style='text-align: center; width: %dpx; color: rgb(%d,%d,%d);'>%s</div></html>",
                VPluginPanel.PANEL_WIDTH - 100, // Account for padding on both sides
                ACCENT_COLOR.getRed(),
                ACCENT_COLOR.getGreen(),
                ACCENT_COLOR.getBlue(),
                text
        );
        taglineLabel.setText(wrappedText);
        revalidate();
    }
}
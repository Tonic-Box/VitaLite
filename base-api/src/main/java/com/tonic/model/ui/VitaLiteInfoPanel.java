package com.tonic.model.ui;

import com.tonic.Static;
import com.tonic.model.ui.components.VPluginPanel;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class VitaLiteInfoPanel extends VPluginPanel
{
    private static final Color BACKGROUND_GRADIENT_START = new Color(45, 45, 50);
    private static final Color BACKGROUND_GRADIENT_END = new Color(35, 35, 40);
    private static final Color ACCENT_COLOR = new Color(64, 169, 211);
    private static final Color ACCENT_GLOW = new Color(64, 169, 211, 30);
    private static final Color LINK_COLOR = new Color(64, 169, 211);
    private static final Color LINK_HOVER_COLOR = new Color(84, 189, 231);
    private static final Color TEXT_COLOR = new Color(200, 200, 205);
    private static final Color HEADER_COLOR = new Color(245, 245, 250);
    private static final Color CARD_BACKGROUND = new Color(55, 55, 60);
    private static final Color SEPARATOR_COLOR = new Color(70, 70, 75);

    public VitaLiteInfoPanel()
    {
        super(true);

        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gradient = new GradientPaint(
                        0, 0, BACKGROUND_GRADIENT_START,
                        0, getHeight(), BACKGROUND_GRADIENT_END
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        contentPanel.add(Box.createVerticalStrut(10));

        JPanel titlePanel = createGlowPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("VitaLite");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(HEADER_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(Box.createVerticalStrut(10));
        titlePanel.add(titleLabel);

        JLabel taglineLabel = new JLabel("Enhanced RuneLite Experience");
        taglineLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        taglineLabel.setForeground(ACCENT_COLOR);
        taglineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(taglineLabel);
        titlePanel.add(Box.createVerticalStrut(10));

        contentPanel.add(titlePanel);
        contentPanel.add(Box.createVerticalStrut(10));

        JPanel versionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        versionPanel.setOpaque(false);
        versionPanel.setMaximumSize(new Dimension(PANEL_WIDTH, 30));

        JLabel versionBadge = new JLabel("RuneLite v-" + Static.getRuneLite().getVersion());
        versionBadge.setFont(new Font("Consolas", Font.BOLD, 11));
        versionBadge.setForeground(ACCENT_COLOR);
        versionBadge.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR, 1),
                new EmptyBorder(2, 8, 2, 8)
        ));
        versionPanel.add(versionBadge);
        contentPanel.add(versionPanel);

        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(createSeparator());
        contentPanel.add(Box.createVerticalStrut(15));

        JLabel connectLabel = new JLabel("Connect & Contribute");
        connectLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        connectLabel.setForeground(HEADER_COLOR);
        connectLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(connectLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        contentPanel.add(createLinkButton(
                "GitHub",
                "View Source Code",
                "https://github.com/Tonic-Box/VitaLite"
        ));

        contentPanel.add(Box.createVerticalStrut(10));

        contentPanel.add(createLinkButton(
                "Discord",
                "Join Community",
                "https://discord.gg/A4S4Fh4gzr"
        ));

        JPanel authorCard = createCard();
        authorCard.setLayout(new BoxLayout(authorCard, BoxLayout.Y_AXIS));

        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(createSeparator());
        contentPanel.add(Box.createVerticalStrut(15));

        JLabel authorTitle = new JLabel("Created by");
        authorTitle.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        authorTitle.setForeground(TEXT_COLOR);
        authorTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        authorCard.add(Box.createVerticalStrut(5));
        authorCard.add(authorTitle);

        JLabel authorName = new JLabel("TonicBox");
        authorName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        authorName.setForeground(HEADER_COLOR);
        authorName.setAlignmentX(Component.CENTER_ALIGNMENT);
        authorCard.add(authorName);
        authorCard.add(Box.createVerticalStrut(5));

        contentPanel.add(authorCard);

        contentPanel.add(Box.createVerticalGlue());

        JLabel footerLabel = new JLabel("Made with <3");
        footerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        footerLabel.setForeground(new Color(150, 150, 155));
        footerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(footerLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        add(contentPanel);
    }

    private JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SEPARATOR_COLOR, 1),
                new EmptyBorder(10, 15, 10, 15)
        ));
        card.setMaximumSize(new Dimension(PANEL_WIDTH - 40, 100));
        return card;
    }

    private JPanel createGlowPanel() {
        JPanel panel = new JPanel() {
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
        };
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(PANEL_WIDTH - 20, 80));
        return panel;
    }

    private JSeparator createSeparator() {
        JSeparator separator = new JSeparator();
        separator.setForeground(SEPARATOR_COLOR);
        separator.setMaximumSize(new Dimension(PANEL_WIDTH - 60, 1));
        return separator;
    }

    private JPanel createLinkButton(String title, String subtitle, String url) {
        JPanel buttonPanel = new JPanel() {
            private boolean isHovered = false;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (isHovered) {
                    g2d.setColor(new Color(70, 70, 75));
                } else {
                    g2d.setColor(CARD_BACKGROUND);
                }
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                g2d.setColor(isHovered ? LINK_HOVER_COLOR : SEPARATOR_COLOR);
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
        };

        buttonPanel.setLayout(new BorderLayout(10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(8, 12, 8, 12));
        buttonPanel.setMaximumSize(new Dimension(PANEL_WIDTH - 40, 50));
        buttonPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(LINK_COLOR);
        textPanel.add(titleLabel);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        subtitleLabel.setForeground(TEXT_COLOR);
        textPanel.add(subtitleLabel);

        buttonPanel.add(textPanel, BorderLayout.CENTER);

        JLabel arrowLabel = new JLabel(">");
        arrowLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        arrowLabel.setForeground(LINK_COLOR);
        buttonPanel.add(arrowLabel, BorderLayout.EAST);

        buttonPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (IOException | URISyntaxException ex) {
                    JOptionPane.showMessageDialog(
                            VitaLiteInfoPanel.this,
                            "Failed to open link: " + url,
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                buttonPanel.putClientProperty("isHovered", true);
                buttonPanel.repaint();
                titleLabel.setForeground(LINK_HOVER_COLOR);
                arrowLabel.setForeground(LINK_HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                buttonPanel.putClientProperty("isHovered", false);
                buttonPanel.repaint();
                titleLabel.setForeground(LINK_COLOR);
                arrowLabel.setForeground(LINK_COLOR);
            }
        });

        return buttonPanel;
    }
}
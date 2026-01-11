package com.tonic.plugins.profiler;

import com.tonic.model.ui.components.FancyCard;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

public class ProfilerPanel extends PluginPanel {
    private final FancyCard card;
    private final JButton openProfilerButton;
    private final JLabel statusLabel;

    @Inject
    public ProfilerPanel() {
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        card = new FancyCard("JVM Profiler", "Performance monitoring and analysis tool");
        add(card, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        statusLabel = new JLabel("Profiler is ready");
        statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        statusLabel.setFont(FontManager.getRunescapeFont());
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(statusLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        openProfilerButton = new JButton("Open Profiler Window");
        openProfilerButton.setFont(FontManager.getRunescapeFont());
        openProfilerButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        openProfilerButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        openProfilerButton.setFocusPainted(false);
        openProfilerButton.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
        openProfilerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        openProfilerButton.setMaximumSize(new Dimension(200, 30));
        openProfilerButton.addActionListener(e -> ProfilerWindow.toggle());
        contentPanel.add(openProfilerButton);

        add(contentPanel, BorderLayout.CENTER);
    }
}

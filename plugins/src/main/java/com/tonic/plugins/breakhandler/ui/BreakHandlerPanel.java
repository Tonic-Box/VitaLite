package com.tonic.plugins.breakhandler.ui;

import com.tonic.plugins.breakhandler.BreakHandler;
import com.tonic.plugins.breakhandler.Break;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BreakHandlerPanel extends PluginPanel
{
    private final BreakHandler breakHandler;
    private final JPanel breaksContainer;
    private final JLabel noBreaksLabel;

    public BreakHandlerPanel()
    {
        this.breakHandler = BreakHandler.getInstance();

        setLayout(new BorderLayout(0, 6));
        setBackground(new Color(40, 40, 40));

        JLabel title = new JLabel("Vita Break Handler");
        title.setForeground(Color.WHITE);
        title.setFont(getFont().deriveFont(Font.BOLD, 14f));
        title.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));

        add(title, BorderLayout.NORTH);

        breaksContainer = new JPanel();
        breaksContainer.setLayout(new BoxLayout(breaksContainer, BoxLayout.Y_AXIS));
        breaksContainer.setBackground(new Color(45, 45, 45));
        breaksContainer.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        noBreaksLabel = new JLabel("No break is scheduled");
        noBreaksLabel.setForeground(Color.LIGHT_GRAY);
        noBreaksLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        breaksContainer.add(noBreaksLabel);
        add(breaksContainer, BorderLayout.CENTER);

        BreakSettingsPanel settingsPanel = new BreakSettingsPanel();
        add(settingsPanel, BorderLayout.SOUTH);

        refreshBreakList();
    }

    public void refreshBreakList()
    {
        breaksContainer.removeAll();

        List<Break> breaks = getActiveBreaks();
        if (breaks.isEmpty())
        {
            breaksContainer.add(noBreaksLabel);
        }
        else
        {
            for (Break b : breaks)
            {
                breaksContainer.add(new BreakPanel(b));
                breaksContainer.add(Box.createVerticalStrut(4));
            }
        }

        breaksContainer.revalidate();
        breaksContainer.repaint();
    }

    private List<Break> getActiveBreaks()
    {
        return breakHandler.getAllBreaks();
    }

}

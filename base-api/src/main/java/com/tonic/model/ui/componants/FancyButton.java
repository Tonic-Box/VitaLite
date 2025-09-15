package com.tonic.model.ui.componants;

import javax.swing.*;
import java.awt.*;

public class FancyButton extends JButton
{
    public FancyButton(String text)
    {
        super(text);
        setAlignmentX(Component.CENTER_ALIGNMENT);
        setMaximumSize(new Dimension(VPluginPanel.PANEL_WIDTH - 40, 30));
        setBackground(new Color(64, 169, 211));
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(new Font("Segoe UI", Font.BOLD, 14));
    }
}

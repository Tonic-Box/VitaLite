package com.tonic.plugins.profiles.panel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class AddJagexAccountPanel extends JPanel {
    private final JButton addJagexAccountButton;
    private final JButton importJagexAccount;

public AddJagexAccountPanel() {
        setLayout(new GridBagLayout());
        setBackground(new Color(48, 48, 48));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);
        gbc.weightx = 1.0;

        // Buttons
        addJagexAccountButton = new JButton("Add Jagex Account");
        importJagexAccount = new JButton("Import Jagex Account");

        // First button
        gbc.gridy = 0;
        add(addJagexAccountButton, gbc);

        // Second button
        gbc.gridy = 1;
        add(importJagexAccount, gbc);
    }

    public void addJagexAccountActionListener(ActionListener actionListener) {
        this.addJagexAccountButton.addActionListener(actionListener);
    }

    public void addImportJagexAccountActionListener(ActionListener actionListener) {
        this.importJagexAccount.addActionListener(actionListener);
    }
}

package com.tonic.plugins.profiles.panel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class AddJagexAccountPanel extends JPanel {
    private final JButton addJagexAccountButton;
    private final JButton importJagexAccount;

    public AddJagexAccountPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(220, 292));
        setMinimumSize(new Dimension(220, 232));
        setMaximumSize(new Dimension(220, 292));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setPreferredSize(new Dimension(220, 32));
        titlePanel.setMinimumSize(new Dimension(220, 32));
        titlePanel.setMaximumSize(new Dimension(220, 32));
        titlePanel.setBackground(new Color(30, 30, 30));

        JLabel titleLabel = new JLabel("Add Jagex Account");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(titleLabel, BorderLayout.WEST);

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBackground(new Color(48, 48, 48));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        GridBagConstraints gbc = new GridBagConstraints();

        JLabel addJagexAccountLabel = new JLabel("<html>Add Jagex Account opens the <b>Official</b> Jagex sign-in in your browser " +
                "and adds the account with all of its characters as one profile. Bank PINs are set per character with Edit." +
                "<br>Import Session adds an account from a session id you already have.</html>");
        addJagexAccountButton = new JButton("Add Jagex Account");
        importJagexAccount = new JButton("Import Session");

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 0;
        gbc.weightx = 1.0;
        contentPanel.add(addJagexAccountLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(2, 18, 2, 18);
        contentPanel.add(addJagexAccountButton, gbc);

        gbc.gridy++;
        contentPanel.add(importJagexAccount, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        add(titlePanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    public void addJagexAccountActionListener(ActionListener actionListener) {
        this.addJagexAccountButton.addActionListener(actionListener);
    }

    public void addImportJagexAccountActionListener(ActionListener actionListener) {
        this.importJagexAccount.addActionListener(actionListener);
    }

    /**
     * Disables both buttons while an account is being added.
     *
     * @param busy whether an account is being added
     */
    public void setBusy(boolean busy) {
        addJagexAccountButton.setEnabled(!busy);
        importJagexAccount.setEnabled(!busy);
    }
}

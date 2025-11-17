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

        JLabel addJagexAccountLabel = new JLabel("<html>To add a Jagex Account, clicking the button will give you a prompt to login to your " +
                "Jagex account via the <b>Official</b> Jagex website, and import the characters you want to add to profiles.  You will name the Profile and customize it's Bank Pin after selecting them to import." +
                "<br>You can import jagex accounts manually if you have your account and session IDs with Import Jagex Account</html>");
        addJagexAccountButton = new JButton("Add Jagex Account");
        importJagexAccount = new JButton("Import Jagex Account");

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
}

package com.tonic.plugins.profiles.panel;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class AddLegacyAccountPanel extends JPanel {

    private final JButton addRSAccountButton;
    @Getter
    private final JTextField identifierField;
    @Getter
    private final JTextField usernameField;
    @Getter
    private final JPasswordField passwordField;
    @Getter
    private final JTextField bankPinField;

    public AddLegacyAccountPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(220, 220));
        setMinimumSize(new Dimension(220, 220));
        setMaximumSize(new Dimension(220, 220));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setPreferredSize(new Dimension(220, 32));
        titlePanel.setMinimumSize(new Dimension(220, 32));
        titlePanel.setMaximumSize(new Dimension(220, 32));
        titlePanel.setBackground(new Color(30, 30, 30));

        JLabel titleLabel = new JLabel("Add Legacy Account");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(titleLabel, BorderLayout.WEST);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBackground(new Color(48, 48, 48));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();

        identifierField = new JTextField("Profile Tag");
        identifierField.setPreferredSize(new Dimension(186, 32));
        identifierField.setMinimumSize(new Dimension(186, 32));
        identifierField.setMaximumSize(new Dimension(186, 32));
        usernameField = new JTextField("Username");
        usernameField.setPreferredSize(new Dimension(186, 32));
        usernameField.setMinimumSize(new Dimension(186, 32));
        usernameField.setMaximumSize(new Dimension(186, 32));
        passwordField = new JPasswordField("Password");
        passwordField.setPreferredSize(new Dimension(186, 32));
        passwordField.setMinimumSize(new Dimension(186, 32));
        passwordField.setMaximumSize(new Dimension(186, 32));
        bankPinField = new JTextField("Bank Pin");
        bankPinField.setColumns(4);
        bankPinField.setPreferredSize(new Dimension(64, 32));
        bankPinField.setMinimumSize(new Dimension(64, 32));
        bankPinField.setMaximumSize(new Dimension(64, 32));
        addRSAccountButton = new JButton("Add Legacy Account");

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 0, 2, 0);
        gbc.gridx = 0;
        gbc.gridy = 0;

        contentPanel.add(identifierField, gbc);

        gbc.gridy++;
        contentPanel.add(usernameField, gbc);

        gbc.gridy++;
        contentPanel.add(passwordField, gbc);

        gbc.gridy++;
        contentPanel.add(bankPinField, gbc);

        gbc.gridy++;
        contentPanel.add(addRSAccountButton, gbc);

        add(titlePanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    public void addRsAccountActionListener(ActionListener actionListener) {
        this.addRSAccountButton.addActionListener(actionListener);
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public String getPassword() {
        return String.valueOf(passwordField.getPassword());
    }

    public String getBankPin() {
        return bankPinField.getText();
    }
}

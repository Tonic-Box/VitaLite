package com.tonic.plugins.profiles.panel;

import com.tonic.plugins.profiles.ProfilesPlugin;
import com.tonic.plugins.profiles.data.Profile;
import com.tonic.plugins.profiles.session.ProfilesSession;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class ProfilePanel extends JPanel {

    @Getter
    private final Profile profile;

    private final JPanel contentPanel;
    private final JButton collapseExpandButton;
    @Getter
    private final JButton editButton;
    @Getter
    private final JButton deleteButton;
    private boolean isExpanded = true;

    private final ProfilesSession profilesSession;

    public ProfilePanel(Profile profile) {
        this.profile = profile;
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(200, 80));
        setPreferredSize(new Dimension(200, 80));
        setMaximumSize(new Dimension(200, 80));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setPreferredSize(new Dimension(220, 48));
        titlePanel.setMinimumSize(new Dimension(220, 48));
        titlePanel.setMaximumSize(new Dimension(220, 48));
        titlePanel.setBackground(new Color(30, 30, 30));

        JLabel titleLabel = new JLabel(profile.getIdentifier());
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        JButton loginButton = new JButton(profile.getIdentifier());
        collapseExpandButton = new JButton("+");

        titlePanel.add(loginButton, BorderLayout.CENTER);

        contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayout(1, 2));
        JLabel usernameLabel = new JLabel(profile.getCharacterName());
        usernameLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        editButton = new JButton("Edit");
        editButton.setBackground(new Color(48, 48, 48));
        editButton.setPreferredSize(new Dimension());
        deleteButton = new JButton("Delete");
        deleteButton.setBackground(new Color(48, 48, 48));
        contentPanel.add(editButton);
        contentPanel.add(deleteButton);

        add(titlePanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        loginButton.addActionListener(e -> login());
        collapseExpandButton.addActionListener(e -> toggleContent());

        this.profilesSession = ProfilesSession.getInstance();
    }

    private void toggleContent() {
        isExpanded = !isExpanded;
        contentPanel.setVisible(isExpanded);
        collapseExpandButton.setText(isExpanded ? "-" : "+");
        revalidate();
        repaint();
    }

    private void login() {
        if (profile == null)
            return;

        profilesSession.login(profile, false);
    }
}

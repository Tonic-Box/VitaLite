package com.tonic.plugins.breakhandler.ui;

import com.tonic.plugins.breakhandler.BreakHandler;
import com.tonic.plugins.breakhandler.settings.Property;
import com.tonic.plugins.profiles.data.Profile;
import com.tonic.plugins.profiles.session.ProfilesSession;
import com.tonic.services.ConfigManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.List;
import java.awt.*;
import java.util.Set;

public class BreakSettingsPanel extends JPanel
{
    private final ConfigManager configManager = BreakHandler.getInstance().getConfigManager();

    public BreakSettingsPanel()
    {
        setLayout(new BorderLayout());
        setOpaque(false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        tabs.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        tabs.addTab("Timing", buildTimingTab());
        tabs.addTab("Account", buildAccountTab());
        tabs.addTab("World", buildWorldTab());

        add(tabs, BorderLayout.CENTER);
    }

    private JComponent buildTimingTab()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gc = gc();
        addSectionLabel(panel, gc, 0, "Break Timing");

        JSpinner minBetween = createSpinner(Property.MIN_BETWEEN.key(),
                120, 1, 358, 1);
        JSpinner maxBetween = createSpinner(Property.MAX_BETWEEN.key(),
                240, 1, 359, 1);
        JSpinner minDuration = createSpinner(Property.MIN_DURATION.key(),
                120, 1, Integer.MAX_VALUE, 1);
        JSpinner maxDuration = createSpinner(Property.MAX_DURATION.key(),
                240, 1, Integer.MAX_VALUE, 1);

        addRow(panel, gc, 1, "Min Between (m):", minBetween);
        addRow(panel, gc, 2, "Max Between (m):", maxBetween);
        addRow(panel, gc, 3, "Min Duration (m):", minDuration);
        addRow(panel, gc, 4, "Max Duration (m):", maxDuration);

        minBetween.addChangeListener(e ->
                configManager.setProperty(Property.MIN_BETWEEN.key(), minBetween.getValue()));
        maxBetween.addChangeListener(e ->
                configManager.setProperty(Property.MAX_BETWEEN.key(), maxBetween.getValue()));
        minDuration.addChangeListener(e ->
                configManager.setProperty(Property.MIN_DURATION.key(), minDuration.getValue()));
        maxDuration.addChangeListener(e ->
                configManager.setProperty(Property.MAX_DURATION.key(), maxDuration.getValue()));

        return wrapNorth(panel);
    }

    private JComponent buildAccountTab()
    {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gc = gc();
        int row = 0;

        addSectionLabel(p, gc, row++, "Account");

        final String modeKey = Property.ACCOUNT_MODE.key();
        final String profileKey = Property.ACCOUNT_PROFILE.key();
        final String userKey = Property.ACCOUNT_USERNAME.key();
        final String passKey = Property.ACCOUNT_PASSWORD.key();

        boolean useProfilesInitial = configManager.getBooleanOrDefault(modeKey, false);
        JCheckBox useProfiles = new JCheckBox("Use Profiles", useProfilesInitial);
        useProfiles.setOpaque(false);
        useProfiles.setForeground(Color.WHITE);

        GridBagConstraints gFull = (GridBagConstraints) gc.clone();
        gFull.gridx = 0; gFull.gridy = row++; gFull.gridwidth = 2; gFull.weightx = 1.0;
        p.add(useProfiles, gFull);

        JPanel cardHolder = new JPanel(new CardLayout());
        cardHolder.setOpaque(false);

        JPanel manualPanel = new JPanel(new GridBagLayout());
        manualPanel.setOpaque(false);
        GridBagConstraints gMan = gc();

        JTextField username = new JTextField(configManager.getStringOrDefault(userKey, ""));
        JPasswordField password = new JPasswordField(configManager.getStringOrDefault(passKey, ""));

        addRow(manualPanel, gMan, 0, "Username:", username);
        addRow(manualPanel, gMan, 1, "Password:", password);

        username.getDocument().addDocumentListener(onChange(() ->
                configManager.setProperty(userKey, username.getText())));
        password.getDocument().addDocumentListener(onChange(() ->
                configManager.setProperty(passKey, new String(password.getPassword()))));

        username.setMaximumSize(new Dimension(Integer.MAX_VALUE, username.getPreferredSize().height));
        password.setMaximumSize(new Dimension(Integer.MAX_VALUE, password.getPreferredSize().height));

        JPanel profilesPanel = new JPanel(new GridBagLayout());
        profilesPanel.setOpaque(false);
        GridBagConstraints gProf = gc();

        List<String> profileNames = getProfileNames();
        profileNames.sort(String::compareToIgnoreCase);

        String lastSelected = configManager.getStringOrDefault(profileKey, "");
        JComboBox<String> profileBox = new JComboBox<>(profileNames.toArray(new String[0]));
        profileBox.setEditable(false);
        profileBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, profileBox.getPreferredSize().height));

        if (!lastSelected.isEmpty()) {
            profileBox.setSelectedItem(lastSelected);
        } else if (profileBox.getItemCount() > 0) {
            profileBox.setSelectedIndex(0);
        }

        addRow(profilesPanel, gProf, 0, "Profile:", profileBox);

        profileBox.addActionListener(e -> {
            Object sel = profileBox.getSelectedItem();
            if (sel != null) {
                configManager.setProperty(profileKey, sel.toString());
            }
        });

        cardHolder.add(manualPanel, "manual");
        cardHolder.add(profilesPanel, "profiles");

        CardLayout cl = (CardLayout) cardHolder.getLayout();
        cl.show(cardHolder, useProfilesInitial ? "profiles" : "manual");

        useProfiles.addActionListener(e -> {
            boolean use = useProfiles.isSelected();
            configManager.setProperty(modeKey, use);

            if (use)
            {
                ProfilesSession profilesSession = ProfilesSession.getInstance();
                profilesSession.loadProfilesFromFile();
            }

            cl.show(cardHolder, use ? "profiles" : "manual");
        });

        GridBagConstraints gCards = (GridBagConstraints) gc.clone();
        gCards.gridx = 0; gCards.gridy = row; gCards.gridwidth = 2; gCards.weightx = 1.0; gCards.fill = GridBagConstraints.HORIZONTAL;
        p.add(cardHolder, gCards);

        return wrapNorth(p);
    }

    private static List<String> getProfileNames() {
        List<String> profileNames = new java.util.ArrayList<>();
        ProfilesSession profilesSession = ProfilesSession.getInstance();
        profilesSession.loadProfilesFromFile();
        try {
            Set<Profile> profiles = profilesSession.getProfiles();
            if (profiles != null) {
                for (Profile pr : profiles) {
                    if (pr != null) {
                        profileNames.add(pr.getIdentifier());
                    }
                }
            }
        } catch (Throwable ignored) {

        }
        return profileNames;
    }


    private JComponent buildWorldTab()
    {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel generalLabel = createHeader("General");
        JCheckBox hopWorlds = createCheckbox(Property.HOP_ENABLED.key(), "Hop Worlds", true);
        JCheckBox f2pOnly = createCheckbox(Property.F2P_ONLY.key(), "F2P only", false);

        p.add(generalLabel);
        p.add(hopWorlds);
        p.add(f2pOnly);
        p.add(Box.createVerticalStrut(8));

        JLabel typesLabel = createHeader("Disable World Types");
        p.add(typesLabel);

        p.add(createCheckbox(Property.DISABLE_HIGH_RISK.key(), "High Risk", false));
        p.add(createCheckbox(Property.DISABLE_LMS.key(), "Last Man Standing", false));
        p.add(createCheckbox(Property.DISABLE_SKILL_TOTAL.key(), "Skill Total", false));
        p.add(createCheckbox(Property.DISABLE_BOUNTY.key(), "Bounty/Target", false));
        p.add(Box.createVerticalStrut(8));

        JLabel regionsLabel = createHeader("Disable Regions");
        p.add(regionsLabel);
        p.add(createCheckbox(Property.DISABLE_REGION_US.key(), "US", false));
        p.add(createCheckbox(Property.DISABLE_REGION_UK.key(), "UK", false));
        p.add(createCheckbox(Property.DISABLE_REGION_DE.key(), "DE", false));
        p.add(createCheckbox(Property.DISABLE_REGION_AU.key(), "AU", false));

        p.add(Box.createVerticalGlue());
        return p;
    }

    private GridBagConstraints gc()
    {
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 2, 4, 2);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        return gc;
    }

    private JPanel wrapNorth(JComponent inner)
    {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(inner, BorderLayout.NORTH);
        return wrap;
    }

    private void addSectionLabel(JPanel panel, GridBagConstraints gc, int row, String text)
    {
        GridBagConstraints g = (GridBagConstraints) gc.clone();
        g.gridx = 0;
        g.gridy = row;
        g.gridwidth = 2;
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        panel.add(l, g);
    }

    private void addRow(JPanel panel, GridBagConstraints gcBase,
                        int row, String label, JComponent field)
    {
        GridBagConstraints gc = (GridBagConstraints) gcBase.clone();
        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 0;
        JLabel l = new JLabel(label);
        l.setForeground(Color.WHITE);
        panel.add(l, gc);

        gc = (GridBagConstraints) gcBase.clone();
        gc.gridx = 1;
        gc.gridy = row;
        gc.weightx = 1.0;
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        panel.add(field, gc);
    }

    private JLabel createHeader(String text)
    {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        l.setBorder(BorderFactory.createEmptyBorder(4, 0, 2, 0));
        return l;
    }

    private JSpinner createSpinner(String key, int def, int min, int max, int step)
    {
        int current = configManager.getIntOrDefault(key, def);
        SpinnerNumberModel model = new SpinnerNumberModel(current, min, max, step);
        JSpinner spinner = new JSpinner(model);
        spinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, spinner.getPreferredSize().height));
        return spinner;
    }

    private JCheckBox createCheckbox(String key, String label, boolean def)
    {
        boolean val = configManager.getBooleanOrDefault(key, def);
        JCheckBox box = new JCheckBox(label, val);
        box.setOpaque(false);
        box.setForeground(Color.WHITE);
        box.addActionListener(e -> configManager.setProperty(key, box.isSelected()));
        return box;
    }

    private static javax.swing.event.DocumentListener onChange(Runnable r)
    {
        return new javax.swing.event.DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                r.run();
            }

            public void removeUpdate(DocumentEvent e) {
                r.run();
            }

            public void changedUpdate(DocumentEvent e) {
                r.run();
            }
        };
    }
}

package com.tonic.plugins.breakhandler.ui;

import com.tonic.services.breakhandler.BreakHandler;
import com.tonic.services.breakhandler.settings.Property;
import com.tonic.plugins.breakhandler.BreakAccount;
import com.tonic.plugins.profiles.data.ProfileRef;
import com.tonic.plugins.profiles.session.ProfilesSession;
import com.tonic.services.ConfigManager;
import net.runelite.client.RuneLite;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;

public class BreakSettingsPanel extends JPanel
{
    private final BreakHandler breakHandler = RuneLite.getInjector().getInstance(BreakHandler.class);
    private static final Color MISSING = new Color(0xE05050);

    private final ConfigManager configManager = breakHandler.getConfigManager();
    private final BreakAccount account = new BreakAccount(breakHandler);
    private boolean filling;

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
        addSectionLabel(p, gc, 0, "Account");

        JComboBox<AccountOption> accounts = new JComboBox<>();
        accounts.setRenderer(new AccountRenderer());
        fillAccounts(accounts);
        accounts.addActionListener(e ->
        {
            Object selected = accounts.getSelectedItem();
            if (!filling && selected instanceof AccountOption && !((AccountOption) selected).missing)
            {
                account.choose(((AccountOption) selected).key);
            }
        });
        accounts.addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
                fillAccounts(accounts);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e)
            {
            }
        });
        addRow(p, gc, 1, "Log in as:", accounts);

        JLabel hint = new JLabel("<html>Current account logs back in to whichever account the client was on. Profiles come from the Profiles plugin.</html>");
        hint.setForeground(Color.GRAY);
        GridBagConstraints g = (GridBagConstraints) gc.clone();
        g.gridx = 0;
        g.gridy = 2;
        g.gridwidth = 2;
        p.add(hint, g);

        return wrapNorth(p);
    }

    private void fillAccounts(JComboBox<AccountOption> box)
    {
        String target = account.target();
        DefaultComboBoxModel<AccountOption> model = new DefaultComboBoxModel<>();
        AccountOption selected = new AccountOption(BreakAccount.CURRENT, "Current account", false);
        model.addElement(selected);
        ProfilesSession profiles = ProfilesSession.getInstance();
        profiles.loadProfilesFromFile();
        for (ProfileRef ref : profiles.getProfileRefs())
        {
            AccountOption option = new AccountOption(ref.getKey(), ref.getName(), false);
            model.addElement(option);
            if (ref.getKey().equals(target))
            {
                selected = option;
            }
        }
        if (!BreakAccount.CURRENT.equals(target) && BreakAccount.CURRENT.equals(selected.key))
        {
            selected = new AccountOption(target, "Missing profile", true);
            model.addElement(selected);
        }
        filling = true;
        box.setModel(model);
        box.setSelectedItem(selected);
        box.setToolTipText(selected.missing ? "The chosen profile is no longer in Profiles; pick another" : null);
        filling = false;
    }

    /**
     * One choice in the account dropdown: the key it saves and what it shows.
     */
    private static final class AccountOption
    {
        private final String key;
        private final String label;
        private final boolean missing;

        private AccountOption(String key, String label, boolean missing)
        {
            this.key = key;
            this.label = label;
            this.missing = missing;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    /**
     * Shows a chosen profile that is gone from Profiles in red.
     */
    private static final class AccountRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused)
        {
            Component component = super.getListCellRendererComponent(list, value, index, selected, focused);
            if (value instanceof AccountOption && ((AccountOption) value).missing)
            {
                component.setForeground(MISSING);
            }
            return component;
        }
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
}

package com.tonic.plugins.multiclientutils.ui;

import com.tonic.Static;
import com.tonic.model.ui.components.FancyButton;
import com.tonic.model.ui.components.FancyCard;
import com.tonic.model.ui.components.OptionPanel;
import com.tonic.model.ui.components.ToggleSlider;
import com.tonic.plugins.multiclientutils.MultiClientUtilPlugin;
import com.tonic.services.ipc.Channel;
import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

public class MultiClientPanel extends PluginPanel
{
    private MultiClientUtilPlugin plugin;
    private ToggleSlider extendedMenus;

    @Inject
    public MultiClientPanel(MultiClientUtilPlugin plugin)
    {
        this.plugin = plugin;

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 10, 0);

        FancyCard card = new FancyCard("Multi-Client", "Tooling for managing multiple clients");
        add(card, c);
        c.gridy++;

        buttonFactory(c, "Come Here", () -> {
            Client client = Static.getClient();
            if(client.getLocalPlayer() == null)
                return;
            plugin.sendMessage("PATHFIND", WorldPointUtil.compress(client.getLocalPlayer().getWorldLocation()), client.getWorld());
        }, "Have all available clients hop to and pathfind to this clients world and location");

        buttonFactory(c, "DD", () -> plugin.sendMessage("DD"), "All clients in this clients area DD under this character.");

        buttonFactory(c, "Follow", () -> plugin.sendMessage("FOLLOW"), "All clients follow this clients character.");

        buttonFactory(c, "Scatter", () -> plugin.sendMessage("SCATTER"), "All clients in this clients area scatter around this character.");

        buttonFactory(c, "Cat Fact", () -> plugin.sendMessage("CATFACT"), "All clients send a cat fact to public chat");

        extendedMenus = new ToggleSlider();
        createToggleOption(
                c,
                "Extended Menus",
                "Extended right click menus",
                extendedMenus,
                () -> plugin.setExtendedMenus(extendedMenus.isSelected())
        );
    }

    private void createToggleOption(GridBagConstraints c, String title, String description, ToggleSlider toggle, Runnable onClick) {
        OptionPanel optionPanel = new OptionPanel();
        optionPanel.init(title, description, toggle, onClick);
        add(optionPanel, c);
        c.gridy++;
    }

    private void buttonFactory(GridBagConstraints c, String text, Runnable onClick, String desc)
    {
        FancyButton button = new FancyButton(text);
        button.setFocusable(false);
        button.addActionListener(e -> onClick.run());
        button.setToolTipText(desc);

        add(button, c);
        c.gridy++;
    }
}

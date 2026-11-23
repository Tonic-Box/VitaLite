package com.tonic.services.streamer;

import com.tonic.Logger;
import com.tonic.Static;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stand-ins for the RuneLite plugins VitaLite never loads, so the streamer mirror looks like RuneLite with them: their title bar buttons and their plugin list rows in the mirror, and inert rows in the real plugin list so its rows line up with the mirror's. None of their code runs: names come from their descriptors, icons from RuneLite's resources, and settings from their config interfaces. Runs on the event dispatch thread only.
 */
final class BlockedPlugins
{
    private static final String PINNED_PLUGINS = "pinnedPlugins";
    private static final String CONFIG_PACKAGE = "net.runelite.client.plugins.config.";

    private static final List<Blocked> BLOCKED = List.of(
            new Blocked("net.runelite.client.plugins.account.AccountPlugin", null, "/net/runelite/client/plugins/account/login_icon.png", "Sign in to RuneLite"),
            new Blocked("net.runelite.client.plugins.twitch.TwitchPlugin", "net.runelite.client.plugins.twitch.TwitchConfig", null, null),
            new Blocked("net.runelite.client.plugins.crowdsourcing.CrowdsourcingPlugin", null, null, null));

    private static final int ICON_SIZE = 16;

    private static final Set<NavigationButton> buttons = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<NavigationButton, Icon> icons = new IdentityHashMap<>();
    private static List<NavigationButton> standInButtons;
    private static List<Row> rows;
    private static List<Row> realRows;

    private BlockedPlugins()
    {
    }

    /**
     * @param button a title bar or sidebar button
     * @return whether it stands in for a blocked plugin's button
     */
    static boolean isStandIn(NavigationButton button)
    {
        return buttons.contains(button);
    }

    /**
     * Gives the blocked plugins' title bar buttons for the mirror, which never reach the real title bar.
     *
     * @return the buttons
     */
    static List<NavigationButton> standInButtons()
    {
        if (standInButtons == null)
        {
            List<NavigationButton> built = new ArrayList<>();
            for (Blocked blocked : BLOCKED)
            {
                if (blocked.icon == null || isLoaded(blocked))
                {
                    continue;
                }
                BufferedImage icon = ImageUtil.loadImageResource(ClientUI.class, blocked.icon);
                NavigationButton button = NavigationButton.builder()
                        .icon(icon)
                        .tooltip(blocked.tooltip)
                        .onClick(() -> {})
                        .build();
                buttons.add(button);
                icons.put(button, new ImageIcon(ImageUtil.resizeImage(icon, ICON_SIZE, ICON_SIZE)));
                built.add(button);
            }
            standInButtons = built;
        }
        return standInButtons;
    }

    /**
     * @param button a stand-in title bar button
     * @return its icon, sized as RuneLite sizes title bar icons
     */
    static Icon standInIcon(NavigationButton button)
    {
        return icons.get(button);
    }

    /**
     * Gives the blocked plugins' rows for the mirror's plugin list, built with RuneLite's own row component and showing the stored pinned and enabled state.
     *
     * @param pluginList RuneLite's plugin list
     * @return the rows, which are never added to the real interface
     */
    static List<Object> rows(Object pluginList)
    {
        if (rows == null)
        {
            rows = build(pluginList, false);
        }
        return shown(rows);
    }

    /**
     * Gives the blocked plugins' rows for the real plugin list while streamer mode is on, with neither a switch nor settings so they do nothing, at the places the mirror shows its rows.
     *
     * @param pluginList RuneLite's plugin list
     * @return the rows, showing the stored pinned state
     */
    static List<Object> realRows(Object pluginList)
    {
        if (realRows == null)
        {
            realRows = build(pluginList, true);
        }
        return shown(realRows);
    }

    /**
     * @param row a plugin list row
     * @return whether it is one of the real plugin list's stand-in rows
     */
    static boolean isRealRow(Object row)
    {
        if (realRows != null)
        {
            for (Row realRow : realRows)
            {
                if (realRow.item == row)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Takes the stand-in rows out of the real plugin list, as streamer mode stops.
     */
    static void removeRealRows()
    {
        if (realRows == null)
        {
            return;
        }
        for (Row row : realRows)
        {
            Container parent = ((Component) row.item).getParent();
            if (parent != null)
            {
                parent.remove((Component) row.item);
            }
        }
    }

    private static List<Object> shown(List<Row> built)
    {
        ConfigManager configManager = Static.getInjector().getInstance(ConfigManager.class);
        String pinnedSetting = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, PINNED_PLUGINS);
        List<String> pinned = pinnedSetting == null ? List.of() : Text.fromCSV(pinnedSetting);
        List<Object> shown = new ArrayList<>(built.size());
        for (Row row : built)
        {
            try
            {
                String enabled = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, row.enabledKey);
                row.setEnabled.invoke(row.item, enabled == null ? row.enabledByDefault : Boolean.parseBoolean(enabled));
                row.setPinned.invoke(row.item, pinned.contains(row.name.replace(",", "")));
                shown.add(row.item);
            }
            catch (ReflectiveOperationException | RuntimeException e)
            {
                Logger.error("Streamer mirror could not show the " + row.name + " row: " + e);
            }
        }
        return shown;
    }

    private static List<Row> build(Object pluginList, boolean inert)
    {
        List<Row> built = new ArrayList<>();
        try
        {
            ClassLoader loader = ClientUI.class.getClassLoader();
            ConfigManager configManager = Static.getInjector().getInstance(ConfigManager.class);
            Class<?> descriptorType = Class.forName(CONFIG_PACKAGE + "PluginConfigurationDescriptor", false, loader);
            Constructor<?> descriptorConstructor = Members.accessible(descriptorType.getDeclaredConstructor(String.class, String.class, String[].class, Plugin.class, Config.class, ConfigDescriptor.class, List.class));
            Class<?> itemType = Class.forName(CONFIG_PACKAGE + "PluginListItem", false, loader);
            Constructor<?> itemConstructor = Members.accessible(itemType.getDeclaredConstructor(pluginList.getClass(), descriptorType));
            Method setEnabled = Members.method(itemType, "setPluginEnabled", boolean.class);
            Method setPinned = Members.method(itemType, "setPinned", boolean.class);

            for (Blocked blocked : BLOCKED)
            {
                if (isLoaded(blocked))
                {
                    continue;
                }
                Class<?> pluginType = Class.forName(blocked.plugin, false, loader);
                PluginDescriptor descriptor = pluginType.getAnnotation(PluginDescriptor.class);
                if (descriptor == null || descriptor.hidden() || descriptor.developerPlugin())
                {
                    continue;
                }
                Config config = null;
                ConfigDescriptor configDescriptor = null;
                if (blocked.config != null && !inert)
                {
                    @SuppressWarnings("unchecked")
                    Class<? extends Config> configType = (Class<? extends Config>) Class.forName(blocked.config, false, loader);
                    config = configManager.getConfig(configType);
                    configDescriptor = configManager.getConfigDescriptor(config);
                }
                Object pluginConfig = descriptorConstructor.newInstance(descriptor.name(), descriptor.description(), descriptor.tags(), inert ? null : new StandInPlugin(), config, configDescriptor, List.of());
                Object item = itemConstructor.newInstance(pluginList, pluginConfig);
                built.add(new Row(descriptor.name(), item, pluginType.getSimpleName().toLowerCase(), descriptor.enabledByDefault(), setEnabled, setPinned));
            }
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            Logger.error("Streamer mirror could not build the blocked plugins' rows: " + e);
        }
        return built;
    }

    private static boolean isLoaded(Blocked blocked)
    {
        for (Plugin plugin : Static.getInjector().getInstance(PluginManager.class).getPlugins())
        {
            if (plugin.getClass().getName().equals(blocked.plugin))
            {
                return true;
            }
        }
        return false;
    }

    private static final class StandInPlugin extends Plugin
    {
    }

    private static final class Blocked
    {
        private final String plugin;
        private final String config;
        private final String icon;
        private final String tooltip;

        private Blocked(String plugin, String config, String icon, String tooltip)
        {
            this.plugin = plugin;
            this.config = config;
            this.icon = icon;
            this.tooltip = tooltip;
        }
    }

    private static final class Row
    {
        private final String name;
        private final Object item;
        private final String enabledKey;
        private final boolean enabledByDefault;
        private final Method setEnabled;
        private final Method setPinned;

        private Row(String name, Object item, String enabledKey, boolean enabledByDefault, Method setEnabled, Method setPinned)
        {
            this.name = name;
            this.item = item;
            this.enabledKey = enabledKey;
            this.enabledByDefault = enabledByDefault;
            this.setEnabled = setEnabled;
            this.setPinned = setPinned;
        }
    }
}

package com.tonic.ui.sdn;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.services.hotswapper.PluginContext;
import com.tonic.util.ReflectBuilder;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Switches the plugins a jar loaded on and off and opens their settings, the way RuneLite's own plugin list does, so both
 * stay in step.
 */
final class PluginControls {
    private static final String CONFIG_PLUGIN = "net.runelite.client.plugins.config.ConfigPlugin";

    private PluginControls() {
    }

    /**
     * @param jar a plugin jar
     * @return the plugins it has loaded, empty when it is not loaded
     */
    static List<Plugin> plugins(File jar) {
        if (jar == null) {
            return List.of();
        }
        PluginContext context = PluginContext.getLoadedPlugins().get(jar.getAbsolutePath());
        return context == null ? List.of() : new ArrayList<>(context.getPlugins());
    }

    /**
     * @param plugins plugins of one jar
     * @return whether any of them is running
     */
    static boolean isActive(List<Plugin> plugins) {
        PluginManager manager = manager();
        for (Plugin plugin : plugins) {
            if (manager.isPluginActive(plugin)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param plugins plugins of one jar
     * @return whether any of them has settings
     */
    static boolean hasConfig(List<Plugin> plugins) {
        PluginManager manager = manager();
        for (Plugin plugin : plugins) {
            if (manager.getPluginConfigProxy(plugin) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Turns plugins on or off and remembers it, as the plugin list's switch does. Runs on the Swing thread.
     *
     * @param plugins plugins of one jar
     * @param active  whether they should run
     */
    static void setActive(List<Plugin> plugins, boolean active) {
        PluginManager manager = manager();
        for (Plugin plugin : plugins) {
            manager.setPluginEnabled(plugin, active);
            try {
                if (active && !manager.isPluginActive(plugin)) {
                    manager.startPlugin(plugin);
                } else if (!active && manager.isPluginActive(plugin)) {
                    manager.stopPlugin(plugin);
                }
            } catch (PluginInstantiationException e) {
                Logger.error("Vita Hub: could not " + (active ? "start " : "stop ") + name(plugin) + ": " + e.getMessage());
            }
        }
    }

    /**
     * Opens a plugin's settings in RuneLite's configuration panel; for a jar with several plugins, the plugin list filtered
     * to them.
     *
     * @param plugins plugins of one jar
     */
    static void openConfig(List<Plugin> plugins) {
        PluginManager manager = manager();
        List<Plugin> configurable = new ArrayList<>();
        for (Plugin plugin : plugins) {
            if (manager.getPluginConfigProxy(plugin) != null) {
                configurable.add(plugin);
            }
        }
        if (configurable.isEmpty()) {
            return;
        }
        try {
            Object configPlugin = configPlugin(manager);
            if (configPlugin == null) {
                return;
            }
            ReflectBuilder panel = ReflectBuilder.of(configPlugin).field("topLevelConfigPanelProvider").method("get", null, null);
            if (configurable.size() == 1) {
                panel.method("openConfigurationPanel", new Class<?>[]{Plugin.class}, new Object[]{configurable.get(0)}).get();
            } else {
                panel.method("openWithFilter", new Class<?>[]{String.class}, new Object[]{name(configurable.get(0))}).get();
            }
        } catch (RuntimeException e) {
            Logger.error("Vita Hub: could not open the settings of " + name(configurable.get(0)) + ": " + e.getMessage());
        }
    }

    /**
     * @param plugin a plugin
     * @return the name its descriptor gives, or its class name
     */
    static String name(Plugin plugin) {
        PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
        return descriptor == null ? plugin.getClass().getSimpleName() : descriptor.name();
    }

    /**
     * @param plugin a plugin
     * @return the description its descriptor gives, or null
     */
    static String description(Plugin plugin) {
        PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
        return descriptor == null || descriptor.description().isEmpty() ? null : descriptor.description();
    }

    /**
     * @param listener called whenever any plugin starts or stops
     */
    static void onPluginChanged(Runnable listener) {
        Static.getInjector().getInstance(EventBus.class).register(PluginChanged.class, event -> listener.run(), 0);
    }

    private static Object configPlugin(PluginManager manager) {
        for (Plugin plugin : manager.getPlugins()) {
            if (plugin.getClass().getName().equals(CONFIG_PLUGIN)) {
                return plugin;
            }
        }
        return null;
    }

    private static PluginManager manager() {
        return Static.getInjector().getInstance(PluginManager.class);
    }
}

package com.tonic.services.hotswapper;

import com.google.common.reflect.ClassPath;
import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.util.ReflectBuilder;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PluginReloader {
    @Getter
    @Setter
    private static ClassLoader classLoader;
    private static PluginManager pluginManager;

    private static boolean isLoaded = false;

    public static void init()
    {
        if(isLoaded)
            return;
        isLoaded = true;

        pluginManager = Static.getInjector().getInstance(PluginManager.class);

        for(PluginContext context : PluginContext.getLoadedPlugins().values())
        {
            if(context.getPluginClasses().isEmpty())
                continue;

            final List<Object> plugins = new ArrayList<>();
            for(Class<?> clazz : context.getPluginClasses())
            {
                Plugin plugin = findLoadedPlugin(clazz);
                if(plugin != null)
                {
                    plugins.add(plugin);
                }
            }
            context.getPlugins().addAll(plugins);
            context.getPluginClasses().clear();
        }
    }

    private static Plugin findLoadedPlugin(Class<?> clazz)
    {
        for(Plugin plugin : pluginManager.getPlugins())
        {
            if(plugin.getClass().equals(clazz))
                return plugin;
        }
        return null;
    }

    public static boolean reloadPlugin(File jarFile) {
        try {
            PluginContext oldContext = PluginContext.getLoadedPlugins().get(jarFile.getAbsolutePath());
            if (oldContext != null) {
                for (Object obj : oldContext.getPlugins()) {
                    Plugin plugin = (Plugin) obj;
                    if (pluginManager.isPluginActive(plugin)) {
                        if(!pluginManager.stopPlugin(plugin)) {
                            Logger.error("Failed to stop plugin: " + plugin.getClass().getName());
                            //Dont want an infinite hang of the service if some other factor is preventing the stopping of
                            //this plugin.
                            return true;
                        }
                    }
                    pluginManager.remove(plugin);
                }
            }

            PluginClassLoader newClassLoader = new PluginClassLoader(jarFile, Static.getClassLoader());

            List<Class<?>> newClasses = ClassPath.from(newClassLoader)
                    .getAllClasses()
                    .stream()
                    .filter(info -> !info.getName().equals("module-info"))
                    .map(ClassPath.ClassInfo::load)
                    .collect(Collectors.toList());

            List<Plugin> newPlugins = pluginManager.loadPlugins(newClasses, null);
            List<Object> instances = new ArrayList<>(newPlugins);

            PluginContext.getLoadedPlugins().put(jarFile.getAbsolutePath(), new PluginContext(
                    newClassLoader, instances, new ArrayList<>(), jarFile.lastModified()
            ));

            if (oldContext != null) {
                oldContext.getClassLoader().close();
            }

            pluginManager.loadDefaultPluginConfiguration(newPlugins);
            for(Plugin plugin : newPlugins)
            {
                pluginManager.startPlugin(plugin);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void forceRebuildPluginList() {
        try
        {
            PluginManager pluginManager = Static.getInjector().getInstance(PluginManager.class);
            Plugin plugin = pluginManager.getPlugins().stream()
                    .filter(p -> p.getClass().getName().equals("net.runelite.client.plugins.config.ConfigPlugin"))
                    .findFirst()
                    .orElse(null);

            ReflectBuilder.of(plugin)
                    .field("pluginListPanelProvider")
                    .method("get", null, null)
                    .method("rebuildPluginList", null, null)
                    .get();
        }
        catch (Exception e)
        {
            Logger.error("Failed to rebuild plugin list UI: " + e.getMessage());
        }
    }

    /*
     * INTERNAL USE ONLY: A call to this is injected into RuneLite's plugin list items
     */
    public static JButton addRedButtonAfterPin(JPanel pluginListItem, Plugin plugin) {
        if (!(pluginListItem.getLayout() instanceof BorderLayout)) {
            return null;
        }

        if(plugin == null) {
            return null;
        }

        PluginContext context = PluginContext.of(plugin.getClass().getName());
        if (context == null) {
            return null;
        }

        BorderLayout layout = (BorderLayout) pluginListItem.getLayout();
        Component pinComponent = layout.getLayoutComponent(BorderLayout.LINE_START);

        if (pinComponent instanceof JPanel) {
            return null;
        }

        if (!(pinComponent instanceof JToggleButton)) {
            return null;
        }

        JToggleButton pinButton = (JToggleButton) pinComponent;
        pluginListItem.remove(pinButton);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout(2, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(pinButton, BorderLayout.WEST);

        CycleButton cycleButton = new CycleButton();
        cycleButton.addActionListener(e -> {
            File jar = context.getFile();
            if(!reloadPlugin(jar))
            {
                forceRebuildPluginList();
                Logger.error("Failed to reload plugin: " + jar.getName());
                return;
            }
            forceRebuildPluginList();
            Logger.info("Reloaded plugin: " + jar.getName());
        });

        leftPanel.add(cycleButton, BorderLayout.CENTER);
        pluginListItem.add(leftPanel, BorderLayout.LINE_START);
        pluginListItem.revalidate();
        pluginListItem.repaint();
        return cycleButton;
    }
}
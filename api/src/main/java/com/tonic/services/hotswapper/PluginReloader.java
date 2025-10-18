package com.tonic.services.hotswapper;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.util.ReflectBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class PluginReloader {
    private static MultiPluginClassLoader pluginClassLoader;
    private static PluginManager pluginManager;

    private static boolean isLoaded = false;

    public static void init()
    {
        if(isLoaded)
            return;
        isLoaded = true;

        pluginManager = Static.getInjector().getInstance(PluginManager.class);

        List<File> jars = findJars().stream()
                .map(Path::toFile)
                .collect(Collectors.toList());

        pluginClassLoader = new MultiPluginClassLoader(jars.toArray(File[]::new), Static.getClassLoader());

        try {
            pluginManager.loadPlugins(pluginClassLoader.getPluginClasses(), null);
        } catch (PluginInstantiationException e) {
            log.error(e.getMessage());
        }

        for (File jar : jars) {
            var loader = pluginClassLoader.getClassLoader(jar);
            var pluginClasses = pluginClassLoader.getPluginClasses(jar);
            var plugins = pluginClasses.stream().map(PluginReloader::findLoadedPlugin).collect(Collectors.toList());
            PluginContext.getLoadedPlugins().put(jar.getAbsolutePath(), new PluginContext(
                    loader, plugins, jar.lastModified()
            ));
        }
    }

    private static Plugin findLoadedPlugin(Class<?> clazz)
    {
        for(Plugin plugin : pluginManager.getPlugins())
        {
            if(plugin.getClass().getName().equals(clazz.getName()))
            {
                return plugin;
            }
        }
        return null;
    }

    public static boolean reloadPlugin(File jarFile) {
        try {
            PluginContext oldContext = PluginContext.getLoadedPlugins().get(jarFile.getAbsolutePath());
            if (oldContext != null) {
                for (Plugin plugin : oldContext.getPlugins()) {
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

            pluginClassLoader.reload(jarFile);
            var loader = pluginClassLoader.getClassLoader(jarFile);
            var newPlugins = pluginManager.loadPlugins(loader.getPluginClasses(), null);

            PluginContext.getLoadedPlugins().put(jarFile.getAbsolutePath(), new PluginContext(
                    loader, newPlugins, jarFile.lastModified()
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
    public static void addRedButtonAfterPin(JPanel pluginListItem, Plugin plugin) {
        SwingUtilities.invokeLater(() -> {
            if (!(pluginListItem.getLayout() instanceof BorderLayout)) {
                return;
            }

            if(plugin == null) {
                return;
            }

            PluginContext context = PluginContext.of(plugin.getClass().getName());
            if (context == null) {
                return;
            }

            BorderLayout layout = (BorderLayout) pluginListItem.getLayout();
            Component pinComponent = layout.getLayoutComponent(BorderLayout.LINE_START);

            if (pinComponent instanceof JPanel) {
                return;
            }

            if (!(pinComponent instanceof JToggleButton)) {
                return;
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
        });
    }

    private static List<Path> findJars() {
        Path external = Static.RUNELITE_DIR.resolve("externalplugins");
        Path sideloaded = Static.RUNELITE_DIR.resolve("sideloaded-plugins");
        try {
            Files.createDirectories(external);
            Files.createDirectories(sideloaded);
        } catch (IOException ignored) {
        }

        return Stream.of(external, sideloaded)
                .flatMap(dir -> {
                    try {
                        return Files.walk(dir);
                    } catch (IOException e) {
                        return Stream.empty();
                    }
                })
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".jar"))
                .collect(Collectors.toList());
    }
}
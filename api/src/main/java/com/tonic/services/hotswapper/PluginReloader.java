package com.tonic.services.hotswapper;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.util.ReflectBuilder;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileVisitOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PluginReloader {
    private static PluginManager pluginManager;

    private static boolean isLoaded = false;

    public static void init()
    {
        if(isLoaded)
            return;
        isLoaded = true;

        pluginManager = Static.getInjector().getInstance(PluginManager.class);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.info("Cleaning up plugin classloaders...");
            for (PluginContext context : PluginContext.getLoadedPlugins().values()) {
                try {
                    context.cleanup();
                } catch (Exception e) {
                    Logger.error("Error cleaning up plugin context: " + e.getMessage());
                }
            }
            PluginContext.getLoadedPlugins().clear();
        }, "PluginClassLoader-Cleanup"));

        registerLoaded(pluginManager);
    }

    /**
     * Gives every sideloaded jar whose plugins are loaded a context holding the class loader they were loaded with, so
     * they can be reloaded; jars that already have one keep it.
     *
     * @param manager the client's plugin manager
     */
    public static synchronized void registerLoaded(PluginManager manager) {
        try {
            usePluginManager(manager);
            register(manager);
        } catch (Throwable e) {
            Logger.error("Failed to register sideloaded plugins for reloading: " + e);
        }
    }

    private static void register(PluginManager manager) {
        Map<PluginClassLoader, List<Plugin>> byLoader = new LinkedHashMap<>();
        for (Plugin plugin : manager.getPlugins()) {
            ClassLoader loader = plugin.getClass().getClassLoader();
            if (loader instanceof PluginClassLoader) {
                byLoader.computeIfAbsent((PluginClassLoader) loader, key -> new ArrayList<>()).add(plugin);
            }
        }

        for (Path path : findJars()) {
            File jar = path.toFile();
            String key = jar.getAbsolutePath();
            if (PluginContext.getLoadedPlugins().containsKey(key)) {
                continue;
            }
            for (Map.Entry<PluginClassLoader, List<Plugin>> entry : byLoader.entrySet()) {
                if (sameFile(entry.getKey().getPluginFile(), jar)) {
                    PluginContext.getLoadedPlugins().put(key, new PluginContext(entry.getKey(), entry.getValue(), jar.lastModified()));
                    break;
                }
            }
        }
    }

    private static boolean sameFile(File a, File b) {
        try {
            return a.getCanonicalFile().equals(b.getCanonicalFile());
        } catch (IOException e) {
            return a.getAbsoluteFile().equals(b.getAbsoluteFile());
        }
    }

    public static boolean reloadPlugin(File jarFile) {
        try {
            if (!unloadPlugin(jarFile)) {
                return true;
            }
            loadPlugin(jarFile, List.of(), true);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Stops and removes every plugin a jar loaded and closes its class loader, so the jar can be replaced or deleted.
     *
     * @param jarFile the plugin jar
     * @return false when a plugin refused to stop and the jar stays loaded
     */
    public static boolean unloadPlugin(File jarFile) {
        PluginManager manager = pluginManager();
        PluginContext oldContext = PluginContext.getLoadedPlugins().get(jarFile.getAbsolutePath());
        if (oldContext == null) {
            return true;
        }
        for (Plugin plugin : new ArrayList<>(oldContext.getPlugins())) {
            try {
                if (manager.isPluginActive(plugin) && !onEventThread(() -> manager.stopPlugin(plugin))) {
                    Logger.error("Failed to stop plugin: " + plugin.getClass().getName());
                    return false;
                }
            } catch (Exception e) {
                Logger.error("Failed to stop plugin: " + plugin.getClass().getName() + ": " + e.getMessage());
                return false;
            }
            manager.remove(plugin);
        }
        PluginContext.getLoadedPlugins().remove(jarFile.getAbsolutePath());
        oldContext.cleanup();
        return true;
    }

    /**
     * Loads a plugin jar, optionally starting its plugins.
     *
     * @param jarFile      the plugin jar
     * @param dependencies the class loaders of the plugins it requires
     * @param start        whether to apply default configuration and start its plugins now, as when installing into a
     *                     running client; at client startup RuneLite starts them itself
     * @return the plugins it loaded
     * @throws Exception if the jar cannot be loaded
     */
    public static List<Plugin> loadPlugin(File jarFile, List<ClassLoader> dependencies, boolean start) throws Exception {
        PluginManager manager = pluginManager();
        PluginClassLoader classLoader = new PluginClassLoader(jarFile, Static.getClassLoader(), dependencies);
        List<Plugin> plugins = manager.loadPlugins(classLoader.getClasses(), null);
        PluginContext.getLoadedPlugins().put(jarFile.getAbsolutePath(), new PluginContext(classLoader, plugins, jarFile.lastModified()));
        if (start) {
            manager.loadDefaultPluginConfiguration(plugins);
            onEventThread(() -> {
                for (Plugin plugin : plugins) {
                    manager.startPlugin(plugin);
                }
                return null;
            });
        }
        return plugins;
    }

    /**
     * @param manager the plugin manager, when it is known before the injector hands it out
     */
    public static void usePluginManager(PluginManager manager) {
        if (pluginManager == null) {
            pluginManager = manager;
        }
    }

    private static <T> T onEventThread(Callable<T> task) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return task.call();
        }
        FutureTask<T> future = new FutureTask<>(task);
        SwingUtilities.invokeAndWait(future);
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw (Exception) cause;
        }
    }

    private static PluginManager pluginManager() {
        if (pluginManager == null) {
            pluginManager = Static.getInjector().getInstance(PluginManager.class);
        }
        return pluginManager;
    }

    public static void forceRebuildPluginList() {
        try
        {
            PluginManager pluginManager = Static.getInjector().getInstance(PluginManager.class);
            Plugin plugin = pluginManager.getPlugins().stream()
                    .filter(p -> p.getClass().getName().equals("net.runelite.client.plugins.config.ConfigPlugin"))
                    .findFirst()
                    .orElse(null);

            if (plugin == null) {
                return;
            }

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
                        return Files.walk(dir, FileVisitOption.FOLLOW_LINKS);
                    } catch (IOException e) {
                        return Stream.empty();
                    }
                })
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".jar"))
                .collect(Collectors.toList());
    }
}

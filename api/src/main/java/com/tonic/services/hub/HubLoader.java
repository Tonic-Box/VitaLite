package com.tonic.services.hub;

import com.tonic.Logger;
import com.tonic.services.hotswapper.PluginContext;
import com.tonic.services.hotswapper.PluginReloader;
import net.runelite.client.plugins.PluginManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads the Hub's installed plugins when the client starts, after RuneLite's sideloaded plugins, each after the plugins
 * it requires; RuneLite then starts them with the rest. Jars left over from an earlier uninstall or update are deleted
 * first.
 */
public final class HubLoader {
    private static boolean loaded;

    private HubLoader() {
    }

    /**
     * @param manager the client's plugin manager
     */
    public static synchronized void load(PluginManager manager) {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            PluginReloader.usePluginManager(manager);
            HubState state = HubState.get();
            deletePending();
            for (HubState.Installed installed : order(state.installed)) {
                File jar = new File(installed.file);
                if (!jar.isFile()) {
                    Logger.error("Vita Hub: " + installed.id + " is installed but its jar is missing; reinstall it from the Hub");
                    continue;
                }
                if (!dependenciesLoaded(installed)) {
                    Logger.error("Vita Hub: " + installed.id + " was not loaded because a plugin it requires is not loaded");
                    continue;
                }
                try {
                    PluginReloader.loadPlugin(jar, dependencyLoaders(installed), false);
                } catch (Throwable e) {
                    Logger.error("Vita Hub: could not load " + installed.id + ": " + e);
                }
            }
        } catch (Throwable e) {
            Logger.error("Vita Hub: loading installed plugins failed: " + e);
        }
        HubService.get().refresh();
    }

    /**
     * @param installed an installed plugin
     * @return the class loaders of the loaded plugins it requires
     */
    static List<ClassLoader> dependencyLoaders(HubState.Installed installed) {
        List<ClassLoader> loaders = new ArrayList<>();
        HubState state = HubState.get();
        for (String id : installed.required()) {
            HubState.Installed required = state.installed.get(id);
            PluginContext context = required == null ? null : PluginContext.getLoadedPlugins().get(new File(required.file).getAbsolutePath());
            if (context != null) {
                loaders.add(context.getClassLoader());
            }
        }
        return loaders;
    }

    private static boolean dependenciesLoaded(HubState.Installed installed) {
        HubState state = HubState.get();
        for (String id : installed.required()) {
            HubState.Installed required = state.installed.get(id);
            if (required == null || !PluginContext.getLoadedPlugins().containsKey(new File(required.file).getAbsolutePath())) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param installed the installed plugins by id
     * @return them ordered so each comes after the plugins it requires; plugins in a cycle are left out
     */
    static List<HubState.Installed> order(Map<String, HubState.Installed> installed) {
        List<HubState.Installed> order = new ArrayList<>();
        Set<String> done = new LinkedHashSet<>();
        for (String id : installed.keySet()) {
            visit(id, installed, new LinkedHashSet<>(), done, order);
        }
        return order;
    }

    private static boolean visit(String id, Map<String, HubState.Installed> installed, Set<String> visiting, Set<String> done, List<HubState.Installed> order) {
        if (done.contains(id)) {
            return true;
        }
        HubState.Installed plugin = installed.get(id);
        if (plugin == null || !visiting.add(id)) {
            return false;
        }
        boolean satisfied = true;
        for (String required : plugin.required()) {
            satisfied &= visit(required, installed, visiting, done, order);
        }
        visiting.remove(id);
        if (satisfied) {
            done.add(id);
            order.add(plugin);
        }
        return satisfied;
    }

    private static void deletePending() {
        HubState state = HubState.get();
        synchronized (state) {
            if (state.pendingDeletes.isEmpty()) {
                return;
            }
            Set<String> inUse = new LinkedHashSet<>();
            for (HubState.Installed installed : state.installed.values()) {
                inUse.add(new File(installed.file).getAbsolutePath());
            }
            List<String> remaining = new ArrayList<>();
            for (String file : state.pendingDeletes) {
                if (inUse.contains(new File(file).getAbsolutePath())) {
                    continue;
                }
                try {
                    Files.deleteIfExists(Paths.get(file));
                } catch (IOException e) {
                    remaining.add(file);
                }
            }
            state.pendingDeletes = remaining;
            state.save();
        }
    }
}

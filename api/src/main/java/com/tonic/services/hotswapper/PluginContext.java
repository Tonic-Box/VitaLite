package com.tonic.services.hotswapper;

import lombok.Getter;
import net.runelite.client.plugins.Plugin;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class PluginContext {
    @Getter
    private static final Map<String, PluginContext> loadedPlugins = new ConcurrentHashMap<>();

    public static PluginContext of(String clazz)
    {
        for(var entry : loadedPlugins.entrySet())
        {
            for(Object c : entry.getValue().getPlugins())
            {
                if(c.getClass().getName().equals(clazz))
                {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private final PluginClassLoader classLoader;
    private final List<Plugin> plugins;
    private final long lastModified;

    public PluginContext(PluginClassLoader classloader, List<Plugin> plugins, long modified) {
        this.classLoader = classloader;
        this.plugins = plugins;
        this.lastModified = modified;
    }

    public File getFile() {
        for (var entry : loadedPlugins.entrySet())
        {
            if (entry.getValue() == this) {
                return new File(entry.getKey());
            }
        }
        return null;
    }
}

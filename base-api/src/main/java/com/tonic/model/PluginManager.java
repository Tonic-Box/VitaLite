package com.tonic.model;

import com.tonic.util.ReflectBuilder;
import com.tonic.util.ReflectUtil;
import lombok.SneakyThrows;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

public class PluginManager
{
    private final Object instance;
    private final Class<?> pluginClass;
    PluginManager(Guice injector) throws ClassNotFoundException {
        this.instance = injector.getBinding("net.runelite.client.plugins.PluginManager");
        this.pluginClass = instance.getClass().getClassLoader().loadClass("net.runelite.client.plugins.Plugin");
    }

    @SneakyThrows
    public List<?> loadPlugins(List<Class<?>> plugins)
    {
        return (List<?>) ReflectUtil.getMethod(
                instance,
                "loadPlugins",
                new Class<?>[]{ List.class, BiConsumer.class },
                new Object[]{ plugins, null }
        );
    }

    @SneakyThrows
    public Object loadDefaultPluginConfiguration(Collection<?> plugins)
    {
        return ReflectUtil.getMethod(
                instance,
                "loadDefaultPluginConfiguration",
                new Class<?>[]{ Collection.class },
                new Object[]{ plugins }
        );
    }

    @SneakyThrows
    public Object startPlugin(Object plugin)
    {
        Class<?> pluginClass = instance.getClass().getClassLoader().loadClass("net.runelite.client.plugins.Plugin");
        return ReflectUtil.getMethod(
                instance,
                "startPlugin",
                new Class<?>[]{ pluginClass },
                new Object[]{ plugin }
        );
    }

    @SneakyThrows
    public Collection<?> getPlugins() {
        return (Collection<?>) ReflectUtil.getMethod(
                instance,
                "getPlugins",
                new Class<?>[]{},
                new Object[]{}
        );
    }

    @SneakyThrows
    public boolean isPluginEnabled(Object plugin) {
        return (boolean) ReflectUtil.getMethod(
                instance,
                "isPluginEnabled",
                new Class<?>[]{pluginClass},
                new Object[]{plugin}
        );
    }

    /**
     * Finds a plugin instance by its fully qualified class name.
     *
     * @param classFqdn the fully qualified class name of the plugin
     * @return the plugin instance, or null if not found
     */
    public Object findPluginByClass(String classFqdn) {
        Collection<?> plugins = getPlugins();
        for (Object plugin : plugins) {
            if (plugin.getClass().getName().equals(classFqdn)) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Stops a plugin by its fully qualified class name if it's currently active.
     *
     * @param classFqdn the fully qualified class name of the plugin
     * @return true if the plugin was stopped, false if not found or not active
     */
    public Object stopPlugin(String classFqdn) {
        try
        {
            Object plugin = findPluginByClass(classFqdn);
            if (plugin != null && isPluginEnabled(plugin)) {
                ReflectUtil.getMethod(
                        instance,
                        "stopPlugin",
                        new Class<?>[]{pluginClass},
                        new Object[]{plugin}
                );
                return plugin;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}

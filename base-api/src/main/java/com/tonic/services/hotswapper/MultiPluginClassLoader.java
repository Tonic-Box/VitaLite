package com.tonic.services.hotswapper;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MultiPluginClassLoader extends ClassLoader {
    private final Map<String, PluginClassLoader> pluginLoaders = new HashMap<>();
    private final Map<String, List<Class<?>>> pluginClasses = new HashMap<>();
    private ClassLoader parent;

    public MultiPluginClassLoader(File[] plugins, ClassLoader parent) {
        super(parent);
        this.parent = parent;

        for (var jar : plugins) {
            load(jar);
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return parent.loadClass(name);
        }
        catch (Exception ignored) {}

        return findClass(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (PluginClassLoader loader : pluginLoaders.values()) {
            try {
                return loader.loadClass(name, false);
            } catch (ClassNotFoundException ignored) {
            }
        }

        throw new ClassNotFoundException();
    }

    public void reload(File jar) {
        load(jar);
    }

    private void load(File jar) {
        try {
            var loader = new PluginClassLoader(jar, this);
            pluginLoaders.put(jar.getAbsolutePath(), loader);
            pluginClasses.put(jar.getAbsolutePath(), loader.getPluginClasses());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public PluginClassLoader getClassLoader(File jar) {
        return pluginLoaders.get(jar.getAbsolutePath());
    }

    public List<Class<?>> getPluginClasses(File jar) {
        return pluginClasses.get(jar.getAbsolutePath());
    }

    public List<Class<?>> getPluginClasses() {
        return pluginClasses.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
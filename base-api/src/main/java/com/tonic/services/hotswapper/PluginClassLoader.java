package com.tonic.services.hotswapper;

import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

public class PluginClassLoader extends URLClassLoader {
    private static final byte[] PLUGIN_DESCRIPTOR = "Lnet/runelite/client/plugins/PluginDescriptor;".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] LOAD_ALL_CLASSES = ("L" + LoadAllClasses.class.getName().replace('.', '/') + ";").getBytes(StandardCharsets.US_ASCII);

    private final ClassLoader parent;
    @Getter
    private final File pluginFile;
    private final JarFile jarFile;
    private final List<ClassLoader> dependencies;

    public PluginClassLoader(File plugin, ClassLoader parent) throws MalformedURLException
    {
        this(plugin, parent, List.of());
    }

    /**
     * @param plugin       the plugin jar
     * @param parent       the loader of VitaLite and RuneLite
     * @param dependencies the loaders of the plugins this one requires, searched after its own jar and before the parent
     * @throws MalformedURLException if the jar's path is not a valid URL
     */
    public PluginClassLoader(File plugin, ClassLoader parent, List<ClassLoader> dependencies) throws MalformedURLException
    {
        super(new URL[]{plugin.toURI().toURL()}, null);
        this.dependencies = List.copyOf(dependencies);
        this.pluginFile = plugin;
        try
        {
            jarFile = new JarFile(plugin, true, ZipFile.OPEN_READ, JarFile.runtimeVersion());
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        this.parent = parent;
    }


    /**
     * @return the jar's classes that carry a PluginDescriptor and extend Plugin
     * @throws IOException if the jar cannot be read or a class cannot be loaded
     */
    public List<Class<?>> getPluginClasses() throws IOException {
        return getClasses().stream()
            .filter(this::inheritsPluginClass)
            .collect(Collectors.toList());
    }

    /**
     * Loads the jar's plugin classes, found by their class files naming PluginDescriptor, leaving the rest of the jar
     * to load when it is used; a jar with a plugin class marked LoadAllClasses has every class loaded.
     *
     * @return the classes loaded
     * @throws IOException if the jar cannot be read or a class cannot be loaded
     */
    public List<Class<?>> getClasses() throws IOException {
        List<String> paths = new ArrayList<>();
        Set<String> pluginPaths = new HashSet<>();
        boolean loadAll = false;
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String path = entry.getName();
            if (!path.endsWith(".class") || path.startsWith("META-INF/") || path.endsWith("module-info.class")) {
                continue;
            }
            paths.add(path);

            byte[] bytes;
            try (InputStream in = jarFile.getInputStream(entry)) {
                bytes = in.readAllBytes();
            }
            if (contains(bytes, PLUGIN_DESCRIPTOR)) {
                pluginPaths.add(path);
                loadAll |= contains(bytes, LOAD_ALL_CLASSES);
            }
        }

        List<Class<?>> classes = new ArrayList<>();
        for (String path : paths) {
            if (!loadAll && !pluginPaths.contains(path)) {
                continue;
            }
            String name = path.substring(0, path.length() - ".class".length()).replace('/', '.');
            try {
                classes.add(loadClass(name));
            } catch (ClassNotFoundException e) {
                throw new IOException("Cannot load " + name, e);
            }
        }
        return classes;
    }

    private static boolean contains(byte[] bytes, byte[] pattern) {
        outer:
        for (int start = 0; start <= bytes.length - pattern.length; start++) {
            for (int i = 0; i < pattern.length; i++) {
                if (bytes[start + i] != pattern[i]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    public boolean inheritsPluginClass(Class<?> clazz) {
        if (clazz.getSuperclass() == null) {
            return false;
        }

        if (clazz.getSuperclass().getName().endsWith(".Plugin")) {
            return true;
        }

        return inheritsPluginClass(clazz.getSuperclass());
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        try
        {
            return super.loadClass(name);
        }
        catch (Throwable ex)
        {
            for (ClassLoader dependency : dependencies)
            {
                try
                {
                    return dependency.loadClass(name);
                }
                catch (ClassNotFoundException ignored)
                {
                }
            }
            return parent.loadClass(name);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/').concat(".class");
        JarEntry entry = jarFile.getJarEntry(path);
        if (entry == null) throw new ClassNotFoundException(name);

        try (InputStream in = jarFile.getInputStream(entry)) {
            byte[] bytes = in.readAllBytes();
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    @Override
    public void close() throws IOException {
        jarFile.close();
        super.close();
    }
}
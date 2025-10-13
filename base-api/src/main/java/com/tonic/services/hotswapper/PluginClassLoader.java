package com.tonic.services.hotswapper;

import com.google.common.reflect.ClassPath;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

public class PluginClassLoader extends URLClassLoader {
    private final JarFile jarFile;
    private ClassLoader parent;

    public PluginClassLoader(File plugin, ClassLoader parent) throws MalformedURLException
    {
        super(new URL[]{plugin.toURI().toURL()}, null);
        try
        {
            jarFile = new JarFile(plugin, true, ZipFile.OPEN_READ, JarFile.runtimeVersion());
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        this.parent = parent;
    }

    public List<Class<?>> getPluginClasses() throws IOException {
        return getClasses().stream()
            .filter(this::inheritsPluginClass)
            .collect(Collectors.toList());
    }

    public List<Class<?>> getClasses() throws IOException {
        return ClassPath.from(this)
                .getAllClasses()
                .stream()
                .filter(info -> !info.getName().equals("module-info"))
                .filter(info -> !info.getName().startsWith("META-INF"))
                .map(ClassPath.ClassInfo::load)
                .collect(Collectors.toList());
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
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        try {
            if (parent != null) {
                return parent.loadClass(name);
            }
        }
        catch (ClassNotFoundException ignored) {};

        return findClass(name);
    }

    public Class<?> loadClass(String name, boolean allowParentDelegation) throws ClassNotFoundException {
        if (allowParentDelegation) {
            return loadClass(name);
        }

        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        return findClass(name);
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
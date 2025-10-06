package com.tonic.services.hotswapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

public class PluginClassLoader extends URLClassLoader {
    private final ClassLoader parent;
    private final JarFile jarFile;

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

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        try
        {
            return super.loadClass(name);
        }
        catch (Throwable ex)
        {
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
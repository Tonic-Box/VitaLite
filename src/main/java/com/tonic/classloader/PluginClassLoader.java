package com.tonic.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoader extends URLClassLoader
{
    private final ClassLoader parent;

    public PluginClassLoader(File plugin, ClassLoader parent) throws MalformedURLException
    {
        super(new URL[]{plugin.toURI().toURL()}, null);

        this.parent = parent;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        try
        {
            return super.loadClass(name);
        }
        catch (ClassNotFoundException ex)
        {
            return parent.loadClass(name);
        }
    }
}
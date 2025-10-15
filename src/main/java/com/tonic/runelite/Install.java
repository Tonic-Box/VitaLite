package com.tonic.runelite;

import com.tonic.VitaLite;
import com.tonic.services.hotswapper.PluginClassLoader;
import com.tonic.vitalite.Main;
import com.tonic.Static;
import com.tonic.model.Guice;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class Install {
    /**
     * Don't remove, call is injected see @{PluginManagerMixin::loadCorePlugins}
     *
     * @param original list of plugin classes to load
     */
    public void injectBuiltInPlugins(List<Class<?>> original) {
        try
        {
            File builtIns = loadBuildIns().toFile();
            PluginClassLoader classLoader = new PluginClassLoader(builtIns, Main.CLASSLOADER);
            original.addAll(classLoader.getPluginClasses());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private Path loadBuildIns() {
        File tempJar = getBuiltIns();
        if (tempJar == null) {
            System.err.println("Failed to load built-in plugins.");
            System.exit(1);
        }
        return tempJar.toPath();
    }

    private static File getBuiltIns()
    {
        String resource = "plugins.jarData";
        try {
            File tempJar = File.createTempFile(resource, ".jar");
            tempJar.deleteOnExit();

            try (InputStream jarStream = VitaLite.class.getResourceAsStream(resource);
                 FileOutputStream fos = new FileOutputStream(tempJar)) {

                if (jarStream == null) {
                    System.err.println("Could not find embedded " + resource + " in resources");
                    return null;
                }

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = jarStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            return tempJar;

        } catch (Exception e) {
            System.err.println("Failed to load embedded JAR: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}

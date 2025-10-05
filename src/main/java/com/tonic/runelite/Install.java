package com.tonic.runelite;

import com.google.common.reflect.ClassPath;
import com.tonic.VitaLite;
import com.tonic.classloader.PluginClassLoader;
import com.tonic.vitalite.Main;
import com.tonic.Static;
import com.tonic.model.Guice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Install {
    /**
     * Don't remove, call is injected see @{PluginManagerMixin::loadCorePlugins}
     *
     * @param original list of plugin classes to load
     */
    public void injectSideLoadPlugins(List<Class<?>> original) {
        List<File> jars = findJars().stream()
                .map(Path::toFile)
                .collect(Collectors.toList());

        for (File jar : jars) {
            try
            {
                ClassLoader classLoader = new PluginClassLoader(jar, Main.CLASSLOADER);
                List<Class<?>> plugins = ClassPath.from(classLoader)
                        .getAllClasses()
                        .stream()
                        .map(ClassPath.ClassInfo::load)
                        .collect(Collectors.toList());
                original.addAll(plugins);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void install() {
        Guice injector = Static.getRuneLite().getInjector();
        Static.set(injector.getBinding("net.runelite.api.Client"), "RL_CLIENT");
    }

    private List<Path> findJars() {
        Path external = Static.RUNELITE_DIR.resolve("externalplugins");
        Path sideloaded = Static.RUNELITE_DIR.resolve("sideloaded-plugins");
        File tempJar = getBuiltIns();
        if (tempJar == null) {
            System.err.println("Failed to load built-in plugins.");
            System.exit(1);
        }
        try {
            Files.createDirectories(external);
            Files.createDirectories(sideloaded);
        } catch (IOException ignored) {
        }

        List<Path> classes = Stream.of(external, sideloaded)
                .flatMap(dir -> {
                    try {
                        return Files.walk(dir);
                    } catch (IOException e) {
                        return Stream.empty();
                    }
                })
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".jar"))
                .collect(Collectors.toList());
        classes.add(tempJar.toPath());
        return classes;
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

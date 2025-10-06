package com.tonic.runelite;

import com.google.common.reflect.ClassPath;
import com.tonic.VitaLite;
import com.tonic.services.hotswapper.PluginClassLoader;
import com.tonic.services.hotswapper.PluginContext;
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
                PluginClassLoader classLoader = new PluginClassLoader(jar, Main.CLASSLOADER);
                List<Class<?>> plugins = ClassPath.from(classLoader)
                        .getAllClasses()
                        .stream()
                        .map(ClassPath.ClassInfo::load)
                        .collect(Collectors.toList());
                original.addAll(plugins);

                List<Class<?>> loadedFromJar = plugins.stream()
                        .filter(c -> {
                            Class<?> parent = c.getSuperclass();
                            if (parent == null)
                                return false;

                            String parentName = parent.getName();
                            return parentName.endsWith(".Plugin") || parentName.endsWith(".VitaPlugin");
                        })
                        .collect(Collectors.toList());

                PluginContext.getLoadedPlugins().put(jar.getAbsolutePath(), new PluginContext(
                        classLoader, new ArrayList<>(), loadedFromJar, jar.lastModified()
                ));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        try
        {
            File builtIns = loadBuildIns().toFile();
            ClassLoader classLoader = new PluginClassLoader(builtIns, Main.CLASSLOADER);
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

    public static void install() {
        Guice injector = Static.getRuneLite().getInjector();
        Static.set(injector.getBinding("net.runelite.api.Client"), "RL_CLIENT");
        Static.set(Main.CLASSLOADER, "CLASSLOADER");
    }

    private Path loadBuildIns() {
        File tempJar = getBuiltIns();
        if (tempJar == null) {
            System.err.println("Failed to load built-in plugins.");
            System.exit(1);
        }
        return tempJar.toPath();
    }

    private List<Path> findJars() {
        Path external = Static.RUNELITE_DIR.resolve("externalplugins");
        Path sideloaded = Static.RUNELITE_DIR.resolve("sideloaded-plugins");
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

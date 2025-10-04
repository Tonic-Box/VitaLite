package com.tonic.runelite;

import com.google.common.reflect.ClassPath;
import com.tonic.VitaLite;
import com.tonic.classloader.PluginClassLoader;
import com.tonic.vitalite.Main;
import com.tonic.Static;
import com.tonic.model.Guice;
import com.tonic.services.CallStackFilter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Install {
    /**
     * Don't remove, call is injected see @{InjectSideLoadCallTransformer}
     *
     * @param original list of plugin classes to load
     */
    public void injectSideLoadPlugins2(List<Class<?>> original) {
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
                        .filter(classInfo -> !classInfo.getName().equals("module-info"))
                        .filter(classInfo -> !classInfo.getSimpleName().equals("module-info"))
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

    public void injectSideLoadPlugins(List<Class<?>> plugins) {
        Queue<ClassByte> classesToLoad = findJars().stream()
                .flatMap(jar -> listFilesInJar(jar).stream())
                .collect(Collectors.toCollection(LinkedList::new));

        while (classesToLoad.peek() != null) {
            ClassByte cb = classesToLoad.poll();
            try {
                if(Main.CLASSLOADER.libraryExists(cb.name))
                  continue;
                Class<?> cls = Main.CLASSLOADER.lookupClass(cb.name, cb.bytes);
                if (cls == null)
                    continue;

                CallStackFilter.processName(cls.getName());
                Class<?> parent = cls.getSuperclass();
                if (parent == null)
                    continue;

                String parentName = parent.getName();
                if (parentName.endsWith(".Plugin") || parentName.endsWith(".VitaPlugin")) {
                    System.out.println("Loaded Sideloaded Plugin: " + cls.getName());
                    plugins.add(cls);
                }
            } catch (NoClassDefFoundError e /*failed to load because it requires another class to load before it!*/) {
                classesToLoad.add(cb);
            } catch (Exception e) {
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
        } catch (IOException e) {
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

    private List<ClassByte> listFilesInJar(Path jarPath) {
        List<ClassByte> classes = new ArrayList<>();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            jar.stream().forEach(entry -> {
                if (entry.isDirectory()) {
                    return;
                }

                String name = entry.getName();

                try (InputStream in = jar.getInputStream(entry)) {
                    byte[] data = in.readAllBytes();

                    if (name.endsWith(".class")) {
                        String fqcn = name.substring(0, name.length() - 6)
                                .replace('/', '.');
                        classes.add(new ClassByte(data, fqcn));
                    } else {
                        Main.CLASSLOADER.addResource(name, data);
                    }
                } catch (IOException io) {
                    System.err.printf("Unable to process %s in %s: %s%n",
                            name, jarPath, io.getMessage());
                }
            });
        } catch (IOException io) {
            io.printStackTrace();
        }

        return classes;
    }
}

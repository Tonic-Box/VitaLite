package com.tonic.runelite;

import com.tonic.vitalite.Main;
import com.tonic.Static;
import com.tonic.model.Guice;
import com.tonic.services.CallStackFilter;

import java.io.ByteArrayOutputStream;
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
     * @param plugins list of plugin classes to load
     */
    public void injectSideLoadPlugins(List<Class<?>> plugins) {
        Queue<ClassByte> classesToLoad = findJars().stream()
                .flatMap(jar -> listFilesInJar(jar).stream())
                .collect(Collectors.toCollection(LinkedList::new));

        while (classesToLoad.peek() != null) {
            ClassByte cb = classesToLoad.poll();
            try {
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

        try {
            Files.createDirectories(external);
            Files.createDirectories(sideloaded);
        } catch (IOException ignored) {
        }

        return Stream.of(external, sideloaded)
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

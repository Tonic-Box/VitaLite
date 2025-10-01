package com.tonic.classloader;

import com.tonic.VitaLite;
import com.tonic.vitalite.Main;
import com.tonic.model.Libs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.HashMap;

public class RLClassLoader extends URLClassLoader {
    private final HashMap<String, byte[]> resources = new HashMap<>();

    public RLClassLoader(URL[] urls) {
        super(urls, Main.class.getClassLoader());
        loadEmbeddedJarAsURL();
    }

    private void loadEmbeddedJarAsURL() {
        try {
            File tempJar = File.createTempFile("api", ".jar");
            tempJar.deleteOnExit();

            try (InputStream jarStream = VitaLite.class.getResourceAsStream("api.jarData");
                 FileOutputStream fos = new FileOutputStream(tempJar)) {

                if (jarStream == null) {
                    System.err.println("Could not find embedded api.jar in resources");
                    return;
                }

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = jarStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            addURL(tempJar.toURI().toURL());

        } catch (Exception e) {
            System.err.println("Failed to load embedded JAR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Class<?> getMain() throws ClassNotFoundException {
        return loadClass("net.runelite.client.RuneLite");
    }

    public void launch(String[] args) throws Exception {
        String[] newArgs = new String[args.length + 1];
        System.arraycopy(args, 0, newArgs, 0, args.length);
        newArgs[args.length] = "--disable-telemetry";
        Class<?> mainClass = getMain();
        Method main = mainClass.getMethod("main", String[].class);
        StringBuilder out = new StringBuilder("RuneLite started with arguments: ");
        for (String arg : newArgs) {
            out.append(arg).append(" ");
        }
        System.out.println(out);
        main.invoke(null, (Object) newArgs);
    }

    public void addResource(String name, byte[] data) {
        if (name == null || data == null) {
            return;
        }
        resources.put(name, data);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            Class<?> loadedClass = this.findLoadedClass(name);
            if (loadedClass != null) {
                return loadedClass;
            }

            byte[] bytes;
            if (ProxyClassProvider.PROXY_CLASSES.containsKey(name)) {
                bytes = ProxyClassProvider.PROXY_CLASSES.get(name);
                loadedClass = loadArtifactClass(name, bytes);
                if (loadedClass != null)
                    return loadedClass;
            }

            bytes = Main.LIBS.gamepackByName(name);
            if (!name.startsWith("net.runelite.") && bytes == null) {
                return super.loadClass(name);
            }

            if (bytes == null)
                bytes = Main.LIBS.classByName(name);

            loadedClass = loadArtifactClass(name, bytes);
            if (loadedClass != null)
                return loadedClass;
        } catch (Exception ignored) {
        }
        return super.loadClass(name);
    }

    private Class<?> loadArtifactClass(String name, byte[] bytes) {
        Class<?> loadedClass;
        name = name.replace("/", ".");
        if (bytes != null) {
            try {
                if (bytes.length > 0) {
                    ProtectionDomain pd = makeProtectionDomainFor(name);
                    loadedClass = defineClass(name, bytes, 0, bytes.length, pd);
                    if (loadedClass != null) {
                        return loadedClass;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public Class<?> forName(String className) throws ClassNotFoundException {
        return Class.forName(className, true, this);
    }

    public Class<?> loadClass(String name, byte[] bytes) {
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return lookupClass(name, bytes);
        }
    }

    public Class<?> lookupClass(String name, byte[] bytes) {
        final ProtectionDomain protDomain = makeProtectionDomainFor(name);
        return defineClass(name, bytes, 0, bytes.length, protDomain);
    }

    public boolean libraryExists(String name)
    {
        return name.startsWith("com.google.gson.") ||
                name.startsWith("net.sf.trove4j.") ||
                name.startsWith("org.json.") ||
                name.startsWith("com.google.code.gson.") ||
                name.startsWith("org.ow2.asm.") ||
                name.startsWith("org.jetbrains.") ||
                name.startsWith("com.google.code.findbugs.") ||
                name.startsWith("com.google.findbugs.") ||
                name.startsWith("org.projectlombok.") ||
                name.startsWith("it.unimi.dsi.") ||
                name.startsWith("org.slf4j.");
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        if (resources.containsKey(name)) {
            return new ByteArrayInputStream(resources.get(name));
        }
        return super.getResourceAsStream(name);
    }

    private ProtectionDomain makeProtectionDomainFor(String className) {
        Libs libs = Main.LIBS;
        Certificate[] certs = libs.getClassCerts().get(className);
        URL jarUrl = libs.getUrls().get(className);

        CodeSource cs = (jarUrl != null)
                ? new CodeSource(jarUrl, certs)
                : new CodeSource(getClass().getProtectionDomain().getCodeSource().getLocation(), (Certificate[]) null);

        return new ProtectionDomain(cs, null, this, null);
    }
}

package com.tonic.plugins.codeeval.completion;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Caches class information for autocompletion by scanning JARs and using reflection
 */
public class ClassCache {
    private final Map<String, ClassInfo> classInfoByFullName = new ConcurrentHashMap<>();
    private final Map<String, ClassInfo> classInfoBySimpleName = new ConcurrentHashMap<>();
    private final Map<String, List<ClassInfo>> classInfoByPackage = new ConcurrentHashMap<>();
    private final ClassLoader classLoader;
    private final Set<String> indexedPackages = new HashSet<>();
    private volatile boolean indexingComplete = false;

    public ClassCache(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Indexes classes from all packages specified
     */
    public void indexPackages(Set<String> packages) {
        if (packages == null || packages.isEmpty()) return;

        indexedPackages.addAll(packages);

        // Try to get URLs from classloader
        if (classLoader instanceof URLClassLoader) {
            URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            for (URL url : urlClassLoader.getURLs()) {
                indexUrl(url, packages);
            }
        }

        // Also scan classpath
        String classpath = System.getProperty("java.class.path");
        if (classpath != null) {
            for (String path : classpath.split(File.pathSeparator)) {
                try {
                    File file = new File(path);
                    if (file.exists()) {
                        indexUrl(file.toURI().toURL(), packages);
                    }
                } catch (Exception ignored) {}
            }
        }

        indexingComplete = true;
    }

    private void indexUrl(URL url, Set<String> packages) {
        try {
            String path = url.getPath();
            if (path.endsWith(".jar")) {
                indexJar(new File(url.toURI()), packages);
            } else {
                // Could be a directory with .class files
                File dir = new File(url.toURI());
                if (dir.isDirectory()) {
                    indexDirectory(dir, "", packages);
                }
            }
        } catch (Exception e) {
            // Silently skip problematic URLs
        }
    }

    private void indexJar(File jarFile, Set<String> packages) {
        if (!jarFile.exists() || !jarFile.canRead()) return;

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.endsWith(".class") && !name.contains("$")) {
                    String className = name.replace('/', '.').replace(".class", "");
                    if (matchesAnyPackage(className, packages)) {
                        cacheClassInfo(className);
                    }
                }
            }
        } catch (Exception e) {
            // Silently skip problematic JARs
        }
    }

    private void indexDirectory(File dir, String packagePrefix, Set<String> packages) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String newPrefix = packagePrefix.isEmpty() ? file.getName() : packagePrefix + "." + file.getName();
                indexDirectory(file, newPrefix, packages);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String className = packagePrefix + "." + file.getName().replace(".class", "");
                if (matchesAnyPackage(className, packages)) {
                    cacheClassInfo(className);
                }
            }
        }
    }

    private boolean matchesAnyPackage(String className, Set<String> packages) {
        for (String pkg : packages) {
            if (className.startsWith(pkg + ".")) {
                // Include classes in this package and all subpackages
                // This ensures enums in subpackages (e.g., net.runelite.api.events.*)
                // are indexed when the parent package is imported
                return true;
            }
        }
        return false;
    }

    private void cacheClassInfo(String className) {
        if (classInfoByFullName.containsKey(className)) return;

        try {
            Class<?> clazz = classLoader.loadClass(className);

            // Skip non-public classes and synthetic/anonymous classes
            if (!java.lang.reflect.Modifier.isPublic(clazz.getModifiers())) return;
            if (clazz.isAnonymousClass() || clazz.isSynthetic()) return;

            ClassInfo info = new ClassInfo(clazz);
            classInfoByFullName.put(className, info);
            classInfoBySimpleName.put(info.getSimpleName(), info);

            // Index by package
            int lastDot = className.lastIndexOf('.');
            if (lastDot > 0) {
                String packageName = className.substring(0, lastDot);
                classInfoByPackage.computeIfAbsent(packageName, k -> new ArrayList<>()).add(info);
            }

        } catch (Throwable e) {
            // Silently skip classes that can't be loaded
        }
    }

    /**
     * Loads a specific class into the cache
     */
    public ClassInfo loadClass(String fullClassName) {
        if (classInfoByFullName.containsKey(fullClassName)) {
            return classInfoByFullName.get(fullClassName);
        }

        try {
            Class<?> clazz = classLoader.loadClass(fullClassName);
            ClassInfo info = new ClassInfo(clazz);
            classInfoByFullName.put(fullClassName, info);
            classInfoBySimpleName.put(info.getSimpleName(), info);
            return info;
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Gets class info by full or simple name, loading on-demand if needed
     */
    public ClassInfo get(String className) {
        if (className == null || className.isEmpty()) return null;

        // Try full name first
        ClassInfo info = classInfoByFullName.get(className);
        if (info != null) {
            return info;
        }

        // Try simple name from cache
        info = classInfoBySimpleName.get(className);
        if (info != null) {
            return info;
        }

        // If it looks like a fully qualified name, try loading directly
        if (className.contains(".")) {
            info = loadClass(className);
            if (info != null) {
                return info;
            }
        }

        // Try loading by simple name from common packages
        return tryLoadBySimpleName(className);
    }

    /**
     * Gets class info by simple name only
     */
    public ClassInfo getBySimpleName(String simpleName) {
        ClassInfo info = classInfoBySimpleName.get(simpleName);
        if (info != null) return info;

        // Try to load from common packages on-demand
        return tryLoadBySimpleName(simpleName);
    }

    /**
     * Attempts to load a class by simple name from known packages
     */
    private ClassInfo tryLoadBySimpleName(String simpleName) {
        // Common package prefixes to try (in priority order)
        String[] commonPackages = {
                "net.runelite.api.",
                "net.runelite.api.coords.",
                "net.runelite.api.widgets.",
                "net.runelite.api.events.",
                "com.tonic.api.",
                "com.tonic.api.entities.",
                "com.tonic.api.game.",
                "com.tonic.api.widgets.",
                "com.tonic.data.wrappers.",
                "com.tonic.services.",
                "com.tonic.",
                "java.util.",
                "java.lang.",
                "java.io."
        };

        for (String pkg : commonPackages) {
            String fullName = pkg + simpleName;
            ClassInfo info = loadClass(fullName);
            if (info != null) {
                return info;
            }
        }

        // Also try indexed packages
        for (String pkg : indexedPackages) {
            String fullName = pkg + "." + simpleName;
            ClassInfo info = loadClass(fullName);
            if (info != null) {
                return info;
            }
        }

        return null;
    }

    /**
     * Gets all classes matching a prefix (for typing class names)
     */
    public List<ClassInfo> getClassesMatching(String prefix) {
        List<ClassInfo> matching = new ArrayList<>();

        for (ClassInfo info : classInfoBySimpleName.values()) {
            if (info.matchesPrefix(prefix)) {
                matching.add(info);
            }
        }

        // Sort by simple name
        matching.sort(Comparator.comparing(ClassInfo::getSimpleName));
        return matching;
    }

    /**
     * Gets all classes in a package
     */
    public List<ClassInfo> getClassesInPackage(String packageName) {
        return classInfoByPackage.getOrDefault(packageName, Collections.emptyList());
    }

    /**
     * Gets methods for a class (loads if needed)
     */
    public List<MethodInfo> getMethodsForClass(String className) {
        ClassInfo info = get(className);
        if (info != null) {
            List<MethodInfo> allMethods = new ArrayList<>();
            allMethods.addAll(info.getMethods());
            allMethods.addAll(info.getStaticMethods());
            return allMethods;
        }
        return Collections.emptyList();
    }

    /**
     * Checks if indexing is complete
     */
    public boolean isIndexingComplete() {
        return indexingComplete;
    }

    /**
     * Gets count of indexed classes
     */
    public int getClassCount() {
        return classInfoByFullName.size();
    }

    /**
     * Gets the underlying class loader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Indexes root package classes (classes with no package, like obfuscated client classes).
     * These are classes like a.class, b.class, etc. from the gamepack JAR.
     */
    public void indexRootPackageClasses() {
        if (classLoader instanceof URLClassLoader) {
            URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            for (URL url : urlClassLoader.getURLs()) {
                indexRootPackageFromUrl(url);
            }
        }

        // Also scan classpath for root package classes
        String classpath = System.getProperty("java.class.path");
        if (classpath != null) {
            for (String path : classpath.split(File.pathSeparator)) {
                try {
                    File file = new File(path);
                    if (file.exists()) {
                        indexRootPackageFromUrl(file.toURI().toURL());
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void indexRootPackageFromUrl(URL url) {
        try {
            String path = url.getPath();
            if (path.endsWith(".jar")) {
                File jarFile = new File(url.toURI());
                if (!jarFile.exists() || !jarFile.canRead()) return;

                try (JarFile jar = new JarFile(jarFile)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();

                        // Root package: no '/' in name, ends with .class, not inner class
                        if (name.endsWith(".class") && !name.contains("/") && !name.contains("$")) {
                            String className = name.replace(".class", "");
                            cacheClassInfo(className);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Silently skip problematic URLs
        }
    }
}

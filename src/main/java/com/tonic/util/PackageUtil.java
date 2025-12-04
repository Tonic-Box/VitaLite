package com.tonic.util;

import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.util.AnnotationUtil;
import com.tonic.util.asm.ClassNodeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;

public class PackageUtil
{
    /**
     * Fetch a map of all valid pairings of mixin class and interface
     * @param mixinPackage package where the mixin classes are
     * @return HashMap(Mixin,IFace)
     */
    public static HashMap<ClassNode,ClassNode> getPairs(String mixinPackage)
    {
        HashMap<ClassNode,ClassNode> pairs = new HashMap<>();

        List<ClassNode> mixins = getClasses(mixinPackage, null);
        List<ClassNode> interfaces = getInterfaces(mixins);

        if(interfaces.isEmpty() || mixins.isEmpty())
            return pairs;

        String name;
        for(ClassNode mixinClazz : mixins)
        {
            try
            {
                name = getSimpleClassName(mixinClazz);

                if(!AnnotationUtil.hasAnnotation(mixinClazz, Mixin.class))
                {
                    if(name.contains("$") || name.toLowerCase().startsWith("vx"))
                    {
                        mixinClazz.access = (mixinClazz.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
                        continue;
                    }
                    System.out.println("[Skipped] <" + name + "> missing @Mixin annotation.");
                    continue;
                }

                String tag = AnnotationUtil.getAnnotation(mixinClazz, Mixin.class, "value");
                if(tag == null || tag.isEmpty())
                {
                    System.out.println("[Skipped] <" + name + "> missing tag identifier in @Mixin annotation.");
                    continue;
                }
                List<String> ext = mixinClazz.interfaces;
                ClassNode ifaceClazz = null;

                if (ext != null && !ext.isEmpty()) {
                    String ifaceName = ext.get(0).replace('/', '.');
                    ifaceClazz = interfaces.stream()
                            .filter(c -> c != null && c.name.replace('/', '.').equals(ifaceName))
                            .findFirst()
                            .orElse(null);
                }

                pairs.put(mixinClazz, ifaceClazz);
            }
            catch (Exception ignored)
            {
                ignored.printStackTrace();
            }
        }
        return pairs;
    }


    private static List<ClassNode> getInterfaces(List<ClassNode> mixins)
    {
        List<ClassNode> interfaces = new ArrayList<>();

        for (ClassNode mixin : mixins) {
            try {
                if (mixin.interfaces == null || mixin.interfaces.isEmpty()) {
                    interfaces.add(null);
                    continue;
                }

                String toMatch = "/" + getSimpleClassName(mixin).replace("Mixin", "");
                String interfaceName = mixin.interfaces.stream()
                        .filter(i -> i.endsWith(toMatch))
                        .findFirst()
                        .orElse(null);

                ClassNode interfaceNode = loadInterfaceClassNode(interfaceName);
                interfaces.add(interfaceNode);
            } catch (Exception e) {
                System.err.println("Failed to load interface for mixin: " + getSimpleClassName(mixin));
                interfaces.add(null); // Ensure list stays synchronized
            }
        }

        return interfaces;
    }

    /**
     * Load a specific interface class by name from the classpath
     */
    private static ClassNode loadInterfaceClassNode(String className) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String resourceName = className + ".class";

            try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
                if (is != null) {
                    return ClassNodeUtil.toNode(is.readAllBytes());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load interface class: " + className);
        }
        return null;
    }

    /**
     * get all classes from a package
     * @param packageName package name
     * @param ignores class names to ignore
     * @return list of CtClass objects from package
     */
    public static List<ClassNode> getClasses(String packageName, List<String> ignores)
    {

        try
        {
            String jarPath = PackageUtil.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            if(jarPath.endsWith(".jar"))
            {
                return getClassesExternal(packageName, ignores);
            }
            else
            {
                return getClassesIntelliJ(packageName, ignores);
            }

        }
        catch (Exception ignored)
        {
        }
        return new ArrayList<>();
    }

    /**
     * get all classes from a package (If running from intellij)
     * @param packageName package name
     * @param ignores class names to ignore
     * @return list of CtClass objects from package
     */
    public static List<ClassNode> getClassesExternal(String packageName, List<String> ignores)
    {
        List<ClassNode> classes = new ArrayList<>();
        try
        {
            String jarPath = PackageUtil.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            String finalPackageName = packageName.replace(".", "/") + "/";
            File file = new File(jarPath);
            try(JarFile jar = new JarFile(file))
            {
                Collections.list(jar.entries()).forEach(e -> {
                    if(e.getName().startsWith(finalPackageName) && !e.getName().replace(finalPackageName, "").contains("/") && e.getName().endsWith(".class"))
                    {
                        if(ignores == null || ignores.stream().noneMatch(s -> e.getName().contains(s)))
                        {
                            try {
                                try (InputStream is = jar.getInputStream(e)) {
                                    classes.add(ClassNodeUtil.toNode(is.readAllBytes()));
                                }
                            } catch (IOException ignored) { }
                        }
                    }
                });
            }
        }
        catch (Exception ignored) {
        }
        return classes;
    }

    /**
     * get all classes from a package (If running from IntelliJ)
     * @param packageName package name
     * @param ignores class names to ignore
     * @return list of CtClass objects from package
     */
    public static List<ClassNode> getClassesIntelliJ(String packageName, List<String> ignores) {
        List<ClassNode> classes = new ArrayList<>();
        try
        {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            List<File> dirs = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                dirs.add(new File(resource.getFile()));
            }
            for (File directory : dirs) {
                classes.addAll(findClasses(directory, ignores));
            }
        }
        catch (Exception ignored) { }
        return classes;
    }

    private static List<ClassNode> findClasses(File directory, List<String> ignores) {
        List<ClassNode> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if(files == null)
            return classes;
        for (File file : files) {
            if (file.getName().endsWith(".class") && (ignores == null || ignores.stream().noneMatch(s -> file.getName().contains(s)))) {
                try {
                    try (DataInputStream stream = new DataInputStream(new DataInputStream(new FileInputStream(file)))) {
                        classes.add(ClassNodeUtil.toNode(stream.readAllBytes()));
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return classes;
    }

    public static String getSimpleClassName(ClassNode classNode) {
        Type type = Type.getObjectType(classNode.name);
        String className = type.getClassName();
        return className.substring(className.lastIndexOf('.') + 1);
    }
}

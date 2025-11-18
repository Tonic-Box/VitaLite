package com.tonic.services.profiler;

import lombok.ToString;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced VM Access - Deep JVM inspection capabilities
 * Provides class metadata inspection, field access, and object header analysis
 */
public class AdvancedVMAccess {

    private static Object internalUnsafe;
    private static MethodHandles.Lookup trustedLookup;
    private static boolean initialized = false;

    private static MethodHandle objectFieldOffsetMethod;
    private static MethodHandle staticFieldOffsetMethod;
    private static MethodHandle staticFieldBaseMethod;

    private static final Map<Class<?>, ClassMetadata> classMetadataCache = new ConcurrentHashMap<>();

    static {
        try {
            initialize();
        } catch (Exception e) {
            System.err.println("AdvancedVMAccess initialization failed: " + e.getMessage());
        }
    }

    private static void initialize() throws Exception {
        internalUnsafe = ModuleBootstrap.getInternalUnsafe();
        trustedLookup = ModuleBootstrap.getTrustedLookup();

        if (internalUnsafe == null || trustedLookup == null) {
            System.err.println("AdvancedVMAccess requires privileged access");
            return;
        }

        Class<?> unsafeClass = internalUnsafe.getClass();

        Method objFieldOffsetMethod = unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class);
        Method staticFieldOffsetMeth = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);
        Method staticFieldBaseMeth = unsafeClass.getDeclaredMethod("staticFieldBase", Field.class);

        objectFieldOffsetMethod = trustedLookup.unreflect(objFieldOffsetMethod);
        staticFieldOffsetMethod = trustedLookup.unreflect(staticFieldOffsetMeth);
        staticFieldBaseMethod = trustedLookup.unreflect(staticFieldBaseMeth);

        initialized = true;
        System.out.println("AdvancedVMAccess initialized - Deep JVM inspection enabled");
    }

    public static boolean isAvailable() {
        return initialized;
    }

    public static String getStatus() {
        return "AdvancedVMAccess: " + (initialized ? "Initialized ✓" : "Failed ✗");
    }

    public static ClassMetadata getClassMetadata(Class<?> clazz) {
        if (!initialized) {
            throw new IllegalStateException("AdvancedVMAccess not initialized");
        }
        return classMetadataCache.computeIfAbsent(clazz, AdvancedVMAccess::analyzeClass);
    }

    private static ClassMetadata analyzeClass(Class<?> clazz) {
        try {
            ClassMetadata metadata = new ClassMetadata();
            metadata.clazz = clazz;
            metadata.name = clazz.getName();
            metadata.modifiers = clazz.getModifiers();
            metadata.isArray = clazz.isArray();
            metadata.isPrimitive = clazz.isPrimitive();
            metadata.isInterface = clazz.isInterface();

            // Analyze fields
            Field[] declaredFields = clazz.getDeclaredFields();
            metadata.fieldCount = declaredFields.length;
            metadata.instanceFields = new ArrayList<>();
            metadata.staticFields = new ArrayList<>();

            for (Field field : declaredFields) {
                FieldInfo fieldInfo = new FieldInfo();
                fieldInfo.field = field;
                fieldInfo.name = field.getName();
                fieldInfo.type = field.getType();
                fieldInfo.modifiers = field.getModifiers();

                try {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        fieldInfo.offset = (Long) staticFieldOffsetMethod.invoke(internalUnsafe, field);
                        fieldInfo.base = staticFieldBaseMethod.invoke(internalUnsafe, field);
                        metadata.staticFields.add(fieldInfo);
                    } else {
                        fieldInfo.offset = (Long) objectFieldOffsetMethod.invoke(internalUnsafe, field);
                        metadata.instanceFields.add(fieldInfo);
                    }
                } catch (Throwable t) {
                    fieldInfo.offset = -1L;
                }
            }

            // Analyze methods
            Method[] declaredMethods = clazz.getDeclaredMethods();
            metadata.methodCount = declaredMethods.length;
            metadata.methods = new ArrayList<>();

            for (Method method : declaredMethods) {
                MethodInfo methodInfo = new MethodInfo();
                methodInfo.method = method;
                methodInfo.name = method.getName();
                methodInfo.returnType = method.getReturnType();
                methodInfo.parameterTypes = method.getParameterTypes();
                methodInfo.modifiers = method.getModifiers();
                methodInfo.isNative = java.lang.reflect.Modifier.isNative(method.getModifiers());
                metadata.methods.add(methodInfo);
            }

            return metadata;

        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze class: " + e.getMessage(), e);
        }
    }

    public static Set<Class<?>> getAllLoadedClasses() {
        if (!initialized) {
            throw new IllegalStateException("AdvancedVMAccess not initialized");
        }

        Set<Class<?>> loadedClasses = new HashSet<>();

        try {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            addClassesFromClassLoader(loadedClasses, systemClassLoader);

            Class<?>[] wellKnownClasses = {
                Object.class, String.class, Class.class, Thread.class,
                System.class, Runtime.class, ClassLoader.class
            };

            Collections.addAll(loadedClasses, wellKnownClasses);

            return loadedClasses;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get loaded classes: " + e.getMessage(), e);
        }
    }

    private static void addClassesFromClassLoader(Set<Class<?>> classes, ClassLoader classLoader) {
        try {
            if (classLoader != null) {
                try {
                    Field classesField = ClassLoader.class.getDeclaredField("classes");
                    classesField.setAccessible(true);
                    Object classesVector = classesField.get(classLoader);

                    if (classesVector instanceof Vector) {
                        Vector<?> vector = (Vector<?>) classesVector;
                        for (Object obj : vector) {
                            if (obj instanceof Class) {
                                classes.add((Class<?>) obj);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static ObjectHeader getObjectHeader(Object obj) {
        if (!initialized || obj == null) {
            return null;
        }

        ObjectHeader header = new ObjectHeader();
        header.object = obj;
        header.clazz = obj.getClass();
        header.headerSize = getObjectHeaderSize();

        return header;
    }

    private static int getObjectHeaderSize() {
        String vmName = System.getProperty("java.vm.name");
        if (vmName.contains("64")) {
            return 12; // 8 bytes mark word + 4 bytes compressed class pointer
        } else {
            return 8;  // 32-bit JVM
        }
    }

    // ==================== DATA STRUCTURES ====================

    @ToString
    public static class ClassMetadata {
        public Class<?> clazz;
        public String name;
        public int modifiers;
        public boolean isArray;
        public boolean isPrimitive;
        public boolean isInterface;
        public int fieldCount;
        public int methodCount;
        public List<FieldInfo> instanceFields = new ArrayList<>();
        public List<FieldInfo> staticFields = new ArrayList<>();
        public List<MethodInfo> methods = new ArrayList<>();
    }

    @ToString
    public static class FieldInfo {
        public Field field;
        public String name;
        public Class<?> type;
        public int modifiers;
        public long offset = -1L;
        public Object base;
    }

    @ToString
    public static class MethodInfo {
        public Method method;
        public String name;
        public Class<?> returnType;
        public Class<?>[] parameterTypes;
        public int modifiers;
        public boolean isNative;
    }

    @ToString
    public static class ObjectHeader {
        public Object object;
        public Class<?> clazz;
        public int headerSize;
    }
}

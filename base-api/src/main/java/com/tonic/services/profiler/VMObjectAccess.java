package com.tonic.services.profiler;

import lombok.ToString;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * VM Object Access - Direct access to JVM runtime objects and structures
 * Provides JVMTI-like functionality purely from Java using internal APIs
 */
public class VMObjectAccess {

    public static boolean isAvailable() {
        return ModuleBootstrap.getInternalUnsafe() != null;
    }

    public static String getStatus() {
        return "VMObjectAccess: " + (isAvailable() ? "Initialized ✓" : "Failed ✗");
    }

    // ==================== THREAD OPERATIONS ====================

    public static Thread[] getAllThreads() {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parent;
        while ((parent = rootGroup.getParent()) != null) {
            rootGroup = parent;
        }

        int threadCount = rootGroup.activeCount();
        Thread[] threads = new Thread[threadCount * 2];
        int actualCount = rootGroup.enumerate(threads, true);

        Thread[] result = new Thread[actualCount];
        System.arraycopy(threads, 0, result, 0, actualCount);
        return result;
    }

    public static ThreadInfo getThreadInfo(Thread thread) {
        ThreadInfo info = new ThreadInfo();
        info.thread = thread;
        info.name = thread.getName();
        info.id = thread.getId();
        info.state = thread.getState();
        info.priority = thread.getPriority();
        info.daemon = thread.isDaemon();
        info.alive = thread.isAlive();

        try {
            StackTraceElement[] stack = thread.getStackTrace();
            info.stackDepth = stack.length;
            info.currentFrame = stack.length > 0 ? stack[0] : null;
        } catch (Exception ignored) {
        }

        return info;
    }

    // ==================== OBJECT SIZE CALCULATION ====================

    public static long getObjectSize(Object obj) {
        if (obj == null) return 0;

        try {
            Class<?> clazz = obj.getClass();
            return calculateObjectSizeEstimate(obj, clazz);
        } catch (Exception e) {
            return -1;
        }
    }

    private static long calculateObjectSizeEstimate(Object obj, Class<?> clazz) {
        long maxOffset = 0;
        int maxFieldSize = 0;

        Class<?> currentClass = clazz;
        while (currentClass != null) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    int fieldSize = getFieldSize(field.getType());
                    // Rough offset estimation
                    maxFieldSize = Math.max(maxFieldSize, fieldSize);
                    maxOffset += fieldSize;
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        long headerSize = System.getProperty("java.vm.name").contains("64") ? 16 : 12;
        long totalSize = headerSize + maxOffset + maxFieldSize;

        if (clazz.isArray()) {
            Class<?> componentType = clazz.getComponentType();
            int length = java.lang.reflect.Array.getLength(obj);
            int componentSize = getFieldSize(componentType);
            totalSize = headerSize + (length * componentSize);
        }

        return (totalSize + 7) & ~7;
    }

    private static int getFieldSize(Class<?> type) {
        if (type == boolean.class || type == byte.class) return 1;
        if (type == char.class || type == short.class) return 2;
        if (type == int.class || type == float.class) return 4;
        if (type == long.class || type == double.class) return 8;
        return 8; // Reference size
    }

    // ==================== MEMORY MANAGEMENT ====================

    public static void forceGarbageCollection() {
        System.gc();
        System.runFinalization();
        System.gc();
    }

    public static MemoryInfo getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        MemoryInfo info = new MemoryInfo();
        info.totalMemory = runtime.totalMemory();
        info.freeMemory = runtime.freeMemory();
        info.maxMemory = runtime.maxMemory();
        info.usedMemory = info.totalMemory - info.freeMemory;
        return info;
    }

    // ==================== DATA STRUCTURES ====================

    @ToString
    public static class ThreadInfo {
        public Thread thread;
        public String name;
        public long id;
        public Thread.State state;
        public int priority;
        public boolean daemon;
        public boolean alive;
        public Long nativeThreadId;
        public int stackDepth;
        public StackTraceElement currentFrame;
    }

    @ToString
    public static class MemoryInfo {
        public long totalMemory;
        public long freeMemory;
        public long maxMemory;
        public long usedMemory;
    }
}

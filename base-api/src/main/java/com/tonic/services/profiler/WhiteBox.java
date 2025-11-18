package com.tonic.services.profiler;

import com.tonic.util.ReflectBuilder;
import lombok.Getter;

import java.lang.reflect.Executable;
import java.util.Objects;

/**
 * WhiteBox - Wrapper for sun.hotspot.WhiteBox using privileged access
 * Provides access to HotSpot WhiteBox testing APIs without requiring -XX:+WhiteBoxAPI
 */
public class WhiteBox {
    @Getter
    private static final Object whiteBoxInstance;
    private static final Class<?> whiteBoxClass;
    private static final boolean available;

    static {
        Object instance = null;
        Class<?> clazz = null;
        boolean isAvailable = false;

        try {
            clazz = Class.forName("sun.hotspot.WhiteBox");
            instance = ReflectBuilder.of(clazz)
                    .staticMethod("getWhiteBox", null, null)
                    .get();

            if (instance != null) {
                isAvailable = true;
            }
        } catch (Exception e) {
            // WhiteBox not available - silent fallback
        }

        whiteBoxInstance = instance;
        whiteBoxClass = clazz;
        available = isAvailable;
    }

    public static boolean isAvailable() {
        return available && ModuleBootstrap.getInternalUnsafe() != null;
    }

    public static String getStatus() {
        if (available) {
            return "WhiteBox: Available ✓ (Bypassed -XX:+WhiteBoxAPI requirement)";
        } else {
            return "WhiteBox: Not Available ✗ (HotSpot WhiteBox class not found)";
        }
    }

    // ==================== MEMORY OPERATIONS ====================

    public static long getObjectAddress(Object obj) {
        Objects.requireNonNull(obj);
        return ReflectBuilder.of(whiteBoxInstance)
                .method("getObjectAddress", new Class<?>[]{Object.class}, new Object[]{obj})
                .get();
    }

    public static long getObjectSize(Object obj) {
        Objects.requireNonNull(obj);
        return ReflectBuilder.of(whiteBoxInstance)
                .method("getObjectSize", new Class<?>[]{Object.class}, new Object[]{obj})
                .get();
    }

    public static boolean isObjectInOldGen(Object obj) {
        Objects.requireNonNull(obj);
        return ReflectBuilder.of(whiteBoxInstance)
                .method("isObjectInOldGen", new Class<?>[]{Object.class}, new Object[]{obj})
                .get();
    }

    public static int getHeapOopSize() {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("getHeapOopSize", null, null)
                .get();
    }

    public static int getVMPageSize() {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("getVMPageSize", null, null)
                .get();
    }

    public static long getHeapAlignment() {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("getHeapAlignment", null, null)
                .get();
    }

    // ==================== GARBAGE COLLECTION ====================

    public static void youngGC() {
        ReflectBuilder.of(whiteBoxInstance)
                .method("youngGC", null, null)
                .get();
    }

    public static void fullGC() {
        ReflectBuilder.of(whiteBoxInstance)
                .method("fullGC", null, null)
                .get();
    }

    public static boolean g1InConcurrentMark() {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("g1InConcurrentMark", null, null)
                .get();
    }

    public static long g1NumFreeRegions() {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("g1NumFreeRegions", null, null)
                .get();
    }

    public static int g1RegionSize() {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("g1RegionSize", null, null)
                .get();
    }

    // ==================== JIT COMPILATION ====================

    public static boolean isMethodCompiled(Executable method) {
        return isMethodCompiled(method, false);
    }

    public static boolean isMethodCompiled(Executable method, boolean isOsr) {
        Objects.requireNonNull(method);
        return ReflectBuilder.of(whiteBoxInstance)
                .method("isMethodCompiled", new Class<?>[]{Executable.class, boolean.class}, new Object[]{method, isOsr})
                .get();
    }

    public static boolean isMethodCompilable(Executable method) {
        return isMethodCompilable(method, -2);
    }

    public static boolean isMethodCompilable(Executable method, int compLevel) {
        return isMethodCompilable(method, compLevel, false);
    }

    public static boolean isMethodCompilable(Executable method, int compLevel, boolean isOsr) {
        Objects.requireNonNull(method);
        return ReflectBuilder.of(whiteBoxInstance)
                .method("isMethodCompilable", new Class<?>[]{Executable.class, int.class, boolean.class}, new Object[]{method, compLevel, isOsr})
                .get();
    }

    public static boolean isMethodQueuedForCompilation(Executable method) {
        Objects.requireNonNull(method);
        return ReflectBuilder.of(whiteBoxInstance)
                .method("isMethodQueuedForCompilation", new Class<?>[]{Executable.class}, new Object[]{method})
                .get();
    }

    public static int deoptimizeMethod(Executable method) {
        return deoptimizeMethod(method, false);
    }

    public static int deoptimizeMethod(Executable method, boolean isOsr) {
        Objects.requireNonNull(method);
        return ReflectBuilder.of(whiteBoxInstance)
                .method("deoptimizeMethod", new Class<?>[]{Executable.class, boolean.class}, new Object[]{method, isOsr})
                .get();
    }

    public static void makeMethodNotCompilable(Executable method) {
        makeMethodNotCompilable(method, -2);
    }

    public static void makeMethodNotCompilable(Executable method, int compLevel) {
        makeMethodNotCompilable(method, compLevel, false);
    }

    public static void makeMethodNotCompilable(Executable method, int compLevel, boolean isOsr) {
        Objects.requireNonNull(method);
        ReflectBuilder.of(whiteBoxInstance)
                .method("makeMethodNotCompilable", new Class<?>[]{Executable.class, int.class, boolean.class}, new Object[]{method, compLevel, isOsr})
                .get();
    }

    public static int getMethodCompilationLevel(Executable method) {
        return getMethodCompilationLevel(method, false);
    }

    public static int getMethodCompilationLevel(Executable method, boolean isOsr) {
        Objects.requireNonNull(method);
        return ReflectBuilder.of(whiteBoxInstance)
                .method("getMethodCompilationLevel", new Class<?>[]{Executable.class, boolean.class}, new Object[]{method, isOsr})
                .get();
    }

    public static boolean enqueueMethodForCompilation(Executable method, int compLevel) {
        return enqueueMethodForCompilation(method, compLevel, -1);
    }

    public static boolean enqueueMethodForCompilation(Executable method, int compLevel, int entryBci) {
        Objects.requireNonNull(method);
        return ReflectBuilder.of(whiteBoxInstance)
                .method("enqueueMethodForCompilation", new Class<?>[]{Executable.class, int.class, int.class}, new Object[]{method, compLevel, entryBci})
                .get();
    }

    public static void clearMethodState(Executable method) {
        Objects.requireNonNull(method);
        ReflectBuilder.of(whiteBoxInstance)
                .method("clearMethodState", new Class<?>[]{Executable.class}, new Object[]{method})
                .get();
    }

    public static void lockCompilation() {
        ReflectBuilder.of(whiteBoxInstance)
                .method("lockCompilation", null, null)
                .get();
    }

    public static void unlockCompilation() {
        ReflectBuilder.of(whiteBoxInstance)
                .method("unlockCompilation", null, null)
                .get();
    }

    public static int getCompileQueueSize(int compLevel) {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("getCompileQueueSize", new Class<?>[]{int.class}, new Object[]{compLevel})
                .get();
    }

    public static void deoptimizeAll() {
        ReflectBuilder.of(whiteBoxInstance)
                .method("deoptimizeAll", null, null)
                .get();
    }

    // ==================== VM FLAGS ====================

    public static boolean isConstantVMFlag(String name) {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("isConstantVMFlag", new Class<?>[]{String.class}, new Object[]{name})
                .get();
    }

    public static boolean isLockedVMFlag(String name) {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("isLockedVMFlag", new Class<?>[]{String.class}, new Object[]{name})
                .get();
    }

    public static void setBooleanVMFlag(String name, boolean value) {
        ReflectBuilder.of(whiteBoxInstance)
                .method("setBooleanVMFlag", new Class<?>[]{String.class, boolean.class}, new Object[]{name, value})
                .get();
    }

    public static void setIntVMFlag(String name, long value) {
        ReflectBuilder.of(whiteBoxInstance)
                .method("setIntVMFlag", new Class<?>[]{String.class, long.class}, new Object[]{name, value})
                .get();
    }

    public static void setStringVMFlag(String name, String value) {
        ReflectBuilder.of(whiteBoxInstance)
                .method("setStringVMFlag", new Class<?>[]{String.class, String.class}, new Object[]{name, value})
                .get();
    }

    public static Boolean getBooleanVMFlag(String name) {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("getBooleanVMFlag", new Class<?>[]{String.class}, new Object[]{name})
                .get();
    }

    public static Long getIntVMFlag(String name) {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("getIntVMFlag", new Class<?>[]{String.class}, new Object[]{name})
                .get();
    }

    public static String getStringVMFlag(String name) {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("getStringVMFlag", new Class<?>[]{String.class}, new Object[]{name})
                .get();
    }

    public static Object getVMFlag(String name) {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("getVMFlag", new Class<?>[]{String.class}, new Object[]{name})
                .get();
    }

    // ==================== UTILITY METHODS ====================

    public static String getCPUFeatures() {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("getCPUFeatures", null, null)
                .get();
    }

    public static long getThreadStackSize() {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("getThreadStackSize", null, null)
                .get();
    }

    public static long getThreadRemainingStackSize() {
        return ReflectBuilder.of(whiteBoxInstance)
                .method("getThreadRemainingStackSize", null, null)
                .get();
    }
}

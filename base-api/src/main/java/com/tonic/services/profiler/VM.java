package com.tonic.services.profiler;

import com.tonic.util.ReflectBuilder;
import lombok.ToString;

import java.util.Map;

/**
 * VM - Wrapper for jdk.internal.misc.VM using privileged access
 * Provides access to JVM internal state and lifecycle management
 */
public class VM {

    private static final Class<?> vmClass;
    private static final boolean available;

    // JVM initialization levels
    public static final int JAVA_LANG_SYSTEM_INITED = 1;
    public static final int MODULE_SYSTEM_INITED = 2;
    public static final int SYSTEM_LOADER_INITIALIZING = 3;
    public static final int SYSTEM_BOOTED = 4;
    public static final int SYSTEM_SHUTDOWN = 5;

    static {
        Class<?> clazz = null;
        boolean isAvailable = false;

        try {
            clazz = Class.forName("jdk.internal.misc.VM");
            if (clazz != null) {
                isAvailable = true;
            }
        } catch (Exception e) {
            // VM not available
        }

        vmClass = clazz;
        available = isAvailable;
    }

    public static boolean isAvailable() {
        return available && ModuleBootstrap.getInternalUnsafe() != null;
    }

    public static String getStatus() {
        if (available) {
            return "VM: Available ✓ (Bypassed jdk.internal.misc access restrictions)";
        } else {
            return "VM: Not Available ✗ (jdk.internal.misc.VM class not found)";
        }
    }

    // ==================== INITIALIZATION LEVEL ====================

    public static int initLevel() {
        return ReflectBuilder.of(vmClass)
            .staticMethod("initLevel", null, null)
            .get();
    }

    public static boolean isModuleSystemInited() {
        return ReflectBuilder.of(vmClass)
            .staticMethod("isModuleSystemInited", null, null)
            .get();
    }

    public static boolean isBooted() {
        return ReflectBuilder.of(vmClass)
            .staticMethod("isBooted", null, null)
            .get();
    }

    public static boolean isShutdown() {
        return ReflectBuilder.of(vmClass)
            .staticMethod("isShutdown", null, null)
            .get();
    }

    // ==================== DIRECT MEMORY ====================

    public static long maxDirectMemory() {
        return ReflectBuilder.of(vmClass)
            .staticMethod("maxDirectMemory", null, null)
            .get();
    }

    public static boolean isDirectMemoryPageAligned() {
        return ReflectBuilder.of(vmClass)
            .staticMethod("isDirectMemoryPageAligned", null, null)
            .get();
    }

    // ==================== SYSTEM PROPERTIES ====================

    public static String getSavedProperty(String key) {
        return ReflectBuilder.of(vmClass)
            .staticMethod("getSavedProperty", new Class<?>[]{String.class}, new Object[]{key})
            .get();
    }

    public static Map<String, String> getSavedProperties() {
        return ReflectBuilder.of(vmClass)
            .staticMethod("getSavedProperties", null, null)
            .get();
    }

    // ==================== FINALIZATION ====================

    public static int getFinalRefCount() {
        return ReflectBuilder.of(vmClass)
            .staticMethod("getFinalRefCount", null, null)
            .get();
    }

    public static int getPeakFinalRefCount() {
        return ReflectBuilder.of(vmClass)
            .staticMethod("getPeakFinalRefCount", null, null)
            .get();
    }

    // ==================== RUNTIME ARGUMENTS ====================

    public static String[] getRuntimeArguments() {
        return ReflectBuilder.of(vmClass)
            .staticMethod("getRuntimeArguments", null, null)
            .get();
    }

    // ==================== VM INFO ====================

    public static VMInfo getVMInfo() {
        VMInfo info = new VMInfo();

        try {
            info.available = isAvailable();
            info.initLevel = initLevel();
            info.isModuleSystemInited = isModuleSystemInited();
            info.isBooted = isBooted();
            info.isShutdown = isShutdown();
            info.maxDirectMemory = maxDirectMemory();
            info.isDirectMemoryPageAligned = isDirectMemoryPageAligned();
            info.finalRefCount = getFinalRefCount();
            info.peakFinalRefCount = getPeakFinalRefCount();

            try {
                info.runtimeArguments = getRuntimeArguments();
            } catch (Exception e) {
                info.runtimeArguments = new String[0];
            }

            info.savedProperties = getSavedProperties();

        } catch (Exception e) {
            info.error = e.getMessage();
        }

        return info;
    }

    @ToString
    public static class VMInfo {
        public boolean available;
        public int initLevel = -1;
        public boolean isModuleSystemInited;
        public boolean isBooted;
        public boolean isShutdown;
        public long maxDirectMemory = -1;
        public boolean isDirectMemoryPageAligned;
        public int finalRefCount = -1;
        public int peakFinalRefCount = -1;
        public String[] runtimeArguments;
        public Map<String, String> savedProperties;
        public String error;

        public String getInitLevelName() {
            switch (initLevel) {
                case JAVA_LANG_SYSTEM_INITED: return "System Initialized";
                case MODULE_SYSTEM_INITED: return "Module System Initialized";
                case SYSTEM_LOADER_INITIALIZING: return "System Loader Initializing";
                case SYSTEM_BOOTED: return "System Booted";
                case SYSTEM_SHUTDOWN: return "System Shutdown";
                default: return "Unknown";
            }
        }
    }
}

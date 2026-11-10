package com.tonic.services.profiler;

import com.tonic.util.ReflectBuilder;

import java.util.Map;

/**
 * Reads jdk.internal.misc.VM through the module bootstrap: launch arguments, saved properties, the direct memory
 * limit and the finalizer queue
 */
public final class VM {
    private VM() {
    }

    /**
     * Checks whether jdk.internal.misc.VM can be read, running the module bootstrap on first call
     * @return whether privileged access is available
     */
    public static boolean isAvailable() {
        return ModuleBootstrap.ensure();
    }

    /**
     * Gets the direct memory limit
     * @return the most direct buffer memory the JVM allows, in bytes
     * @throws IllegalStateException if privileged access is unavailable
     */
    public static long maxDirectMemory() {
        return call("maxDirectMemory");
    }

    /**
     * Gets the system properties as they were when the JVM started
     * @return the saved properties
     * @throws IllegalStateException if privileged access is unavailable
     */
    public static Map<String, String> getSavedProperties() {
        return call("getSavedProperties");
    }

    /**
     * Gets the number of objects waiting for finalization
     * @return the current finalizer queue length
     * @throws IllegalStateException if privileged access is unavailable
     */
    public static int getFinalRefCount() {
        return call("getFinalRefCount");
    }

    /**
     * Gets the longest the finalizer queue has been
     * @return the peak finalizer queue length
     * @throws IllegalStateException if privileged access is unavailable
     */
    public static int getPeakFinalRefCount() {
        return call("getPeakFinalRefCount");
    }

    /**
     * Gets the JVM's launch arguments, as the launcher passed them
     * @return the arguments
     * @throws IllegalStateException if privileged access is unavailable
     */
    public static String[] getRuntimeArguments() {
        return call("getRuntimeArguments");
    }

    private static <T> T call(String method) {
        if (!isAvailable()) {
            throw new IllegalStateException("privileged JVM access is unavailable");
        }
        try {
            return ReflectBuilder.of(Class.forName("jdk.internal.misc.VM")).staticMethod(method, null, null).get();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}

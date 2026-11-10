package com.tonic.services.profiler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * Field offsets and array geometry from jdk.internal.misc.Unsafe, reached through the module bootstrap
 */
public final class AdvancedVMAccess {
    private static MethodHandle objectFieldOffset;
    private static MethodHandle arrayBaseOffset;
    private static MethodHandle arrayIndexScale;
    private static Object unsafe;
    private static Boolean ready;

    private AdvancedVMAccess() {
    }

    /**
     * Checks whether the internal Unsafe can be used, running the module bootstrap on first call
     * @return whether field offsets and array geometry are available
     */
    public static synchronized boolean isAvailable() {
        if (ready == null) {
            ready = initialize();
        }
        return ready;
    }

    /**
     * Gets an instance field's offset from the start of its object
     * @param field a non-static field
     * @return the offset in bytes
     * @throws IllegalStateException if the internal Unsafe is unavailable
     */
    public static long objectFieldOffset(Field field) {
        requireAvailable();
        return (long) invoke(objectFieldOffset, field);
    }

    /**
     * Gets where an array type's first element starts
     * @param arrayClass an array class
     * @return the offset in bytes
     * @throws IllegalStateException if the internal Unsafe is unavailable
     */
    public static int arrayBaseOffset(Class<?> arrayClass) {
        requireAvailable();
        return (int) invoke(arrayBaseOffset, arrayClass);
    }

    /**
     * Gets the size of one element of an array type
     * @param arrayClass an array class
     * @return the element size in bytes
     * @throws IllegalStateException if the internal Unsafe is unavailable
     */
    public static int arrayIndexScale(Class<?> arrayClass) {
        requireAvailable();
        return (int) invoke(arrayIndexScale, arrayClass);
    }

    private static boolean initialize() {
        unsafe = ModuleBootstrap.getInternalUnsafe();
        MethodHandles.Lookup lookup = ModuleBootstrap.getTrustedLookup();
        if (unsafe == null || lookup == null) {
            return false;
        }
        try {
            Class<?> type = unsafe.getClass();
            objectFieldOffset = lookup.unreflect(type.getDeclaredMethod("objectFieldOffset", Field.class)).bindTo(unsafe);
            arrayBaseOffset = lookup.unreflect(type.getDeclaredMethod("arrayBaseOffset", Class.class)).bindTo(unsafe);
            arrayIndexScale = lookup.unreflect(type.getDeclaredMethod("arrayIndexScale", Class.class)).bindTo(unsafe);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static void requireAvailable() {
        if (!isAvailable()) {
            throw new IllegalStateException("privileged JVM access is unavailable");
        }
    }

    private static Object invoke(MethodHandle handle, Object argument) {
        try {
            return handle.invoke(argument);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}

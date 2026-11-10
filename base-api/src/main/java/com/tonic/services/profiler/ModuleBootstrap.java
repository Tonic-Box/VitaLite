package com.tonic.services.profiler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Privileged JVM access without launch flags: the trusted lookup, both Unsafe instances, and every boot module package
 * opened to unnamed modules. Runs once, on first use.
 */
public class ModuleBootstrap
{
    private static Object unsafe;
    private static Object internalUnsafe;
    private static MethodHandles.Lookup trustedLookup;
    private static Boolean ready;

    /**
     * Runs the bootstrap the first time it is called
     * @return whether privileged access is available
     */
    public static synchronized boolean ensure() {
        if (ready == null) {
            try {
                bootstrapUnsafeAccess();
                ready = true;
            } catch (Throwable e) {
                System.err.println("[Profiler] Privileged JVM access unavailable: " + e);
                ready = false;
            }
        }
        return ready;
    }

    /**
     * Gets sun.misc.Unsafe
     * @return the instance, or null when privileged access is unavailable
     */
    public static Object getUnsafe() {
        return ensure() ? unsafe : null;
    }

    /**
     * Gets jdk.internal.misc.Unsafe
     * @return the instance, or null when privileged access or the internal Unsafe is unavailable
     */
    public static Object getInternalUnsafe() {
        return ensure() ? internalUnsafe : null;
    }

    /**
     * Gets the trusted lookup that bypasses access checks
     * @return the lookup, or null when privileged access is unavailable
     */
    public static MethodHandles.Lookup getTrustedLookup() {
        return ensure() ? trustedLookup : null;
    }

    private static void bootstrapUnsafeAccess() throws Exception {
        try {
            Field theUnsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            unsafe = theUnsafeField.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Cannot get Unsafe - this method requires Unsafe access", e);
        }

        Class<?> unsafeClass = unsafe.getClass();
        Method staticFieldBase = unsafeClass.getMethod("staticFieldBase", Field.class);
        Method staticFieldOffset = unsafeClass.getMethod("staticFieldOffset", Field.class);
        Method getObject = unsafeClass.getMethod("getObject", Object.class, long.class);

        Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        Object fieldBase = staticFieldBase.invoke(unsafe, implLookupField);
        long fieldOffset = (Long) staticFieldOffset.invoke(unsafe, implLookupField);
        trustedLookup = (MethodHandles.Lookup) getObject.invoke(unsafe, fieldBase, fieldOffset);

        bootstrapInternalUnsafe();

        openModulePackages();
    }

    private static void bootstrapInternalUnsafe() {
        try {
            Class<?> internalUnsafeClass = Class.forName("jdk.internal.misc.Unsafe");

            Field theInternalUnsafeField = internalUnsafeClass.getDeclaredField("theUnsafe");

            MethodHandle fieldHandle = trustedLookup.unreflectGetter(theInternalUnsafeField);
            internalUnsafe = fieldHandle.invoke();

        } catch (Throwable e) {
            System.out.println("[WARNING] Could not get internal Unsafe: " + e.getClass().getSimpleName());
            try {
                Class<?> internalUnsafeClass = Class.forName("jdk.internal.misc.Unsafe");
                Field theInternalUnsafeField = internalUnsafeClass.getDeclaredField("theUnsafe");

                Method staticFieldBase = unsafe.getClass().getMethod("staticFieldBase", Field.class);
                Method staticFieldOffset = unsafe.getClass().getMethod("staticFieldOffset", Field.class);
                Method getObject = unsafe.getClass().getMethod("getObject", Object.class, long.class);

                Object fieldBase = staticFieldBase.invoke(unsafe, theInternalUnsafeField);
                long fieldOffset = (Long) staticFieldOffset.invoke(unsafe, theInternalUnsafeField);
                internalUnsafe = getObject.invoke(unsafe, fieldBase, fieldOffset);

            } catch (Exception e2) {
                System.out.println("[INFO] Could not access internal Unsafe via any method: " + e2.getClass().getSimpleName());
                internalUnsafe = null;
            }
        }
    }

    private static void openModulePackages() throws Exception {
        Class<?> modulesClass = Class.forName("jdk.internal.module.Modules");
        Method addOpensMethod = modulesClass.getDeclaredMethod("addOpensToAllUnnamed", Module.class, String.class);
        MethodHandle addOpensHandle = trustedLookup.unreflect(addOpensMethod);
        ModuleLayer layer = ModuleLayer.boot();
        Set<Module> allModules = layer.modules();

        for (Module module : allModules) {
            Set<String> packages = module.getPackages();
            if (!packages.isEmpty()) {
                for (String pkg : packages) {
                    try {
                        addOpensHandle.invoke(module, pkg);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }
}

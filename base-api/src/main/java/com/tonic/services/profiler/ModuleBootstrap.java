package com.tonic.services.profiler;

import lombok.Getter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

public class ModuleBootstrap
{
    @Getter
    private static Object unsafe;
    @Getter
    private static Object internalUnsafe;
    @Getter
    private static MethodHandles.Lookup trustedLookup;

    static {
        try {
            bootstrapUnsafeAccess();
        } catch (Exception e) {
            throw new RuntimeException("Failed to bootstrap Unsafe access", e);
        }
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

    private static void bootstrapInternalUnsafe() throws Exception {
        try {
            // Get the internal jdk.internal.misc.Unsafe class
            Class<?> internalUnsafeClass = Class.forName("jdk.internal.misc.Unsafe");

            // Use trusted lookup to access the internal Unsafe field
            Field theInternalUnsafeField = internalUnsafeClass.getDeclaredField("theUnsafe");

            // Use trusted lookup to get unrestricted access to the field
            MethodHandle fieldHandle = trustedLookup.unreflectGetter(theInternalUnsafeField);
            internalUnsafe = fieldHandle.invoke();


        } catch (Throwable e) {
            System.out.println("[WARNING] Could not get internal Unsafe: " + e.getClass().getSimpleName());
            // Fallback: try direct field access using our sun.misc.Unsafe
            try {
                Class<?> internalUnsafeClass = Class.forName("jdk.internal.misc.Unsafe");
                Field theInternalUnsafeField = internalUnsafeClass.getDeclaredField("theUnsafe");

                // Use sun.misc.Unsafe to access the internal field directly
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

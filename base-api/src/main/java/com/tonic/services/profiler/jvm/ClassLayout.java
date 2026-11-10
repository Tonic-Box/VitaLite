package com.tonic.services.profiler.jvm;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.tonic.services.profiler.AdvancedVMAccess;

import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The real memory layout of a class's instances: each field's offset and size from the JVM, and the exact shallow size
 */
public final class ClassLayout {
    public final Class<?> type;
    public final List<Slot> slots;
    public final int headerSize;
    public final int referenceSize;
    public final int alignment;
    public final int contendedPadding;
    public final long shallowSize;

    /**
     * One instance field in the layout
     */
    public static final class Slot {
        public final long offset;
        public final int size;
        public final Field field;

        private Slot(long offset, int size, Field field) {
            this.offset = offset;
            this.size = size;
            this.field = field;
        }
    }

    private ClassLayout(Class<?> type, List<Slot> slots, int headerSize, int referenceSize, int alignment, int contendedPadding, long shallowSize) {
        this.type = type;
        this.slots = slots;
        this.headerSize = headerSize;
        this.referenceSize = referenceSize;
        this.alignment = alignment;
        this.contendedPadding = contendedPadding;
        this.shallowSize = shallowSize;
    }

    /**
     * Lays out a class's instances, including the fields it inherits and the padding after @Contended field groups
     * @param type a class that can have instances
     * @return its layout, fields ordered by offset
     * @throws IllegalArgumentException if the type is an array, primitive, interface or abstract class
     * @throws IllegalStateException if the internal Unsafe is unavailable
     */
    public static ClassLayout of(Class<?> type) {
        if (type.isArray() || type.isPrimitive() || type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            throw new IllegalArgumentException(type.getName() + " has no instances of its own to lay out");
        }
        int referenceSize = AdvancedVMAccess.arrayIndexScale(Object[].class);
        int headerSize = (int) offsetOf(Probe.class, "value");
        int alignment = intOption("ObjectAlignmentInBytes", 8);
        int paddingWidth = intOption("ContendedPaddingWidth", 128);

        List<Slot> slots = new ArrayList<>();
        long end = headerSize;
        int padding = 0;
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            long classEnd = 0;
            boolean contended = isContended(current.getDeclaredAnnotations());
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Slot slot = new Slot(AdvancedVMAccess.objectFieldOffset(field), sizeOf(field.getType(), referenceSize), field);
                slots.add(slot);
                classEnd = Math.max(classEnd, slot.offset + slot.size);
                contended |= isContended(field.getDeclaredAnnotations());
            }
            if (contended && classEnd > 0 && contendedApplies(current)) {
                classEnd += paddingWidth;
                padding += paddingWidth;
            }
            end = Math.max(end, classEnd);
        }
        slots.sort(Comparator.comparingLong(slot -> slot.offset));
        long shallow = (end + alignment - 1) / alignment * alignment;
        return new ClassLayout(type, slots, headerSize, referenceSize, alignment, padding, shallow);
    }

    private static boolean isContended(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            String name = annotation.annotationType().getName();
            if (name.equals("jdk.internal.vm.annotation.Contended") || name.equals("sun.misc.Contended")) {
                return true;
            }
        }
        return false;
    }

    private static boolean contendedApplies(Class<?> type) {
        HotSpotDiagnosticMXBean hotspot = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        boolean enabled = Boolean.parseBoolean(hotspot.getVMOption("EnableContended").getValue());
        boolean restricted = Boolean.parseBoolean(hotspot.getVMOption("RestrictContended").getValue());
        return enabled && (!restricted || type.getClassLoader() == null || type.getClassLoader() == ClassLoader.getPlatformClassLoader());
    }

    private static int sizeOf(Class<?> type, int referenceSize) {
        if (type == long.class || type == double.class) {
            return 8;
        }
        if (type == int.class || type == float.class) {
            return 4;
        }
        if (type == short.class || type == char.class) {
            return 2;
        }
        if (type == byte.class || type == boolean.class) {
            return 1;
        }
        return referenceSize;
    }

    private static long offsetOf(Class<?> type, String field) {
        try {
            return AdvancedVMAccess.objectFieldOffset(type.getDeclaredField(field));
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int intOption(String name, int fallback) {
        try {
            return Integer.parseInt(ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class).getVMOption(name).getValue());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static final class Probe {
        private byte value;
    }
}

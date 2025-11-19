package com.tonic.services.profiler.sampling;

import java.util.Objects;

/**
 * Immutable method signature for profiling analysis
 * Uniquely identifies a method with class, name, and descriptor
 */
public class MethodSignature {
    public static final MethodSignature ROOT = new MethodSignature("<ROOT>", "", "");

    private final String className;
    private final String methodName;
    private final String descriptor;
    private final int hashCode;

    public MethodSignature(String className, String methodName, String descriptor) {
        this.className = className;
        this.methodName = methodName;
        this.descriptor = descriptor;
        this.hashCode = Objects.hash(className, methodName, descriptor);
    }

    /**
     * Create from StackTraceElement
     */
    public static MethodSignature from(StackTraceElement frame) {
        return new MethodSignature(
            frame.getClassName(),
            frame.getMethodName(),
            "" // Java doesn't expose method descriptors in stack traces
        );
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getDescriptor() {
        return descriptor;
    }

    /**
     * Get simple class name (without package)
     */
    public String getSimpleClassName() {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }

    /**
     * Get package name
     */
    public String getPackageName() {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(0, lastDot) : "";
    }

    /**
     * Get full method signature as string
     */
    public String getFullSignature() {
        return className + "." + methodName + descriptor;
    }

    /**
     * Get display name (short form)
     */
    public String getDisplayName() {
        return getSimpleClassName() + "." + methodName + "()";
    }

    /**
     * Check if this is a JDK/framework internal method
     */
    public boolean isSystemMethod() {
        return className.startsWith("java.") ||
               className.startsWith("javax.") ||
               className.startsWith("sun.") ||
               className.startsWith("com.sun.") ||
               className.startsWith("jdk.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodSignature that = (MethodSignature) o;
        return Objects.equals(className, that.className) &&
               Objects.equals(methodName, that.methodName) &&
               Objects.equals(descriptor, that.descriptor);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return getFullSignature();
    }
}

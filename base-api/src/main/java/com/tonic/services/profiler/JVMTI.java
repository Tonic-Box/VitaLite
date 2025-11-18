package com.tonic.services.profiler;

import lombok.ToString;

import java.util.Set;

/**
 * JVMTI - JVM Tool Interface Implementation for Java
 * Unified interface providing comprehensive JVMTI-like API from pure Java
 */
public class JVMTI {

    public static boolean initialize() {
        boolean vmObjectAccessOk = VMObjectAccess.isAvailable();
        boolean advancedVMAccessOk = AdvancedVMAccess.isAvailable();

        if (!vmObjectAccessOk) {
            System.err.println("JVMTI: VMObjectAccess initialization failed");
        }

        if (!advancedVMAccessOk) {
            System.err.println("JVMTI: AdvancedVMAccess initialization failed");
        }

        return vmObjectAccessOk && advancedVMAccessOk;
    }

    public static boolean isAvailable() {
        return VMObjectAccess.isAvailable() && AdvancedVMAccess.isAvailable();
    }

    public static String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("JVMTI Interface Status:\n");
        sb.append("  ").append(VMObjectAccess.getStatus()).append("\n");
        sb.append("  ").append(AdvancedVMAccess.getStatus()).append("\n");
        sb.append("  ").append(VM.getStatus()).append("\n");
        sb.append("  Overall: ").append(isAvailable() ? "Available ✓" : "Unavailable ✗");
        return sb.toString();
    }

    // ==================== THREAD MANAGEMENT ====================

    public static Thread[] getAllThreads() {
        return VMObjectAccess.getAllThreads();
    }

    public static VMObjectAccess.ThreadInfo getThreadInfo(Thread thread) {
        return VMObjectAccess.getThreadInfo(thread);
    }

    // ==================== OBJECT INSPECTION ====================

    public static long getObjectSize(Object obj) {
        return VMObjectAccess.getObjectSize(obj);
    }

    public static AdvancedVMAccess.ObjectHeader getObjectHeader(Object obj) {
        return AdvancedVMAccess.getObjectHeader(obj);
    }

    // ==================== CLASS INSPECTION ====================

    public static Set<Class<?>> getLoadedClasses() {
        return AdvancedVMAccess.getAllLoadedClasses();
    }

    public static AdvancedVMAccess.ClassMetadata getClassMetadata(Class<?> clazz) {
        return AdvancedVMAccess.getClassMetadata(clazz);
    }

    // ==================== MEMORY MANAGEMENT ====================

    public static void forceGarbageCollection() {
        VMObjectAccess.forceGarbageCollection();
    }

    public static VMObjectAccess.MemoryInfo getMemoryInfo() {
        return VMObjectAccess.getMemoryInfo();
    }

    // ==================== VM INFO ====================

    public static JVMInfo getJVMInfo() {
        JVMInfo info = new JVMInfo();

        // Basic JVM properties
        info.javaVersion = System.getProperty("java.version");
        info.javaVendor = System.getProperty("java.vendor");
        info.jvmName = System.getProperty("java.vm.name");
        info.jvmVersion = System.getProperty("java.vm.version");
        info.jvmVendor = System.getProperty("java.vm.vendor");
        info.osName = System.getProperty("os.name");
        info.osArch = System.getProperty("os.arch");
        info.osVersion = System.getProperty("os.version");

        // Thread information
        Thread[] threads = getAllThreads();
        info.threadCount = threads.length;
        info.currentThread = Thread.currentThread().getName();

        // Memory information
        info.memoryInfo = getMemoryInfo();

        // Class information
        Set<Class<?>> classes = getLoadedClasses();
        info.loadedClassCount = classes.size();

        // Availability
        info.jvmtiAvailable = isAvailable();
        info.vmAvailable = VM.isAvailable();

        return info;
    }

    // ==================== DIAGNOSTICS ====================

    public static DiagnosticResults runDiagnostics() {
        DiagnosticResults results = new DiagnosticResults();

        try {
            // Memory diagnostics
            VMObjectAccess.MemoryInfo beforeGC = getMemoryInfo();
            forceGarbageCollection();
            VMObjectAccess.MemoryInfo afterGC = getMemoryInfo();

            results.memoryFreedByGC = beforeGC.usedMemory - afterGC.usedMemory;
            results.memoryUtilization = (double) afterGC.usedMemory / afterGC.totalMemory;

            // Thread diagnostics
            Thread[] allThreads = getAllThreads();
            results.totalThreads = allThreads.length;
            results.daemonThreads = 0;
            results.aliveThreads = 0;

            for (Thread thread : allThreads) {
                if (thread.isDaemon()) results.daemonThreads++;
                if (thread.isAlive()) results.aliveThreads++;
            }

            // Class diagnostics
            Set<Class<?>> loadedClasses = getLoadedClasses();
            results.totalLoadedClasses = loadedClasses.size();
            results.interfaceCount = 0;
            results.arrayClassCount = 0;

            for (Class<?> clazz : loadedClasses) {
                if (clazz.isInterface()) results.interfaceCount++;
                if (clazz.isArray()) results.arrayClassCount++;
            }

            // Object size testing
            results.testObjectSizes = new long[] {
                getObjectSize(new Object()),
                getObjectSize(new String("test")),
                getObjectSize(new int[10]),
                getObjectSize(new Thread())
            };

            results.success = true;

        } catch (Exception e) {
            results.success = false;
            results.error = e.getMessage();
        }

        return results;
    }

    // ==================== DATA STRUCTURES ====================

    @ToString
    public static class JVMInfo {
        public String javaVersion;
        public String javaVendor;
        public String jvmName;
        public String jvmVersion;
        public String jvmVendor;
        public String osName;
        public String osArch;
        public String osVersion;
        public int threadCount;
        public String currentThread;
        public VMObjectAccess.MemoryInfo memoryInfo;
        public int loadedClassCount;
        public boolean jvmtiAvailable;
        public boolean vmAvailable;
    }

    @ToString
    public static class DiagnosticResults {
        public boolean success;
        public String error;
        public long memoryFreedByGC;
        public double memoryUtilization;
        public int totalThreads;
        public int daemonThreads;
        public int aliveThreads;
        public int totalLoadedClasses;
        public int interfaceCount;
        public int arrayClassCount;
        public long[] testObjectSizes;
    }
}

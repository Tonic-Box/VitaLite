package com.tonic.services.profiler;

import lombok.ToString;

import java.lang.management.*;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JIT Compiler Access - Advanced JIT compilation control API
 * Provides monitoring and information about HotSpot JIT compilation behavior
 * Uses standard Java Management APIs that are always available
 */
public class JITCompilerAccess {

    // Compilation levels from HotSpot
    public static final int COMP_LEVEL_NONE = 0;              // Interpreter
    public static final int COMP_LEVEL_SIMPLE = 1;            // C1 full optimization
    public static final int COMP_LEVEL_LIMITED_PROFILE = 2;   // C1 with counters
    public static final int COMP_LEVEL_FULL_PROFILE = 3;      // C1 with full profiling
    public static final int COMP_LEVEL_FULL_OPTIMIZATION = 4; // C2/JVMCI

    private static final Map<Method, CompilationInfo> compilationCache = new ConcurrentHashMap<>();
    private static CompilationMXBean compilationMXBean;

    static {
        try {
            compilationMXBean = ManagementFactory.getCompilationMXBean();
        } catch (Exception e) {
            compilationMXBean = null;
        }
    }

    public static boolean isAvailable() {
        return compilationMXBean != null;
    }

    public static String getCompilerName() {
        if (!isAvailable()) return "Unknown";
        try {
            return compilationMXBean.getName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static long getTotalCompilationTime() {
        if (!isAvailable()) return -1;
        try {
            return compilationMXBean.getTotalCompilationTime();
        } catch (Exception e) {
            return -1;
        }
    }

    public static boolean isCompilationTimeMonitoringSupported() {
        if (!isAvailable()) return false;
        try {
            return compilationMXBean.isCompilationTimeMonitoringSupported();
        } catch (Exception e) {
            return false;
        }
    }

    public static CompilerStatistics getCompilerStatistics() {
        CompilerStatistics stats = new CompilerStatistics();

        if (isAvailable()) {
            stats.compilerName = getCompilerName();
            stats.totalCompilationTime = getTotalCompilationTime();
            stats.compilationTimeMonitoringSupported = isCompilationTimeMonitoringSupported();
            stats.trackedMethodsCount = compilationCache.size();
        }

        Runtime runtime = Runtime.getRuntime();
        stats.availableProcessors = runtime.availableProcessors();
        stats.totalMemory = runtime.totalMemory();
        stats.freeMemory = runtime.freeMemory();

        return stats;
    }

    public static String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("JIT Compiler Status:\n");
        sb.append("  Available: ").append(isAvailable() ? "Yes" : "No").append("\n");

        if (isAvailable()) {
            sb.append("  Compiler: ").append(getCompilerName()).append("\n");
            sb.append("  Time Monitoring: ").append(isCompilationTimeMonitoringSupported() ? "Supported" : "Not Supported").append("\n");

            long totalTime = getTotalCompilationTime();
            if (totalTime >= 0) {
                sb.append("  Total Compilation Time: ").append(totalTime).append(" ms\n");
            }

            sb.append("  Tracked Methods: ").append(compilationCache.size()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Request compilation of a method at specific level
     * Note: Without WhiteBox, this only tracks the request but doesn't control JVM
     */
    public static boolean compileMethod(Method method, int level) {
        if (!isAvailable()) return false;
        validateCompilationLevel(level);

        CompilationInfo info = compilationCache.computeIfAbsent(method, k -> new CompilationInfo());
        info.requestedLevel = level;
        info.compilationRequested = true;
        info.lastCompilationTime = System.currentTimeMillis();

        return true; // Track request, actual compilation controlled by JVM
    }

    /**
     * Get compilation level for a method
     * Note: Without WhiteBox, returns tracked level or -1
     */
    public static int getCompilationLevel(Method method) {
        CompilationInfo info = compilationCache.get(method);
        return info != null ? info.compilationLevel : -1;
    }

    /**
     * Check if method is compiled
     * Note: Without WhiteBox, returns tracked state
     */
    public static boolean isCompiled(Method method) {
        CompilationInfo info = compilationCache.get(method);
        return info != null && info.isCompiled;
    }

    /**
     * Mark method to prevent inlining
     * Note: Without WhiteBox, this only tracks the preference
     */
    public static boolean setDontInline(Method method, boolean dontInline) {
        if (!isAvailable()) return false;

        CompilationInfo info = compilationCache.computeIfAbsent(method, k -> new CompilationInfo());
        info.dontInline = dontInline;
        return true;
    }

    /**
     * Clear method profiling data
     * Note: Without WhiteBox, clears tracking data only
     */
    public static void clearMethodData(Method method) {
        compilationCache.remove(method);
    }

    /**
     * Set custom compilation threshold for a method
     * Note: Without WhiteBox, this only tracks the preference
     */
    public static boolean setCompilationThreshold(Method method, int threshold) {
        if (!isAvailable()) return false;

        CompilationInfo info = compilationCache.computeIfAbsent(method, k -> new CompilationInfo());
        info.customThreshold = threshold;
        return true;
    }

    /**
     * Request deoptimization of a compiled method
     * Note: Without WhiteBox, marks for tracking only
     */
    public static boolean deoptimizeMethod(Method method) {
        if (!isAvailable()) return false;

        CompilationInfo info = compilationCache.computeIfAbsent(method, k -> new CompilationInfo());
        info.forceRecompilation = true;
        clearMethodData(method);
        return true;
    }

    /**
     * Request deoptimization of all compiled methods
     * Note: Without WhiteBox, clears all tracking data only
     */
    public static void deoptimizeAll() {
        if (!isAvailable()) return;

        // Mark all tracked methods for deoptimization
        for (CompilationInfo info : compilationCache.values()) {
            info.forceRecompilation = true;
        }

        // Clear the cache
        compilationCache.clear();
    }

    /**
     * Get number of methods in compile queue
     * Note: Without WhiteBox, returns count of tracked methods
     */
    public static int getCompileQueueSize() {
        return compilationCache.size();
    }

    /**
     * Get comprehensive compilation information for a method
     */
    public static CompilationInfo getCompilationInfo(Method method) {
        CompilationInfo info = compilationCache.computeIfAbsent(method, k -> new CompilationInfo());

        // Update with current tracked state
        info.methodName = method.getName();
        info.className = method.getDeclaringClass().getName();

        return info;
    }

    /**
     * Get all tracked methods
     */
    public static Map<Method, CompilationInfo> getTrackedMethods() {
        return new ConcurrentHashMap<>(compilationCache);
    }

    /**
     * Get detailed compilation status report
     */
    public static String getCompilationStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("===============================================================\n");
        sb.append("          JIT COMPILER ACCESS STATUS REPORT\n");
        sb.append("===============================================================\n\n");

        sb.append("Compiler Information:\n");
        sb.append("  Available:      ").append(isAvailable() ? "Yes" : "No").append("\n");

        if (isAvailable()) {
            sb.append("  Compiler Name:  ").append(getCompilerName()).append("\n");
            sb.append("  Time Tracking:  ").append(isCompilationTimeMonitoringSupported() ? "Supported" : "Not Supported").append("\n");

            long totalTime = getTotalCompilationTime();
            if (totalTime >= 0) {
                sb.append("  Total Time:     ").append(totalTime).append(" ms\n");
            }
        }

        sb.append("\nMethod Tracking:\n");
        sb.append("  Tracked Methods: ").append(compilationCache.size()).append("\n");

        CompilerStatistics stats = getCompilerStatistics();
        sb.append("\nSystem Resources:\n");
        sb.append("  Processors:     ").append(stats.availableProcessors).append("\n");
        sb.append("  Total Memory:   ").append(stats.totalMemory / (1024 * 1024)).append(" MB\n");
        sb.append("  Free Memory:    ").append(stats.freeMemory / (1024 * 1024)).append(" MB\n");

        if (!compilationCache.isEmpty()) {
            sb.append("\nTracked Method Details:\n");
            int count = 0;
            for (Map.Entry<Method, CompilationInfo> entry : compilationCache.entrySet()) {
                if (count++ >= 10) {
                    sb.append("  ... and ").append(compilationCache.size() - 10).append(" more\n");
                    break;
                }
                CompilationInfo info = entry.getValue();
                sb.append("  ").append(info.getFullMethodName());
                if (info.compilationRequested) {
                    sb.append(" [Requested: Level ").append(info.requestedLevel).append("]");
                }
                sb.append("\n");
            }
        }

        sb.append("\n===============================================================\n");

        return sb.toString();
    }

    public static String getLevelName(int level) {
        switch (level) {
            case COMP_LEVEL_NONE: return "Interpreter";
            case COMP_LEVEL_SIMPLE: return "C1 (Simple)";
            case COMP_LEVEL_LIMITED_PROFILE: return "C1 (Limited Profile)";
            case COMP_LEVEL_FULL_PROFILE: return "C1 (Full Profile)";
            case COMP_LEVEL_FULL_OPTIMIZATION: return "C2/JVMCI (Full Optimization)";
            default: return "Unknown (" + level + ")";
        }
    }

    private static void validateCompilationLevel(int level) {
        if (level < 0 || level > 4) {
            throw new IllegalArgumentException("Invalid compilation level: " + level + ". Must be 0-4");
        }
    }

    /**
     * Compilation information for a method
     */
    @ToString
    public static class CompilationInfo {
        public String methodName = "";
        public String className = "";
        public int compilationLevel = -1;
        public boolean isCompiled = false;
        public long invocationCount = 0;
        public long backedgeCount = 0;
        public boolean dontInline = false;
        public int customThreshold = -1;
        public int requestedLevel = -1;
        public boolean compilationRequested = false;
        public boolean forceRecompilation = false;
        public long lastCompilationTime = 0;

        public String getFullMethodName() {
            return className + "." + methodName;
        }

        public String getCompilationLevelName() {
            return getLevelName(compilationLevel);
        }
    }

    /**
     * JIT compiler statistics
     */
    @ToString
    public static class CompilerStatistics {
        public String compilerName = "Unknown";
        public long totalCompilationTime = 0;
        public boolean compilationTimeMonitoringSupported = false;
        public int availableProcessors = 0;
        public long totalMemory = 0;
        public long freeMemory = 0;
        public int trackedMethodsCount = 0;
    }
}

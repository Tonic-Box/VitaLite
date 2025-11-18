package com.tonic.services.profiler.leak;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Analyzes ClassLoader instances for potential memory leaks
 * Particularly useful for detecting plugin/module unload issues
 */
public class ClassLoaderAnalyzer {
    private final Map<Integer, ClassLoaderInfo> classLoaderHistory;
    private long lastAnalysisTime = 0;

    public ClassLoaderAnalyzer() {
        this.classLoaderHistory = new ConcurrentHashMap<>();
    }

    /**
     * Analyze current classloader state
     */
    public ClassLoaderAnalysis analyze() {
        long now = System.currentTimeMillis();
        Map<Integer, ClassLoaderInfo> currentClassLoaders = scanClassLoaders();

        // Detect orphaned classloaders (present in history but "should" be gone)
        List<ClassLoaderInfo> orphaned = new ArrayList<>();
        List<ClassLoaderInfo> active = new ArrayList<>();

        for (Map.Entry<Integer, ClassLoaderInfo> entry : currentClassLoaders.entrySet()) {
            ClassLoaderInfo current = entry.getValue();
            ClassLoaderInfo historical = classLoaderHistory.get(entry.getKey());

            if (historical != null) {
                // Update seen time
                current.lastSeenTimestamp = now;
                current.firstSeenTimestamp = historical.firstSeenTimestamp;

                // Check if it's been around for a long time
                long ageMs = now - historical.firstSeenTimestamp;
                if (ageMs > 300000) { // 5 minutes
                    current.isOld = true;
                }
            } else {
                current.firstSeenTimestamp = now;
                current.lastSeenTimestamp = now;
            }

            active.add(current);
        }

        // Find orphaned (in history but not in current scan)
        for (Map.Entry<Integer, ClassLoaderInfo> entry : classLoaderHistory.entrySet()) {
            if (!currentClassLoaders.containsKey(entry.getKey())) {
                // This classloader was seen before but is now gone (good!)
                // We can optionally track "cleaned up" classloaders
            }
        }

        // Update history
        classLoaderHistory.putAll(currentClassLoaders);
        lastAnalysisTime = now;

        // Identify suspicious classloaders
        List<ClassLoaderInfo> suspicious = new ArrayList<>();
        for (ClassLoaderInfo info : active) {
            if (info.isOld || info.className.contains("PluginClassLoader")) {
                suspicious.add(info);
            }
        }

        return new ClassLoaderAnalysis(
            active.size(),
            suspicious,
            ManagementFactory.getClassLoadingMXBean().getLoadedClassCount(),
            ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount(),
            ManagementFactory.getClassLoadingMXBean().getUnloadedClassCount(),
            now
        );
    }

    /**
     * Scan for current classloaders
     * Note: This is a best-effort scan using available JVM APIs
     */
    private Map<Integer, ClassLoaderInfo> scanClassLoaders() {
        Map<Integer, ClassLoaderInfo> found = new HashMap<>();

        // Get all threads and their context classloaders
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        for (Thread thread : threads) {
            try {
                ClassLoader cl = thread.getContextClassLoader();
                if (cl != null) {
                    int hash = System.identityHashCode(cl);
                    if (!found.containsKey(hash)) {
                        found.put(hash, new ClassLoaderInfo(
                            cl.getClass().getName(),
                            hash,
                            cl.toString()
                        ));
                    }
                }
            } catch (Exception e) {
                // Ignore security exceptions or other issues
            }
        }

        // Also check system classloaders
        ClassLoader systemCL = ClassLoader.getSystemClassLoader();
        if (systemCL != null) {
            int hash = System.identityHashCode(systemCL);
            found.putIfAbsent(hash, new ClassLoaderInfo(
                systemCL.getClass().getName(),
                hash,
                "System ClassLoader"
            ));
        }

        ClassLoader platformCL = ClassLoader.getPlatformClassLoader();
        if (platformCL != null) {
            int hash = System.identityHashCode(platformCL);
            found.putIfAbsent(hash, new ClassLoaderInfo(
                platformCL.getClass().getName(),
                hash,
                "Platform ClassLoader"
            ));
        }

        return found;
    }

    /**
     * Clear analysis history
     */
    public void clear() {
        classLoaderHistory.clear();
        lastAnalysisTime = 0;
    }

    /**
     * Information about a specific classloader
     */
    public static class ClassLoaderInfo {
        public final String className;
        public final int identityHash;
        public final String description;
        public long firstSeenTimestamp;
        public long lastSeenTimestamp;
        public boolean isOld = false;

        public ClassLoaderInfo(String className, int identityHash, String description) {
            this.className = className;
            this.identityHash = identityHash;
            this.description = description;
        }

        public long getAgeMs(long currentTime) {
            return currentTime - firstSeenTimestamp;
        }

        public boolean isPluginClassLoader() {
            return className.contains("PluginClassLoader");
        }

        public boolean isSystemClassLoader() {
            return className.contains("AppClassLoader") ||
                   className.contains("PlatformClassLoader") ||
                   description.contains("System") ||
                   description.contains("Platform");
        }
    }

    /**
     * Results of classloader analysis
     */
    public static class ClassLoaderAnalysis {
        public final int activeClassLoaderCount;
        public final List<ClassLoaderInfo> suspiciousClassLoaders;
        public final long loadedClassCount;
        public final long totalLoadedClassCount;
        public final long unloadedClassCount;
        public final long timestamp;

        public ClassLoaderAnalysis(int activeClassLoaderCount,
                                  List<ClassLoaderInfo> suspiciousClassLoaders,
                                  long loadedClassCount,
                                  long totalLoadedClassCount,
                                  long unloadedClassCount,
                                  long timestamp) {
            this.activeClassLoaderCount = activeClassLoaderCount;
            this.suspiciousClassLoaders = new ArrayList<>(suspiciousClassLoaders);
            this.loadedClassCount = loadedClassCount;
            this.totalLoadedClassCount = totalLoadedClassCount;
            this.unloadedClassCount = unloadedClassCount;
            this.timestamp = timestamp;
        }

        public boolean hasSuspiciousClassLoaders() {
            return !suspiciousClassLoaders.isEmpty();
        }

        public int getSuspiciousCount() {
            return suspiciousClassLoaders.size();
        }

        public long getClassUnloadRate() {
            return totalLoadedClassCount > 0
                ? (unloadedClassCount * 100) / totalLoadedClassCount
                : 0;
        }
    }
}

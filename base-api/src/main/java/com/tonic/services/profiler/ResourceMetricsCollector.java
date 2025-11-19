package com.tonic.services.profiler;

import java.lang.management.*;

/**
 * Collects system resource metrics from JVM management beans
 * Tracks deltas for CPU and GC percentage calculations
 */
public class ResourceMetricsCollector {
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final com.sun.management.OperatingSystemMXBean osBean;

    // GC tracking
    private long lastGcTime = 0;
    private long lastGcCount = 0;
    private long lastTimestamp = 0;

    // Memory pools
    private MemoryPoolMXBean metaspacePool;
    private MemoryPoolMXBean codeCachePool;

    public ResourceMetricsCollector() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();

        // Get OS bean with process CPU load support
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            this.osBean = (com.sun.management.OperatingSystemMXBean) osBean;
        } else {
            this.osBean = null;
        }

        // Find memory pools
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            String name = pool.getName();
            if (name.contains("Metaspace")) {
                this.metaspacePool = pool;
            } else if (name.contains("Code Cache")) {
                this.codeCachePool = pool;
            }
        }

        // Initialize tracking
        this.lastTimestamp = System.currentTimeMillis();
        this.lastGcTime = getTotalGcTime();
        this.lastGcCount = getTotalGcCount();
    }

    /**
     * Collect current resource metrics snapshot
     */
    public MetricSnapshot collect() {
        long now = System.currentTimeMillis();

        // CPU metrics
        double cpuPercent = collectCpuPercent();

        // GC metrics
        long currentGcTime = getTotalGcTime();
        long currentGcCount = getTotalGcCount();

        double gcPercent = 0;
        if (lastTimestamp > 0) {
            long timeDelta = now - lastTimestamp;
            if (timeDelta > 0) {
                long gcTimeDelta = currentGcTime - lastGcTime;
                gcPercent = (gcTimeDelta * 100.0) / timeDelta;
                gcPercent = Math.max(0, Math.min(100, gcPercent)); // Clamp 0-100
            }
        }

        lastGcTime = currentGcTime;
        lastGcCount = currentGcCount;
        lastTimestamp = now;

        // Heap memory
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long heapUsed = heapUsage.getUsed();
        long heapCommitted = heapUsage.getCommitted();
        long heapMax = heapUsage.getMax();

        // Metaspace
        long metaspaceUsed = 0;
        long metaspaceCommitted = 0;
        long metaspaceMax = -1;

        if (metaspacePool != null) {
            MemoryUsage metaspaceUsage = metaspacePool.getUsage();
            metaspaceUsed = metaspaceUsage.getUsed();
            metaspaceCommitted = metaspaceUsage.getCommitted();
            metaspaceMax = metaspaceUsage.getMax();
        }

        // Code Cache
        long codeCacheUsed = 0;
        long codeCacheMax = -1;

        if (codeCachePool != null) {
            MemoryUsage codeCacheUsage = codeCachePool.getUsage();
            codeCacheUsed = codeCacheUsage.getUsed();
            codeCacheMax = codeCacheUsage.getMax();
        }

        // Thread counts
        int threadCount = threadBean.getThreadCount();
        int daemonCount = threadBean.getDaemonThreadCount();
        int peakThreadCount = threadBean.getPeakThreadCount();

        return new MetricSnapshot(
            now,
            cpuPercent,
            gcPercent,
            currentGcCount,
            currentGcTime,
            heapUsed,
            heapCommitted,
            heapMax,
            metaspaceUsed,
            metaspaceCommitted,
            metaspaceMax,
            threadCount,
            daemonCount,
            peakThreadCount,
            codeCacheUsed,
            codeCacheMax
        );
    }

    /**
     * Get JVM process CPU usage percentage (0-100)
     */
    private double collectCpuPercent() {
        if (osBean == null) {
            return 0;
        }

        try {
            double load = osBean.getProcessCpuLoad();
            if (load < 0) {
                // Not available yet (initial readings)
                return 0;
            }
            return load * 100.0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get total GC time across all collectors (milliseconds)
     */
    private long getTotalGcTime() {
        long total = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long time = gc.getCollectionTime();
            if (time > 0) {
                total += time;
            }
        }
        return total;
    }

    /**
     * Get total GC count across all collectors
     */
    private long getTotalGcCount() {
        long total = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = gc.getCollectionCount();
            if (count > 0) {
                total += count;
            }
        }
        return total;
    }

    /**
     * Check if process CPU load is available
     */
    public boolean isCpuLoadAvailable() {
        return osBean != null;
    }

    /**
     * Check if metaspace pool is available
     */
    public boolean isMetaspaceAvailable() {
        return metaspacePool != null;
    }

    /**
     * Check if code cache pool is available
     */
    public boolean isCodeCacheAvailable() {
        return codeCachePool != null;
    }

    /**
     * Reset delta tracking (useful when pausing/resuming)
     */
    public void resetTracking() {
        this.lastTimestamp = System.currentTimeMillis();
        this.lastGcTime = getTotalGcTime();
        this.lastGcCount = getTotalGcCount();
    }
}

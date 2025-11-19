package com.tonic.services.profiler;

import lombok.ToString;

/**
 * Immutable snapshot of system resource metrics at a point in time
 */
@ToString
public class MetricSnapshot {
    public final long timestamp;

    // CPU & GC metrics
    public final double cpuPercent;
    public final double gcPercent;
    public final long gcCount;
    public final long gcTime;

    // Heap memory metrics (bytes)
    public final long heapUsed;
    public final long heapCommitted;
    public final long heapMax;

    // Metaspace metrics (bytes)
    public final long metaspaceUsed;
    public final long metaspaceCommitted;
    public final long metaspaceMax; // -1 if unlimited

    // Thread metrics
    public final int threadCount;
    public final int daemonThreadCount;
    public final int peakThreadCount;

    // Additional memory pools
    public final long codeCacheUsed;
    public final long codeCacheMax;

    public MetricSnapshot(
        long timestamp,
        double cpuPercent,
        double gcPercent,
        long gcCount,
        long gcTime,
        long heapUsed,
        long heapCommitted,
        long heapMax,
        long metaspaceUsed,
        long metaspaceCommitted,
        long metaspaceMax,
        int threadCount,
        int daemonThreadCount,
        int peakThreadCount,
        long codeCacheUsed,
        long codeCacheMax
    ) {
        this.timestamp = timestamp;
        this.cpuPercent = cpuPercent;
        this.gcPercent = gcPercent;
        this.gcCount = gcCount;
        this.gcTime = gcTime;
        this.heapUsed = heapUsed;
        this.heapCommitted = heapCommitted;
        this.heapMax = heapMax;
        this.metaspaceUsed = metaspaceUsed;
        this.metaspaceCommitted = metaspaceCommitted;
        this.metaspaceMax = metaspaceMax;
        this.threadCount = threadCount;
        this.daemonThreadCount = daemonThreadCount;
        this.peakThreadCount = peakThreadCount;
        this.codeCacheUsed = codeCacheUsed;
        this.codeCacheMax = codeCacheMax;
    }

    /**
     * Get heap utilization percentage (0-100)
     */
    public double getHeapUtilization() {
        if (heapMax <= 0) return 0;
        return (heapUsed * 100.0) / heapMax;
    }

    /**
     * Get metaspace utilization percentage (0-100, or -1 if unlimited)
     */
    public double getMetaspaceUtilization() {
        if (metaspaceMax <= 0) return -1; // Unlimited
        return (metaspaceUsed * 100.0) / metaspaceMax;
    }

    /**
     * Get user thread count (non-daemon)
     */
    public int getUserThreadCount() {
        return threadCount - daemonThreadCount;
    }
}

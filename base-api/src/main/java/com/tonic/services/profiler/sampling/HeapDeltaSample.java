package com.tonic.services.profiler.sampling;

import java.util.HashMap;
import java.util.Map;

/**
 * Snapshot of heap memory state and growth
 */
public class HeapDeltaSample {
    public final long timestamp;

    // Heap totals
    public final long heapUsed;
    public final long heapCommitted;
    public final long heapMax;

    // Memory pool deltas
    public final Map<String, PoolDelta> poolDeltas;

    // Derived metrics
    public final double allocationRateMBPerSec;
    public final boolean gcOccurredSinceLast;

    public HeapDeltaSample(
        long timestamp,
        long heapUsed,
        long heapCommitted,
        long heapMax,
        Map<String, PoolDelta> poolDeltas,
        double allocationRateMBPerSec,
        boolean gcOccurredSinceLast
    ) {
        this.timestamp = timestamp;
        this.heapUsed = heapUsed;
        this.heapCommitted = heapCommitted;
        this.heapMax = heapMax;
        this.poolDeltas = new HashMap<>(poolDeltas);
        this.allocationRateMBPerSec = allocationRateMBPerSec;
        this.gcOccurredSinceLast = gcOccurredSinceLast;
    }

    /**
     * Get heap utilization percentage (0-100)
     */
    public double getHeapUtilization() {
        return heapMax > 0 ? (heapUsed * 100.0) / heapMax : 0;
    }

    /**
     * Get pool delta by name
     */
    public PoolDelta getPoolDelta(String poolName) {
        return poolDeltas.get(poolName);
    }

    /**
     * Check if memory pressure is high
     */
    public boolean isHighPressure() {
        return getHeapUtilization() > 85;
    }

    @Override
    public String toString() {
        return String.format("HeapDeltaSample[used=%.1fMB, utilization=%.1f%%, rate=%.2fMB/s]",
            heapUsed / (1024.0 * 1024.0),
            getHeapUtilization(),
            allocationRateMBPerSec);
    }

    /**
     * Memory pool delta information
     */
    public static class PoolDelta {
        public final String poolName;
        public final long usedBefore;
        public final long usedAfter;
        public final long delta;
        public final long maxSize;

        public PoolDelta(String poolName, long usedBefore, long usedAfter, long maxSize) {
            this.poolName = poolName;
            this.usedBefore = usedBefore;
            this.usedAfter = usedAfter;
            this.delta = usedAfter - usedBefore;
            this.maxSize = maxSize;
        }

        public double getUtilization() {
            return maxSize > 0 ? (usedAfter * 100.0) / maxSize : 0;
        }

        public boolean isGrowing() {
            return delta > 0;
        }

        @Override
        public String toString() {
            return String.format("%s: %+d bytes (%.1f%% full)",
                poolName, delta, getUtilization());
        }
    }
}

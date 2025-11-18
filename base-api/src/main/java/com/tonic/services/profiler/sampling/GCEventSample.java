package com.tonic.services.profiler.sampling;

/**
 * Snapshot of garbage collection activity
 */
public class GCEventSample {
    public final long timestamp;

    // GC counts
    public final long youngGCCount;
    public final long fullGCCount;

    // GC times (cumulative)
    public final long youngGCTime;
    public final long fullGCTime;

    // Deltas from previous sample
    public final long youngGCDelta;
    public final long fullGCDelta;
    public final long youngGCTimeDelta;
    public final long fullGCTimeDelta;

    // Derived metrics
    public final double gcOverheadPercent;
    public final double youngGCFrequency; // Collections per second

    public GCEventSample(
        long timestamp,
        long youngGCCount,
        long fullGCCount,
        long youngGCTime,
        long fullGCTime,
        long youngGCDelta,
        long fullGCDelta,
        long youngGCTimeDelta,
        long fullGCTimeDelta,
        double gcOverheadPercent,
        double youngGCFrequency
    ) {
        this.timestamp = timestamp;
        this.youngGCCount = youngGCCount;
        this.fullGCCount = fullGCCount;
        this.youngGCTime = youngGCTime;
        this.fullGCTime = fullGCTime;
        this.youngGCDelta = youngGCDelta;
        this.fullGCDelta = fullGCDelta;
        this.youngGCTimeDelta = youngGCTimeDelta;
        this.fullGCTimeDelta = fullGCTimeDelta;
        this.gcOverheadPercent = gcOverheadPercent;
        this.youngGCFrequency = youngGCFrequency;
    }

    /**
     * Check if any GC occurred
     */
    public boolean hadGC() {
        return youngGCDelta > 0 || fullGCDelta > 0;
    }

    /**
     * Check if Full GC occurred
     */
    public boolean hadFullGC() {
        return fullGCDelta > 0;
    }

    /**
     * Check if GC overhead is high (>5%)
     */
    public boolean isHighGCOverhead() {
        return gcOverheadPercent > 5.0;
    }

    /**
     * Get total GC count
     */
    public long getTotalGCCount() {
        return youngGCCount + fullGCCount;
    }

    /**
     * Get total GC time
     */
    public long getTotalGCTime() {
        return youngGCTime + fullGCTime;
    }

    @Override
    public String toString() {
        return String.format("GCEventSample[young=%d, full=%d, overhead=%.2f%%]",
            youngGCCount, fullGCCount, gcOverheadPercent);
    }
}

package com.tonic.services.profiler.sampling;

import java.util.Objects;

/**
 * Snapshot of heap statistics for a single class type
 * Similar to VisualVM's heap histogram showing per-class memory usage
 */
public class HeapHistogramSample {
    public final String className;
    public final long instanceCount;
    public final long totalBytes;
    public final long timestamp;

    // Derived metrics
    public final long averageSize;
    public final String packageName;
    public final String simpleClassName;

    public HeapHistogramSample(String className, long instanceCount, long totalBytes, long timestamp) {
        this.className = className;
        this.instanceCount = instanceCount;
        this.totalBytes = totalBytes;
        this.timestamp = timestamp;

        // Calculate derived values
        this.averageSize = instanceCount > 0 ? totalBytes / instanceCount : 0;

        // Extract package and simple class name
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            this.packageName = className.substring(0, lastDot);
            this.simpleClassName = className.substring(lastDot + 1);
        } else {
            this.packageName = "<default>";
            this.simpleClassName = className;
        }
    }

    /**
     * Calculate percentage of total heap
     */
    public double getPercentOfTotal(long totalHeapBytes) {
        return totalHeapBytes > 0 ? (totalBytes * 100.0) / totalHeapBytes : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeapHistogramSample that = (HeapHistogramSample) o;
        return Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className);
    }

    @Override
    public String toString() {
        return String.format("%s: %d instances, %d bytes (avg: %d bytes)",
            className, instanceCount, totalBytes, averageSize);
    }
}

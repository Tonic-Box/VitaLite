package com.tonic.services.profiler.recording;

import lombok.ToString;

/**
 * Per-method statistics from JFR profiling samples.
 * Tracks both "self time" (method was executing) and "total time" (method was on stack).
 */
@ToString
public class MethodStats {
    public final String methodKey;  // ClassName.methodName
    public final String className;
    public final String methodName;

    private int selfSamples;   // Times this method was at TOP of stack (executing)
    private int totalSamples;  // Times this method was anywhere on stack

    public MethodStats(String methodKey) {
        this.methodKey = methodKey;
        int lastDot = methodKey.lastIndexOf('.');
        if (lastDot > 0) {
            this.className = methodKey.substring(0, lastDot);
            this.methodName = methodKey.substring(lastDot + 1);
        } else {
            this.className = "";
            this.methodName = methodKey;
        }
    }

    public void incrementSelfSamples() {
        selfSamples++;
    }

    public void incrementTotalSamples() {
        totalSamples++;
    }

    public int getSelfSamples() {
        return selfSamples;
    }

    public int getTotalSamples() {
        return totalSamples;
    }

    /**
     * Get self time as percentage of total recording samples
     */
    public double getSelfPercent(int totalRecordingSamples) {
        if (totalRecordingSamples <= 0) return 0;
        return (selfSamples * 100.0) / totalRecordingSamples;
    }

    /**
     * Get total time as percentage of total recording samples
     */
    public double getTotalPercent(int totalRecordingSamples) {
        if (totalRecordingSamples <= 0) return 0;
        return (totalSamples * 100.0) / totalRecordingSamples;
    }

    /**
     * Estimate self time in milliseconds based on sample period
     */
    public long getEstimatedSelfMs(int samplePeriodMs) {
        return (long) selfSamples * samplePeriodMs;
    }

    /**
     * Estimate total time in milliseconds based on sample period
     */
    public long getEstimatedTotalMs(int samplePeriodMs) {
        return (long) totalSamples * samplePeriodMs;
    }

    /**
     * Get shortened class name (last component only)
     */
    public String getShortClassName() {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(lastDot + 1) : className;
    }

    /**
     * Get display name (ShortClassName.methodName)
     */
    public String getDisplayName() {
        return getShortClassName() + "." + methodName;
    }
}

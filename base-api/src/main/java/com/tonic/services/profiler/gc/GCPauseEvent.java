package com.tonic.services.profiler.gc;

import lombok.Getter;

/**
 * Represents a single GC pause event
 */
@Getter
public class GCPauseEvent {
    private final long timestamp;
    private final String gcName;
    private final String gcAction;
    private final String gcCause;
    private final long duration;
    private final long beforeGC;
    private final long afterGC;
    private final long maxMemory;
    private final GCType gcType;

    public GCPauseEvent(long timestamp, String gcName, String gcAction, String gcCause,
                        long duration, long beforeGC, long afterGC, long maxMemory) {
        this.timestamp = timestamp;
        this.gcName = gcName;
        this.gcAction = gcAction;
        this.gcCause = gcCause;
        this.duration = duration;
        this.beforeGC = beforeGC;
        this.afterGC = afterGC;
        this.maxMemory = maxMemory;
        this.gcType = determineGCType(gcName, gcAction);
    }

    private GCType determineGCType(String name, String action) {
        String combined = (name + " " + action).toLowerCase();

        if (combined.contains("young") || combined.contains("minor") ||
            combined.contains("scavenge") || combined.contains("copy")) {
            return GCType.YOUNG;
        } else if (combined.contains("old") || combined.contains("major") ||
                   combined.contains("full") || combined.contains("mark")) {
            return GCType.OLD;
        }

        return GCType.UNKNOWN;
    }

    public long getMemoryFreed() {
        return beforeGC - afterGC;
    }

    public double getMemoryFreedPercent() {
        return beforeGC > 0 ? ((double) getMemoryFreed() / beforeGC) * 100.0 : 0;
    }

    @Getter
    public enum GCType {
        YOUNG("Young Gen", "Minor collection"),
        OLD("Old Gen", "Major/Full collection"),
        UNKNOWN("Unknown", "Unclassified");

        private final String label;
        private final String description;

        GCType(String label, String description) {
            this.label = label;
            this.description = description;
        }

    }
}

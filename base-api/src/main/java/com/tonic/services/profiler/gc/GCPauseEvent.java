package com.tonic.services.profiler.gc;

/**
 * Represents a single GC pause event
 */
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
        } else if (combined.contains("mixed")) {
            return GCType.MIXED;
        }

        return GCType.UNKNOWN;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getGcName() {
        return gcName;
    }

    public String getGcAction() {
        return gcAction;
    }

    public String getGcCause() {
        return gcCause;
    }

    public long getDuration() {
        return duration;
    }

    public long getBeforeGC() {
        return beforeGC;
    }

    public long getAfterGC() {
        return afterGC;
    }

    public long getMaxMemory() {
        return maxMemory;
    }

    public long getMemoryFreed() {
        return beforeGC - afterGC;
    }

    public double getMemoryFreedPercent() {
        return beforeGC > 0 ? ((double) getMemoryFreed() / beforeGC) * 100.0 : 0;
    }

    public GCType getGcType() {
        return gcType;
    }

    public enum GCType {
        YOUNG("Young Gen", "Minor collection"),
        OLD("Old Gen", "Major/Full collection"),
        MIXED("Mixed", "Both generations"),
        UNKNOWN("Unknown", "Unclassified");

        private final String label;
        private final String description;

        GCType(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }
}

package com.tonic.services.profiler.timeline;

/**
 * Represents a single event in the timeline
 */
public class TimelineEvent implements Comparable<TimelineEvent> {
    private final long timestamp;
    private final EventType type;
    private final String description;
    private final long duration;
    private final EventSeverity severity;
    private final String details;

    public TimelineEvent(long timestamp, EventType type, String description,
                        long duration, EventSeverity severity, String details) {
        this.timestamp = timestamp;
        this.type = type;
        this.description = description;
        this.duration = duration;
        this.severity = severity;
        this.details = details;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public EventType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public long getDuration() {
        return duration;
    }

    public EventSeverity getSeverity() {
        return severity;
    }

    public String getDetails() {
        return details;
    }

    public long getEndTimestamp() {
        return timestamp + duration;
    }

    @Override
    public int compareTo(TimelineEvent other) {
        return Long.compare(this.timestamp, other.timestamp);
    }

    /**
     * Event types for different JVM activities
     */
    public enum EventType {
        GC_YOUNG("GC Young", "Young generation garbage collection", 0xFF9800),
        GC_OLD("GC Old", "Old generation garbage collection", 0xF44336),
        GC_MIXED("GC Mixed", "Mixed garbage collection", 0xFF5722),
        COMPILATION("Compilation", "JIT compilation event", 0x2196F3),
        THREAD_START("Thread Start", "Thread created", 0x4CAF50),
        THREAD_END("Thread End", "Thread terminated", 0x9E9E9E),
        SAFEPOINT("Safepoint", "JVM safepoint pause", 0x9C27B0),
        CLASS_LOAD("Class Load", "Class loading event", 0x00BCD4);

        private final String label;
        private final String description;
        private final int color;

        EventType(String label, String description, int color) {
            this.label = label;
            this.description = description;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }

        public int getColor() {
            return color;
        }
    }

    /**
     * Event severity levels
     */
    public enum EventSeverity {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High"),
        CRITICAL("Critical");

        private final String label;

        EventSeverity(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}

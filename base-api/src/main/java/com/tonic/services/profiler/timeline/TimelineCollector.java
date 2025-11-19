package com.tonic.services.profiler.timeline;

import com.tonic.services.profiler.gc.GCPauseAnalyzer;
import com.tonic.services.profiler.gc.GCPauseEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Collects and aggregates timeline events from multiple sources
 */
public class TimelineCollector {
    private final List<TimelineEvent> events;
    private final int maxEvents;
    private final GCPauseAnalyzer gcAnalyzer;

    public TimelineCollector(int maxEvents, GCPauseAnalyzer gcAnalyzer) {
        this.events = new CopyOnWriteArrayList<>();
        this.maxEvents = maxEvents;
        this.gcAnalyzer = gcAnalyzer;
    }

    /**
     * Collect events from all sources
     */
    public void collectEvents() {
        events.clear();

        // Collect GC events
        if (gcAnalyzer != null) {
            collectGCEvents();
        }

        // Sort events by timestamp
        Collections.sort(events);

        // Trim to max size
        if (events.size() > maxEvents) {
            events.subList(0, events.size() - maxEvents).clear();
        }
    }

    private void collectGCEvents() {
        List<GCPauseEvent> gcEvents = gcAnalyzer.getAllEvents();

        for (GCPauseEvent gcEvent : gcEvents) {
            TimelineEvent.EventType type;
            switch (gcEvent.getGcType()) {
                case YOUNG:
                    type = TimelineEvent.EventType.GC_YOUNG;
                    break;
                case OLD:
                    type = TimelineEvent.EventType.GC_OLD;
                    break;
                case MIXED:
                    type = TimelineEvent.EventType.GC_MIXED;
                    break;
                default:
                    type = TimelineEvent.EventType.GC_YOUNG;
                    break;
            }

            TimelineEvent.EventSeverity severity;
            if (gcEvent.getDuration() > 1000) {
                severity = TimelineEvent.EventSeverity.CRITICAL;
            } else if (gcEvent.getDuration() > 500) {
                severity = TimelineEvent.EventSeverity.HIGH;
            } else if (gcEvent.getDuration() > 100) {
                severity = TimelineEvent.EventSeverity.MEDIUM;
            } else {
                severity = TimelineEvent.EventSeverity.LOW;
            }

            String description = String.format("%s (%d ms)",
                gcEvent.getGcName(), gcEvent.getDuration());

            String details = String.format(
                "Cause: %s\nAction: %s\nDuration: %d ms\nMemory Freed: %.1f MB (%.1f%%)",
                gcEvent.getGcCause(),
                gcEvent.getGcAction(),
                gcEvent.getDuration(),
                gcEvent.getMemoryFreed() / (1024.0 * 1024.0),
                gcEvent.getMemoryFreedPercent()
            );

            TimelineEvent event = new TimelineEvent(
                gcEvent.getTimestamp(),
                type,
                description,
                gcEvent.getDuration(),
                severity,
                details
            );

            events.add(event);
        }
    }

    /**
     * Get all events
     */
    public List<TimelineEvent> getAllEvents() {
        return new ArrayList<>(events);
    }

    /**
     * Get events in time range
     */
    public List<TimelineEvent> getEventsInRange(long startTime, long endTime) {
        List<TimelineEvent> filtered = new ArrayList<>();
        for (TimelineEvent event : events) {
            if (event.getTimestamp() >= startTime && event.getTimestamp() <= endTime) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    /**
     * Get events by type
     */
    public List<TimelineEvent> getEventsByType(TimelineEvent.EventType type) {
        List<TimelineEvent> filtered = new ArrayList<>();
        for (TimelineEvent event : events) {
            if (event.getType() == type) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    /**
     * Get event statistics
     */
    public TimelineStatistics getStatistics() {
        if (events.isEmpty()) {
            return new TimelineStatistics(0, 0, 0, 0, 0, 0, 0, 0);
        }

        int totalEvents = events.size();
        int gcEvents = 0;
        int compilationEvents = 0;
        int threadEvents = 0;
        long totalDuration = 0;
        long longestEvent = 0;

        for (TimelineEvent event : events) {
            totalDuration += event.getDuration();
            if (event.getDuration() > longestEvent) {
                longestEvent = event.getDuration();
            }

            switch (event.getType()) {
                case GC_YOUNG:
                case GC_OLD:
                case GC_MIXED:
                    gcEvents++;
                    break;
                case COMPILATION:
                    compilationEvents++;
                    break;
                case THREAD_START:
                case THREAD_END:
                    threadEvents++;
                    break;
            }
        }

        long oldestEvent = events.get(0).getTimestamp();
        long newestEvent = events.get(events.size() - 1).getTimestamp();
        long timeSpan = newestEvent - oldestEvent;

        return new TimelineStatistics(
            totalEvents,
            gcEvents,
            compilationEvents,
            threadEvents,
            totalDuration,
            longestEvent,
            oldestEvent,
            timeSpan
        );
    }

    /**
     * Timeline statistics
     */
    public static class TimelineStatistics {
        public final int totalEvents;
        public final int gcEvents;
        public final int compilationEvents;
        public final int threadEvents;
        public final long totalDuration;
        public final long longestEvent;
        public final long oldestEventTime;
        public final long timeSpan;

        public TimelineStatistics(int totalEvents, int gcEvents, int compilationEvents,
                                 int threadEvents, long totalDuration, long longestEvent,
                                 long oldestEventTime, long timeSpan) {
            this.totalEvents = totalEvents;
            this.gcEvents = gcEvents;
            this.compilationEvents = compilationEvents;
            this.threadEvents = threadEvents;
            this.totalDuration = totalDuration;
            this.longestEvent = longestEvent;
            this.oldestEventTime = oldestEventTime;
            this.timeSpan = timeSpan;
        }
    }
}

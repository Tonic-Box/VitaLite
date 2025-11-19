package com.tonic.services.profiler.gc;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Monitors and analyzes GC pause events in real-time
 */
public class GCPauseAnalyzer implements NotificationListener {
    private final List<GCPauseEvent> pauseEvents;
    private final int maxEvents;
    private volatile boolean monitoring = false;
    private final List<NotificationEmitter> registeredEmitters;

    // Statistics
    private long totalPauses = 0;
    private long totalPauseTime = 0;
    private long longestPause = 0;
    private GCPauseEvent longestPauseEvent = null;

    public GCPauseAnalyzer(int maxEvents) {
        this.pauseEvents = new CopyOnWriteArrayList<>();
        this.maxEvents = maxEvents;
        this.registeredEmitters = new ArrayList<>();
    }

    /**
     * Start monitoring GC events
     */
    public void startMonitoring() {
        if (monitoring) {
            return;
        }

        monitoring = true;

        // Register listener with all GC beans
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            if (gcBean instanceof NotificationEmitter) {
                NotificationEmitter emitter = (NotificationEmitter) gcBean;
                emitter.addNotificationListener(this, null, null);
                registeredEmitters.add(emitter);
            }
        }
    }

    /**
     * Stop monitoring
     */
    public void stopMonitoring() {
        monitoring = false;

        // Unregister listeners
        for (NotificationEmitter emitter : registeredEmitters) {
            try {
                emitter.removeNotificationListener(this);
            } catch (Exception e) {
                // Ignore
            }
        }
        registeredEmitters.clear();
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        if (!monitoring) {
            return;
        }

        String notifType = notification.getType();
        if (!notifType.equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
            return;
        }

        try {
            CompositeData cd = (CompositeData) notification.getUserData();
            GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from(cd);
            GcInfo gcInfo = info.getGcInfo();

            String gcName = info.getGcName();
            String gcAction = info.getGcAction();
            String gcCause = info.getGcCause();
            long duration = gcInfo.getDuration();
            long timestamp = System.currentTimeMillis();

            // Get memory usage before and after
            Map<String, MemoryUsage> memBefore = gcInfo.getMemoryUsageBeforeGc();
            Map<String, MemoryUsage> memAfter = gcInfo.getMemoryUsageAfterGc();

            long beforeGC = getTotalUsed(memBefore);
            long afterGC = getTotalUsed(memAfter);
            long maxMemory = getTotalMax(memAfter);

            // Create event
            GCPauseEvent event = new GCPauseEvent(
                timestamp, gcName, gcAction, gcCause,
                duration, beforeGC, afterGC, maxMemory
            );

            // Add to history
            pauseEvents.add(event);
            if (pauseEvents.size() > maxEvents) {
                pauseEvents.remove(0);
            }

            // Update statistics
            totalPauses++;
            totalPauseTime += duration;
            if (duration > longestPause) {
                longestPause = duration;
                longestPauseEvent = event;
            }

        } catch (Exception e) {
            System.err.println("Error processing GC notification: " + e.getMessage());
        }
    }

    private long getTotalUsed(Map<String, MemoryUsage> memUsage) {
        long total = 0;
        for (MemoryUsage usage : memUsage.values()) {
            total += usage.getUsed();
        }
        return total;
    }

    private long getTotalMax(Map<String, MemoryUsage> memUsage) {
        long total = 0;
        for (MemoryUsage usage : memUsage.values()) {
            long max = usage.getMax();
            if (max > 0) {
                total += max;
            } else {
                total += usage.getCommitted();
            }
        }
        return total;
    }

    /**
     * Get recent pause events
     */
    public List<GCPauseEvent> getRecentEvents(int count) {
        int size = pauseEvents.size();
        if (size <= count) {
            return new ArrayList<>(pauseEvents);
        }
        return new ArrayList<>(pauseEvents.subList(size - count, size));
    }

    /**
     * Get all pause events
     */
    public List<GCPauseEvent> getAllEvents() {
        return new ArrayList<>(pauseEvents);
    }

    /**
     * Get statistics
     */
    public GCStatistics getStatistics() {
        if (pauseEvents.isEmpty()) {
            return new GCStatistics(0, 0, 0, 0, 0, 0, null);
        }

        long avgPause = totalPauses > 0 ? totalPauseTime / totalPauses : 0;

        // Calculate percentiles
        List<Long> durations = new ArrayList<>();
        for (GCPauseEvent event : pauseEvents) {
            durations.add(event.getDuration());
        }
        Collections.sort(durations);

        long p50 = getPercentile(durations, 0.50);
        long p95 = getPercentile(durations, 0.95);
        long p99 = getPercentile(durations, 0.99);

        return new GCStatistics(
            totalPauses,
            totalPauseTime,
            avgPause,
            longestPause,
            p95,
            p99,
            longestPauseEvent
        );
    }

    private long getPercentile(List<Long> sorted, double percentile) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    /**
     * Get pause events by type
     */
    public Map<GCPauseEvent.GCType, List<GCPauseEvent>> getEventsByType() {
        Map<GCPauseEvent.GCType, List<GCPauseEvent>> byType = new HashMap<>();
        for (GCPauseEvent.GCType type : GCPauseEvent.GCType.values()) {
            byType.put(type, new ArrayList<>());
        }

        for (GCPauseEvent event : pauseEvents) {
            byType.get(event.getGcType()).add(event);
        }

        return byType;
    }

    /**
     * Clear all events and statistics
     */
    public void clear() {
        pauseEvents.clear();
        totalPauses = 0;
        totalPauseTime = 0;
        longestPause = 0;
        longestPauseEvent = null;
    }

    public boolean isMonitoring() {
        return monitoring;
    }

    /**
     * GC Statistics summary
     */
    public static class GCStatistics {
        public final long totalPauses;
        public final long totalPauseTime;
        public final long avgPause;
        public final long longestPause;
        public final long p95Pause;
        public final long p99Pause;
        public final GCPauseEvent longestPauseEvent;

        public GCStatistics(long totalPauses, long totalPauseTime, long avgPause,
                           long longestPause, long p95Pause, long p99Pause,
                           GCPauseEvent longestPauseEvent) {
            this.totalPauses = totalPauses;
            this.totalPauseTime = totalPauseTime;
            this.avgPause = avgPause;
            this.longestPause = longestPause;
            this.p95Pause = p95Pause;
            this.p99Pause = p99Pause;
            this.longestPauseEvent = longestPauseEvent;
        }
    }
}

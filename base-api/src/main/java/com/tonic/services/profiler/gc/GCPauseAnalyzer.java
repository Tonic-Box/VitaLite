package com.tonic.services.profiler.gc;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Listens for GC notifications and keeps the most recent pauses, with heap use before and after each
 */
public class GCPauseAnalyzer implements NotificationListener {
    private final int capacity;
    private final ArrayDeque<GCPauseEvent> events = new ArrayDeque<>();
    private final List<NotificationEmitter> emitters = new ArrayList<>();
    private final Set<String> heapPools = new HashSet<>();
    private final long vmStart = ManagementFactory.getRuntimeMXBean().getStartTime();

    /**
     * Creates the analyzer
     * @param capacity the number of most recent pauses kept
     */
    public GCPauseAnalyzer(int capacity) {
        this.capacity = capacity;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP) {
                heapPools.add(pool.getName());
            }
        }
    }

    /**
     * Starts listening to every garbage collector
     */
    public synchronized void startMonitoring() {
        if (!emitters.isEmpty()) {
            return;
        }
        for (GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (collector instanceof NotificationEmitter) {
                NotificationEmitter emitter = (NotificationEmitter) collector;
                emitter.addNotificationListener(this, null, null);
                emitters.add(emitter);
            }
        }
    }

    /**
     * Stops listening, safe to call more than once
     */
    public synchronized void stopMonitoring() {
        for (NotificationEmitter emitter : emitters) {
            try {
                emitter.removeNotificationListener(this);
            } catch (Exception ignored) {
            }
        }
        emitters.clear();
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        if (!GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType())) {
            return;
        }
        GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
        GcInfo gc = info.getGcInfo();
        Map<String, MemoryUsage> before = gc.getMemoryUsageBeforeGc();
        Map<String, MemoryUsage> after = gc.getMemoryUsageAfterGc();
        add(new GCPauseEvent(vmStart + gc.getStartTime(), info.getGcName(), info.getGcAction(), info.getGcCause(), gc.getDuration(), heapUsed(before), heapUsed(after), heapMax(after)));
    }

    /**
     * Records a pause, dropping the oldest when full
     * @param event the pause
     */
    public synchronized void add(GCPauseEvent event) {
        events.addLast(event);
        while (events.size() > capacity) {
            events.removeFirst();
        }
    }

    /**
     * Gets the kept pauses
     * @return the pauses, oldest first
     */
    public synchronized List<GCPauseEvent> getEvents() {
        return new ArrayList<>(events);
    }

    /**
     * Summarises the kept pauses
     * @return the statistics over every kept pause
     */
    public GCStatistics getStatistics() {
        List<GCPauseEvent> kept = getEvents();
        if (kept.isEmpty()) {
            return new GCStatistics(0, 0, 0, 0, 0, null);
        }
        long[] durations = new long[kept.size()];
        long total = 0;
        GCPauseEvent longest = kept.get(0);
        for (int index = 0; index < durations.length; index++) {
            GCPauseEvent event = kept.get(index);
            durations[index] = event.getDuration();
            total += event.getDuration();
            if (event.getDuration() > longest.getDuration()) {
                longest = event;
            }
        }
        Arrays.sort(durations);
        return new GCStatistics(kept.size(), total, total / kept.size(), percentile(durations, 0.95), percentile(durations, 0.99), longest);
    }

    private long heapUsed(Map<String, MemoryUsage> pools) {
        long used = 0;
        for (Map.Entry<String, MemoryUsage> pool : pools.entrySet()) {
            if (heapPools.contains(pool.getKey())) {
                used += pool.getValue().getUsed();
            }
        }
        return used;
    }

    private long heapMax(Map<String, MemoryUsage> pools) {
        long max = 0;
        for (Map.Entry<String, MemoryUsage> pool : pools.entrySet()) {
            if (heapPools.contains(pool.getKey())) {
                MemoryUsage usage = pool.getValue();
                max += usage.getMax() > 0 ? usage.getMax() : usage.getCommitted();
            }
        }
        return max;
    }

    private static long percentile(long[] sorted, double fraction) {
        int index = (int) Math.ceil(fraction * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
    }

    /**
     * Statistics over the kept pauses
     */
    public static class GCStatistics {
        public final int pauses;
        public final long totalPauseTime;
        public final long averagePause;
        public final long p95Pause;
        public final long p99Pause;
        public final GCPauseEvent longest;

        /**
         * Creates the statistics
         * @param pauses the number of pauses
         * @param totalPauseTime their total duration in milliseconds
         * @param averagePause their mean duration in milliseconds
         * @param p95Pause the 95th percentile duration in milliseconds
         * @param p99Pause the 99th percentile duration in milliseconds
         * @param longest the longest pause, or null with none
         */
        public GCStatistics(int pauses, long totalPauseTime, long averagePause, long p95Pause, long p99Pause, GCPauseEvent longest) {
            this.pauses = pauses;
            this.totalPauseTime = totalPauseTime;
            this.averagePause = averagePause;
            this.p95Pause = p95Pause;
            this.p99Pause = p99Pause;
            this.longest = longest;
        }
    }
}

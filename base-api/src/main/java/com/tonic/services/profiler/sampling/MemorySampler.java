package com.tonic.services.profiler.sampling;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Memory profiler using heap delta analysis and GC event tracking
 * Estimates allocation rates by observing heap growth and garbage collection
 */
public class MemorySampler {
    private final MemoryMXBean memoryBean;
    private final List<MemoryPoolMXBean> memoryPools;
    private final Map<String, GarbageCollectorMXBean> gcBeans;

    private final RingBuffer<HeapDeltaSample> heapSamples;
    private final RingBuffer<GCEventSample> gcSamples;
    private final HeapHistogramSampler histogramSampler;

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong totalSamples = new AtomicLong(0);

    // Heap histogram state
    private List<HeapHistogramSample> latestHistogram;
    private boolean captureHistogram = false;

    // Previous state for delta calculation
    private long lastTimestamp = 0;
    private long lastHeapUsed = 0;
    private Map<String, Long> lastPoolUsed = new HashMap<>();
    private Map<String, Long> lastGCCounts = new HashMap<>();
    private Map<String, Long> lastGCTimes = new HashMap<>();

    // Configuration
    private int samplingIntervalMs = 1000;

    public MemorySampler(int maxSamples) {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.memoryPools = ManagementFactory.getMemoryPoolMXBeans();
        this.gcBeans = new HashMap<>();

        // Find GC beans
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcBeans.put(gc.getName(), gc);
        }

        this.heapSamples = new RingBuffer<>(maxSamples);
        this.gcSamples = new RingBuffer<>(maxSamples);
        this.histogramSampler = new HeapHistogramSampler();
    }

    /**
     * Start sampling
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            // Initialize baseline
            initializeBaseline();

            scheduler = Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "Memory-Sampler");
                t.setDaemon(true);
                return t;
            });

            scheduler.scheduleAtFixedRate(
                this::captureSample,
                samplingIntervalMs,
                samplingIntervalMs,
                TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * Stop sampling
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (scheduler != null) {
                scheduler.shutdown();
                try {
                    scheduler.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                scheduler = null;
            }
        }
    }

    /**
     * Initialize baseline for delta calculations
     */
    private void initializeBaseline() {
        lastTimestamp = System.currentTimeMillis();

        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        lastHeapUsed = heapUsage.getUsed();

        lastPoolUsed.clear();
        for (MemoryPoolMXBean pool : memoryPools) {
            lastPoolUsed.put(pool.getName(), pool.getUsage().getUsed());
        }

        lastGCCounts.clear();
        lastGCTimes.clear();
        for (Map.Entry<String, GarbageCollectorMXBean> entry : gcBeans.entrySet()) {
            GarbageCollectorMXBean gc = entry.getValue();
            lastGCCounts.put(entry.getKey(), gc.getCollectionCount());
            lastGCTimes.put(entry.getKey(), gc.getCollectionTime());
        }
    }

    /**
     * Capture a single sample
     */
    private void captureSample() {
        try {
            long timestamp = System.currentTimeMillis();
            long timeDelta = timestamp - lastTimestamp;

            // Capture heap state
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            long heapUsed = heapUsage.getUsed();
            long heapCommitted = heapUsage.getCommitted();
            long heapMax = heapUsage.getMax();

            // Calculate heap delta
            long heapDelta = heapUsed - lastHeapUsed;

            // Capture pool deltas
            Map<String, HeapDeltaSample.PoolDelta> poolDeltas = new HashMap<>();
            for (MemoryPoolMXBean pool : memoryPools) {
                String poolName = pool.getName();
                MemoryUsage usage = pool.getUsage();
                long currentUsed = usage.getUsed();
                long previousUsed = lastPoolUsed.getOrDefault(poolName, 0L);

                poolDeltas.put(poolName, new HeapDeltaSample.PoolDelta(
                    poolName,
                    previousUsed,
                    currentUsed,
                    usage.getMax()
                ));

                lastPoolUsed.put(poolName, currentUsed);
            }

            // Check for GC events
            boolean gcOccurred = detectGCEvents(timestamp);

            // Calculate allocation rate (MB/s)
            double allocationRate = 0;
            if (timeDelta > 0) {
                // If GC occurred, heap delta may be negative
                // Estimate allocation as: delta + (amount GC'd)
                // For simplicity, use absolute delta if GC occurred
                long netChange = gcOccurred ? Math.abs(heapDelta) : Math.max(0, heapDelta);
                allocationRate = (netChange / (1024.0 * 1024.0)) / (timeDelta / 1000.0);
            }

            // Create heap sample
            HeapDeltaSample heapSample = new HeapDeltaSample(
                timestamp,
                heapUsed,
                heapCommitted,
                heapMax,
                poolDeltas,
                allocationRate,
                gcOccurred
            );

            heapSamples.add(heapSample);
            totalSamples.incrementAndGet();

            // Update for next iteration
            lastTimestamp = timestamp;
            lastHeapUsed = heapUsed;

        } catch (Exception e) {
            System.err.println("Memory sampling error: " + e.getMessage());
        }
    }

    /**
     * Detect and record GC events
     */
    private boolean detectGCEvents(long timestamp) {
        boolean gcOccurred = false;

        long youngGCCount = 0;
        long fullGCCount = 0;
        long youngGCTime = 0;
        long fullGCTime = 0;
        long youngGCDelta = 0;
        long fullGCDelta = 0;
        long youngGCTimeDelta = 0;
        long fullGCTimeDelta = 0;

        for (Map.Entry<String, GarbageCollectorMXBean> entry : gcBeans.entrySet()) {
            String name = entry.getKey();
            GarbageCollectorMXBean gc = entry.getValue();

            long currentCount = gc.getCollectionCount();
            long currentTime = gc.getCollectionTime();
            long lastCount = lastGCCounts.getOrDefault(name, 0L);
            long lastTime = lastGCTimes.getOrDefault(name, 0L);

            long countDelta = currentCount - lastCount;
            long timeDelta = currentTime - lastTime;

            if (countDelta > 0) {
                gcOccurred = true;

                // Classify as young or full GC (heuristic based on name)
                if (isYoungGC(name)) {
                    youngGCCount = currentCount;
                    youngGCTime = currentTime;
                    youngGCDelta += countDelta;
                    youngGCTimeDelta += timeDelta;
                } else {
                    fullGCCount = currentCount;
                    fullGCTime = currentTime;
                    fullGCDelta += countDelta;
                    fullGCTimeDelta += timeDelta;
                }
            }

            lastGCCounts.put(name, currentCount);
            lastGCTimes.put(name, currentTime);
        }

        if (gcOccurred) {
            // Calculate GC overhead
            long wallClockTime = timestamp - lastTimestamp;
            double gcOverhead = wallClockTime > 0 ?
                ((youngGCTimeDelta + fullGCTimeDelta) * 100.0) / wallClockTime : 0;

            // Calculate GC frequency
            double gcFrequency = wallClockTime > 0 ?
                (youngGCDelta * 1000.0) / wallClockTime : 0;

            GCEventSample gcSample = new GCEventSample(
                timestamp,
                youngGCCount,
                fullGCCount,
                youngGCTime,
                fullGCTime,
                youngGCDelta,
                fullGCDelta,
                youngGCTimeDelta,
                fullGCTimeDelta,
                gcOverhead,
                gcFrequency
            );

            gcSamples.add(gcSample);
        }

        return gcOccurred;
    }

    /**
     * Heuristic to determine if GC is young generation
     */
    private boolean isYoungGC(String gcName) {
        String lower = gcName.toLowerCase();
        return lower.contains("young") ||
               lower.contains("scavenge") ||
               lower.contains("parnew") ||
               lower.contains("copy");
    }

    // ==================== Configuration Methods ====================

    public void setSamplingInterval(int intervalMs) {
        this.samplingIntervalMs = Math.max(100, intervalMs);
    }

    // ==================== Data Access Methods ====================

    public RingBuffer<HeapDeltaSample> getHeapSamples() {
        return heapSamples;
    }

    public RingBuffer<GCEventSample> getGCSamples() {
        return gcSamples;
    }

    public boolean isRunning() {
        return running.get();
    }

    public long getTotalSamples() {
        return totalSamples.get();
    }

    public int getSamplingIntervalMs() {
        return samplingIntervalMs;
    }

    public SamplingStats getStats() {
        return new SamplingStats(
            isRunning(),
            totalSamples.get(),
            heapSamples.size(),
            heapSamples.capacity(),
            gcSamples.size(),
            samplingIntervalMs
        );
    }

    // ==================== Heap Histogram Methods ====================

    /**
     * Capture a heap histogram snapshot
     * This can be expensive, so it's done on-demand
     */
    public List<HeapHistogramSample> captureHeapHistogram() {
        latestHistogram = histogramSampler.captureHistogram();
        return latestHistogram;
    }

    /**
     * Get the latest heap histogram
     */
    public List<HeapHistogramSample> getLatestHistogram() {
        return latestHistogram;
    }

    /**
     * Enable/disable automatic histogram capture on stop
     */
    public void setCaptureHistogramOnStop(boolean capture) {
        this.captureHistogram = capture;
    }

    /**
     * Clear all samples
     */
    public void clear() {
        heapSamples.clear();
        gcSamples.clear();
        totalSamples.set(0);
        lastPoolUsed.clear();
        lastGCCounts.clear();
        lastGCTimes.clear();
        latestHistogram = null;
        histogramSampler.clear();
    }

    /**
     * Sampling statistics
     */
    public static class SamplingStats {
        public final boolean running;
        public final long totalSamples;
        public final int currentHeapSamples;
        public final int maxSamples;
        public final int gcEventCount;
        public final int intervalMs;

        public SamplingStats(boolean running, long totalSamples, int currentHeapSamples,
                           int maxSamples, int gcEventCount, int intervalMs) {
            this.running = running;
            this.totalSamples = totalSamples;
            this.currentHeapSamples = currentHeapSamples;
            this.maxSamples = maxSamples;
            this.gcEventCount = gcEventCount;
            this.intervalMs = intervalMs;
        }

        @Override
        public String toString() {
            return String.format("MemorySampler[running=%s, samples=%d/%d, gcEvents=%d, interval=%dms]",
                running, currentHeapSamples, maxSamples, gcEventCount, intervalMs);
        }
    }
}

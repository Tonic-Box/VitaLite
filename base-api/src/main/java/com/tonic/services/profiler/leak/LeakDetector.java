package com.tonic.services.profiler.leak;

import com.tonic.services.profiler.sampling.HeapHistogramSample;
import com.tonic.services.profiler.sampling.HeapHistogramSampler;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Detects memory leaks by analyzing heap snapshots over time
 */
public class LeakDetector {
    private final HeapHistogramSampler histogramSampler;
    private final ClassLoaderAnalyzer classLoaderAnalyzer;
    private final Map<String, ClassGrowthTracker> growthTrackers;
    private final List<HeapSnapshot> snapshots;
    private final ScheduledExecutorService scheduler;

    private HeapSnapshot baselineSnapshot;
    private volatile boolean monitoring = false;
    private int samplingIntervalSeconds = 30;
    private int maxSnapshots = 100;

    public LeakDetector(HeapHistogramSampler histogramSampler) {
        this.histogramSampler = histogramSampler;
        this.classLoaderAnalyzer = new ClassLoaderAnalyzer();
        this.growthTrackers = new ConcurrentHashMap<>();
        this.snapshots = new CopyOnWriteArrayList<>();
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "Leak-Detector");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Start monitoring for leaks
     */
    public void startMonitoring() {
        if (monitoring) {
            return;
        }

        monitoring = true;

        // Capture baseline immediately
        captureBaseline();

        // Schedule periodic snapshots
        scheduler.scheduleAtFixedRate(
            this::captureSnapshot,
            samplingIntervalSeconds,
            samplingIntervalSeconds,
            TimeUnit.SECONDS
        );
    }

    /**
     * Stop monitoring
     */
    public void stopMonitoring() {
        monitoring = false;
        // Don't shutdown scheduler - just stop scheduling new tasks
    }

    /**
     * Capture baseline snapshot
     */
    public void captureBaseline() {
        // Force GC before baseline
        System.gc();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<HeapHistogramSample> samples = histogramSampler.captureHistogram();
        baselineSnapshot = new HeapSnapshot(samples, System.currentTimeMillis(), true);
        snapshots.clear();
        growthTrackers.clear();

        // Initialize trackers from baseline
        for (HeapHistogramSample sample : samples) {
            growthTrackers.put(sample.className, new ClassGrowthTracker(sample.className));
        }
    }

    /**
     * Capture a snapshot
     */
    public void captureSnapshot() {
        List<HeapHistogramSample> samples = histogramSampler.captureHistogram();
        HeapSnapshot snapshot = new HeapSnapshot(samples, System.currentTimeMillis(), false);

        // Add to snapshots list (with size limit)
        snapshots.add(snapshot);
        if (snapshots.size() > maxSnapshots) {
            snapshots.remove(0);
        }

        // Update growth trackers
        for (HeapHistogramSample sample : samples) {
            ClassGrowthTracker tracker = growthTrackers.computeIfAbsent(
                sample.className,
                ClassGrowthTracker::new
            );
            tracker.addDataPoint(sample.instanceCount, sample.totalBytes, snapshot.timestamp);
        }
    }

    /**
     * Force GC and capture snapshot
     */
    public void captureSnapshotAfterGC() {
        System.gc();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        captureSnapshot();
    }

    /**
     * Analyze for leaks
     */
    public List<LeakSuspicion> analyzeSuspiciousClasses() {
        if (baselineSnapshot == null || snapshots.isEmpty()) {
            return Collections.emptyList();
        }

        HeapSnapshot latestSnapshot = snapshots.get(snapshots.size() - 1);
        List<LeakSuspicion> suspicions = new ArrayList<>();

        // Analyze each class that has growth
        for (Map.Entry<String, ClassGrowthTracker> entry : growthTrackers.entrySet()) {
            String className = entry.getKey();
            ClassGrowthTracker tracker = entry.getValue();

            // Get baseline and current values
            HeapHistogramSample baseline = baselineSnapshot.getSample(className);
            HeapHistogramSample current = latestSnapshot.getSample(className);

            if (baseline == null || current == null) {
                continue;
            }

            // Calculate growth rate (instances per minute)
            double growthRate = tracker.calculateGrowthRate();

            // Only flag if there's actual growth
            if (growthRate > 0 && current.instanceCount > baseline.instanceCount) {
                LeakSuspicion suspicion = new LeakSuspicion(
                    className,
                    current.instanceCount,
                    baseline.instanceCount,
                    current.totalBytes,
                    baseline.totalBytes,
                    growthRate,
                    tracker.getDataPointCount(),
                    tracker.getFirstTimestamp(),
                    tracker.getLastTimestamp()
                );

                suspicions.add(suspicion);
            }
        }

        // Sort by confidence and growth rate
        Collections.sort(suspicions);

        return suspicions;
    }

    /**
     * Get top N suspicious classes
     */
    public List<LeakSuspicion> getTopSuspiciousClasses(int limit) {
        List<LeakSuspicion> all = analyzeSuspiciousClasses();
        return all.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Get statistics for a specific class
     */
    public ClassGrowthTracker getTrackerForClass(String className) {
        return growthTrackers.get(className);
    }

    /**
     * Get memory statistics
     */
    public MemoryStats getMemoryStats() {
        if (baselineSnapshot == null) {
            return new MemoryStats(0, 0, 0, 0, 0, 0, 0);
        }

        HeapSnapshot latest = snapshots.isEmpty() ? baselineSnapshot : snapshots.get(snapshots.size() - 1);

        long baselineTotal = baselineSnapshot.getTotalBytes();
        long currentTotal = latest.getTotalBytes();
        long baselineInstances = baselineSnapshot.getTotalInstances();
        long currentInstances = latest.getTotalInstances();

        long heapUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        long heapMax = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();

        return new MemoryStats(
            baselineTotal,
            currentTotal,
            baselineInstances,
            currentInstances,
            snapshots.size(),
            heapUsed,
            heapMax
        );
    }

    public boolean isMonitoring() {
        return monitoring;
    }

    public HeapSnapshot getBaselineSnapshot() {
        return baselineSnapshot;
    }

    public List<HeapSnapshot> getSnapshots() {
        return new ArrayList<>(snapshots);
    }

    public void setSamplingInterval(int seconds) {
        this.samplingIntervalSeconds = Math.max(5, seconds);
    }

    public int getSamplingInterval() {
        return samplingIntervalSeconds;
    }

    /**
     * Get ClassLoader analysis
     */
    public ClassLoaderAnalyzer.ClassLoaderAnalysis getClassLoaderAnalysis() {
        return classLoaderAnalyzer.analyze();
    }

    /**
     * Heap snapshot at a point in time
     */
    public static class HeapSnapshot {
        private final Map<String, HeapHistogramSample> sampleMap;
        private final long timestamp;
        private final boolean isBaseline;

        public HeapSnapshot(List<HeapHistogramSample> samples, long timestamp, boolean isBaseline) {
            this.sampleMap = new HashMap<>();
            this.timestamp = timestamp;
            this.isBaseline = isBaseline;

            for (HeapHistogramSample sample : samples) {
                sampleMap.put(sample.className, sample);
            }
        }

        public HeapHistogramSample getSample(String className) {
            return sampleMap.get(className);
        }

        public long getTotalBytes() {
            return sampleMap.values().stream()
                .mapToLong(s -> s.totalBytes)
                .sum();
        }

        public long getTotalInstances() {
            return sampleMap.values().stream()
                .mapToLong(s -> s.instanceCount)
                .sum();
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isBaseline() {
            return isBaseline;
        }
    }

    /**
     * Tracks growth for a specific class over time
     */
    public static class ClassGrowthTracker {
        private final String className;
        private final List<DataPoint> dataPoints;
        private long firstTimestamp;
        private long lastTimestamp;

        public ClassGrowthTracker(String className) {
            this.className = className;
            this.dataPoints = new ArrayList<>();
        }

        public void addDataPoint(long instances, long bytes, long timestamp) {
            if (dataPoints.isEmpty()) {
                firstTimestamp = timestamp;
            }
            lastTimestamp = timestamp;
            dataPoints.add(new DataPoint(instances, bytes, timestamp));
        }

        public double calculateGrowthRate() {
            if (dataPoints.size() < 2) {
                return 0;
            }

            DataPoint first = dataPoints.get(0);
            DataPoint last = dataPoints.get(dataPoints.size() - 1);

            long timeDiffMs = last.timestamp - first.timestamp;
            if (timeDiffMs <= 0) {
                return 0;
            }

            long instanceDiff = last.instances - first.instances;
            double minutes = timeDiffMs / 60000.0;

            return minutes > 0 ? instanceDiff / minutes : 0;
        }

        public int getDataPointCount() {
            return dataPoints.size();
        }

        public long getFirstTimestamp() {
            return firstTimestamp;
        }

        public long getLastTimestamp() {
            return lastTimestamp;
        }

        public List<DataPoint> getDataPoints() {
            return new ArrayList<>(dataPoints);
        }

        public static class DataPoint {
            public final long instances;
            public final long bytes;
            public final long timestamp;

            public DataPoint(long instances, long bytes, long timestamp) {
                this.instances = instances;
                this.bytes = bytes;
                this.timestamp = timestamp;
            }
        }
    }

    /**
     * Memory statistics
     */
    public static class MemoryStats {
        public final long baselineBytes;
        public final long currentBytes;
        public final long baselineInstances;
        public final long currentInstances;
        public final int snapshotCount;
        public final long heapUsed;
        public final long heapMax;

        public MemoryStats(long baselineBytes, long currentBytes, long baselineInstances,
                          long currentInstances, int snapshotCount, long heapUsed, long heapMax) {
            this.baselineBytes = baselineBytes;
            this.currentBytes = currentBytes;
            this.baselineInstances = baselineInstances;
            this.currentInstances = currentInstances;
            this.snapshotCount = snapshotCount;
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
        }

        public long getBytesDelta() {
            return currentBytes - baselineBytes;
        }

        public long getInstancesDelta() {
            return currentInstances - baselineInstances;
        }

        public double getHeapUsagePercent() {
            return heapMax > 0 ? (heapUsed * 100.0 / heapMax) : 0;
        }
    }
}

package com.tonic.services.profiler.sampling;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes collected samples to produce profiling results
 * Builds method hotspots, call trees, and memory analysis
 */
public class SampleAnalyzer {

    /**
     * Analyze CPU samples to produce comprehensive results
     */
    public CPUAnalysisResults analyzeCPU(RingBuffer<StackSample> samples) {
        List<StackSample> allSamples = samples.getAll();
        if (allSamples.isEmpty()) {
            return new CPUAnalysisResults();
        }

        CPUAnalysisResults results = new CPUAnalysisResults();
        results.totalSamples = allSamples.size();
        results.timeRange = calculateTimeRange(allSamples);
        results.methodHotspots = buildMethodHotspots(allSamples);
        results.threadBreakdown = buildThreadBreakdown(allSamples);
        results.packageAggregation = aggregateByPackage(results.methodHotspots);

        return results;
    }

    /**
     * Analyze memory samples to produce results
     */
    public MemoryAnalysisResults analyzeMemory(
        RingBuffer<HeapDeltaSample> heapSamples,
        RingBuffer<GCEventSample> gcSamples
    ) {
        MemoryAnalysisResults results = new MemoryAnalysisResults();

        List<HeapDeltaSample> allHeapSamples = heapSamples.getAll();
        if (!allHeapSamples.isEmpty()) {
            results.averageAllocationRate = calculateAverageAllocationRate(allHeapSamples);
            results.peakAllocationRate = calculatePeakAllocationRate(allHeapSamples);
            results.totalEstimatedAllocation = calculateTotalAllocation(allHeapSamples);
            results.heapGrowthTrend = calculateHeapTrend(allHeapSamples);
        }

        List<GCEventSample> allGCSamples = gcSamples.getAll();
        if (!allGCSamples.isEmpty()) {
            results.totalGCEvents = countTotalGCEvents(allGCSamples);
            results.averageGCOverhead = calculateAverageGCOverhead(allGCSamples);
            results.fullGCCount = countFullGCs(allGCSamples);
        }

        return results;
    }

    // ==================== CPU Analysis Methods ====================

    private TimeRange calculateTimeRange(List<StackSample> samples) {
        if (samples.isEmpty()) return new TimeRange(0, 0);

        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        for (StackSample sample : samples) {
            min = Math.min(min, sample.timestamp);
            max = Math.max(max, sample.timestamp);
        }

        return new TimeRange(min, max);
    }

    private Map<MethodSignature, MethodStats> buildMethodHotspots(List<StackSample> samples) {
        Map<MethodSignature, MethodStats> hotspots = new HashMap<>();

        for (StackSample sample : samples) {
            for (int i = 0; i < sample.stackTrace.length; i++) {
                StackTraceElement frame = sample.stackTrace[i];
                MethodSignature sig = MethodSignature.from(frame);

                MethodStats stats = hotspots.computeIfAbsent(sig, k -> new MethodStats(sig));
                stats.totalSamples++;

                if (i == 0) {
                    stats.selfSamples++;
                }

                // Track per-thread
                stats.threadSamples.merge(sample.threadId, 1, Integer::sum);
            }
        }

        // Calculate percentages
        for (MethodStats stats : hotspots.values()) {
            stats.selfTimePercent = (stats.selfSamples * 100.0) / samples.size();
            stats.totalTimePercent = (stats.totalSamples * 100.0) / samples.size();
        }

        return hotspots;
    }

    private Map<Long, ThreadStats> buildThreadBreakdown(List<StackSample> samples) {
        Map<Long, ThreadStats> threads = new HashMap<>();

        for (StackSample sample : samples) {
            ThreadStats stats = threads.computeIfAbsent(sample.threadId,
                k -> new ThreadStats(sample.threadId, sample.threadName));

            stats.sampleCount++;
            stats.states.merge(sample.threadState, 1, Integer::sum);

            if (sample.threadCpuTime > 0) {
                stats.totalCpuTime = Math.max(stats.totalCpuTime, sample.threadCpuTime);
            }
        }

        return threads;
    }

    private Map<String, PackageStats> aggregateByPackage(Map<MethodSignature, MethodStats> methods) {
        Map<String, PackageStats> packages = new HashMap<>();

        for (Map.Entry<MethodSignature, MethodStats> entry : methods.entrySet()) {
            String packageName = entry.getKey().getPackageName();
            if (packageName.isEmpty()) packageName = "<default>";

            PackageStats stats = packages.computeIfAbsent(packageName, PackageStats::new);
            stats.totalSamples += entry.getValue().totalSamples;
            stats.selfSamples += entry.getValue().selfSamples;
            stats.methodCount++;
        }

        return packages;
    }

    // ==================== Memory Analysis Methods ====================

    private double calculateAverageAllocationRate(List<HeapDeltaSample> samples) {
        if (samples.isEmpty()) return 0;

        double sum = 0;
        for (HeapDeltaSample sample : samples) {
            sum += sample.allocationRateMBPerSec;
        }

        return sum / samples.size();
    }

    private double calculatePeakAllocationRate(List<HeapDeltaSample> samples) {
        return samples.stream()
            .mapToDouble(s -> s.allocationRateMBPerSec)
            .max()
            .orElse(0);
    }

    private double calculateTotalAllocation(List<HeapDeltaSample> samples) {
        if (samples.size() < 2) return 0;

        HeapDeltaSample first = samples.get(0);
        HeapDeltaSample last = samples.get(samples.size() - 1);

        long timeDelta = last.timestamp - first.timestamp;
        if (timeDelta <= 0) return 0;

        double avgRate = calculateAverageAllocationRate(samples);
        return (avgRate * timeDelta) / 1000.0; // MB
    }

    private String calculateHeapTrend(List<HeapDeltaSample> samples) {
        if (samples.size() < 2) return "STABLE";

        long firstUsed = samples.get(0).heapUsed;
        long lastUsed = samples.get(samples.size() - 1).heapUsed;

        long delta = lastUsed - firstUsed;
        double percentChange = (delta * 100.0) / firstUsed;

        if (percentChange > 10) return "GROWING";
        if (percentChange < -10) return "SHRINKING";
        return "STABLE";
    }

    private int countTotalGCEvents(List<GCEventSample> samples) {
        return (int) samples.stream()
            .filter(GCEventSample::hadGC)
            .count();
    }

    private double calculateAverageGCOverhead(List<GCEventSample> samples) {
        if (samples.isEmpty()) return 0;

        double sum = samples.stream()
            .mapToDouble(s -> s.gcOverheadPercent)
            .sum();

        return sum / samples.size();
    }

    private int countFullGCs(List<GCEventSample> samples) {
        return (int) samples.stream()
            .filter(GCEventSample::hadFullGC)
            .count();
    }

    // ==================== Result Classes ====================

    public static class CPUAnalysisResults {
        public int totalSamples;
        public TimeRange timeRange;
        public Map<MethodSignature, MethodStats> methodHotspots = new HashMap<>();
        public Map<Long, ThreadStats> threadBreakdown = new HashMap<>();
        public Map<String, PackageStats> packageAggregation = new HashMap<>();

        public List<MethodStats> getTopMethods(int count) {
            return methodHotspots.values().stream()
                .sorted((a, b) -> Double.compare(b.selfTimePercent, a.selfTimePercent))
                .limit(count)
                .collect(Collectors.toList());
        }

        public List<PackageStats> getTopPackages(int count) {
            return packageAggregation.values().stream()
                .sorted((a, b) -> Integer.compare(b.totalSamples, a.totalSamples))
                .limit(count)
                .collect(Collectors.toList());
        }
    }

    public static class MemoryAnalysisResults {
        public double averageAllocationRate;
        public double peakAllocationRate;
        public double totalEstimatedAllocation;
        public String heapGrowthTrend;
        public int totalGCEvents;
        public double averageGCOverhead;
        public int fullGCCount;
    }

    public static class TimeRange {
        public final long startTime;
        public final long endTime;

        public TimeRange(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public long getDuration() {
            return endTime - startTime;
        }
    }

    public static class MethodStats {
        public final MethodSignature method;
        public int totalSamples;
        public int selfSamples;
        public double selfTimePercent;
        public double totalTimePercent;
        public Map<Long, Integer> threadSamples = new HashMap<>();

        public MethodStats(MethodSignature method) {
            this.method = method;
        }
    }

    public static class ThreadStats {
        public final long threadId;
        public final String threadName;
        public int sampleCount;
        public long totalCpuTime;
        public Map<Thread.State, Integer> states = new HashMap<>();

        public ThreadStats(long threadId, String threadName) {
            this.threadId = threadId;
            this.threadName = threadName;
        }
    }

    public static class PackageStats {
        public final String packageName;
        public int totalSamples;
        public int selfSamples;
        public int methodCount;

        public PackageStats(String packageName) {
            this.packageName = packageName;
        }
    }
}

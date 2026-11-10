package com.tonic.services.profiler.recording;

import com.tonic.services.profiler.visualization.FlameGraphNode;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The results of one JFR recording: Java execution samples per method, the flame graph and call trees built from
 * them, the samples of threads blocked in native code, and allocations when they were recorded
 */
public class MethodTimingResults {
    public final Map<String, MethodStats> methodStats;
    public final int totalSamples;
    public final int truncatedSamples;
    public final int samplePeriodMs;
    public final long recordingDurationMs;
    public final long recordingStartTime;
    public final long recordingEndTime;
    public final FlameGraphNode flameGraphRoot;
    public final CallTreeNode callTreeRoot;
    public final CallTreeNode callersRoot;
    public final Map<String, CallTreeNode> methodNodes;
    public final Map<String, MethodStats> nativeStats;
    public final int nativeSamples;
    public final Allocations allocations;

    /**
     * Allocation totals, by the allocated class and by the method that allocated
     */
    public static final class Allocations {
        public final long totalBytes;
        public final Map<String, long[]> byClass;
        public final Map<String, long[]> bySite;

        /**
         * Creates the totals
         * @param totalBytes every allocation event's bytes added together
         * @param byClass bytes and event count per allocated class
         * @param bySite bytes and event count per allocating method
         */
        public Allocations(long totalBytes, Map<String, long[]> byClass, Map<String, long[]> bySite) {
            this.totalBytes = totalBytes;
            this.byClass = byClass;
            this.bySite = bySite;
        }

        /**
         * Sorts totals by bytes
         * @param totals bytes and event count per key
         * @return the entries, most bytes first
         */
        public static List<Map.Entry<String, long[]>> sorted(Map<String, long[]> totals) {
            return totals.entrySet().stream().sorted(Comparator.comparingLong((Map.Entry<String, long[]> entry) -> entry.getValue()[0]).reversed()).collect(Collectors.toList());
        }
    }

    /**
     * Creates the results
     * @param methodStats Java execution samples per method
     * @param totalSamples Java execution samples taken
     * @param truncatedSamples samples whose stack was cut short
     * @param samplePeriodMs the sampling period
     * @param recordingStartTime when recording started, in epoch milliseconds
     * @param recordingEndTime when recording stopped, in epoch milliseconds
     * @param flameGraphRoot the flame graph, outermost frames at the root
     * @param callTreeRoot the top-down call tree, outermost frames at the root
     * @param callersRoot the bottom-up tree, running methods at the root and their callers below
     * @param methodNodes each method's callers and callees, from frames adjacent in the real stacks
     * @param nativeStats samples of threads blocked in native code, per method
     * @param nativeSamples native samples taken
     * @param allocations allocation totals, or null when allocations were not recorded
     */
    public MethodTimingResults(Map<String, MethodStats> methodStats, int totalSamples, int truncatedSamples, int samplePeriodMs, long recordingStartTime, long recordingEndTime, FlameGraphNode flameGraphRoot, CallTreeNode callTreeRoot, CallTreeNode callersRoot, Map<String, CallTreeNode> methodNodes, Map<String, MethodStats> nativeStats, int nativeSamples, Allocations allocations) {
        this.methodStats = methodStats;
        this.totalSamples = totalSamples;
        this.truncatedSamples = truncatedSamples;
        this.samplePeriodMs = samplePeriodMs;
        this.recordingStartTime = recordingStartTime;
        this.recordingEndTime = recordingEndTime;
        this.recordingDurationMs = recordingEndTime - recordingStartTime;
        this.flameGraphRoot = flameGraphRoot;
        this.callTreeRoot = callTreeRoot;
        this.callersRoot = callersRoot;
        this.methodNodes = methodNodes;
        this.nativeStats = nativeStats;
        this.nativeSamples = nativeSamples;
        this.allocations = allocations;
    }

    /**
     * Gets a method's callers and callees
     * @param methodKey the class and method name
     * @return the method's node, or null when it was never sampled
     */
    public CallTreeNode getMethodNode(String methodKey) {
        return methodNodes.get(methodKey);
    }

    /**
     * Gets every sampled method by the samples it was running in
     * @return the methods, most self samples first
     */
    public List<MethodStats> getAllMethodsBySelfTime() {
        return sorted(methodStats, Comparator.comparingInt(MethodStats::getSelfSamples));
    }

    /**
     * Gets the methods that ran the most
     * @param limit the most methods to return; zero or less returns none
     * @return up to that many methods, most self samples first
     */
    public List<MethodStats> getTopMethodsBySelfTime(int limit) {
        return getAllMethodsBySelfTime().stream().limit(Math.max(0, limit)).collect(Collectors.toList());
    }

    /**
     * Gets the number of methods sampled
     * @return the method count
     */
    public int getMethodCount() {
        return methodStats.size();
    }

    /**
     * Gets the methods threads were blocked in, by samples
     * @return the methods, most self samples first
     */
    public List<MethodStats> getNativeMethodsBySelfTime() {
        return sorted(nativeStats, Comparator.comparingInt(MethodStats::getSelfSamples));
    }

    /**
     * Checks whether the recording caught any Java execution
     * @return true when there are execution samples
     */
    public boolean hasData() {
        return totalSamples > 0 && !methodStats.isEmpty();
    }

    /**
     * Gets the recording's length
     * @return the duration, such as 12.3s or 2m 5s
     */
    public String getFormattedDuration() {
        long seconds = recordingDurationMs / 1000;
        if (seconds >= 60) {
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        }
        return String.format("%d.%ds", seconds, recordingDurationMs % 1000 / 100);
    }

    private static List<MethodStats> sorted(Map<String, MethodStats> stats, Comparator<MethodStats> order) {
        return stats.values().stream().sorted(order.reversed()).collect(Collectors.toList());
    }
}

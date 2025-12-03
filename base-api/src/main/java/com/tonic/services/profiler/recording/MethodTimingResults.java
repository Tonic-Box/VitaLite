package com.tonic.services.profiler.recording;

import com.tonic.services.profiler.visualization.FlameGraphNode;
import lombok.ToString;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregated results from a JFR method timing recording session.
 */
@ToString(exclude = {"flameGraphRoot", "callTreeRoot", "methodNodes"})
public class MethodTimingResults {
    public final Map<String, MethodStats> methodStats;
    public final int totalSamples;
    public final int samplePeriodMs;
    public final long recordingDurationMs;
    public final long recordingStartTime;
    public final long recordingEndTime;

    // Flame graph and call tree data
    public final FlameGraphNode flameGraphRoot;
    public final CallTreeNode callTreeRoot;
    public final Map<String, CallTreeNode> methodNodes;  // For caller/callee lookup

    public MethodTimingResults(
            Map<String, MethodStats> methodStats,
            int totalSamples,
            int samplePeriodMs,
            long recordingStartTime,
            long recordingEndTime,
            FlameGraphNode flameGraphRoot,
            CallTreeNode callTreeRoot,
            Map<String, CallTreeNode> methodNodes
    ) {
        this.methodStats = methodStats;
        this.totalSamples = totalSamples;
        this.samplePeriodMs = samplePeriodMs;
        this.recordingStartTime = recordingStartTime;
        this.recordingEndTime = recordingEndTime;
        this.recordingDurationMs = recordingEndTime - recordingStartTime;
        this.flameGraphRoot = flameGraphRoot;
        this.callTreeRoot = callTreeRoot;
        this.methodNodes = methodNodes;
    }

    /**
     * Get caller/callee info for a specific method
     */
    public CallTreeNode getMethodNode(String methodKey) {
        return methodNodes != null ? methodNodes.get(methodKey) : null;
    }

    /**
     * Get all methods sorted by self samples (highest first)
     */
    public List<MethodStats> getAllMethodsBySelfTime() {
        return methodStats.values().stream()
                .sorted(Comparator.comparingInt(MethodStats::getSelfSamples).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get all methods sorted by total samples (highest first)
     */
    public List<MethodStats> getAllMethodsByTotalTime() {
        return methodStats.values().stream()
                .sorted(Comparator.comparingInt(MethodStats::getTotalSamples).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get top N methods by self time
     */
    public List<MethodStats> getTopMethodsBySelfTime(int limit) {
        return methodStats.values().stream()
                .sorted(Comparator.comparingInt(MethodStats::getSelfSamples).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get top N methods by total time
     */
    public List<MethodStats> getTopMethodsByTotalTime(int limit) {
        return methodStats.values().stream()
                .sorted(Comparator.comparingInt(MethodStats::getTotalSamples).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get number of unique methods captured
     */
    public int getMethodCount() {
        return methodStats.size();
    }

    /**
     * Get estimated total CPU time based on samples
     */
    public long getEstimatedTotalCpuMs() {
        return (long) totalSamples * samplePeriodMs;
    }

    /**
     * Check if recording has meaningful data
     */
    public boolean hasData() {
        return totalSamples > 0 && !methodStats.isEmpty();
    }

    /**
     * Get recording duration as formatted string
     */
    public String getFormattedDuration() {
        long seconds = recordingDurationMs / 1000;
        long millis = recordingDurationMs % 1000;
        if (seconds >= 60) {
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%dm %ds", minutes, seconds);
        }
        return String.format("%d.%ds", seconds, millis / 100);
    }
}

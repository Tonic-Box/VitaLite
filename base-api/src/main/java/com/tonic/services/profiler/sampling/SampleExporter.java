package com.tonic.services.profiler.sampling;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Exports sampling results to various formats
 */
public class SampleExporter {

    /**
     * Export CPU analysis results to CSV
     */
    public void exportCPUToCSV(PrintWriter writer, SampleAnalyzer.CPUAnalysisResults results) {
        // Method hotspots
        writer.println("=== CPU Method Hotspots ===");
        writer.println("Method,Class,Package,Self Samples,Total Samples,Self %,Total %,Thread Count");

        List<SampleAnalyzer.MethodStats> topMethods = results.getTopMethods(1000);
        for (SampleAnalyzer.MethodStats stats : topMethods) {
            writer.printf("%s,%s,%s,%d,%d,%.2f,%.2f,%d\n",
                escapeCsv(stats.method.getMethodName()),
                escapeCsv(stats.method.getSimpleClassName()),
                escapeCsv(stats.method.getPackageName()),
                stats.selfSamples,
                stats.totalSamples,
                stats.selfTimePercent,
                stats.totalTimePercent,
                stats.threadSamples.size()
            );
        }

        writer.println();

        // Thread breakdown
        writer.println("=== Thread Breakdown ===");
        writer.println("Thread ID,Thread Name,Sample Count,CPU Time (ns),Runnable,Blocked,Waiting,Other");

        for (SampleAnalyzer.ThreadStats stats : results.threadBreakdown.values()) {
            writer.printf("%d,%s,%d,%d,%d,%d,%d,%d\n",
                stats.threadId,
                escapeCsv(stats.threadName),
                stats.sampleCount,
                stats.totalCpuTime,
                stats.states.getOrDefault(Thread.State.RUNNABLE, 0),
                stats.states.getOrDefault(Thread.State.BLOCKED, 0),
                stats.states.getOrDefault(Thread.State.WAITING, 0) +
                    stats.states.getOrDefault(Thread.State.TIMED_WAITING, 0),
                stats.states.getOrDefault(Thread.State.NEW, 0) +
                    stats.states.getOrDefault(Thread.State.TERMINATED, 0)
            );
        }

        writer.println();

        // Package aggregation
        writer.println("=== Package Aggregation ===");
        writer.println("Package,Total Samples,Self Samples,Method Count");

        List<SampleAnalyzer.PackageStats> topPackages = results.getTopPackages(100);
        for (SampleAnalyzer.PackageStats stats : topPackages) {
            writer.printf("%s,%d,%d,%d\n",
                escapeCsv(stats.packageName),
                stats.totalSamples,
                stats.selfSamples,
                stats.methodCount
            );
        }
    }

    /**
     * Export memory analysis results to CSV
     */
    public void exportMemoryToCSV(PrintWriter writer, SampleAnalyzer.MemoryAnalysisResults results) {
        writer.println("=== Memory Analysis Summary ===");
        writer.println("Metric,Value");
        writer.printf("Average Allocation Rate (MB/s),%.2f\n", results.averageAllocationRate);
        writer.printf("Peak Allocation Rate (MB/s),%.2f\n", results.peakAllocationRate);
        writer.printf("Total Estimated Allocation (MB),%.2f\n", results.totalEstimatedAllocation);
        writer.printf("Heap Growth Trend,%s\n", results.heapGrowthTrend);
        writer.printf("Total GC Events,%d\n", results.totalGCEvents);
        writer.printf("Average GC Overhead (%%),%.2f\n", results.averageGCOverhead);
        writer.printf("Full GC Count,%d\n", results.fullGCCount);
    }

    /**
     * Export raw heap samples to CSV
     */
    public void exportHeapSamplesToCSV(PrintWriter writer, RingBuffer<HeapDeltaSample> samples) {
        writer.println("=== Heap Delta Samples ===");
        writer.println("Timestamp,Heap Used (MB),Heap Committed (MB),Heap Max (MB),Utilization %,Allocation Rate (MB/s),GC Occurred");

        for (HeapDeltaSample sample : samples.getAll()) {
            writer.printf("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%s\n",
                sample.timestamp,
                sample.heapUsed / (1024.0 * 1024.0),
                sample.heapCommitted / (1024.0 * 1024.0),
                sample.heapMax / (1024.0 * 1024.0),
                sample.getHeapUtilization(),
                sample.allocationRateMBPerSec,
                sample.gcOccurredSinceLast ? "Yes" : "No"
            );
        }
    }

    /**
     * Export GC samples to CSV
     */
    public void exportGCSamplesToCSV(PrintWriter writer, RingBuffer<GCEventSample> samples) {
        writer.println("=== GC Event Samples ===");
        writer.println("Timestamp,Young GC Count,Full GC Count,Young GC Time (ms),Full GC Time (ms),GC Overhead %,Young GC Frequency (Hz)");

        for (GCEventSample sample : samples.getAll()) {
            writer.printf("%d,%d,%d,%d,%d,%.2f,%.3f\n",
                sample.timestamp,
                sample.youngGCCount,
                sample.fullGCCount,
                sample.youngGCTime,
                sample.fullGCTime,
                sample.gcOverheadPercent,
                sample.youngGCFrequency
            );
        }
    }

    /**
     * Export heap histogram to CSV
     */
    public void exportHeapHistogramToCSV(PrintWriter writer, java.util.List<HeapHistogramSample> histogram) {
        writer.println("=== Heap Histogram ===");
        writer.println("Class Name,Package,Instances,Total Bytes,Average Size (bytes),% of Heap");

        if (histogram == null || histogram.isEmpty()) {
            writer.println("No heap histogram data available");
            return;
        }

        // Calculate total heap bytes for percentage
        long totalHeapBytes = histogram.stream().mapToLong(s -> s.totalBytes).sum();

        for (HeapHistogramSample sample : histogram) {
            writer.printf("%s,%s,%d,%d,%d,%.2f\n",
                escapeCsv(sample.className),
                escapeCsv(sample.packageName),
                sample.instanceCount,
                sample.totalBytes,
                sample.averageSize,
                sample.getPercentOfTotal(totalHeapBytes)
            );
        }
    }

    /**
     * Export complete sampling session
     */
    public void exportComplete(
        PrintWriter writer,
        SampleAnalyzer.CPUAnalysisResults cpuResults,
        SampleAnalyzer.MemoryAnalysisResults memResults,
        RingBuffer<HeapDeltaSample> heapSamples,
        RingBuffer<GCEventSample> gcSamples,
        java.util.List<HeapHistogramSample> heapHistogram
    ) {
        writer.println("Profiling Session Export");
        writer.println("Generated: " + new java.util.Date());
        writer.println();

        if (cpuResults != null && cpuResults.totalSamples > 0) {
            exportCPUToCSV(writer, cpuResults);
            writer.println();
        }

        if (memResults != null) {
            exportMemoryToCSV(writer, memResults);
            writer.println();
        }

        if (heapSamples != null && !heapSamples.isEmpty()) {
            exportHeapSamplesToCSV(writer, heapSamples);
            writer.println();
        }

        if (gcSamples != null && !gcSamples.isEmpty()) {
            exportGCSamplesToCSV(writer, gcSamples);
            writer.println();
        }

        if (heapHistogram != null && !heapHistogram.isEmpty()) {
            exportHeapHistogramToCSV(writer, heapHistogram);
        }
    }

    /**
     * Escape CSV special characters
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

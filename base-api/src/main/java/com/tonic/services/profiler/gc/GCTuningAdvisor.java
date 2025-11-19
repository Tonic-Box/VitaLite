package com.tonic.services.profiler.gc;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides GC tuning recommendations based on observed pause patterns
 */
public class GCTuningAdvisor {
    private final GCPauseAnalyzer analyzer;

    public GCTuningAdvisor(GCPauseAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Generate tuning recommendations
     */
    public List<Recommendation> getRecommendations() {
        List<Recommendation> recommendations = new ArrayList<>();

        GCPauseAnalyzer.GCStatistics stats = analyzer.getStatistics();
        if (stats.totalPauses < 10) {
            recommendations.add(new Recommendation(
                Severity.INFO,
                "Insufficient Data",
                "Collect more GC events (at least 10) for meaningful analysis",
                null
            ));
            return recommendations;
        }

        // Detect GC algorithm
        String gcAlgorithm = detectGCAlgorithm();

        // Check for excessive pause times
        if (stats.p99Pause > 1000) { // > 1 second
            recommendations.add(new Recommendation(
                Severity.CRITICAL,
                "Very Long GC Pauses",
                "99th percentile pause time is " + stats.p99Pause + "ms. This can cause severe application lag.",
                gcAlgorithm.contains("G1") ?
                    "-XX:MaxGCPauseMillis=200 (reduce target pause time)" :
                    "Consider switching to G1GC: -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
            ));
        } else if (stats.p99Pause > 500) { // > 500ms
            recommendations.add(new Recommendation(
                Severity.WARNING,
                "Long GC Pauses",
                "99th percentile pause time is " + stats.p99Pause + "ms. May impact responsiveness.",
                gcAlgorithm.contains("G1") ?
                    "-XX:MaxGCPauseMillis=100 or increase heap size" :
                    "Consider G1GC for better pause time control"
            ));
        }

        // Check GC frequency
        if (stats.totalPauses > 100) {
            double pausesPerSecond = stats.totalPauses / 60.0; // Assuming 1 minute window
            if (pausesPerSecond > 10) {
                recommendations.add(new Recommendation(
                    Severity.WARNING,
                    "Frequent GC Events",
                    "GC occurring " + String.format("%.1f", pausesPerSecond) + " times per second. High overhead.",
                    "Increase heap size: -Xmx<larger_value> or optimize allocation patterns"
                ));
            }
        }

        // Check GC time percentage
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        if (uptimeMs > 0) {
            double gcTimePercent = (stats.totalPauseTime * 100.0) / uptimeMs;
            if (gcTimePercent > 10) {
                recommendations.add(new Recommendation(
                    Severity.CRITICAL,
                    "Excessive GC Overhead",
                    String.format("%.1f%% of runtime spent in GC. Severe performance impact.", gcTimePercent),
                    "Increase heap size significantly or optimize object allocation"
                ));
            } else if (gcTimePercent > 5) {
                recommendations.add(new Recommendation(
                    Severity.WARNING,
                    "High GC Overhead",
                    String.format("%.1f%% of runtime spent in GC.", gcTimePercent),
                    "Consider increasing heap size: -Xmx<larger_value>"
                ));
            }
        }

        // Check heap utilization
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        double heapUtilization = (heapUsed * 100.0) / heapMax;

        if (heapUtilization > 90) {
            recommendations.add(new Recommendation(
                Severity.CRITICAL,
                "Critical Heap Pressure",
                String.format("Heap %.1f%% full. Risk of OutOfMemoryError.", heapUtilization),
                "Increase heap size immediately: -Xmx<larger_value>"
            ));
        } else if (heapUtilization > 80) {
            recommendations.add(new Recommendation(
                Severity.WARNING,
                "High Heap Utilization",
                String.format("Heap %.1f%% full. Consider increasing heap size.", heapUtilization),
                "Increase heap size: -Xmx<larger_value>"
            ));
        } else if (heapUtilization < 30) {
            recommendations.add(new Recommendation(
                Severity.INFO,
                "Low Heap Utilization",
                String.format("Heap only %.1f%% used. May be over-provisioned.", heapUtilization),
                "Consider reducing heap size to free system memory: -Xmx<smaller_value>"
            ));
        }

        // Algorithm-specific recommendations
        if (gcAlgorithm.contains("SerialGC")) {
            recommendations.add(new Recommendation(
                Severity.INFO,
                "Using Serial GC",
                "Serial GC is single-threaded. Not recommended for multi-core systems.",
                "Switch to G1GC: -XX:+UseG1GC or Parallel GC: -XX:+UseParallelGC"
            ));
        } else if (gcAlgorithm.contains("ParallelGC")) {
            if (stats.longestPause > 500) {
                recommendations.add(new Recommendation(
                    Severity.INFO,
                    "Parallel GC Pause Times",
                    "Parallel GC can have long pauses. Consider G1GC for better pause control.",
                    "Switch to G1GC: -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
                ));
            }
        }

        // If no issues found
        if (recommendations.isEmpty()) {
            recommendations.add(new Recommendation(
                Severity.SUCCESS,
                "GC Performance Healthy",
                "No significant GC issues detected. Performance is within acceptable ranges.",
                null
            ));
        }

        return recommendations;
    }

    /**
     * Detect which GC algorithm is being used
     */
    private String detectGCAlgorithm() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        StringBuilder sb = new StringBuilder();

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(gcBean.getName());
        }

        String gcNames = sb.toString();

        // Detect common GC algorithms
        if (gcNames.contains("G1")) {
            return "G1GC";
        } else if (gcNames.contains("Parallel")) {
            return "ParallelGC";
        } else if (gcNames.contains("ConcurrentMarkSweep") || gcNames.contains("CMS")) {
            return "CMS";
        } else if (gcNames.contains("ZGC")) {
            return "ZGC";
        } else if (gcNames.contains("Shenandoah")) {
            return "Shenandoah";
        } else if (gcNames.contains("Serial")) {
            return "SerialGC";
        }

        return "Unknown (" + gcNames + ")";
    }

    /**
     * A single tuning recommendation
     */
    public static class Recommendation {
        public final Severity severity;
        public final String title;
        public final String description;
        public final String suggestion;

        public Recommendation(Severity severity, String title, String description, String suggestion) {
            this.severity = severity;
            this.title = title;
            this.description = description;
            this.suggestion = suggestion;
        }
    }

    /**
     * Recommendation severity levels
     */
    public enum Severity {
        SUCCESS("Healthy", 0x4CAF50),    // Green
        INFO("Info", 0x2196F3),          // Blue
        WARNING("Warning", 0xFF9800),    // Orange
        CRITICAL("Critical", 0xF44336);  // Red

        private final String label;
        private final int color;

        Severity(String label, int color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public int getColor() {
            return color;
        }
    }
}

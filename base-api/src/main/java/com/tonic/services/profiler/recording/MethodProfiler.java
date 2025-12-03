package com.tonic.services.profiler.recording;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Manual method timing profiler for exact per-invocation timing.
 *
 * Usage:
 *   MethodProfiler.begin("MyClass.myMethod");
 *   // ... your code ...
 *   MethodProfiler.end("MyClass.myMethod");
 *
 * Or with try-with-resources:
 *   try (var ignored = MethodProfiler.time("MyClass.myMethod")) {
 *       // ... your code ...
 *   }
 *
 * Or with lambda:
 *   MethodProfiler.time("MyClass.myMethod", () -> {
 *       // ... your code ...
 *   });
 *
 *   result = MethodProfiler.timeResult("MyClass.myMethod", () -> {
 *       return computeValue();
 *   });
 */
public class MethodProfiler {

    private static final Map<String, MethodTiming> timings = new ConcurrentHashMap<>();
    private static final ThreadLocal<Map<String, Long>> activeTimings = ThreadLocal.withInitial(ConcurrentHashMap::new);
    private static volatile boolean enabled = false;  // Disabled by default for zero overhead

    /**
     * Start timing a method/section. Must be paired with end().
     */
    public static void begin(String label) {
        if (!enabled) return;
        activeTimings.get().put(label, System.nanoTime());
    }

    /**
     * End timing a method/section. Must be paired with begin().
     */
    public static void end(String label) {
        if (!enabled) return;

        Long startTime = activeTimings.get().remove(label);
        if (startTime == null) {
            return; // No matching begin()
        }

        long elapsedNanos = System.nanoTime() - startTime;
        timings.computeIfAbsent(label, MethodTiming::new).record(elapsedNanos);
    }

    /**
     * Time a code block using try-with-resources.
     * Usage: try (var ignored = MethodProfiler.time("label")) { ... }
     */
    public static TimingScope time(String label) {
        begin(label);
        return () -> end(label);
    }

    /**
     * Time a void operation with a lambda.
     */
    public static void time(String label, Runnable operation) {
        begin(label);
        try {
            operation.run();
        } finally {
            end(label);
        }
    }

    /**
     * Time an operation that returns a value.
     */
    public static <T> T timeResult(String label, java.util.function.Supplier<T> operation) {
        begin(label);
        try {
            return operation.get();
        } finally {
            end(label);
        }
    }

    /**
     * Get all recorded timings sorted by total time (highest first).
     */
    public static List<MethodTiming> getAllTimings() {
        return timings.values().stream()
                .sorted(Comparator.comparingLong(MethodTiming::getTotalNanos).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get all recorded timings sorted by average time per call (highest first).
     */
    public static List<MethodTiming> getAllTimingsByAverage() {
        return timings.values().stream()
                .sorted(Comparator.comparingDouble(MethodTiming::getAverageMs).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get timing for a specific label.
     */
    public static MethodTiming getTiming(String label) {
        return timings.get(label);
    }

    /**
     * Clear all recorded timings.
     */
    public static void clear() {
        timings.clear();
        activeTimings.get().clear();
    }

    /**
     * Enable or disable profiling (disabled = zero overhead).
     */
    public static void setEnabled(boolean enabled) {
        MethodProfiler.enabled = enabled;
    }

    /**
     * Check if profiling is enabled.
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Get count of tracked methods.
     */
    public static int getMethodCount() {
        return timings.size();
    }

    /**
     * Check if there's any data recorded.
     */
    public static boolean hasData() {
        return !timings.isEmpty();
    }

    /**
     * Generate a text report of all profiling data.
     */
    public static String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== MethodProfiler Report ===\n");
        sb.append(String.format("Generated: %s\n", java.time.LocalDateTime.now()));
        sb.append(String.format("Methods tracked: %d\n\n", timings.size()));

        List<MethodTiming> sorted = getAllTimings();
        if (sorted.isEmpty()) {
            sb.append("No data recorded.\n");
            return sb.toString();
        }

        // Calculate totals
        long totalCalls = sorted.stream().mapToLong(MethodTiming::getCallCount).sum();
        double totalMs = sorted.stream().mapToDouble(MethodTiming::getTotalMs).sum();

        sb.append(String.format("Total calls: %,d\n", totalCalls));
        sb.append(String.format("Total time: %.2fms\n\n", totalMs));

        // Header
        sb.append(String.format("%-50s %12s %12s %12s %12s %12s\n",
                "Method", "Calls", "Total", "Average", "Min", "Max"));
        sb.append("-".repeat(110)).append("\n");

        // Data rows
        for (MethodTiming timing : sorted) {
            sb.append(String.format("%-50s %,12d %12s %12s %12s %12s\n",
                    truncate(timing.getLabel(), 50),
                    timing.getCallCount(),
                    timing.getFormattedTotalTime(),
                    timing.getFormattedAverageTime(),
                    timing.getFormattedMinTime(),
                    timing.getFormattedMaxTime()));
        }

        return sb.toString();
    }

    /**
     * Generate a CSV report of all profiling data.
     */
    public static String generateCSVReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Method,Calls,TotalMs,AverageMs,MinMs,MaxMs\n");

        for (MethodTiming timing : getAllTimings()) {
            sb.append(String.format("\"%s\",%d,%.6f,%.6f,%.6f,%.6f\n",
                    timing.getLabel().replace("\"", "\"\""),
                    timing.getCallCount(),
                    timing.getTotalMs(),
                    timing.getAverageMs(),
                    timing.getMinMs(),
                    timing.getMaxMs()));
        }

        return sb.toString();
    }

    private static String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }

    /**
     * AutoCloseable scope for try-with-resources timing.
     */
    @FunctionalInterface
    public interface TimingScope extends AutoCloseable {
        @Override
        void close(); // No exception
    }

    /**
     * Timing statistics for a single method/label.
     */
    public static class MethodTiming {
        private final String label;
        private final AtomicLong callCount = new AtomicLong(0);
        private final AtomicLong totalNanos = new AtomicLong(0);
        private volatile long minNanos = Long.MAX_VALUE;
        private volatile long maxNanos = Long.MIN_VALUE;
        private final Object minMaxLock = new Object();

        public MethodTiming(String label) {
            this.label = label;
        }

        void record(long elapsedNanos) {
            callCount.incrementAndGet();
            totalNanos.addAndGet(elapsedNanos);

            synchronized (minMaxLock) {
                if (elapsedNanos < minNanos) minNanos = elapsedNanos;
                if (elapsedNanos > maxNanos) maxNanos = elapsedNanos;
            }
        }

        public String getLabel() {
            return label;
        }

        public long getCallCount() {
            return callCount.get();
        }

        public long getTotalNanos() {
            return totalNanos.get();
        }

        public double getTotalMs() {
            return totalNanos.get() / 1_000_000.0;
        }

        public double getAverageNanos() {
            long count = callCount.get();
            return count > 0 ? (double) totalNanos.get() / count : 0;
        }

        public double getAverageMs() {
            return getAverageNanos() / 1_000_000.0;
        }

        public double getMinMs() {
            return minNanos == Long.MAX_VALUE ? 0 : minNanos / 1_000_000.0;
        }

        public double getMaxMs() {
            return maxNanos == Long.MIN_VALUE ? 0 : maxNanos / 1_000_000.0;
        }

        /**
         * Get display name (last part of label if it contains dots).
         */
        public String getDisplayName() {
            int lastDot = label.lastIndexOf('.');
            if (lastDot > 0 && lastDot < label.length() - 1) {
                // Return ClassName.methodName format
                String beforeDot = label.substring(0, lastDot);
                int secondLastDot = beforeDot.lastIndexOf('.');
                if (secondLastDot > 0) {
                    return label.substring(secondLastDot + 1);
                }
            }
            return label;
        }

        /**
         * Format total time as human-readable string.
         */
        public String getFormattedTotalTime() {
            return formatMs(getTotalMs());
        }

        /**
         * Format average time as human-readable string.
         */
        public String getFormattedAverageTime() {
            double avgMs = getAverageMs();
            if (avgMs < 0.001) {
                return String.format("%.2f\u00B5s", getAverageNanos() / 1000.0);
            }
            return formatMs(avgMs);
        }

        /**
         * Format min time as human-readable string.
         */
        public String getFormattedMinTime() {
            double ms = getMinMs();
            if (ms < 0.001) {
                return String.format("%.2f\u00B5s", minNanos / 1000.0);
            }
            return formatMs(ms);
        }

        /**
         * Format max time as human-readable string.
         */
        public String getFormattedMaxTime() {
            double ms = getMaxMs();
            if (ms < 0.001) {
                return String.format("%.2f\u00B5s", maxNanos / 1000.0);
            }
            return formatMs(ms);
        }

        private String formatMs(double ms) {
            if (ms < 1) {
                return String.format("%.3fms", ms);
            } else if (ms < 1000) {
                return String.format("%.2fms", ms);
            } else if (ms < 60000) {
                return String.format("%.2fs", ms / 1000.0);
            } else {
                long minutes = (long) (ms / 60000);
                double seconds = (ms % 60000) / 1000.0;
                return String.format("%dm %.1fs", minutes, seconds);
            }
        }

        @Override
        public String toString() {
            return String.format("%s: calls=%d, total=%s, avg=%s, min=%s, max=%s",
                    label, getCallCount(), getFormattedTotalTime(),
                    getFormattedAverageTime(), getFormattedMinTime(), getFormattedMaxTime());
        }
    }
}

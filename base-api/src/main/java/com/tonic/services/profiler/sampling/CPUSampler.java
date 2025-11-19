package com.tonic.services.profiler.sampling;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CPU profiler using stack trace sampling and thread CPU time tracking
 * Captures stack traces at regular intervals to build statistical profile
 */
public class CPUSampler {
    private final ThreadMXBean threadBean;
    private final RingBuffer<StackSample> stackSamples;
    private final Map<Long, ThreadCPUData> lastCPUData;

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong totalSamples = new AtomicLong(0);
    private final AtomicLong failedSamples = new AtomicLong(0);

    // Configuration
    private int samplingIntervalMs = 50;
    private int maxStackDepth = 64;
    private boolean filterSystemFrames = false;
    private Set<String> includedPackages = null;
    private Set<String> excludedPackages = null;
    private boolean onlyRunnableThreads = false;

    public CPUSampler(int maxSamples) {
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.stackSamples = new RingBuffer<>(maxSamples);
        this.lastCPUData = new HashMap<>();

        // Enable thread CPU time tracking if available
        if (threadBean.isThreadCpuTimeSupported()) {
            threadBean.setThreadCpuTimeEnabled(true);
        }
    }

    /**
     * Start sampling
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduler = Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "CPU-Sampler");
                t.setDaemon(true);
                return t;
            });

            scheduler.scheduleAtFixedRate(
                this::captureSample,
                0,
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
     * Capture a single sample
     */
    private void captureSample() {
        try {
            long timestamp = System.currentTimeMillis();

            // Get all thread stack traces
            ThreadInfo[] allThreads = threadBean.dumpAllThreads(false, false);

            for (ThreadInfo threadInfo : allThreads) {
                if (threadInfo == null) continue;

                // Filter by thread state if requested
                if (onlyRunnableThreads && threadInfo.getThreadState() != Thread.State.RUNNABLE) {
                    continue;
                }

                // Get stack trace
                StackTraceElement[] stack = threadInfo.getStackTrace();
                if (stack == null || stack.length == 0) {
                    continue;
                }

                // Apply filters
                if (!shouldIncludeStack(stack)) {
                    continue;
                }

                // Limit stack depth
                if (stack.length > maxStackDepth) {
                    StackTraceElement[] truncated = new StackTraceElement[maxStackDepth];
                    System.arraycopy(stack, 0, truncated, 0, maxStackDepth);
                    stack = truncated;
                }

                // Get thread CPU time
                long cpuTime = 0;
                long userTime = 0;
                if (threadBean.isThreadCpuTimeEnabled()) {
                    try {
                        cpuTime = threadBean.getThreadCpuTime(threadInfo.getThreadId());
                        userTime = threadBean.getThreadUserTime(threadInfo.getThreadId());
                    } catch (Exception ignored) {
                        // Thread may have terminated
                    }
                }

                // Create sample
                StackSample sample = new StackSample(
                    timestamp,
                    threadInfo.getThreadId(),
                    threadInfo.getThreadName(),
                    threadInfo.getThreadState(),
                    stack,
                    cpuTime,
                    userTime,
                    threadInfo.getBlockedTime(),
                    threadInfo.getWaitedTime()
                );

                stackSamples.add(sample);
                totalSamples.incrementAndGet();

                // Update CPU time tracking
                updateCPUTracking(threadInfo.getThreadId(), cpuTime, userTime, timestamp);
            }

        } catch (Exception e) {
            failedSamples.incrementAndGet();
            System.err.println("CPU sampling error: " + e.getMessage());
        }
    }

    /**
     * Update thread CPU time tracking for delta calculations
     */
    private void updateCPUTracking(long threadId, long cpuTime, long userTime, long timestamp) {
        ThreadCPUData data = lastCPUData.computeIfAbsent(threadId, k -> new ThreadCPUData());
        data.lastCpuTime = cpuTime;
        data.lastUserTime = userTime;
        data.lastTimestamp = timestamp;
    }

    /**
     * Check if stack should be included based on filters
     */
    private boolean shouldIncludeStack(StackTraceElement[] stack) {
        if (stack.length == 0) return false;

        // When filtering system frames, check if the stack contains ANY non-system code
        // This is important for applications with child classloaders where system code
        // often appears at the top of stacks (event dispatch, thread pools, etc.)
        if (filterSystemFrames) {
            boolean hasNonSystemCode = false;
            for (StackTraceElement frame : stack) {
                if (!isSystemClass(frame.getClassName())) {
                    hasNonSystemCode = true;
                    break;
                }
            }
            // Reject stacks that are ONLY system code
            if (!hasNonSystemCode) {
                return false;
            }
        }

        // Package inclusion filter - check if ANY frame matches
        if (includedPackages != null && !includedPackages.isEmpty()) {
            boolean included = false;
            for (StackTraceElement frame : stack) {
                String className = frame.getClassName();
                for (String pkg : includedPackages) {
                    if (className.startsWith(pkg)) {
                        included = true;
                        break;
                    }
                }
                if (included) break;
            }
            if (!included) return false;
        }

        // Package exclusion filter - reject if top frame is excluded
        // (but don't reject entire stack just because system code appears somewhere)
        if (excludedPackages != null && !excludedPackages.isEmpty()) {
            String topClassName = stack[0].getClassName();
            for (String pkg : excludedPackages) {
                if (topClassName.startsWith(pkg)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check if class is a system/framework class
     */
    private boolean isSystemClass(String className) {
        // Core JDK classes
        if (className.startsWith("java.") ||
            className.startsWith("javax.") ||
            className.startsWith("sun.") ||
            className.startsWith("com.sun.") ||
            className.startsWith("jdk.")) {
            return true;
        }

        // Common framework/infrastructure that usually isn't interesting
        if (className.startsWith("org.jfree.") ||     // JFreeChart (used in profiler UI)
            className.startsWith("org.slf4j.") ||     // Logging framework
            className.startsWith("ch.qos.logback.")) { // Logback
            return true;
        }

        // Thread infrastructure
        if (className.contains("$$Lambda$") ||        // Lambda proxies
            className.contains("$Proxy") ||           // Dynamic proxies
            className.startsWith("sun.reflect.")) {   // Reflection infrastructure
            return true;
        }

        return false;
    }

    // ==================== Configuration Methods ====================

    public void setSamplingInterval(int intervalMs) {
        this.samplingIntervalMs = Math.max(10, intervalMs);
    }

    public void setMaxStackDepth(int depth) {
        this.maxStackDepth = Math.max(1, depth);
    }

    public void setFilterSystemFrames(boolean filter) {
        this.filterSystemFrames = filter;
    }

    public void setIncludedPackages(Set<String> packages) {
        this.includedPackages = packages;
    }

    public void setExcludedPackages(Set<String> packages) {
        this.excludedPackages = packages;
    }

    public void setOnlyRunnableThreads(boolean onlyRunnable) {
        this.onlyRunnableThreads = onlyRunnable;
    }

    // ==================== Data Access Methods ====================

    public RingBuffer<StackSample> getStackSamples() {
        return stackSamples;
    }

    public Map<Long, ThreadCPUData> getCPUData() {
        synchronized (lastCPUData) {
            return new HashMap<>(lastCPUData);
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public long getTotalSamples() {
        return totalSamples.get();
    }

    public long getFailedSamples() {
        return failedSamples.get();
    }

    public int getSamplingIntervalMs() {
        return samplingIntervalMs;
    }

    public SamplingStats getStats() {
        return new SamplingStats(
            isRunning(),
            totalSamples.get(),
            failedSamples.get(),
            stackSamples.size(),
            stackSamples.capacity(),
            stackSamples.getDroppedCount(),
            samplingIntervalMs
        );
    }

    /**
     * Clear all samples
     */
    public void clear() {
        stackSamples.clear();
        synchronized (lastCPUData) {
            lastCPUData.clear();
        }
        totalSamples.set(0);
        failedSamples.set(0);
    }

    /**
     * Thread CPU time tracking data
     */
    public static class ThreadCPUData {
        public long lastCpuTime;
        public long lastUserTime;
        public long lastTimestamp;
    }

    /**
     * Sampling statistics
     */
    public static class SamplingStats {
        public final boolean running;
        public final long totalSamples;
        public final long failedSamples;
        public final int currentSamples;
        public final int maxSamples;
        public final long droppedSamples;
        public final int intervalMs;

        public SamplingStats(boolean running, long totalSamples, long failedSamples,
                           int currentSamples, int maxSamples, long droppedSamples, int intervalMs) {
            this.running = running;
            this.totalSamples = totalSamples;
            this.failedSamples = failedSamples;
            this.currentSamples = currentSamples;
            this.maxSamples = maxSamples;
            this.droppedSamples = droppedSamples;
            this.intervalMs = intervalMs;
        }

        public double getSuccessRate() {
            return totalSamples > 0 ? (totalSamples - failedSamples) * 100.0 / totalSamples : 100.0;
        }

        @Override
        public String toString() {
            return String.format("CPUSampler[running=%s, samples=%d/%d, dropped=%d, interval=%dms]",
                running, currentSamples, maxSamples, droppedSamples, intervalMs);
        }
    }
}

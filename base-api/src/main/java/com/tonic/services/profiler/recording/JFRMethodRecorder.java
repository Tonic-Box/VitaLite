package com.tonic.services.profiler.recording;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingFile;

import com.tonic.services.profiler.visualization.FlameGraphNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JFR-based method timing recorder.
 * Uses Java Flight Recorder's ExecutionSample events for low-overhead profiling.
 *
 * Usage:
 *   recorder.setSamplePeriodMs(10);
 *   recorder.setFilterMode(FilterMode.PACKAGE);
 *   recorder.setPackageFilter("com.tonic");
 *   recorder.startRecording();
 *   // ... do work ...
 *   recorder.stopRecording();
 *   MethodTimingResults results = recorder.analyze();
 */
public class JFRMethodRecorder {

    /**
     * Filter mode for which methods to include in results
     */
    public enum FilterMode {
        /** Include all Java methods */
        ALL,
        /** Include only methods from classes starting with packageFilter */
        PACKAGE,
        /** Include only methods from the exact classFilter class */
        CLASS
    }

    private Recording recording;
    private Path tempFile;
    private volatile boolean isRecording;
    private long recordingStartTime;
    private long recordingEndTime;

    // Configuration
    private int samplePeriodMs = 10;
    private FilterMode filterMode = FilterMode.ALL;
    private String packageFilter = "";
    private String classFilter = "";

    /**
     * Check if JFR is available on this JVM
     */
    public static boolean isAvailable() {
        try {
            return FlightRecorder.isAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Start a new recording session
     */
    public void startRecording() throws IOException {
        if (isRecording) {
            throw new IllegalStateException("Recording already in progress");
        }

        // Create temp file for JFR data
        tempFile = Files.createTempFile("jfr_method_recording_", ".jfr");

        // Create and configure recording
        recording = new Recording();

        // Enable ExecutionSample for method profiling
        recording.enable("jdk.ExecutionSample")
                .withPeriod(Duration.ofMillis(samplePeriodMs))
                .withStackTrace();

        // Also enable NativeMethodSample for native code (optional)
        recording.enable("jdk.NativeMethodSample")
                .withPeriod(Duration.ofMillis(samplePeriodMs));

        // Configure recording destination
        recording.setToDisk(true);
        recording.setDestination(tempFile);

        // Start recording
        recording.start();
        recordingStartTime = System.currentTimeMillis();
        isRecording = true;
    }

    /**
     * Stop the current recording session
     */
    public void stopRecording() {
        if (!isRecording || recording == null) {
            return;
        }

        recordingEndTime = System.currentTimeMillis();
        isRecording = false;

        try {
            recording.stop();
            recording.close();
        } catch (Exception e) {
            System.err.println("Error stopping JFR recording: " + e.getMessage());
        }
    }

    /**
     * Analyze the recorded data and return method timing results.
     * Should be called after stopRecording().
     */
    public MethodTimingResults analyze() throws IOException {
        if (tempFile == null || !Files.exists(tempFile)) {
            return new MethodTimingResults(new HashMap<>(), 0, samplePeriodMs,
                    recordingStartTime, recordingEndTime, null, null, new HashMap<>());
        }

        Map<String, MethodStats> stats = new HashMap<>();
        FlameGraphNode flameRoot = new FlameGraphNode("(all)", "", "");
        CallTreeNode callTreeRoot = CallTreeNode.createRoot();
        Map<String, CallTreeNode> methodNodes = new HashMap<>();  // For caller/callee tracking
        int totalSamples = 0;

        try (RecordingFile rf = new RecordingFile(tempFile)) {
            while (rf.hasMoreEvents()) {
                RecordedEvent event = rf.readEvent();
                String eventName = event.getEventType().getName();

                // Process execution samples
                if ("jdk.ExecutionSample".equals(eventName) ||
                    "jdk.NativeMethodSample".equals(eventName)) {

                    RecordedStackTrace stackTrace = event.getStackTrace();
                    if (stackTrace != null) {
                        processStackTrace(stackTrace, stats, flameRoot, callTreeRoot, methodNodes);
                        totalSamples++;
                    }
                }
            }
        } finally {
            // Cleanup temp file
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                // Ignore cleanup errors
            }
            tempFile = null;
        }

        // Update flame root sample count
        flameRoot.addSamples(totalSamples);

        return new MethodTimingResults(stats, totalSamples, samplePeriodMs,
                recordingStartTime, recordingEndTime, flameRoot, callTreeRoot, methodNodes);
    }

    /**
     * Process a single stack trace sample
     */
    private void processStackTrace(RecordedStackTrace stackTrace, Map<String, MethodStats> stats,
                                   FlameGraphNode flameRoot, CallTreeNode callTreeRoot,
                                   Map<String, CallTreeNode> methodNodes) {
        List<RecordedFrame> frames = stackTrace.getFrames();
        Set<String> seenMethods = new HashSet<>();  // For total time deduplication

        // Build list of filtered frames for flame graph (reversed - bottom to top)
        List<FrameInfo> filteredFrames = new ArrayList<>();

        for (int i = 0; i < frames.size(); i++) {
            RecordedFrame frame = frames.get(i);

            // Skip non-Java frames
            if (!frame.isJavaFrame()) {
                continue;
            }

            RecordedMethod method = frame.getMethod();
            if (method == null || method.getType() == null) {
                continue;
            }

            String className = method.getType().getName();
            String methodName = method.getName();

            // Skip internal/synthetic methods
            if (methodName.startsWith("lambda$") && !shouldIncludeLambdas()) {
                continue;
            }

            // Apply filter
            if (!matchesFilter(className)) {
                continue;
            }

            String methodKey = className + "." + methodName;
            String packageName = extractPackage(className);

            // Add to filtered frames for flame graph
            filteredFrames.add(new FrameInfo(methodKey, className, methodName, packageName, i == 0));

            MethodStats methodStats = stats.computeIfAbsent(methodKey, MethodStats::new);

            // Top frame = self time (this method was actually executing)
            if (i == 0) {
                methodStats.incrementSelfSamples();
            }

            // All frames = total time (method was on the call stack)
            // Only count once per sample to avoid double-counting recursive calls
            if (seenMethods.add(methodKey)) {
                methodStats.incrementTotalSamples();
            }

            // Track in methodNodes for caller/callee analysis
            CallTreeNode methodNode = methodNodes.computeIfAbsent(methodKey,
                k -> new CallTreeNode(methodKey, className, methodName, packageName));
            if (i == 0) {
                methodNode.addSelfSample();
            }
            methodNode.addTotalSample();
        }

        // Build flame graph (bottom-up: root -> callers -> ... -> executing method)
        if (!filteredFrames.isEmpty()) {
            FlameGraphNode currentFlame = flameRoot;
            // Reverse iterate (from bottom of stack to top)
            for (int i = filteredFrames.size() - 1; i >= 0; i--) {
                FrameInfo fi = filteredFrames.get(i);
                currentFlame = currentFlame.getOrCreateChild(fi.methodName, fi.className, fi.packageName);
                currentFlame.addSample();
            }
        }

        // Build call tree (top-down: root -> called methods -> ... -> leaf)
        if (!filteredFrames.isEmpty()) {
            CallTreeNode currentCall = callTreeRoot;
            // Forward iterate (from top of stack - executing - down to callers)
            for (FrameInfo fi : filteredFrames) {
                currentCall = currentCall.getOrCreateChild(fi.methodKey, fi.className, fi.methodName, fi.packageName);
                if (fi.isTop) {
                    currentCall.addSelfSample();
                }
                currentCall.addTotalSample();
            }
        }

        // Track caller/callee relationships
        for (int i = 0; i < filteredFrames.size(); i++) {
            FrameInfo current = filteredFrames.get(i);
            CallTreeNode currentNode = methodNodes.get(current.methodKey);

            if (currentNode != null) {
                // The frame below in the list is the caller (called current)
                if (i + 1 < filteredFrames.size()) {
                    FrameInfo caller = filteredFrames.get(i + 1);
                    currentNode.addCaller(caller.methodKey);

                    // Also track callee from caller's perspective
                    CallTreeNode callerNode = methodNodes.get(caller.methodKey);
                    if (callerNode != null) {
                        callerNode.addCallee(current.methodKey);
                    }
                }
            }
        }
    }

    /**
     * Extract package name from fully qualified class name
     */
    private String extractPackage(String className) {
        if (className == null) return "";
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }

    /**
     * Helper class to hold frame information
     */
    private static class FrameInfo {
        final String methodKey;
        final String className;
        final String methodName;
        final String packageName;
        final boolean isTop;

        FrameInfo(String methodKey, String className, String methodName, String packageName, boolean isTop) {
            this.methodKey = methodKey;
            this.className = className;
            this.methodName = methodName;
            this.packageName = packageName;
            this.isTop = isTop;
        }
    }

    /**
     * Check if a class matches the current filter
     */
    private boolean matchesFilter(String className) {
        if (className == null) return false;

        switch (filterMode) {
            case ALL:
                return true;

            case PACKAGE:
                if (packageFilter == null || packageFilter.isEmpty()) {
                    return true;
                }
                return className.startsWith(packageFilter);

            case CLASS:
                if (classFilter == null || classFilter.isEmpty()) {
                    return true;
                }
                return className.equals(classFilter);

            default:
                return true;
        }
    }

    /**
     * Whether to include lambda methods in results
     */
    private boolean shouldIncludeLambdas() {
        // For now, exclude lambdas by default (they clutter results)
        return false;
    }

    // ==================== Configuration Setters ====================

    public void setSamplePeriodMs(int samplePeriodMs) {
        if (isRecording) {
            throw new IllegalStateException("Cannot change settings while recording");
        }
        this.samplePeriodMs = Math.max(1, Math.min(1000, samplePeriodMs));
    }

    public void setFilterMode(FilterMode filterMode) {
        if (isRecording) {
            throw new IllegalStateException("Cannot change settings while recording");
        }
        this.filterMode = filterMode != null ? filterMode : FilterMode.ALL;
    }

    public void setPackageFilter(String packageFilter) {
        if (isRecording) {
            throw new IllegalStateException("Cannot change settings while recording");
        }
        this.packageFilter = packageFilter != null ? packageFilter.trim() : "";
    }

    public void setClassFilter(String classFilter) {
        if (isRecording) {
            throw new IllegalStateException("Cannot change settings while recording");
        }
        this.classFilter = classFilter != null ? classFilter.trim() : "";
    }

    // ==================== Getters ====================

    public boolean isRecording() {
        return isRecording;
    }

    public int getSamplePeriodMs() {
        return samplePeriodMs;
    }

    public FilterMode getFilterMode() {
        return filterMode;
    }

    public String getPackageFilter() {
        return packageFilter;
    }

    public String getClassFilter() {
        return classFilter;
    }

    public long getRecordingDurationMs() {
        if (isRecording) {
            return System.currentTimeMillis() - recordingStartTime;
        }
        return recordingEndTime - recordingStartTime;
    }
}

package com.tonic.services.profiler.recording;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingFile;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Records Java Flight Recorder samples: Java execution, threads blocked in native code, and optionally allocations,
 * then turns them into results
 */
public class JFRMethodRecorder {
    private static final String EXECUTION = "jdk.ExecutionSample";
    private static final String NATIVE = "jdk.NativeMethodSample";
    private static final String ALLOCATION_IN_TLAB = "jdk.ObjectAllocationInNewTLAB";
    private static final String ALLOCATION_OUTSIDE_TLAB = "jdk.ObjectAllocationOutsideTLAB";

    /**
     * Which methods the results show
     */
    public enum FilterMode {
        ALL,
        PACKAGE,
        CLASS
    }

    private Recording recording;
    private Path tempFile;
    private volatile boolean isRecording;
    private long recordingStartTime;
    private long recordingEndTime;
    @Getter
    private int samplePeriodMs = 10;
    private FilterMode filterMode = FilterMode.ALL;
    private String packageFilter = "";
    private String classFilter = "";
    private boolean recordAllocations;
    private boolean allocationsRecorded;

    /**
     * Checks whether this JVM has Flight Recorder
     * @return true when recordings can be made
     */
    public static boolean isAvailable() {
        try {
            return FlightRecorder.isAvailable();
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Starts recording, discarding any recording not yet analyzed
     * @throws IOException if the recording file cannot be created
     * @throws IllegalStateException if a recording is already running or Flight Recorder is unavailable
     */
    public synchronized void startRecording() throws IOException {
        if (isRecording) {
            throw new IllegalStateException("Recording already in progress");
        }
        if (!isAvailable()) {
            throw new IllegalStateException("Flight Recorder is not available in this JVM");
        }
        discard();
        tempFile = Files.createTempFile("vitalite-recording-", ".jfr");
        recording = new Recording();
        recording.enable(EXECUTION).withPeriod(Duration.ofMillis(samplePeriodMs)).withStackTrace();
        recording.enable(NATIVE).withPeriod(Duration.ofMillis(samplePeriodMs)).withStackTrace();
        if (recordAllocations) {
            recording.enable(ALLOCATION_IN_TLAB).withStackTrace();
            recording.enable(ALLOCATION_OUTSIDE_TLAB).withStackTrace();
        }
        allocationsRecorded = recordAllocations;
        recording.setToDisk(true);
        recording.setDestination(tempFile);
        recording.start();
        recordingStartTime = System.currentTimeMillis();
        isRecording = true;
    }

    /**
     * Stops recording and writes the recording file, which can take a moment; safe to call more than once
     */
    public synchronized void stopRecording() {
        if (!isRecording) {
            return;
        }
        recordingEndTime = System.currentTimeMillis();
        isRecording = false;
        try {
            recording.stop();
        } finally {
            recording.close();
            recording = null;
        }
    }

    /**
     * Reads the stopped recording into results and deletes its file
     * @return the results, empty when nothing was recorded
     * @throws IOException if the recording file cannot be read
     */
    public synchronized MethodTimingResults analyze() throws IOException {
        StackAnalyzer analyzer = new StackAnalyzer(this::matchesFilter);
        if (tempFile == null || !Files.exists(tempFile)) {
            return analyzer.results(samplePeriodMs, recordingStartTime, recordingEndTime, false);
        }
        try (RecordingFile file = new RecordingFile(tempFile)) {
            while (file.hasMoreEvents()) {
                RecordedEvent event = file.readEvent();
                String name = event.getEventType().getName();
                RecordedStackTrace stack = event.getStackTrace();
                if (stack == null) {
                    continue;
                }
                switch (name) {
                    case EXECUTION:
                        analyzer.addCpuSample(frames(stack), stack.isTruncated());
                        break;
                    case NATIVE:
                        analyzer.addNativeSample(frames(stack));
                        break;
                    case ALLOCATION_IN_TLAB:
                        analyzer.addAllocation(className(event.getClass("objectClass")), event.getLong("tlabSize"), frames(stack));
                        break;
                    case ALLOCATION_OUTSIDE_TLAB:
                        analyzer.addAllocation(className(event.getClass("objectClass")), event.getLong("allocationSize"), frames(stack));
                        break;
                    default:
                        break;
                }
            }
        } finally {
            discard();
        }
        return analyzer.results(samplePeriodMs, recordingStartTime, recordingEndTime, allocationsRecorded);
    }

    /**
     * Stops any recording and deletes its file without analyzing it; safe to call more than once
     */
    public synchronized void discard() {
        if (isRecording) {
            stopRecording();
        }
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
            }
            tempFile = null;
        }
    }

    private static List<StackAnalyzer.Frame> frames(RecordedStackTrace stack) {
        List<StackAnalyzer.Frame> frames = new ArrayList<>();
        for (RecordedFrame frame : stack.getFrames()) {
            RecordedMethod method = frame.getMethod();
            if (frame.isJavaFrame() && method != null && method.getType() != null) {
                frames.add(new StackAnalyzer.Frame(method.getType().getName(), method.getName()));
            }
        }
        return frames;
    }

    private static String className(RecordedClass type) {
        return type == null ? "(unknown)" : type.getName();
    }

    private boolean matchesFilter(String className) {
        switch (filterMode) {
            case PACKAGE:
                return packageFilter.isEmpty() || className.startsWith(packageFilter);
            case CLASS:
                return classFilter.isEmpty() || className.equals(classFilter);
            default:
                return true;
        }
    }

    /**
     * Sets the sampling period
     * @param samplePeriodMs milliseconds between samples, clamped to 1 to 1000
     * @throws IllegalStateException if recording
     */
    public void setSamplePeriodMs(int samplePeriodMs) {
        requireStopped();
        this.samplePeriodMs = Math.max(1, Math.min(1000, samplePeriodMs));
    }

    /**
     * Sets which methods the results show
     * @param filterMode all methods, one package or one class
     * @throws IllegalStateException if recording
     */
    public void setFilterMode(FilterMode filterMode) {
        requireStopped();
        this.filterMode = filterMode != null ? filterMode : FilterMode.ALL;
    }

    /**
     * Sets the package the PACKAGE filter keeps
     * @param packageFilter a package prefix such as com.tonic
     * @throws IllegalStateException if recording
     */
    public void setPackageFilter(String packageFilter) {
        requireStopped();
        this.packageFilter = packageFilter != null ? packageFilter.trim() : "";
    }

    /**
     * Sets the class the CLASS filter keeps
     * @param classFilter a fully qualified class name
     * @throws IllegalStateException if recording
     */
    public void setClassFilter(String classFilter) {
        requireStopped();
        this.classFilter = classFilter != null ? classFilter.trim() : "";
    }

    /**
     * Sets whether allocations are recorded, which costs noticeably more than sampling alone
     * @param recordAllocations true to record allocation events with their stacks
     * @throws IllegalStateException if recording
     */
    public void setRecordAllocations(boolean recordAllocations) {
        requireStopped();
        this.recordAllocations = recordAllocations;
    }

    /**
     * Checks whether a recording is running
     * @return true while recording
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Gets how long the current or last recording ran
     * @return the duration in milliseconds
     */
    public long getRecordingDurationMs() {
        return (isRecording ? System.currentTimeMillis() : recordingEndTime) - recordingStartTime;
    }

    private void requireStopped() {
        if (isRecording) {
            throw new IllegalStateException("Cannot change settings while recording");
        }
    }
}

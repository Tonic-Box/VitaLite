package com.tonic.services.mouserecorder.markov;

import com.tonic.Logger;
import com.tonic.services.mouserecorder.MouseDataPoint;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Background service that records real mouse movements during gameplay.
 * Captures samples at regular intervals and makes them available for Markov chain training.
 * <p>
 * Thread-safe and designed to run continuously in the background without impacting performance.
 */
public class MouseRecordingService
{
    private final Supplier<Integer> mouseXSupplier;
    private final Supplier<Integer> mouseYSupplier;
    private final int samplingRateMs;
    private final int maxBufferSize;

    private final AtomicBoolean recording;
    private final List<MouseDataPoint> recordedSamples;
    private ScheduledExecutorService executor;

    /**
     * -- GETTER --
     *  Gets total samples recorded since service started.
     */
    @Getter
    private volatile long totalSamplesRecorded;
    private volatile long recordingStartTime;
    private volatile int lastX;
    private volatile int lastY;

    /**
     * Creates a mouse recording service.
     *
     * @param mouseXSupplier   Supplier for current mouse X coordinate
     * @param mouseYSupplier   Supplier for current mouse Y coordinate
     * @param samplingRateMs   Milliseconds between samples (default: 50ms = 20Hz)
     * @param maxBufferSize    Maximum samples to keep in buffer (older samples auto-removed)
     */
    public MouseRecordingService(Supplier<Integer> mouseXSupplier,
                                 Supplier<Integer> mouseYSupplier,
                                 int samplingRateMs,
                                 int maxBufferSize)
    {
        this.mouseXSupplier = mouseXSupplier;
        this.mouseYSupplier = mouseYSupplier;
        this.samplingRateMs = samplingRateMs;
        this.maxBufferSize = maxBufferSize;

        this.recording = new AtomicBoolean(false);
        this.recordedSamples = new CopyOnWriteArrayList<>();
        this.totalSamplesRecorded = 0;
        this.lastX = -1;
        this.lastY = -1;
    }

    /**
     * Starts recording mouse movements.
     */
    public synchronized void startRecording()
    {
        if (recording.get())
        {
            Logger.warn("MouseRecordingService already recording");
            return;
        }

        recording.set(true);
        recordingStartTime = System.currentTimeMillis();

        lastX = mouseXSupplier.get();
        lastY = mouseYSupplier.get();

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MouseRecordingService");
            t.setDaemon(true);
            return t;
        });

        executor.scheduleAtFixedRate(this::recordSample, 0, samplingRateMs, TimeUnit.MILLISECONDS);

        Logger.info("MouseRecordingService started (sampling: " + samplingRateMs + "ms)");
    }

    /**
     * Stops recording mouse movements.
     */
    public synchronized void stopRecording()
    {
        if (!recording.get())
        {
            return;
        }

        recording.set(false);

        if (executor != null)
        {
            executor.shutdown();
            try
            {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            executor = null;
        }

        Logger.info("MouseRecordingService stopped (recorded: " + totalSamplesRecorded + " samples)");
    }

    /**
     * Checks if currently recording.
     */
    public boolean isRecording()
    {
        return recording.get();
    }

    /**
     * Records a single mouse sample.
     */
    private void recordSample()
    {
        if (!recording.get())
        {
            return;
        }

        try
        {
            int x = mouseXSupplier.get();
            int y = mouseYSupplier.get();
            long time = System.currentTimeMillis();

            if (x == lastX && y == lastY)
            {
                return;
            }

            // Skip off-screen coordinates (-1,-1 means mouse is off-screen)
            // Recording these contaminates training data with invalid transitions
            if (x == -1 || y == -1)
            {
                return;
            }

            MouseDataPoint point = new MouseDataPoint(x, y, time);
            recordedSamples.add(point);
            totalSamplesRecorded++;

            if (recordedSamples.size() > maxBufferSize)
            {
                recordedSamples.remove(0);
            }

            lastX = x;
            lastY = y;
        }
        catch (Exception e)
        {
            if (recording.get())
            {
                Logger.error("Error recording mouse sample: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets all recorded samples and optionally clears the buffer.
     *
     * @param clearAfterGet If true, clears the buffer after returning samples
     * @return List of recorded samples
     */
    public List<MouseDataPoint> getSamples(boolean clearAfterGet)
    {
        List<MouseDataPoint> samples = new ArrayList<>(recordedSamples);

        if (clearAfterGet)
        {
            recordedSamples.clear();
        }

        return samples;
    }

    /**
     * Gets samples recorded since last call to this method.
     *
     * @return New samples since last poll
     */
    public List<MouseDataPoint> pollNewSamples()
    {
        return getSamples(true);
    }

    /**
     * Gets current buffer size.
     */
    public int getBufferSize()
    {
        return recordedSamples.size();
    }

    /**
     * Gets recording duration in milliseconds.
     */
    public long getRecordingDurationMs()
    {
        if (!recording.get())
        {
            return 0;
        }
        return System.currentTimeMillis() - recordingStartTime;
    }

    /**
     * Clears the sample buffer without stopping recording.
     */
    public void clearBuffer()
    {
        recordedSamples.clear();
    }

    /**
     * Returns statistics about the recording session.
     */
    public RecordingStatistics getStatistics()
    {
        return new RecordingStatistics(
            totalSamplesRecorded,
            recordedSamples.size(),
            getRecordingDurationMs(),
            recording.get(),
            samplingRateMs
        );
    }

    /**
     * Statistics about a recording session.
     */
    public static class RecordingStatistics
    {
        public final long totalSamples;
        public final int bufferSize;
        public final long durationMs;
        public final boolean isRecording;
        public final int samplingRateMs;

        private RecordingStatistics(long totalSamples, int bufferSize, long durationMs,
                                    boolean isRecording, int samplingRateMs)
        {
            this.totalSamples = totalSamples;
            this.bufferSize = bufferSize;
            this.durationMs = durationMs;
            this.isRecording = isRecording;
            this.samplingRateMs = samplingRateMs;
        }

        public double getSamplesPerSecond()
        {
            if (durationMs == 0) return 0;
            return (totalSamples * 1000.0) / durationMs;
        }

        @Override
        public String toString()
        {
            return String.format(
                "Recording{samples=%d, buffer=%d, duration=%.1fs, rate=%.1f/s, active=%b}",
                totalSamples, bufferSize, durationMs / 1000.0,
                getSamplesPerSecond(), isRecording
            );
        }
    }
}

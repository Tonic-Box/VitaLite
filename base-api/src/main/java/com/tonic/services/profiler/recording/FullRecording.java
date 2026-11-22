package com.tonic.services.profiler.recording;

import com.tonic.services.profiler.frames.FrameProfiler;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Duration;

/**
 * A full Flight Recorder recording written to a file for offline analysis: the JDK's profile settings, every safepoint and VM operation, waits of 1 ms or more, 5 ms execution samples, and one event per client frame.
 */
public final class FullRecording
{
    private static final Duration SAMPLE_PERIOD = Duration.ofMillis(5);
    private static final String[] UNFILTERED = {"jdk.SafepointBegin", "jdk.SafepointStateSynchronization", "jdk.SafepointEnd", "jdk.ExecuteVMOperation", "jdk.GarbageCollection", "jdk.GCPhasePause", "jdk.Deoptimization", "jdk.Compilation"};

    private FullRecording()
    {
    }

    /**
     * Records for a while and writes the recording to a new temporary file.
     *
     * @param seconds how long to record, clamped to 1 to 300
     * @return the file
     * @throws IOException if the file cannot be written
     * @throws ParseException if the JDK's profile settings cannot be read
     * @throws InterruptedException if interrupted while recording
     */
    public static Path record(int seconds) throws IOException, ParseException, InterruptedException
    {
        Path file = Files.createTempFile("vitalite-full-", ".jfr");
        try (Recording recording = new Recording(Configuration.getConfiguration("profile")))
        {
            for (String event : UNFILTERED)
            {
                recording.enable(event).withThreshold(Duration.ZERO);
            }
            recording.enable("jdk.ExecutionSample").withPeriod(SAMPLE_PERIOD);
            recording.enable("jdk.NativeMethodSample").withPeriod(SAMPLE_PERIOD);
            JFRMethodRecorder.enableLockWaits(recording);
            recording.enable(FrameProfiler.EVENT_NAME).withThreshold(Duration.ZERO);
            recording.start();
            Thread.sleep(Math.max(1, Math.min(300, seconds)) * 1000L);
            recording.stop();
            recording.dump(file);
        }
        return file;
    }
}

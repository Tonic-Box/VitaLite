package com.tonic.services.mouserecorder.trajectory;

import com.tonic.Logger;
import com.tonic.services.mouserecorder.DecodedMousePacket;
import com.tonic.services.mouserecorder.MouseDataPoint;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Captures actual client mouse movement packets during manual play for training data.
 * Provides thread-safe packet submission and stores trajectories directly to the database.
 */
public class TrajectoryPacketCapture
{
    private final TrajectoryDatabase database;
    private final AtomicBoolean recording;
    private final AtomicInteger packetsProcessed;
    private final AtomicInteger trajectoriesStored;

    public TrajectoryPacketCapture(TrajectoryDatabase database)
    {
        this.database = database;
        this.recording = new AtomicBoolean(false);
        this.packetsProcessed = new AtomicInteger(0);
        this.trajectoriesStored = new AtomicInteger(0);
    }

    /**
     * Starts packet capture.
     */
    public void startRecording()
    {
        if (recording.compareAndSet(false, true))
        {
            Logger.info("Started trajectory packet capture");
        }
    }

    /**
     * Stops packet capture.
     */
    public void stopRecording()
    {
        if (recording.compareAndSet(true, false))
        {
            Logger.info("Stopped trajectory packet capture (processed=" + packetsProcessed.get() +
                            ", stored=" + trajectoriesStored.get() + ")");
        }
    }

    /**
     * Submits an already-decoded mouse movement packet directly.
     * Use this if you already have DecodedMousePacket objects from logging.
     * Thread-safe and stores immediately to database.
     *
     * @param decoded Decoded mouse movement packet
     */
    public void submitDecodedPacket(DecodedMousePacket decoded)
    {
        if (!recording.get())
        {
            return;
        }

        if (decoded == null || decoded.getSampleCount() < 1)
        {
            return;
        }

        try
        {
            List<MouseDataPoint> points = decoded.toMouseDataPoints();

            if (!points.isEmpty())
            {
                database.addTrajectory(points);
                packetsProcessed.incrementAndGet();
                int stored = trajectoriesStored.incrementAndGet();

                if (stored % 50 == 0)
                {
                    Logger.info("Stored " + stored + " trajectories from captured packets");
                }
            }
        }
        catch (Exception e)
        {
            Logger.error("Failed to store decoded packet: " + e.getMessage());
        }
    }

    /**
     * Returns true if currently recording packets.
     */
    public boolean isRecording()
    {
        return recording.get();
    }

    /**
     * Gets the number of packets processed.
     */
    public int getPacketsProcessed()
    {
        return packetsProcessed.get();
    }

    /**
     * Gets the number of trajectories stored.
     */
    public int getTrajectoriesStored()
    {
        return trajectoriesStored.get();
    }

    /**
     * Resets statistics counters.
     */
    public void resetStatistics()
    {
        packetsProcessed.set(0);
        trajectoriesStored.set(0);
    }

    /**
     * Gets capture statistics as a formatted string.
     */
    public String getStatisticsString()
    {
        return String.format("PacketCapture{recording=%s, processed=%d, stored=%d}",
            isRecording(), packetsProcessed.get(), trajectoriesStored.get());
    }
}

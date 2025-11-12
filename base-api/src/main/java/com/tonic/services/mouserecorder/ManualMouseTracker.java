package com.tonic.services.mouserecorder;

import com.google.common.eventbus.Subscribe;
import com.tonic.Logger;
import com.tonic.events.PacketSent;
import com.tonic.packets.PacketMapReader;
import com.tonic.packets.types.MapEntry;
import com.tonic.services.mouserecorder.trajectory.TrajectoryGeneratorConfig;
import com.tonic.services.mouserecorder.trajectory.TrajectoryService;
import com.tonic.util.config.ConfigFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Tracks manual mouse movements from the game client and records them as training data.
 * Samples mouse position at regular intervals and detects movement sequences.
 */
public class ManualMouseTracker
{
    private static int CLICK_PACKET_ID = -1;
    private static final long SAMPLE_INTERVAL_MS = 20;
    private static final TrajectoryGeneratorConfig config = ConfigFactory.create(TrajectoryGeneratorConfig.class);

    private static Timer samplingTimer = null;
    private static final List<MouseDataPoint> currentSequence = new ArrayList<>();
    private static long lastMovementTime = 0;
    private static long lastClickTime = 0;
    private static long lastCheckedClickTime = 0;
    private static int lastX = -1;
    private static int lastY = -1;
    private static boolean isTracking = false;
    private static java.awt.event.MouseAdapter clickListener = null;
    private static java.awt.Component trackedCanvas = null;

    /**
     * Start tracking manual mouse movements.
     * @param xProvider Provider for X coordinate
     * @param yProvider Provider for Y coordinate
     * @param canvas The game canvas component to listen for clicks on
     */
    public static synchronized void startTracking(CoordinateProvider xProvider, CoordinateProvider yProvider, java.awt.Component canvas)
    {
        if (isTracking)
        {
            Logger.info("ManualMouseTracker: Already tracking");
            return;
        }

        isTracking = true;
        lastClickTime = System.currentTimeMillis();
        lastCheckedClickTime = lastClickTime;
        trackedCanvas = canvas;

        samplingTimer = new Timer("ManualMouseTracker", true);

        samplingTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                try
                {
                    sampleMousePosition(xProvider, yProvider);
                }
                catch (Exception e)
                {
                    Logger.error("ManualMouseTracker: Error sampling mouse: " + e.getMessage());
                }
            }
        }, 0, SAMPLE_INTERVAL_MS);

        Logger.info("ManualMouseTracker: Started tracking manual mouse movements");
    }

    /**
     * Stop tracking manual mouse movements.
     */
    public static synchronized void stopTracking()
    {
        if (!isTracking)
        {
            return;
        }

        isTracking = false;

        if (samplingTimer != null)
        {
            samplingTimer.cancel();
            samplingTimer = null;
        }

        if (clickListener != null && trackedCanvas != null)
        {
            trackedCanvas.removeMouseListener(clickListener);
        }
        clickListener = null;
        trackedCanvas = null;

        finalizeCurrentSequence();
        Logger.info("ManualMouseTracker: Stopped tracking");
    }

    private static void sampleMousePosition(CoordinateProvider xProvider, CoordinateProvider yProvider)
    {
        int currentX = xProvider.get();
        int currentY = yProvider.get();
        long currentTime = System.currentTimeMillis();

        // Check if a click occurred (AWT listener updated lastClickTime)
        if (lastClickTime != lastCheckedClickTime)
        {
            if (!currentSequence.isEmpty())
            {
                finalizeCurrentSequence();
            }
            lastCheckedClickTime = lastClickTime;
            return;
        }

        if (currentX == -1 || currentY == -1)
        {
            if (!currentSequence.isEmpty())
            {
                finalizeCurrentSequence();
            }
            return;
        }

        if (lastX == currentX && lastY == currentY)
        {
            if (!currentSequence.isEmpty() && (currentTime - lastMovementTime) > config.getMovementTimeoutMs())
            {
                finalizeCurrentSequence();
            }
            return;
        }

        MouseDataPoint point = new MouseDataPoint(currentX, currentY, currentTime);
        currentSequence.add(point);

        lastX = currentX;
        lastY = currentY;
        lastMovementTime = currentTime;
    }

    private static void finalizeCurrentSequence()
    {
        if (currentSequence.isEmpty())
        {
            return;
        }

        if (currentSequence.size() >= 2)
        {
            List<MouseDataPoint> sequence = new ArrayList<>(currentSequence);
            TrajectoryService.recordTrajectory(sequence);
        }

        currentSequence.clear();
        lastMovementTime = System.currentTimeMillis();
    }

    /**
     * Interface for getting a single coordinate value.
     */
    public interface CoordinateProvider
    {
        int get();
    }

    public static void onPacketSent(PacketSent packetSent)
    {
        if(CLICK_PACKET_ID == -1)
        {
            MapEntry clickPacketEntry = PacketMapReader.get("OP_MOUSE_CLICK");
            CLICK_PACKET_ID = clickPacketEntry.getPacket().getId();
        }

        if(packetSent.getId() != CLICK_PACKET_ID)
        {
            return;
        }

        lastClickTime = System.currentTimeMillis();
    }
}

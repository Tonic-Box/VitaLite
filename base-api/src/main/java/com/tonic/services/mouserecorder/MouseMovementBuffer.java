package com.tonic.services.mouserecorder;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.TPacketBufferNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Mimics the OSRS client's MouseRecorder behavior exactly.
 *
 * Continuously samples mouse position every 50ms and buffers samples.
 * Sends movement packets when:
 * - Buffer reaches 40+ samples (~2 seconds of movement)
 * - Mouse button is pressed (via forceFlush() called from ClickManager)
 * - Mouse button is held (dragging)
 *
 * This matches Client.java:3008-3108 behavior precisely.
 * The flush check at line 3011 runs every game cycle (~20ms):
 *   if (MouseHandler_lastButton != 0 || index >= 40) { flush }
 *
 * This creates the natural pattern of:
 * - Regular 2-second packets during continuous movement (40 samples)
 * - Immediate flush on button press (smaller packets on clicks)
 */
public class MouseMovementBuffer
{
    private static final long SAMPLE_INTERVAL_MS = 50;
    private static final int FLUSH_THRESHOLD = 40;
    private static final int MAX_BUFFER_SIZE = 500;

    private static MouseMovementBuffer instance = null;
    private final Object lock = new Object();

    // Circular buffer matching client's MouseRecorder
    private final int[] xs = new int[MAX_BUFFER_SIZE];
    private final int[] ys = new int[MAX_BUFFER_SIZE];
    private final long[] millis = new long[MAX_BUFFER_SIZE];
    private int index = 0;

    private Timer samplingTimer = null;
    private boolean isRunning = false;

    // Current mouse position (updated by movement generator or idle jitter)
    private int currentX = -1;
    private int currentY = -1;
    private long lastFlushTime = 0;

    // Track if we're currently dragging
    private boolean isDragging = false;

    // Path playback for simulating mouse movement over time
    private List<MouseDataPoint> plannedPath = null;
    private int pathIndex = 0;
    private long pathStartTime = 0;

    private MouseMovementBuffer()
    {
    }

    public static synchronized MouseMovementBuffer getInstance()
    {
        if (instance == null)
        {
            instance = new MouseMovementBuffer();
        }
        return instance;
    }

    /**
     * Starts the background sampling thread.
     */
    public synchronized void start()
    {
        if (isRunning)
        {
            return;
        }

        isRunning = true;
        samplingTimer = new Timer("MouseMovementBuffer-Sampler", true);

        samplingTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                sample();
            }
        }, 0, SAMPLE_INTERVAL_MS);

        Logger.info("MouseMovementBuffer: Started sampling (50ms interval)");
    }

    /**
     * Stops the background sampling thread.
     */
    public synchronized void stop()
    {
        if (!isRunning)
        {
            return;
        }

        isRunning = false;

        if (samplingTimer != null)
        {
            samplingTimer.cancel();
            samplingTimer = null;
        }

        // Flush any remaining samples
        flush();

        // Reset encoder's persistent state
        MousePacketEncoder.reset();

        Logger.info("MouseMovementBuffer: Stopped sampling");
    }

    /**
     * Sets the current mouse position for sampling.
     * Called by idle jitter or when not following a path.
     */
    public void setCurrentPosition(int x, int y)
    {
        synchronized (lock)
        {
            this.currentX = x;
            this.currentY = y;
            this.plannedPath = null; // Clear any ongoing path
        }
    }

    /**
     * Starts playing back a movement path over time.
     * The sampling thread will follow this path based on timestamps.
     *
     * @param path List of points with timestamps to follow
     */
    public void playPath(List<MouseDataPoint> path)
    {
        synchronized (lock)
        {
            if (path == null || path.isEmpty())
            {
                return;
            }

            this.plannedPath = new ArrayList<>(path);
            this.pathIndex = 0;
            this.pathStartTime = System.currentTimeMillis();

            // Set initial position
            if (!plannedPath.isEmpty())
            {
                MouseDataPoint first = plannedPath.get(0);
                this.currentX = first.getX();
                this.currentY = first.getY();
            }
        }
    }

    /**
     * Sets dragging state.
     * When true, forces immediate flush on next sample.
     */
    public void setDragging(boolean dragging)
    {
        synchronized (lock)
        {
            this.isDragging = dragging;
        }
    }

    /**
     * Samples current mouse position and adds to buffer.
     * Matches MouseRecorder.run() behavior exactly.
     * If following a path, updates position based on elapsed time.
     */
    private void sample()
    {
        if (!isRunning)
        {
            return;
        }

        synchronized (lock)
        {
            // Update position if following a planned path
            updatePositionFromPath();

            if (index < MAX_BUFFER_SIZE)
            {
                xs[index] = currentX;
                ys[index] = currentY;
                millis[index] = System.currentTimeMillis();
                index++;
            }
        }

        // Check if we should flush (matches Client.java:3011)
        checkFlush();
    }

    /**
     * Updates current position by following the planned path based on elapsed time.
     * Called by sampling thread to simulate mouse moving along generated path.
     */
    private void updatePositionFromPath()
    {
        if (plannedPath == null || pathIndex >= plannedPath.size())
        {
            return;
        }

        long elapsed = System.currentTimeMillis() - pathStartTime;
        MouseDataPoint firstPoint = plannedPath.get(0);

        // Find which point we should be at based on elapsed time
        while (pathIndex < plannedPath.size())
        {
            MouseDataPoint point = plannedPath.get(pathIndex);
            long pointTime = point.getTimestampMillis() - firstPoint.getTimestampMillis();

            if (pointTime <= elapsed)
            {
                currentX = point.getX();
                currentY = point.getY();
                pathIndex++;
            }
            else
            {
                break; // Haven't reached this point yet
            }
        }

        // Path complete
        if (pathIndex >= plannedPath.size())
        {
            plannedPath = null;
        }
    }

    /**
     * Checks if buffer should be flushed and sends packet if needed.
     * Matches Client.java:3011 flush condition exactly:
     *   if (MouseHandler_lastButton != 0 || index >= 40)
     *
     * Checked every sample (50ms) to approximate the game's per-cycle (20ms) checks.
     */
    private void checkFlush()
    {
        synchronized (lock)
        {
            // Flush when: dragging (button held) OR buffer reaches 40 samples
            // Button press triggers forceFlush() from ClickManager before click packet
            if (isDragging || index >= FLUSH_THRESHOLD)
            {
                flush();
            }
        }
    }

    /**
     * Encodes buffered samples and sends movement packet.
     * Matches Client.java:3018-3106 encoding logic.
     */
    private void flush()
    {
        synchronized (lock)
        {
            if (index == 0)
            {
                return;
            }

            EncodedMousePacket packet = null;

            try
            {
                long flushStartTime = System.currentTimeMillis();

                List<MouseDataPoint> points = new ArrayList<>(index);
                for (int i = 0; i < index; i++)
                {
                    points.add(new MouseDataPoint(xs[i], ys[i], millis[i]));
                }

                MouseMovementSequence sequence = new MouseMovementSequence(points);
                packet = MousePacketEncoder.encode(sequence);

                if (packet == null)
                {
                    index = 0;
                    lastFlushTime = flushStartTime;
                    return;
                }

                EncodedMousePacket finalPacket = packet;
                boolean await = Static.invoke(() -> {
                    try
                    {
                        if (finalPacket.getSamplesEncoded() > 0)
                        {
                            sendPacket(finalPacket);

                            int consumed = finalPacket.getSamplesEncoded();
                            if (consumed >= index)
                            {
                                index = 0;
                            }
                            else
                            {
                                index -= consumed;
                                System.arraycopy(xs, consumed, xs, 0, index);
                                System.arraycopy(ys, consumed, ys, 0, index);
                                System.arraycopy(millis, consumed, millis, 0, index);
                            }

                            lastFlushTime = flushStartTime;
                        }
                        else
                        {
                            finalPacket.getBuffer().dispose();
                        }
                    }
                    catch (Exception e)
                    {
                        finalPacket.getBuffer().dispose();
                        Logger.error("MouseMovementBuffer: Send packet failed: " + e.getMessage());
                        index = 0; // Clear buffer on error
                    }
                    return true;
                });
            }
            catch (Exception e)
            {
                if (packet != null)
                {
                    packet.getBuffer().dispose();
                }
                Logger.error("MouseMovementBuffer: Flush failed: " + e.getMessage());
                index = 0;
            }
        }
    }

    /**
     * Sends the encoded packet to the server.
     */
    private void sendPacket(EncodedMousePacket packet)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            try
            {
                TPacketBufferNode node = packet.getBuffer().toPacketBufferNode(client);
                client.getPacketWriter().addNode(node);
            }
            catch (Exception e)
            {
                packet.getBuffer().dispose();
                Logger.error("MouseMovementBuffer: Failed to send packet: " + e.getMessage());
            }
        });
    }

    /**
     * Gets current buffer size.
     */
    public int getBufferSize()
    {
        synchronized (lock)
        {
            return index;
        }
    }

    /**
     * Clears the buffer immediately.
     */
    public void clear()
    {
        synchronized (lock)
        {
            index = 0;
        }
    }

    /**
     * Forces an immediate flush regardless of buffer size.
     */
    public void forceFlush()
    {
        flush();
    }
}
